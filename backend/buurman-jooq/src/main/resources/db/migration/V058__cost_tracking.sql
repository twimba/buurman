-- Cost tracking: daily snapshots of per-provider spend, normalized to EUR.
-- Additive only; safe to roll back. One row per provider per poll; queries take the
-- latest per provider (current) and the latest per (provider, period_month) (trend).

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
    )
);

CREATE INDEX idx_cost_snapshot_latest ON cost_snapshot (provider, captured_at DESC);

CREATE INDEX idx_cost_snapshot_period ON cost_snapshot (period_month, provider);
