CREATE TABLE meal_slot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    week_start_date DATE NOT NULL,
    day_index INTEGER NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    note TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_meal_slot_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE,
    CONSTRAINT chk_meal_slot_day_index
        CHECK (day_index >= 0 AND day_index <= 6),
    CONSTRAINT chk_meal_slot_meal_type
        CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER')),
    CONSTRAINT uk_meal_slot_family_week_day_type
        UNIQUE (family_id, week_start_date, day_index, meal_type)
);

CREATE TABLE meal_slot_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    recipe_id UUID,
    title_snapshot VARCHAR(160) NOT NULL,
    image_url_snapshot TEXT,
    note_snapshot TEXT,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_meal_slot_entry_slot
        FOREIGN KEY (slot_id) REFERENCES meal_slot(id) ON DELETE CASCADE,
    CONSTRAINT fk_meal_slot_entry_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE SET NULL,
    CONSTRAINT chk_meal_slot_entry_role
        CHECK (role IN ('PRIMARY', 'EXTRA')),
    CONSTRAINT chk_meal_slot_entry_source_type
        CHECK (source_type IN ('RECIPE', 'QUICK')),
    CONSTRAINT uk_meal_slot_entry_slot_sort
        UNIQUE (slot_id, sort_order)
);

CREATE UNIQUE INDEX uk_meal_slot_entry_primary
    ON meal_slot_entry(slot_id)
    WHERE role = 'PRIMARY';

CREATE INDEX idx_meal_slot_family_week
    ON meal_slot(family_id, week_start_date);

CREATE INDEX idx_meal_slot_entry_recipe
    ON meal_slot_entry(recipe_id);
