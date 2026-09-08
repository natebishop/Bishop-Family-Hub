CREATE TABLE recipe (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    image_url TEXT,
    note TEXT,
    source_url TEXT,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_recipe_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE
);

CREATE TABLE recipe_ingredient (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    text VARCHAR(500) NOT NULL,
    CONSTRAINT fk_recipe_ingredient_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
    CONSTRAINT uk_recipe_ingredient_recipe_sort UNIQUE (recipe_id, sort_order)
);

CREATE TABLE recipe_instruction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    text TEXT NOT NULL,
    CONSTRAINT fk_recipe_instruction_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
    CONSTRAINT uk_recipe_instruction_recipe_sort UNIQUE (recipe_id, sort_order)
);

CREATE TABLE recipe_tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    name VARCHAR(60) NOT NULL,
    CONSTRAINT fk_recipe_tag_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
    CONSTRAINT uk_recipe_tag_recipe_sort UNIQUE (recipe_id, sort_order),
    CONSTRAINT uk_recipe_tag_recipe_name UNIQUE (recipe_id, name)
);

CREATE INDEX idx_recipe_family_updated_at ON recipe(family_id, updated_at DESC);
CREATE INDEX idx_recipe_family_favorite ON recipe(family_id, favorite);
