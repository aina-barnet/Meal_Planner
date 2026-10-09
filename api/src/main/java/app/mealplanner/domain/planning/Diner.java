package app.mealplanner.domain.planning;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import java.util.Objects;
import java.util.Set;

/**
 * A member eating a particular meal, with the constraints that apply to them.
 *
 * @param allergens          allergies and intolerances; both are hard constraints
 * @param dislikedIngredients ingredient ids the member dislikes; a soft preference
 */
public record Diner(
		long memberId,
		String name,
		DinerLocation location,
		DietType diet,
		Set<Allergen> allergens,
		Set<Long> dislikedIngredients) {

	public Diner {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(location, "location");
		Objects.requireNonNull(diet, "diet");
		allergens = Set.copyOf(allergens);
		dislikedIngredients = Set.copyOf(dislikedIngredients);
	}
}
