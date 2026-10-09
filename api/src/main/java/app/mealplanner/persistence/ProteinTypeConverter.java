package app.mealplanner.persistence;

import app.mealplanner.domain.catalogue.ProteinType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a recipe's protein type as its code (a foreign key to {@code protein}). */
@Converter
public class ProteinTypeConverter implements AttributeConverter<ProteinType, String> {

	@Override
	public String convertToDatabaseColumn(ProteinType protein) {
		return protein == null ? null : protein.code();
	}

	@Override
	public ProteinType convertToEntityAttribute(String code) {
		return code == null ? null : new ProteinType(code);
	}
}
