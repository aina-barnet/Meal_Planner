package app.mealplanner.domain.shopping;

import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Ingredient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * The result of planning a week's shopping.
 *
 * @param byAisle       items grouped by aisle, aisles in walking order and items by name
 * @param staplesUsed   pantry staples (oil, spices) the week uses; assumed at home, never bought
 * @param pantryAfter   pantry quantities once the week is cooked: what was at home, minus what
 *                      the meals use, plus the leftovers of every rounded-up package. Keyed by
 *                      ingredient id; ingredients that run out are absent.
 */
public record ShoppingList(
		SortedMap<Aisle, List<ShoppingListItem>> byAisle,
		List<Ingredient> staplesUsed,
		Map<Long, BigDecimal> pantryAfter) {

	public List<ShoppingListItem> items() {
		return byAisle.values().stream().flatMap(List::stream).toList();
	}
}
