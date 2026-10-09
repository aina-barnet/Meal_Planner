package app.mealplanner.domain.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import app.mealplanner.domain.Fixtures;
import org.junit.jupiter.api.Test;

class NutritionCalculatorTest {

	@Test
	void computesNutritionPerServingFromTheIngredients() {
		// eggs 360 g × 1.43 = 514.8; potato 600 g × 0.77 = 462; onion 150 g × 0.40 = 60; oil 70 g × 8.84 = 618.8
		// total 1655.6 kcal / 4 servings = 413.9
		Nutrition perServing = NutritionCalculator.perServing(Fixtures.truita());

		assertThat(perServing.kcal()).isCloseTo(413.9, within(0.01));
		// protein: 45.36 + 12 + 1.65 + 0 = 59.01 / 4
		assertThat(perServing.proteinG()).isCloseTo(14.7525, within(0.001));
		// fat: 34.2 + 0.6 + 0.15 + 70 = 104.95 / 4
		assertThat(perServing.fatG()).isCloseTo(26.2375, within(0.001));
	}
}
