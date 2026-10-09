package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;
import java.util.Objects;

/** One line of a recipe, expressed for the recipe's base servings. */
public record RecipeIngredient(Ingredient ingredient, BigDecimal quantity, Unit unit, ScalingMode scalingMode) {

	public RecipeIngredient {
		Objects.requireNonNull(ingredient, "ingredient");
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(scalingMode, "scalingMode");
		if (quantity == null || quantity.signum() <= 0) {
			throw new IllegalArgumentException("Quantity must be positive for " + ingredient.name());
		}
	}
}
