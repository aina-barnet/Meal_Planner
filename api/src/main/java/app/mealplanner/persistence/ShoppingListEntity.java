package app.mealplanner.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Snapshot of the shopping list taken when a week is confirmed. */
@Entity
@Table(name = "shopping_list")
public class ShoppingListEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "week_plan_id")
	private WeekPlanEntity weekPlan;

	@Column(name = "generated_at")
	private Instant generatedAt;

	@OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ShoppingListItemEntity> items = new ArrayList<>();

	protected ShoppingListEntity() {
	}

	public ShoppingListEntity(WeekPlanEntity weekPlan, Instant generatedAt) {
		this.weekPlan = weekPlan;
		this.generatedAt = generatedAt;
	}

	public Long getId() {
		return id;
	}

	public WeekPlanEntity getWeekPlan() {
		return weekPlan;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public List<ShoppingListItemEntity> getItems() {
		return items;
	}
}
