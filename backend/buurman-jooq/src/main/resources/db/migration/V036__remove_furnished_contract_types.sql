-- Convert FURNISHED/UNFURNISHED contracts based on whether they have an end date:
--   with end_date    → FIXED_TERM (bounded duration)
--   without end_date → INDEFINITE (open-ended)
UPDATE contracts
SET
    contract_type = CASE
        WHEN end_date IS NOT NULL THEN 'FIXED_TERM'
        ELSE 'INDEFINITE'
    END
WHERE
    contract_type IN ('FURNISHED', 'UNFURNISHED');

-- Add CHECK constraint to prevent future invalid values
ALTER TABLE contracts
ADD CONSTRAINT chk_contracts_contract_type CHECK (contract_type IN ('FIXED_TERM', 'INDEFINITE'));
