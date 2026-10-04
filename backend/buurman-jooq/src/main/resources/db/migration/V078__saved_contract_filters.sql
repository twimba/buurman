-- ===== from V079__saved_contract_filters.sql =====
CREATE TABLE saved_contract_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    user_id UUID NOT NULL REFERENCES users (id),
    name VARCHAR(100) NOT NULL,
    criteria JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_saved_contract_filters_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_saved_contract_filters_user ON saved_contract_filters (team_id, user_id)
WHERE
    deleted_at IS NULL;
