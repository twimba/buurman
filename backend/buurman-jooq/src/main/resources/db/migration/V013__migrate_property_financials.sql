-- =============================================================================
-- V013__migrate_property_financials.sql
-- Migrate existing property financial data to normalized tables
-- =============================================================================
-- 1. Migrate purchase data to property_acquisitions
INSERT INTO
    property_acquisitions (
        identifier,
        property_id,
        team_id,
        acquisition_type,
        acquisition_date,
        purchase_price,
        purchase_price_currency,
        land_value,
        land_value_currency,
        depreciation_method,
        depreciation_years,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'ACQ' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'PURCHASE',
    p.purchase_date,
    p.purchase_price,
    p.purchase_price_currency,
    p.land_value,
    p.land_value_currency,
    p.depreciation_method,
    p.depreciation_years,
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND (
        p.purchase_price IS NOT NULL
        OR p.purchase_date IS NOT NULL
    );

-- 2. Migrate current market value to property_valuations
INSERT INTO
    property_valuations (
        identifier,
        property_id,
        team_id,
        valuation_type,
        valuation_date,
        amount,
        currency,
        source,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'VAL' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'MARKET',
    coalesce(p.market_value_date, p.updated_at::DATE),
    p.current_market_value,
    coalesce(p.current_market_value_currency, 'EUR'),
    'Migrated from property record',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.current_market_value IS NOT NULL;

-- 3. Migrate mortgage data to property_financings
INSERT INTO
    property_financings (
        identifier,
        property_id,
        team_id,
        financing_type,
        rate_type,
        original_amount,
        original_amount_currency,
        current_balance,
        current_balance_currency,
        interest_rate,
        monthly_payment,
        monthly_payment_currency,
        payment_variable,
        start_date,
        end_date,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'FIN' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'MORTGAGE',
    CASE p.mortgage_type
        WHEN 'FIXED_RATE' THEN 'FIXED'
        WHEN 'VARIABLE_RATE' THEN 'VARIABLE'
        WHEN 'INTEREST_ONLY' THEN 'INTEREST_ONLY'
        ELSE 'FIXED'
    END,
    p.mortgage_amount,
    coalesce(p.mortgage_amount_currency, 'EUR'),
    p.mortgage_amount,
    coalesce(p.mortgage_amount_currency, 'EUR'),
    p.mortgage_interest_rate,
    p.monthly_mortgage_payment,
    p.monthly_mortgage_payment_currency,
    p.mortgage_payment_variable,
    coalesce(
        p.mortgage_start_date,
        p.purchase_date,
        p.created_at::DATE
    ),
    p.mortgage_end_date,
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.mortgage_amount IS NOT NULL
    AND p.mortgage_type IS NOT NULL
    AND p.mortgage_type != 'NONE';

-- 4. Migrate annual insurance to property_insurances
INSERT INTO
    property_insurances (
        identifier,
        property_id,
        team_id,
        insurance_type,
        annual_premium,
        annual_premium_currency,
        payment_frequency,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'INS' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'BUILDING',
    p.annual_insurance,
    coalesce(p.annual_insurance_currency, 'EUR'),
    'ANNUALLY',
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.annual_insurance IS NOT NULL
    AND p.annual_insurance > 0;

-- 5. Migrate annual property tax to property_taxes
INSERT INTO
    property_taxes (
        identifier,
        property_id,
        team_id,
        tax_type,
        annual_amount,
        currency,
        payment_frequency,
        due_months,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'TAX' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'PROPERTY',
    p.annual_property_tax,
    coalesce(p.annual_property_tax_currency, 'EUR'),
    'ANNUALLY',
    p.annual_property_tax_due_month,
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.annual_property_tax IS NOT NULL
    AND p.annual_property_tax > 0;

-- 6. Migrate HOA fees to property_fees
INSERT INTO
    property_fees (
        identifier,
        property_id,
        team_id,
        fee_type,
        annual_amount,
        currency,
        payment_frequency,
        due_months,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'FEE' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'HOA',
    p.annual_hoa_fee,
    coalesce(p.annual_hoa_fee_currency, 'EUR'),
    'ANNUALLY',
    p.annual_hoa_fee_due_month,
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.annual_hoa_fee IS NOT NULL
    AND p.annual_hoa_fee > 0;

-- 7. Migrate management fees
INSERT INTO
    property_fees (
        identifier,
        property_id,
        team_id,
        fee_type,
        annual_amount,
        currency,
        payment_frequency,
        due_months,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'FEE' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'MANAGEMENT',
    p.annual_management_fee,
    coalesce(p.annual_management_fee_currency, 'EUR'),
    'ANNUALLY',
    p.annual_management_fee_due_month,
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.annual_management_fee IS NOT NULL
    AND p.annual_management_fee > 0;

-- 8. Migrate maintenance reserve fees
INSERT INTO
    property_fees (
        identifier,
        property_id,
        team_id,
        fee_type,
        annual_amount,
        currency,
        payment_frequency,
        due_months,
        status,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'FEE' || upper(
        substr(replace(gen_random_uuid()::TEXT, '-', ''), 1, 26)
    ),
    p.id,
    p.team_id,
    'MAINTENANCE_RESERVE',
    p.annual_maintenance_reserve,
    coalesce(p.annual_maintenance_reserve_currency, 'EUR'),
    'ANNUALLY',
    p.annual_maintenance_reserve_due_month,
    'ACTIVE',
    p.created_at,
    p.updated_at,
    p.created_by,
    p.updated_by
FROM
    properties p
WHERE
    p.deleted_at IS NULL
    AND p.annual_maintenance_reserve IS NOT NULL
    AND p.annual_maintenance_reserve > 0;
