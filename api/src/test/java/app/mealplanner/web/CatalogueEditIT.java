package app.mealplanner.web;

import static org.assertj.core.api.Assertions.assertThat;

import app.mealplanner.TestcontainersConfiguration;
import app.mealplanner.application.CatalogueService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The family's own ingredients and recipes, editing and hiding, through the HTTP API. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogueEditIT {

	private static final String TOFU = """
			{"name":"Tofu ferm","category":"LEGUME","canonicalUnit":"G","packageSize":400,"aisle":"DAIRY_EGGS",
			 "kcalPer100":144,"proteinPer100":15.7,"carbsPer100":2.3,"fatPer100":8.7,
			 "allergens":["SOY"],"conversions":{"UNIT":200},"pantryStaple":false}""";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	CatalogueService catalogue;

	@AfterEach
	void removeWhatTheTestsAdded() {
		jdbc.execute("TRUNCATE household CASCADE");
		jdbc.update("DELETE FROM recipe WHERE custom");
		jdbc.update("DELETE FROM ingredient WHERE custom");
		jdbc.update("DELETE FROM protein WHERE custom");
		jdbc.update("UPDATE recipe SET hidden = false");
		catalogue.invalidate();
	}

	@Test
	void familyCanAddAnIngredientAndARecipeThatUsesIt() {
		MvcTestResult tofu = post("/api/ingredients", TOFU);
		assertThat(tofu).hasStatus(201).bodyJson().extractingPath("$.custom").isEqualTo(true);
		long tofuId = id(tofu);

		MvcTestResult recipe = post("/api/recipes", recipe("Tofu amb verdures", tofuId, "UNIT", 2, true, "REHEAT"));

		assertThat(recipe).hasStatus(201);
		assertThat(recipe).bodyJson().extractingPath("$.summary.custom").isEqualTo(true);
		// 2 tofu blocks (400 g × 1.44) + 400 g potato (× 0.77) + 2 tbsp oil (28 g × 8.84) = 576 + 308 + 247.52 → /4
		assertThat(recipe).bodyJson().extractingPath("$.summary.nutrition.kcal").isEqualTo(283);
		List<String> allergens = JsonPath.read(body(recipe), "$.summary.allergens");
		assertThat(allergens).containsExactly("SOY");
		List<String> diets = JsonPath.read(body(recipe), "$.summary.diets");
		assertThat(diets).contains("VEGAN");

		List<String> names = JsonPath.read(body(mvc.get().uri("/api/recipes?q=tofu").exchange()), "$[*].name");
		assertThat(names).containsExactly("Tofu amb verdures");
	}

	@Test
	void rejectsUnitsTheIngredientCannotBeExpressedIn() {
		long tofuId = id(post("/api/ingredients", TOFU));

		MvcTestResult rejected = post("/api/recipes", recipe("Tofu en cullerades", tofuId, "TBSP", 2, false, null));

		assertThat(rejected).hasStatus(422).bodyJson().extractingPath("$.code").isEqualTo("INVALID_RECIPE_LINES");
		List<String> reasons = JsonPath.read(body(rejected), "$.reasons");
		assertThat(reasons).singleElement().asString().contains("Tofu ferm").contains("cullerades");
	}

	@Test
	void portableRecipesMustSayHowTheyAreEaten() {
		long tofuId = id(post("/api/ingredients", TOFU));

		assertThat(post("/api/recipes", recipe("Sense dir", tofuId, "UNIT", 1, true, null))).hasStatus(422)
				.bodyJson().extractingPath("$.code").isEqualTo("LUNCHBOX_SERVING_REQUIRED");
	}

	@Test
	void anyRecipeCanBeEditedIncludingSeededOnes() {
		long risotto = jdbc.queryForObject("SELECT id FROM recipe WHERE code = 'risotto-bolets'", Long.class);
		String original = body(mvc.get().uri("/api/recipes/" + risotto).exchange());
		int originalKcal = JsonPath.read(original, "$.summary.nutrition.kcal");

		// Same recipe for 8 instead of 4: half the kcal per serving.
		MvcTestResult edited = put("/api/recipes/" + risotto, asRequest(original).replace("\"baseServings\":4", "\"baseServings\":8"));

		assertThat(edited).hasStatusOk();
		int editedKcal = JsonPath.read(body(edited), "$.summary.nutrition.kcal");
		assertThat(editedKcal).isBetween(originalKcal / 2 - 1, originalKcal / 2 + 1);
		assertThat(put("/api/recipes/" + risotto, asRequest(original))).hasStatusOk();
	}

	@Test
	void hiddenRecipesAreNotOfferedButStayResolvable() {
		long fideua = jdbc.queryForObject("SELECT id FROM recipe WHERE code = 'fideua'", Long.class);

		assertThat(patch("/api/recipes/" + fideua, "{\"hidden\":true}")).hasStatusOk()
				.bodyJson().extractingPath("$.hidden").isEqualTo(true);

		List<Integer> visible = JsonPath.read(body(mvc.get().uri("/api/recipes").exchange()), "$[*].id");
		List<Integer> all = JsonPath.read(body(mvc.get().uri("/api/recipes?includeHidden=true").exchange()), "$[*].id");
		assertThat(visible).doesNotContain((int) fideua);
		assertThat(all).contains((int) fideua);
		assertThat(mvc.get().uri("/api/recipes/" + fideua).exchange()).hasStatusOk();

		assertThat(patch("/api/recipes/" + fideua, "{\"hidden\":false}")).hasStatusOk();
		List<Integer> again = JsonPath.read(body(mvc.get().uri("/api/recipes").exchange()), "$[*].id");
		assertThat(again).contains((int) fideua);
	}

	@Test
	void ingredientEditsCannotBreakRecipesAndSeededIngredientsAreFixed() {
		long tofuId = id(post("/api/ingredients", TOFU));
		post("/api/recipes", recipe("Tofu amb verdures", tofuId, "UNIT", 2, false, null));

		MvcTestResult broken = put("/api/ingredients/" + tofuId, TOFU.replace("\"conversions\":{\"UNIT\":200}", "\"conversions\":{}"));
		assertThat(broken).hasStatus(422).bodyJson().extractingPath("$.code").isEqualTo("INGREDIENT_IN_USE");

		assertThat(put("/api/ingredients/" + tofuId, TOFU.replace("144", "150"))).hasStatusOk()
				.bodyJson().extractingPath("$.kcalPer100").isEqualTo(150.0);

		long egg = jdbc.queryForObject("SELECT id FROM ingredient WHERE code = 'egg'", Long.class);
		assertThat(put("/api/ingredients/" + egg, TOFU.replace("Tofu ferm", "Ous nous"))).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("INGREDIENT_NOT_EDITABLE");
		assertThat(post("/api/ingredients", TOFU)).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("DUPLICATE_INGREDIENT");
	}

	@Test
	void familyCanAddTheirOwnProteinTypes() {
		MvcTestResult created = post("/api/proteins", "{\"name\":\"Tofu i seitan\"}");
		assertThat(created).hasStatus(201).bodyJson().extractingPath("$.custom").isEqualTo(true);
		String code = JsonPath.read(body(created), "$.code");
		long tofuId = id(post("/api/ingredients", TOFU));

		MvcTestResult recipe = post("/api/recipes",
				recipe("Tofu amb verdures", tofuId, "UNIT", 2, false, null).replace("\"LEGUMES\"", "\"" + code + "\""));

		assertThat(recipe).hasStatus(201).bodyJson().extractingPath("$.summary.proteinType").isEqualTo(code);
		List<Integer> used = JsonPath.read(body(mvc.get().uri("/api/proteins").exchange()),
				"$[?(@.code == '" + code + "')].recipes");
		assertThat(used).containsExactly(1);
		List<String> filtered = JsonPath.read(body(mvc.get().uri("/api/recipes?proteinType=" + code).exchange()), "$[*].name");
		assertThat(filtered).containsExactly("Tofu amb verdures");

		assertThat(mvc.delete().uri("/api/proteins/" + code).exchange()).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("PROTEIN_IN_USE");
		assertThat(put("/api/proteins/" + code, "{\"name\":\"Tofu\"}")).hasStatusOk()
				.bodyJson().extractingPath("$.name").isEqualTo("Tofu");
		// Seeded types can be renamed too; recipes keep their code.
		assertThat(put("/api/proteins/POULTRY", "{\"name\":\"Pollastre i gall dindi\"}")).hasStatusOk()
				.bodyJson().extractingPath("$.name").isEqualTo("Pollastre i gall dindi");
		assertThat(put("/api/proteins/POULTRY", "{\"name\":\"Aviram\"}")).hasStatusOk();
		assertThat(mvc.delete().uri("/api/proteins/FISH").exchange()).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("PROTEIN_IN_USE");
		assertThat(post("/api/proteins", "{\"name\":\"peix\"}")).hasStatus(409)
				.bodyJson().extractingPath("$.code").isEqualTo("DUPLICATE_PROTEIN");

		String spare = JsonPath.read(body(post("/api/proteins", "{\"name\":\"Insectes\"}")), "$.code");
		assertThat(mvc.delete().uri("/api/proteins/" + spare).exchange()).hasStatus(204);
	}

	@Test
	void recipesMustUseAnExistingProteinType() {
		long tofuId = id(post("/api/ingredients", TOFU));

		assertThat(post("/api/recipes", recipe("Misteri", tofuId, "UNIT", 1, false, null).replace("\"LEGUMES\"", "\"NOPE\"")))
				.hasStatus(422).bodyJson().extractingPath("$.code").isEqualTo("UNKNOWN_PROTEIN");
	}

	// ------------------------------------------------------------------ helpers

	private String recipe(String name, long ingredientId, String unit, int quantity, boolean portable, String serving) {
		long potato = jdbc.queryForObject("SELECT id FROM ingredient WHERE code = 'potato'", Long.class);
		long oil = jdbc.queryForObject("SELECT id FROM ingredient WHERE code = 'olive_oil'", Long.class);
		return """
				{"name":"%s","description":"Prova","baseServings":4,"prepMinutes":30,
				 "proteinType":"LEGUMES","cuisine":"OTHER","seasons":["AUTUMN","WINTER"],
				 "portable":%s,"lunchboxServing":%s,
				 "ingredients":[
				   {"ingredientId":%d,"quantity":%d,"unit":"%s","scalingMode":"LINEAR"},
				   {"ingredientId":%d,"quantity":400,"unit":"G","scalingMode":"LINEAR"},
				   {"ingredientId":%d,"quantity":2,"unit":"TBSP","scalingMode":"LINEAR"}],
				 "steps":["Talleu-ho tot.","Coeu-ho 20 minuts."]}"""
				.formatted(name, portable, serving == null ? "null" : "\"" + serving + "\"", ingredientId, quantity, unit,
						potato, oil);
	}

	/** Turns a recipe detail response back into an update request. */
	private static String asRequest(String detail) {
		var s = JsonPath.parse(detail);
		List<Object> lines = s.read("$.ingredients[*]");
		StringBuilder ingredients = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				ingredients.append(',');
			}
			ingredients.append("{\"ingredientId\":").append((Object) s.read("$.ingredients[" + i + "].ingredientId"))
					.append(",\"quantity\":").append((Object) s.read("$.ingredients[" + i + "].quantity"))
					.append(",\"unit\":\"").append((Object) s.read("$.ingredients[" + i + "].unit"))
					.append("\",\"scalingMode\":\"").append((Object) s.read("$.ingredients[" + i + "].scalingMode"))
					.append("\"}");
		}
		List<String> steps = s.read("$.steps");
		List<String> seasons = s.read("$.summary.seasons");
		String serving = s.read("$.summary.lunchboxServing");
		return "{\"name\":\"" + s.read("$.summary.name") + "\",\"description\":\"" + s.read("$.summary.description")
				+ "\",\"baseServings\":" + s.read("$.summary.baseServings") + ",\"prepMinutes\":"
				+ s.read("$.summary.prepMinutes") + ",\"proteinType\":\"" + s.read("$.summary.proteinType")
				+ "\",\"cuisine\":\"" + s.read("$.summary.cuisine") + "\",\"seasons\":" + json(seasons)
				+ ",\"portable\":" + s.read("$.summary.portable") + ",\"lunchboxServing\":"
				+ (serving == null ? "null" : "\"" + serving + "\"") + ",\"ingredients\":[" + ingredients
				+ "],\"steps\":" + json(steps) + "}";
	}

	private static String json(List<String> values) {
		return values.stream().map(v -> "\"" + v.replace("\"", "\\\"") + "\"").toList().toString();
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
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
