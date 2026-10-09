package app.mealplanner.domain.catalogue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A recipe as written, for {@code baseServings} people.
 *
 * @param lunchboxServing how it is eaten from a lunchbox; {@code null} exactly when not portable
 */
public record Recipe(
		long id,
		String name,
		int baseServings,
		int prepMinutes,
		ProteinType proteinType,
		Cuisine cuisine,
		Set<Season> seasons,
		boolean portable,
		LunchboxServing lunchboxServing,
		List<RecipeIngredient> ingredients) {

	public Recipe {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(proteinType, "proteinType");
		Objects.requireNonNull(cuisine, "cuisine");
		if (baseServings <= 0) {
			throw new IllegalArgumentException("Base servings must be positive for " + name);
		}
		if (portable != (lunchboxServing != null)) {
			throw new IllegalArgumentException("A recipe has a lunchbox serving exactly when it is portable: " + name);
		}
		if (ingredients.isEmpty()) {
			throw new IllegalArgumentException("A recipe needs at least one ingredient: " + name);
		}
		seasons = Set.copyOf(seasons);
		ingredients = List.copyOf(ingredients);
	}
}
