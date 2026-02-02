-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    tax_number VARCHAR(50),
    id_number VARCHAR(50),
    current_property_id UUID REFERENCES properties(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP,
    UNIQUE(team_id, identifier)
);

-- Property tenant history table
CREATE TABLE property_tenant_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    moved_in_at TIMESTAMP,
    moved_out_at TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL REFERENCES users(id),
    performed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for tenants
CREATE INDEX idx_tenants_team ON tenants(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_property ON tenants(current_property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_email ON tenants(team_id, email) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_name ON tenants(team_id, name) WHERE deleted_at IS NULL;

-- Partial unique index for email uniqueness (only for active tenants)
CREATE UNIQUE INDEX idx_tenants_team_email_unique ON tenants(team_id, email) WHERE deleted_at IS NULL;

-- Indexes for property_tenant_history
CREATE INDEX idx_tenant_history_tenant ON property_tenant_history(tenant_id);
CREATE INDEX idx_tenant_history_property ON property_tenant_history(property_id);
CREATE INDEX idx_tenant_history_team ON property_tenant_history(team_id);

-- Comments for documentation
COMMENT ON TABLE tenants IS 'Stores tenant information for property management';
COMMENT ON TABLE property_tenant_history IS 'Tracks the history of tenant-property assignments';
COMMENT ON COLUMN tenants.current_property_id IS 'The property currently occupied by the tenant (null if not assigned)';
COMMENT ON COLUMN property_tenant_history.action_type IS 'Type of action: LINKED or UNLINKED';
