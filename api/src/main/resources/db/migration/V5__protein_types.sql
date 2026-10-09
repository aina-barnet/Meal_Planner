-- Protein types become data instead of a fixed list, so the family can add their own
-- (tofu, seitan, lamb…). The generator only needs to know whether two recipes share one.

CREATE TABLE protein (
    code     VARCHAR(40) PRIMARY KEY,
    name     VARCHAR(60) NOT NULL UNIQUE,
    custom   BOOLEAN     NOT NULL DEFAULT false,
    position INT         NOT NULL
);

INSERT INTO protein (code, name, position) VALUES
    ('FISH',       'Peix',     1),
    ('SEAFOOD',    'Marisc',   2),
    ('POULTRY',    'Aviram',   3),
    ('PORK',       'Porc',     4),
    ('BEEF',       'Vedella',  5),
    ('LAMB',       'Xai',      6),
    ('RABBIT',     'Conill',   7),
    ('LEGUMES',    'Llegums',  8),
    ('EGGS',       'Ous',      9),
    ('DAIRY',      'Làctics', 10),
    ('VEGETABLES', 'Verdures', 11);

ALTER TABLE recipe DROP CONSTRAINT recipe_protein_type_check;
ALTER TABLE recipe ALTER COLUMN protein_type TYPE VARCHAR(40);
ALTER TABLE recipe ADD CONSTRAINT recipe_protein_fk FOREIGN KEY (protein_type) REFERENCES protein(code);
