ALTER TABLE calendar_event
    ADD COLUMN synced_calendar_id UUID REFERENCES google_synced_calendar(id) ON DELETE SET NULL,
    ADD COLUMN exdates TEXT;
