-- ---------------------------------------------------------------------------
-- contracts
-- ---------------------------------------------------------------------------
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    -- Contract type
    contract_type VARCHAR(50) NOT NULL,
    -- Dates
    start_date DATE NOT NULL,
    end_date DATE,
    signed_date DATE,
    -- Financial terms (minor units)
    rent_amount BIGINT NOT NULL,
    deposit_amount BIGINT,
    security_deposit BIGINT,
    -- Per-field currencies
    rent_amount_currency VARCHAR(3) NOT NULL,
    deposit_amount_currency VARCHAR(3),
    security_deposit_currency VARCHAR(3),
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
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_contracts_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contracts_rent_amount CHECK (rent_amount > 0),
    CONSTRAINT chk_contracts_deposit_amount CHECK (
        deposit_amount IS NULL
        OR deposit_amount >= 0
    ),
    CONSTRAINT chk_contracts_security_deposit CHECK (
        security_deposit IS NULL
        OR security_deposit >= 0
    ),
    CONSTRAINT chk_contracts_payment_due_day CHECK (
        payment_due_day IS NULL
        OR (
            payment_due_day >= 1
            AND payment_due_day <= 31
        )
    ),
    CONSTRAINT chk_contracts_late_fee CHECK (
        late_fee_percentage IS NULL
        OR (
            late_fee_percentage >= 0
            AND late_fee_percentage <= 100
        )
    ),
    CONSTRAINT chk_contracts_end_date CHECK (
        end_date IS NULL
        OR end_date >= start_date
    )
);

CREATE INDEX idx_contracts_team_id ON contracts (team_id);

CREATE INDEX idx_contracts_property_id ON contracts (property_id);

CREATE INDEX idx_contracts_status ON contracts (status);

CREATE INDEX idx_contracts_start_date ON contracts (start_date);

CREATE INDEX idx_contracts_end_date ON contracts (end_date);

CREATE INDEX idx_contracts_deleted_at ON contracts (deleted_at);

CREATE INDEX idx_contracts_active ON contracts (team_id, property_id, status)
WHERE
    deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- contract_parties
-- ---------------------------------------------------------------------------
CREATE TABLE contract_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_contract_parties_team_identifier UNIQUE (team_id, identifier)
);

CREATE UNIQUE INDEX uq_contract_parties_contract_tenant ON contract_parties (contract_id, tenant_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contract_parties_team_id ON contract_parties (team_id);

CREATE INDEX idx_contract_parties_contract_id ON contract_parties (contract_id);

CREATE INDEX idx_contract_parties_tenant_id ON contract_parties (tenant_id);

CREATE INDEX idx_contract_parties_role ON contract_parties (role);

-- ---------------------------------------------------------------------------
-- contract_rent_periods
-- ---------------------------------------------------------------------------
CREATE TABLE contract_rent_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    rent_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_rent_period_amount_positive CHECK (rent_amount > 0),
    CONSTRAINT uq_contract_rent_period_effective_from UNIQUE (contract_id, effective_from)
);

CREATE INDEX idx_contract_rent_periods_team_id ON contract_rent_periods (team_id);

CREATE INDEX idx_contract_rent_periods_contract_id ON contract_rent_periods (contract_id);

CREATE INDEX idx_contract_rent_periods_effective_from ON contract_rent_periods (effective_from);

-- ---------------------------------------------------------------------------
-- payments
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    -- Financial details (minor units)
    amount BIGINT NOT NULL,
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
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
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

-- ---------------------------------------------------------------------------
-- payment_receivals
-- ---------------------------------------------------------------------------
CREATE TABLE payment_receivals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    payment_id UUID NOT NULL REFERENCES payments (id),
    -- Financial details (minor units)
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    receival_date DATE NOT NULL,
    -- Additional details
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_receivals_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_receivals_amount CHECK (amount > 0)
);

CREATE INDEX idx_payment_receivals_team_id ON payment_receivals (team_id);

CREATE INDEX idx_payment_receivals_payment_id ON payment_receivals (payment_id);

CREATE INDEX idx_payment_receivals_receival_date ON payment_receivals (receival_date);

CREATE INDEX idx_payment_receivals_deleted_at ON payment_receivals (deleted_at);

-- ---------------------------------------------------------------------------
-- payment_instructions
-- ---------------------------------------------------------------------------
CREATE TABLE payment_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    payment_method VARCHAR(50) NOT NULL,
    bank_name VARCHAR(255),
    account_holder_name VARCHAR(255),
    iban VARCHAR(34),
    bic_swift VARCHAR(11),
    account_number VARCHAR(50),
    routing_number VARCHAR(50),
    payment_reference VARCHAR(255),
    additional_details TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_pi_team_identifier ON payment_instructions (team_id, identifier);

CREATE INDEX idx_pi_team ON payment_instructions (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_pi_deleted_at ON payment_instructions (deleted_at);

CREATE UNIQUE INDEX idx_pi_team_default ON payment_instructions (team_id)
WHERE
    deleted_at IS NULL
    AND is_default = TRUE;

-- ---------------------------------------------------------------------------
-- contract_payment_instructions
-- ---------------------------------------------------------------------------
CREATE TABLE contract_payment_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts (id),
    payment_instruction_id UUID REFERENCES payment_instructions (id),
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    custom_name VARCHAR(255),
    custom_description TEXT,
    custom_payment_method VARCHAR(50),
    custom_bank_name VARCHAR(255),
    custom_account_holder_name VARCHAR(255),
    custom_iban VARCHAR(34),
    custom_bic_swift VARCHAR(11),
    custom_account_number VARCHAR(50),
    custom_routing_number VARCHAR(50),
    custom_payment_reference VARCHAR(255),
    custom_additional_details TEXT,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT chk_cpi_dates CHECK (
        effective_to IS NULL
        OR effective_to >= effective_from
    ),
    CONSTRAINT chk_cpi_source CHECK (
        (is_custom = TRUE)
        OR (payment_instruction_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_cpi_team_identifier ON contract_payment_instructions (team_id, identifier);

CREATE INDEX idx_cpi_contract ON contract_payment_instructions (team_id, contract_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_cpi_template ON contract_payment_instructions (payment_instruction_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_cpi_deleted_at ON contract_payment_instructions (deleted_at);

-- ---------------------------------------------------------------------------
-- expenses
-- ---------------------------------------------------------------------------
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    -- Expense details
    category VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    expense_date DATE NOT NULL,
    -- Description
    description TEXT NOT NULL,
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_expenses_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_expenses_amount CHECK (amount > 0)
);

CREATE INDEX idx_expenses_team_id ON expenses (team_id);

CREATE INDEX idx_expenses_property_id ON expenses (property_id);

CREATE INDEX idx_expenses_category ON expenses (category);

CREATE INDEX idx_expenses_expense_date ON expenses (expense_date);

CREATE INDEX idx_expenses_deleted_at ON expenses (deleted_at);

CREATE INDEX idx_expenses_reporting ON expenses (team_id, expense_date, category)
WHERE
    deleted_at IS NULL;
