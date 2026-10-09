package app.mealplanner.domain.nutrition;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The data about a household member needed to estimate their energy needs.
 *
 * @param kcalOverride manual daily kcal set by the user; replaces the estimate when present
 * @param mainMealsPct share of the daily kcal eaten at lunch and dinner together, 10–100.
 *                     Breakfast and snacks are personal and outside the plan.
 */
public record MemberProfile(
		LocalDate birthDate,
		Sex sex,
		double weightKg,
		double heightCm,
		ActivityLevel activityLevel,
		Integer kcalOverride,
		int mainMealsPct) {

	public static final int DEFAULT_MAIN_MEALS_PCT = 65;

	public MemberProfile {
		Objects.requireNonNull(birthDate, "birthDate");
		Objects.requireNonNull(sex, "sex");
		Objects.requireNonNull(activityLevel, "activityLevel");
		if (weightKg <= 0 || heightCm <= 0) {
			throw new IllegalArgumentException("Weight and height must be positive");
		}
		if (kcalOverride != null && kcalOverride <= 0) {
			throw new IllegalArgumentException("A kcal override must be positive");
		}
		if (mainMealsPct < 10 || mainMealsPct > 100) {
			throw new IllegalArgumentException("Main meals share must be between 10 and 100");
		}
	}
}
