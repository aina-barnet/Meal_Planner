package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(name = "member")
public class MemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "household_id")
	private HouseholdEntity household;

	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "diet_type")
	private DietType dietType;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "member_allergen", joinColumns = @JoinColumn(name = "member_id"))
	@MapKeyColumn(name = "allergen")
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "kind")
	@Enumerated(EnumType.STRING)
	private Map<Allergen, AllergenKind> allergens = new EnumMap<>(Allergen.class);

	protected MemberEntity() {
	}

	public MemberEntity(HouseholdEntity household) {
		this.household = household;
	}

	public Long getId() {
		return id;
	}

	public HouseholdEntity getHousehold() {
		return household;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DietType getDietType() {
		return dietType;
	}

	public void setDietType(DietType dietType) {
		this.dietType = dietType;
	}

	public Map<Allergen, AllergenKind> getAllergens() {
		return allergens;
	}
}
