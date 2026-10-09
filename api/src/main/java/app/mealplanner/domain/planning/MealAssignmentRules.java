package app.mealplanner.domain.planning;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeFacts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The hard constraints a recipe must satisfy to be assigned to a meal, whether by the generator
 * or by hand:
 * <ul>
 *   <li>if anyone takes the meal in a lunchbox, the recipe must be portable;</li>
 *   <li>no diner may be allergic or intolerant to any of its allergens;</li>
 *   <li>every diner's diet must allow all of its ingredients.</li>
 * </ul>
 * Dislikes are soft and are not checked here.
 */
public final class MealAssignmentRules {

	private MealAssignmentRules() {
	}

	public static List<AssignmentViolation> violations(Recipe recipe, Collection<Diner> diners) {
		List<AssignmentViolation> violations = new ArrayList<>();
		long takeaway = diners.stream().filter(d -> d.location() == DinerLocation.TAKEAWAY).count();
		if (takeaway > 0 && !recipe.portable()) {
			violations.add(new AssignmentViolation.NotPortable((int) takeaway));
		}
		Set<Allergen> allergens = RecipeFacts.allergens(recipe);
		for (Diner diner : diners) {
			diner.allergens().stream()
					.filter(allergens::contains)
					.sorted()
					.forEach(allergen -> violations.add(new AssignmentViolation.ContainsAllergen(diner.name(), allergen)));
			if (!diner.diet().allows(recipe)) {
				violations.add(new AssignmentViolation.IncompatibleDiet(diner.name(), diner.diet()));
			}
		}
		return violations;
	}

	public static boolean allows(Recipe recipe, Collection<Diner> diners) {
		return violations(recipe, diners).isEmpty();
	}
}
