package app.mealplanner.application;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.Nutrition;
import app.mealplanner.domain.catalogue.NutritionCalculator;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.catalogue.RecipeFacts;
import app.mealplanner.domain.catalogue.RecipeIngredient;
import app.mealplanner.persistence.IngredientEntity;
import app.mealplanner.persistence.RecipeEntity;
import app.mealplanner.persistence.Repositories.IngredientRepository;
import app.mealplanner.persistence.Repositories.RecipeRepository;
import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads the seeded catalogue once and serves it as domain objects. The catalogue is read-only
 * (it only changes through Flyway migrations), so keeping it in memory is safe and makes every
 * planning operation a pure in-memory computation.
 */
@Service
public class CatalogueService {

	private static final Comparator<String> CATALAN = Collator.getInstance(Locale.forLanguageTag("ca"))::compare;

	private final IngredientRepository ingredients;
	private final RecipeRepository recipes;
	private final TransactionTemplate readOnlyTx;
	private volatile Catalogue catalogue;

	public CatalogueService(IngredientRepository ingredients, RecipeRepository recipes,
			PlatformTransactionManager transactionManager) {
		this.ingredients = ingredients;
		this.recipes = recipes;
		this.readOnlyTx = new TransactionTemplate(transactionManager);
		this.readOnlyTx.setReadOnly(true);
	}

	/** A recipe with the presentation data the domain does not need. */
	public record RecipeView(
			Recipe recipe,
			String code,
			String description,
			List<String> steps,
			Nutrition perServing,
			Set<Allergen> allergens,
			Set<DietType> diets,
			boolean custom,
			boolean hidden) {
	}

	public List<Ingredient> ingredients() {
		return catalogue().ingredients.values().stream()
				.sorted(Comparator.comparing(Ingredient::name, CATALAN))
				.toList();
	}

	public Ingredient ingredient(long id) {
		Ingredient ingredient = catalogue().ingredients.get(id);
		if (ingredient == null) {
			throw new Errors.NotFound("INGREDIENT_NOT_FOUND", "No existeix l'ingredient " + id + ".");
		}
		return ingredient;
	}

	public boolean ingredientExists(long id) {
		return catalogue().ingredients.containsKey(id);
	}

	/** Whether the family added this ingredient (only those can be edited). */
	public boolean isCustomIngredient(long id) {
		return catalogue().customIngredients.contains(id);
	}

	/** Visible recipes: what the generator and the recipe picker may propose. */
	public List<RecipeView> recipes() {
		return allRecipes().stream().filter(view -> !view.hidden()).toList();
	}

	/** Every recipe, hidden ones included, for managing the catalogue. */
	public List<RecipeView> allRecipes() {
		return catalogue().recipes.values().stream()
				.sorted(Comparator.comparing(view -> view.recipe().name(), CATALAN))
				.toList();
	}

	/** Drops the cached catalogue; the next read reloads it. Call after the catalogue changes. */
	public void invalidate() {
		catalogue = null;
	}

	public RecipeView recipe(long id) {
		RecipeView view = catalogue().recipes.get(id);
		if (view == null) {
			throw new Errors.NotFound("RECIPE_NOT_FOUND", "No existeix la recepta " + id + ".");
		}
		return view;
	}

	private Catalogue catalogue() {
		Catalogue loaded = catalogue;
		if (loaded == null) {
			synchronized (this) {
				loaded = catalogue;
				if (loaded == null) {
					loaded = readOnlyTx.execute(status -> load());
					catalogue = loaded;
				}
			}
		}
		return loaded;
	}

	private Catalogue load() {
		List<IngredientEntity> ingredientEntities = ingredients.findAll();
		Map<Long, Ingredient> ingredientsById = ingredientEntities.stream()
				.map(CatalogueService::toDomain)
				.collect(Collectors.toMap(Ingredient::id, Function.identity()));
		Set<Long> customIngredients = ingredientEntities.stream()
				.filter(IngredientEntity::isCustom)
				.map(IngredientEntity::getId)
				.collect(Collectors.toUnmodifiableSet());
		Map<Long, RecipeView> recipesById = new LinkedHashMap<>();
		for (RecipeEntity entity : recipes.findAllWithIngredients()) {
			Recipe recipe = toDomain(entity, ingredientsById);
			recipesById.put(recipe.id(), new RecipeView(recipe, entity.getCode(), entity.getDescription(),
					entity.getSteps().stream().map(RecipeEntity.Step::getInstruction).toList(),
					NutritionCalculator.perServing(recipe), RecipeFacts.allergens(recipe),
					RecipeFacts.compatibleDiets(recipe), entity.isCustom(), entity.isHidden()));
		}
		return new Catalogue(Map.copyOf(ingredientsById), customIngredients, recipesById);
	}

	private static Ingredient toDomain(IngredientEntity e) {
		return new Ingredient(e.getId(), e.getName(), e.getCategory(), e.getCanonicalUnit(), e.getPackageSize(),
				e.getAisle(),
				new Nutrition(e.getKcalPer100().doubleValue(), e.getProteinPer100().doubleValue(),
						e.getCarbsPer100().doubleValue(), e.getFatPer100().doubleValue()),
				e.getAllergens(), e.getConversions(), e.isPantryStaple());
	}

	private static Recipe toDomain(RecipeEntity e, Map<Long, Ingredient> ingredientsById) {
		List<RecipeIngredient> lines = e.getIngredients().stream()
				.map(ri -> new RecipeIngredient(ingredientsById.get(ri.getIngredient().getId()), ri.getQuantity(),
						ri.getUnit(), ri.getScalingMode()))
				.toList();
		return new Recipe(e.getId(), e.getName(), e.getBaseServings(), e.getPrepMinutes(), e.getProteinType(),
				e.getCuisine(), e.getSeasons(), e.isPortable(), e.getLunchboxServing(), lines);
	}

	record Catalogue(Map<Long, Ingredient> ingredients, Set<Long> customIngredients, Map<Long, RecipeView> recipes) {
	}
}
