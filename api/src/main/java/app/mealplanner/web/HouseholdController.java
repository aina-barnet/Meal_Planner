package app.mealplanner.web;

import app.mealplanner.application.HouseholdService;
import app.mealplanner.application.HouseholdService.MemberData;
import app.mealplanner.domain.nutrition.MemberProfile;
import app.mealplanner.persistence.AllergenKind;
import app.mealplanner.persistence.MemberEntity;
import app.mealplanner.web.Dtos.AllergenEntry;
import app.mealplanner.web.Dtos.HouseholdRequest;
import app.mealplanner.web.Dtos.HouseholdResponse;
import app.mealplanner.web.Dtos.MemberRequest;
import app.mealplanner.web.Dtos.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/household")
@Tag(name = "Household", description = "The household and its members")
public class HouseholdController {

	private final HouseholdService household;

	public HouseholdController(HouseholdService household) {
		this.household = household;
	}

	@GetMapping
	@Operation(summary = "Get the household", description = "404 HOUSEHOLD_NOT_SET_UP until the setup wizard has run")
	public HouseholdResponse get() {
		return HouseholdResponse.of(household.require());
	}

	@PutMapping
	@Operation(summary = "Create or rename the household")
	public HouseholdResponse save(@Valid @RequestBody HouseholdRequest request) {
		return HouseholdResponse.of(household.save(request.name()));
	}

	@GetMapping("/members")
	@Operation(summary = "List members with their orientative energy requirements")
	public List<MemberResponse> members() {
		return household.members().stream().map(this::response).toList();
	}

	@GetMapping("/members/{id}")
	public MemberResponse member(@PathVariable long id) {
		return response(household.member(id));
	}

	@PostMapping("/members")
	@ResponseStatus(HttpStatus.CREATED)
	public MemberResponse create(@Valid @RequestBody MemberRequest request) {
		return response(household.createMember(data(request)));
	}

	@PutMapping("/members/{id}")
	public MemberResponse update(@PathVariable long id, @Valid @RequestBody MemberRequest request) {
		return response(household.updateMember(id, data(request)));
	}

	@DeleteMapping("/members/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long id) {
		household.deleteMember(id);
	}

	private MemberResponse response(MemberEntity member) {
		return MemberResponse.of(member, household.requirement(member));
	}

	private static MemberData data(MemberRequest r) {
		Map<app.mealplanner.domain.catalogue.Allergen, AllergenKind> allergens = new EnumMap<>(
				app.mealplanner.domain.catalogue.Allergen.class);
		if (r.allergens() != null) {
			for (AllergenEntry entry : r.allergens()) {
				allergens.put(entry.allergen(), entry.kind());
			}
		}
		return new MemberData(r.name(), r.birthDate(), r.sex(), r.weightKg(), r.heightCm(), r.activityLevel(),
				r.dietType(), r.dietNotes(), r.kcalOverride(),
				r.mainMealsPct() == null ? MemberProfile.DEFAULT_MAIN_MEALS_PCT : r.mainMealsPct(), allergens,
				r.dislikedIngredientIds() == null ? new HashSet<>() : new HashSet<>(r.dislikedIngredientIds()));
	}
}
