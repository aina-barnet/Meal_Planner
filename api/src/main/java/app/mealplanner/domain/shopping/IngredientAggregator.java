package app.mealplanner.domain.shopping;

import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.planning.PortionScaler;
import app.mealplanner.domain.planning.ScaledIngredient;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scales every planned meal and sums the same ingredient across all of them, in canonical units.
 * The result is keyed by ingredient id, in first-seen order.
 */
public final class IngredientAggregator {

	private IngredientAggregator() {
	}

	public static Map<Long, Total> aggregate(Collection<PlannedMeal> meals) {
		Map<Long, Total> totals = new LinkedHashMap<>();
		for (PlannedMeal meal : meals) {
			for (ScaledIngredient scaled : PortionScaler.scale(meal.recipe(), meal.servings().total())) {
				totals.merge(scaled.ingredient().id(), new Total(scaled.ingredient(), scaled.canonicalQuantity()),
						Total::plus);
			}
		}
		return totals;
	}

	public record Total(Ingredient ingredient, BigDecimal quantity) {

		Total plus(Total other) {
			return new Total(ingredient, quantity.add(other.quantity));
		}
	}
}
