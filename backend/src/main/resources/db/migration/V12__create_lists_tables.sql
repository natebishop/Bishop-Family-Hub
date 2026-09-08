CREATE TABLE list_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL UNIQUE,
    show_completed_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_list_preferences_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE
);

CREATE TABLE list_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    kind VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    seeded BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_list_category_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE,
    CONSTRAINT ck_list_category_supported_kind
        CHECK (kind IN ('GROCERY', 'TODO')),
    CONSTRAINT uk_list_category_id_family_kind UNIQUE (id, family_id, kind),
    CONSTRAINT uk_list_category_family_kind_name UNIQUE (family_id, kind, name)
);

CREATE TABLE shared_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    category_display_mode VARCHAR(20) NOT NULL,
    show_completed_override BOOLEAN,
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_shared_list_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE
        ,
    CONSTRAINT ck_shared_list_general_display_mode
        CHECK (kind <> 'GENERAL' OR category_display_mode = 'FLAT'),
    CONSTRAINT uk_shared_list_id_family_kind UNIQUE (id, family_id, kind)
);

CREATE TABLE shared_list_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID NOT NULL,
    family_id UUID NOT NULL,
    list_kind VARCHAR(20) NOT NULL,
    category_id UUID,
    text VARCHAR(100) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_shared_list_item_list
        FOREIGN KEY (list_id, family_id, list_kind)
        REFERENCES shared_list(id, family_id, kind)
        ON DELETE CASCADE,
    CONSTRAINT fk_shared_list_item_category
        FOREIGN KEY (category_id, family_id, list_kind)
        REFERENCES list_category(id, family_id, kind),
    CONSTRAINT ck_shared_list_item_general_category
        CHECK (list_kind <> 'GENERAL' OR category_id IS NULL)
);

CREATE INDEX idx_shared_list_family_id ON shared_list(family_id);
CREATE INDEX idx_shared_list_item_list_id ON shared_list_item(list_id);

INSERT INTO list_preferences (family_id, show_completed_by_default)
SELECT id, TRUE
FROM family
ON CONFLICT (family_id) DO NOTHING;

INSERT INTO list_category (family_id, kind, name, seeded, sort_order)
SELECT f.id, seed.kind, seed.name, TRUE, seed.sort_order
FROM family f
CROSS JOIN (
    VALUES
        ('GROCERY', 'Produce', 0),
        ('GROCERY', 'Dairy', 1),
        ('GROCERY', 'Pantry', 2),
        ('GROCERY', 'Frozen', 3),
        ('GROCERY', 'Household', 4),
        ('TODO', 'Urgent', 0),
        ('TODO', 'Soon', 1),
        ('TODO', 'Later', 2)
) AS seed(kind, name, sort_order)
ON CONFLICT (family_id, kind, name) DO NOTHING;
