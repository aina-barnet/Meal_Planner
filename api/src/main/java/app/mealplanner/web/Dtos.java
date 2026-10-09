package app.mealplanner.web;

import app.mealplanner.application.CatalogueService.RecipeView;
import app.mealplanner.application.WeekPlanService.MealDetail;
import app.mealplanner.application.WeekPlanService.WeekDetail;
import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.Cuisine;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.LunchboxServing;
import app.mealplanner.domain.catalogue.Nutrition;
import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Season;
import app.mealplanner.domain.catalogue.Unit;
import app.mealplanner.domain.catalogue.UnitConverter;
import app.mealplanner.domain.nutrition.ActivityLevel;
import app.mealplanner.domain.nutrition.EnergyRequirement;
import app.mealplanner.domain.nutrition.Sex;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.domain.planning.MealSlot;
import app.mealplanner.persistence.AllergenKind;
import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.WeekPlanEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Request and response bodies of the REST API, and their mapping from the application layer. */
public final class Dtos {

	private Dtos() {
	}

	// ------------------------------------------------------------- household

	public record HouseholdRequest(@NotBlank @Size(max = 120) String name) {
	}

	public record HouseholdResponse(long id, String name) {

		static HouseholdResponse of(HouseholdEntity h) {
			return new HouseholdResponse(h.getId(), h.getName());
		}
	}

	public record AllergenEntry(@NotNull Allergen allergen, @NotNull AllergenKind kind) {
	}

	public record MemberRequest(
			@NotBlank @Size(max = 80) String name,
			@NotNull @Past LocalDate birthDate,
			@NotNull Sex sex,
			@NotNull @DecimalMin("2") @DecimalMax("400") BigDecimal weightKg,
			@NotNull @DecimalMin("40") @DecimalMax("250") BigDecimal heightCm,
			@NotNull ActivityLevel activityLevel,
			@NotNull DietType dietType,
			@Size(max = 500) String dietNotes,
			@Schema(description = "Manual daily kcal; replaces the estimate") @Min(500) @Max(6000) Integer kcalOverride,
			@Schema(description = "Share of daily kcal eaten at lunch + dinner; default 65") @Min(10) @Max(100) Integer mainMealsPct,
			List<@Valid AllergenEntry> allergens,
			List<@NotNull Long> dislikedIngredientIds) {
	}

	@Schema(description = "Orientative estimate (Mifflin-St Jeor × activity factor). Not medical advice.")
	public record RequirementResponse(
			int ageYears,
			int basalKcal,
			int estimatedDailyKcal,
			int dailyKcal,
			boolean overridden,
			int perMainMealKcal,
			boolean equationValidForAge) {

		static RequirementResponse of(EnergyRequirement r) {
			return new RequirementResponse(r.ageYears(), r.basalKcal(), r.estimatedDailyKcal(), r.dailyKcal(),
					r.overridden(), r.perMainMealKcal(), r.equationValidForAge());
		}
	}

	public record MemberResponse(
			long id,
			String name,
			LocalDate birthDate,
			Sex sex,
			BigDecimal weightKg,
			BigDecimal heightCm,
			ActivityLevel activityLevel,
			DietType dietType,
			String dietNotes,
			Integer kcalOverride,
			int mainMealsPct,
			List<AllergenEntry> allergens,
			List<Long> dislikedIngredientIds,
			RequirementResponse requirement) {

		static MemberResponse of(MemberEntity m, EnergyRequirement requirement) {
			return new MemberResponse(m.getId(), m.getName(), m.getBirthDate(), m.getSex(), m.getWeightKg(),
					m.getHeightCm(), m.getActivityLevel(), m.getDietType(), m.getDietNotes(), m.getKcalOverride(),
					m.getMainMealsPct(),
					m.getAllergens().entrySet().stream()
							.map(e -> new AllergenEntry(e.getKey(), e.getValue()))
							.sorted(Comparator.comparing(AllergenEntry::allergen))
							.toList(),
					m.getDislikedIngredientIds().stream().sorted().toList(),
					RequirementResponse.of(requirement));
		}
	}

	// ------------------------------------------------------------- catalogue

	public record IngredientResponse(
			long id,
			String name,
			IngredientCategory category,
			CanonicalUnit canonicalUnit,
			BigDecimal packageSize,
			Aisle aisle,
			boolean pantryStaple,
			Set<Allergen> allergens,
			double kcalPer100,
			double proteinPer100,
			double carbsPer100,
			double fatPer100,
			@Schema(description = "Canonical units (g or ml) per one recipe unit, e.g. UNIT: 60 for an egg")
			java.util.Map<Unit, BigDecimal> conversions,
			@Schema(description = "Added by the family; only these can be edited") boolean custom) {

		static IngredientResponse of(Ingredient i, boolean custom) {
			var n = i.nutritionPer100();
			return new IngredientResponse(i.id(), i.name(), i.category(), i.canonicalUnit(), i.packageSize(),
					i.aisle(), i.pantryStaple(), new TreeSet<>(i.allergens()), n.kcal(), n.proteinG(), n.carbsG(),
					n.fatG(), new java.util.TreeMap<>(i.conversions()), custom);
		}
	}

