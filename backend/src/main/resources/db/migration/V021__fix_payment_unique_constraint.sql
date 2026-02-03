-- Fix unique constraint to exclude soft-deleted payments
-- This allows a contract to have new payments generated after previous ones were soft-deleted

-- Drop the existing constraint
ALTER TABLE payments DROP CONSTRAINT unique_contract_due_date;

-- Create a partial unique index that only applies to non-deleted payments
CREATE UNIQUE INDEX unique_contract_due_date_active
ON payments(contract_id, due_date)
WHERE deleted_at IS NULL;
