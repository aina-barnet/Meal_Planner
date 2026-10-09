package app.mealplanner.web;

import app.mealplanner.application.CatalogueEditor;
import app.mealplanner.application.CatalogueService;
import app.mealplanner.application.CatalogueService.RecipeView;
import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.Cuisine;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Season;
import app.mealplanner.web.Dtos.HideRecipeRequest;
import app.mealplanner.web.Dtos.IngredientRequest;
import app.mealplanner.web.Dtos.IngredientResponse;
import app.mealplanner.web.Dtos.RecipeDetail;
import app.mealplanner.web.Dtos.RecipeRequest;
import app.mealplanner.web.Dtos.RecipeSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalogue", description = "Ingredients and recipes: seeded ones and the family's own")
public class CatalogueController {

	private final CatalogueService catalogue;
	private final CatalogueEditor editor;

	public CatalogueController(CatalogueService catalogue, CatalogueEditor editor) {
		this.catalogue = catalogue;
		this.editor = editor;
	}

	@GetMapping("/ingredients")
	public List<IngredientResponse> ingredients() {
		return catalogue.ingredients().stream().map(this::ingredient).toList();
	}

	@PostMapping("/ingredients")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add an ingredient of the family's own")
	public IngredientResponse createIngredient(@Valid @RequestBody IngredientRequest request) {
		return ingredient(catalogue.ingredient(editor.createIngredient(data(request))));
	}

	@PutMapping("/ingredients/{id}")
	@Operation(summary = "Edit an ingredient the family added",
			description = "409 INGREDIENT_NOT_EDITABLE for seeded ingredients; 422 INGREDIENT_IN_USE if recipes "
					+ "would be left with unconvertible quantities.")
	public IngredientResponse updateIngredient(@PathVariable long id, @Valid @RequestBody IngredientRequest request) {
		editor.updateIngredient(id, data(request));
		return ingredient(catalogue.ingredient(id));
	}

	@GetMapping("/recipes")
	@Operation(summary = "Browse and filter the recipe catalogue")
	public List<RecipeSummary> recipes(
			@Parameter(description = "Text search on name and description, accent-insensitive") @RequestParam(required = false) String q,
			@RequestParam(required = false) String proteinType,
			@RequestParam(required = false) Cuisine cuisine,
			@RequestParam(required = false) Season season,
			@Parameter(description = "Only lunchbox-friendly recipes") @RequestParam(required = false) Boolean portable,
			@Parameter(description = "Only recipes compatible with this diet") @RequestParam(required = false) DietType diet,
			@Parameter(description = "Exclude recipes containing any of these") @RequestParam(required = false) Set<Allergen> excludeAllergens,
			@RequestParam(required = false) Integer maxPrepMinutes,
			@Parameter(description = "Include hidden recipes (for managing the catalogue)")
			@RequestParam(defaultValue = "false") boolean includeHidden) {
		Predicate<RecipeView> filter = view -> true;
		if (q != null && !q.isBlank()) {
			String needle = fold(q);
			filter = filter.and(v -> fold(v.recipe().name()).contains(needle)
					|| (v.description() != null && fold(v.description()).contains(needle)));
		}
		if (proteinType != null) {
			filter = filter.and(v -> v.recipe().proteinType().code().equals(proteinType));
		}
		if (cuisine != null) {
			filter = filter.and(v -> v.recipe().cuisine() == cuisine);
		}
		if (season != null) {
			filter = filter.and(v -> v.recipe().seasons().contains(season));
		}
		if (portable != null) {
			filter = filter.and(v -> v.recipe().portable() == portable);
		}
		if (diet != null) {
			filter = filter.and(v -> v.diets().contains(diet));
		}
		if (excludeAllergens != null && !excludeAllergens.isEmpty()) {
			filter = filter.and(v -> v.allergens().stream().noneMatch(excludeAllergens::contains));
		}
		if (maxPrepMinutes != null) {
			filter = filter.and(v -> v.recipe().prepMinutes() <= maxPrepMinutes);
		}
		return (includeHidden ? catalogue.allRecipes() : catalogue.recipes()).stream()
				.filter(filter)
				.map(RecipeSummary::of)
				.toList();
	}

	@PostMapping("/recipes")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add a recipe of the family's own",
			description = "422 INVALID_RECIPE_LINES when a quantity's unit cannot be converted for its ingredient.")
	public RecipeDetail createRecipe(@Valid @RequestBody RecipeRequest request) {
		return RecipeDetail.of(catalogue.recipe(editor.createRecipe(data(request))));
	}

	@PutMapping("/recipes/{id}")
	@Operation(summary = "Edit any recipe, seeded or own")
	public RecipeDetail updateRecipe(@PathVariable long id, @Valid @RequestBody RecipeRequest request) {
		editor.updateRecipe(id, data(request));
		return RecipeDetail.of(catalogue.recipe(id));
	}

	@PatchMapping("/recipes/{id}")
	@Operation(summary = "Hide or show a recipe",
			description = "Hidden recipes are not proposed by the generator nor offered in the picker; meals that "
					+ "already use them keep them. Recipes are never deleted.")
	public RecipeSummary hide(@PathVariable long id, @Valid @RequestBody HideRecipeRequest request) {
		editor.setHidden(id, request.hidden());
		return RecipeSummary.of(catalogue.recipe(id));
	}

	private IngredientResponse ingredient(app.mealplanner.domain.catalogue.Ingredient ingredient) {
		return IngredientResponse.of(ingredient, catalogue.isCustomIngredient(ingredient.id()));
	}

	private static CatalogueEditor.IngredientData data(IngredientRequest r) {
		return new CatalogueEditor.IngredientData(r.name(), r.category(), r.canonicalUnit(), r.packageSize(), r.aisle(),
				r.kcalPer100(), r.proteinPer100(), r.carbsPer100(), r.fatPer100(),
				r.allergens() == null ? Set.of() : r.allergens(),
				r.conversions() == null ? java.util.Map.of() : r.conversions(), r.pantryStaple());
	}

	private static CatalogueEditor.RecipeData data(RecipeRequest r) {
		return new CatalogueEditor.RecipeData(r.name(), r.description(), r.baseServings(), r.prepMinutes(),
				new app.mealplanner.domain.catalogue.ProteinType(r.proteinType().strip()), r.cuisine(), r.seasons(),
				r.portable(), r.lunchboxServing(),
				r.ingredients().stream()
						.map(l -> new CatalogueEditor.LineData(l.ingredientId(), l.quantity(), l.unit(), l.scalingMode()))
						.toList(),
				r.steps());
	}

	@GetMapping("/recipes/{id}")
	@Operation(summary = "Recipe with ingredients, quantities and steps")
	public RecipeDetail recipe(@PathVariable long id) {
		return RecipeDetail.of(catalogue.recipe(id));
	}

	/** Lower-case and strip accents, so "llentia" finds "Llénties" and "carbasso" finds "Carbassó". */
	static String fold(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}
}
