-- Contract Rent Periods: tracks rent amount changes over time per contract
CREATE TABLE contract_rent_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    rent_amount DECIMAL(10, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_rent_period_amount_positive CHECK (rent_amount > 0),
    CONSTRAINT uq_contract_rent_period_effective_from UNIQUE (contract_id, effective_from)
);

CREATE INDEX idx_contract_rent_periods_team_id ON contract_rent_periods (team_id);

CREATE INDEX idx_contract_rent_periods_contract_id ON contract_rent_periods (contract_id);

CREATE INDEX idx_contract_rent_periods_effective_from ON contract_rent_periods (effective_from);

-- Migrate existing contracts: create one rent period per contract
INSERT INTO
    contract_rent_periods (
        identifier,
        team_id,
        contract_id,
        rent_amount,
        effective_from,
        effective_to,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'CRP' || right(replace(gen_random_uuid()::TEXT, '-', ''), 26),
    c.team_id,
    c.id,
    c.rent_amount,
    c.start_date,
    NULL,
    c.created_at,
    c.updated_at,
    c.created_by,
    c.updated_by
FROM
    contracts c
WHERE
    c.deleted_at IS NULL
    AND c.rent_amount IS NOT NULL;
