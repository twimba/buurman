ALTER TABLE team_preferences
ADD COLUMN default_language VARCHAR(5) NOT NULL DEFAULT 'en';

ALTER TABLE team_preferences
ADD CONSTRAINT chk_team_default_language CHECK (
    default_language IN ('en', 'nl', 'pt', 'es', 'fr', 'de')
);
