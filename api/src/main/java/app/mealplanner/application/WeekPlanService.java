package app.mealplanner.application;

import app.mealplanner.application.CatalogueService.RecipeView;
import app.mealplanner.domain.planning.AssignmentViolation;
import app.mealplanner.domain.planning.Diner;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.domain.planning.MealAssignmentRules;
import app.mealplanner.domain.planning.MealServings;
import app.mealplanner.domain.planning.MealSlot;
import app.mealplanner.domain.shopping.PantryRollback;
import app.mealplanner.domain.shopping.PlannedMeal;
import app.mealplanner.domain.shopping.ShoppingList;
import app.mealplanner.domain.shopping.ShoppingListCalculator;
import app.mealplanner.domain.shopping.ShoppingListItem;
import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.MealEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.PantryItemEntity;
import app.mealplanner.persistence.Repositories.PantryItemRepository;
import app.mealplanner.persistence.Repositories.ShoppingListRepository;
import app.mealplanner.persistence.Repositories.WeekPlanRepository;
import app.mealplanner.persistence.ShoppingListEntity;
import app.mealplanner.persistence.ShoppingListItemEntity;
import app.mealplanner.persistence.WeekPlanEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WeekPlanService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d/M", Locale.forLanguageTag("ca"));

	private final WeekPlanRepository weeks;
	private final PantryItemRepository pantry;
	private final ShoppingListRepository shoppingLists;
	private final HouseholdService household;
	private final CatalogueService catalogue;
	private final Clock clock;

	public WeekPlanService(WeekPlanRepository weeks, PantryItemRepository pantry,
			ShoppingListRepository shoppingLists, HouseholdService household, CatalogueService catalogue,
			Clock clock) {
		this.weeks = weeks;
		this.pantry = pantry;
		this.shoppingLists = shoppingLists;
		this.household = household;
		this.catalogue = catalogue;
		this.clock = clock;
	}

	/** Who eats a meal and where. */
	public record MealConfig(LocalDate date, MealSlot slot, Map<Long, DinerLocation> attendees, int extraServings) {
	}

	public record MealDetail(
			MealEntity meal,
			RecipeView recipe,
			int dinersAtHome,
			int dinersTakeaway,
			List<String> notes) {

		public int totalServings() {
			return dinersAtHome + dinersTakeaway + meal.getExtraServings();
		}
	}

	public record WeekDetail(WeekPlanEntity week, List<MealDetail> meals) {
	}

	// ---------------------------------------------------------------- queries

	@Transactional(readOnly = true)
	public List<WeekPlanEntity> list() {
		return household.find().map(h -> weeks.findByHouseholdIdOrderByWeekStartDesc(h.getId())).orElse(List.of());
	}

	@Transactional(readOnly = true)
	public WeekDetail detail(long weekId) {
		WeekPlanEntity week = week(weekId);
		Map<Long, MemberEntity> members = membersById();
		List<MealDetail> meals = week.getMeals().stream()
				.sorted(java.util.Comparator.comparing(MealEntity::getDate).thenComparing(MealEntity::getSlot))
				.map(meal -> detail(meal, members))
				.toList();
		return new WeekDetail(week, meals);
	}

	/** Recipes that satisfy every hard constraint of a meal: what the recipe picker offers. */
	@Transactional(readOnly = true)
	public List<RecipeView> allowedRecipes(long weekId, long mealId) {
		MealEntity meal = meal(week(weekId), mealId);
		List<Diner> diners = diners(meal, membersById());
		return catalogue.recipes().stream()
				.filter(view -> MealAssignmentRules.allows(view.recipe(), diners))
				.toList();
	}

	/** Pantry staples (oil, spices) used by the week's dishes, by name. */
	@Transactional(readOnly = true)
	public List<String> staplesUsed(long weekId) {
		return week(weekId).getMeals().stream()
				.filter(meal -> meal.getRecipeId() != null)
				.flatMap(meal -> catalogue.recipe(meal.getRecipeId()).recipe().ingredients().stream())
				.map(line -> line.ingredient())
				.filter(ingredient -> ingredient.pantryStaple())
				.map(ingredient -> ingredient.name())
				.distinct()
				.sorted(java.text.Collator.getInstance(Locale.forLanguageTag("ca")))
				.toList();
	}

	// ---------------------------------------------------------------- commands

	/** Creates a draft week with every lunch and dinner planned for every member, at home. */
	public WeekPlanEntity create(LocalDate weekStart) {
		if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw new Errors.RuleViolation("WEEK_START_NOT_MONDAY", "Les setmanes comencen en dilluns.", List.of());
		}
		HouseholdEntity home = household.require();
		if (weeks.existsByHouseholdIdAndWeekStart(home.getId(), weekStart)) {
			throw new Errors.Conflict("WEEK_EXISTS", "Ja hi ha una planificació per a aquesta setmana.");
		}
		WeekPlanEntity week = new WeekPlanEntity(home, weekStart);
		Map<Long, DinerLocation> everyoneHome = household.members().stream()
				.collect(Collectors.toMap(MemberEntity::getId, m -> DinerLocation.HOME));
		for (int day = 0; day < 7; day++) {
			for (MealSlot slot : MealSlot.values()) {
				MealEntity meal = new MealEntity(week, weekStart.plusDays(day), slot);
				meal.getAttendees().putAll(everyoneHome);
				week.getMeals().add(meal);
			}
		}
		return weeks.save(week);
	}

	public void delete(long weekId) {
		weeks.delete(draft(weekId));
	}

	/**
	 * Copies a week's menu onto another week: same dish, diners and extra servings for each day
	 * and slot. The target week is created if needed; if it is a draft its meals are replaced. A
	 * confirmed target is refused. Dishes that no longer fit (members changed since) are removed
	 * with the reason, as when reconfiguring a week.
	 */
	public WeekDetail copy(long sourceWeekId, LocalDate targetWeekStart) {
		WeekPlanEntity source = week(sourceWeekId);
		if (targetWeekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw new Errors.RuleViolation("WEEK_START_NOT_MONDAY", "Les setmanes comencen en dilluns.", List.of());
		}
		if (targetWeekStart.equals(source.getWeekStart())) {
			throw new Errors.RuleViolation("SAME_WEEK", "Trieu una setmana diferent de la d'origen.", List.of());
		}
		HouseholdEntity home = household.require();
		WeekPlanEntity target = weeks.findByHouseholdIdAndWeekStart(home.getId(), targetWeekStart)
				.orElseGet(() -> weeks.save(new WeekPlanEntity(home, targetWeekStart)));
		if (target.getStatus() != WeekPlanEntity.Status.DRAFT) {
			throw new Errors.Conflict("WEEK_CONFIRMED",
					"La setmana de destinació ja està confirmada. Torneu-la a esborrany per poder-hi copiar un menú.");
		}
		// Remove the old meals first: the new ones reuse the same (day, slot) keys.
		target.getMeals().clear();
		weeks.saveAndFlush(target);

		Map<Long, MemberEntity> members = membersById();
		long offset = java.time.temporal.ChronoUnit.DAYS.between(source.getWeekStart(), targetWeekStart);
		for (MealEntity original : source.getMeals()) {
			MealEntity copy = new MealEntity(target, original.getDate().plusDays(offset), original.getSlot());
			original.getAttendees().forEach((memberId, location) -> {
				if (members.containsKey(memberId)) {
					copy.getAttendees().put(memberId, location);
				}
			});
			if (copy.getAttendees().isEmpty()) {
				continue;
			}
			copy.setExtraServings(original.getExtraServings());
			copy.setRecipeId(original.getRecipeId());
			copy.setLocked(original.isLocked());
			dropRecipeIfNowInvalid(copy, members);
			target.getMeals().add(copy);
		}
		weeks.saveAndFlush(target);
		return detail(target.getId());
	}

	/**
	 * Replaces the week's set of meals. Meals that keep their day and slot keep their recipe and
	 * lock; if the new diners make that recipe invalid, it is removed and the reason recorded.
	 */
	public WeekDetail configure(long weekId, List<MealConfig> configs) {
		WeekPlanEntity week = draft(weekId);
		Map<Long, MemberEntity> members = membersById();
		validateConfigs(week, configs, members);

		Map<String, MealEntity> existing = week.getMeals().stream()
				.collect(Collectors.toMap(m -> key(m.getDate(), m.getSlot()), Function.identity()));
		Set<String> wanted = new HashSet<>();
		for (MealConfig config : configs) {
			String key = key(config.date(), config.slot());
			wanted.add(key);
			MealEntity meal = existing.get(key);
			if (meal == null) {
				meal = new MealEntity(week, config.date(), config.slot());
				week.getMeals().add(meal);
			}
			meal.getAttendees().clear();
			meal.getAttendees().putAll(config.attendees());
			meal.setExtraServings(config.extraServings());
			dropRecipeIfNowInvalid(meal, members);
		}
		week.getMeals().removeIf(meal -> !wanted.contains(key(meal.getDate(), meal.getSlot())));
		weeks.saveAndFlush(week);
		return detail(weekId);
	}

	/** Assigns a recipe to a meal by hand, or clears it with {@code null}. */
	public WeekDetail assignRecipe(long weekId, long mealId, Long recipeId) {
		WeekPlanEntity week = draft(weekId);
		MealEntity meal = meal(week, mealId);
		if (recipeId != null) {
			RecipeView recipe = catalogue.recipe(recipeId);
			rejectIfInvalid(recipe, meal, membersById());
		}
		meal.setRecipeId(recipeId);
		meal.setEmptyReason(null);
		return detail(weekId);
	}

	public WeekDetail updateMeal(long weekId, long mealId, Boolean locked, Integer extraServings) {
		WeekPlanEntity week = draft(weekId);
		MealEntity meal = meal(week, mealId);
		if (locked != null) {
			meal.setLocked(locked);
		}
		if (extraServings != null) {
			meal.setExtraServings(extraServings);
		}
		return detail(weekId);
	}

	/** Swaps the dishes of two meals, as when dragging one card onto another. Locks travel with the dish. */
	public WeekDetail swap(long weekId, long mealIdA, long mealIdB) {
		WeekPlanEntity week = draft(weekId);
		MealEntity a = meal(week, mealIdA);
		MealEntity b = meal(week, mealIdB);
		Map<Long, MemberEntity> members = membersById();
		if (b.getRecipeId() != null) {
			rejectIfInvalid(catalogue.recipe(b.getRecipeId()), a, members);
		}
		if (a.getRecipeId() != null) {
			rejectIfInvalid(catalogue.recipe(a.getRecipeId()), b, members);
		}
		Long recipeA = a.getRecipeId();
		boolean lockedA = a.isLocked();
		a.setRecipeId(b.getRecipeId());
		a.setLocked(b.isLocked());
		b.setRecipeId(recipeA);
		b.setLocked(lockedA);
		a.setEmptyReason(null);
		b.setEmptyReason(null);
		return detail(weekId);
	}

	/**
	 * Confirms the week: every meal must have a valid recipe. Produces the shopping list snapshot
	 * and moves the pantry forward to what will be left once the week is cooked.
	 */
	public WeekDetail confirm(long weekId) {
		WeekPlanEntity week = draft(weekId);
		if (week.getMeals().isEmpty()) {
			throw new Errors.RuleViolation("EMPTY_WEEK", "La setmana no té cap àpat.", List.of());
		}
		List<String> unassigned = week.getMeals().stream()
				.filter(meal -> meal.getRecipeId() == null)
				.map(meal -> label(meal.getDate(), meal.getSlot()))
				.toList();
		if (!unassigned.isEmpty()) {
			throw new Errors.RuleViolation("UNASSIGNED_MEALS", "Hi ha àpats sense plat.", unassigned);
		}
		Map<Long, MemberEntity> members = membersById();
		List<PlannedMeal> planned = new ArrayList<>();
		for (MealEntity meal : week.getMeals()) {
			RecipeView recipe = catalogue.recipe(meal.getRecipeId());
			rejectIfInvalid(recipe, meal, members);
			planned.add(new PlannedMeal(recipe.recipe(), servings(meal)));
		}

		HouseholdEntity home = household.require();
		List<PantryItemEntity> pantryItems = pantry.findByHouseholdId(home.getId());
		Map<Long, BigDecimal> atHome = pantryItems.stream()
				.collect(Collectors.toMap(PantryItemEntity::getIngredientId, PantryItemEntity::getQuantity));
		ShoppingList list = ShoppingListCalculator.calculate(planned, atHome);

		ShoppingListEntity snapshot = new ShoppingListEntity(week, clock.instant());
		for (ShoppingListItem item : list.items()) {
			snapshot.getItems().add(new ShoppingListItemEntity(snapshot, item.ingredient().id(), item.required(),
					item.fromPantry(), item.toBuy(), item.packages(), item.ingredient().packageSize(), item.leftover()));
		}
		shoppingLists.save(snapshot);
		updatePantry(home, pantryItems, list.pantryAfter());
		week.confirm(clock.instant());
		return detail(weekId);
	}

	/**
	 * Takes a confirmed week back to draft, e.g. after confirming by mistake: the shopping list is
	 * discarded and the pantry movement of the confirmation is undone. Refused while a later week
	 * is confirmed, because that week's pantry was computed from this one's leftovers.
	 */
	public WeekDetail reopen(long weekId) {
		WeekPlanEntity week = week(weekId);
		if (week.getStatus() != WeekPlanEntity.Status.CONFIRMED) {
			throw new Errors.Conflict("WEEK_NOT_CONFIRMED", "La setmana ja és un esborrany.");
		}
		HouseholdEntity home = household.require();
		List<WeekPlanEntity> later = weeks.findByHouseholdIdAndStatusAndWeekStartAfterOrderByWeekStartAsc(
				home.getId(), WeekPlanEntity.Status.CONFIRMED, week.getWeekStart());
		if (!later.isEmpty()) {
			throw new Errors.Conflict("LATER_WEEK_CONFIRMED", "Primer cal tornar a esborrany la setmana del "
					+ DateTimeFormatter.ofPattern("d/M", Locale.forLanguageTag("ca")).format(later.getLast().getWeekStart())
					+ ", que es va calcular amb el rebost que deixa aquesta.");
		}

		shoppingLists.findByWeekPlanId(weekId).ifPresent(list -> {
			List<PantryItemEntity> pantryItems = pantry.findByHouseholdId(home.getId());
			Map<Long, BigDecimal> current = pantryItems.stream()
					.collect(Collectors.toMap(PantryItemEntity::getIngredientId, PantryItemEntity::getQuantity));
			List<PantryRollback.Movement> movements = list.getItems().stream()
					.map(item -> new PantryRollback.Movement(item.getIngredientId(), item.getFromPantryQty(),
							item.getLeftoverQty()))
					.toList();
			updatePantry(home, pantryItems, PantryRollback.undo(current, movements));
			shoppingLists.delete(list);
		});
		week.reopen();
		return detail(weekId);
	}

	// ---------------------------------------------------------------- helpers

	private void updatePantry(HouseholdEntity home, List<PantryItemEntity> current, Map<Long, BigDecimal> after) {
		Map<Long, PantryItemEntity> byIngredient = current.stream()
				.collect(Collectors.toMap(PantryItemEntity::getIngredientId, Function.identity()));
		for (PantryItemEntity item : current) {
			if (!after.containsKey(item.getIngredientId())) {
				pantry.delete(item);
			}
		}
		after.forEach((ingredientId, quantity) -> {
			PantryItemEntity item = byIngredient.get(ingredientId);
			if (item == null) {
				pantry.save(new PantryItemEntity(home, ingredientId, quantity));
			} else {
				item.setQuantity(quantity);
			}
		});
	}

	private MealDetail detail(MealEntity meal, Map<Long, MemberEntity> members) {
		MealServings servings = servings(meal);
		RecipeView recipe = meal.getRecipeId() == null ? null : catalogue.recipe(meal.getRecipeId());
		List<String> notes = recipe == null ? List.of() : dislikeNotes(recipe, meal, members);
		return new MealDetail(meal, recipe, servings.dinersAtHome(), servings.dinersTakeaway(), notes);
	}

	private List<String> dislikeNotes(RecipeView recipe, MealEntity meal, Map<Long, MemberEntity> members) {
		Set<Long> inRecipe = recipe.recipe().ingredients().stream()
				.map(line -> line.ingredient().id())
				.collect(Collectors.toSet());
		List<String> notes = new ArrayList<>();
		for (Long memberId : meal.getAttendees().keySet()) {
			MemberEntity member = members.get(memberId);
			if (member == null) {
				continue;
			}
			member.getDislikedIngredientIds().stream()
					.filter(inRecipe::contains)
					.map(id -> catalogue.ingredient(id).name().toLowerCase(Locale.forLanguageTag("ca")))
					.forEach(name -> notes.add("A " + member.getName() + " no li agrada: " + name + "."));
		}
		return notes;
	}

	private void dropRecipeIfNowInvalid(MealEntity meal, Map<Long, MemberEntity> members) {
		if (meal.getRecipeId() == null) {
			return;
		}
		RecipeView recipe = catalogue.recipe(meal.getRecipeId());
		List<AssignmentViolation> violations = MealAssignmentRules.violations(recipe.recipe(), diners(meal, members));
		if (!violations.isEmpty()) {
			meal.setRecipeId(null);
			meal.setLocked(false);
			meal.setEmptyReason("S'ha tret el plat: " + Labels.describe(violations.getFirst(), recipe.recipe().name()));
		}
	}

	private void rejectIfInvalid(RecipeView recipe, MealEntity meal, Map<Long, MemberEntity> members) {
		List<AssignmentViolation> violations = MealAssignmentRules.violations(recipe.recipe(), diners(meal, members));
		if (!violations.isEmpty()) {
			throw new Errors.RuleViolation("ASSIGNMENT_NOT_ALLOWED",
					"No es pot posar «" + recipe.recipe().name() + "» al " + label(meal.getDate(), meal.getSlot()) + ".",
					violations.stream().map(v -> Labels.describe(v, recipe.recipe().name())).toList());
		}
	}

	List<Diner> diners(MealEntity meal, Map<Long, MemberEntity> members) {
		return meal.getAttendees().entrySet().stream()
				.filter(entry -> members.containsKey(entry.getKey()))
				.map(entry -> household.diner(members.get(entry.getKey()), entry.getValue()))
				.toList();
	}

	private static MealServings servings(MealEntity meal) {
		Collection<DinerLocation> locations = meal.getAttendees().values();
		int home = (int) locations.stream().filter(l -> l == DinerLocation.HOME).count();
		int takeaway = (int) locations.stream().filter(l -> l == DinerLocation.TAKEAWAY).count();
		return new MealServings(home, takeaway, meal.getExtraServings());
	}

	private void validateConfigs(WeekPlanEntity week, List<MealConfig> configs, Map<Long, MemberEntity> members) {
		List<String> problems = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		LocalDate start = week.getWeekStart();
		for (MealConfig config : configs) {
			String label = label(config.date(), config.slot());
			if (config.date().isBefore(start) || config.date().isAfter(start.plusDays(6))) {
				problems.add("El " + label + " no és d'aquesta setmana.");
			}
			if (!seen.add(key(config.date(), config.slot()))) {
				problems.add("El " + label + " està repetit.");
			}
			if (config.attendees().isEmpty()) {
				problems.add("El " + label + " no té ningú que mengi.");
			}
			config.attendees().keySet().stream()
					.filter(id -> !members.containsKey(id))
					.forEach(id -> problems.add("El membre " + id + " no és de la llar."));
		}
		if (!problems.isEmpty()) {
			throw new Errors.RuleViolation("INVALID_WEEK_CONFIGURATION", "La configuració de la setmana no és vàlida.",
					problems);
		}
	}

	private WeekPlanEntity week(long weekId) {
		return weeks.findWithMealsById(weekId).orElseThrow(() -> new Errors.NotFound("WEEK_NOT_FOUND",
				"No existeix la setmana " + weekId + "."));
	}

	WeekPlanEntity draft(long weekId) {
		WeekPlanEntity week = week(weekId);
		if (week.getStatus() != WeekPlanEntity.Status.DRAFT) {
			throw new Errors.Conflict("WEEK_CONFIRMED", "La setmana ja està confirmada i no es pot modificar.");
		}
		return week;
	}

	private static MealEntity meal(WeekPlanEntity week, long mealId) {
		return week.getMeals().stream()
				.filter(meal -> meal.getId() == mealId)
				.findFirst()
				.orElseThrow(() -> new Errors.NotFound("MEAL_NOT_FOUND", "No existeix l'àpat " + mealId + "."));
	}

	Map<Long, MemberEntity> membersById() {
		Map<Long, MemberEntity> byId = new HashMap<>();
		household.members().forEach(m -> byId.put(m.getId(), m));
		return byId;
	}

	private static String key(LocalDate date, MealSlot slot) {
		return date + "/" + slot;
	}

	static String label(LocalDate date, MealSlot slot) {
		return (slot == MealSlot.LUNCH ? "dinar de " : "sopar de ") + DAY.format(date);
	}
}
