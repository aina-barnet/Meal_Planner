package app.mealplanner.application;

import app.mealplanner.persistence.Repositories.ShoppingListRepository;
import app.mealplanner.persistence.ShoppingListEntity;
import app.mealplanner.persistence.ShoppingListItemEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShoppingListService {

	private final ShoppingListRepository lists;

	public ShoppingListService(ShoppingListRepository lists) {
		this.lists = lists;
	}

	@Transactional(readOnly = true)
	public ShoppingListEntity forWeek(long weekId) {
		return lists.findByWeekPlanId(weekId).orElseThrow(() -> new Errors.NotFound("SHOPPING_LIST_NOT_FOUND",
				"La llista de la compra es genera en confirmar la setmana."));
	}

	public ShoppingListEntity check(long weekId, long itemId, boolean checked) {
		ShoppingListEntity list = forWeek(weekId);
		ShoppingListItemEntity item = list.getItems().stream()
				.filter(i -> i.getId() == itemId)
				.findFirst()
				.orElseThrow(() -> new Errors.NotFound("SHOPPING_ITEM_NOT_FOUND",
						"No existeix l'element " + itemId + " de la llista."));
		item.setChecked(checked);
		return list;
	}
}
