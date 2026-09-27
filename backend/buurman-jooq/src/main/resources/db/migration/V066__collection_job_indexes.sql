-- Partial index used by the overdue condition, the arrears view, the dunning ladder and the
-- payment list filter: those all treat PARTIALLY_PAID as open, which the V004 index excluded.
DROP INDEX IF EXISTS idx_payments_pending;

CREATE INDEX idx_payments_pending ON payments (team_id, status, due_date)
WHERE
    deleted_at IS NULL
    AND status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE');

-- Cross-team daily late-fee scan: only contracts with late fees enabled are candidates.
CREATE INDEX idx_contracts_late_fee_enabled ON contracts (id)
WHERE
    late_fee_enabled
    AND deleted_at IS NULL;

-- Receival lookups per payment (balance sums, settlement checks) within a team.
CREATE INDEX IF NOT EXISTS idx_payment_receivals_team_payment ON payment_receivals (team_id, payment_id)
WHERE
    deleted_at IS NULL;
