package app.mealplanner.persistence;

import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.domain.planning.MealSlot;
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
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "meal")
public class MealEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "week_plan_id")
	private WeekPlanEntity weekPlan;

	@Column(name = "meal_date")
	private LocalDate date;

	@Enumerated(EnumType.STRING)
	private MealSlot slot;

	@Column(name = "recipe_id")
	private Long recipeId;

	@Column(name = "extra_servings")
	private int extraServings;

	private boolean locked;

	@Column(name = "empty_reason")
	private String emptyReason;

	/** Member id → where they eat this meal. Members not present do not eat it. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "meal_attendee", joinColumns = @JoinColumn(name = "meal_id"))
	@MapKeyColumn(name = "member_id")
	@Column(name = "location")
	@Enumerated(EnumType.STRING)
	private Map<Long, DinerLocation> attendees = new HashMap<>();

	protected MealEntity() {
	}

	public MealEntity(WeekPlanEntity weekPlan, LocalDate date, MealSlot slot) {
		this.weekPlan = weekPlan;
		this.date = date;
		this.slot = slot;
	}

	public Long getId() {
		return id;
	}

	public WeekPlanEntity getWeekPlan() {
		return weekPlan;
	}

	public LocalDate getDate() {
		return date;
	}

	public MealSlot getSlot() {
		return slot;
	}

	public Long getRecipeId() {
		return recipeId;
	}

	public void setRecipeId(Long recipeId) {
		this.recipeId = recipeId;
	}

	public int getExtraServings() {
		return extraServings;
	}

	public void setExtraServings(int extraServings) {
		this.extraServings = extraServings;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public String getEmptyReason() {
		return emptyReason;
	}

	public void setEmptyReason(String emptyReason) {
		this.emptyReason = emptyReason;
	}

	public Map<Long, DinerLocation> getAttendees() {
		return attendees;
	}
}