	public record IngredientRequest(
			@NotBlank @Size(max = 120) String name,
			@NotNull IngredientCategory category,
			@NotNull CanonicalUnit canonicalUnit,
			@NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal packageSize,
			@NotNull Aisle aisle,
			@NotNull @DecimalMin("0") @DecimalMax("900") BigDecimal kcalPer100,
			@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal proteinPer100,
			@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal carbsPer100,
			@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal fatPer100,
			Set<Allergen> allergens,
			java.util.Map<Unit, @NotNull @DecimalMin("0.001") BigDecimal> conversions,
			boolean pantryStaple) {
	}

	public record RecipeLineRequest(
			@NotNull Long ingredientId,
			@NotNull @DecimalMin("0.001") BigDecimal quantity,
			@NotNull Unit unit,
			@NotNull ScalingMode scalingMode) {
	}

	public record RecipeRequest(
			@NotBlank @Size(max = 160) String name,
			@Size(max = 500) String description,
			@Min(1) @Max(20) int baseServings,
			@Min(1) @Max(600) int prepMinutes,
			@Schema(description = "Code of a protein type, see /api/proteins") @NotBlank String proteinType,
			@NotNull Cuisine cuisine,
			@NotEmpty Set<@NotNull Season> seasons,
			boolean portable,
			@Schema(description = "Required when portable") LunchboxServing lunchboxServing,
			@NotEmpty List<@Valid @NotNull RecipeLineRequest> ingredients,
			@NotEmpty List<@NotBlank @Size(max = 1000) String> steps) {
	}

	public record HideRecipeRequest(@NotNull Boolean hidden) {
	}

	public record ProteinRequest(@NotBlank @Size(max = 60) String name) {
	}

	public record ProteinResponse(
			String code,
			String name,
			@Schema(description = "Added by the family (seeded ones can be renamed and deleted too)") boolean custom,
			@Schema(description = "How many recipes use it") long recipes) {
	}

	@Schema(description = "Nutrition per serving, computed from the ingredient list")
	public record NutritionResponse(int kcal, double proteinG, double carbsG, double fatG) {

		static NutritionResponse of(Nutrition n) {
			return new NutritionResponse((int) Math.round(n.kcal()), round1(n.proteinG()), round1(n.carbsG()),
					round1(n.fatG()));
		}
	}

	public record RecipeSummary(
			long id,
			String code,
			String name,
			String description,
			int baseServings,
			int prepMinutes,
			@Schema(description = "Code of a protein type, see /api/proteins") String proteinType,
			Cuisine cuisine,
			Set<Season> seasons,
			boolean portable,
			LunchboxServing lunchboxServing,
			NutritionResponse nutrition,
			Set<Allergen> allergens,
			Set<DietType> diets,
			@Schema(description = "Added by the family") boolean custom,
			@Schema(description = "Hidden from the generator and the picker") boolean hidden) {

		static RecipeSummary of(RecipeView v) {
			var r = v.recipe();
			return new RecipeSummary(r.id(), v.code(), r.name(), v.description(), r.baseServings(), r.prepMinutes(),
					r.proteinType().code(), r.cuisine(), new TreeSet<>(r.seasons()), r.portable(), r.lunchboxServing(),
					NutritionResponse.of(v.perServing()), new TreeSet<>(v.allergens()), new TreeSet<>(v.diets()),
					v.custom(), v.hidden());
		}
	}

	public record RecipeIngredientResponse(
			long ingredientId,
			String name,
			BigDecimal quantity,
			Unit unit,
			ScalingMode scalingMode,
			BigDecimal canonicalQuantity,
			CanonicalUnit canonicalUnit,
			boolean pantryStaple) {
	}

	public record RecipeDetail(RecipeSummary summary, List<RecipeIngredientResponse> ingredients, List<String> steps) {

		static RecipeDetail of(RecipeView v) {
			return new RecipeDetail(RecipeSummary.of(v),
					v.recipe().ingredients().stream()
							.map(line -> new RecipeIngredientResponse(line.ingredient().id(), line.ingredient().name(),
									line.quantity(), line.unit(), line.scalingMode(),
									UnitConverter.toCanonical(line.ingredient(), line.quantity(), line.unit())
											.setScale(1, RoundingMode.HALF_UP),
									line.ingredient().canonicalUnit(), line.ingredient().pantryStaple()))
							.toList(),
					v.steps());
		}
	}

