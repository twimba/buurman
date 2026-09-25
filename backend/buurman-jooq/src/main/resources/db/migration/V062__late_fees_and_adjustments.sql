-- BUUR-101: late fees, cancellations with reason, write-offs and contact credits.

-- Payments: a type (rent vs. late fee), the rent payment a fee belongs to, and the reason a
-- payment was cancelled or a fee waived.
ALTER TABLE payments
    ADD COLUMN payment_type VARCHAR(20) NOT NULL DEFAULT 'RENT',
    ADD COLUMN parent_payment_id UUID REFERENCES payments (id),
    ADD COLUMN cancel_reason TEXT,
    ADD COLUMN waived_at TIMESTAMP,
    ADD COLUMN waived_by UUID REFERENCES users (id),
    ADD COLUMN waive_reason TEXT,
    ADD CONSTRAINT chk_payments_type CHECK (payment_type IN ('RENT', 'LATE_FEE'));

CREATE INDEX idx_payments_parent_payment_id ON payments (parent_payment_id)
WHERE
    parent_payment_id IS NOT NULL;

-- Contracts: late fees are opt-in per contract with a grace period; the percentage already exists.
ALTER TABLE contracts
    ADD COLUMN late_fee_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN late_fee_grace_days INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_contracts_late_fee_grace CHECK (late_fee_grace_days >= 0);

-- Credits owed to a tenant: overpayments and manual credit notes, applied to later payments or
-- refunded. Amounts in minor units; remaining_amount tracks what is still unapplied.
CREATE TABLE contact_credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contact_id UUID NOT NULL REFERENCES contacts (id),
    contract_id UUID REFERENCES contracts (id),
    amount BIGINT NOT NULL,
    remaining_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(20) NOT NULL,
    reason TEXT,
    source_payment_id UUID REFERENCES payments (id),
    refunded_at TIMESTAMP,
    refund_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_contact_credits_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contact_credits_amount CHECK (amount > 0),
    CONSTRAINT chk_contact_credits_remaining CHECK (remaining_amount >= 0 AND remaining_amount <= amount),
    CONSTRAINT chk_contact_credits_source CHECK (source IN ('OVERPAYMENT', 'CREDIT_NOTE'))
);

CREATE INDEX idx_contact_credits_team_id ON contact_credits (team_id);

CREATE INDEX idx_contact_credits_contact_id ON contact_credits (contact_id);

CREATE INDEX idx_contact_credits_open ON contact_credits (team_id, contact_id)
WHERE
    remaining_amount > 0
    AND deleted_at IS NULL;

-- Receivals: how the balance was settled — cash, a write-off, or an applied credit.
ALTER TABLE payment_receivals
    ADD COLUMN receival_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    ADD COLUMN credit_id UUID REFERENCES contact_credits (id),
    ADD CONSTRAINT chk_receivals_type CHECK (receival_type IN ('PAYMENT', 'WRITE_OFF', 'CREDIT'));
