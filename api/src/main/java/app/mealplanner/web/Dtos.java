package app.mealplanner.web;

import app.mealplanner.application.CatalogueService.RecipeView;
import app.mealplanner.application.WeekPlanService.MealDetail;
import app.mealplanner.application.WeekPlanService.WeekDetail;
import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.Unit;
import app.mealplanner.domain.catalogue.UnitConverter;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.domain.planning.MealSlot;
import app.mealplanner.persistence.AllergenKind;
import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.WeekPlanEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
			@NotNull DietType dietType,
			List<@Valid AllergenEntry> allergens) {
	}

	public record MemberResponse(
			long id,
			String name,
			DietType dietType,
			List<AllergenEntry> allergens) {

		static MemberResponse of(MemberEntity m) {
			return new MemberResponse(m.getId(), m.getName(), m.getDietType(),
					m.getAllergens().entrySet().stream()
							.map(e -> new AllergenEntry(e.getKey(), e.getValue()))
							.sorted(Comparator.comparing(AllergenEntry::allergen))
							.toList());
		}
	}

	// ------------------------------------------------------------- catalogue

	public record IngredientResponse(
			long id,
			String name,
			IngredientCategory category,
			CanonicalUnit canonicalUnit,
			Set<Allergen> allergens,
			@Schema(description = "Canonical units (g or ml) per one recipe unit, e.g. UNIT: 60 for an egg")
			java.util.Map<Unit, BigDecimal> conversions,
			@Schema(description = "Added by the family; only these can be edited") boolean custom) {

		static IngredientResponse of(Ingredient i, boolean custom) {
			return new IngredientResponse(i.id(), i.name(), i.category(), i.canonicalUnit(), new TreeSet<>(i.allergens()),
					new java.util.TreeMap<>(i.conversions()), custom);
		}
	}

	public record IngredientRequest(
			@NotBlank @Size(max = 120) String name,
			@NotNull IngredientCategory category,
			@NotNull CanonicalUnit canonicalUnit,
			Set<Allergen> allergens,
			java.util.Map<Unit, @NotNull @DecimalMin("0.001") BigDecimal> conversions) {
	}

	public record RecipeLineRequest(
			@NotNull Long ingredientId,
			@NotNull @DecimalMin("0.001") BigDecimal quantity,
			@NotNull Unit unit) {
	}

	public record RecipeRequest(
			@NotBlank @Size(max = 160) String name,
			@Min(1) @Max(20) int baseServings,
			@Min(1) @Max(600) int prepMinutes,
			@Schema(description = "Code of a protein type, see /api/proteins") @NotBlank String proteinType,
			@Schema(description = "Also works in a lunchbox; every dish can be eaten at home") boolean portable,
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

	public record RecipeSummary(
			long id,
			String code,
			String name,
			int baseServings,
			int prepMinutes,
			@Schema(description = "Code of a protein type, see /api/proteins") String proteinType,
			boolean portable,
			Set<Allergen> allergens,
			Set<DietType> diets,
			@Schema(description = "Added by the family") boolean custom,
			@Schema(description = "Hidden from the generator and the picker") boolean hidden) {

		static RecipeSummary of(RecipeView v) {
			var r = v.recipe();
			return new RecipeSummary(r.id(), v.code(), r.name(), r.baseServings(), r.prepMinutes(),
					r.proteinType().code(), r.portable(),
					new TreeSet<>(v.allergens()), new TreeSet<>(v.diets()), v.custom(), v.hidden());
		}
	}

	public record RecipeIngredientResponse(
			long ingredientId,
			String name,
			BigDecimal quantity,
			Unit unit,
			BigDecimal canonicalQuantity,
			CanonicalUnit canonicalUnit) {
	}

	public record RecipeDetail(RecipeSummary summary, List<RecipeIngredientResponse> ingredients, List<String> steps) {

		static RecipeDetail of(RecipeView v) {
			return new RecipeDetail(RecipeSummary.of(v),
					v.recipe().ingredients().stream()
							.map(line -> new RecipeIngredientResponse(line.ingredient().id(), line.ingredient().name(),
									line.quantity(), line.unit(),
									UnitConverter.toCanonical(line.ingredient(), line.quantity(), line.unit())
											.setScale(1, RoundingMode.HALF_UP),
									line.ingredient().canonicalUnit()))
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
			RecipeSummary recipe) {

		static MealResponse of(MealDetail d) {
			var m = d.meal();
			RecipeSummary recipe = d.recipe() == null ? null : RecipeSummary.of(d.recipe());
			return new MealResponse(m.getId(), m.getDate(), m.getSlot(),
					m.getAttendees().entrySet().stream()
							.map(e -> new AttendeeDto(e.getKey(), e.getValue()))
							.sorted(Comparator.comparing(AttendeeDto::memberId))
							.toList(),
					d.dinersAtHome(), d.dinersTakeaway(), m.getExtraServings(), d.totalServings(), m.isLocked(),
					m.getEmptyReason(), recipe);
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
}
