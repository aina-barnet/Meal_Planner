package app.mealplanner.web;

import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The whole planning flow through the HTTP API, against a real PostgreSQL. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WeekFlowIT {

	private static final String MONDAY = "2026-10-12";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void cleanHousehold() {
		jdbc.execute("TRUNCATE household CASCADE");
	}

	@Test
	void householdIsMissingUntilSetUp() {
		assertThat(mvc.get().uri("/api/household")).hasStatus(404)
				.bodyJson().extractingPath("$.code").isEqualTo("HOUSEHOLD_NOT_SET_UP");

		assertThat(put("/api/household", "{\"name\":\"Casa Barnet\"}")).hasStatusOk()
				.bodyJson().extractingPath("$.name").isEqualTo("Casa Barnet");
		assertThat(put("/api/household", "{\"name\":\"Casa nova\"}")).hasStatusOk();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM household", Long.class)).isEqualTo(1);
	}

	@Test
	void memberComesBackWithAnOrientativeRequirement() {
		setUpHousehold();

		MvcTestResult created = post("/api/household/members", member("Anna", "FEMALE", null));

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.requirement.estimatedDailyKcal").isNotNull();
		assertThat(created).bodyJson().extractingPath("$.mainMealsPct").isEqualTo(65);
		assertThat(created).bodyJson().extractingPath("$.requirement.overridden").isEqualTo(false);

		long id = id(created);
		MvcTestResult overridden = put("/api/household/members/" + id, member("Anna", "FEMALE", 1900));
		assertThat(overridden).bodyJson().extractingPath("$.requirement.dailyKcal").isEqualTo(1900);
		assertThat(overridden).bodyJson().extractingPath("$.requirement.perMainMealKcal").isEqualTo(618);
	}

	@Test
	void rejectsInvalidMembersWithFieldErrors() {
		setUpHousehold();

		MvcTestResult result = post("/api/household/members", """
				{"name":"","birthDate":"2999-01-01","sex":"FEMALE","weightKg":-3,"heightCm":165,
				 "activityLevel":"LIGHT","dietType":"OMNIVORE"}""");

		assertThat(result).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
		List<String> fields = JsonPath.read(body(result), "$.errors[*].field");
		assertThat(fields).contains("name", "birthDate", "weightKg");
	}

	@Test
	void newWeekPlansEveryLunchAndDinnerForEveryone() {
		setUpHousehold();
		post("/api/household/members", member("Anna", "FEMALE", null));
		post("/api/household/members", member("Pau", "MALE", null));

		MvcTestResult week = post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}");

		assertThat(week).hasStatus(201).bodyJson().extractingPath("$.meals.length()").isEqualTo(14);
		assertThat(week).bodyJson().extractingPath("$.meals[0].dinersAtHome").isEqualTo(2);
		assertThat(mvc.get().uri("/api/weeks").exchange()).hasStatusOk()
				.bodyJson().extractingPath("$[0].mealCount").isEqualTo(14);
		assertThat(post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}")).hasStatus(409);
		assertThat(post("/api/weeks", "{\"weekStart\":\"2026-10-13\"}")).hasStatus(422)
				.bodyJson().extractingPath("$.code").isEqualTo("WEEK_START_NOT_MONDAY");
	}

	@Test
	void lunchboxRuleIsEnforcedByTheApi() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();

		MvcTestResult rejected = put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("lluc-forn") + "}");

		assertThat(rejected).hasStatus(422).bodyJson().extractingPath("$.code").isEqualTo("ASSIGNMENT_NOT_ALLOWED");
		List<String> reasons = JsonPath.read(body(rejected), "$.reasons");
		assertThat(reasons).singleElement().asString().contains("carmanyola");

		assertThat(put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}"))
				.hasStatusOk();
		// The non-portable dish is fine at a dinner everyone eats at home...
		assertThat(put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("lluc-forn") + "}"))
				.hasStatusOk();
		// ...but cannot be dragged onto the lunchbox lunch.
		assertThat(post("/api/weeks/" + f.weekId + "/swap",
				"{\"mealIdA\":" + f.lunchId + ",\"mealIdB\":" + f.dinnerId + "}")).hasStatus(422);
	}

	@Test
	void allowedRecipesForALunchboxMealAreAllPortable() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();

		List<Boolean> portable = JsonPath.read(body(mvc.get().uri(mealUri(f, f.lunchId) + "/allowed-recipes").exchange()),
				"$[*].portable");

		assertThat(portable).isNotEmpty().containsOnly(true);
	}

	@Test
	void allergiesAreEnforcedByTheApi() {
		setUpHousehold();
		long member = id(post("/api/household/members", """
				{"name":"Pau","birthDate":"1990-04-02","sex":"MALE","weightKg":75,"heightCm":178,
				 "activityLevel":"MODERATE","dietType":"OMNIVORE",
				 "allergens":[{"allergen":"EGGS","kind":"ALLERGY"}]}"""));
		long weekId = id(post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}"));
		long mealId = firstMealId(weekId);

		MvcTestResult rejected = put("/api/weeks/" + weekId + "/meals/" + mealId + "/recipe",
				"{\"recipeId\":" + recipe("truita-patates") + "}");

		assertThat(rejected).hasStatus(422);
		assertThat(body(rejected)).contains("Pau").contains("ou");
		assertThat(member).isPositive();
	}

	@Test
	void changingDinersDropsDishesThatNoLongerFit() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("lluc-forn") + "}");

		// Now Pau takes the dinner to work as well.
		MvcTestResult result = put("/api/weeks/" + f.weekId + "/meals", configure(f, "TAKEAWAY", "TAKEAWAY"));

		assertThat(result).hasStatusOk();
		Map<String, Object> dinner = JsonPath.read(body(result), "$.meals[1]");
		assertThat(dinner.get("recipe")).isNull();
		assertThat((String) dinner.get("emptyReason")).contains("carmanyola");
	}

	@Test
	void confirmingProducesTheShoppingListAndMovesThePantryForward() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		assertThat(post("/api/weeks/" + f.weekId + "/confirm", "")).hasStatus(422)
				.bodyJson().extractingPath("$.code").isEqualTo("UNASSIGNED_MEALS");

		put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		long eggs = ingredient("egg");
		assertThat(put("/api/pantry/" + eggs, "{\"quantity\":100}")).hasStatus(204);

		assertThat(post("/api/weeks/" + f.weekId + "/confirm", "")).hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");

		// Two truites for 2 diners each: 6 eggs per 4 servings → 3 eggs per meal → 360 g.
		// 100 g at home → buy 260 g → 1 dozen (720 g), 460 g left over.
		MvcTestResult list = mvc.get().uri("/api/weeks/" + f.weekId + "/shopping-list").exchange();
		assertThat(list).hasStatusOk();
		Map<String, Object> egg = JsonPath.<List<Map<String, Object>>>read(body(list),
				"$.aisles[*].items[?(@.ingredientId == " + eggs + ")]").getFirst();
		assertThat(((Number) egg.get("required")).doubleValue()).isEqualTo(360.0);
		assertThat(((Number) egg.get("fromPantry")).doubleValue()).isEqualTo(100.0);
		assertThat(egg.get("packages")).isEqualTo(1);
		List<String> staples = JsonPath.read(body(list), "$.staples");
		assertThat(staples).contains("Oli d'oliva verge extra", "Sal");
		List<String> onTheList = JsonPath.read(body(list), "$.aisles[*].items[*].name");
		assertThat(onTheList).doesNotContain("Oli d'oliva verge extra", "Sal");

		BigDecimalAssert.isEqualTo(jdbc.queryForObject(
				"SELECT quantity FROM pantry_item WHERE ingredient_id = ?", java.math.BigDecimal.class, eggs), 460);

		long itemId = ((Number) egg.get("id")).longValue();
		assertThat(patch("/api/weeks/" + f.weekId + "/shopping-list/items/" + itemId, "{\"checked\":true}"))
				.hasStatusOk();

		assertThat(put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":null}")).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("WEEK_CONFIRMED");
	}

	@Test
	void aConfirmedWeekCanGoBackToDraftAndThePantryIsRestored() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		long eggs = ingredient("egg");
		long rice = ingredient("rice");
		put("/api/pantry/" + eggs, "{\"quantity\":100}");
		assertThat(post("/api/weeks/" + f.weekId + "/confirm", "")).hasStatusOk();
		// After confirming, the family adds rice by hand: that must survive going back to draft.
		put("/api/pantry/" + rice, "{\"quantity\":500}");

		MvcTestResult reopened = post("/api/weeks/" + f.weekId + "/reopen", "");

		assertThat(reopened).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
		assertThat(pantryQuantity(eggs)).isEqualByComparingTo("100");
		assertThat(pantryQuantity(rice)).isEqualByComparingTo("500");
		assertThat(jdbc.queryForObject("SELECT count(*) FROM pantry_item", Long.class)).isEqualTo(2);
		assertThat(mvc.get().uri("/api/weeks/" + f.weekId + "/shopping-list").exchange()).hasStatus(404);
		assertThat(put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":null}")).hasStatusOk();
		assertThat(post("/api/weeks/" + f.weekId + "/reopen", "")).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("WEEK_NOT_CONFIRMED");
	}

	@Test
	void aWeekCannotGoBackToDraftWhileALaterWeekIsConfirmed() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		post("/api/weeks/" + f.weekId + "/confirm", "");
		long nextWeek = id(post("/api/weeks", "{\"weekStart\":\"2026-10-19\"}"));
		put("/api/weeks/" + nextWeek + "/meals", """
				{"meals":[{"date":"2026-10-19","slot":"LUNCH","extraServings":0,
				  "attendees":[{"memberId":%d,"location":"HOME"}]}]}""".formatted(f.anna));
		long meal = ((Number) JsonPath.read(body(mvc.get().uri("/api/weeks/" + nextWeek).exchange()), "$.meals[0].id"))
				.longValue();
		put("/api/weeks/" + nextWeek + "/meals/" + meal + "/recipe", "{\"recipeId\":" + recipe("empedrat") + "}");
		assertThat(post("/api/weeks/" + nextWeek + "/confirm", "")).hasStatusOk();

		assertThat(post("/api/weeks/" + f.weekId + "/reopen", "")).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("LATER_WEEK_CONFIRMED");
		assertThat(post("/api/weeks/" + nextWeek + "/reopen", "")).hasStatusOk();
		assertThat(post("/api/weeks/" + f.weekId + "/reopen", "")).hasStatusOk();
	}

	@Test
	void aMenuCanBeCopiedOntoANewOrDraftWeekButNotOntoAConfirmedOne() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		put(mealUri(f, f.lunchId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("lluc-forn") + "}");
		patch(mealUri(f, f.dinnerId), "{\"extraServings\":2}");

		MvcTestResult copied = post("/api/weeks/" + f.weekId + "/copy", "{\"targetWeekStart\":\"2026-10-19\"}");

		assertThat(copied).hasStatusOk().bodyJson().extractingPath("$.weekStart").isEqualTo("2026-10-19");
		assertThat(copied).bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
		List<Map<String, Object>> meals = JsonPath.read(body(copied), "$.meals");
		assertThat(meals).extracting(m -> m.get("date")).containsExactly("2026-10-19", "2026-10-19");
		assertThat(JsonPath.<Integer>read(body(copied), "$.meals[0].dinersTakeaway")).isEqualTo(1);
		assertThat(((Number) JsonPath.read(body(copied), "$.meals[1].recipe.id")).longValue()).isEqualTo(recipe("lluc-forn"));
		assertThat(JsonPath.<Integer>read(body(copied), "$.meals[1].extraServings")).isEqualTo(2);
		long target = ((Number) JsonPath.read(body(copied), "$.id")).longValue();

		// Copying again onto the same draft replaces its meals instead of duplicating them.
		assertThat(post("/api/weeks/" + f.weekId + "/copy", "{\"targetWeekStart\":\"2026-10-19\"}")).hasStatusOk()
				.bodyJson().extractingPath("$.meals.length()").isEqualTo(2);

		assertThat(post("/api/weeks/" + target + "/confirm", "")).hasStatusOk();
		assertThat(post("/api/weeks/" + f.weekId + "/copy", "{\"targetWeekStart\":\"2026-10-19\"}")).hasStatus(409);
		assertThat(post("/api/weeks/" + f.weekId + "/copy", "{\"targetWeekStart\":\"" + MONDAY + "\"}")).hasStatus(422)
				.bodyJson().extractingPath("$.code").isEqualTo("SAME_WEEK");
	}

	@Test
	void copyingDropsDishesThatNoLongerFitTheDiners() {
		Fixture f = weekWithOneLunchboxLunchAndOneDinner();
		put(mealUri(f, f.dinnerId) + "/recipe", "{\"recipeId\":" + recipe("truita-patates") + "}");
		// Since then, Anna has become allergic to eggs.
		put("/api/household/members/" + f.anna, """
				{"name":"Anna","birthDate":"1988-06-15","sex":"FEMALE","weightKg":65,"heightCm":168,
				 "activityLevel":"LIGHT","dietType":"OMNIVORE","allergens":[{"allergen":"EGGS","kind":"ALLERGY"}]}""");

		MvcTestResult copied = post("/api/weeks/" + f.weekId + "/copy", "{\"targetWeekStart\":\"2026-10-26\"}");

		assertThat(copied).hasStatusOk();
		assertThat(JsonPath.<Object>read(body(copied), "$.meals[1].recipe")).isNull();
		assertThat(JsonPath.<String>read(body(copied), "$.meals[1].emptyReason")).contains("Anna");
	}

	@Test
	void thePantryCanBeEmptied() {
		setUpHousehold();
		put("/api/pantry/" + ingredient("egg"), "{\"quantity\":300}");
		put("/api/pantry/" + ingredient("rice"), "{\"quantity\":500}");

		assertThat(mvc.delete().uri("/api/pantry").exchange()).hasStatus(204);

		assertThat(mvc.get().uri("/api/pantry").exchange()).hasStatusOk()
				.bodyJson().extractingPath("$.length()").isEqualTo(0);
	}

	private java.math.BigDecimal pantryQuantity(long ingredientId) {
		return jdbc.queryForObject("SELECT quantity FROM pantry_item WHERE ingredient_id = ?",
				java.math.BigDecimal.class, ingredientId);
	}

	@Test
	void generatorFillsTheWeekWithValidDishesAndKeepsLockedOnes() {
		setUpHousehold();
		long anna = id(post("/api/household/members", member("Anna", "FEMALE", null)));
		long pau = id(post("/api/household/members", """
				{"name":"Pau","birthDate":"1985-09-02","sex":"MALE","weightKg":80,"heightCm":181,
				 "activityLevel":"MODERATE","dietType":"VEGETARIAN",
				 "allergens":[{"allergen":"EGGS","kind":"INTOLERANCE"}]}"""));
		long weekId = id(post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}"));
		Fixture f = new Fixture(weekId, anna, pau, 0, 0);
		MvcTestResult configured = put("/api/weeks/" + weekId + "/meals", configure(f, "TAKEAWAY", "HOME"));
		long lunch = ((Number) JsonPath.read(body(configured), "$.meals[0].id")).longValue();
		long lockedRecipe = recipe("cigrons-espinacs");
		assertThat(put("/api/weeks/" + weekId + "/meals/" + lunch + "/recipe", "{\"recipeId\":" + lockedRecipe + "}"))
				.hasStatusOk();
		patch("/api/weeks/" + weekId + "/meals/" + lunch, "{\"locked\":true}");

		MvcTestResult generated = post("/api/weeks/" + weekId + "/generate", "");

		assertThat(generated).hasStatusOk();
		List<Map<String, Object>> meals = JsonPath.read(body(generated), "$.meals");
		assertThat(meals).hasSize(2).allSatisfy(meal -> assertThat(meal.get("recipe")).isNotNull());
		assertThat(((Number) JsonPath.read(body(generated), "$.meals[0].recipe.id")).longValue()).isEqualTo(lockedRecipe);
		List<String> dinnerDiets = JsonPath.read(body(generated), "$.meals[1].recipe.diets");
		List<String> dinnerAllergens = JsonPath.read(body(generated), "$.meals[1].recipe.allergens");
		assertThat(dinnerDiets).contains("VEGETARIAN");
		assertThat(dinnerAllergens).doesNotContain("EGGS");
	}

	@Test
	void generatorLeavesImpossibleMealsEmptyWithAReason() {
		setUpHousehold();
		long id = id(post("/api/household/members", """
				{"name":"Marta","birthDate":"1990-01-01","sex":"FEMALE","weightKg":60,"heightCm":165,
				 "activityLevel":"LIGHT","dietType":"VEGAN",
				 "allergens":[{"allergen":"GLUTEN","kind":"ALLERGY"},{"allergen":"CELERY","kind":"ALLERGY"},
				              {"allergen":"SULPHITES","kind":"ALLERGY"},{"allergen":"SESAME","kind":"ALLERGY"}]}"""));
		long weekId = id(post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}"));
		put("/api/weeks/" + weekId + "/meals", """
				{"meals":[{"date":"%s","slot":"LUNCH","extraServings":0,
				  "attendees":[{"memberId":%d,"location":"TAKEAWAY"}]}]}""".formatted(MONDAY, id));

		MvcTestResult generated = post("/api/weeks/" + weekId + "/generate", "");

		assertThat(generated).hasStatusOk();
		assertThat(JsonPath.<Object>read(body(generated), "$.meals[0].recipe")).isNull();
		assertThat(JsonPath.<String>read(body(generated), "$.meals[0].emptyReason")).startsWith("Cap plat");
	}

	@Test
	void recipeCatalogueCanBeFiltered() {
		List<Boolean> portable = JsonPath.read(body(mvc.get().uri("/api/recipes?portable=true&diet=VEGAN").exchange()),
				"$[*].portable");
		assertThat(portable).hasSizeGreaterThanOrEqualTo(5).containsOnly(true);

		List<String> names = JsonPath.read(body(mvc.get().uri("/api/recipes?q=carbasso").exchange()), "$[*].name");
		assertThat(names).anyMatch(name -> name.contains("carbassó"));

		assertThat(mvc.get().uri("/api/recipes/" + recipe("fideua")).exchange()).hasStatusOk()
				.bodyJson().extractingPath("$.steps.length()").isEqualTo(4);
		assertThat(mvc.get().uri("/api/recipes/999999").exchange()).hasStatus(404);
	}

	@Test
	void documentsTheApiWithOpenApi() {
		assertThat(mvc.get().uri("/api/openapi").exchange()).hasStatusOk()
				.bodyJson().extractingPath("$.paths['/api/weeks/{weekId}/confirm']").isNotNull();
	}

	// ------------------------------------------------------------------ helpers

	private record Fixture(long weekId, long anna, long pau, long lunchId, long dinnerId) {
	}

	/** Anna and Pau; Monday lunch with Pau taking a lunchbox, Monday dinner both at home. */
	private Fixture weekWithOneLunchboxLunchAndOneDinner() {
		setUpHousehold();
		long anna = id(post("/api/household/members", member("Anna", "FEMALE", null)));
		long pau = id(post("/api/household/members", member("Pau", "MALE", null)));
		long weekId = id(post("/api/weeks", "{\"weekStart\":\"" + MONDAY + "\"}"));
		Fixture partial = new Fixture(weekId, anna, pau, 0, 0);
		MvcTestResult configured = put("/api/weeks/" + weekId + "/meals", configure(partial, "TAKEAWAY", "HOME"));
		assertThat(configured).hasStatusOk().bodyJson().extractingPath("$.meals.length()").isEqualTo(2);
		long lunch = ((Number) JsonPath.read(body(configured), "$.meals[0].id")).longValue();
		long dinner = ((Number) JsonPath.read(body(configured), "$.meals[1].id")).longValue();
		return new Fixture(weekId, anna, pau, lunch, dinner);
	}

	private static String configure(Fixture f, String pauAtLunch, String pauAtDinner) {
		return """
				{"meals":[
				  {"date":"%s","slot":"LUNCH","extraServings":0,
				   "attendees":[{"memberId":%d,"location":"HOME"},{"memberId":%d,"location":"%s"}]},
				  {"date":"%s","slot":"DINNER","extraServings":0,
				   "attendees":[{"memberId":%d,"location":"HOME"},{"memberId":%d,"location":"%s"}]}
				]}""".formatted(MONDAY, f.anna, f.pau, pauAtLunch, MONDAY, f.anna, f.pau, pauAtDinner);
	}

	private static String mealUri(Fixture f, long mealId) {
		return "/api/weeks/" + f.weekId + "/meals/" + mealId;
	}

	private void setUpHousehold() {
		assertThat(put("/api/household", "{\"name\":\"Casa\"}")).hasStatusOk();
	}

	private static String member(String name, String sex, Integer kcalOverride) {
		return """
				{"name":"%s","birthDate":"1988-06-15","sex":"%s","weightKg":65,"heightCm":168,
				 "activityLevel":"LIGHT","dietType":"OMNIVORE","kcalOverride":%s}"""
				.formatted(name, sex, kcalOverride == null ? "null" : kcalOverride.toString());
	}

	private long firstMealId(long weekId) {
		return ((Number) JsonPath.read(body(mvc.get().uri("/api/weeks/" + weekId).exchange()), "$.meals[0].id"))
				.longValue();
	}

	private long recipe(String code) {
		return jdbc.queryForObject("SELECT id FROM recipe WHERE code = ?", Long.class, code);
	}

	private long ingredient(String code) {
		return jdbc.queryForObject("SELECT id FROM ingredient WHERE code = ?", Long.class, code);
	}

	private MvcTestResult post(String uri, String json) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private MvcTestResult put(String uri, String json) {
		return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private MvcTestResult patch(String uri, String json) {
		return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private static long id(MvcTestResult result) {
		return ((Number) JsonPath.read(body(result), "$.id")).longValue();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final class BigDecimalAssert {

		static void isEqualTo(java.math.BigDecimal actual, int expected) {
			assertThat(actual).isEqualByComparingTo(java.math.BigDecimal.valueOf(expected));
		}
	}
}
