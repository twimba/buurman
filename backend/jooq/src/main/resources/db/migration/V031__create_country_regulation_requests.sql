-- Country regulation requests: users can request regulation data for countries not yet available
CREATE TABLE rent_regulation_country_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    country_name VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_country_requests_team_id ON rent_regulation_country_requests (team_id);

CREATE UNIQUE INDEX idx_country_requests_unique ON rent_regulation_country_requests (team_id, lower(country_name), created_by);
