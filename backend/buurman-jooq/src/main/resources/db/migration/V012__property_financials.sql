-- =============================================================================
-- V012__property_financials.sql
-- Property Financials Module: acquisitions, valuations, financings,
-- financing payments, insurances, taxes, fees
-- =============================================================================
-- =============================================================================
-- 1. property_acquisitions (1:1 with property)
-- =============================================================================
-- Extracted from properties table columns: purchase_price, purchase_date,
-- land_value, depreciation_method, depreciation_years
CREATE TABLE property_acquisitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Acquisition details
    acquisition_type VARCHAR(30) NOT NULL DEFAULT 'PURCHASE',
    -- PURCHASE, INHERITANCE, GIFT, FORECLOSURE, AUCTION, OTHER
    acquisition_date DATE,
    -- Pricing (minor units)
    purchase_price BIGINT,
    purchase_price_currency VARCHAR(3),
    closing_costs BIGINT,
    closing_costs_currency VARCHAR(3),
    renovation_costs BIGINT,
    renovation_costs_currency VARCHAR(3),
    -- Land & Depreciation
    land_value BIGINT,
    land_value_currency VARCHAR(3),
    depreciation_method VARCHAR(30),
    -- STRAIGHT_LINE, DECLINING_BALANCE, NONE
    depreciation_years INTEGER,
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_acquisitions_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_acquisitions_property UNIQUE (property_id, team_id),
    CONSTRAINT chk_acquisitions_purchase_price CHECK (
        purchase_price IS NULL
        OR purchase_price > 0
    ),
    CONSTRAINT chk_acquisitions_closing_costs CHECK (
        closing_costs IS NULL
        OR closing_costs >= 0
    ),
    CONSTRAINT chk_acquisitions_renovation_costs CHECK (
        renovation_costs IS NULL
        OR renovation_costs >= 0
    ),
    CONSTRAINT chk_acquisitions_land_value CHECK (
        land_value IS NULL
        OR land_value >= 0
    ),
    CONSTRAINT chk_acquisitions_depreciation_years CHECK (
        depreciation_years IS NULL
        OR depreciation_years > 0
    )
);

CREATE INDEX idx_acquisitions_team ON property_acquisitions (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_acquisitions_property ON property_acquisitions (team_id, property_id)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 2. property_valuations (1:N with property, time series)
-- =============================================================================
-- Replaces properties.current_market_value / market_value_date
-- Allows historical tracking of property values over time
CREATE TABLE property_valuations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Valuation details
    valuation_type VARCHAR(30) NOT NULL DEFAULT 'MARKET',
    -- MARKET, APPRAISAL, TAX_ASSESSED, PURCHASE, INSURANCE, USER_ESTIMATE
    valuation_date DATE NOT NULL,
    -- Amount (minor units)
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    -- Source
    source VARCHAR(100),
    -- e.g. "Zillow", "County Assessor", "Licensed Appraiser", "Owner Estimate"
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_valuations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_valuations_amount CHECK (amount > 0)
);

