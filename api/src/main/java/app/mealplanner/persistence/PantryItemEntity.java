package app.mealplanner.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** An ingredient quantity currently at home, in the ingredient's canonical unit. */
@Entity
@Table(name = "pantry_item")
public class PantryItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "household_id")
	private HouseholdEntity household;

	@Column(name = "ingredient_id")
	private Long ingredientId;

	private BigDecimal quantity;

	protected PantryItemEntity() {
	}

	public PantryItemEntity(HouseholdEntity household, Long ingredientId, BigDecimal quantity) {
		this.household = household;
		this.ingredientId = ingredientId;
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}

	public Long getIngredientId() {
		return ingredientId;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
}
