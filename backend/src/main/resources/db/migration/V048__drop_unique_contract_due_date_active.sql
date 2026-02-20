-- BUUR-42: Drop the unique constraint on (contract_id, due_date) for active payments.
-- Multiple payments per contract per day is a valid business scenario
-- (e.g., deposit + first month rent on the same day).
DROP INDEX IF EXISTS unique_contract_due_date_active;
