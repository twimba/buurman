-- Allow -1 as a sentinel value for variable mortgage payments.
-- When monthly_mortgage_payment = -1, it indicates the payment amount varies
-- and should not be used for fixed projections.
ALTER TABLE properties
DROP CONSTRAINT chk_monthly_mortgage_payment_positive;

ALTER TABLE properties
ADD CONSTRAINT chk_monthly_mortgage_payment_valid CHECK (
    monthly_mortgage_payment IS NULL
    OR monthly_mortgage_payment > 0
    OR monthly_mortgage_payment = -1
);
