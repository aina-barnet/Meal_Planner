package app.mealplanner.web;

import app.mealplanner.application.CatalogueService;
import app.mealplanner.application.MenuGenerationService;
import app.mealplanner.application.ShoppingListService;
import app.mealplanner.application.WeekPlanService;
import app.mealplanner.application.WeekPlanService.MealConfig;
import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.persistence.ShoppingListEntity;
import app.mealplanner.persistence.ShoppingListItemEntity;
import app.mealplanner.web.Dtos.AisleResponse;
import app.mealplanner.web.Dtos.AssignRecipeRequest;
import app.mealplanner.web.Dtos.AttendeeDto;
import app.mealplanner.web.Dtos.CheckItemRequest;
import app.mealplanner.web.Dtos.ConfigureWeekRequest;
import app.mealplanner.web.Dtos.CopyWeekRequest;
import app.mealplanner.web.Dtos.CreateWeekRequest;
import app.mealplanner.web.Dtos.RecipeSummary;
import app.mealplanner.web.Dtos.ShoppingItemResponse;
import app.mealplanner.web.Dtos.ShoppingListResponse;
import app.mealplanner.web.Dtos.SwapRequest;
import app.mealplanner.web.Dtos.UpdateMealRequest;
import app.mealplanner.web.Dtos.WeekResponse;
import app.mealplanner.web.Dtos.WeekSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weeks")
@Tag(name = "Weeks", description = "Week plans, meals, confirmation and the shopping list")
public class WeekPlanController {

	private static final Comparator<String> CATALAN = Collator.getInstance(Locale.forLanguageTag("ca"))::compare;

	private final WeekPlanService weeks;
	private final ShoppingListService shoppingLists;
	private final CatalogueService catalogue;
	private final MenuGenerationService generator;

	public WeekPlanController(WeekPlanService weeks, ShoppingListService shoppingLists, CatalogueService catalogue,
			MenuGenerationService generator) {
		this.weeks = weeks;
		this.shoppingLists = shoppingLists;
		this.catalogue = catalogue;
		this.generator = generator;
	}

	@GetMapping
	public List<WeekSummary> list() {
		return weeks.list().stream().map(WeekSummary::of).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a draft week",
			description = "weekStart must be a Monday. Every lunch and dinner is pre-planned with all members at home.")
	public WeekResponse create(@Valid @RequestBody CreateWeekRequest request) {
		return WeekResponse.of(weeks.detail(weeks.create(request.weekStart()).getId()));
	}

	@GetMapping("/{weekId}")
	public WeekResponse get(@PathVariable long weekId) {
		return WeekResponse.of(weeks.detail(weekId));
	}

	@DeleteMapping("/{weekId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a draft week")
	public void delete(@PathVariable long weekId) {
		weeks.delete(weekId);
	}

	@PostMapping("/{weekId}/copy")
	@Operation(summary = "Copy this week's menu onto another week",
			description = "Same dish, diners and extra servings per day and slot. Creates the target week if needed; "
					+ "replaces the meals of a draft target; 409 WEEK_CONFIRMED for a confirmed target. Returns the "
					+ "target week.")
	public WeekResponse copy(@PathVariable long weekId, @Valid @RequestBody CopyWeekRequest request) {
		return WeekResponse.of(weeks.copy(weekId, request.targetWeekStart()));
	}

	@PutMapping("/{weekId}/meals")
	@Operation(summary = "Configure which meals to plan and who eats each one, where",
			description = "Replaces the set of meals. Meals that keep their day and slot keep their dish, unless the "
					+ "new diners make it invalid (then it is removed and emptyReason says why).")
	public WeekResponse configure(@PathVariable long weekId, @Valid @RequestBody ConfigureWeekRequest request) {
		List<MealConfig> configs = request.meals().stream()
				.map(m -> new MealConfig(m.date(), m.slot(), attendees(m.attendees()), m.extraServings()))
				.toList();
		return WeekResponse.of(weeks.configure(weekId, configs));
	}

	@GetMapping("/{weekId}/meals/{mealId}/allowed-recipes")
	@Operation(summary = "Recipes that satisfy the meal's hard constraints (lunchbox, allergies, diets)")
	public List<RecipeSummary> allowedRecipes(@PathVariable long weekId, @PathVariable long mealId) {
		return weeks.allowedRecipes(weekId, mealId).stream().map(RecipeSummary::of).toList();
	}