CREATE INDEX idx_valuations_team ON property_valuations (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_valuations_property ON property_valuations (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_valuations_date ON property_valuations (property_id, valuation_date DESC)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 3. property_financings (1:N with property)
-- =============================================================================
-- Replaces properties.mortgage_* columns
-- Supports multiple financing instruments per property (mortgage, leasing, etc.)
CREATE TABLE property_financings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Financing type
    financing_type VARCHAR(30) NOT NULL,
    -- MORTGAGE, LEASING, LOAN, LINE_OF_CREDIT, PRIVATE_FINANCING, OTHER
    -- Rate type
    rate_type VARCHAR(30) NOT NULL DEFAULT 'FIXED',
    -- FIXED, VARIABLE, INTEREST_ONLY, HYBRID
    -- Lender
    lender_name VARCHAR(255),
    loan_number VARCHAR(100),
    -- Amounts (minor units)
    original_amount BIGINT NOT NULL,
    original_amount_currency VARCHAR(3) NOT NULL,
    current_balance BIGINT,
    current_balance_currency VARCHAR(3),
    -- Rate
    interest_rate DECIMAL(6, 4),
    -- e.g. 3.7500 = 3.75%
    -- Payment
    monthly_payment BIGINT,
    monthly_payment_currency VARCHAR(3),
    payment_variable BOOLEAN NOT NULL DEFAULT FALSE,
    -- Term
    start_date DATE NOT NULL,
    end_date DATE,
    term_months INTEGER,
    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE, PAID_OFF, REFINANCED, DEFAULTED
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_financings_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_financings_original_amount CHECK (original_amount > 0),
    CONSTRAINT chk_financings_current_balance CHECK (
        current_balance IS NULL
        OR current_balance >= 0
    ),
    CONSTRAINT chk_financings_interest_rate CHECK (
        interest_rate IS NULL
        OR (
            interest_rate >= 0
            AND interest_rate <= 100
        )
    ),
    CONSTRAINT chk_financings_monthly_payment CHECK (
        monthly_payment IS NULL
        OR monthly_payment > 0
    ),
    CONSTRAINT chk_financings_term_months CHECK (
        term_months IS NULL
        OR term_months > 0
    ),
    CONSTRAINT chk_financings_dates CHECK (
        end_date IS NULL
        OR end_date > start_date
    )
);

CREATE INDEX idx_financings_team ON property_financings (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_financings_property ON property_financings (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_financings_status ON property_financings (team_id, status)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 4. financing_payments (1:N with financing, time series)
-- =============================================================================
-- Individual payments against a financing instrument
CREATE TABLE financing_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    financing_id UUID NOT NULL REFERENCES property_financings (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Payment details
    payment_date DATE NOT NULL,
    -- Amounts (minor units) -- all in same currency as the financing
    total_amount BIGINT NOT NULL,
    principal_amount BIGINT,
    interest_amount BIGINT,
    escrow_amount BIGINT,
    extra_payment BIGINT,
    currency VARCHAR(3) NOT NULL,
    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    -- SCHEDULED, COMPLETED, MISSED, LATE
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_fpayments_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_fpayments_total_amount CHECK (total_amount > 0),
    CONSTRAINT chk_fpayments_principal CHECK (
        principal_amount IS NULL
        OR principal_amount >= 0
    ),
    CONSTRAINT chk_fpayments_interest CHECK (
        interest_amount IS NULL
        OR interest_amount >= 0
    ),
    CONSTRAINT chk_fpayments_escrow CHECK (
        escrow_amount IS NULL
        OR escrow_amount >= 0
    ),
    CONSTRAINT chk_fpayments_extra CHECK (
        extra_payment IS NULL
        OR extra_payment >= 0
    )
);

CREATE INDEX idx_fpayments_team ON financing_payments (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_fpayments_financing ON financing_payments (team_id, financing_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_fpayments_date ON financing_payments (financing_id, payment_date DESC)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_fpayments_status ON financing_payments (team_id, status)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 5. property_insurances (1:N with property)
-- =============================================================================
-- Replaces properties.annual_insurance / annual_insurance_currency
CREATE TABLE property_insurances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Insurance details
    insurance_type VARCHAR(30) NOT NULL,
    -- BUILDING, LIABILITY, CONTENTS, FLOOD, EARTHQUAKE, UMBRELLA, RENT_GUARANTEE, OTHER
    provider VARCHAR(255),
    policy_number VARCHAR(100),
    -- Coverage (minor units)
    coverage_amount BIGINT,
    coverage_amount_currency VARCHAR(3),
    -- Premium (minor units)
    annual_premium BIGINT NOT NULL,
    annual_premium_currency VARCHAR(3) NOT NULL,
    payment_frequency VARCHAR(20) NOT NULL DEFAULT 'ANNUALLY',
    -- MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY
    -- Dates
    start_date DATE,
    end_date DATE,
    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE, EXPIRED, CANCELLED
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_insurances_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_insurances_coverage CHECK (
        coverage_amount IS NULL
        OR coverage_amount > 0
    ),
    CONSTRAINT chk_insurances_premium CHECK (annual_premium > 0),
    CONSTRAINT chk_insurances_dates CHECK (
        start_date IS NULL
        OR end_date IS NULL
        OR end_date > start_date
    )
);

CREATE INDEX idx_insurances_team ON property_insurances (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_insurances_property ON property_insurances (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_insurances_status ON property_insurances (team_id, status)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 6. property_taxes (1:N with property)
-- =============================================================================
-- Replaces properties.annual_property_tax
CREATE TABLE property_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Tax details
    tax_type VARCHAR(30) NOT NULL,
    -- PROPERTY, MUNICIPAL, STATE, LOCAL, LAND, SPECIAL_ASSESSMENT, OTHER
    authority VARCHAR(255),
    -- e.g. "Amsterdam Municipality", "Province of North Holland"
    -- Amount (minor units)
    annual_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_frequency VARCHAR(20) NOT NULL DEFAULT 'ANNUALLY',
    -- MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY
    due_months VARCHAR(50),
    -- Comma-separated month numbers: "1,3,7" (reusing existing pattern)
    -- Period
    tax_year INTEGER,
    start_date DATE,
    end_date DATE,
    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE, EXPIRED, EXEMPT
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_taxes_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_taxes_annual_amount CHECK (annual_amount > 0),
    CONSTRAINT chk_taxes_year CHECK (
        tax_year IS NULL
        OR (
            tax_year >= 1900
            AND tax_year <= 2100
        )
    ),
    CONSTRAINT chk_taxes_dates CHECK (
        start_date IS NULL
        OR end_date IS NULL
        OR end_date >= start_date
    )
);

CREATE INDEX idx_taxes_team ON property_taxes (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_taxes_property ON property_taxes (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_taxes_status ON property_taxes (team_id, status)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- 7. property_fees (1:N with property)
-- =============================================================================
-- Replaces properties.annual_hoa_fee, annual_management_fee, annual_maintenance_reserve
CREATE TABLE property_fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Fee details
    fee_type VARCHAR(30) NOT NULL,
    -- HOA, MANAGEMENT, MAINTENANCE_RESERVE, CLEANING, GARDENING, SECURITY,
    -- WASTE_MANAGEMENT, WATER, UTILITIES, OTHER
    name VARCHAR(255),
    -- Amount (minor units)
    annual_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    -- MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY
    due_months VARCHAR(50),
    -- Comma-separated month numbers (reusing existing pattern)
    -- Period
    start_date DATE,
    end_date DATE,
    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE, EXPIRED, CANCELLED
    -- Notes
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_fees_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_fees_annual_amount CHECK (annual_amount > 0),
    CONSTRAINT chk_fees_dates CHECK (
        start_date IS NULL
        OR end_date IS NULL
        OR end_date >= start_date
    )
);

CREATE INDEX idx_fees_team ON property_fees (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_fees_property ON property_fees (team_id, property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_fees_status ON property_fees (team_id, status)
WHERE
    deleted_at IS NULL;
