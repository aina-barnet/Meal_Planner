package app.mealplanner.web;

import app.mealplanner.application.ProteinService;
import app.mealplanner.application.ProteinService.ProteinView;
import app.mealplanner.web.Dtos.ProteinRequest;
import app.mealplanner.web.Dtos.ProteinResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proteins")
@Tag(name = "Proteins", description = "Protein types used to vary the menu; seeded ones and the family's own")
public class ProteinController {

	private final ProteinService proteins;

	public ProteinController(ProteinService proteins) {
		this.proteins = proteins;
	}

	@GetMapping
	public List<ProteinResponse> list() {
		return proteins.list().stream().map(ProteinController::response).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Add a protein type of the family's own")
	public ProteinResponse create(@Valid @RequestBody ProteinRequest request) {
		return response(proteins.create(request.name()));
	}

	@PutMapping("/{code}")
	@Operation(summary = "Rename a protein type", description = "Seeded or own; recipes keep referring to it by code.")
	public ProteinResponse rename(@PathVariable String code, @Valid @RequestBody ProteinRequest request) {
		return response(proteins.rename(code, request.name()));
	}

	@DeleteMapping("/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a protein type", description = "Seeded or own. 409 PROTEIN_IN_USE while recipes use it.")
	public void delete(@PathVariable String code) {
		proteins.delete(code);
	}

	private static ProteinResponse response(ProteinView v) {
		return new ProteinResponse(v.code(), v.name(), v.custom(), v.recipes());
	}
}
