-- Weekly Family Meal Planner — base schema.
-- Enumerations are stored as VARCHAR with CHECK constraints so the database stays readable
-- and the Java enums remain the single source of truth for behaviour.

-- ---------------------------------------------------------------------------
-- Catalogue: ingredients
-- ---------------------------------------------------------------------------
CREATE TABLE ingredient (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(60)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    category        VARCHAR(20)  NOT NULL CHECK (category IN
                        ('VEGETABLE','FRUIT','HERB','LEGUME','GRAIN','NUT','OIL','SPICE','CONDIMENT',
                         'DAIRY','EGG','FISH','SEAFOOD','MEAT','OTHER')),
    canonical_unit  VARCHAR(2)   NOT NULL CHECK (canonical_unit IN ('G','ML')),
    package_size    NUMERIC(10,2) NOT NULL CHECK (package_size > 0),
    aisle           VARCHAR(20)  NOT NULL CHECK (aisle IN
                        ('PRODUCE','FISHMONGER','BUTCHER','DAIRY_EGGS','BAKERY','PANTRY',
                         'CANNED','SPICES','FROZEN','DRINKS')),
    kcal_per_100    NUMERIC(7,2) NOT NULL CHECK (kcal_per_100 >= 0),
    protein_per_100 NUMERIC(6,2) NOT NULL CHECK (protein_per_100 >= 0),
    carbs_per_100   NUMERIC(6,2) NOT NULL CHECK (carbs_per_100 >= 0),
    fat_per_100     NUMERIC(6,2) NOT NULL CHECK (fat_per_100 >= 0),
    -- Staples (oil, spices) are always assumed to be at home and never go on the shopping list.
    pantry_staple   BOOLEAN      NOT NULL DEFAULT false
);

-- The 14 allergens that EU regulation 1169/2011 requires to be declared.
CREATE TABLE ingredient_allergen (
    ingredient_id BIGINT      NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    allergen      VARCHAR(20) NOT NULL CHECK (allergen IN
                      ('GLUTEN','CRUSTACEANS','EGGS','FISH','PEANUTS','SOY','MILK','TREE_NUTS',
                       'CELERY','MUSTARD','SESAME','SULPHITES','LUPIN','MOLLUSCS')),
    PRIMARY KEY (ingredient_id, allergen)
);

-- Ingredient-specific conversions: how many canonical units (g or ml) one recipe unit is.
-- Metric conversions (kg→g, l→ml, cl→ml) are generic and live in the domain, not here.
CREATE TABLE ingredient_unit_conversion (
    ingredient_id     BIGINT        NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    unit              VARCHAR(10)   NOT NULL CHECK (unit IN
                          ('UNIT','TBSP','TSP','CLOVE','PINCH','SLICE','BUNCH','SPRIG','CAN','CUP')),
    canonical_per_unit NUMERIC(10,3) NOT NULL CHECK (canonical_per_unit > 0),
    PRIMARY KEY (ingredient_id, unit)
);

-- ---------------------------------------------------------------------------
-- Catalogue: recipes
-- ---------------------------------------------------------------------------
CREATE TABLE recipe (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(60)  NOT NULL UNIQUE,
    name             VARCHAR(160) NOT NULL,
    description      VARCHAR(500),
    base_servings    INT          NOT NULL CHECK (base_servings > 0),
    prep_minutes     INT          NOT NULL CHECK (prep_minutes > 0),
    protein_type     VARCHAR(20)  NOT NULL CHECK (protein_type IN
                         ('FISH','SEAFOOD','POULTRY','PORK','BEEF','LAMB','RABBIT',
                          'LEGUMES','EGGS','DAIRY','VEGETABLES')),
    cuisine          VARCHAR(20)  NOT NULL CHECK (cuisine IN
                         ('CATALAN','SPANISH','ITALIAN','GREEK','MOROCCAN','LEVANTINE','FRENCH')),
    portable         BOOLEAN      NOT NULL,
    lunchbox_serving VARCHAR(10)  CHECK (lunchbox_serving IN ('COLD','REHEAT')),
    CONSTRAINT recipe_lunchbox_serving_iff_portable
        CHECK ((portable AND lunchbox_serving IS NOT NULL) OR (NOT portable AND lunchbox_serving IS NULL))
);

CREATE TABLE recipe_season (
    recipe_id BIGINT      NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    season    VARCHAR(10) NOT NULL CHECK (season IN ('SPRING','SUMMER','AUTUMN','WINTER')),
    PRIMARY KEY (recipe_id, season)
);

CREATE TABLE recipe_step (
    recipe_id   BIGINT NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    position    INT    NOT NULL CHECK (position > 0),
    instruction TEXT   NOT NULL,
    PRIMARY KEY (recipe_id, position)
);

CREATE TABLE recipe_ingredient (
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT        NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    ingredient_id BIGINT        NOT NULL REFERENCES ingredient(id),
    position      INT           NOT NULL,
    quantity      NUMERIC(10,3) NOT NULL CHECK (quantity > 0),
    unit          VARCHAR(10)   NOT NULL CHECK (unit IN
                      ('G','KG','ML','CL','L','UNIT','TBSP','TSP','CLOVE','PINCH','SLICE','BUNCH','SPRIG','CAN','CUP')),
    scaling_mode  VARCHAR(10)   NOT NULL CHECK (scaling_mode IN ('LINEAR','FIXED','SUBLINEAR')),
    UNIQUE (recipe_id, position)
);
CREATE INDEX recipe_ingredient_recipe_idx ON recipe_ingredient(recipe_id);

