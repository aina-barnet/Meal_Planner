package app.mealplanner.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A protein type: seeded, or added by the family ({@code custom}). */
@Entity
@Table(name = "protein")
public class ProteinEntity {

	@Id
	private String code;

	private String name;

	private boolean custom;

	private int position;

	protected ProteinEntity() {
	}

	public ProteinEntity(String code, String name, int position) {
		this.code = code;
		this.name = name;
		this.custom = true;
		this.position = position;
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

	public boolean isCustom() {
		return custom;
	}

	public int getPosition() {
		return position;
	}
}
