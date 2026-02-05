CREATE TABLE contracts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier              VARCHAR(29) NOT NULL,
    team_id                 UUID NOT NULL REFERENCES teams(id),
    property_id             UUID NOT NULL REFERENCES properties(id),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),

    -- Contract type
    contract_type           VARCHAR(50) NOT NULL,

    -- Dates
    start_date              DATE NOT NULL,
    end_date                DATE,
    signed_date             DATE,

    -- Financial terms
    rent_amount             DECIMAL(10, 2) NOT NULL,
    deposit_amount          DECIMAL(10, 2),
    security_deposit        DECIMAL(10, 2),
    currency                VARCHAR(3) DEFAULT 'EUR',

    -- Payment terms
    payment_frequency       VARCHAR(20) NOT NULL,
    payment_due_day         INTEGER,

    -- Renewal & Termination
    auto_renewal            BOOLEAN DEFAULT FALSE,
    renewal_notice_days     INTEGER DEFAULT 30,
    termination_notice_days INTEGER DEFAULT 30,

    -- Fees
    late_fee_percentage     DECIMAL(5, 2),

    -- Status
    status                  VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Additional details
    terms_and_conditions    TEXT,
    notes                   TEXT,

    -- Audit
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              UUID NOT NULL REFERENCES users(id),
    updated_by              UUID NOT NULL REFERENCES users(id),
    deleted_at              TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_contracts_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contracts_contract_type CHECK (contract_type IN ('FIXED_TERM', 'INDEFINITE', 'FURNISHED', 'UNFURNISHED')),
    CONSTRAINT chk_contracts_payment_frequency CHECK (payment_frequency IN ('MONTHLY', 'QUARTERLY', 'ANNUALLY')),
    CONSTRAINT chk_contracts_status CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'PENDING_SIGNATURE')),
    CONSTRAINT chk_contracts_rent_amount CHECK (rent_amount > 0),
    CONSTRAINT chk_contracts_deposit_amount CHECK (deposit_amount IS NULL OR deposit_amount >= 0),
    CONSTRAINT chk_contracts_security_deposit CHECK (security_deposit IS NULL OR security_deposit >= 0),
    CONSTRAINT chk_contracts_payment_due_day CHECK (payment_due_day IS NULL OR (payment_due_day >= 1 AND payment_due_day <= 31)),
    CONSTRAINT chk_contracts_late_fee CHECK (late_fee_percentage IS NULL OR (late_fee_percentage >= 0 AND late_fee_percentage <= 100)),
    CONSTRAINT chk_contracts_end_date CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_contracts_team_id ON contracts(team_id);
CREATE INDEX idx_contracts_property_id ON contracts(property_id);
CREATE INDEX idx_contracts_tenant_id ON contracts(tenant_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_start_date ON contracts(start_date);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
CREATE INDEX idx_contracts_deleted_at ON contracts(deleted_at);
CREATE INDEX idx_contracts_active ON contracts(team_id, property_id, status) WHERE deleted_at IS NULL;
