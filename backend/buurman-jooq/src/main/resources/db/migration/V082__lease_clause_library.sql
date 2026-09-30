CREATE TABLE lease_clause_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    clause_key VARCHAR(64) NOT NULL,
    title_i18n_key VARCHAR(128) NOT NULL,
    body_i18n_key VARCHAR(128) NOT NULL,
    default_included BOOLEAN NOT NULL DEFAULT TRUE,
    optional BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_lease_clause_templates_identifier UNIQUE (identifier),
    CONSTRAINT uq_lease_clause_templates_country_key_version UNIQUE (country_code, clause_key, version)
);

CREATE INDEX idx_lease_clause_templates_country ON lease_clause_templates (country_code)
WHERE
    deleted_at IS NULL;

CREATE TABLE contract_lease_clauses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    clause_template_id UUID NOT NULL REFERENCES lease_clause_templates (id),
    included BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT uq_contract_lease_clauses_contract_template UNIQUE (contract_id, clause_template_id)
);

CREATE INDEX idx_contract_lease_clauses_contract ON contract_lease_clauses (contract_id);

CREATE INDEX idx_contract_lease_clauses_team ON contract_lease_clauses (team_id);
