-- Google event IDs are only unique within a Google calendar. The old global
-- index made imports fail when two selected calendars reused the same ID.

-- V10 introduced synced_calendar_id after Google events already existed. Keep
-- rows that can be safely attributed to the member's only synced calendar;
-- discard the remaining unscoped Google cache so it can be rebuilt cleanly.
UPDATE calendar_event e
SET synced_calendar_id = sc.id
FROM google_synced_calendar sc
WHERE e.source = 'GOOGLE'
  AND e.synced_calendar_id IS NULL
  AND sc.member_id = e.member_id
  AND NOT EXISTS (
      SELECT 1
      FROM google_synced_calendar other
      WHERE other.member_id = e.member_id
        AND other.id <> sc.id
  );

DELETE FROM calendar_event
WHERE source = 'GOOGLE'
  AND synced_calendar_id IS NULL;

-- Remove cache for calendars that were already turned off before this fix.
-- Native events are not affected because this is explicitly Google-scoped.
DELETE FROM calendar_event e
USING google_synced_calendar sc
WHERE e.synced_calendar_id = sc.id
  AND e.source = 'GOOGLE'
  AND sc.enabled = false;

DROP INDEX IF EXISTS idx_calendar_event_google_id;

CREATE UNIQUE INDEX idx_calendar_event_synced_calendar_google_id
    ON calendar_event(synced_calendar_id, google_event_id)
    WHERE source = 'GOOGLE'
      AND synced_calendar_id IS NOT NULL
      AND google_event_id IS NOT NULL;
