package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An ingredient of the recipes.
 *
 * @param conversions canonical units per one ingredient-specific unit (1 egg = 60 g)
 */
public record Ingredient(
		long id,
		String name,
		IngredientCategory category,
		CanonicalUnit canonicalUnit,
		Set<Allergen> allergens,
		Map<Unit, BigDecimal> conversions) {

	public Ingredient {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(category, "category");
		Objects.requireNonNull(canonicalUnit, "canonicalUnit");
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
