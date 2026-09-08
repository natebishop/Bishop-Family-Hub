ALTER TABLE calendar_event
    ADD COLUMN recurrence_rule    VARCHAR(255),
    ADD COLUMN recurring_event_id UUID REFERENCES calendar_event(id) ON DELETE CASCADE,
    ADD COLUMN original_date      DATE,
    ADD COLUMN is_cancelled       BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_calendar_event_recurring_event_id
    ON calendar_event(recurring_event_id);

ALTER TABLE calendar_event
    ADD CONSTRAINT chk_no_recurring_multiday
    CHECK (recurrence_rule IS NULL OR end_date IS NULL);

ALTER TABLE calendar_event
    ADD CONSTRAINT uq_recurring_event_original_date
    UNIQUE (recurring_event_id, original_date);
