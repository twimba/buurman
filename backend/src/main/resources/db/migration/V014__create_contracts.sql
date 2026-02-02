-- Create contracts table for rental agreements
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Contract Type
    contract_type VARCHAR(50) NOT NULL,

    -- Dates
    start_date DATE NOT NULL,
    end_date DATE,
    signed_date DATE,

    -- Financial terms
    rent_amount DECIMAL(10, 2) NOT NULL,
    deposit_amount DECIMAL(10, 2),
    security_deposit DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Payment terms
    payment_frequency VARCHAR(20) NOT NULL,
    payment_due_day INTEGER,

    -- Renewal & Termination
    auto_renewal BOOLEAN DEFAULT FALSE,
    renewal_notice_days INTEGER DEFAULT 30,
    termination_notice_days INTEGER DEFAULT 30,

    -- Fees
    late_fee_percentage DECIMAL(5, 2),

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Additional details
    terms_and_conditions TEXT,
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_contract_identifier UNIQUE(team_id, identifier),
    CONSTRAINT positive_rent_amount CHECK (rent_amount > 0),
    CONSTRAINT positive_deposit_amount CHECK (deposit_amount IS NULL OR deposit_amount >= 0),
    CONSTRAINT positive_security_deposit CHECK (security_deposit IS NULL OR security_deposit >= 0),
    CONSTRAINT valid_payment_due_day CHECK (payment_due_day IS NULL OR (payment_due_day >= 1 AND payment_due_day <= 31)),
    CONSTRAINT valid_late_fee CHECK (late_fee_percentage IS NULL OR (late_fee_percentage >= 0 AND late_fee_percentage <= 100)),
    CONSTRAINT end_date_after_start CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT valid_contract_type CHECK (contract_type IN ('FIXED_TERM', 'INDEFINITE', 'FURNISHED', 'UNFURNISHED')),
    CONSTRAINT valid_payment_frequency CHECK (payment_frequency IN ('MONTHLY', 'QUARTERLY', 'ANNUALLY')),
    CONSTRAINT valid_status CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'PENDING_SIGNATURE'))
);

-- Create indexes for performance
CREATE INDEX idx_contracts_team_id ON contracts(team_id);
CREATE INDEX idx_contracts_property_id ON contracts(property_id);
CREATE INDEX idx_contracts_tenant_id ON contracts(tenant_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_start_date ON contracts(start_date);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
CREATE INDEX idx_contracts_deleted_at ON contracts(deleted_at);

-- Index for finding active contracts by property
CREATE INDEX idx_contracts_active ON contracts(team_id, property_id, status)
WHERE deleted_at IS NULL;

-- Comments for documentation
COMMENT ON TABLE contracts IS 'Rental agreements between property owners and tenants';
COMMENT ON COLUMN contracts.identifier IS 'ULID identifier for external use';
COMMENT ON COLUMN contracts.contract_type IS 'Type of rental agreement (FIXED_TERM, INDEFINITE, FURNISHED, UNFURNISHED)';
COMMENT ON COLUMN contracts.payment_frequency IS 'How often rent is paid (MONTHLY, QUARTERLY, ANNUALLY)';
COMMENT ON COLUMN contracts.payment_due_day IS 'Day of month payment is due (1-31)';
COMMENT ON COLUMN contracts.auto_renewal IS 'Whether contract automatically renews at end date';
COMMENT ON COLUMN contracts.status IS 'Current status (DRAFT, ACTIVE, EXPIRED, TERMINATED, PENDING_SIGNATURE)';
