package app.mealplanner.domain.catalogue;

/** A recipe quantity cannot be expressed in its ingredient's canonical unit. */
public class UnconvertibleUnitException extends RuntimeException {

	public UnconvertibleUnitException(Ingredient ingredient, Unit unit) {
		super("Cannot convert " + unit + " of " + ingredient.name() + " to " + ingredient.canonicalUnit());
	}
}
