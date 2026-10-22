package app.mealplanner.domain.generator;

import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.Cuisine;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.LunchboxServing;
import app.mealplanner.domain.catalogue.ProteinType;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeIngredient;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Season;
import app.mealplanner.domain.catalogue.Unit;
import app.mealplanner.domain.generator.MenuGenerator.PastUse;
import app.mealplanner.domain.generator.MenuGenerator.Result;
import app.mealplanner.domain.generator.MenuGenerator.Slot;
import app.mealplanner.domain.planning.AssignmentViolation;
import app.mealplanner.domain.planning.Diner;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.domain.planning.MealAssignmentRules;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MenuGeneratorTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

	private static final Ingredient BASE = ingredient(1, "Base", IngredientCategory.GRAIN, Set.of());
	private static final Ingredient FISH = ingredient(2, "Peix", IngredientCategory.FISH, Set.of(Allergen.FISH));
	private static final Ingredient CHICKEN = ingredient(3, "Pollastre", IngredientCategory.MEAT, Set.of());
	private static final Ingredient EGG = ingredient(4, "Ou", IngredientCategory.EGG, Set.of(Allergen.EGGS));
	private static final Ingredient LENTIL = ingredient(5, "Llenties", IngredientCategory.LEGUME, Set.of());

	private static final Diner ANNA_HOME = diner(1, "Anna", DinerLocation.HOME, DietType.OMNIVORE, Set.of());
	private static final Diner PAU_HOME = diner(2, "Pau", DinerLocation.HOME, DietType.OMNIVORE, Set.of());
	private static final Diner PAU_LUNCHBOX = diner(2, "Pau", DinerLocation.TAKEAWAY, DietType.OMNIVORE, Set.of());

	@Test
	void neverProposesANonPortableDishForALunchboxMeal() {
		List<Recipe> catalogue = mixedCatalogue();
		List<Slot> week = fullWeek(List.of(ANNA_HOME, PAU_LUNCHBOX), List.of(ANNA_HOME, PAU_HOME));

		for (int seed = 0; seed < 50; seed++) {
			for (Result result : new MenuGenerator(catalogue, new Random(seed)).generate(week, List.of())) {
				Slot slot = week.stream().filter(s -> s.mealId() == result.mealId()).findFirst().orElseThrow();
				assertThat(result).isInstanceOf(Result.Assigned.class);
				Recipe recipe = ((Result.Assigned) result).recipe();
				assertThat(MealAssignmentRules.allows(recipe, slot.diners()))
						.as("seed %d, meal %d: %s", seed, slot.mealId(), recipe.name())
						.isTrue();
				if (slot.order() == 0) {
					assertThat(recipe.portable()).isTrue();
				}
			}
		}
	}

	@Test
	void respectsAllergiesAndDiets() {
		Diner allergicToFish = diner(3, "Laia", DinerLocation.HOME, DietType.OMNIVORE, Set.of(Allergen.FISH));
		Diner vegetarian = diner(4, "Joan", DinerLocation.HOME, DietType.VEGETARIAN, Set.of());
		List<Slot> week = fullWeek(List.of(allergicToFish, vegetarian), List.of(allergicToFish, vegetarian));

		List<Result> results = new MenuGenerator(mixedCatalogue(), new Random(7)).generate(week, List.of());

		assertThat(results).allSatisfy(result -> {
			Recipe recipe = ((Result.Assigned) result).recipe();
			assertThat(recipe.proteinType()).isIn(ProteinType.EGGS, ProteinType.LEGUMES);
		});
	}

	@Test
	void leavesTheSlotEmptyAndExplainsWhenNothingFits() {
		Diner vegan = diner(5, "Marta", DinerLocation.TAKEAWAY, DietType.VEGAN, Set.of());
		List<Recipe> catalogue = List.of(
				recipe(10, "Peix al forn", ProteinType.FISH, false, FISH),
				recipe(11, "Truita", ProteinType.EGGS, true, EGG));

		List<Result> results = new MenuGenerator(catalogue, new Random(1))
				.generate(List.of(slot(1, MONDAY, 0, List.of(vegan))), List.of());

		assertThat(results).singleElement().isInstanceOf(Result.Empty.class);
		Map<AssignmentViolation, Long> reasons = ((Result.Empty) results.getFirst()).eliminatedBy();
		assertThat(reasons).containsEntry(new AssignmentViolation.IncompatibleDiet("Marta", DietType.VEGAN), 2L)
				.containsEntry(new AssignmentViolation.NotPortable(1), 1L);
	}

	@Test
	void keepsLockedMealsAndVariesAroundThem() {
		List<Recipe> catalogue = mixedCatalogue();
		Recipe locked = catalogue.getFirst();
		List<Slot> week = new ArrayList<>(fullWeek(List.of(ANNA_HOME), List.of(ANNA_HOME)));
		Slot wednesdayLunch = week.get(4);
		week.set(4, new Slot(wednesdayLunch.mealId(), wednesdayLunch.date(), 0, wednesdayLunch.diners(), locked));

		List<Result> results = new MenuGenerator(catalogue, new Random(3)).generate(week, List.of());

		Result kept = results.stream().filter(r -> r.mealId() == wednesdayLunch.mealId()).findFirst().orElseThrow();
		assertThat(kept).isEqualTo(new Result.Assigned(wednesdayLunch.mealId(), locked, true, 0));
		long sameDish = results.stream()
				.filter(r -> r instanceof Result.Assigned a && a.recipe().equals(locked))
				.count();
		assertThat(sameDish).as("the locked dish is not repeated elsewhere").isEqualTo(1);
	}

	@Test
	void doesNotRepeatADishInTheSameWeekWhenTheCatalogueAllows() {
		List<Slot> week = fullWeek(List.of(ANNA_HOME, PAU_HOME), List.of(ANNA_HOME, PAU_HOME));

		for (int seed = 0; seed < 20; seed++) {
			List<Result> results = new MenuGenerator(mixedCatalogue(), new Random(seed)).generate(week, List.of());
			Set<Long> distinct = new HashSet<>();
			results.forEach(r -> distinct.add(((Result.Assigned) r).recipe().id()));
			assertThat(distinct).as("seed %d", seed).hasSize(14);
		}
	}

	@Test
	void rotatesProteinsBetweenConsecutiveMeals() {
		List<Slot> week = fullWeek(List.of(ANNA_HOME), List.of(ANNA_HOME));
		int transitions = 0;
		int repeats = 0;

		for (int seed = 0; seed < 50; seed++) {
			List<Result> results = new MenuGenerator(mixedCatalogue(), new Random(seed)).generate(week, List.of());
			for (int i = 1; i < results.size(); i++) {
				transitions++;
				if (((Result.Assigned) results.get(i - 1)).recipe().proteinType()
						== ((Result.Assigned) results.get(i)).recipe().proteinType()) {
					repeats++;
				}
			}
		}

		// Rotation is a score, not a hard rule. With four proteins, picking blindly would repeat
		// about 25 % of the time; the scoring should make it rare.
		assertThat((double) repeats / transitions).isLessThan(0.08);
	}

	@Test
	void prefersDishesNotPlannedRecently() {
		List<Recipe> catalogue = mixedCatalogue();
		Recipe recent = catalogue.getFirst();
		MenuGenerator generator = new MenuGenerator(catalogue, new Random(0));
		Slot slot = slot(1, MONDAY, 0, List.of(ANNA_HOME));

		var week = new MenuGenerator.Week(List.of(new PastUse(recent.id(), MONDAY.minusDays(7))));
		assertThat(generator.recency(recent, slot, week)).isCloseTo(0.25, offset());
		assertThat(generator.recency(catalogue.get(1), slot, week)).isEqualTo(1.0);
	}

	@Test
	void penalisesDislikesWithoutExcludingTheDish() {
		Recipe chicken = recipe(20, "Pollastre", ProteinType.POULTRY, true, CHICKEN);
		Diner dislikesChicken = new Diner(9, "Nil", DinerLocation.HOME, DietType.OMNIVORE, Set.of(), Set.of(CHICKEN.id()));
		MenuGenerator generator = new MenuGenerator(List.of(chicken), new Random(0));
		var week = new MenuGenerator.Week(List.of());

		double liked = generator.score(chicken, slot(1, MONDAY, 0, List.of(ANNA_HOME)), week);
		double disliked = generator.score(chicken, slot(1, MONDAY, 0, List.of(dislikesChicken)), week);

		assertThat(liked - disliked).isCloseTo(MenuGenerator.DISLIKE_PENALTY, offset());
		assertThat(new MenuGenerator(List.of(chicken), new Random(0))
				.generate(List.of(slot(1, MONDAY, 0, List.of(dislikesChicken))), List.of()))
				.singleElement().isInstanceOf(Result.Assigned.class);
	}

	@Test
	void sameSeedSameWeekDifferentSeedsDifferentWeeks() {
		List<Slot> week = fullWeek(List.of(ANNA_HOME, PAU_HOME), List.of(ANNA_HOME, PAU_HOME));
		List<Recipe> catalogue = mixedCatalogue();

		assertThat(dishes(new MenuGenerator(catalogue, new Random(42)).generate(week, List.of())))
				.isEqualTo(dishes(new MenuGenerator(catalogue, new Random(42)).generate(week, List.of())));
		Set<List<Long>> menus = new HashSet<>();
		for (int seed = 0; seed < 10; seed++) {
			menus.add(dishes(new MenuGenerator(catalogue, new Random(seed)).generate(week, List.of())));
		}
		assertThat(menus).hasSizeGreaterThan(1);
	}

	// ------------------------------------------------------------------ fixtures

	/** 24 recipes over four proteins, half portable. */
	private static List<Recipe> mixedCatalogue() {
		List<Recipe> recipes = new ArrayList<>();
		Ingredient[] proteins = {FISH, CHICKEN, EGG, LENTIL};
		ProteinType[] types = {ProteinType.FISH, ProteinType.POULTRY, ProteinType.EGGS, ProteinType.LEGUMES};
		for (int i = 0; i < 24; i++) {
			int p = i % 4;
			recipes.add(recipe(100 + i, "Plat " + i, types[p], i % 2 == 0 || p >= 2, proteins[p]));
		}
		return recipes;
	}

	private static List<Slot> fullWeek(List<Diner> lunchDiners, List<Diner> dinnerDiners) {
		List<Slot> slots = new ArrayList<>();
		for (int day = 0; day < 7; day++) {
			slots.add(slot(day * 2L + 1, MONDAY.plusDays(day), 0, lunchDiners));
			slots.add(slot(day * 2L + 2, MONDAY.plusDays(day), 1, dinnerDiners));
		}
		return slots;
	}

	private static Slot slot(long id, LocalDate date, int order, List<Diner> diners) {
		return new Slot(id, date, order, diners, null);
	}

	private static Diner diner(long id, String name, DinerLocation location, DietType diet, Set<Allergen> allergens) {
		return new Diner(id, name, location, diet, allergens, Set.of());
	}

	private static Ingredient ingredient(long id, String name, IngredientCategory category, Set<Allergen> allergens) {
		return new Ingredient(id, name, category, CanonicalUnit.G, BigDecimal.valueOf(1000), Aisle.PANTRY,
				allergens, Map.of(), false);
	}

	/** A recipe for 1 serving with its protein as the only ingredient. */
	private static Recipe recipe(long id, String name, ProteinType type, boolean portable, Ingredient protein) {
		return new Recipe(id, name, 1, 30, type, Cuisine.values()[(int) (id % Cuisine.values().length)],
				Set.of(Season.AUTUMN), portable, portable ? LunchboxServing.REHEAT : null,
				List.of(new RecipeIngredient(protein, BigDecimal.valueOf(150), Unit.G, ScalingMode.LINEAR)));
	}

	private static List<Long> dishes(List<Result> results) {
		return results.stream().map(r -> ((Result.Assigned) r).recipe().id()).toList();
	}

	private static org.assertj.core.data.Offset<Double> offset() {
		return org.assertj.core.data.Offset.offset(1e-9);
	}
}
