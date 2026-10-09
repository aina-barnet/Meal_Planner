package app.mealplanner.domain.planning;

import app.mealplanner.domain.nutrition.EnergyRequirement;
import java.util.Collection;
import java.util.OptionalDouble;

/**
 * The aggregate kcal a meal should provide: the sum of the per-meal targets of the members who
 * eat it, at home or from a lunchbox. Extra servings cooked for another day do not count.
 */
public final class MealKcalTarget {

	private MealKcalTarget() {
	}

	public static int forAttendees(Collection<EnergyRequirement> attendees) {
		return attendees.stream().mapToInt(EnergyRequirement::perMainMealKcal).sum();
	}

	/**
	 * How well a recipe covers the target: the kcal the diners get (one serving each) over the
	 * target. 1.0 is a perfect fit. Empty when there is no target to compare against.
	 */
	public static OptionalDouble fit(int target, double kcalPerServing, int diners) {
		if (target <= 0 || diners <= 0) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(kcalPerServing * diners / target);
	}
}
