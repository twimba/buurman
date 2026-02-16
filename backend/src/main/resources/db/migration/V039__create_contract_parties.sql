-- Contract parties: supports multiple people per contract with different roles
CREATE TABLE contract_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_contract_parties_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contract_parties_role CHECK (
        role IN (
            'PRIMARY_TENANT',
            'GUARANTOR',
            'COSIGNER',
            'EXTRA_TENANT'
        )
    )
);

-- Partial unique: same tenant cannot appear twice on the same active contract
CREATE UNIQUE INDEX uq_contract_parties_contract_tenant ON contract_parties (contract_id, tenant_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contract_parties_team_id ON contract_parties (team_id);

CREATE INDEX idx_contract_parties_contract_id ON contract_parties (contract_id);

CREATE INDEX idx_contract_parties_tenant_id ON contract_parties (tenant_id);

CREATE INDEX idx_contract_parties_role ON contract_parties (role);

-- Migrate existing data: each contract's tenant_id becomes a PRIMARY_TENANT party
INSERT INTO
    contract_parties (
        identifier,
        team_id,
        contract_id,
        tenant_id,
        role,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    'CTP' || right(replace(gen_random_uuid()::TEXT, '-', ''), 26),
    c.team_id,
    c.id,
    c.tenant_id,
    'PRIMARY_TENANT',
    c.created_at,
    c.updated_at,
    c.created_by,
    c.updated_by
FROM
    contracts c
WHERE
    c.tenant_id IS NOT NULL;

-- Drop the tenant_id column from contracts (no longer needed)
ALTER TABLE contracts
DROP COLUMN tenant_id;

-- Drop existing simple triggers
DELETE FROM qrtz_simple_triggers
WHERE
    trigger_name IN (
        'databaseMetricsRefreshTrigger',
        'notificationOutboxTrigger',
        'thumbnailBackfillTrigger'
    );

DELETE FROM qrtz_triggers
WHERE
    trigger_name IN (
        'databaseMetricsRefreshTrigger',
        'notificationOutboxTrigger',
        'thumbnailBackfillTrigger'
    );

DELETE FROM qrtz_job_details
WHERE
    job_name IN ('thumbnailBackfillJob');
