ALTER TABLE team_preferences
DROP CONSTRAINT chk_team_default_language;

ALTER TABLE team_preferences
ADD CONSTRAINT chk_team_default_language CHECK (
    default_language IN (
        'en',
        'nl',
        'pt',
        'es',
        'fr',
        'de',
        'it',
        'sv',
        'fi',
        'el',
        'pl',
        'da',
        'nb'
    )
);
