-- Investment & Financial fields for Property Dashboard (BUUR-34)

-- Acquisition & Valuation
ALTER TABLE properties ADD COLUMN currency VARCHAR(3);
ALTER TABLE properties ADD COLUMN purchase_price BIGINT;
ALTER TABLE properties ADD COLUMN purchase_date DATE;
ALTER TABLE properties ADD COLUMN current_market_value BIGINT;
ALTER TABLE properties ADD COLUMN market_value_date DATE;

-- Mortgage / Financing
ALTER TABLE properties ADD COLUMN mortgage_type VARCHAR(30);
ALTER TABLE properties ADD COLUMN mortgage_amount BIGINT;
ALTER TABLE properties ADD COLUMN mortgage_interest_rate DECIMAL(5,3);
ALTER TABLE properties ADD COLUMN mortgage_start_date DATE;
ALTER TABLE properties ADD COLUMN mortgage_end_date DATE;
ALTER TABLE properties ADD COLUMN monthly_mortgage_payment BIGINT;

-- Operating Costs (annual, stored in minor units)
ALTER TABLE properties ADD COLUMN annual_property_tax BIGINT;
ALTER TABLE properties ADD COLUMN annual_insurance BIGINT;
ALTER TABLE properties ADD COLUMN annual_hoa_fee BIGINT;
ALTER TABLE properties ADD COLUMN annual_management_fee BIGINT;
ALTER TABLE properties ADD COLUMN annual_maintenance_reserve BIGINT;

-- Depreciation
ALTER TABLE properties ADD COLUMN depreciation_method VARCHAR(30);
ALTER TABLE properties ADD COLUMN depreciation_years INTEGER;
ALTER TABLE properties ADD COLUMN land_value BIGINT;

-- Constraints
ALTER TABLE properties ADD CONSTRAINT chk_purchase_price_positive
    CHECK (purchase_price IS NULL OR purchase_price > 0);
ALTER TABLE properties ADD CONSTRAINT chk_current_market_value_positive
    CHECK (current_market_value IS NULL OR current_market_value > 0);
ALTER TABLE properties ADD CONSTRAINT chk_mortgage_amount_positive
    CHECK (mortgage_amount IS NULL OR mortgage_amount > 0);
ALTER TABLE properties ADD CONSTRAINT chk_mortgage_interest_rate_range
    CHECK (mortgage_interest_rate IS NULL OR (mortgage_interest_rate >= 0 AND mortgage_interest_rate <= 100));
ALTER TABLE properties ADD CONSTRAINT chk_depreciation_years_positive
    CHECK (depreciation_years IS NULL OR depreciation_years > 0);
ALTER TABLE properties ADD CONSTRAINT chk_land_value_non_negative
    CHECK (land_value IS NULL OR land_value >= 0);
ALTER TABLE properties ADD CONSTRAINT chk_mortgage_type_valid
    CHECK (mortgage_type IS NULL OR mortgage_type IN ('FIXED_RATE', 'VARIABLE_RATE', 'INTEREST_ONLY', 'NONE'));
ALTER TABLE properties ADD CONSTRAINT chk_depreciation_method_valid
    CHECK (depreciation_method IS NULL OR depreciation_method IN ('STRAIGHT_LINE', 'DECLINING_BALANCE', 'NONE'));
