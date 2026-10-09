package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;

/** Computes a recipe's nutrition per serving from its ingredient list. */
public final class NutritionCalculator {

	private NutritionCalculator() {
	}

	public static Nutrition perServing(Recipe recipe) {
		Nutrition total = Nutrition.ZERO;
		for (RecipeIngredient line : recipe.ingredients()) {
			BigDecimal canonical = UnitConverter.toCanonical(line.ingredient(), line.quantity(), line.unit());
			total = total.plus(line.ingredient().nutritionPer100().times(canonical.doubleValue() / 100.0));
		}
		return total.times(1.0 / recipe.baseServings());
	}
}
