-- Simpler dishes and members. A dish keeps its name, servings, time, main protein, whether it
-- also works in a lunchbox (every dish can be eaten at home), ingredients and steps; its
-- description, cuisine, seasons and cold-or-reheat lunchbox serving are no longer kept. A member
-- keeps name, diet and allergies; the free-text diet notes go.

DROP TABLE recipe_season;

-- Dropping lunchbox_serving also drops the CHECK that tied it to portable.
ALTER TABLE recipe
    DROP COLUMN description,
    DROP COLUMN cuisine,
    DROP COLUMN lunchbox_serving;

ALTER TABLE member
    DROP COLUMN diet_notes;
