package app.mealplanner.application;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.ProteinType;
import app.mealplanner.domain.catalogue.Unit;
import app.mealplanner.domain.catalogue.UnitConverter;
import app.mealplanner.persistence.IngredientEntity;
import app.mealplanner.persistence.RecipeEntity;
import app.mealplanner.persistence.RecipeIngredientEntity;
import app.mealplanner.persistence.Repositories.IngredientRepository;
import app.mealplanner.persistence.Repositories.ProteinRepository;
import app.mealplanner.persistence.Repositories.RecipeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Changes to the catalogue made by the family: their own ingredients and recipes, edits to any
 * recipe, and hiding recipes. Hidden recipes are never deleted, so weeks that used them still work.
 */
@Service
@Transactional
public class CatalogueEditor {

	private final IngredientRepository ingredients;
	private final RecipeRepository recipes;
	private final ProteinRepository proteins;
	private final CatalogueService catalogue;

	public CatalogueEditor(IngredientRepository ingredients, RecipeRepository recipes, ProteinRepository proteins,
			CatalogueService catalogue) {
		this.ingredients = ingredients;
		this.recipes = recipes;
		this.proteins = proteins;
		this.catalogue = catalogue;
	}

	public record IngredientData(
			String name,
			IngredientCategory category,
			CanonicalUnit canonicalUnit,
			Set<Allergen> allergens,
			Map<Unit, BigDecimal> conversions) {
	}

	public record LineData(long ingredientId, BigDecimal quantity, Unit unit) {
	}

	public record RecipeData(
			String name,
			int baseServings,
			int prepMinutes,
			ProteinType proteinType,
			boolean portable,
			List<LineData> lines,
			List<String> steps) {
	}

	// ------------------------------------------------------------------ ingredients

	public long createIngredient(IngredientData data) {
		rejectDuplicateName(data.name(), null);
		IngredientEntity entity = IngredientEntity.custom("custom-" + UUID.randomUUID().toString().substring(0, 8));
		apply(entity, data);
		long id = ingredients.save(entity).getId();
		reloadAfterCommit();
		return id;
	}

	/** Only ingredients the family added can be edited; the seeded ones are reference data. */
	public void updateIngredient(long id, IngredientData data) {
		IngredientEntity entity = ingredients.findById(id).orElseThrow(() -> new Errors.NotFound(
				"INGREDIENT_NOT_FOUND", "No existeix l'ingredient " + id + "."));
		if (!entity.isCustom()) {
			throw new Errors.Conflict("INGREDIENT_NOT_EDITABLE", "Només es poden editar els ingredients propis.");
		}
		rejectDuplicateName(data.name(), id);
		List<String> broken = linesBrokenBy(id, data);
		if (!broken.isEmpty()) {
			throw new Errors.RuleViolation("INGREDIENT_IN_USE",
					"Aquest canvi deixaria receptes amb quantitats que no es poden convertir.", broken);
		}
		apply(entity, data);
		reloadAfterCommit();
	}

	// ------------------------------------------------------------------ recipes

	public long createRecipe(RecipeData data) {
		RecipeEntity entity = RecipeEntity.custom("custom-" + UUID.randomUUID().toString().substring(0, 8));
		apply(entity, data);
		long id = recipes.save(entity).getId();
		reloadAfterCommit();
		return id;
	}

	public void updateRecipe(long id, RecipeData data) {
		RecipeEntity entity = recipe(id);
		apply(entity, data);
		reloadAfterCommit();
	}

	/** Hidden recipes are no longer proposed or offered, but meals that use them keep them. */
	public void setHidden(long id, boolean hidden) {
		recipe(id).setHidden(hidden);
		reloadAfterCommit();
	}

	// ------------------------------------------------------------------ helpers

	private RecipeEntity recipe(long id) {
		return recipes.findById(id).orElseThrow(() -> new Errors.NotFound("RECIPE_NOT_FOUND",
				"No existeix la recepta " + id + "."));
	}

	private void apply(IngredientEntity entity, IngredientData data) {
		List<String> metric = data.conversions().keySet().stream()
				.filter(Unit::isMetric)
				.map(Labels::unit)
				.toList();
		if (!metric.isEmpty()) {
			throw new Errors.RuleViolation("INVALID_CONVERSION",
					"Les equivalències són per a unitats com «unitat» o «cullerada», no per a g, kg, ml o l.", metric);
		}
		entity.setName(data.name().strip());
		entity.setCategory(data.category());
		entity.setCanonicalUnit(data.canonicalUnit());
		entity.getAllergens().clear();
		entity.getAllergens().addAll(data.allergens());
		entity.getConversions().clear();
		entity.getConversions().putAll(data.conversions());
	}

