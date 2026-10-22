package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.ProteinType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe: seeded by migrations or added by the family ({@code custom}). Any recipe can be
 * edited, and hidden rather than deleted so that past weeks keep working.
 */
@Entity
@Table(name = "recipe")
public class RecipeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String code;

	private String name;

	@Column(name = "base_servings")
	private int baseServings;

	@Column(name = "prep_minutes")
	private int prepMinutes;

	@Convert(converter = ProteinTypeConverter.class)
	@Column(name = "protein_type")
	private ProteinType proteinType;

	private boolean portable;

	private boolean custom;

	private boolean hidden;

	@ElementCollection
	@CollectionTable(name = "recipe_step", joinColumns = @JoinColumn(name = "recipe_id"))
	@OrderBy("position")
	private List<Step> steps = new ArrayList<>();

	@OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

	protected RecipeEntity() {
	}

	/** A new recipe added by the family. */
	public static RecipeEntity custom(String code) {
		RecipeEntity entity = new RecipeEntity();
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

	public int getBaseServings() {
		return baseServings;
	}

	public void setBaseServings(int baseServings) {
		this.baseServings = baseServings;
	}

	public int getPrepMinutes() {
		return prepMinutes;
	}

	public void setPrepMinutes(int prepMinutes) {
		this.prepMinutes = prepMinutes;
	}

	public ProteinType getProteinType() {
		return proteinType;
	}

	public void setProteinType(ProteinType proteinType) {
		this.proteinType = proteinType;
	}

	public boolean isPortable() {
		return portable;
	}

	public void setPortable(boolean portable) {
		this.portable = portable;
	}

	public boolean isCustom() {
		return custom;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public List<RecipeIngredientEntity> getIngredients() {
		return ingredients;
	}

	@Embeddable
	public static class Step {

		private int position;

		private String instruction;

		protected Step() {
		}

		public Step(int position, String instruction) {
			this.position = position;
			this.instruction = instruction;
		}

		public int getPosition() {
			return position;
		}

		public String getInstruction() {
			return instruction;
		}
	}
}
