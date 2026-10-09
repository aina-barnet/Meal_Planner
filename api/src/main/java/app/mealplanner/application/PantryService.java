package app.mealplanner.application;

import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.PantryItemEntity;
import app.mealplanner.persistence.Repositories.PantryItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the household has at home, in canonical units (g or ml). */
@Service
@Transactional
public class PantryService {

	private final PantryItemRepository pantry;
	private final HouseholdService household;
	private final CatalogueService catalogue;

	public PantryService(PantryItemRepository pantry, HouseholdService household, CatalogueService catalogue) {
		this.pantry = pantry;
		this.household = household;
		this.catalogue = catalogue;
	}

	@Transactional(readOnly = true)
	public List<PantryItemEntity> items() {
		return household.find().map(h -> pantry.findByHouseholdId(h.getId())).orElse(List.of());
	}

	/** Sets the quantity at home; zero removes the item. */
	public void set(long ingredientId, BigDecimal quantity) {
		catalogue.ingredient(ingredientId);
		HouseholdEntity home = household.require();
		var existing = pantry.findByHouseholdIdAndIngredientId(home.getId(), ingredientId);
		if (quantity.signum() == 0) {
			existing.ifPresent(pantry::delete);
		} else if (existing.isPresent()) {
			existing.get().setQuantity(quantity);
		} else {
			pantry.save(new PantryItemEntity(home, ingredientId, quantity));
		}
	}

	/** Empties the pantry, e.g. to start over from what is really at home. */
	public void clear() {
		household.find().ifPresent(home -> pantry.deleteAll(pantry.findByHouseholdId(home.getId())));
	}

	public void remove(long ingredientId) {
		HouseholdEntity home = household.require();
		pantry.findByHouseholdIdAndIngredientId(home.getId(), ingredientId).ifPresent(pantry::delete);
	}
}
