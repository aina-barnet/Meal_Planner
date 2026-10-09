package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.nutrition.ActivityLevel;
import app.mealplanner.domain.nutrition.Sex;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	private Sex sex;

	@Column(name = "weight_kg")
	private BigDecimal weightKg;

	@Column(name = "height_cm")
	private BigDecimal heightCm;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_level")
	private ActivityLevel activityLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "diet_type")
	private DietType dietType;

	@Column(name = "diet_notes")
	private String dietNotes;

	@Column(name = "kcal_override")
	private Integer kcalOverride;

	@Column(name = "main_meals_pct")
	private int mainMealsPct;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "member_allergen", joinColumns = @JoinColumn(name = "member_id"))
	@MapKeyColumn(name = "allergen")
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "kind")
	@Enumerated(EnumType.STRING)
	private Map<Allergen, AllergenKind> allergens = new EnumMap<>(Allergen.class);

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "member_dislike", joinColumns = @JoinColumn(name = "member_id"))
	@Column(name = "ingredient_id")
	private Set<Long> dislikedIngredientIds = new HashSet<>();

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

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(BigDecimal weightKg) {
		this.weightKg = weightKg;
	}

	public BigDecimal getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(BigDecimal heightCm) {
		this.heightCm = heightCm;
	}

	public ActivityLevel getActivityLevel() {
		return activityLevel;
	}

	public void setActivityLevel(ActivityLevel activityLevel) {
		this.activityLevel = activityLevel;
	}

	public DietType getDietType() {
		return dietType;
	}

	public void setDietType(DietType dietType) {
		this.dietType = dietType;
	}

	public String getDietNotes() {
		return dietNotes;
	}

	public void setDietNotes(String dietNotes) {
		this.dietNotes = dietNotes;
	}

	public Integer getKcalOverride() {
		return kcalOverride;
	}

	public void setKcalOverride(Integer kcalOverride) {
		this.kcalOverride = kcalOverride;
	}

	public int getMainMealsPct() {
		return mainMealsPct;
	}

	public void setMainMealsPct(int mainMealsPct) {
		this.mainMealsPct = mainMealsPct;
	}

	public Map<Allergen, AllergenKind> getAllergens() {
		return allergens;
	}

	public Set<Long> getDislikedIngredientIds() {
		return dislikedIngredientIds;
	}
}
