ALTER TABLE family_member
    ADD CONSTRAINT uk_family_member_family_id_id UNIQUE (family_id, id);

CREATE TABLE chore (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    assigned_to_member_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    due_date DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_chore_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE,
    CONSTRAINT fk_chore_assignee_in_family
        FOREIGN KEY (family_id, assigned_to_member_id)
        REFERENCES family_member(family_id, id)
        ON DELETE CASCADE
);
