package app.mealplanner.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EnergyRequirementCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	@Test
	void appliesMifflinStJeorForMen() {
		// 30 years, 80 kg, 180 cm: 800 + 1125 − 150 + 5 = 1780; × 1.55 = 2759
		var member = new MemberProfile(LocalDate.of(1996, 1, 15), Sex.MALE, 80, 180, ActivityLevel.MODERATE, null,
				65);

		EnergyRequirement result = EnergyRequirementCalculator.calculate(member, TODAY);

		assertThat(result.ageYears()).isEqualTo(30);
		assertThat(result.basalKcal()).isEqualTo(1780);
		assertThat(result.estimatedDailyKcal()).isEqualTo(2759);
		assertThat(result.dailyKcal()).isEqualTo(2759);
		assertThat(result.overridden()).isFalse();
	}

	@Test
	void appliesMifflinStJeorForWomen() {
		// 40 years, 60 kg, 165 cm: 600 + 1031.25 − 200 − 161 = 1270.25; × 1.2 = 1524.3
		var member = new MemberProfile(LocalDate.of(1986, 3, 1), Sex.FEMALE, 60, 165, ActivityLevel.SEDENTARY, null,
				65);

		EnergyRequirement result = EnergyRequirementCalculator.calculate(member, TODAY);

		assertThat(result.basalKcal()).isEqualTo(1270);
		assertThat(result.estimatedDailyKcal()).isEqualTo(1524);
	}

	@Test
	void ageChangesOnTheBirthdayNotBefore() {
		var dayBefore = new MemberProfile(LocalDate.of(1996, 10, 9), Sex.MALE, 80, 180, ActivityLevel.MODERATE, null,
				65);
		var onBirthday = new MemberProfile(LocalDate.of(1996, 10, 8), Sex.MALE, 80, 180, ActivityLevel.MODERATE, null,
				65);

		assertThat(EnergyRequirementCalculator.calculate(dayBefore, TODAY).ageYears()).isEqualTo(29);
		assertThat(EnergyRequirementCalculator.calculate(onBirthday, TODAY).ageYears()).isEqualTo(30);
	}

	@Test
	void manualOverrideReplacesTheEstimateButKeepsItVisible() {
		var member = new MemberProfile(LocalDate.of(1996, 1, 15), Sex.MALE, 80, 180, ActivityLevel.MODERATE, 2200,
				65);

		EnergyRequirement result = EnergyRequirementCalculator.calculate(member, TODAY);

		assertThat(result.dailyKcal()).isEqualTo(2200);
		assertThat(result.estimatedDailyKcal()).isEqualTo(2759);
		assertThat(result.overridden()).isTrue();
	}

	@Test
	void splitsTheMainMealsShareEquallyBetweenLunchAndDinner() {
		// 2000 kcal, 70 % at lunch + dinner = 1400 → 700 per meal
		var member = new MemberProfile(LocalDate.of(1990, 1, 1), Sex.FEMALE, 60, 165, ActivityLevel.LIGHT, 2000, 70);

		assertThat(EnergyRequirementCalculator.calculate(member, TODAY).perMainMealKcal()).isEqualTo(700);
	}

	@Test
	void flagsMinorsBecauseTheEquationWasValidatedOnAdults() {
		var child = new MemberProfile(LocalDate.of(2016, 5, 1), Sex.FEMALE, 30, 135, ActivityLevel.ACTIVE, null, 65);
		var adult = new MemberProfile(LocalDate.of(2008, 10, 8), Sex.FEMALE, 55, 160, ActivityLevel.ACTIVE, null, 65);

		assertThat(EnergyRequirementCalculator.calculate(child, TODAY).equationValidForAge()).isFalse();
		assertThat(EnergyRequirementCalculator.calculate(adult, TODAY).equationValidForAge()).isTrue();
	}

	@Test
	void rejectsInvalidProfiles() {
		assertThatThrownBy(() -> new MemberProfile(LocalDate.of(1990, 1, 1), Sex.MALE, 0, 180, ActivityLevel.LIGHT,
				null, 65)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MemberProfile(LocalDate.of(1990, 1, 1), Sex.MALE, 80, 180, ActivityLevel.LIGHT,
				null, 5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MemberProfile(LocalDate.of(1990, 1, 1), Sex.MALE, 80, 180, ActivityLevel.LIGHT,
				0, 65)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsBirthDatesInTheFuture() {
		var unborn = new MemberProfile(LocalDate.of(2027, 1, 1), Sex.MALE, 3, 50, ActivityLevel.SEDENTARY, null, 65);

		assertThatThrownBy(() -> EnergyRequirementCalculator.calculate(unborn, TODAY))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
