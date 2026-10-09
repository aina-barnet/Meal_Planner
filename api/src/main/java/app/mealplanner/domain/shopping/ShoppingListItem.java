package app.mealplanner.domain.shopping;

import app.mealplanner.domain.catalogue.Ingredient;
import java.math.BigDecimal;

/**
 * One ingredient on the shopping list. All quantities are in the ingredient's canonical unit.
 *
 * @param required   total needed by the week's meals
 * @param fromPantry part of {@code required} already at home
 * @param toBuy      {@code required − fromPantry}
 * @param packages   whole packages to buy; zero when the pantry covers everything
 * @param purchased  {@code packages × packageSize}
 * @param leftover   {@code purchased − toBuy}, kept in the pantry for the following week
 */
public record ShoppingListItem(
		Ingredient ingredient,
		BigDecimal required,
		BigDecimal fromPantry,
		BigDecimal toBuy,
		int packages,
		BigDecimal purchased,
		BigDecimal leftover) {

	public boolean coveredByPantry() {
		return packages == 0;
	}
}
