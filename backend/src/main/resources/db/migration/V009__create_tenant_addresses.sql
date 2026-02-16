CREATE TABLE tenant_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id),
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'Netherlands',
    address_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT chk_tenant_addresses_type CHECK (
        address_type IN (
            'CURRENT',
            'MAILING',
            'RELATIVE',
            'WORK',
            'HISTORIC'
        )
    ),
    CONSTRAINT chk_tenant_addresses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX idx_tenant_addresses_team_identifier ON tenant_addresses (team_id, identifier);

CREATE INDEX idx_tenant_addresses_tenant_id ON tenant_addresses (tenant_id);

CREATE INDEX idx_tenant_addresses_team_id ON tenant_addresses (team_id);

CREATE INDEX idx_tenant_addresses_status ON tenant_addresses (status);

CREATE INDEX idx_tenant_addresses_type ON tenant_addresses (address_type);

CREATE INDEX idx_tenant_addresses_deleted_at ON tenant_addresses (deleted_at);

CREATE INDEX idx_tenant_addresses_coordinates ON tenant_addresses (latitude, longitude)
WHERE
    latitude IS NOT NULL
    AND longitude IS NOT NULL;

CREATE UNIQUE INDEX idx_tenant_addresses_unique_current_active ON tenant_addresses (tenant_id)
WHERE
    address_type = 'CURRENT'
    AND status = 'ACTIVE'
    AND deleted_at IS NULL;
