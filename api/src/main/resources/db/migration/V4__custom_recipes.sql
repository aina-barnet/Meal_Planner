-- The family can add its own recipes and ingredients, edit any recipe, and hide recipes.
-- Recipes are hidden rather than deleted so that past weeks that used them keep working.

ALTER TABLE recipe
    ADD COLUMN custom BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE ingredient
    ADD COLUMN custom BOOLEAN NOT NULL DEFAULT false;

-- Home recipes do not always belong to one of the catalogue's cuisines.
ALTER TABLE recipe DROP CONSTRAINT recipe_cuisine_check;
ALTER TABLE recipe ADD CONSTRAINT recipe_cuisine_check CHECK (cuisine IN
    ('CATALAN','SPANISH','ITALIAN','GREEK','MOROCCAN','LEVANTINE','FRENCH','OTHER'));
