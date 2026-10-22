package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.Aisle;
import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.CanonicalUnit;
import app.mealplanner.domain.catalogue.IngredientCategory;
import app.mealplanner.domain.catalogue.Unit;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** An ingredient: seeded by migrations, or added by the family ({@code custom}). */
@Entity
@Table(name = "ingredient")
public class IngredientEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String code;

	private String name;

	@Enumerated(EnumType.STRING)
	private IngredientCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "canonical_unit")
	private CanonicalUnit canonicalUnit;

	@Column(name = "package_size")
	private BigDecimal packageSize;

	@Enumerated(EnumType.STRING)
	private Aisle aisle;

	@Column(name = "pantry_staple")
	private boolean pantryStaple;

	private boolean custom;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "ingredient_allergen", joinColumns = @JoinColumn(name = "ingredient_id"))
	@Column(name = "allergen")
	@Enumerated(EnumType.STRING)
	private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "ingredient_unit_conversion", joinColumns = @JoinColumn(name = "ingredient_id"))
	@MapKeyColumn(name = "unit")
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "canonical_per_unit")
	private Map<Unit, BigDecimal> conversions = new EnumMap<>(Unit.class);

	protected IngredientEntity() {
	}

	/** A new ingredient added by the family. */
	public static IngredientEntity custom(String code) {
		IngredientEntity entity = new IngredientEntity();
		entity.code = code;
		entity.custom = true;
		return entity;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IngredientCategory getCategory() {
		return category;
	}

	public void setCategory(IngredientCategory category) {
		this.category = category;
	}

	public CanonicalUnit getCanonicalUnit() {
		return canonicalUnit;
	}

	public void setCanonicalUnit(CanonicalUnit canonicalUnit) {
		this.canonicalUnit = canonicalUnit;
	}

	public BigDecimal getPackageSize() {
		return packageSize;
	}

	public void setPackageSize(BigDecimal packageSize) {
		this.packageSize = packageSize;
	}

	public Aisle getAisle() {
		return aisle;
	}

	public void setAisle(Aisle aisle) {
		this.aisle = aisle;
	}

	public boolean isPantryStaple() {
		return pantryStaple;
	}

	public void setPantryStaple(boolean pantryStaple) {
		this.pantryStaple = pantryStaple;
	}

	public boolean isCustom() {
		return custom;
	}

	public Set<Allergen> getAllergens() {
		return allergens;
	}

	public Map<Unit, BigDecimal> getConversions() {
		return conversions;
	}
}
