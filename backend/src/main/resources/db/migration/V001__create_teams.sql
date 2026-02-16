CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    name VARCHAR(255) NOT NULL,
    settings JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CONSTRAINT teams_identifier_key UNIQUE (identifier)
);

CREATE INDEX idx_teams_identifier ON teams (identifier);

CREATE INDEX idx_teams_settings ON teams USING gin (settings);
