package app.mealplanner.domain.shopping;

import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Ingredient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Turns a week of planned meals into a shopping list:
 *
 * <ol>
 *   <li>scale every recipe to its servings and convert each line to canonical units;</li>
 *   <li>sum the same ingredient across all meals;</li>
 *   <li>set pantry staples (oil, spices) aside: they are assumed to be at home;</li>
 *   <li>subtract what is already in the pantry;</li>
 *   <li>round what is left up to whole purchase packages;</li>
 *   <li>group by supermarket aisle;</li>
 *   <li>carry the leftover of every rounded package into the pantry for the following week.</li>
 * </ol>
 */
public final class ShoppingListCalculator {

	/** Quantities are kept to the hundredth of a gram or millilitre. */
	private static final int SCALE = 2;
	private static final Comparator<ShoppingListItem> BY_NAME = Comparator.comparing(
			item -> item.ingredient().name(), Collator.getInstance(Locale.forLanguageTag("ca")));

	private ShoppingListCalculator() {
	}

	/** @param pantry quantities at home, in canonical units, keyed by ingredient id */
	public static ShoppingList calculate(Collection<PlannedMeal> meals, Map<Long, BigDecimal> pantry) {
		Map<Long, BigDecimal> pantryAfter = new HashMap<>(pantry);
		SortedMap<Aisle, List<ShoppingListItem>> byAisle = new TreeMap<>();
		List<Ingredient> staples = new ArrayList<>();

		for (IngredientAggregator.Total total : IngredientAggregator.aggregate(meals).values()) {
			Ingredient ingredient = total.ingredient();
			if (ingredient.pantryStaple()) {
				staples.add(ingredient);
				continue;
			}
			BigDecimal atHome = pantry.getOrDefault(ingredient.id(), BigDecimal.ZERO);
			ShoppingListItem item = itemFor(ingredient, total.quantity().setScale(SCALE, RoundingMode.HALF_UP), atHome);
			byAisle.computeIfAbsent(ingredient.aisle(), aisle -> new ArrayList<>()).add(item);

			BigDecimal remaining = atHome.subtract(item.fromPantry()).add(item.leftover());
			if (remaining.signum() > 0) {
				pantryAfter.put(ingredient.id(), remaining);
			} else {
				pantryAfter.remove(ingredient.id());
			}
		}

		byAisle.values().forEach(items -> items.sort(BY_NAME));
		staples.sort(Comparator.comparing(Ingredient::name, Collator.getInstance(Locale.forLanguageTag("ca"))));
		return new ShoppingList(Collections.unmodifiableSortedMap(byAisle), List.copyOf(staples), Map.copyOf(pantryAfter));
	}

	static ShoppingListItem itemFor(Ingredient ingredient, BigDecimal required, BigDecimal atHome) {
		BigDecimal fromPantry = required.min(atHome.max(BigDecimal.ZERO)).setScale(SCALE, RoundingMode.HALF_UP);
		BigDecimal toBuy = required.subtract(fromPantry);
		int packages = toBuy.divide(ingredient.packageSize(), 0, RoundingMode.CEILING).intValueExact();
		BigDecimal purchased = ingredient.packageSize().multiply(BigDecimal.valueOf(packages)).setScale(SCALE,
				RoundingMode.HALF_UP);
		return new ShoppingListItem(ingredient, required, fromPantry, toBuy, packages, purchased,
				purchased.subtract(toBuy));
	}
}
