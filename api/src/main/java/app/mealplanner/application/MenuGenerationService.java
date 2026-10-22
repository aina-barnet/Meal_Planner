package app.mealplanner.application;

import app.mealplanner.application.CatalogueService.RecipeView;
import app.mealplanner.application.WeekPlanService.WeekDetail;
import app.mealplanner.domain.generator.MenuGenerator;
import app.mealplanner.domain.generator.MenuGenerator.PastUse;
import app.mealplanner.domain.generator.MenuGenerator.Result;
import app.mealplanner.domain.generator.MenuGenerator.Slot;
import app.mealplanner.domain.planning.MealSlot;
import app.mealplanner.persistence.MealEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.Repositories.MealRepository;
import app.mealplanner.persistence.WeekPlanEntity;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maps a week to the domain {@link MenuGenerator} and writes its proposal back. */
@Service
@Transactional
public class MenuGenerationService {

	private final WeekPlanService weeks;
	private final MealRepository meals;
	private final HouseholdService household;
	private final CatalogueService catalogue;

	public MenuGenerationService(WeekPlanService weeks, MealRepository meals, HouseholdService household,
			CatalogueService catalogue) {
		this.weeks = weeks;
		this.meals = meals;
		this.household = household;
		this.catalogue = catalogue;
	}

	/** Proposes a dish for every meal that is not locked. Locked meals keep theirs. */
	public WeekDetail generate(long weekId) {
		WeekPlanEntity week = weeks.draft(weekId);
		Map<Long, MemberEntity> members = weeks.membersById();

		List<Slot> slots = week.getMeals().stream()
				.map(meal -> new Slot(meal.getId(), meal.getDate(), meal.getSlot() == MealSlot.LUNCH ? 0 : 1,
						weeks.diners(meal, members),
						meal.isLocked() && meal.getRecipeId() != null ? catalogue.recipe(meal.getRecipeId()).recipe() : null))
				.toList();

		List<PastUse> history = meals.findPlannedElsewhere(household.require().getId(), week.getId(),
				week.getWeekStart().minusDays(MenuGenerator.RECENCY_DAYS), week.getWeekStart().minusDays(1))
				.stream()
				.map(meal -> new PastUse(meal.getRecipeId(), meal.getDate()))
				.toList();

		List<app.mealplanner.domain.catalogue.Recipe> recipes = catalogue.recipes().stream()
				.map(RecipeView::recipe)
				.toList();
		List<Result> results = new MenuGenerator(recipes, new Random()).generate(slots, history);

		Map<Long, MealEntity> byId = week.getMeals().stream()
				.collect(Collectors.toMap(MealEntity::getId, Function.identity()));
		for (Result result : results) {
			MealEntity meal = byId.get(result.mealId());
			switch (result) {
				case Result.Assigned assigned -> {
					meal.setRecipeId(assigned.recipe().id());
					meal.setEmptyReason(null);
				}
				case Result.Empty empty -> {
					meal.setRecipeId(null);
					meal.setLocked(false);
					meal.setEmptyReason(Labels.noCandidates(empty.eliminatedBy()));
				}
			}
		}
		return weeks.detail(weekId);
	}
}
