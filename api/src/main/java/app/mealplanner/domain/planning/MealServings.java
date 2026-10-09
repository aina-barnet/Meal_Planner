package app.mealplanner.domain.planning;

/**
 * How many servings of a meal to cook.
 *
 * @param extraServings cooked deliberately on top, to eat another day
 */
public record MealServings(int dinersAtHome, int dinersTakeaway, int extraServings) {

	public MealServings {
		if (dinersAtHome < 0 || dinersTakeaway < 0 || extraServings < 0) {
			throw new IllegalArgumentException("Servings cannot be negative");
		}
	}

	public int total() {
		return dinersAtHome + dinersTakeaway + extraServings;
	}

	/** A meal with lunchbox diners may only be assigned a portable recipe. */
	public boolean requiresPortable() {
		return dinersTakeaway > 0;
	}
}
