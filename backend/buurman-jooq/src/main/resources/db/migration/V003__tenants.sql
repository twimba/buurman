-- ---------------------------------------------------------------------------
-- tenants
-- ---------------------------------------------------------------------------
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    tax_number VARCHAR(50),
    id_number VARCHAR(50),
    additional_info TEXT,
    current_property_id UUID REFERENCES properties (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    updated_by UUID REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_tenants_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_tenants_phone_e164 CHECK (
        phone IS NULL
        OR phone ~ '^\+[1-9]\d{1,14}$'
    )
);

CREATE INDEX idx_tenants_team ON tenants (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_tenants_property ON tenants (current_property_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_tenants_email ON tenants (team_id, email)
WHERE
    deleted_at IS NULL;

CREATE UNIQUE INDEX idx_tenants_team_email_unique ON tenants (team_id, email)
WHERE
    email IS NOT NULL
    AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- tenant_addresses
-- ---------------------------------------------------------------------------
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
    geocode_accuracy VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP
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

-- ---------------------------------------------------------------------------
-- property_tenant_history
-- ---------------------------------------------------------------------------
CREATE TABLE property_tenant_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    tenant_id UUID NOT NULL REFERENCES tenants (id),
    moved_in_at TIMESTAMP,
    moved_out_at TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL REFERENCES users (id),
    performed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenant_history_tenant ON property_tenant_history (tenant_id);

CREATE INDEX idx_tenant_history_property ON property_tenant_history (property_id);

CREATE INDEX idx_tenant_history_team ON property_tenant_history (team_id);
