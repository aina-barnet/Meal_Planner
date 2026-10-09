package app.mealplanner.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "week_plan")
public class WeekPlanEntity {

	public enum Status {
		DRAFT,
		CONFIRMED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "household_id")
	private HouseholdEntity household;

	@Column(name = "week_start")
	private LocalDate weekStart;

	@Enumerated(EnumType.STRING)
	private Status status = Status.DRAFT;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@OneToMany(mappedBy = "weekPlan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("date, slot")
	private List<MealEntity> meals = new ArrayList<>();

	protected WeekPlanEntity() {
	}

	public WeekPlanEntity(HouseholdEntity household, LocalDate weekStart) {
		this.household = household;
		this.weekStart = weekStart;
	}

	public Long getId() {
		return id;
	}

	public LocalDate getWeekStart() {
		return weekStart;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getConfirmedAt() {
		return confirmedAt;
	}

	public void confirm(Instant at) {
		this.status = Status.CONFIRMED;
		this.confirmedAt = at;
	}

	public void reopen() {
		this.status = Status.DRAFT;
		this.confirmedAt = null;
	}

	public List<MealEntity> getMeals() {
		return meals;
	}
}
