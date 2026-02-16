CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    -- Financial details
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    -- Dates
    payment_date DATE,
    due_date DATE NOT NULL,
    -- Status & tracking
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE,
    -- Additional details
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_payments_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (
        status IN (
            'PENDING',
            'PAID',
            'OVERDUE',
            'CANCELLED',
            'PARTIALLY_PAID'
        )
    )
);

CREATE INDEX idx_payments_team_id ON payments (team_id);

CREATE INDEX idx_payments_contract_id ON payments (contract_id);

CREATE INDEX idx_payments_status ON payments (status);

CREATE INDEX idx_payments_due_date ON payments (due_date);

CREATE INDEX idx_payments_payment_date ON payments (payment_date);

CREATE INDEX idx_payments_deleted_at ON payments (deleted_at);

CREATE INDEX idx_payments_pending ON payments (team_id, status, due_date)
WHERE
    deleted_at IS NULL
    AND status IN ('PENDING', 'OVERDUE');

CREATE INDEX idx_payments_auto_generated ON payments (auto_generated)
WHERE
    auto_generated = TRUE
    AND deleted_at IS NULL;

-- Prevent duplicate payments per contract+due_date (only for active records)
CREATE UNIQUE INDEX unique_contract_due_date_active ON payments (contract_id, due_date)
WHERE
    deleted_at IS NULL;
