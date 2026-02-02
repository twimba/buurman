-- Create payments table for rent payment tracking
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),

    -- Financial details
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Payment dates
    payment_date DATE,
    due_date DATE NOT NULL,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- Additional details
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_payment_identifier UNIQUE(team_id, identifier),
    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED'))
);

-- Create indexes for performance
CREATE INDEX idx_payments_team_id ON payments(team_id);
CREATE INDEX idx_payments_contract_id ON payments(contract_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_due_date ON payments(due_date);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
CREATE INDEX idx_payments_deleted_at ON payments(deleted_at);

-- Index for finding pending/overdue payments
CREATE INDEX idx_payments_pending ON payments(team_id, status, due_date)
WHERE deleted_at IS NULL AND status IN ('PENDING', 'OVERDUE');

-- Comments for documentation
COMMENT ON TABLE payments IS 'Rent payment records linked to contracts';
COMMENT ON COLUMN payments.identifier IS 'ULID identifier for external use';
COMMENT ON COLUMN payments.payment_date IS 'Actual date payment was received (NULL if not yet paid)';
COMMENT ON COLUMN payments.due_date IS 'Date payment is/was due';
COMMENT ON COLUMN payments.status IS 'Current status (PENDING, PAID, OVERDUE, CANCELLED)';
