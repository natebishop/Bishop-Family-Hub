ALTER TABLE family
    ADD COLUMN timezone VARCHAR(100) NOT NULL DEFAULT 'America/Los_Angeles';

UPDATE family
SET timezone = 'America/Los_Angeles'
WHERE timezone IS NULL;
