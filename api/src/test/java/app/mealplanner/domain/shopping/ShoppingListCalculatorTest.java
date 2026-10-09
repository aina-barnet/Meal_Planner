package app.mealplanner.domain.shopping;

import static app.mealplanner.domain.Fixtures.EGG;
import static app.mealplanner.domain.Fixtures.MILK;
import static app.mealplanner.domain.Fixtures.OLIVE_OIL;
import static app.mealplanner.domain.Fixtures.ONION;
import static app.mealplanner.domain.Fixtures.POTATO;
import static app.mealplanner.domain.Fixtures.dec;
import static app.mealplanner.domain.Fixtures.line;
import static app.mealplanner.domain.Fixtures.recipe;
import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.domain.Fixtures;
import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Unit;
import app.mealplanner.domain.planning.MealServings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShoppingListCalculatorTest {

	/** Crema for 2: 500 ml milk, 1 egg, 400 g potato. */
	private final Recipe crema = recipe(20, "Crema", 2, List.of(
			line(MILK, "0.5", Unit.L, ScalingMode.LINEAR),
			line(EGG, "1", Unit.UNIT, ScalingMode.LINEAR),
			line(POTATO, "400", Unit.G, ScalingMode.LINEAR)));

	@Test
	void sumsTheSameIngredientAcrossMealsInCanonicalUnits() {
		// truita for 4 (eggs 360 g) + crema for 4 (factor 2: eggs 120 g) = 480 g of eggs
		var meals = List.of(
				new PlannedMeal(Fixtures.truita(), new MealServings(2, 2, 0)),
				new PlannedMeal(crema, new MealServings(4, 0, 0)));

		ShoppingList list = ShoppingListCalculator.calculate(meals, Map.of());

		assertThat(item(list, EGG.id()).required()).isEqualByComparingTo("480");
		// potato 600 + 800 = 1400 g; milk 1000 ml
		assertThat(item(list, POTATO.id()).required()).isEqualByComparingTo("1400");
		assertThat(item(list, MILK.id()).required()).isEqualByComparingTo("1000");
	}

	@Test
	void extraServingsAreCookedAndBought() {
		// truita for 4 + 2 extra = 6 servings: 9 eggs = 540 g
		var meals = List.of(new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 2)));

		assertThat(item(ShoppingListCalculator.calculate(meals, Map.of()), EGG.id()).required())
				.isEqualByComparingTo("540");
	}

	@Test
	void roundsUpToWholePackagesAndRecordsTheLeftover() {
		// 480 g of eggs, a dozen is 720 g: buy 1, 240 g left over
		var meals = List.of(
				new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)),
				new PlannedMeal(crema, new MealServings(4, 0, 0)));

		ShoppingListItem eggs = item(ShoppingListCalculator.calculate(meals, Map.of()), EGG.id());

		assertThat(eggs.packages()).isEqualTo(1);
		assertThat(eggs.purchased()).isEqualByComparingTo("720");
		assertThat(eggs.leftover()).isEqualByComparingTo("240");
	}

	@Test
	void anExactMultipleOfThePackageNeedsNoExtraPackage() {
		// crema for 8: 2000 ml of milk = exactly 2 cartons
		var meals = List.of(new PlannedMeal(crema, new MealServings(8, 0, 0)));

		ShoppingListItem milk = item(ShoppingListCalculator.calculate(meals, Map.of()), MILK.id());

		assertThat(milk.packages()).isEqualTo(2);
		assertThat(milk.leftover()).isEqualByComparingTo("0");
	}

	@Test
	void fractionalServingsDoNotTipIntoAnExtraPackage() {
		// base 3, cooked for 2 three times: 3 × 2/3 × 1500 = 3000 g, exactly 1.5 bags of 2000 g → 2.
		// Repeating decimals must not push it over a package boundary.
		Recipe thirds = recipe(30, "Terços", 3, List.of(line(POTATO, "1500", Unit.G, ScalingMode.LINEAR)));
		var meal = new PlannedMeal(thirds, new MealServings(2, 0, 0));

		ShoppingListItem potato = item(ShoppingListCalculator.calculate(List.of(meal, meal, meal), Map.of()),
				POTATO.id());

		assertThat(potato.required()).isEqualByComparingTo("3000");
		assertThat(potato.packages()).isEqualTo(2);

		// base 3 cooked for 2, once, with a 1000 g pack: 1000 g exactly → 1 package, not 2
		Recipe onionThirds = recipe(31, "Ceba", 3, List.of(line(ONION, "1500", Unit.G, ScalingMode.LINEAR)));
		ShoppingListItem onion = item(ShoppingListCalculator.calculate(
				List.of(new PlannedMeal(onionThirds, new MealServings(2, 0, 0))), Map.of()), ONION.id());
		assertThat(onion.packages()).isEqualTo(1);
	}

	@Test
	void subtractsPantryBeforeRounding() {
		// 480 g of eggs needed, 300 g at home: buy 180 g → 1 dozen, 540 g left over
		var meals = List.of(
				new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)),
				new PlannedMeal(crema, new MealServings(4, 0, 0)));

		ShoppingListItem eggs = item(ShoppingListCalculator.calculate(meals, Map.of(EGG.id(), dec("300"))),
				EGG.id());

		assertThat(eggs.fromPantry()).isEqualByComparingTo("300");
		assertThat(eggs.toBuy()).isEqualByComparingTo("180");
		assertThat(eggs.packages()).isEqualTo(1);
		assertThat(eggs.leftover()).isEqualByComparingTo("540");
	}

	@Test
	void showsItemsFullyCoveredByThePantryWithNothingToBuy() {
		var meals = List.of(new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)));

		ShoppingListItem eggs = item(ShoppingListCalculator.calculate(meals, Map.of(EGG.id(), dec("500"))),
				EGG.id());

		assertThat(eggs.coveredByPantry()).isTrue();
		assertThat(eggs.fromPantry()).isEqualByComparingTo("360");
		assertThat(eggs.toBuy()).isEqualByComparingTo("0");
		assertThat(eggs.packages()).isZero();
	}

	@Test
	void carriesPantryRemaindersAndPackageLeftoversToTheNextWeek() {
		long unrelated = 99;
		var meals = List.of(new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)));
		Map<Long, BigDecimal> pantry = Map.of(
				EGG.id(), dec("500"),        // needs 360 → 140 remain
				POTATO.id(), dec("100"),     // needs 600 → buys 500 of a 2000 bag → 1500 left over
				unrelated, dec("250"));      // untouched

		Map<Long, BigDecimal> after = ShoppingListCalculator.calculate(meals, pantry).pantryAfter();

		assertThat(after.get(EGG.id())).isEqualByComparingTo("140");
		assertThat(after.get(POTATO.id())).isEqualByComparingTo("1500");
		assertThat(after.get(ONION.id())).isEqualByComparingTo("850");
		assertThat(after.get(unrelated)).isEqualByComparingTo("250");
	}

	@Test
	void removesIngredientsThatRunOutFromThePantry() {
		// crema for 4 uses exactly the 1000 ml at home
		var meals = List.of(new PlannedMeal(crema, new MealServings(4, 0, 0)));

		Map<Long, BigDecimal> after = ShoppingListCalculator.calculate(meals, Map.of(MILK.id(), dec("1000")))
				.pantryAfter();

		assertThat(after).doesNotContainKey(MILK.id());
	}

	@Test
	void keepsPantryStaplesOffTheListAndReportsThemSeparately() {
		var meals = List.of(new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)));

		ShoppingList list = ShoppingListCalculator.calculate(meals, Map.of(OLIVE_OIL.id(), dec("100")));

		assertThat(list.items()).noneMatch(item -> item.ingredient().equals(OLIVE_OIL));
		assertThat(list.staplesUsed()).containsExactly(OLIVE_OIL);
		assertThat(list.pantryAfter().get(OLIVE_OIL.id())).isEqualByComparingTo("100");
	}

	@Test
	void groupsByAisleInWalkingOrderAndSortsItemsByName() {
		var meals = List.of(
				new PlannedMeal(Fixtures.truita(), new MealServings(4, 0, 0)),
				new PlannedMeal(crema, new MealServings(2, 0, 0)));

		ShoppingList list = ShoppingListCalculator.calculate(meals, Map.of());

		assertThat(list.byAisle().keySet()).containsExactly(Aisle.PRODUCE, Aisle.DAIRY_EGGS);
		assertThat(list.byAisle().get(Aisle.PRODUCE)).extracting(i -> i.ingredient().name())
				.containsExactly("Ceba", "Patata");
		assertThat(list.byAisle().get(Aisle.DAIRY_EGGS)).extracting(i -> i.ingredient().name())
				.containsExactly("Llet", "Ous");
	}

	@Test
	void anEmptyWeekNeedsNothing() {
		ShoppingList list = ShoppingListCalculator.calculate(List.of(), Map.of(EGG.id(), dec("60")));

		assertThat(list.items()).isEmpty();
		assertThat(list.pantryAfter()).containsEntry(EGG.id(), dec("60"));
	}

	private static ShoppingListItem item(ShoppingList list, long ingredientId) {
		return list.items().stream()
				.filter(item -> item.ingredient().id() == ingredientId)
				.findFirst()
				.orElseThrow();
	}
}
