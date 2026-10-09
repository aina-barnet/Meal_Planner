package app.mealplanner.domain.catalogue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Facts derived from a recipe's ingredient list, so they can never drift from it. */
public final class RecipeFacts {

	private RecipeFacts() {
	}

	public static Set<Allergen> allergens(Recipe recipe) {
		Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
		recipe.ingredients().forEach(line -> allergens.addAll(line.ingredient().allergens()));
		return allergens;
	}

	public static Set<DietType> compatibleDiets(Recipe recipe) {
		return Arrays.stream(DietType.values())
				.filter(diet -> diet.allows(recipe))
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(DietType.class)));
	}
}
