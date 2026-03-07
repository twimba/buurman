-- ---------------------------------------------------------------------------
-- V026: Team-level currency enforcement (BUUR-51)
-- ---------------------------------------------------------------------------
-- Makes default_currency mandatory on team_preferences, adds onboarding
-- tracking, and creates an audit table for currency changes.
-- ---------------------------------------------------------------------------
-- 1. Backfill NULL default_currency to 'EUR'
UPDATE team_preferences
SET
    default_currency = 'EUR'
WHERE
    default_currency IS NULL;

-- 2. Make default_currency NOT NULL with default
ALTER TABLE team_preferences
ALTER COLUMN default_currency
SET NOT NULL,
ALTER COLUMN default_currency
SET DEFAULT 'EUR';

-- 3. Track onboarding completion
ALTER TABLE team_preferences
ADD COLUMN onboarding_completed_at TIMESTAMP;

-- 4. Currency change audit log
CREATE TABLE currency_change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id),
    old_currency VARCHAR(3) NOT NULL,
    new_currency VARCHAR(3) NOT NULL,
    change_mode VARCHAR(20) NOT NULL, -- RELABEL or CONVERT
    conversion_rate NUMERIC(18, 8), -- NULL for RELABEL mode
    affected_contracts INTEGER NOT NULL DEFAULT 0,
    affected_payments INTEGER NOT NULL DEFAULT 0,
    affected_expenses INTEGER NOT NULL DEFAULT 0,
    affected_financials INTEGER NOT NULL DEFAULT 0,
    changed_by UUID,
    changed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_currency_change_log_team ON currency_change_log (team_id);
