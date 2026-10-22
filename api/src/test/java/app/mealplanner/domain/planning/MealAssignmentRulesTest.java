package app.mealplanner.domain.planning;

import static app.mealplanner.domain.Fixtures.EGG;
import static app.mealplanner.domain.Fixtures.POTATO;
import static app.mealplanner.domain.Fixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.ProteinType;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeFacts;
import app.mealplanner.domain.catalogue.RecipeIngredient;
import app.mealplanner.domain.catalogue.Unit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MealAssignmentRulesTest {

	private static final Ingredient EGG_WITH_ALLERGEN = new Ingredient(1, "Ous", IngredientCategory.EGG,
			CanonicalUnit.G, Set.of(Allergen.EGGS),
			Map.of(Unit.UNIT, dec("60")));
	private static final Ingredient HAKE = new Ingredient(7, "Lluç", IngredientCategory.FISH, CanonicalUnit.G, Set.of(Allergen.FISH), Map.of());

	private final Recipe truita = recipe("Truita", true, EGG_WITH_ALLERGEN, POTATO);
	private final Recipe hake = recipe("Lluç al forn", false, HAKE, POTATO);

	@Test
	void anyLunchboxDinerRequiresAPortableRecipe() {
		var diners = List.of(diner("Anna", DinerLocation.HOME), diner("Pau", DinerLocation.TAKEAWAY),
				diner("Marta", DinerLocation.TAKEAWAY));

		assertThat(MealAssignmentRules.violations(hake, diners))
				.containsExactly(new AssignmentViolation.NotPortable(2));
		assertThat(MealAssignmentRules.allows(truita, diners)).isTrue();
	}

	@Test
	void nonPortableRecipesAreFineWhenEveryoneEatsAtHome() {
		assertThat(MealAssignmentRules.allows(hake, List.of(diner("Anna", DinerLocation.HOME)))).isTrue();
	}

	@Test
	void rejectsAllergensOfAnyDiner() {
		var allergic = new Diner(2, "Pau", DinerLocation.HOME, DietType.OMNIVORE, Set.of(Allergen.EGGS));

		assertThat(MealAssignmentRules.violations(truita, List.of(diner("Anna", DinerLocation.HOME), allergic)))
				.containsExactly(new AssignmentViolation.ContainsAllergen("Pau", Allergen.EGGS));
	}

	@Test
	void rejectsRecipesTheDietExcludes() {
		var vegetarian = new Diner(3, "Marta", DinerLocation.HOME, DietType.VEGETARIAN, Set.of());
		var vegan = new Diner(4, "Joan", DinerLocation.HOME, DietType.VEGAN, Set.of());

		assertThat(MealAssignmentRules.violations(hake, List.of(vegetarian)))
				.containsExactly(new AssignmentViolation.IncompatibleDiet("Marta", DietType.VEGETARIAN));
		assertThat(MealAssignmentRules.allows(truita, List.of(vegetarian))).isTrue();
		assertThat(MealAssignmentRules.allows(truita, List.of(vegan))).isFalse();
	}

	@Test
	void derivesAllergensAndDietsFromTheIngredients() {
		assertThat(RecipeFacts.allergens(truita)).containsExactly(Allergen.EGGS);
		assertThat(RecipeFacts.compatibleDiets(truita))
				.containsExactlyInAnyOrder(DietType.OMNIVORE, DietType.PESCATARIAN, DietType.VEGETARIAN);
		assertThat(RecipeFacts.compatibleDiets(hake))
				.containsExactlyInAnyOrder(DietType.OMNIVORE, DietType.PESCATARIAN);
		assertThat(DietType.VEGAN.allows(EGG.category())).isFalse();
	}

	private static Diner diner(String name, DinerLocation location) {
		return new Diner(name.hashCode(), name, location, DietType.OMNIVORE, Set.of());
	}

	private static Recipe recipe(String name, boolean portable, Ingredient... ingredients) {
		List<RecipeIngredient> lines = java.util.Arrays.stream(ingredients)
				.map(i -> new RecipeIngredient(i, dec("100"), Unit.G))
				.toList();
		return new Recipe(name.hashCode(), name, 4, 30, ProteinType.EGGS, portable, lines);
	}
}