	@PutMapping("/{weekId}/meals/{mealId}/recipe")
	@Operation(summary = "Assign a dish to a meal by hand",
			description = "422 ASSIGNMENT_NOT_ALLOWED when the dish breaks a hard constraint, e.g. a non-portable "
					+ "dish for a meal with lunchbox diners.")
	public WeekResponse assign(@PathVariable long weekId, @PathVariable long mealId,
			@RequestBody AssignRecipeRequest request) {
		return WeekResponse.of(weeks.assignRecipe(weekId, mealId, request.recipeId()));
	}

	@PatchMapping("/{weekId}/meals/{mealId}")
	@Operation(summary = "Lock a meal or change its extra servings")
	public WeekResponse update(@PathVariable long weekId, @PathVariable long mealId,
			@Valid @RequestBody UpdateMealRequest request) {
		return WeekResponse.of(weeks.updateMeal(weekId, mealId, request.locked(), request.extraServings()));
	}

	@PostMapping("/{weekId}/swap")
	@Operation(summary = "Swap the dishes of two meals", description = "Both moves must satisfy the hard constraints.")
	public WeekResponse swap(@PathVariable long weekId, @Valid @RequestBody SwapRequest request) {
		return WeekResponse.of(weeks.swap(weekId, request.mealIdA(), request.mealIdB()));
	}

	@PostMapping("/{weekId}/generate")
	@Operation(summary = "Propose a balanced menu",
			description = "Fills every meal that is not locked, using rules plus scoring (protein rotation, variety, "
					+ "recency) with some randomness. Meals no recipe can satisfy are left empty with an "
					+ "emptyReason explaining which constraints ruled the catalogue out.")
	public WeekResponse generate(@PathVariable long weekId) {
		return WeekResponse.of(generator.generate(weekId));
	}

	@PostMapping("/{weekId}/confirm")
	@Operation(summary = "Confirm the week and generate its shopping list",
			description = "Every meal needs a dish. The pantry moves forward to what remains after the week, "
					+ "including leftovers of rounded-up packages.")
	public WeekResponse confirm(@PathVariable long weekId) {
		return WeekResponse.of(weeks.confirm(weekId));
	}

	@PostMapping("/{weekId}/reopen")
	@Operation(summary = "Take a confirmed week back to draft",
			description = "Discards the shopping list and undoes the confirmation's pantry movement (manual pantry "
					+ "edits made since are kept). 409 LATER_WEEK_CONFIRMED while a later week is confirmed.")
	public WeekResponse reopen(@PathVariable long weekId) {
		return WeekResponse.of(weeks.reopen(weekId));
	}

	@GetMapping("/{weekId}/shopping-list")
	@Operation(summary = "Shopping list of a confirmed week, grouped by aisle")
	public ShoppingListResponse shoppingList(@PathVariable long weekId) {
		return response(weekId, shoppingLists.forWeek(weekId));
	}

	@PatchMapping("/{weekId}/shopping-list/items/{itemId}")
	@Operation(summary = "Tick or untick a shopping list item")
	public ShoppingListResponse check(@PathVariable long weekId, @PathVariable long itemId,
			@Valid @RequestBody CheckItemRequest request) {
		return response(weekId, shoppingLists.check(weekId, itemId, request.checked()));
	}

	private ShoppingListResponse response(long weekId, ShoppingListEntity list) {
		Map<Aisle, List<ShoppingItemResponse>> byAisle = new TreeMap<>();
		for (ShoppingListItemEntity item : list.getItems()) {
			Ingredient ingredient = catalogue.ingredient(item.getIngredientId());
			byAisle.computeIfAbsent(ingredient.aisle(), a -> new java.util.ArrayList<>())
					.add(new ShoppingItemResponse(item.getId(), ingredient.id(), ingredient.name(),
							ingredient.canonicalUnit(), item.getRequiredQty(), item.getFromPantryQty(),
							item.getToBuyQty(), item.getPackages(), item.getPackageSize(), item.getLeftoverQty(),
							item.getPackages() == 0, item.isChecked()));
		}
		List<AisleResponse> aisles = byAisle.entrySet().stream()
				.map(e -> new AisleResponse(e.getKey(), e.getValue().stream()
						.sorted(Comparator.comparing(ShoppingItemResponse::name, CATALAN))
						.toList()))
				.toList();
		return new ShoppingListResponse(weekId, list.getGeneratedAt(), aisles, weeks.staplesUsed(weekId));
	}

	private static Map<Long, DinerLocation> attendees(List<AttendeeDto> attendees) {
		return attendees.stream().collect(Collectors.toMap(AttendeeDto::memberId, AttendeeDto::location,
				(a, b) -> b, LinkedHashMap::new));
	}
}
