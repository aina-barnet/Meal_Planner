package app.mealplanner.domain.catalogue;

/** Energy and macronutrients. Used both per 100 g/ml of an ingredient and per serving of a recipe. */
public record Nutrition(double kcal, double proteinG, double carbsG, double fatG) {

	public static final Nutrition ZERO = new Nutrition(0, 0, 0, 0);

	public Nutrition {
		if (kcal < 0 || proteinG < 0 || carbsG < 0 || fatG < 0) {
			throw new IllegalArgumentException("Nutrition values cannot be negative");
		}
	}

	public Nutrition plus(Nutrition other) {
		return new Nutrition(kcal + other.kcal, proteinG + other.proteinG, carbsG + other.carbsG, fatG + other.fatG);
	}

	public Nutrition times(double factor) {
		return new Nutrition(kcal * factor, proteinG * factor, carbsG * factor, fatG * factor);
	}
}
