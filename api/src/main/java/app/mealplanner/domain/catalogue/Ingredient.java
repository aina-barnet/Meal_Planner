package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A purchasable ingredient.
 *
 * @param packageSize     size of one supermarket package, in the canonical unit
 * @param conversions     canonical units per one ingredient-specific unit (1 egg = 60 g)
 * @param pantryStaple    always assumed to be at home (oil, spices); never on the shopping list
 */
public record Ingredient(
		long id,
		String name,
		IngredientCategory category,
		CanonicalUnit canonicalUnit,
		BigDecimal packageSize,
		Aisle aisle,
		Set<Allergen> allergens,
		Map<Unit, BigDecimal> conversions,
		boolean pantryStaple) {

	public Ingredient {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(canonicalUnit, "canonicalUnit");
		Objects.requireNonNull(aisle, "aisle");
		if (packageSize == null || packageSize.signum() <= 0) {
			throw new IllegalArgumentException("Package size must be positive for " + name);
		}
		allergens = Set.copyOf(allergens);
		conversions = Map.copyOf(conversions);
		conversions.forEach((unit, factor) -> {
			if (unit.isMetric()) {
				throw new IllegalArgumentException("Metric unit " + unit + " needs no conversion for " + name);
			}
			if (factor.signum() <= 0) {
				throw new IllegalArgumentException("Conversion factor must be positive for " + name);
			}
		});
	}
}
