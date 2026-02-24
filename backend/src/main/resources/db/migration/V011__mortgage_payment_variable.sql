-- Replace -1 sentinel value for variable mortgage payments with an explicit boolean column
ALTER TABLE properties
ADD COLUMN mortgage_payment_variable BOOLEAN NOT NULL DEFAULT FALSE;

-- Migrate existing sentinel values: -1 means variable payment
UPDATE properties
SET
    mortgage_payment_variable = TRUE,
    monthly_mortgage_payment = NULL
WHERE
    monthly_mortgage_payment = -1;

-- Drop old constraint that allowed -1
ALTER TABLE properties
DROP CONSTRAINT IF EXISTS chk_monthly_mortgage_payment_valid;

-- Add clean constraint: only NULL or positive values
ALTER TABLE properties
ADD CONSTRAINT chk_monthly_mortgage_payment_valid CHECK (
    monthly_mortgage_payment IS NULL
    OR monthly_mortgage_payment > 0
);
