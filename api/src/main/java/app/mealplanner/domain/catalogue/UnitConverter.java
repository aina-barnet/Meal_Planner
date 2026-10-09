package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;

/** Expresses a recipe quantity in the ingredient's canonical unit (g or ml). */
public final class UnitConverter {

	private UnitConverter() {
	}

	/** Whether a quantity of this ingredient can be written in this unit. */
	public static boolean supports(Ingredient ingredient, Unit unit) {
		return unit.isMetric() ? unit.metricBase() == ingredient.canonicalUnit() : ingredient.conversions().containsKey(unit);
	}

	public static BigDecimal toCanonical(Ingredient ingredient, BigDecimal quantity, Unit unit) {
		if (unit.isMetric()) {
			if (unit.metricBase() != ingredient.canonicalUnit()) {
				throw new UnconvertibleUnitException(ingredient, unit);
			}
			return quantity.multiply(unit.metricFactor());
		}
		BigDecimal factor = ingredient.conversions().get(unit);
		if (factor == null) {
			throw new UnconvertibleUnitException(ingredient, unit);
		}
		return quantity.multiply(factor);
	}
}
