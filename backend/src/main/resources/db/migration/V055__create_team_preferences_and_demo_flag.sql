-- Add demo flag to teams table (internal use, never exposed in API)
ALTER TABLE teams
ADD COLUMN demo BOOLEAN NOT NULL DEFAULT FALSE;

-- Create team_preferences table (1:1 with teams)
CREATE TABLE team_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Payment settings
    payments_ahead_count INTEGER NOT NULL DEFAULT 3,
    auto_generation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Regional settings
    default_currency VARCHAR(3),
    default_country VARCHAR(100) DEFAULT 'Netherlands',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Amsterdam',
    date_format VARCHAR(20) NOT NULL DEFAULT 'DD/MM/YYYY',
    fiscal_year_start_month VARCHAR(2) NOT NULL DEFAULT '01',
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_preferences_team UNIQUE (team_id),
    CONSTRAINT chk_payments_ahead CHECK (payments_ahead_count BETWEEN 1 AND 12),
    CONSTRAINT chk_fiscal_month CHECK (
        fiscal_year_start_month IN (
            '01',
            '02',
            '03',
            '04',
            '05',
            '06',
            '07',
            '08',
            '09',
            '10',
            '11',
            '12'
        )
    )
);

CREATE INDEX idx_team_preferences_team ON team_preferences (team_id);
