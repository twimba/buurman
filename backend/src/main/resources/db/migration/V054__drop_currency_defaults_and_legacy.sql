-- Remove the DEFAULT 'EUR' on rent_amount_currency.
-- The application must always provide the currency explicitly;
-- there should be no silent database-level default.
ALTER TABLE contracts
ALTER COLUMN rent_amount_currency
DROP DEFAULT;

-- Drop the legacy shared currency columns (renamed in V053)
ALTER TABLE contracts
DROP COLUMN currency_legacy;

ALTER TABLE properties
DROP COLUMN currency_legacy;
