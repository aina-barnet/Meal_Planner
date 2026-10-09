package app.mealplanner.application;

import app.mealplanner.domain.catalogue.Allergen;
import app.mealplanner.domain.catalogue.DietType;
import app.mealplanner.domain.nutrition.ActivityLevel;
import app.mealplanner.domain.nutrition.EnergyRequirement;
import app.mealplanner.domain.nutrition.EnergyRequirementCalculator;
import app.mealplanner.domain.nutrition.MemberProfile;
import app.mealplanner.domain.nutrition.Sex;
import app.mealplanner.domain.planning.Diner;
import app.mealplanner.domain.planning.DinerLocation;
import app.mealplanner.persistence.AllergenKind;
import app.mealplanner.persistence.HouseholdEntity;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.persistence.Repositories.HouseholdRepository;
import app.mealplanner.persistence.Repositories.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HouseholdService {

	private final HouseholdRepository households;
	private final MemberRepository members;
	private final CatalogueService catalogue;
	private final Clock clock;

	public HouseholdService(HouseholdRepository households, MemberRepository members, CatalogueService catalogue,
			Clock clock) {
		this.households = households;
		this.members = members;
		this.catalogue = catalogue;
		this.clock = clock;
	}

	public record MemberData(
			String name,
			LocalDate birthDate,
			Sex sex,
			BigDecimal weightKg,
			BigDecimal heightCm,
			ActivityLevel activityLevel,
			DietType dietType,
			String dietNotes,
			Integer kcalOverride,
			int mainMealsPct,
			Map<Allergen, AllergenKind> allergens,
			Set<Long> dislikedIngredientIds) {
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

	/** Today's orientative estimate for a member. */
	public EnergyRequirement requirement(MemberEntity member) {
		return EnergyRequirementCalculator.calculate(profile(member), LocalDate.now(clock));
	}

	public Diner diner(MemberEntity member, DinerLocation location) {
		return new Diner(member.getId(), member.getName(), location, member.getDietType(),
				member.getAllergens().keySet(), member.getDislikedIngredientIds());
	}

	private static MemberProfile profile(MemberEntity member) {
		return new MemberProfile(member.getBirthDate(), member.getSex(), member.getWeightKg().doubleValue(),
				member.getHeightCm().doubleValue(), member.getActivityLevel(), member.getKcalOverride(),
				member.getMainMealsPct());
	}

	private void apply(MemberEntity member, MemberData data) {
		List<Long> unknown = data.dislikedIngredientIds().stream().filter(id -> !catalogue.ingredientExists(id)).toList();
		if (!unknown.isEmpty()) {
			throw new Errors.RuleViolation("UNKNOWN_INGREDIENT", "Hi ha ingredients que no existeixen.",
					unknown.stream().map(id -> "Ingredient " + id).toList());
		}
		member.setName(data.name().strip());
		member.setBirthDate(data.birthDate());
		member.setSex(data.sex());
		member.setWeightKg(data.weightKg());
		member.setHeightCm(data.heightCm());
		member.setActivityLevel(data.activityLevel());
		member.setDietType(data.dietType());
		member.setDietNotes(data.dietNotes() == null || data.dietNotes().isBlank() ? null : data.dietNotes().strip());
		member.setKcalOverride(data.kcalOverride());
		member.setMainMealsPct(data.mainMealsPct());
		member.getAllergens().clear();
		member.getAllergens().putAll(data.allergens());
		member.getDislikedIngredientIds().clear();
		member.getDislikedIngredientIds().addAll(data.dislikedIngredientIds());
	}
}
