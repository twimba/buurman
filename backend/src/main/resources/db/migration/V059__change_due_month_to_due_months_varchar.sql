-- Change operating cost due month columns from SMALLINT to VARCHAR
-- to support multi-month selection (e.g. '1,3,7' for Jan, Mar, Jul).
-- NULL means all months (divided by 12).

-- Drop existing CHECK constraints
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_property_tax_due_month;
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_insurance_due_month;
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_hoa_fee_due_month;
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_management_fee_due_month;
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_maintenance_reserve_due_month;

-- Convert columns: cast existing SMALLINT to VARCHAR (e.g. 3 -> '3')
ALTER TABLE properties ALTER COLUMN annual_property_tax_due_month TYPE VARCHAR(50) USING annual_property_tax_due_month::TEXT;
ALTER TABLE properties ALTER COLUMN annual_insurance_due_month TYPE VARCHAR(50) USING annual_insurance_due_month::TEXT;
ALTER TABLE properties ALTER COLUMN annual_hoa_fee_due_month TYPE VARCHAR(50) USING annual_hoa_fee_due_month::TEXT;
ALTER TABLE properties ALTER COLUMN annual_management_fee_due_month TYPE VARCHAR(50) USING annual_management_fee_due_month::TEXT;
ALTER TABLE properties ALTER COLUMN annual_maintenance_reserve_due_month TYPE VARCHAR(50) USING annual_maintenance_reserve_due_month::TEXT;
