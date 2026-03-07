-- Add country_metadata JSONB column to contracts
ALTER TABLE contracts
ADD COLUMN country_metadata JSONB;

-- Add country_code to contracts (denormalized from property for query efficiency)
ALTER TABLE contracts
ADD COLUMN country_code VARCHAR(2);

-- Index for filtering contracts by country
CREATE INDEX idx_contracts_country_code ON contracts (team_id, country_code)
WHERE
    deleted_at IS NULL;
