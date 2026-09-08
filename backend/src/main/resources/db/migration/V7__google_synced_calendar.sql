CREATE TABLE google_synced_calendar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id UUID NOT NULL REFERENCES google_oauth_token(id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES family_member(id) ON DELETE CASCADE,
    google_calendar_id VARCHAR(255) NOT NULL,
    calendar_name VARCHAR(255),
    sync_token TEXT,
    last_synced_at TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
