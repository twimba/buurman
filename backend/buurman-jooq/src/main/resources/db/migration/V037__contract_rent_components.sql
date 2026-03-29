CREATE TABLE contract_rent_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    component_type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_rent_comp_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_rent_comp_amount CHECK (amount > 0),
    CONSTRAINT chk_rent_comp_type CHECK (
        component_type IN (
            'BASE_RENT',
            'UTILITIES_ADVANCE',
            'SERVICE_COSTS',
            'HOA_FEES',
            'FURNITURE_RENTAL',
            'PARKING',
            'STORAGE',
            'GARBAGE_COLLECTION',
            'OTHER'
        )
    ),
    CONSTRAINT chk_rent_comp_other_desc CHECK (
        component_type != 'OTHER'
        OR description IS NOT NULL
    )
);

-- One of each non-OTHER type per active contract
CREATE UNIQUE INDEX uq_rent_comp_typed ON contract_rent_components (contract_id, component_type)
WHERE
    component_type != 'OTHER'
    AND deleted_at IS NULL;

CREATE INDEX idx_rent_comp_team ON contract_rent_components (team_id);

CREATE INDEX idx_rent_comp_contract ON contract_rent_components (contract_id);

CREATE INDEX idx_rent_comp_active ON contract_rent_components (team_id, contract_id)
WHERE
    deleted_at IS NULL;
