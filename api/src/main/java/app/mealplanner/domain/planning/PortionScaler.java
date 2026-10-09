package app.mealplanner.domain.planning;

import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeIngredient;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.UnitConverter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Scales a recipe to a number of servings and expresses every line in canonical units.
 *
 * <p>The scaling factor is {@code servings / baseServings}, applied per line by its mode:
 * <ul>
 *   <li>LINEAR: quantity × factor</li>
 *   <li>FIXED: quantity, whatever the servings</li>
 *   <li>SUBLINEAR: quantity × (1 + (factor − 1) / 2), i.e. half the rate. Doubling the servings
 *       gives 1.5× and halving them gives 0.75×.</li>
 * </ul>
 */
public final class PortionScaler {

	private static final MathContext PRECISION = MathContext.DECIMAL64;
	private static final BigDecimal HALF = new BigDecimal("0.5");

	private PortionScaler() {
	}

	public static BigDecimal factor(Recipe recipe, int servings) {
		return BigDecimal.valueOf(servings).divide(BigDecimal.valueOf(recipe.baseServings()), PRECISION);
	}

	/** Returns nothing for zero servings: nobody eats, nothing is cooked. */
	public static List<ScaledIngredient> scale(Recipe recipe, int servings) {
		if (servings < 0) {
			throw new IllegalArgumentException("Servings cannot be negative");
		}
		if (servings == 0) {
			return List.of();
		}
		BigDecimal factor = factor(recipe, servings);
		return recipe.ingredients().stream()
				.map(line -> new ScaledIngredient(line.ingredient(), scaleLine(line, factor)))
				.toList();
	}

	private static BigDecimal scaleLine(RecipeIngredient line, BigDecimal factor) {
		BigDecimal base = UnitConverter.toCanonical(line.ingredient(), line.quantity(), line.unit());
		return base.multiply(effectiveFactor(line.scalingMode(), factor), PRECISION);
	}

	static BigDecimal effectiveFactor(ScalingMode mode, BigDecimal factor) {
		return switch (mode) {
			case LINEAR -> factor;
			case FIXED -> BigDecimal.ONE;
			case SUBLINEAR -> BigDecimal.ONE.add(factor.subtract(BigDecimal.ONE).multiply(HALF));
		};
	}
}
