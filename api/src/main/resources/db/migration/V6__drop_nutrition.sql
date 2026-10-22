-- The planner is about organising the week and the shopping, not about nutrition: calories,
-- macronutrients and the body data used to estimate energy needs are no longer kept.

ALTER TABLE ingredient
    DROP COLUMN kcal_per_100,
    DROP COLUMN protein_per_100,
    DROP COLUMN carbs_per_100,
    DROP COLUMN fat_per_100;

ALTER TABLE member
    DROP COLUMN birth_date,
    DROP COLUMN sex,
    DROP COLUMN weight_kg,
    DROP COLUMN height_cm,
    DROP COLUMN activity_level,
    DROP COLUMN kcal_override,
    DROP COLUMN main_meals_pct;