	// ------------------------------------------------------------- weeks

	public record CreateWeekRequest(@NotNull LocalDate weekStart) {
	}

	public record CopyWeekRequest(@Schema(description = "Monday of the week to copy onto") @NotNull LocalDate targetWeekStart) {
	}

	public record AttendeeDto(@NotNull Long memberId, @NotNull DinerLocation location) {
	}

	public record MealConfigRequest(
			@NotNull LocalDate date,
			@NotNull MealSlot slot,
			@NotNull List<@Valid @NotNull AttendeeDto> attendees,
			@Min(0) @Max(20) int extraServings) {
	}

	public record ConfigureWeekRequest(@NotNull List<@Valid @NotNull MealConfigRequest> meals) {
	}

	public record AssignRecipeRequest(@Schema(description = "null clears the meal") Long recipeId) {
	}

	public record UpdateMealRequest(Boolean locked, @Min(0) @Max(20) Integer extraServings) {
	}

	public record SwapRequest(@NotNull Long mealIdA, @NotNull Long mealIdB) {
	}

	public record WeekSummary(long id, LocalDate weekStart, WeekPlanEntity.Status status, Instant confirmedAt,
			int mealCount, int assignedCount) {

		static WeekSummary of(WeekPlanEntity w) {
			return new WeekSummary(w.getId(), w.getWeekStart(), w.getStatus(), w.getConfirmedAt(), w.getMeals().size(),
					(int) w.getMeals().stream().filter(m -> m.getRecipeId() != null).count());
		}
	}

	public record MealResponse(
			long id,
			LocalDate date,
			MealSlot slot,
			List<AttendeeDto> attendees,
			int dinersAtHome,
			int dinersTakeaway,
			int extraServings,
			int totalServings,
			boolean locked,
			String emptyReason,
			RecipeSummary recipe,
			@Schema(description = "Sum of the attendees' per-meal kcal targets") int kcalTarget,
			@Schema(description = "kcal the diners get from the recipe, one serving each") Integer kcalOffered,
			@Schema(description = "kcalOffered / kcalTarget; 1.0 is a perfect fit") Double kcalFit,
			@Schema(description = "Soft warnings, such as disliked ingredients") List<String> notes) {

		static MealResponse of(MealDetail d) {
			var m = d.meal();
			RecipeSummary recipe = d.recipe() == null ? null : RecipeSummary.of(d.recipe());
			Integer offered = recipe == null ? null : recipe.nutrition().kcal() * (d.dinersAtHome() + d.dinersTakeaway());
			return new MealResponse(m.getId(), m.getDate(), m.getSlot(),
					m.getAttendees().entrySet().stream()
							.map(e -> new AttendeeDto(e.getKey(), e.getValue()))
							.sorted(Comparator.comparing(AttendeeDto::memberId))
							.toList(),
					d.dinersAtHome(), d.dinersTakeaway(), m.getExtraServings(), d.totalServings(), m.isLocked(),
					m.getEmptyReason(), recipe, d.kcalTarget(), offered,
					d.kcalFit() == null ? null : Math.round(d.kcalFit() * 100) / 100.0, d.notes());
		}
	}

	public record WeekResponse(long id, LocalDate weekStart, WeekPlanEntity.Status status, Instant confirmedAt,
			List<MealResponse> meals) {

		static WeekResponse of(WeekDetail d) {
			var w = d.week();
			return new WeekResponse(w.getId(), w.getWeekStart(), w.getStatus(), w.getConfirmedAt(),
					d.meals().stream().map(MealResponse::of).toList());
		}
	}

	// ------------------------------------------------------------- shopping & pantry

	public record ShoppingItemResponse(
			long id,
			long ingredientId,
			String name,
			CanonicalUnit canonicalUnit,
			BigDecimal required,
			BigDecimal fromPantry,
			BigDecimal toBuy,
			int packages,
			BigDecimal packageSize,
			BigDecimal leftover,
			boolean coveredByPantry,
			boolean checked) {
	}

	public record AisleResponse(Aisle aisle, List<ShoppingItemResponse> items) {
	}

	public record ShoppingListResponse(long weekId, Instant generatedAt, List<AisleResponse> aisles,
			@Schema(description = "Pantry staples the week uses: assumed at home, not bought") List<String> staples) {
	}

	public record CheckItemRequest(@NotNull Boolean checked) {
	}

	public record PantryItemResponse(long ingredientId, String name, CanonicalUnit canonicalUnit, Aisle aisle,
			BigDecimal quantity) {
	}

	public record PantryRequest(@NotNull @DecimalMin("0") @DecimalMax("1000000") BigDecimal quantity) {
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}
}
