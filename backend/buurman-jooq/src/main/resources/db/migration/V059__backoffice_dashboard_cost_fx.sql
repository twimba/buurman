-- BUUR-96: Backoffice Dashboard v2 + cost tracking + FX rates (collapsed branch migration).
-- Additive only; safe to roll back. Combines what were V059–V065 on this branch into one
-- migration, producing the net schema directly (the intermediate single-row fx_rate shape is
-- skipped — fx_rate is created in its final dated form).
-- === Dashboard v2: per-user layout + action-item snooze ===
-- backoffice_user_id is the Keycloak subject UUID of the acting Buurmy. Backoffice admins are
-- not necessarily rows in the tenant `users` table, so there is no FK (matches
-- impersonation_sessions.admin_user_id).
CREATE TABLE backoffice_user_dashboard_layout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backoffice_user_id UUID NOT NULL,
    layout JSONB NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_backoffice_dashboard_layout_user UNIQUE (backoffice_user_id)
);

CREATE TABLE backoffice_action_item_snooze (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backoffice_user_id UUID NOT NULL,
    item_key VARCHAR(120) NOT NULL,
    snoozed_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_backoffice_action_snooze UNIQUE (backoffice_user_id, item_key)
);

CREATE INDEX idx_action_snooze_user ON backoffice_action_item_snooze (backoffice_user_id, snoozed_until);

-- === Team default country: stop forcing a default ===
-- Country is user-chosen (onboarding / team settings); new teams start with no country until the
-- user sets one. Existing values are left untouched.
ALTER TABLE team_preferences
ALTER COLUMN default_country_code
DROP DEFAULT;

-- === Cost tracking: per-provider monthly spend, normalized to EUR ===
-- One row per (provider, period_month): each poll upserts that month's row in place (captured_at
-- tracks the last refresh). This keeps the table bounded (providers × months) and makes the manual
-- /refresh endpoint idempotent within a month. The current month's row is the live figure; the set
-- of period_month rows is the trend.
CREATE TABLE cost_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(40) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT now(),
    period_month DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_minor BIGINT NOT NULL,
    amount_eur_minor BIGINT NOT NULL,
    fx_rate NUMERIC(18, 8) NOT NULL,
    breakdown JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_cost_source_type CHECK (
        source_type IN ('ACTUAL', 'ESTIMATED', 'SUBSCRIPTION')
    ),
    CONSTRAINT uq_cost_snapshot_provider_month UNIQUE (provider, period_month)
);

CREATE INDEX idx_cost_snapshot_period ON cost_snapshot (period_month, provider);

-- Editable monthly cost amounts (EUR) for providers without a usable cost API, or as a fallback
-- when an API source is unavailable. One row per provider; edited from the backoffice Costs page.
CREATE TABLE cost_manual_amount (
    provider VARCHAR(40) PRIMARY KEY,
    amount_eur_minor BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(200),
    CONSTRAINT chk_cost_manual_amount_nonneg CHECK (amount_eur_minor >= 0)
);

-- === FX rates: dated history, anchored on EUR ===
-- Keep one row per (currency, rate_date) so we retain daily history. Conversions pick the rate
-- effective on the relevant date (latest rate_date on/before it). `rate` is how many EUR (the base)
-- one unit of `currency` buys, so amount_eur = amount * rate. Cost normalization prefers this over
-- the static config fallback.
CREATE TABLE fx_rate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency VARCHAR(3) NOT NULL,
    rate_date DATE NOT NULL,
    rate NUMERIC(18, 8) NOT NULL,
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_fx_rate_positive CHECK (rate > 0),
    CONSTRAINT uq_fx_rate_currency_date UNIQUE (currency, rate_date)
);

-- Lookup: newest rate on/before a given date, per currency.
CREATE INDEX idx_fx_rate_lookup ON fx_rate (currency, rate_date DESC);

-- Tracked FX currency pairs, all anchored on EUR (we store the source/quote currency; base is EUR).
-- refresh/backfill iterate these rows.
CREATE TABLE fx_pair (
    currency VARCHAR(3) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255)
);

-- Seed the one pair the app needs out of the box (Twilio bills USD). Admins manage the rest.
INSERT INTO
    fx_pair (currency, created_by)
VALUES
    ('USD', 'system');
