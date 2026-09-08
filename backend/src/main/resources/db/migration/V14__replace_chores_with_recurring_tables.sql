DROP TABLE IF EXISTS chore;

CREATE TABLE chore_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL,
    assigned_to_member_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    cadence VARCHAR(20) NOT NULL,
    active_from DATE NOT NULL,
    archived_at TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT now(),
    updated_at TIMESTAMP(6) DEFAULT now(),
    CONSTRAINT fk_chore_template_family
        FOREIGN KEY (family_id) REFERENCES family(id) ON DELETE CASCADE,
    CONSTRAINT fk_chore_template_assignee_in_family
        FOREIGN KEY (family_id, assigned_to_member_id)
        REFERENCES family_member(family_id, id)
        ON DELETE CASCADE
);

CREATE TABLE chore_period_completion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chore_template_id UUID NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_chore_period_completion_template
        FOREIGN KEY (chore_template_id) REFERENCES chore_template(id) ON DELETE CASCADE,
    CONSTRAINT uk_chore_period_completion_template_period
        UNIQUE (chore_template_id, period_start_date, period_end_date)
);
