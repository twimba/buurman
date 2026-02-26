-- =============================================================================
-- V015__drop_deprecated_property_financial_columns.sql
-- Drop deprecated financial columns from properties table.
-- All financial data now lives in dedicated tables created in V012:
--   property_acquisitions, property_valuations, property_financings,
--   financing_payments, property_insurances, property_taxes, property_fees
-- Data was migrated in V013, columns marked deprecated in V014.
-- =============================================================================
-- Fix invalid tax_type values inserted by demo generator before bug fix
UPDATE property_taxes
SET
    tax_type = 'PROPERTY'
WHERE
    tax_type = 'PROPERTY_TAX';

ALTER TABLE properties
DROP COLUMN IF EXISTS purchase_price,
DROP COLUMN IF EXISTS purchase_price_currency,
DROP COLUMN IF EXISTS purchase_date,
DROP COLUMN IF EXISTS current_market_value,
DROP COLUMN IF EXISTS current_market_value_currency,
DROP COLUMN IF EXISTS market_value_date,
DROP COLUMN IF EXISTS mortgage_type,
DROP COLUMN IF EXISTS mortgage_amount,
DROP COLUMN IF EXISTS mortgage_amount_currency,
DROP COLUMN IF EXISTS mortgage_interest_rate,
DROP COLUMN IF EXISTS mortgage_start_date,
DROP COLUMN IF EXISTS mortgage_end_date,
DROP COLUMN IF EXISTS mortgage_payment_variable,
DROP COLUMN IF EXISTS monthly_mortgage_payment,
DROP COLUMN IF EXISTS monthly_mortgage_payment_currency,
DROP COLUMN IF EXISTS annual_property_tax,
DROP COLUMN IF EXISTS annual_property_tax_currency,
DROP COLUMN IF EXISTS annual_property_tax_due_month,
DROP COLUMN IF EXISTS annual_insurance,
DROP COLUMN IF EXISTS annual_insurance_currency,
DROP COLUMN IF EXISTS annual_insurance_due_month,
DROP COLUMN IF EXISTS annual_hoa_fee,
DROP COLUMN IF EXISTS annual_hoa_fee_currency,
DROP COLUMN IF EXISTS annual_hoa_fee_due_month,
DROP COLUMN IF EXISTS annual_management_fee,
DROP COLUMN IF EXISTS annual_management_fee_currency,
DROP COLUMN IF EXISTS annual_management_fee_due_month,
DROP COLUMN IF EXISTS annual_maintenance_reserve,
DROP COLUMN IF EXISTS annual_maintenance_reserve_currency,
DROP COLUMN IF EXISTS annual_maintenance_reserve_due_month,
DROP COLUMN IF EXISTS depreciation_method,
DROP COLUMN IF EXISTS depreciation_years,
DROP COLUMN IF EXISTS land_value,
DROP COLUMN IF EXISTS land_value_currency;
