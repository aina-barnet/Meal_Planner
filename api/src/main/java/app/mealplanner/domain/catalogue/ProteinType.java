package app.mealplanner.domain.catalogue;

import java.util.Objects;

/**
 * Main protein source of a recipe, used to vary the menu from day to day. The catalogue ships
 * with the constants below; the family can add their own, identified by their code.
 */
public record ProteinType(String code) {

	public static final ProteinType FISH = new ProteinType("FISH");
	public static final ProteinType SEAFOOD = new ProteinType("SEAFOOD");
	public static final ProteinType POULTRY = new ProteinType("POULTRY");
	public static final ProteinType PORK = new ProteinType("PORK");
	public static final ProteinType BEEF = new ProteinType("BEEF");
	public static final ProteinType LAMB = new ProteinType("LAMB");
	public static final ProteinType RABBIT = new ProteinType("RABBIT");
	public static final ProteinType LEGUMES = new ProteinType("LEGUMES");
	public static final ProteinType EGGS = new ProteinType("EGGS");
	public static final ProteinType DAIRY = new ProteinType("DAIRY");
	public static final ProteinType VEGETABLES = new ProteinType("VEGETABLES");

	public ProteinType {
		Objects.requireNonNull(code, "code");
		if (code.isBlank()) {
			throw new IllegalArgumentException("A protein type needs a code");
		}
	}

	@Override
	public String toString() {
		return code;
	}
}
