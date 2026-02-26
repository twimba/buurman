-- =============================================================================
-- V014__deprecate_property_financial_columns.sql
-- Mark old financial columns as deprecated.
-- Actual column removal deferred to a later release after all code paths updated.
-- =============================================================================
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
