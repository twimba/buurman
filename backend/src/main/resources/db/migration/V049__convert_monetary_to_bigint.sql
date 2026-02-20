-- Convert monetary DECIMAL(10,2) columns to BIGINT (minor units).
-- All existing data uses 2-decimal currencies (EUR default).
-- Multiply by 100 to convert to minor units before type change.
-- contracts: rent_amount (NOT NULL), deposit_amount (nullable), security_deposit (nullable)
ALTER TABLE contracts
ALTER COLUMN rent_amount TYPE BIGINT USING (rent_amount * 100)::BIGINT;

ALTER TABLE contracts
ALTER COLUMN deposit_amount TYPE BIGINT USING (
    CASE
        WHEN deposit_amount IS NOT NULL THEN (deposit_amount * 100)::BIGINT
    END
);

ALTER TABLE contracts
ALTER COLUMN security_deposit TYPE BIGINT USING (
    CASE
        WHEN security_deposit IS NOT NULL THEN (security_deposit * 100)::BIGINT
    END
);

-- payments: amount (NOT NULL)
ALTER TABLE payments
ALTER COLUMN amount TYPE BIGINT USING (amount * 100)::BIGINT;

-- expenses: amount (NOT NULL)
ALTER TABLE expenses
ALTER COLUMN amount TYPE BIGINT USING (amount * 100)::BIGINT;

-- payment_receivals: amount (NOT NULL)
ALTER TABLE payment_receivals
ALTER COLUMN amount TYPE BIGINT USING (amount * 100)::BIGINT;

-- contract_rent_periods: rent_amount (NOT NULL)
ALTER TABLE contract_rent_periods
ALTER COLUMN rent_amount TYPE BIGINT USING (rent_amount * 100)::BIGINT;
