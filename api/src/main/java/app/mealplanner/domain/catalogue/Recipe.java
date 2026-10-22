package app.mealplanner.domain.catalogue;

import java.util.List;
import java.util.Objects;

/**
 * A recipe as written, for {@code baseServings} people. Every dish can be eaten at home;
 * {@code portable} says whether it also works in a lunchbox.
 */
public record Recipe(
		long id,
		String name,
		int baseServings,
		int prepMinutes,
		ProteinType proteinType,
		boolean portable,
		List<RecipeIngredient> ingredients) {

	public Recipe {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(proteinType, "proteinType");
		if (baseServings <= 0) {
			throw new IllegalArgumentException("Base servings must be positive for " + name);
		}
		if (ingredients.isEmpty()) {
			throw new IllegalArgumentException("A recipe needs at least one ingredient: " + name);
		}
		ingredients = List.copyOf(ingredients);
	}
}
