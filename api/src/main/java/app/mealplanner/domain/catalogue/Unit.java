package app.mealplanner.domain.catalogue;

import java.math.BigDecimal;

/**
 * Units a recipe may express an ingredient quantity in.
 *
 * <p>Metric units carry a fixed factor to a canonical unit. Every other unit (a clove, a
 * tablespoon, a can) means something different per ingredient and is resolved through the
 * ingredient's own conversion table.
 */
public enum Unit {
	G(CanonicalUnit.G, "1"),
	KG(CanonicalUnit.G, "1000"),
	ML(CanonicalUnit.ML, "1"),
	CL(CanonicalUnit.ML, "10"),
	L(CanonicalUnit.ML, "1000"),
	UNIT,
	TBSP,
	TSP,
	CLOVE,
	PINCH,
	SLICE,
	BUNCH,
	SPRIG,
	CAN,
	CUP;

	private final CanonicalUnit metricBase;
	private final BigDecimal metricFactor;

	Unit() {
		this(null, null);
	}

	Unit(CanonicalUnit metricBase, String metricFactor) {
		this.metricBase = metricBase;
		this.metricFactor = metricFactor == null ? null : new BigDecimal(metricFactor);
	}

	public boolean isMetric() {
		return metricBase != null;
	}

	/** Canonical unit this metric unit belongs to; {@code null} for ingredient-specific units. */
	public CanonicalUnit metricBase() {
		return metricBase;
	}

	/** Canonical units per one of this metric unit; {@code null} for ingredient-specific units. */
	public BigDecimal metricFactor() {
		return metricFactor;
	}
}
