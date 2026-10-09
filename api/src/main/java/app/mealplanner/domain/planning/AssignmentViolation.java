package app.mealplanner.domain.planning;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;

/** Why a recipe may not be assigned to a meal. */
public sealed interface AssignmentViolation {

	/** The meal has lunchbox diners and the recipe does not survive being packed and reheated. */
	record NotPortable(int dinersTakeaway) implements AssignmentViolation {
	}

	/** The recipe contains an allergen a diner is allergic or intolerant to. */
	record ContainsAllergen(String dinerName, Allergen allergen) implements AssignmentViolation {
	}

	/** The recipe contains an ingredient the diner's diet excludes. */
	record IncompatibleDiet(String dinerName, DietType diet) implements AssignmentViolation {
	}
}
