-- Add CHECK constraints for operating cost and mortgage payment fields
ALTER TABLE properties ADD CONSTRAINT chk_annual_property_tax_non_negative
    CHECK (annual_property_tax IS NULL OR annual_property_tax >= 0);

ALTER TABLE properties ADD CONSTRAINT chk_annual_insurance_non_negative
    CHECK (annual_insurance IS NULL OR annual_insurance >= 0);

ALTER TABLE properties ADD CONSTRAINT chk_annual_hoa_fee_non_negative
    CHECK (annual_hoa_fee IS NULL OR annual_hoa_fee >= 0);

ALTER TABLE properties ADD CONSTRAINT chk_annual_management_fee_non_negative
    CHECK (annual_management_fee IS NULL OR annual_management_fee >= 0);

ALTER TABLE properties ADD CONSTRAINT chk_annual_maintenance_reserve_non_negative
    CHECK (annual_maintenance_reserve IS NULL OR annual_maintenance_reserve >= 0);

ALTER TABLE properties ADD CONSTRAINT chk_monthly_mortgage_payment_positive
    CHECK (monthly_mortgage_payment IS NULL OR monthly_mortgage_payment > 0);

-- Date ordering constraints
ALTER TABLE properties ADD CONSTRAINT chk_mortgage_dates_valid
    CHECK (mortgage_start_date IS NULL OR mortgage_end_date IS NULL
           OR mortgage_end_date > mortgage_start_date);
