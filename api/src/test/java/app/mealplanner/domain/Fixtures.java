package app.mealplanner.domain;

import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.Unit;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Small, hand-checkable catalogue for domain tests. */
public final class Fixtures {

	public static final Ingredient EGG = new Ingredient(1, "Ous", IngredientCategory.EGG, CanonicalUnit.G, Set.of(),
			Map.of(Unit.UNIT, dec("60")));

	public static final Ingredient POTATO = new Ingredient(2, "Patata", IngredientCategory.VEGETABLE, CanonicalUnit.G,
			Set.of(), Map.of(Unit.UNIT, dec("200")));

	public static final Ingredient OLIVE_OIL = new Ingredient(3, "Oli d'oliva", IngredientCategory.OIL, CanonicalUnit.G,
			Set.of(), Map.of(Unit.TBSP, dec("14")));

	public static final Ingredient MILK = new Ingredient(4, "Llet", IngredientCategory.DAIRY, CanonicalUnit.ML, Set.of(),
			Map.of());

	private Fixtures() {
	}

	public static BigDecimal dec(String value) {
		return new BigDecimal(value);
	}
}
