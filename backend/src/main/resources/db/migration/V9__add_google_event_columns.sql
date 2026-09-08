ALTER TABLE calendar_event
    ADD COLUMN google_event_id VARCHAR(1024),
    ADD COLUMN html_link VARCHAR(2048),
    ADD COLUMN etag VARCHAR(255),
    ADD COLUMN google_updated_at TIMESTAMPTZ;

CREATE UNIQUE INDEX idx_calendar_event_google_id
    ON calendar_event(google_event_id)
    WHERE google_event_id IS NOT NULL;
