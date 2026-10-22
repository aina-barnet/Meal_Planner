package app.mealplanner.domain;

import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.Cuisine;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.ProteinType;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeIngredient;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Season;
import app.mealplanner.domain.catalogue.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small, hand-checkable catalogue for domain tests. */
public final class Fixtures {

	public static final Ingredient EGG = new Ingredient(1, "Ous", IngredientCategory.EGG, CanonicalUnit.G,
			dec("720"), Aisle.DAIRY_EGGS, Set.of(),
			Map.of(Unit.UNIT, dec("60")), false);

	public static final Ingredient POTATO = new Ingredient(2, "Patata", IngredientCategory.VEGETABLE,
			CanonicalUnit.G, dec("2000"), Aisle.PRODUCE, Set.of(),
			Map.of(Unit.UNIT, dec("200")), false);

	public static final Ingredient OLIVE_OIL = new Ingredient(3, "Oli d'oliva", IngredientCategory.OIL,
			CanonicalUnit.G, dec("920"), Aisle.PANTRY, Set.of(),
			Map.of(Unit.TBSP, dec("14")), true);

	public static final Ingredient MILK = new Ingredient(4, "Llet", IngredientCategory.DAIRY, CanonicalUnit.ML,
			dec("1000"), Aisle.DAIRY_EGGS, Set.of(), Map.of(), false);

	public static final Ingredient BAY_LEAF = new Ingredient(5, "Llorer", IngredientCategory.SPICE,
			CanonicalUnit.G, dec("10"), Aisle.SPICES, Set.of(),
			Map.of(Unit.UNIT, dec("0.2")), true);

	public static final Ingredient ONION = new Ingredient(6, "Ceba", IngredientCategory.VEGETABLE,
			CanonicalUnit.G, dec("1000"), Aisle.PRODUCE, Set.of(),
			Map.of(Unit.UNIT, dec("150")), false);

	private Fixtures() {
	}

	public static BigDecimal dec(String value) {
		return new BigDecimal(value);
	}

	public static RecipeIngredient line(Ingredient ingredient, String quantity, Unit unit, ScalingMode mode) {
		return new RecipeIngredient(ingredient, dec(quantity), unit, mode);
	}

	/** Truita de patates for 4: 6 eggs (360 g), 600 g potato, 1 onion (150 g), 5 tbsp oil (70 g). */
	public static Recipe truita() {
		return recipe(10, "Truita de patates", 4, List.of(
				line(EGG, "6", Unit.UNIT, ScalingMode.LINEAR),
				line(POTATO, "600", Unit.G, ScalingMode.LINEAR),
				line(ONION, "1", Unit.UNIT, ScalingMode.LINEAR),
				line(OLIVE_OIL, "5", Unit.TBSP, ScalingMode.LINEAR)));
	}

	public static Recipe recipe(long id, String name, int baseServings, List<RecipeIngredient> lines) {
		return new Recipe(id, name, baseServings, 30, ProteinType.EGGS, Cuisine.SPANISH, Set.of(Season.SPRING),
				false, null, lines);
	}
}
