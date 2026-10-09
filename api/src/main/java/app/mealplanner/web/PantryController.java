package app.mealplanner.web;

import app.mealplanner.application.CatalogueService;
import app.mealplanner.application.PantryService;
import app.mealplanner.domain.catalogue.Ingredient;
import app.mealplanner.web.Dtos.PantryItemResponse;
import app.mealplanner.web.Dtos.PantryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pantry")
@Tag(name = "Pantry", description = "What is at home, in canonical units (g or ml)")
public class PantryController {

	private final PantryService pantry;
	private final CatalogueService catalogue;

	public PantryController(PantryService pantry, CatalogueService catalogue) {
		this.pantry = pantry;
		this.catalogue = catalogue;
	}

	@GetMapping
	public List<PantryItemResponse> list() {
		return pantry.items().stream()
				.map(item -> {
					Ingredient i = catalogue.ingredient(item.getIngredientId());
					return new PantryItemResponse(i.id(), i.name(), i.canonicalUnit(), i.aisle(), item.getQuantity());
				})
				.sorted(Comparator.comparing(PantryItemResponse::name,
						Collator.getInstance(Locale.forLanguageTag("ca"))::compare))
				.toList();
	}

	@PutMapping("/{ingredientId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Set the quantity at home", description = "Zero removes the item.")
	public void set(@PathVariable long ingredientId, @Valid @RequestBody PantryRequest request) {
		pantry.set(ingredientId, request.quantity());
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Empty the pantry")
	public void clear() {
		pantry.clear();
	}

	@DeleteMapping("/{ingredientId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable long ingredientId) {
		pantry.remove(ingredientId);
	}
}
