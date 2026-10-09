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

@Entity
@Table(name = "shopping_list_item")
public class ShoppingListItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "shopping_list_id")
	private ShoppingListEntity shoppingList;

	@Column(name = "ingredient_id")
	private Long ingredientId;

	@Column(name = "required_qty")
	private BigDecimal requiredQty;

	@Column(name = "from_pantry_qty")
	private BigDecimal fromPantryQty;

	@Column(name = "to_buy_qty")
	private BigDecimal toBuyQty;

	private int packages;

	@Column(name = "package_size")
	private BigDecimal packageSize;

	@Column(name = "leftover_qty")
	private BigDecimal leftoverQty;

	private boolean checked;

	protected ShoppingListItemEntity() {
	}

	public ShoppingListItemEntity(ShoppingListEntity shoppingList, Long ingredientId, BigDecimal requiredQty,
			BigDecimal fromPantryQty, BigDecimal toBuyQty, int packages, BigDecimal packageSize,
			BigDecimal leftoverQty) {
		this.shoppingList = shoppingList;
		this.ingredientId = ingredientId;
		this.requiredQty = requiredQty;
		this.fromPantryQty = fromPantryQty;
		this.toBuyQty = toBuyQty;
		this.packages = packages;
		this.packageSize = packageSize;
		this.leftoverQty = leftoverQty;
	}

	public Long getId() {
		return id;
	}

	public Long getIngredientId() {
		return ingredientId;
	}

	public BigDecimal getRequiredQty() {
		return requiredQty;
	}

	public BigDecimal getFromPantryQty() {
		return fromPantryQty;
	}

	public BigDecimal getToBuyQty() {
		return toBuyQty;
	}

	public int getPackages() {
		return packages;
	}

	public BigDecimal getPackageSize() {
		return packageSize;
	}

	public BigDecimal getLeftoverQty() {
		return leftoverQty;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}
}
