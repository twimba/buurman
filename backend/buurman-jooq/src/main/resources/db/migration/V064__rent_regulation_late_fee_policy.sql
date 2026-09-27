-- BUUR-101: late-fee policy per country in the rent regulation catalogue.
-- Advisory reference data: whether a flat late fee may be charged on residential rent, and the
-- statutory maximum where one exists. Enforced when charging late fees and validated on contracts.
ALTER TABLE rent_regulation_countries
ADD COLUMN late_fee_policy VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN late_fee_max_percentage DECIMAL(5, 2),
ADD COLUMN late_fee_notes TEXT,
ADD CONSTRAINT chk_rent_regulation_late_fee_policy CHECK (
    late_fee_policy IN (
        'UNKNOWN',
        'ALLOWED',
        'CAPPED',
        'INTEREST_ONLY',
        'FORBIDDEN'
    )
);
