-- The planner organises the week's meals; it no longer does the shopping. The pantry, the
-- shopping lists, the purchase data of ingredients (package size, aisle, pantry staple), the
-- scaling mode of recipe lines (only used to scale quantities for buying) and members' dislikes
-- are no longer kept.

DROP TABLE shopping_list_item;
DROP TABLE shopping_list;
DROP TABLE pantry_item;
DROP TABLE member_dislike;

ALTER TABLE ingredient
    DROP COLUMN package_size,
    DROP COLUMN aisle,
    DROP COLUMN pantry_staple;

ALTER TABLE recipe_ingredient
    DROP COLUMN scaling_mode;
