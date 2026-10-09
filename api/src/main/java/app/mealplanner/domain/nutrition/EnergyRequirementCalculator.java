package app.mealplanner.domain.nutrition;

import java.time.LocalDate;
import java.time.Period;

/**
 * Estimates daily energy needs with the Mifflin-St Jeor equation:
 *
 * <pre>
 *   BMR = 10 × weight(kg) + 6.25 × height(cm) − 5 × age(years) + s
 *   s   = +5 for men, −161 for women
 *   daily kcal = BMR × activity factor
 * </pre>
 *
 * <p>The planned meals (lunch and dinner) together take {@code mainMealsPct} percent of the
 * daily figure, split equally between them.
 */
public final class EnergyRequirementCalculator {

	private static final int ADULT_AGE = 18;

	private EnergyRequirementCalculator() {
	}

	/** @param on the date the age is computed at; passed in so results are reproducible. */
	public static EnergyRequirement calculate(MemberProfile member, LocalDate on) {
		if (member.birthDate().isAfter(on)) {
			throw new IllegalArgumentException("Birth date is after the reference date");
		}
		int age = Period.between(member.birthDate(), on).getYears();
		double sexConstant = member.sex() == Sex.MALE ? 5 : -161;
		double bmr = 10 * member.weightKg() + 6.25 * member.heightCm() - 5 * age + sexConstant;
		int estimated = (int) Math.round(bmr * member.activityLevel().factor());
		boolean overridden = member.kcalOverride() != null;
		int daily = overridden ? member.kcalOverride() : estimated;
		int perMainMeal = (int) Math.round(daily * member.mainMealsPct() / 100.0 / 2);
		return new EnergyRequirement(age, (int) Math.round(bmr), estimated, daily, overridden, perMainMeal,
				age >= ADULT_AGE);
	}
}