-- ---------------------------------------------------------------------------
-- Household
-- ---------------------------------------------------------------------------
CREATE TABLE household (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- One installation serves one household.
CREATE UNIQUE INDEX household_singleton ON household ((true));

CREATE TABLE member (
    id             BIGSERIAL PRIMARY KEY,
    household_id   BIGINT        NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    name           VARCHAR(80)   NOT NULL,
    birth_date     DATE          NOT NULL,
    sex            VARCHAR(6)    NOT NULL CHECK (sex IN ('FEMALE','MALE')),
    weight_kg      NUMERIC(5,1)  NOT NULL CHECK (weight_kg > 0),
    height_cm      NUMERIC(5,1)  NOT NULL CHECK (height_cm > 0),
    activity_level VARCHAR(12)   NOT NULL CHECK (activity_level IN
                       ('SEDENTARY','LIGHT','MODERATE','ACTIVE','VERY_ACTIVE')),
    diet_type      VARCHAR(12)   NOT NULL CHECK (diet_type IN
                       ('OMNIVORE','PESCATARIAN','VEGETARIAN','VEGAN')),
    diet_notes     VARCHAR(500),
    kcal_override  INT           CHECK (kcal_override > 0),
    -- Share of the daily kcal eaten at lunch + dinner (the planned meals), split equally between
    -- them. Breakfast and snacks are personal and outside the plan.
    main_meals_pct INT           NOT NULL DEFAULT 65 CHECK (main_meals_pct BETWEEN 10 AND 100)
);
CREATE INDEX member_household_idx ON member(household_id);

-- Allergies and intolerances are hard constraints for the menu generator.
CREATE TABLE member_allergen (
    member_id BIGINT      NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    allergen  VARCHAR(20) NOT NULL CHECK (allergen IN
                  ('GLUTEN','CRUSTACEANS','EGGS','FISH','PEANUTS','SOY','MILK','TREE_NUTS',
                   'CELERY','MUSTARD','SESAME','SULPHITES','LUPIN','MOLLUSCS')),
    kind      VARCHAR(12) NOT NULL CHECK (kind IN ('ALLERGY','INTOLERANCE')),
    PRIMARY KEY (member_id, allergen)
);

-- Dislikes are soft: the generator penalises them but does not exclude the recipe.
CREATE TABLE member_dislike (
    member_id     BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, ingredient_id)
);

CREATE TABLE pantry_item (
    id            BIGSERIAL PRIMARY KEY,
    household_id  BIGINT        NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    ingredient_id BIGINT        NOT NULL REFERENCES ingredient(id),
    quantity      NUMERIC(12,2) NOT NULL CHECK (quantity >= 0),
    UNIQUE (household_id, ingredient_id)
);

-- ---------------------------------------------------------------------------
-- Planning
-- ---------------------------------------------------------------------------
CREATE TABLE week_plan (
    id           BIGSERIAL PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES household(id) ON DELETE CASCADE,
    week_start   DATE        NOT NULL CHECK (EXTRACT(ISODOW FROM week_start) = 1),
    status       VARCHAR(10) NOT NULL CHECK (status IN ('DRAFT','CONFIRMED')),
    confirmed_at TIMESTAMPTZ,
    UNIQUE (household_id, week_start)
);

CREATE TABLE meal (
    id             BIGSERIAL PRIMARY KEY,
    week_plan_id   BIGINT      NOT NULL REFERENCES week_plan(id) ON DELETE CASCADE,
    meal_date      DATE        NOT NULL,
    slot           VARCHAR(6)  NOT NULL CHECK (slot IN ('LUNCH','DINNER')),
    recipe_id      BIGINT      REFERENCES recipe(id),
    extra_servings INT         NOT NULL DEFAULT 0 CHECK (extra_servings >= 0),
    locked         BOOLEAN     NOT NULL DEFAULT false,
    -- Set by the generator when no recipe satisfies the hard constraints.
    empty_reason   VARCHAR(500),
    UNIQUE (week_plan_id, meal_date, slot)
);

-- Who eats each meal and where. dinersAtHome / dinersTakeaway are derived from these rows.
CREATE TABLE meal_attendee (
    meal_id   BIGINT     NOT NULL REFERENCES meal(id) ON DELETE CASCADE,
    member_id BIGINT     NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    location  VARCHAR(8) NOT NULL CHECK (location IN ('HOME','TAKEAWAY')),
    PRIMARY KEY (meal_id, member_id)
);

-- ---------------------------------------------------------------------------
-- Shopping list (snapshot taken when a week is confirmed)
-- ---------------------------------------------------------------------------
CREATE TABLE shopping_list (
    id           BIGSERIAL PRIMARY KEY,
    week_plan_id BIGINT      NOT NULL UNIQUE REFERENCES week_plan(id) ON DELETE CASCADE,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE shopping_list_item (
    id               BIGSERIAL PRIMARY KEY,
    shopping_list_id BIGINT        NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
    ingredient_id    BIGINT        NOT NULL REFERENCES ingredient(id),
    required_qty     NUMERIC(12,2) NOT NULL,
    from_pantry_qty  NUMERIC(12,2) NOT NULL,
    to_buy_qty       NUMERIC(12,2) NOT NULL,
    packages         INT           NOT NULL,
    package_size     NUMERIC(10,2) NOT NULL,
    leftover_qty     NUMERIC(12,2) NOT NULL,
    checked          BOOLEAN       NOT NULL DEFAULT false,
    UNIQUE (shopping_list_id, ingredient_id)
);
