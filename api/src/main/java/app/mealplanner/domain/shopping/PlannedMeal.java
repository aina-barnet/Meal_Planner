package app.mealplanner.domain.shopping;

import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.planning.MealServings;
import java.util.Objects;

/** A recipe assigned to a meal, with the servings to cook. */
public record PlannedMeal(Recipe recipe, MealServings servings) {

	public PlannedMeal {
		Objects.requireNonNull(recipe, "recipe");
		Objects.requireNonNull(servings, "servings");
	}
}
