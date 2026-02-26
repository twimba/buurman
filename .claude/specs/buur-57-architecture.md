# BUUR-57: Property Financials Module - Technical Architecture

## 1. Overview

Extract mortgage/financing data from the monolithic `properties` table into dedicated, normalized tables. Introduce a complete property financials module covering: acquisitions, valuations, financing, financing payments, insurances, taxes, and fees. Remove the `MORTGAGE_PAYMENT` expense category. Update dashboards, calculations, and reports.

### Design Principles
- **Normalize**: Move financial columns from `properties` to dedicated tables with proper lifecycle
- **Multi-entity per property**: A property can have multiple financings, insurances, taxes, fees
- **Temporal data**: Valuations and financing payments are time-series data
- **Backward compatible**: Migrate existing data, deprecate old columns gracefully
- **Pattern aligned**: Follow every existing codebase convention exactly

---

## 2. Database Schema Design

### Migration: V014__property_financials.sql

All tables follow existing conventions:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `identifier VARCHAR(29) NOT NULL` (ULID with 3-char prefix)
- `team_id UUID NOT NULL REFERENCES teams(id)`
- `property_id UUID NOT NULL REFERENCES properties(id)`
- Audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`
- Soft delete: `deleted_at TIMESTAMP`
- `UNIQUE(team_id, identifier)` constraint
- Monetary amounts stored as `BIGINT` (minor units) with companion `_currency VARCHAR(3)` column
- Partial indexes with `WHERE deleted_at IS NULL`

```sql
-- =============================================================================
-- V014__property_financials.sql
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
    -- Computed: purchase_price + closing_costs + renovation_costs
    -- (calculated in service layer, not stored)
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
        purchase_price IS NULL OR purchase_price > 0
    ),
    CONSTRAINT chk_acquisitions_closing_costs CHECK (
        closing_costs IS NULL OR closing_costs >= 0
    ),
    CONSTRAINT chk_acquisitions_renovation_costs CHECK (
        renovation_costs IS NULL OR renovation_costs >= 0
    ),
    CONSTRAINT chk_acquisitions_land_value CHECK (
        land_value IS NULL OR land_value >= 0
    ),
    CONSTRAINT chk_acquisitions_depreciation_years CHECK (
        depreciation_years IS NULL OR depreciation_years > 0
    )
);

CREATE INDEX idx_acquisitions_team ON property_acquisitions (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_acquisitions_property ON property_acquisitions (team_id, property_id)
WHERE deleted_at IS NULL;

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
WHERE deleted_at IS NULL;

CREATE INDEX idx_valuations_property ON property_valuations (team_id, property_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_valuations_date ON property_valuations (property_id, valuation_date DESC)
WHERE deleted_at IS NULL;

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
        current_balance IS NULL OR current_balance >= 0
    ),
    CONSTRAINT chk_financings_interest_rate CHECK (
        interest_rate IS NULL OR (interest_rate >= 0 AND interest_rate <= 100)
    ),
    CONSTRAINT chk_financings_monthly_payment CHECK (
        monthly_payment IS NULL OR monthly_payment > 0
    ),
    CONSTRAINT chk_financings_term_months CHECK (
        term_months IS NULL OR term_months > 0
    ),
    CONSTRAINT chk_financings_dates CHECK (
        end_date IS NULL OR end_date > start_date
    )
);

CREATE INDEX idx_financings_team ON property_financings (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_financings_property ON property_financings (team_id, property_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_financings_status ON property_financings (team_id, status)
WHERE deleted_at IS NULL;

-- =============================================================================
-- 4. financing_payments (1:N with financing, time series)
-- =============================================================================
-- Individual payments against a financing instrument
-- Replaces MORTGAGE_PAYMENT expenses with structured payment breakdowns
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
        principal_amount IS NULL OR principal_amount >= 0
    ),
    CONSTRAINT chk_fpayments_interest CHECK (
        interest_amount IS NULL OR interest_amount >= 0
    ),
    CONSTRAINT chk_fpayments_escrow CHECK (
        escrow_amount IS NULL OR escrow_amount >= 0
    ),
    CONSTRAINT chk_fpayments_extra CHECK (
        extra_payment IS NULL OR extra_payment >= 0
    )
);

CREATE INDEX idx_fpayments_team ON financing_payments (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_fpayments_financing ON financing_payments (team_id, financing_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_fpayments_date ON financing_payments (financing_id, payment_date DESC)
WHERE deleted_at IS NULL;

CREATE INDEX idx_fpayments_status ON financing_payments (team_id, status)
WHERE deleted_at IS NULL;

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
        coverage_amount IS NULL OR coverage_amount > 0
    ),
    CONSTRAINT chk_insurances_premium CHECK (annual_premium > 0),
    CONSTRAINT chk_insurances_dates CHECK (
        start_date IS NULL OR end_date IS NULL OR end_date > start_date
    )
);

CREATE INDEX idx_insurances_team ON property_insurances (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_insurances_property ON property_insurances (team_id, property_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_insurances_status ON property_insurances (team_id, status)
WHERE deleted_at IS NULL;

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
        tax_year IS NULL OR (tax_year >= 1900 AND tax_year <= 2100)
    ),
    CONSTRAINT chk_taxes_dates CHECK (
        start_date IS NULL OR end_date IS NULL OR end_date >= start_date
    )
);

CREATE INDEX idx_taxes_team ON property_taxes (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_taxes_property ON property_taxes (team_id, property_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_taxes_status ON property_taxes (team_id, status)
WHERE deleted_at IS NULL;

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
        start_date IS NULL OR end_date IS NULL OR end_date >= start_date
    )
);

