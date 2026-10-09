package app.mealplanner.domain.planning;

import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.domain.nutrition.EnergyRequirement;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealKcalTargetTest {

	@Test
	void sumsThePerMealTargetsOfTheAttendees() {
		var adult = new EnergyRequirement(40, 1600, 2400, 2400, false, 780, true);
		var child = new EnergyRequirement(9, 1100, 1700, 1700, false, 553, false);

		assertThat(MealKcalTarget.forAttendees(List.of(adult, adult, child))).isEqualTo(2113);
		assertThat(MealKcalTarget.forAttendees(List.of())).isZero();
	}
}
