CREATE TABLE tenants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier          VARCHAR(29) NOT NULL,
    team_id             UUID NOT NULL REFERENCES teams(id),
    first_name          VARCHAR(255) NOT NULL,
    last_name           VARCHAR(255),
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(50),
    tax_number          VARCHAR(50),
    id_number           VARCHAR(50),
    additional_info     TEXT,
    current_property_id UUID REFERENCES properties(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          UUID REFERENCES users(id),
    updated_by          UUID REFERENCES users(id),
    deleted_at          TIMESTAMP,

    CONSTRAINT uq_tenants_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_tenants_team ON tenants(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_property ON tenants(current_property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_email ON tenants(team_id, email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_tenants_team_email_unique ON tenants(team_id, email) WHERE deleted_at IS NULL;

-- Tenant-property assignment history
CREATE TABLE property_tenant_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL REFERENCES teams(id),
    property_id     UUID NOT NULL REFERENCES properties(id),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    moved_in_at     TIMESTAMP,
    moved_out_at    TIMESTAMP,
    action_type     VARCHAR(50) NOT NULL,
    performed_by    UUID NOT NULL REFERENCES users(id),
    performed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_history_tenant ON property_tenant_history(tenant_id);
CREATE INDEX idx_tenant_history_property ON property_tenant_history(property_id);
CREATE INDEX idx_tenant_history_team ON property_tenant_history(team_id);