CREATE INDEX idx_fees_team ON property_fees (team_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_fees_property ON property_fees (team_id, property_id)
WHERE deleted_at IS NULL;

CREATE INDEX idx_fees_status ON property_fees (team_id, status)
WHERE deleted_at IS NULL;
```

---

## 3. Migration Strategy

### V015__migrate_property_financials.sql

Migrate existing data from `properties` into the new tables, then deprecate old columns.

```sql
-- =============================================================================
-- V015__migrate_property_financials.sql
-- Migrate existing property financial data to normalized tables
-- =============================================================================

-- 1. Migrate purchase data to property_acquisitions
INSERT INTO property_acquisitions (
    identifier, property_id, team_id,
    acquisition_type, acquisition_date,
    purchase_price, purchase_price_currency,
    land_value, land_value_currency,
    depreciation_method, depreciation_years,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'ACQ' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'PURCHASE', p.purchase_date,
    p.purchase_price, p.purchase_price_currency,
    p.land_value, p.land_value_currency,
    p.depreciation_method, p.depreciation_years,
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND (p.purchase_price IS NOT NULL OR p.purchase_date IS NOT NULL);

-- 2. Migrate current market value to property_valuations
INSERT INTO property_valuations (
    identifier, property_id, team_id,
    valuation_type, valuation_date,
    amount, currency,
    source,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'VAL' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'MARKET', COALESCE(p.market_value_date, p.updated_at::DATE),
    p.current_market_value, COALESCE(p.current_market_value_currency, 'EUR'),
    'Migrated from property record',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.current_market_value IS NOT NULL;

-- 3. Migrate mortgage data to property_financings
INSERT INTO property_financings (
    identifier, property_id, team_id,
    financing_type, rate_type,
    original_amount, original_amount_currency,
    current_balance, current_balance_currency,
    interest_rate,
    monthly_payment, monthly_payment_currency,
    payment_variable,
    start_date, end_date,
    status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'FIN' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'MORTGAGE',
    CASE p.mortgage_type
        WHEN 'FIXED_RATE' THEN 'FIXED'
        WHEN 'VARIABLE_RATE' THEN 'VARIABLE'
        WHEN 'INTEREST_ONLY' THEN 'INTEREST_ONLY'
        ELSE 'FIXED'
    END,
    p.mortgage_amount, COALESCE(p.mortgage_amount_currency, 'EUR'),
    p.mortgage_amount, COALESCE(p.mortgage_amount_currency, 'EUR'),
    p.mortgage_interest_rate,
    p.monthly_mortgage_payment, p.monthly_mortgage_payment_currency,
    p.mortgage_payment_variable,
    COALESCE(p.mortgage_start_date, p.purchase_date, p.created_at::DATE),
    p.mortgage_end_date,
    'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.mortgage_amount IS NOT NULL
  AND p.mortgage_type IS NOT NULL
  AND p.mortgage_type != 'NONE';

-- 4. Migrate annual insurance to property_insurances
INSERT INTO property_insurances (
    identifier, property_id, team_id,
    insurance_type, annual_premium, annual_premium_currency,
    payment_frequency, status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'INS' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'BUILDING', p.annual_insurance, COALESCE(p.annual_insurance_currency, 'EUR'),
    'ANNUALLY', 'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.annual_insurance IS NOT NULL
  AND p.annual_insurance > 0;

-- 5. Migrate annual property tax to property_taxes
INSERT INTO property_taxes (
    identifier, property_id, team_id,
    tax_type, annual_amount, currency,
    payment_frequency, due_months, status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'TAX' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'PROPERTY', p.annual_property_tax, COALESCE(p.annual_property_tax_currency, 'EUR'),
    'ANNUALLY', p.annual_property_tax_due_month, 'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.annual_property_tax IS NOT NULL
  AND p.annual_property_tax > 0;

-- 6. Migrate HOA fees to property_fees
INSERT INTO property_fees (
    identifier, property_id, team_id,
    fee_type, annual_amount, currency,
    payment_frequency, due_months, status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'FEE' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'HOA', p.annual_hoa_fee, COALESCE(p.annual_hoa_fee_currency, 'EUR'),
    'ANNUALLY', p.annual_hoa_fee_due_month, 'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.annual_hoa_fee IS NOT NULL
  AND p.annual_hoa_fee > 0;

-- 7. Migrate management fees
INSERT INTO property_fees (
    identifier, property_id, team_id,
    fee_type, annual_amount, currency,
    payment_frequency, due_months, status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'FEE' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'MANAGEMENT', p.annual_management_fee, COALESCE(p.annual_management_fee_currency, 'EUR'),
    'ANNUALLY', p.annual_management_fee_due_month, 'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.annual_management_fee IS NOT NULL
  AND p.annual_management_fee > 0;

-- 8. Migrate maintenance reserve fees
INSERT INTO property_fees (
    identifier, property_id, team_id,
    fee_type, annual_amount, currency,
    payment_frequency, due_months, status,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'FEE' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    p.id, p.team_id,
    'MAINTENANCE_RESERVE', p.annual_maintenance_reserve, COALESCE(p.annual_maintenance_reserve_currency, 'EUR'),
    'ANNUALLY', p.annual_maintenance_reserve_due_month, 'ACTIVE',
    p.created_at, p.updated_at, p.created_by, p.updated_by
FROM properties p
WHERE p.deleted_at IS NULL
  AND p.annual_maintenance_reserve IS NOT NULL
  AND p.annual_maintenance_reserve > 0;

-- 9. Migrate MORTGAGE_PAYMENT expenses to financing_payments
-- Link them to the financing record for the same property
INSERT INTO financing_payments (
    identifier, financing_id, team_id,
    payment_date, total_amount, currency,
    status, notes,
    created_at, updated_at, created_by, updated_by
)
SELECT
    'FPY' || upper(substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)),
    f.id, e.team_id,
    e.expense_date, e.amount, COALESCE(e.currency, 'EUR'),
    'COMPLETED', COALESCE(e.description, '') || COALESCE(' | ' || e.notes, ''),
    e.created_at, e.updated_at, e.created_by, e.updated_by
FROM expenses e
JOIN property_financings f ON f.property_id = e.property_id AND f.team_id = e.team_id
WHERE e.deleted_at IS NULL
  AND e.category = 'MORTGAGE_PAYMENT'
  AND f.deleted_at IS NULL;

-- 10. Soft-delete migrated MORTGAGE_PAYMENT expenses
UPDATE expenses
SET deleted_at = now()
WHERE category = 'MORTGAGE_PAYMENT'
  AND deleted_at IS NULL;
```

### V016__deprecate_property_financial_columns.sql (Phase 2 - separate migration)

```sql
-- =============================================================================
-- V016__deprecate_property_financial_columns.sql
-- Mark old financial columns as deprecated.
-- Actual column removal deferred to a later release after all code paths updated.
-- =============================================================================

-- Add comment to mark deprecated columns (columns remain for backward compatibility)
COMMENT ON COLUMN properties.purchase_price IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.purchase_price_currency IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.purchase_date IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.current_market_value IS 'DEPRECATED: Use property_valuations table';
COMMENT ON COLUMN properties.current_market_value_currency IS 'DEPRECATED: Use property_valuations table';
COMMENT ON COLUMN properties.market_value_date IS 'DEPRECATED: Use property_valuations table';
COMMENT ON COLUMN properties.mortgage_type IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_amount IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_amount_currency IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_interest_rate IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_start_date IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_end_date IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.monthly_mortgage_payment IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.monthly_mortgage_payment_currency IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.mortgage_payment_variable IS 'DEPRECATED: Use property_financings table';
COMMENT ON COLUMN properties.annual_property_tax IS 'DEPRECATED: Use property_taxes table';
COMMENT ON COLUMN properties.annual_property_tax_currency IS 'DEPRECATED: Use property_taxes table';
COMMENT ON COLUMN properties.annual_property_tax_due_month IS 'DEPRECATED: Use property_taxes table';
COMMENT ON COLUMN properties.annual_insurance IS 'DEPRECATED: Use property_insurances table';
COMMENT ON COLUMN properties.annual_insurance_currency IS 'DEPRECATED: Use property_insurances table';
COMMENT ON COLUMN properties.annual_insurance_due_month IS 'DEPRECATED: Use property_insurances table';
COMMENT ON COLUMN properties.annual_hoa_fee IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_hoa_fee_currency IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_hoa_fee_due_month IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_management_fee IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_management_fee_currency IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_management_fee_due_month IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_maintenance_reserve IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_maintenance_reserve_currency IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.annual_maintenance_reserve_due_month IS 'DEPRECATED: Use property_fees table';
COMMENT ON COLUMN properties.land_value IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.land_value_currency IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.depreciation_method IS 'DEPRECATED: Use property_acquisitions table';
COMMENT ON COLUMN properties.depreciation_years IS 'DEPRECATED: Use property_acquisitions table';

-- Remove MORTGAGE_PAYMENT from expense categories (soft: just comment)
-- Actual enum removal happens in Java code
```

### Phased Approach

| Phase | Description | Migration |
|-------|-------------|-----------|
| 1 | Create new tables, migrate data, soft-delete mortgage expenses | V014, V015 |
| 2 | Comment deprecated columns, code reads from new tables | V016 |
| 3 | (Future) Drop deprecated columns from properties table | V017+ (later release) |

---

## 4. Backend Architecture

### 4.1 New Entity Prefixes (EntityPrefix.java additions)

```java
ACQ("ACQ", "Property Acquisitions"),
VAL("VAL", "Property Valuations"),
FIN("FIN", "Property Financings"),
FPY("FPY", "Financing Payments"),
INS("INS", "Property Insurances"),
PTX("PTX", "Property Taxes"),
FEE("FEE", "Property Fees"),
```

### 4.2 UlidGenerator.java additions

```java
public static Ulid newAcquisitionId() { return generate(EntityPrefix.ACQ); }
public static Ulid newValuationId()   { return generate(EntityPrefix.VAL); }
public static Ulid newFinancingId()   { return generate(EntityPrefix.FIN); }
public static Ulid newFinancingPaymentId() { return generate(EntityPrefix.FPY); }
public static Ulid newInsuranceId()   { return generate(EntityPrefix.INS); }
public static Ulid newPropertyTaxId() { return generate(EntityPrefix.PTX); }
public static Ulid newPropertyFeeId() { return generate(EntityPrefix.FEE); }
```

### 4.3 Domain Classes

All domain classes follow the established pattern:
- `@SuppressWarnings("NullAway.Init")`
- `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- `@Builder.Default private Optional<T> field = Optional.empty()` for nullable fields
- Enums defined as inner classes

#### PropertyAcquisition.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyAcquisition {

  public enum AcquisitionType {
    PURCHASE, INHERITANCE, GIFT, FORECLOSURE, AUCTION, OTHER
  }

  public enum DepreciationMethod {
    STRAIGHT_LINE, DECLINING_BALANCE, NONE
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private AcquisitionType acquisitionType;
  @Builder.Default private Optional<LocalDate> acquisitionDate = Optional.empty();
  @Builder.Default private Optional<BigDecimal> purchasePrice = Optional.empty();
  @Builder.Default private Optional<String> purchasePriceCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> closingCosts = Optional.empty();
  @Builder.Default private Optional<String> closingCostsCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> renovationCosts = Optional.empty();
  @Builder.Default private Optional<String> renovationCostsCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> landValue = Optional.empty();
  @Builder.Default private Optional<String> landValueCurrency = Optional.empty();
  @Builder.Default private Optional<DepreciationMethod> depreciationMethod = Optional.empty();
  @Builder.Default private Optional<Integer> depreciationYears = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### PropertyValuation.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyValuation {

  public enum ValuationType {
    MARKET, APPRAISAL, TAX_ASSESSED, PURCHASE, INSURANCE, USER_ESTIMATE
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private ValuationType valuationType;
  private LocalDate valuationDate;
  private BigDecimal amount;
  private String currency;
  @Builder.Default private Optional<String> source = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### PropertyFinancing.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyFinancing {

  public enum FinancingType {
    MORTGAGE, LEASING, LOAN, LINE_OF_CREDIT, PRIVATE_FINANCING, OTHER
  }

  public enum RateType {
    FIXED, VARIABLE, INTEREST_ONLY, HYBRID
  }

  public enum FinancingStatus {
    ACTIVE, PAID_OFF, REFINANCED, DEFAULTED
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private FinancingType financingType;
  private RateType rateType;
  @Builder.Default private Optional<String> lenderName = Optional.empty();
  @Builder.Default private Optional<String> loanNumber = Optional.empty();
  private BigDecimal originalAmount;
  private String originalAmountCurrency;
  @Builder.Default private Optional<BigDecimal> currentBalance = Optional.empty();
  @Builder.Default private Optional<String> currentBalanceCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> interestRate = Optional.empty();
  @Builder.Default private Optional<BigDecimal> monthlyPayment = Optional.empty();
  @Builder.Default private Optional<String> monthlyPaymentCurrency = Optional.empty();
  private boolean paymentVariable;
  private LocalDate startDate;
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  @Builder.Default private Optional<Integer> termMonths = Optional.empty();
  private FinancingStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### FinancingPayment.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancingPayment {

  public enum PaymentStatus {
    SCHEDULED, COMPLETED, MISSED, LATE
  }

  private UUID id;
  private String identifier;
  private UUID financingId;
  private UUID teamId;
  private LocalDate paymentDate;
  private BigDecimal totalAmount;
  @Builder.Default private Optional<BigDecimal> principalAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> interestAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> escrowAmount = Optional.empty();
  @Builder.Default private Optional<BigDecimal> extraPayment = Optional.empty();
  private String currency;
  private PaymentStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### PropertyInsurance.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyInsurance {

  public enum InsuranceType {
    BUILDING, LIABILITY, CONTENTS, FLOOD, EARTHQUAKE, UMBRELLA, RENT_GUARANTEE, OTHER
  }

  public enum InsuranceStatus {
    ACTIVE, EXPIRED, CANCELLED
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private InsuranceType insuranceType;
  @Builder.Default private Optional<String> provider = Optional.empty();
  @Builder.Default private Optional<String> policyNumber = Optional.empty();
  @Builder.Default private Optional<BigDecimal> coverageAmount = Optional.empty();
  @Builder.Default private Optional<String> coverageAmountCurrency = Optional.empty();
  private BigDecimal annualPremium;
  private String annualPremiumCurrency;
  private String paymentFrequency;
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private InsuranceStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### PropertyTax.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyTax {

  public enum TaxType {
    PROPERTY, MUNICIPAL, STATE, LOCAL, LAND, SPECIAL_ASSESSMENT, OTHER
  }

  public enum TaxStatus {
    ACTIVE, EXPIRED, EXEMPT
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private TaxType taxType;
  @Builder.Default private Optional<String> authority = Optional.empty();
  private BigDecimal annualAmount;
  private String currency;
  private String paymentFrequency;
  @Builder.Default private Optional<String> dueMonths = Optional.empty();
  @Builder.Default private Optional<Integer> taxYear = Optional.empty();
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private TaxStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

#### PropertyFee.java
```java
@SuppressWarnings("NullAway.Init")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyFee {

  public enum FeeType {
    HOA, MANAGEMENT, MAINTENANCE_RESERVE, CLEANING, GARDENING, SECURITY,
    WASTE_MANAGEMENT, WATER, UTILITIES, OTHER
  }

  public enum FeeStatus {
    ACTIVE, EXPIRED, CANCELLED
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private FeeType feeType;
  @Builder.Default private Optional<String> name = Optional.empty();
  private BigDecimal annualAmount;
  private String currency;
  private String paymentFrequency;
  @Builder.Default private Optional<String> dueMonths = Optional.empty();
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private FeeStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

### 4.4 Repositories

Each repository follows the exact same pattern as `ExpenseRepository`:
- `@Repository @RequiredArgsConstructor`
- `private final DSLContext dsl; private final XxxRecordMapper mapper; private final Clock clock;`
- `findByIdentifierAndTeamId`, `getByIdentifierAndTeamId`, `findAllByTeamId`
- `findByPropertyIdAndTeamId` (for property sub-resources)
- `save()` with insert/update logic, minor-unit conversion
- `softDeleteByIdAndTeamId()`
- Paginated query with `PaginationHelper`

**New repositories (7 total):**

| Repository | Table | Key queries |
|-----------|-------|-------------|
| `PropertyAcquisitionRepository` | `property_acquisitions` | `findByPropertyIdAndTeamId` (returns Optional, since 1:1) |
| `PropertyValuationRepository` | `property_valuations` | `findByPropertyIdAndTeamId` (list, ordered by date DESC), `findLatestByPropertyIdAndTeamId` |
| `PropertyFinancingRepository` | `property_financings` | `findByPropertyIdAndTeamId` (list), `findActiveByPropertyIdAndTeamId`, `sumActiveBalanceByPropertyIdAndTeamId` |
| `FinancingPaymentRepository` | `financing_payments` | `findByFinancingIdAndTeamId` (list, paginated), `sumByFinancingIdAndTeamId`, `findByDateRange` |
| `PropertyInsuranceRepository` | `property_insurances` | `findByPropertyIdAndTeamId` (list), `findActiveByPropertyIdAndTeamId`, `sumAnnualPremiumsByPropertyIdAndTeamId` |
| `PropertyTaxRepository` | `property_taxes` | `findByPropertyIdAndTeamId` (list), `findActiveByPropertyIdAndTeamId`, `sumAnnualAmountsByPropertyIdAndTeamId` |
| `PropertyFeeRepository` | `property_fees` | `findByPropertyIdAndTeamId` (list), `findActiveByPropertyIdAndTeamId`, `sumAnnualAmountsByPropertyIdAndTeamId` |

**New record mappers (7 total):**

| Mapper | Input | Output |
|--------|-------|--------|
| `PropertyAcquisitionRecordMapper` | `PropertyAcquisitionsRecord` | `Optional<PropertyAcquisition>` |
| `PropertyValuationRecordMapper` | `PropertyValuationsRecord` | `Optional<PropertyValuation>` |
| `PropertyFinancingRecordMapper` | `PropertyFinancingsRecord` | `Optional<PropertyFinancing>` |
| `FinancingPaymentRecordMapper` | `FinancingPaymentsRecord` | `Optional<FinancingPayment>` |
| `PropertyInsuranceRecordMapper` | `PropertyInsurancesRecord` | `Optional<PropertyInsurance>` |
| `PropertyTaxRecordMapper` | `PropertyTaxesRecord` | `Optional<PropertyTax>` |
| `PropertyFeeRecordMapper` | `PropertyFeesRecord` | `Optional<PropertyFee>` |

### 4.5 Services

Each service follows the same pattern as `ExpenseService`:
- `@Service @Slf4j @RequiredArgsConstructor`
- `@Transactional` on mutations, `@Transactional(readOnly = true)` on reads
- `@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")` on mutations
- `@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")` on reads (via controller)
- Audit logging via `AuditService`
- Returns response DTOs

**New services (7+1 total):**

| Service | Domain | Key methods |
|---------|--------|-------------|
| `PropertyAcquisitionService` | Acquisitions | `getOrCreate`, `update`, `getByProperty` |
| `PropertyValuationService` | Valuations | `create`, `update`, `delete`, `listByProperty`, `getLatestByProperty` |
| `PropertyFinancingService` | Financings | `create`, `update`, `delete`, `listByProperty`, `getFinancing` |
| `FinancingPaymentService` | Financing payments | `create`, `update`, `delete`, `listByFinancing` (paginated), `getPayment` |
| `PropertyInsuranceService` | Insurances | `create`, `update`, `delete`, `listByProperty` |
| `PropertyTaxService` | Taxes | `create`, `update`, `delete`, `listByProperty` |
| `PropertyFeeService` | Fees | `create`, `update`, `delete`, `listByProperty` |
| `PropertyFinancialsService` | **Aggregator** | `getFinancialSummary(propertyId)` - combines data from all 6 sub-services into a unified financial overview for the property dashboard |

### 4.6 DTOs

#### Request DTOs (Java records)

```java
// Acquisitions
public record CreatePropertyAcquisitionRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyAcquisition.AcquisitionType acquisitionType,
    Optional<LocalDate> acquisitionDate,
    Optional<@Positive BigDecimal> purchasePrice,
    Optional<String> purchasePriceCurrency,
    Optional<@PositiveOrZero BigDecimal> closingCosts,
    Optional<String> closingCostsCurrency,
    Optional<@PositiveOrZero BigDecimal> renovationCosts,
    Optional<String> renovationCostsCurrency,
    Optional<@PositiveOrZero BigDecimal> landValue,
    Optional<String> landValueCurrency,
    Optional<PropertyAcquisition.DepreciationMethod> depreciationMethod,
    Optional<@Positive Integer> depreciationYears,
    Optional<String> notes) {}

// Valuations
public record CreatePropertyValuationRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyValuation.ValuationType valuationType,
    @NotNull LocalDate valuationDate,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    Optional<String> source,
    Optional<String> notes) {}

// Financings
public record CreatePropertyFinancingRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyFinancing.FinancingType financingType,
    @NotNull PropertyFinancing.RateType rateType,
    Optional<String> lenderName,
    Optional<String> loanNumber,
    @NotNull @Positive BigDecimal originalAmount,
    @NotBlank String originalAmountCurrency,
    Optional<@PositiveOrZero BigDecimal> currentBalance,
    Optional<String> currentBalanceCurrency,
    Optional<BigDecimal> interestRate,
    Optional<@Positive BigDecimal> monthlyPayment,
    Optional<String> monthlyPaymentCurrency,
    Optional<Boolean> paymentVariable,
    @NotNull LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<@Positive Integer> termMonths,
    Optional<String> notes) {}

// Financing Payments
public record CreateFinancingPaymentRequest(
    @NotNull LocalDate paymentDate,
    @NotNull @Positive BigDecimal totalAmount,
    Optional<@PositiveOrZero BigDecimal> principalAmount,
    Optional<@PositiveOrZero BigDecimal> interestAmount,
    Optional<@PositiveOrZero BigDecimal> escrowAmount,
    Optional<@PositiveOrZero BigDecimal> extraPayment,
    @NotBlank String currency,
    Optional<FinancingPayment.PaymentStatus> status,
    Optional<String> notes) {}

// Insurances
public record CreatePropertyInsuranceRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyInsurance.InsuranceType insuranceType,
    Optional<String> provider,
    Optional<String> policyNumber,
    Optional<@Positive BigDecimal> coverageAmount,
    Optional<String> coverageAmountCurrency,
    @NotNull @Positive BigDecimal annualPremium,
    @NotBlank String annualPremiumCurrency,
    @NotBlank String paymentFrequency,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<String> notes) {}

// Taxes
public record CreatePropertyTaxRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyTax.TaxType taxType,
    Optional<String> authority,
    @NotNull @Positive BigDecimal annualAmount,
    @NotBlank String currency,
    @NotBlank String paymentFrequency,
    Optional<String> dueMonths,
    Optional<Integer> taxYear,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<String> notes) {}

// Fees
public record CreatePropertyFeeRequest(
    @NotNull String propertyIdentifier,
    @NotNull PropertyFee.FeeType feeType,
    Optional<String> name,
    @NotNull @Positive BigDecimal annualAmount,
    @NotBlank String currency,
    @NotBlank String paymentFrequency,
    Optional<String> dueMonths,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<String> notes) {}
```

Update DTOs follow the same shape but with all fields Optional (for partial updates, following `UpdateExpenseRequest` pattern).

#### Response DTOs (Java records)

```java
// Acquisitions
public record PropertyAcquisitionResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyAcquisition.AcquisitionType acquisitionType,
    Optional<LocalDate> acquisitionDate,
    Optional<BigDecimal> purchasePrice,
    Optional<String> purchasePriceCurrency,
    Optional<BigDecimal> closingCosts,
    Optional<String> closingCostsCurrency,
    Optional<BigDecimal> renovationCosts,
    Optional<String> renovationCostsCurrency,
    Optional<BigDecimal> costBasis,  // computed: purchase + closing + renovation
    Optional<BigDecimal> landValue,
    Optional<String> landValueCurrency,
    Optional<PropertyAcquisition.DepreciationMethod> depreciationMethod,
    Optional<Integer> depreciationYears,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Valuations
public record PropertyValuationResponse(
    String identifier,
    PropertyValuation.ValuationType valuationType,
    LocalDate valuationDate,
    BigDecimal amount,
    String currency,
    Optional<String> source,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Financings
public record PropertyFinancingResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyFinancing.FinancingType financingType,
    PropertyFinancing.RateType rateType,
    Optional<String> lenderName,
    Optional<String> loanNumber,
    BigDecimal originalAmount,
    String originalAmountCurrency,
    Optional<BigDecimal> currentBalance,
    Optional<String> currentBalanceCurrency,
    Optional<BigDecimal> interestRate,
    Optional<BigDecimal> monthlyPayment,
    Optional<String> monthlyPaymentCurrency,
    boolean paymentVariable,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<Integer> termMonths,
    PropertyFinancing.FinancingStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Financing Payments
public record FinancingPaymentResponse(
    String identifier,
    LocalDate paymentDate,
    BigDecimal totalAmount,
    Optional<BigDecimal> principalAmount,
    Optional<BigDecimal> interestAmount,
    Optional<BigDecimal> escrowAmount,
    Optional<BigDecimal> extraPayment,
    String currency,
    FinancingPayment.PaymentStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Insurances
public record PropertyInsuranceResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyInsurance.InsuranceType insuranceType,
    Optional<String> provider,
    Optional<String> policyNumber,
    Optional<BigDecimal> coverageAmount,
    Optional<String> coverageAmountCurrency,
    BigDecimal annualPremium,
    String annualPremiumCurrency,
    String paymentFrequency,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyInsurance.InsuranceStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Taxes
public record PropertyTaxResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyTax.TaxType taxType,
    Optional<String> authority,
    BigDecimal annualAmount,
    String currency,
    String paymentFrequency,
    Optional<String> dueMonths,
    Optional<Integer> taxYear,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyTax.TaxStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Fees
public record PropertyFeeResponse(
    String identifier,
    Optional<PropertySummary> property,
    PropertyFee.FeeType feeType,
    Optional<String> name,
    BigDecimal annualAmount,
    String currency,
    String paymentFrequency,
    Optional<String> dueMonths,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyFee.FeeStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}

// Aggregated financial summary for a property
public record PropertyFinancialSummaryResponse(
    PropertyAcquisitionResponse acquisition,
    PropertyValuationResponse latestValuation,
    List<PropertyValuationResponse> valuationHistory,
    List<PropertyFinancingResponse> financings,
    List<PropertyInsuranceResponse> insurances,
    List<PropertyTaxResponse> taxes,
    List<PropertyFeeResponse> fees,
    // Computed metrics
    Optional<BigDecimal> totalFinancingBalance,
    Optional<BigDecimal> totalAnnualInsurance,
    Optional<BigDecimal> totalAnnualTaxes,
    Optional<BigDecimal> totalAnnualFees,
    Optional<BigDecimal> totalAnnualCosts,  // taxes + insurance + fees
    Optional<BigDecimal> netWorth,  // latest valuation - total financing balance
    Optional<String> currency) {}
```

### 4.7 MapStruct Mappers

Follow the dual-mapper pattern: **RecordMapper** (JOOQ Record -> Domain) + **MapStruct Mapper** (Request -> Domain, Domain -> Response).

```java
// Example: PropertyFinancingMapper.java
@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyFinancingMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "paymentVariable",
      expression = "java(request.paymentVariable().orElse(false))")
  PropertyFinancing toEntity(CreatePropertyFinancingRequest request);

  @Mapping(target = "property", ignore = true)
  PropertyFinancingResponse toResponse(PropertyFinancing financing);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget PropertyFinancing financing,
      UpdatePropertyFinancingRequest request);
}
```

Same pattern for all 7 entities.

### 4.8 Controllers

Sub-resources of properties, following the PropertyDashboardController pattern:

```
/properties/{identifier}/financials                          -- GET: aggregated summary
/properties/{identifier}/financials/acquisition              -- GET/PUT: single acquisition
/properties/{identifier}/financials/valuations               -- GET (list)/POST
/properties/{identifier}/financials/valuations/{valId}       -- GET/PUT/DELETE
/properties/{identifier}/financials/financings               -- GET (list)/POST
/properties/{identifier}/financials/financings/{finId}       -- GET/PUT/DELETE
/properties/{identifier}/financials/financings/{finId}/payments     -- GET (list)/POST
/properties/{identifier}/financials/financings/{finId}/payments/{payId}  -- GET/PUT/DELETE
/properties/{identifier}/financials/insurances               -- GET (list)/POST
/properties/{identifier}/financials/insurances/{insId}       -- GET/PUT/DELETE
/properties/{identifier}/financials/taxes                    -- GET (list)/POST
/properties/{identifier}/financials/taxes/{taxId}            -- GET/PUT/DELETE
/properties/{identifier}/financials/fees                     -- GET (list)/POST
/properties/{identifier}/financials/fees/{feeId}             -- GET/PUT/DELETE
```

**Controller classes (7 + 1 aggregator):**

| Controller | Path prefix | Tag |
|-----------|-------------|-----|
| `PropertyFinancialsController` | `/properties/{identifier}/financials` | "Property Financials" |
| `PropertyAcquisitionController` | `/properties/{identifier}/financials/acquisition` | "Property Acquisitions" |
| `PropertyValuationController` | `/properties/{identifier}/financials/valuations` | "Property Valuations" |
| `PropertyFinancingController` | `/properties/{identifier}/financials/financings` | "Property Financings" |
| `FinancingPaymentController` | `/properties/{identifier}/financials/financings/{financingIdentifier}/payments` | "Financing Payments" |
| `PropertyInsuranceController` | `/properties/{identifier}/financials/insurances` | "Property Insurances" |
| `PropertyTaxController` | `/properties/{identifier}/financials/taxes` | "Property Taxes" |
| `PropertyFeeController` | `/properties/{identifier}/financials/fees` | "Property Fees" |

### 4.9 Expense.ExpenseCategory Changes

Remove `MORTGAGE_PAYMENT` from the enum. The migration soft-deletes existing mortgage expenses, so no runtime impact.

```java
public enum ExpenseCategory {
  MAINTENANCE,
  REPAIR,
  UTILITY,
  TAX,          // Keep for general tax expenses not tied to property taxes
  INSURANCE,    // Keep for general insurance expenses not tied to property insurances
  LEGAL,
  MARKETING,
  CLEANING,
  LANDSCAPING,
  PROPERTY_MANAGEMENT,
  FEES,
  PROPERTY_TAX, // Keep for backward compat with existing data
  // MORTGAGE_PAYMENT -- REMOVED
  OTHER
}
```

### 4.10 PropertyDashboardService Updates

The `PropertyDashboardService` must be refactored to read from the new tables instead of the monolithic `properties` columns:

1. **SummaryMetrics**: Replace `property.getMortgageAmount()` etc. with queries to `PropertyFinancingRepository.findActiveByPropertyIdAndTeamId()` and `PropertyTaxRepository.sumAnnualAmountsByPropertyIdAndTeamId()` etc.

2. **CashFlowChart**: Replace `property.getMonthlyMortgagePayment()` with sum of active financing monthly payments. Replace property-level operating costs with aggregated taxes + insurance + fees from new tables.

3. **EquityChart**: Use `PropertyValuationRepository.findLatestByPropertyIdAndTeamId()` for market value and `PropertyFinancingRepository.sumActiveBalanceByPropertyIdAndTeamId()` for mortgage balance.

4. **ExpenseBreakdown**: Remove MORTGAGE_PAYMENT special-casing. Operating costs come from property_taxes, property_insurances, property_fees.

5. **FutureTrend**: Read expected costs from active taxes, insurances, fees, and financing payments instead of property columns.

6. **DataCompleteness**: Check `PropertyAcquisitionRepository`, `PropertyValuationRepository`, `PropertyFinancingRepository` etc. instead of property fields.

### 4.11 Financial Calculations Service

Create `PropertyFinancialsService` as the aggregation layer:

```java
@Service
@RequiredArgsConstructor
public class PropertyFinancialsService {

  private final PropertyAcquisitionRepository acquisitionRepo;
  private final PropertyValuationRepository valuationRepo;
  private final PropertyFinancingRepository financingRepo;
  private final FinancingPaymentRepository financingPaymentRepo;
  private final PropertyInsuranceRepository insuranceRepo;
  private final PropertyTaxRepository taxRepo;
  private final PropertyFeeRepository feeRepo;
  private final PropertyRepository propertyRepo;
  // + mappers

  @Transactional(readOnly = true)
  public PropertyFinancialSummaryResponse getFinancialSummary(
      String propertyIdentifier, UserPrincipal principal) {
    // 1. Resolve property
    // 2. Fetch all financial sub-entities
    // 3. Compute aggregates (totalFinancingBalance, totalAnnualCosts, netWorth)
    // 4. Build and return response
  }

  /** Used by PropertyDashboardService for financial calculations */
  public BigDecimal getTotalActiveFinancingBalance(UUID propertyId, UUID teamId) { ... }
  public BigDecimal getTotalAnnualInsurance(UUID propertyId, UUID teamId) { ... }
  public BigDecimal getTotalAnnualTaxes(UUID propertyId, UUID teamId) { ... }
  public BigDecimal getTotalAnnualFees(UUID propertyId, UUID teamId) { ... }
  public BigDecimal getTotalMonthlyFinancingPayment(UUID propertyId, UUID teamId) { ... }
  public Optional<BigDecimal> getLatestValuation(UUID propertyId, UUID teamId) { ... }
}
```

---

## 5. API Design

### Full REST Endpoint Listing

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| **Aggregated** | | | |
| GET | `/properties/{id}/financials` | Full financial summary | Admin/Editor/Viewer |
| **Acquisitions** | | | |
| GET | `/properties/{id}/financials/acquisition` | Get acquisition details | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/acquisition` | Create or update acquisition | Admin/Editor |
| **Valuations** | | | |
| GET | `/properties/{id}/financials/valuations` | List valuations | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/valuations` | Add valuation | Admin/Editor |
| GET | `/properties/{id}/financials/valuations/{valId}` | Get valuation | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/valuations/{valId}` | Update valuation | Admin/Editor |
| DELETE | `/properties/{id}/financials/valuations/{valId}` | Delete valuation | Admin |
| **Financings** | | | |
| GET | `/properties/{id}/financials/financings` | List financings | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/financings` | Add financing | Admin/Editor |
| GET | `/properties/{id}/financials/financings/{finId}` | Get financing | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/financings/{finId}` | Update financing | Admin/Editor |
| DELETE | `/properties/{id}/financials/financings/{finId}` | Delete financing | Admin |
| **Financing Payments** | | | |
| GET | `/properties/{id}/financials/financings/{finId}/payments` | List payments (paginated) | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/financings/{finId}/payments` | Add payment | Admin/Editor |
| GET | `/properties/{id}/financials/financings/{finId}/payments/{payId}` | Get payment | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/financings/{finId}/payments/{payId}` | Update payment | Admin/Editor |
| DELETE | `/properties/{id}/financials/financings/{finId}/payments/{payId}` | Delete payment | Admin |
| **Insurances** | | | |
| GET | `/properties/{id}/financials/insurances` | List insurances | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/insurances` | Add insurance | Admin/Editor |
| GET | `/properties/{id}/financials/insurances/{insId}` | Get insurance | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/insurances/{insId}` | Update insurance | Admin/Editor |
| DELETE | `/properties/{id}/financials/insurances/{insId}` | Delete insurance | Admin |
| **Taxes** | | | |
| GET | `/properties/{id}/financials/taxes` | List taxes | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/taxes` | Add tax | Admin/Editor |
| GET | `/properties/{id}/financials/taxes/{taxId}` | Get tax | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/taxes/{taxId}` | Update tax | Admin/Editor |
| DELETE | `/properties/{id}/financials/taxes/{taxId}` | Delete tax | Admin |
| **Fees** | | | |
| GET | `/properties/{id}/financials/fees` | List fees | Admin/Editor/Viewer |
| POST | `/properties/{id}/financials/fees` | Add fee | Admin/Editor |
| GET | `/properties/{id}/financials/fees/{feeId}` | Get fee | Admin/Editor/Viewer |
| PUT | `/properties/{id}/financials/fees/{feeId}` | Update fee | Admin/Editor |
| DELETE | `/properties/{id}/financials/fees/{feeId}` | Delete fee | Admin |

**Total: 31 endpoints**

---

## 6. Frontend Architecture

### 6.1 New API Modules

```
app/src/api/
  propertyFinancials.ts     -- Aggregated summary endpoint
  propertyAcquisitions.ts   -- Acquisition CRUD
  propertyValuations.ts     -- Valuations CRUD
  propertyFinancings.ts     -- Financings CRUD
  financingPayments.ts      -- Financing payments CRUD
  propertyInsurances.ts     -- Insurances CRUD
  propertyTaxes.ts          -- Taxes CRUD
  propertyFees.ts           -- Fees CRUD
```

Each follows the pattern in `expenses.ts`:
```typescript
// Example: propertyFinancings.ts
export const getPropertyFinancings = async (propertyId: string): Promise<PropertyFinancingResponse[]> => {
  const response = await client.get(`/properties/${propertyId}/financials/financings`);
  return response.data;
};

export const createPropertyFinancing = async (
  propertyId: string, data: CreatePropertyFinancingRequest
): Promise<PropertyFinancingResponse> => {
  const response = await client.post(`/properties/${propertyId}/financials/financings`, data);
  return response.data;
};
// ... update, delete, getById
```

### 6.2 React Query Hooks

```
app/src/hooks/
  usePropertyFinancialHooks.ts    -- Aggregated summary hook
  useAcquisitionHooks.ts          -- Acquisition hooks
  useValuationHooks.ts            -- Valuation hooks
  useFinancingHooks.ts            -- Financing hooks
  useFinancingPaymentHooks.ts     -- Financing payment hooks
  useInsuranceHooks.ts            -- Insurance hooks
  usePropertyTaxHooks.ts          -- Tax hooks
  usePropertyFeeHooks.ts          -- Fee hooks
```

Each follows the pattern in `useExpenseHooks.ts` with proper cache invalidation:
```typescript
// Query keys for cache invalidation
['propertyFinancials', propertyId]     // aggregated summary
['propertyAcquisition', propertyId]    // acquisition
['propertyValuations', propertyId]     // valuation list
['propertyFinancings', propertyId]     // financing list
['financingPayments', financingId]     // payment list
['propertyInsurances', propertyId]     // insurance list
['propertyTaxes', propertyId]          // tax list
['propertyFees', propertyId]           // fee list

// Mutations invalidate:
// - Their own query key
// - ['propertyFinancials', propertyId]  (aggregated summary)
// - ['propertyDashboard', propertyId]   (dashboard recalculates)
// - ['dashboard']                       (global dashboard)
```

### 6.3 TypeScript Types

```
app/src/types/
  propertyFinancials.ts    -- All financial type definitions
```

### 6.4 Component Structure

```
app/src/components/property/financials/
  PropertyFinancialsTab.tsx          -- Main tab component (replaces current financial fields)
  AcquisitionSection.tsx             -- Acquisition details form/display
  ValuationSection.tsx               -- Valuation history list + add form
  FinancingSection.tsx               -- Financing instruments list
  FinancingDetail.tsx                -- Single financing with payment history
  FinancingPaymentList.tsx           -- Payment table for a financing
  InsuranceSection.tsx               -- Insurance policies list
  TaxSection.tsx                     -- Tax obligations list
  FeeSection.tsx                     -- Recurring fees list
  FinancialSummaryCard.tsx           -- Summary metrics card (net worth, total costs)
  // Dialogs/Forms
  AddValuationDialog.tsx
  AddFinancingDialog.tsx
  AddFinancingPaymentDialog.tsx
  AddInsuranceDialog.tsx
  AddTaxDialog.tsx
  AddFeeDialog.tsx
```

### 6.5 Dashboard Widget Updates

Update the property dashboard page to use the new financial data:
- **Equity Chart**: Use latest valuation vs. financing balance
- **Cash Flow Chart**: Use financing payments instead of mortgage expenses
- **Expense Breakdown**: Remove MORTGAGE_PAYMENT category, add financing costs
- **Summary Metrics**: Read from new aggregated financial summary

---

## 7. Data Integrity & Edge Cases

### Foreign Key Relationships
- `property_acquisitions.property_id` -> `properties.id` (CASCADE DELETE)
- `property_valuations.property_id` -> `properties.id` (CASCADE DELETE)
- `property_financings.property_id` -> `properties.id` (CASCADE DELETE)
- `financing_payments.financing_id` -> `property_financings.id` (CASCADE DELETE)
- All tables have `team_id` -> `teams.id` (CASCADE DELETE)

### What Happens When a Property is Deleted?
- `properties` uses soft delete (`deleted_at`)
- CASCADE DELETE on foreign keys only triggers on hard delete
- For soft delete: the service layer should NOT cascade-delete financials
- Financials remain queryable even for soft-deleted properties
- Frontend hides financials tab for deleted properties

### Consistency Rules
- Financing `current_balance` is user-maintained (not auto-calculated from payments). This avoids complex consistency enforcement and gives users control.
- If a financing is set to `PAID_OFF`, the UI should suggest setting `current_balance = 0`
- Financing payment `total_amount` does not need to equal `principal + interest + escrow + extra` (allows incomplete breakdowns)

### Multi-Currency
- Each financial entity has its own currency field (following existing pattern)
- Aggregations only sum amounts with matching currencies
- The `PropertyFinancialsService` aggregation uses the property's most common currency for the summary

---

## 8. Performance Considerations

### Index Strategy
All indexes are defined in the DDL above with partial indexes (`WHERE deleted_at IS NULL`) following the existing pattern. Key composite indexes:
- `(team_id, property_id)` on every property sub-resource table
- `(financing_id, payment_date DESC)` for payment queries
- `(property_id, valuation_date DESC)` for latest valuation lookups

### Query Optimization
- `PropertyFinancialsService.getFinancialSummary()` runs 7 queries (one per table). For small landlords (target audience), this is fine. Each query uses the composite index.
- `PropertyDashboardService` will call `PropertyFinancialsService` methods that aggregate using SQL `SUM()` queries rather than loading all records into memory.
- Financing payment pagination uses `PaginationHelper` for LIMIT/OFFSET.

### Data Volume Projections
For a typical small landlord (5-20 properties):
- Acquisitions: 5-20 rows (1:1 with properties)
- Valuations: 50-200 rows (1-10 per property per year)
- Financings: 10-40 rows (1-2 per property)
- Financing payments: 600-2400 rows (12/year per financing, 2-5 years)
- Insurances: 10-40 rows
- Taxes: 10-40 rows
- Fees: 20-60 rows

Total: ~700-2800 rows across all tables. No materialized views needed.

### Future Optimization (if needed)
- Add materialized view for `property_financial_summary` if dashboard query latency becomes an issue
- Add batch query methods to reduce N+1 when listing multiple properties' financials

---

## 9. Implementation Plan

### Phase 1: Foundation (Backend)
1. Create V014 migration (new tables)
2. Create V015 migration (data migration)
3. Add entity prefixes and UlidGenerator methods
4. Create domain classes (7)
5. Create record mappers (7)
6. Create repositories (7)
7. Create MapStruct mappers (7)
8. Create request/response DTOs (14 create + 14 update + 7 response + 1 summary)
9. Create services (7 + 1 aggregator)
10. Create controllers (7 + 1 aggregator)
11. Remove MORTGAGE_PAYMENT from ExpenseCategory enum
12. Update PropertyDashboardService to read from new tables
13. Update DashboardService if it references property financial columns
14. Create V016 migration (deprecation comments)

### Phase 2: Frontend
1. Create TypeScript types
2. Create API modules (8)
3. Create React Query hooks (8)
4. Create PropertyFinancialsTab component and sub-components
5. Integrate tab into property detail page
6. Update property dashboard widgets
7. Remove MORTGAGE_PAYMENT from expense category dropdowns/filters
8. Update reports page if it references property financial columns

### Phase 3: Cleanup
1. Remove deprecated financial fields from Property domain class
2. Remove deprecated fields from PropertyRepository queries
3. Remove deprecated fields from property create/update DTOs
4. Drop deprecated columns (V017 migration, separate release)
