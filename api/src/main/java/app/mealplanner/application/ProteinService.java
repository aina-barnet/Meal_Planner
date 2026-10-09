package app.mealplanner.application;

import app.mealplanner.domain.catalogue.ProteinType;
import app.mealplanner.persistence.ProteinEntity;
import app.mealplanner.persistence.Repositories.ProteinRepository;
import app.mealplanner.persistence.Repositories.RecipeRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protein types: the seeded ones plus the family's own. Any type can be renamed, and deleted
 * while no recipe uses it. Recipes refer to the code, which never changes, so renaming is safe.
 */
@Service
@Transactional
public class ProteinService {

	private final ProteinRepository proteins;
	private final RecipeRepository recipes;

	public ProteinService(ProteinRepository proteins, RecipeRepository recipes) {
		this.proteins = proteins;
		this.recipes = recipes;
	}

	public record ProteinView(String code, String name, boolean custom, long recipes) {
	}

	@Transactional(readOnly = true)
	public List<ProteinView> list() {
		return proteins.findAllByOrderByPositionAscNameAsc().stream().map(this::view).toList();
	}

	public ProteinView create(String name) {
		String clean = name.strip();
		rejectDuplicate(clean, null);
		int position = proteins.findAll().stream().mapToInt(ProteinEntity::getPosition).max().orElse(0) + 1;
		ProteinEntity entity = new ProteinEntity("custom-" + UUID.randomUUID().toString().substring(0, 8), clean, position);
		return view(proteins.save(entity));
	}

	public ProteinView rename(String code, String name) {
		ProteinEntity entity = existing(code);
		String clean = name.strip();
		rejectDuplicate(clean, code);
		entity.setName(clean);
		return view(entity);
	}

	/** Only while no recipe uses it. */
	public void delete(String code) {
		ProteinEntity entity = existing(code);
		long used = recipes.countByProteinType(new ProteinType(code));
		if (used > 0) {
			throw new Errors.Conflict("PROTEIN_IN_USE", "No es pot esborrar «" + entity.getName() + "»: "
					+ (used == 1 ? "1 plat la fa servir." : used + " plats la fan servir."));
		}
		proteins.delete(entity);
	}

	private ProteinEntity existing(String code) {
		return proteins.findById(code).orElseThrow(() -> new Errors.NotFound("PROTEIN_NOT_FOUND",
				"No existeix el tipus de proteïna «" + code + "»."));
	}

	private void rejectDuplicate(String name, String exceptCode) {
		String wanted = name.toLowerCase(Locale.ROOT);
		boolean taken = proteins.findAll().stream()
				.anyMatch(p -> p.getName().toLowerCase(Locale.ROOT).equals(wanted) && !p.getCode().equals(exceptCode));
		if (taken) {
			throw new Errors.Conflict("DUPLICATE_PROTEIN", "Ja hi ha un tipus de proteïna que es diu «" + name + "».");
		}
	}

	private ProteinView view(ProteinEntity entity) {
		return new ProteinView(entity.getCode(), entity.getName(), entity.isCustom(),
				recipes.countByProteinType(new ProteinType(entity.getCode())));
	}
}
