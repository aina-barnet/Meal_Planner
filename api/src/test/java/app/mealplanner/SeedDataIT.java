package app.mealplanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that the Flyway migrations apply cleanly and that the seeded catalogue is complete
 * enough for the app to be useful from the first launch.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SeedDataIT {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void seedsAtLeastFortyRecipes() {
		assertThat(count("SELECT count(*) FROM recipe")).isGreaterThanOrEqualTo(40);
	}

	@Test
	void everyRecipeHasIngredientsAndSteps() {
		assertThat(jdbc.queryForList("""
				SELECT r.code FROM recipe r
				WHERE NOT EXISTS (SELECT 1 FROM recipe_ingredient ri WHERE ri.recipe_id = r.id)
				   OR NOT EXISTS (SELECT 1 FROM recipe_step s WHERE s.recipe_id = r.id)
				""", String.class)).isEmpty();
	}

	@Test
	void everyRecipeUnitConvertsToTheIngredientCanonicalUnit() {
		List<String> unconvertible = jdbc.queryForList("""
				SELECT r.code || ' / ' || i.code || ' / ' || ri.unit
				FROM recipe_ingredient ri
				JOIN recipe r ON r.id = ri.recipe_id
				JOIN ingredient i ON i.id = ri.ingredient_id
				WHERE NOT (
				      (ri.unit IN ('G','KG') AND i.canonical_unit = 'G')
				   OR (ri.unit IN ('ML','CL','L') AND i.canonical_unit = 'ML')
				   OR EXISTS (SELECT 1 FROM ingredient_unit_conversion c
				              WHERE c.ingredient_id = i.id AND c.unit = ri.unit))
				""", String.class);
		assertThat(unconvertible).isEmpty();
	}

	@Test
	void hasEnoughPortableRecipesForAWeekOfLunchboxesAcrossProteins() {
		assertThat(count("SELECT count(*) FROM recipe WHERE portable")).isGreaterThanOrEqualTo(20);
		assertThat(count("SELECT count(DISTINCT protein_type) FROM recipe WHERE portable"))
				.isGreaterThanOrEqualTo(6);
	}

	@Test
	void hasPortableVeganRecipes() {
		// Vegan = no ingredient from an animal category.
		long veganPortable = count("""
				SELECT count(*) FROM recipe r
				WHERE r.portable AND NOT EXISTS (
				    SELECT 1 FROM recipe_ingredient ri JOIN ingredient i ON i.id = ri.ingredient_id
				    WHERE ri.recipe_id = r.id AND i.category IN ('MEAT','FISH','SEAFOOD','DAIRY','EGG'))
				""");
		assertThat(veganPortable).isGreaterThanOrEqualTo(5);
	}

	private long count(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}
}
