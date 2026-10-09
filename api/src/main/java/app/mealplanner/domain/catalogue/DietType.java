package app.mealplanner.domain.catalogue;

import java.util.EnumSet;
import java.util.Set;

/** A member's dietary pattern, expressed as the ingredient categories it excludes. */
public enum DietType {
	OMNIVORE(EnumSet.noneOf(IngredientCategory.class)),
	PESCATARIAN(EnumSet.of(IngredientCategory.MEAT)),
	VEGETARIAN(EnumSet.of(IngredientCategory.MEAT, IngredientCategory.FISH, IngredientCategory.SEAFOOD)),
	VEGAN(EnumSet.of(IngredientCategory.MEAT, IngredientCategory.FISH, IngredientCategory.SEAFOOD,
			IngredientCategory.DAIRY, IngredientCategory.EGG));

	private final Set<IngredientCategory> excluded;

	DietType(Set<IngredientCategory> excluded) {
		this.excluded = excluded;
	}

	public boolean allows(IngredientCategory category) {
		return !excluded.contains(category);
	}

	public boolean allows(Recipe recipe) {
		return recipe.ingredients().stream().allMatch(line -> allows(line.ingredient().category()));
	}
}
