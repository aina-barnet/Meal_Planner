package app.mealplanner.domain.generator;

import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.NutritionCalculator;
import app.mealplanner.domain.catalogue.Recipe;
import app.mealplanner.domain.planning.AssignmentViolation;
import app.mealplanner.domain.planning.Diner;
import app.mealplanner.domain.planning.MealAssignmentRules;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Proposes a dish for every meal of a week with rules plus scoring. No solver, no LLM.
 *
 * <h2>How it works</h2>
 *
 * Meals are filled in chronological order (Monday lunch, Monday dinner, Tuesday lunch…). Locked
 * meals keep their dish and count as already decided from the start, so the rest of the week
 * varies around them.
 *
 * <p><b>1. Hard constraints.</b> For each meal, the catalogue is filtered with
 * {@link MealAssignmentRules}: if anyone takes the meal in a lunchbox the dish must be portable,
 * no diner may be allergic or intolerant to any of its allergens, and every diner's diet
 * (vegetarian, vegan…) must allow all of its ingredients. If nothing survives, the meal is left
 * empty and the result says which constraints eliminated how many recipes. Nothing invalid is
 * ever proposed to fill the gap.
 *
 * <p><b>2. Scoring.</b> Every surviving recipe gets a score in roughly [0, 1], a weighted sum of:
 * <ul>
 *   <li><b>kcal fit (35 %)</b>: one serving per diner compared with the meal's aggregate target
 *       (the sum of the diners' per-meal targets). 1 when it matches exactly, falling linearly to
 *       0 when it is 50 % or more off. Meals without a target score 0.5.</li>
 *   <li><b>protein rotation (25 %)</b>: how long ago the same protein source (fish, legumes,
 *       poultry…) was last served in the previous two days. Never in that window: 1. Two days
 *       ago: 0.6. Yesterday: 0.2. Earlier the same day: 0.</li>
 *   <li><b>variety (20 %)</b>: 60 % for the share of the recipe's vegetables not yet used this
 *       week (0.5 for recipes without vegetables), 40 % for cuisine: 1 minus the share of the
 *       last three meals with the same cuisine.</li>
 *   <li><b>recency (20 %)</b>: 0 if the dish is already on this week's menu. Otherwise it grows
 *       linearly with the days since it was last planned in a previous week, reaching 1 at 28
 *       days, or if it has never been planned.</li>
 * </ul>
 * Each diner who dislikes one of the recipe's ingredients subtracts 0.15. Dislikes are soft, so
 * the recipe stays eligible.
 *
 * <p><b>3. Choice.</b> Dishes already on this week's menu are set aside while any other candidate
 * remains, so a week only repeats a dish when the constraints leave nothing else. Of the rest,
 * the five best-scoring recipes are kept and one is drawn at random with
 * probability proportional to score³. The cube strongly favours the best fits but still varies,
 * so generating the same week twice gives different menus. The random generator is injected,
 * which makes tests reproducible.
 */
public final class MenuGenerator {

	static final double WEIGHT_KCAL = 0.35;
	static final double WEIGHT_PROTEIN = 0.25;
	static final double WEIGHT_VARIETY = 0.20;
	static final double WEIGHT_RECENCY = 0.20;
	static final double DISLIKE_PENALTY = 0.15;
	static final int TOP_CANDIDATES = 5;
	public static final int RECENCY_DAYS = 28;

	private final List<Recipe> catalogue;
	private final Map<Long, Double> kcalPerServing;
	private final Map<Long, Set<Long>> vegetables;
	private final RandomGenerator random;

	public MenuGenerator(List<Recipe> catalogue, RandomGenerator random) {
		this.catalogue = List.copyOf(catalogue);
		this.random = random;
		this.kcalPerServing = this.catalogue.stream()
				.collect(Collectors.toMap(Recipe::id, r -> NutritionCalculator.perServing(r).kcal()));
		this.vegetables = this.catalogue.stream()
				.collect(Collectors.toMap(Recipe::id, r -> r.ingredients().stream()
						.filter(line -> line.ingredient().category() == IngredientCategory.VEGETABLE)
						.map(line -> line.ingredient().id())
						.collect(Collectors.toSet())));
	}

	/** One meal to fill. */
	public record Slot(long mealId, LocalDate date, int order, List<Diner> diners, int kcalTarget, Recipe locked) {

		/** @param order position within the day: 0 for lunch, 1 for dinner */
		public Slot {
			diners = List.copyOf(diners);
		}

		boolean before(Slot other) {
			return date.isBefore(other.date) || (date.equals(other.date) && order < other.order);
		}
	}

	/** A dish planned in a previous week, for recency. */
	public record PastUse(long recipeId, LocalDate date) {
	}

	public sealed interface Result {

		long mealId();

		/** A dish was chosen (or kept, for locked meals). */
		record Assigned(long mealId, Recipe recipe, boolean kept, double score) implements Result {
		}

		/** No recipe satisfies the hard constraints; how many recipes each constraint ruled out. */
		record Empty(long mealId, Map<AssignmentViolation, Long> eliminatedBy) implements Result {
		}
	}

	public List<Result> generate(List<Slot> slots, List<PastUse> history) {
		List<Slot> ordered = slots.stream()
				.sorted(Comparator.comparing(Slot::date).thenComparingInt(Slot::order))
				.toList();
		Week week = new Week(history);
		ordered.stream().filter(s -> s.locked() != null).forEach(s -> week.add(s, s.locked()));

		List<Result> results = new ArrayList<>();
		for (Slot slot : ordered) {
			if (slot.locked() != null) {
				results.add(new Result.Assigned(slot.mealId(), slot.locked(), true, 0));
				continue;
			}
			List<Recipe> candidates = catalogue.stream()
					.filter(recipe -> MealAssignmentRules.allows(recipe, slot.diners()))
					.toList();
			if (candidates.isEmpty()) {
				results.add(new Result.Empty(slot.mealId(), eliminatedBy(slot)));
				continue;
			}
			List<Recipe> unused = candidates.stream().filter(recipe -> !week.contains(recipe)).toList();
			if (!unused.isEmpty()) {
				candidates = unused;
			}
			Scored chosen = choose(candidates.stream()
					.map(recipe -> new Scored(recipe, score(recipe, slot, week)))
					.sorted(Comparator.comparingDouble(Scored::score).reversed())
					.limit(TOP_CANDIDATES)
					.toList());
			week.add(slot, chosen.recipe());
			results.add(new Result.Assigned(slot.mealId(), chosen.recipe(), false, chosen.score()));
		}
		return results;
	}

	// ------------------------------------------------------------------ scoring

	double score(Recipe recipe, Slot slot, Week week) {
		double score = WEIGHT_KCAL * kcalFit(recipe, slot)
				+ WEIGHT_PROTEIN * proteinRotation(recipe, slot, week)
				+ WEIGHT_VARIETY * variety(recipe, slot, week)
				+ WEIGHT_RECENCY * recency(recipe, slot, week);
		return score - DISLIKE_PENALTY * dislikers(recipe, slot);
	}

	double kcalFit(Recipe recipe, Slot slot) {
		if (slot.kcalTarget() <= 0 || slot.diners().isEmpty()) {
			return 0.5;
		}
		double offered = kcalPerServing(recipe) * slot.diners().size();
		double deviation = Math.abs(offered - slot.kcalTarget()) / slot.kcalTarget();
		return Math.max(0, 1 - deviation / 0.5);
	}

	double proteinRotation(Recipe recipe, Slot slot, Week week) {
		long closest = week.decided.stream()
				.filter(d -> d.slot().before(slot))
				.filter(d -> d.recipe().proteinType().equals(recipe.proteinType()))
				.mapToLong(d -> ChronoUnit.DAYS.between(d.slot().date(), slot.date()))
				.filter(days -> days <= 2)
				.min()
				.orElse(Long.MAX_VALUE);
		if (closest == 0) {
			return 0;
		}
		if (closest == 1) {
			return 0.2;
		}
		if (closest == 2) {
			return 0.6;
		}
		return 1;
	}

	double variety(Recipe recipe, Slot slot, Week week) {
		Set<Long> recipeVegetables = vegetables.getOrDefault(recipe.id(), Set.of());
		double vegetableScore;
		if (recipeVegetables.isEmpty()) {
			vegetableScore = 0.5;
		} else {
			Set<Long> used = new HashSet<>();
			week.decided.forEach(d -> used.addAll(vegetables.getOrDefault(d.recipe().id(), Set.of())));
			long fresh = recipeVegetables.stream().filter(v -> !used.contains(v)).count();
			vegetableScore = (double) fresh / recipeVegetables.size();
		}
		List<Decided> previous = week.decided.stream()
				.filter(d -> d.slot().before(slot))
				.sorted(Comparator.comparing((Decided d) -> d.slot().date()).thenComparingInt(d -> d.slot().order()))
				.toList();
		List<Decided> lastThree = previous.subList(Math.max(0, previous.size() - 3), previous.size());
		long sameCuisine = lastThree.stream().filter(d -> d.recipe().cuisine() == recipe.cuisine()).count();
		double cuisineScore = 1 - sameCuisine / 3.0;
		return 0.6 * vegetableScore + 0.4 * cuisineScore;
	}

	double recency(Recipe recipe, Slot slot, Week week) {
		if (week.contains(recipe)) {
			return 0;
		}
		LocalDate lastUse = week.lastUse.get(recipe.id());
		if (lastUse == null) {
			return 1;
		}
		long days = ChronoUnit.DAYS.between(lastUse, slot.date());
		return Math.max(0, Math.min(1, (double) days / RECENCY_DAYS));
	}

	private double kcalPerServing(Recipe recipe) {
		Double cached = kcalPerServing.get(recipe.id());
		return cached != null ? cached : NutritionCalculator.perServing(recipe).kcal();
	}

	private static long dislikers(Recipe recipe, Slot slot) {
		Set<Long> ingredients = recipe.ingredients().stream()
				.map(line -> line.ingredient().id())
				.collect(Collectors.toSet());
		return slot.diners().stream()
				.filter(diner -> diner.dislikedIngredients().stream().anyMatch(ingredients::contains))
				.count();
	}

	// ------------------------------------------------------------------ choice

	private Scored choose(List<Scored> top) {
		double[] weights = top.stream().mapToDouble(s -> Math.pow(Math.max(s.score(), 0.01), 3)).toArray();
		double total = 0;
		for (double weight : weights) {
			total += weight;
		}
		double draw = random.nextDouble() * total;
		for (int i = 0; i < top.size(); i++) {
			draw -= weights[i];
			if (draw < 0) {
				return top.get(i);
			}
		}
		return top.getLast();
	}

	private Map<AssignmentViolation, Long> eliminatedBy(Slot slot) {
		Map<AssignmentViolation, Long> counts = new LinkedHashMap<>();
		for (Recipe recipe : catalogue) {
			for (AssignmentViolation violation : MealAssignmentRules.violations(recipe, slot.diners())) {
				counts.merge(violation, 1L, Long::sum);
			}
		}
		return counts.entrySet().stream()
				.sorted(Map.Entry.<AssignmentViolation, Long>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	private record Scored(Recipe recipe, double score) {
	}

	private record Decided(Slot slot, Recipe recipe) {
	}

	/** What has been decided so far this week, plus previous weeks' history. */
	static final class Week {

		private final List<Decided> decided = new ArrayList<>();
		private final Map<Long, LocalDate> lastUse = new HashMap<>();

		Week(List<PastUse> history) {
			history.forEach(use -> lastUse.merge(use.recipeId(), use.date(), (a, b) -> a.isAfter(b) ? a : b));
		}

		void add(Slot slot, Recipe recipe) {
			decided.add(new Decided(slot, recipe));
		}

		boolean contains(Recipe recipe) {
			return decided.stream().anyMatch(d -> d.recipe().id() == recipe.id());
		}
	}
}
