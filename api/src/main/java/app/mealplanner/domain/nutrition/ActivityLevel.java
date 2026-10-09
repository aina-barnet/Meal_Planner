package app.mealplanner.domain.nutrition;

/** Physical activity level and the conventional multiplier applied to basal metabolic rate. */
public enum ActivityLevel {
	/** Little or no exercise. */
	SEDENTARY(1.2),
	/** Light exercise 1–3 days a week. */
	LIGHT(1.375),
	/** Moderate exercise 3–5 days a week. */
	MODERATE(1.55),
	/** Hard exercise 6–7 days a week. */
	ACTIVE(1.725),
	/** Very hard exercise or a physical job. */
	VERY_ACTIVE(1.9);

	private final double factor;

	ActivityLevel(double factor) {
		this.factor = factor;
	}

	public double factor() {
		return factor;
	}
}
