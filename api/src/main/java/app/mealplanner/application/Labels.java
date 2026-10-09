package app.mealplanner.application;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.planning.AssignmentViolation;
import java.util.Map;

/** Catalan wording for messages the API returns to the user. */
public final class Labels {

	private static final Map<Allergen, String> ALLERGENS = Map.ofEntries(
			Map.entry(Allergen.GLUTEN, "gluten"),
			Map.entry(Allergen.CRUSTACEANS, "crustacis"),
			Map.entry(Allergen.EGGS, "ou"),
			Map.entry(Allergen.FISH, "peix"),
			Map.entry(Allergen.PEANUTS, "cacauets"),
			Map.entry(Allergen.SOY, "soja"),
			Map.entry(Allergen.MILK, "llet"),
			Map.entry(Allergen.TREE_NUTS, "fruits de closca"),
			Map.entry(Allergen.CELERY, "api"),
			Map.entry(Allergen.MUSTARD, "mostassa"),
			Map.entry(Allergen.SESAME, "sèsam"),
			Map.entry(Allergen.SULPHITES, "sulfits"),
			Map.entry(Allergen.LUPIN, "tramussos"),
			Map.entry(Allergen.MOLLUSCS, "mol·luscs"));

	private static final Map<DietType, String> DIETS = Map.of(
			DietType.OMNIVORE, "omnívora",
			DietType.PESCATARIAN, "pescetariana",
			DietType.VEGETARIAN, "vegetariana",
			DietType.VEGAN, "vegana");

	private static final Map<app.mealplanner.domain.catalogue.Unit, String> UNITS = Map.ofEntries(
			Map.entry(app.mealplanner.domain.catalogue.Unit.G, "g"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.KG, "kg"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.ML, "ml"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.CL, "cl"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.L, "l"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.UNIT, "unitats"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.TBSP, "cullerades"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.TSP, "culleradetes"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.CLOVE, "grans"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.PINCH, "pessics"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.SLICE, "llesques"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.BUNCH, "manats"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.SPRIG, "branques"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.CAN, "llaunes"),
			Map.entry(app.mealplanner.domain.catalogue.Unit.CUP, "tasses"));

	private Labels() {
	}

	public static String unit(app.mealplanner.domain.catalogue.Unit unit) {
		return UNITS.get(unit);
	}

	public static String allergen(Allergen allergen) {
		return ALLERGENS.get(allergen);
	}

	public static String diet(DietType diet) {
		return DIETS.get(diet);
	}

	/** Why the generator left a meal empty, e.g. "Cap plat compatible: 14 no aguanten la carmanyola; …". */
	public static String noCandidates(Map<AssignmentViolation, Long> eliminatedBy) {
		String reasons = eliminatedBy.entrySet().stream()
				.limit(3)
				.map(e -> e.getValue() + " " + switch (e.getKey()) {
					case AssignmentViolation.NotPortable v -> "no aguanten la carmanyola";
					case AssignmentViolation.ContainsAllergen v -> "contenen " + allergen(v.allergen()) + " (" + v.dinerName() + ")";
					case AssignmentViolation.IncompatibleDiet v -> "no són aptes per a la dieta " + diet(v.diet()) + " de "
							+ v.dinerName();
				})
				.collect(java.util.stream.Collectors.joining("; "));
		return "Cap plat del catàleg és compatible amb qui menja: " + reasons + ".";
	}

	public static String describe(AssignmentViolation violation, String recipeName) {
		return switch (violation) {
			case AssignmentViolation.NotPortable v -> "«" + recipeName + "» no aguanta bé la carmanyola i "
					+ (v.dinersTakeaway() == 1 ? "1 persona se l'emporta" : v.dinersTakeaway() + " persones se l'emporten")
					+ " en aquest àpat.";
			case AssignmentViolation.ContainsAllergen v -> "«" + recipeName + "» conté " + allergen(v.allergen())
					+ " i " + v.dinerName() + " hi és al·lèrgic/a o intolerant.";
			case AssignmentViolation.IncompatibleDiet v -> "«" + recipeName + "» no és compatible amb la dieta "
					+ diet(v.diet()) + " de " + v.dinerName() + ".";
		};
	}
}
