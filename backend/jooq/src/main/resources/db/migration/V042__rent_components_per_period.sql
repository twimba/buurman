-- Move rent components from contract-level to rent-period-level.
-- Each rent period now owns its own set of components.
-- 1. Add rent_period_id column (nullable initially for backfill)
ALTER TABLE contract_rent_components
ADD COLUMN rent_period_id UUID;

-- 2. Backfill: link each contract's active components to its current/latest rent period
UPDATE contract_rent_components crc
SET
    rent_period_id = (
        SELECT
            crp.id
        FROM
            contract_rent_periods crp
        WHERE
            crp.contract_id = crc.contract_id
            AND crp.team_id = crc.team_id
            AND crp.deleted_at IS NULL
        ORDER BY
            crp.effective_from DESC
        LIMIT
            1
    )
WHERE
    crc.deleted_at IS NULL;

-- 3. For soft-deleted components that couldn't be linked, link to latest period (including deleted)
UPDATE contract_rent_components crc
SET
    rent_period_id = (
        SELECT
            crp.id
        FROM
            contract_rent_periods crp
        WHERE
            crp.contract_id = crc.contract_id
            AND crp.team_id = crc.team_id
        ORDER BY
            crp.effective_from DESC
        LIMIT
            1
    )
WHERE
    crc.rent_period_id IS NULL;

-- 4. Make rent_period_id NOT NULL
ALTER TABLE contract_rent_components
ALTER COLUMN rent_period_id
SET NOT NULL;

-- 5. Add FK constraint
ALTER TABLE contract_rent_components
ADD CONSTRAINT fk_rent_comp_rent_period FOREIGN KEY (rent_period_id) REFERENCES contract_rent_periods (id);

-- 6. Drop old unique index (per contract) and create new one (per period)
DROP INDEX IF EXISTS uq_rent_comp_typed;

CREATE UNIQUE INDEX uq_rent_comp_typed ON contract_rent_components (rent_period_id, component_type)
WHERE
    component_type != 'OTHER'
    AND deleted_at IS NULL;

-- 7. Index on rent_period_id for lookups
CREATE INDEX idx_rent_comp_rent_period_id ON contract_rent_components (rent_period_id);
