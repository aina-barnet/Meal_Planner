package app.mealplanner.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data repositories. Kept together because each one is a one-liner. */
public final class Repositories {

	private Repositories() {
	}

	public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {
	}

	public interface ProteinRepository extends JpaRepository<ProteinEntity, String> {

		List<ProteinEntity> findAllByOrderByPositionAscNameAsc();
	}

	public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {

		long countByProteinType(app.mealplanner.domain.catalogue.ProteinType proteinType);

		@Query("select distinct r from RecipeEntity r left join fetch r.ingredients ri left join fetch ri.ingredient")
		List<RecipeEntity> findAllWithIngredients();
	}

	public interface HouseholdRepository extends JpaRepository<HouseholdEntity, Long> {

		Optional<HouseholdEntity> findFirstByOrderByIdAsc();
	}

	public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

		List<MemberEntity> findByHouseholdIdOrderByIdAsc(Long householdId);
	}

	public interface WeekPlanRepository extends JpaRepository<WeekPlanEntity, Long> {

		@EntityGraph(attributePaths = "meals")
		List<WeekPlanEntity> findByHouseholdIdOrderByWeekStartDesc(Long householdId);

		boolean existsByHouseholdIdAndWeekStart(Long householdId, LocalDate weekStart);

		@EntityGraph(attributePaths = "meals")
		Optional<WeekPlanEntity> findByHouseholdIdAndWeekStart(Long householdId, LocalDate weekStart);

		@EntityGraph(attributePaths = "meals")
		Optional<WeekPlanEntity> findWithMealsById(Long id);
	}

	public interface MealRepository extends JpaRepository<MealEntity, Long> {

		/** Dishes planned in the household's other weeks between two dates, for recency. */
		@Query("""
				select m from MealEntity m
				where m.weekPlan.household.id = :householdId and m.weekPlan.id <> :excludedWeekId
				  and m.recipeId is not null and m.date between :from and :to""")
		List<MealEntity> findPlannedElsewhere(Long householdId, Long excludedWeekId, LocalDate from, LocalDate to);
	}
}
