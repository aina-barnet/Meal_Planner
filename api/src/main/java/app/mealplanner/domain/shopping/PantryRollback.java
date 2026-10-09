package app.mealplanner.domain.shopping;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Undoes the pantry movement of a confirmed week when it goes back to draft.
 *
 * <p>Confirming a week moved each ingredient's pantry quantity by {@code leftover − fromPantry}:
 * what the meals took from home was consumed, and the leftover of the rounded packages was added.
 * Rolling back applies the opposite movement to the <em>current</em> pantry rather than restoring
 * a snapshot, so manual pantry edits made since the confirmation are kept. A quantity never goes
 * below zero (if the family has already used the leftovers, there is nothing to take back).
 */
public final class PantryRollback {

	private PantryRollback() {
	}

	/** What a confirmed week did to the pantry for one ingredient, from its shopping list line. */
	public record Movement(long ingredientId, BigDecimal fromPantry, BigDecimal leftover) {
	}

	/** @return the pantry before the week was confirmed; ingredients at zero are absent */
	public static Map<Long, BigDecimal> undo(Map<Long, BigDecimal> pantry, Collection<Movement> movements) {
		Map<Long, BigDecimal> restored = new HashMap<>(pantry);
		for (Movement movement : movements) {
			BigDecimal current = restored.getOrDefault(movement.ingredientId(), BigDecimal.ZERO);
			BigDecimal before = current.subtract(movement.leftover()).add(movement.fromPantry());
			if (before.signum() > 0) {
				restored.put(movement.ingredientId(), before);
			} else {
				restored.remove(movement.ingredientId());
			}
		}
		return Map.copyOf(restored);
	}
}
