package app.mealplanner.domain.shopping;

import static app.mealplanner.domain.Fixtures.EGG;
import static app.mealplanner.domain.Fixtures.MILK;
import static app.mealplanner.domain.Fixtures.ONION;
import static app.mealplanner.domain.Fixtures.POTATO;
import static app.mealplanner.domain.Fixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.domain.Fixtures;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.planning.MealServings;
import app.mealplanner.domain.shopping.PantryRollback.Movement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PantryRollbackTest {

	@Test
	void confirmingAndRollingBackRestoresThePantry() {
		Recipe truita = Fixtures.truita();
		Map<Long, BigDecimal> before = Map.of(EGG.id(), dec("100"), POTATO.id(), dec("2500"), MILK.id(), dec("300"));

		ShoppingList list = ShoppingListCalculator.calculate(
				List.of(new PlannedMeal(truita, new MealServings(4, 0, 2))), before);
		List<Movement> movements = list.items().stream()
				.map(item -> new Movement(item.ingredient().id(), item.fromPantry(), item.leftover()))
				.toList();

		Map<Long, BigDecimal> restored = PantryRollback.undo(list.pantryAfter(), movements);

		assertThat(restored).containsOnlyKeys(EGG.id(), POTATO.id(), MILK.id());
		assertThat(restored.get(EGG.id())).isEqualByComparingTo("100");
		assertThat(restored.get(POTATO.id())).isEqualByComparingTo("2500");
		assertThat(restored.get(MILK.id())).isEqualByComparingTo("300");
		assertThat(restored).doesNotContainKey(ONION.id());
	}

	@Test
	void keepsManualPantryChangesMadeAfterConfirming() {
		// Confirming took 100 g of eggs from home and left 360 g over; since then the family
		// bought 500 g of rice by hand.
		Map<Long, BigDecimal> now = Map.of(EGG.id(), dec("360"), 77L, dec("500"));

		Map<Long, BigDecimal> restored = PantryRollback.undo(now, List.of(new Movement(EGG.id(), dec("100"), dec("360"))));

		assertThat(restored.get(EGG.id())).isEqualByComparingTo("100");
		assertThat(restored.get(77L)).isEqualByComparingTo("500");
	}

	@Test
	void neverGoesBelowZeroWhenLeftoversWereAlreadyUsed() {
		Map<Long, BigDecimal> now = Map.of(EGG.id(), dec("50"));

		Map<Long, BigDecimal> restored = PantryRollback.undo(now, List.of(new Movement(EGG.id(), dec("0"), dec("360"))));

		assertThat(restored).doesNotContainKey(EGG.id());
	}
}
