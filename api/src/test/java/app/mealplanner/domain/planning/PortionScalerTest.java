package app.mealplanner.domain.planning;

import static app.mealplanner.domain.Fixtures.BAY_LEAF;
import static app.mealplanner.domain.Fixtures.EGG;
import static app.mealplanner.domain.Fixtures.OLIVE_OIL;
import static app.mealplanner.domain.Fixtures.line;
import static app.mealplanner.domain.Fixtures.recipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Unit;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortionScalerTest {

	/** For 4: 4 eggs (240 g) linear, 1 bay leaf (0.2 g) fixed, 4 tbsp oil (56 g) sublinear. */
	private final Recipe recipe = recipe(1, "Prova", 4, List.of(
			line(EGG, "4", Unit.UNIT, ScalingMode.LINEAR),
			line(BAY_LEAF, "1", Unit.UNIT, ScalingMode.FIXED),
			line(OLIVE_OIL, "4", Unit.TBSP, ScalingMode.SUBLINEAR)));

	@Test
	void totalServingsAddHomeTakeawayAndExtra() {
		var servings = new MealServings(2, 2, 1);

		assertThat(servings.total()).isEqualTo(5);
		assertThat(servings.requiresPortable()).isTrue();
		assertThat(new MealServings(4, 0, 2).requiresPortable()).isFalse();
	}

	@Test
	void factorIsServingsOverBaseServings() {
		assertThat(PortionScaler.factor(recipe, 6)).isEqualByComparingTo("1.5");
		assertThat(PortionScaler.factor(recipe, 1)).isEqualByComparingTo("0.25");
	}

	@Test
	void baseServingsLeaveEveryLineUnchanged() {
		assertThat(quantities(PortionScaler.scale(recipe, 4))).containsExactly("240", "0.2", "56");
	}

	@Test
	void doublingScalesLinearFullyFixedNotAndSublinearByHalf() {
		// factor 2: eggs 480, bay leaf 0.2, oil 56 × 1.5 = 84
		assertThat(quantities(PortionScaler.scale(recipe, 8))).containsExactly("480", "0.2", "84");
	}

	@Test
	void halvingScalesSublinearToThreeQuarters() {
		// factor 0.5: eggs 120, bay leaf 0.2, oil 56 × 0.75 = 42
		assertThat(quantities(PortionScaler.scale(recipe, 2))).containsExactly("120", "0.2", "42");
	}

	@Test
	void zeroServingsCookNothing() {
		assertThat(PortionScaler.scale(recipe, 0)).isEmpty();
	}

	@Test
	void rejectsNegativeServings() {
		assertThatThrownBy(() -> PortionScaler.scale(recipe, -1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MealServings(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	private static List<String> quantities(List<ScaledIngredient> scaled) {
		return scaled.stream()
				.map(ScaledIngredient::canonicalQuantity)
				.map(BigDecimal::stripTrailingZeros)
				.map(BigDecimal::toPlainString)
				.toList();
	}
}
