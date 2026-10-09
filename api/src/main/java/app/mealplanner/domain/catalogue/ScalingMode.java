package app.mealplanner.domain.catalogue;

/** How an ingredient quantity responds to cooking more or fewer servings than the recipe base. */
public enum ScalingMode {
	/** Scales with servings. */
	LINEAR,
	/** Does not scale: a bay leaf, a pinch of salt. */
	FIXED,
	/** Scales at half the rate of the servings. */
	SUBLINEAR
}
