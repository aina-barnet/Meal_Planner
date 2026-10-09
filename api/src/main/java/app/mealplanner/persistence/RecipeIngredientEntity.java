package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.ScalingMode;
import app.mealplanner.domain.catalogue.Unit;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredientEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipe_id")
	private RecipeEntity recipe;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ingredient_id")
	private IngredientEntity ingredient;

	private int position;

	private BigDecimal quantity;

	@Enumerated(EnumType.STRING)
	private Unit unit;

	@Enumerated(EnumType.STRING)
	@Column(name = "scaling_mode")
	private ScalingMode scalingMode;

	protected RecipeIngredientEntity() {
	}

	public RecipeIngredientEntity(RecipeEntity recipe, int position) {
		this.recipe = recipe;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public IngredientEntity getIngredient() {
		return ingredient;
	}

	public int getPosition() {
		return position;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public Unit getUnit() {
		return unit;
	}

	public ScalingMode getScalingMode() {
		return scalingMode;
	}

	public void set(IngredientEntity ingredient, BigDecimal quantity, Unit unit, ScalingMode scalingMode) {
		this.ingredient = ingredient;
		this.quantity = quantity;
		this.unit = unit;
		this.scalingMode = scalingMode;
	}
}
