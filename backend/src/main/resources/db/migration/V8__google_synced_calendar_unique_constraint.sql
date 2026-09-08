ALTER TABLE google_synced_calendar
    ADD CONSTRAINT uq_synced_calendar_member_google_id UNIQUE (member_id, google_calendar_id);
