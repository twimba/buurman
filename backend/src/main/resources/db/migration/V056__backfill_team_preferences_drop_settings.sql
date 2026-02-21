-- Backfill demo flag from JSONB settings
UPDATE teams
SET
    demo = TRUE
WHERE
    settings ? 'demoData'
    AND (settings ->> 'demoData')::BOOLEAN = TRUE;

-- Backfill team_preferences from JSONB settings
INSERT INTO
    team_preferences (
        team_id,
        payments_ahead_count,
        auto_generation_enabled,
        default_currency,
        default_country,
        timezone,
        date_format,
        fiscal_year_start_month
    )
SELECT
    t.id,
    coalesce(
        (t.settings -> 'payments' ->> 'paymentsAheadCount')::INTEGER,
        3
    ),
    coalesce(
        (
            t.settings -> 'payments' ->> 'autoGenerationEnabled'
        )::BOOLEAN,
        TRUE
    ),
    nullif(
        t.settings -> 'regional' ->> 'defaultCurrency',
        ''
    ),
    coalesce(
        nullif(t.settings -> 'regional' ->> 'defaultCountry', ''),
        'Netherlands'
    ),
    coalesce(
        nullif(t.settings -> 'regional' ->> 'timezone', ''),
        'Europe/Amsterdam'
    ),
    coalesce(
        nullif(t.settings -> 'regional' ->> 'dateFormat', ''),
        'DD/MM/YYYY'
    ),
    coalesce(
        nullif(
            t.settings -> 'regional' ->> 'fiscalYearStartMonth',
            ''
        ),
        '01'
    )
FROM
    teams t
WHERE
    NOT EXISTS (
        SELECT
            1
        FROM
            team_preferences tp
        WHERE
            tp.team_id = t.id
    );

-- Drop the JSONB settings column
DROP INDEX IF EXISTS idx_teams_settings;

ALTER TABLE teams
DROP COLUMN settings;
