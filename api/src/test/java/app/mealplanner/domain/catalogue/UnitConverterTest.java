package app.mealplanner.domain.catalogue;

import static app.mealplanner.domain.Fixtures.EGG;
import static app.mealplanner.domain.Fixtures.MILK;
import static app.mealplanner.domain.Fixtures.OLIVE_OIL;
import static app.mealplanner.domain.Fixtures.POTATO;
import static app.mealplanner.domain.Fixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UnitConverterTest {

	@Test
	void convertsMetricUnitsWithinTheSameDimension() {
		assertThat(UnitConverter.toCanonical(POTATO, dec("1.5"), Unit.KG)).isEqualByComparingTo("1500");
		assertThat(UnitConverter.toCanonical(MILK, dec("1.2"), Unit.L)).isEqualByComparingTo("1200");
		assertThat(UnitConverter.toCanonical(MILK, dec("15"), Unit.CL)).isEqualByComparingTo("150");
		assertThat(UnitConverter.toCanonical(MILK, dec("250"), Unit.ML)).isEqualByComparingTo("250");
	}

	@Test
	void usesIngredientSpecificConversions() {
		assertThat(UnitConverter.toCanonical(EGG, dec("6"), Unit.UNIT)).isEqualByComparingTo("360");
		assertThat(UnitConverter.toCanonical(OLIVE_OIL, dec("3"), Unit.TBSP)).isEqualByComparingTo("42");
	}

	@Test
	void rejectsMetricUnitsOfTheWrongDimension() {
		assertThatThrownBy(() -> UnitConverter.toCanonical(MILK, dec("100"), Unit.G))
				.isInstanceOf(UnconvertibleUnitException.class)
				.hasMessageContaining("Llet");
		assertThatThrownBy(() -> UnitConverter.toCanonical(POTATO, dec("1"), Unit.L))
				.isInstanceOf(UnconvertibleUnitException.class);
	}

	@Test
	void rejectsUnitsTheIngredientHasNoConversionFor() {
		assertThatThrownBy(() -> UnitConverter.toCanonical(EGG, dec("1"), Unit.TBSP))
				.isInstanceOf(UnconvertibleUnitException.class);
	}
}
