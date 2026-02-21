-- Due month (1-12) for each annual operating cost on properties
ALTER TABLE properties
ADD COLUMN annual_property_tax_due_month SMALLINT;

ALTER TABLE properties
ADD COLUMN annual_insurance_due_month SMALLINT;

ALTER TABLE properties
ADD COLUMN annual_hoa_fee_due_month SMALLINT;

ALTER TABLE properties
ADD COLUMN annual_management_fee_due_month SMALLINT;

ALTER TABLE properties
ADD COLUMN annual_maintenance_reserve_due_month SMALLINT;

ALTER TABLE properties
ADD CONSTRAINT chk_property_tax_due_month CHECK (
    annual_property_tax_due_month IS NULL
    OR (
        annual_property_tax_due_month >= 1
        AND annual_property_tax_due_month <= 12
    )
);

ALTER TABLE properties
ADD CONSTRAINT chk_insurance_due_month CHECK (
    annual_insurance_due_month IS NULL
    OR (
        annual_insurance_due_month >= 1
        AND annual_insurance_due_month <= 12
    )
);

ALTER TABLE properties
ADD CONSTRAINT chk_hoa_fee_due_month CHECK (
    annual_hoa_fee_due_month IS NULL
    OR (
        annual_hoa_fee_due_month >= 1
        AND annual_hoa_fee_due_month <= 12
    )
);

ALTER TABLE properties
ADD CONSTRAINT chk_management_fee_due_month CHECK (
    annual_management_fee_due_month IS NULL
    OR (
        annual_management_fee_due_month >= 1
        AND annual_management_fee_due_month <= 12
    )
);

ALTER TABLE properties
ADD CONSTRAINT chk_maintenance_reserve_due_month CHECK (
    annual_maintenance_reserve_due_month IS NULL
    OR (
        annual_maintenance_reserve_due_month >= 1
        AND annual_maintenance_reserve_due_month <= 12
    )
);
