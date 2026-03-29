-- V020: Data takeout (Buurman Takeout) support
-- Tracks async data export requests per team
CREATE TABLE data_takeouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    file_key VARCHAR(500),
    file_size BIGINT,
    error TEXT,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_data_takeouts_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_data_takeouts_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT chk_data_takeouts_progress CHECK (
        progress >= 0
        AND progress <= 100
    )
);

CREATE INDEX idx_data_takeouts_team_id ON data_takeouts (team_id);

CREATE INDEX idx_data_takeouts_status ON data_takeouts (status);

CREATE INDEX idx_data_takeouts_expires_at ON data_takeouts (expires_at)
WHERE
    deleted_at IS NULL;

-- Add takeout retention setting to team preferences
ALTER TABLE team_preferences
ADD COLUMN takeout_retention_days INTEGER NOT NULL DEFAULT 30;
