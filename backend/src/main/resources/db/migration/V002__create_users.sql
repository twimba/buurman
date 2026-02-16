CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    default_team_id UUID REFERENCES teams (id) ON DELETE SET NULL,
    active_team_id UUID REFERENCES teams (id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT users_identifier_key UNIQUE (identifier),
    CONSTRAINT users_keycloak_id_key UNIQUE (keycloak_id),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE INDEX idx_users_identifier ON users (identifier);

CREATE INDEX idx_users_keycloak_id ON users (keycloak_id);

CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_default_team ON users (default_team_id);

CREATE INDEX idx_users_active_team ON users (active_team_id);
