-- Per-field currency columns (BUUR-50)
-- Each monetary value gets its own currency column so different currencies
-- can be used within the same entity (e.g., rent in EUR, deposit in USD).
-- ============================================================================
-- CONTRACTS: 3 monetary fields → 3 currency columns
-- ============================================================================
ALTER TABLE contracts
ADD COLUMN rent_amount_currency VARCHAR(3);

ALTER TABLE contracts
ADD COLUMN deposit_amount_currency VARCHAR(3);

ALTER TABLE contracts
ADD COLUMN security_deposit_currency VARCHAR(3);

-- Backfill from existing shared currency column
UPDATE contracts
SET
    rent_amount_currency = currency,
    deposit_amount_currency = CASE
        WHEN deposit_amount IS NOT NULL THEN currency
    END,
    security_deposit_currency = CASE
        WHEN security_deposit IS NOT NULL THEN currency
    END;

-- rent_amount is always required, so its currency must be NOT NULL
ALTER TABLE contracts
ALTER COLUMN rent_amount_currency
SET NOT NULL;

ALTER TABLE contracts
ALTER COLUMN rent_amount_currency
SET DEFAULT 'EUR';

-- Rename old column (drop in V054 after full stack migration)
ALTER TABLE contracts
RENAME COLUMN currency TO currency_legacy;

-- ============================================================================
-- PROPERTIES: 10 monetary fields → 10 currency columns
-- ============================================================================
-- Purchase & Valuation
ALTER TABLE properties
ADD COLUMN purchase_price_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN current_market_value_currency VARCHAR(3);

-- Mortgage
ALTER TABLE properties
ADD COLUMN mortgage_amount_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN monthly_mortgage_payment_currency VARCHAR(3);

-- Operating Costs (annual)
ALTER TABLE properties
ADD COLUMN annual_property_tax_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN annual_insurance_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN annual_hoa_fee_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN annual_management_fee_currency VARCHAR(3);

ALTER TABLE properties
ADD COLUMN annual_maintenance_reserve_currency VARCHAR(3);

-- Depreciation
ALTER TABLE properties
ADD COLUMN land_value_currency VARCHAR(3);

-- Backfill from existing shared currency column (nullable, default to EUR)
UPDATE properties
SET
    purchase_price_currency = CASE
        WHEN purchase_price IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    current_market_value_currency = CASE
        WHEN current_market_value IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    mortgage_amount_currency = CASE
        WHEN mortgage_amount IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    monthly_mortgage_payment_currency = CASE
        WHEN monthly_mortgage_payment IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    annual_property_tax_currency = CASE
        WHEN annual_property_tax IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    annual_insurance_currency = CASE
        WHEN annual_insurance IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    annual_hoa_fee_currency = CASE
        WHEN annual_hoa_fee IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    annual_management_fee_currency = CASE
        WHEN annual_management_fee IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    annual_maintenance_reserve_currency = CASE
        WHEN annual_maintenance_reserve IS NOT NULL THEN coalesce(currency, 'EUR')
    END,
    land_value_currency = CASE
        WHEN land_value IS NOT NULL THEN coalesce(currency, 'EUR')
    END;

-- Rename old column (drop in V054 after full stack migration)
ALTER TABLE properties
RENAME COLUMN currency TO currency_legacy;
