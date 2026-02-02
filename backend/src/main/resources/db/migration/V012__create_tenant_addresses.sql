-- Create tenant_addresses table
CREATE TABLE tenant_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id),
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'Netherlands',
    address_type VARCHAR(20) NOT NULL CHECK (address_type IN ('CURRENT', 'MAILING', 'RELATIVE', 'WORK', 'HISTORIC')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_tenant_addresses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_addresses_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Create indexes
CREATE INDEX idx_tenant_addresses_tenant_id ON tenant_addresses(tenant_id);
CREATE INDEX idx_tenant_addresses_team_id ON tenant_addresses(team_id);
CREATE INDEX idx_tenant_addresses_status ON tenant_addresses(status);
CREATE INDEX idx_tenant_addresses_type ON tenant_addresses(address_type);
CREATE INDEX idx_tenant_addresses_deleted_at ON tenant_addresses(deleted_at);

-- Create unique constraint for one active CURRENT address per tenant
CREATE UNIQUE INDEX idx_tenant_addresses_unique_current_active
ON tenant_addresses(tenant_id)
WHERE address_type = 'CURRENT' AND status = 'ACTIVE' AND deleted_at IS NULL;
