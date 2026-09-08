-- Guard: reject pre-existing categories that collide under the new normalized (lower+btrim) name before swapping the unique constraint.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM list_category
        GROUP BY family_id, kind, lower(btrim(name))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enable normalized list-category names: duplicates exist';
    END IF;
END $$;

ALTER TABLE list_category DROP CONSTRAINT ck_list_category_supported_kind;
ALTER TABLE list_category ADD CONSTRAINT ck_list_category_supported_kind
    CHECK (kind IN ('GROCERY', 'TODO', 'GENERAL'));

ALTER TABLE shared_list DROP CONSTRAINT ck_shared_list_general_display_mode;
ALTER TABLE shared_list_item DROP CONSTRAINT ck_shared_list_item_general_category;

ALTER TABLE list_category DROP CONSTRAINT uk_list_category_family_kind_name;
CREATE UNIQUE INDEX uk_list_category_family_kind_normalized_name
    ON list_category (family_id, kind, lower(btrim(name)));

CREATE TABLE list_category_catalog_scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    kind VARCHAR(20) NOT NULL,
    CONSTRAINT fk_list_category_catalog_scope_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE,
    CONSTRAINT ck_list_category_catalog_scope_kind
        CHECK (kind IN ('GROCERY', 'TODO', 'GENERAL')),
    CONSTRAINT uk_list_category_catalog_scope_family_kind
        UNIQUE (family_id, kind)
);

INSERT INTO list_category_catalog_scope (family_id, kind)
SELECT family.id, kinds.kind
FROM family
CROSS JOIN (VALUES ('GROCERY'), ('TODO'), ('GENERAL')) AS kinds(kind);
