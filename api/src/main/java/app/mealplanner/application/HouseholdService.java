package app.mealplanner.application;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.planning.Diner;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.persistence.AllergenKind;
import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.Repositories.HouseholdRepository;
import app.mealplanner.persistence.Repositories.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HouseholdService {

	private final HouseholdRepository households;
	private final MemberRepository members;

	public HouseholdService(HouseholdRepository households, MemberRepository members) {
		this.households = households;
		this.members = members;
	}

	public record MemberData(
			String name,
			DietType dietType,
			Map<Allergen, AllergenKind> allergens) {
	}

	@Transactional(readOnly = true)
	public Optional<HouseholdEntity> find() {
		return households.findFirstByOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public HouseholdEntity require() {
		return find().orElseThrow(() -> new Errors.NotFound("HOUSEHOLD_NOT_SET_UP",
				"Encara no s'ha configurat la llar."));
	}

	/** Creates the household on first use, renames it afterwards. */
	public HouseholdEntity save(String name) {
		HouseholdEntity household = find().orElseGet(() -> new HouseholdEntity(name));
		household.setName(name.strip());
		return households.save(household);
	}

	@Transactional(readOnly = true)
	public List<MemberEntity> members() {
		return find().map(h -> members.findByHouseholdIdOrderByIdAsc(h.getId())).orElse(List.of());
	}

	@Transactional(readOnly = true)
	public MemberEntity member(long id) {
		return members.findById(id).orElseThrow(() -> new Errors.NotFound("MEMBER_NOT_FOUND",
				"No existeix el membre " + id + "."));
	}

	public MemberEntity createMember(MemberData data) {
		MemberEntity member = new MemberEntity(require());
		apply(member, data);
		return members.save(member);
	}

	public MemberEntity updateMember(long id, MemberData data) {
		MemberEntity member = member(id);
		apply(member, data);
		return members.save(member);
	}

	public void deleteMember(long id) {
		members.delete(member(id));
	}

	public Diner diner(MemberEntity member, DinerLocation location) {
		return new Diner(member.getId(), member.getName(), location, member.getDietType(),
				member.getAllergens().keySet());
	}

	private void apply(MemberEntity member, MemberData data) {
		member.setName(data.name().strip());
		member.setDietType(data.dietType());
		member.getAllergens().clear();
		member.getAllergens().putAll(data.allergens());
	}
}
