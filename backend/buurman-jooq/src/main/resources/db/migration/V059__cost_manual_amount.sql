-- Editable monthly cost amounts (EUR) for providers without a usable cost API, or as a fallback
-- when an API source is unavailable. One row per provider; edited from the backoffice Costs page.
-- Additive only; safe to roll back.

CREATE TABLE cost_manual_amount (
    provider VARCHAR(40) PRIMARY KEY,
    amount_eur_minor BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(200),
    CONSTRAINT chk_cost_manual_amount_nonneg CHECK (amount_eur_minor >= 0)
);
