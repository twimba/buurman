-- BUUR-101: deposit lifecycle and payment plans.
-- One deposit per contract: what was received, where it is held, deductions on move-out and
-- what was returned. Amounts in minor units.
CREATE TABLE deposits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    contact_id UUID REFERENCES contacts (id),
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    received_date DATE,
    held_where VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'EXPECTED',
    return_due_date DATE,
    returned_date DATE,
    returned_amount BIGINT NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_deposits_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_deposits_amount CHECK (amount > 0),
    CONSTRAINT chk_deposits_returned CHECK (returned_amount >= 0),
    CONSTRAINT chk_deposits_status CHECK (
        status IN (
            'EXPECTED',
            'HELD',
            'PARTIALLY_RETURNED',
            'RETURNED',
            'FORFEITED'
        )
    )
);

CREATE UNIQUE INDEX uq_deposits_contract_active ON deposits (contract_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_deposits_team_id ON deposits (team_id);

CREATE INDEX idx_deposits_return_due ON deposits (team_id, return_due_date)
WHERE
    deleted_at IS NULL
    AND status IN ('EXPECTED', 'HELD');

CREATE TABLE deposit_deductions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    deposit_id UUID NOT NULL REFERENCES deposits (id),
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason TEXT NOT NULL,
    deduction_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_deposit_deductions_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_deposit_deductions_amount CHECK (amount > 0)
);

CREATE INDEX idx_deposit_deductions_deposit_id ON deposit_deductions (deposit_id);

-- Payment plans: an overdue balance split into instalments. The covered payments are settled
-- with PLAN receivals; the instalments are INSTALMENT payments linked to the plan.
CREATE TABLE payment_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    contact_id UUID REFERENCES contacts (id),
    total_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    instalment_count INTEGER NOT NULL,
    start_date DATE NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    cancel_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_payment_plans_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_payment_plans_total CHECK (total_amount > 0),
    CONSTRAINT chk_payment_plans_count CHECK (instalment_count BETWEEN 1 AND 36),
    CONSTRAINT chk_payment_plans_frequency CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY')),
    CONSTRAINT chk_payment_plans_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_payment_plans_contract_id ON payment_plans (contract_id);

CREATE INDEX idx_payment_plans_team_status ON payment_plans (team_id, status)
WHERE
    deleted_at IS NULL;

ALTER TABLE payments
DROP CONSTRAINT chk_payments_type,
ADD CONSTRAINT chk_payments_type CHECK (
    payment_type IN ('RENT', 'LATE_FEE', 'INSTALMENT')
),
ADD COLUMN payment_plan_id UUID REFERENCES payment_plans (id);

CREATE INDEX idx_payments_payment_plan_id ON payments (payment_plan_id)
WHERE
    payment_plan_id IS NOT NULL;

ALTER TABLE payment_receivals
DROP CONSTRAINT chk_receivals_type,
ADD CONSTRAINT chk_receivals_type CHECK (
    receival_type IN ('PAYMENT', 'WRITE_OFF', 'CREDIT', 'PLAN')
),
ADD COLUMN payment_plan_id UUID REFERENCES payment_plans (id);
