package app.mealplanner.domain.nutrition;

/**
 * An orientative estimate of a member's daily energy needs. It is not medical advice.
 *
 * @param ageYears              age on the reference date
 * @param basalKcal             basal metabolic rate (Mifflin-St Jeor)
 * @param estimatedDailyKcal    basal rate times the activity factor
 * @param dailyKcal             the figure the planner uses: the manual override if set, else the estimate
 * @param overridden            whether {@code dailyKcal} comes from a manual override
 * @param perMainMealKcal       target for one planned meal (lunch or dinner)
 * @param equationValidForAge   false for under-18s: the equation was validated on adults, so the
 *                              estimate is even rougher and an override is recommended
 */
public record EnergyRequirement(
		int ageYears,
		int basalKcal,
		int estimatedDailyKcal,
		int dailyKcal,
		boolean overridden,
		int perMainMealKcal,
		boolean equationValidForAge) {
}