	private void apply(RecipeEntity entity, RecipeData data) {
		if (!proteins.existsById(data.proteinType().code())) {
			throw new Errors.RuleViolation("UNKNOWN_PROTEIN", "No existeix el tipus de proteïna «"
					+ data.proteinType().code() + "».", List.of());
		}
		List<String> problems = new ArrayList<>();
		List<Ingredient> lineIngredients = new ArrayList<>();
		for (int i = 0; i < data.lines().size(); i++) {
			LineData line = data.lines().get(i);
			if (!catalogue.ingredientExists(line.ingredientId())) {
				problems.add("Línia " + (i + 1) + ": l'ingredient " + line.ingredientId() + " no existeix.");
				lineIngredients.add(null);
				continue;
			}
			Ingredient ingredient = catalogue.ingredient(line.ingredientId());
			lineIngredients.add(ingredient);
			if (!UnitConverter.supports(ingredient, line.unit())) {
				problems.add("«" + ingredient.name() + "» no es pot expressar en " + Labels.unit(line.unit())
						+ ". Unitats possibles: " + supportedUnits(ingredient) + ".");
			}
		}
		if (!problems.isEmpty()) {
			throw new Errors.RuleViolation("INVALID_RECIPE_LINES", "Hi ha ingredients amb unitats no vàlides.", problems);
		}

		entity.setName(data.name().strip());
		entity.setBaseServings(data.baseServings());
		entity.setPrepMinutes(data.prepMinutes());
		entity.setProteinType(data.proteinType());
		entity.setPortable(data.portable());
		entity.getSteps().clear();
		for (int i = 0; i < data.steps().size(); i++) {
			entity.getSteps().add(new RecipeEntity.Step(i + 1, data.steps().get(i).strip()));
		}

		// Lines are updated in place by position: replacing them would insert before deleting and
		// clash with the (recipe, position) unique constraint.
		List<RecipeIngredientEntity> lines = entity.getIngredients();
		for (int i = 0; i < data.lines().size(); i++) {
			LineData line = data.lines().get(i);
			RecipeIngredientEntity row;
			if (i < lines.size()) {
				row = lines.get(i);
			} else {
				row = new RecipeIngredientEntity(entity, i + 1);
				lines.add(row);
			}
			row.set(ingredients.getReferenceById(line.ingredientId()), line.quantity(), line.unit());
		}
		while (lines.size() > data.lines().size()) {
			lines.removeLast();
		}
	}

	/** Recipe lines that the new ingredient data would make impossible to convert. */
	private List<String> linesBrokenBy(long ingredientId, IngredientData data) {
		Ingredient changed = new Ingredient(ingredientId, data.name(), data.category(), data.canonicalUnit(),
				data.allergens(), data.conversions().entrySet().stream()
						.filter(e -> !e.getKey().isMetric())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		return catalogue.allRecipes().stream()
				.flatMap(view -> view.recipe().ingredients().stream()
						.filter(line -> line.ingredient().id() == ingredientId)
						.filter(line -> !UnitConverter.supports(changed, line.unit()))
						.map(line -> "«" + view.recipe().name() + "» en fa servir " + Labels.unit(line.unit()) + "."))
				.distinct()
				.toList();
	}

	private void rejectDuplicateName(String name, Long exceptId) {
		String wanted = name.strip().toLowerCase(Locale.ROOT);
		boolean taken = catalogue.ingredients().stream()
				.anyMatch(i -> i.name().toLowerCase(Locale.ROOT).equals(wanted) && (exceptId == null || i.id() != exceptId));
		if (taken) {
			throw new Errors.Conflict("DUPLICATE_INGREDIENT", "Ja hi ha un ingredient que es diu «" + name.strip() + "».");
		}
	}

	private static String supportedUnits(Ingredient ingredient) {
		return Arrays.stream(Unit.values())
				.filter(unit -> UnitConverter.supports(ingredient, unit))
				.map(Labels::unit)
				.collect(Collectors.joining(", "));
	}

	/** The cached catalogue is reloaded once this transaction has committed. */
	private void reloadAfterCommit() {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				catalogue.invalidate();
			}
		});
	}
}
