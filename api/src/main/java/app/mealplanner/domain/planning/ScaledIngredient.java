package app.mealplanner.domain.planning;

import app.mealplanner.domain.catalogue.Ingredient;
import java.math.BigDecimal;

/** An ingredient quantity for a specific number of servings, in the ingredient's canonical unit. */
public record ScaledIngredient(Ingredient ingredient, BigDecimal canonicalQuantity) {
}
