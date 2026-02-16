CREATE TABLE team_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invited_by UUID NOT NULL REFERENCES users (id),
    invited_at TIMESTAMP NOT NULL DEFAULT now(),
    accepted_at TIMESTAMP,
    accepted_by UUID REFERENCES users (id),
    email_sent_at TIMESTAMP,
    email_error TEXT,
    pending_first_name VARCHAR(255),
    pending_last_name VARCHAR(255),
    resent_at TIMESTAMP,
    resent_count INTEGER DEFAULT 0,
    deleted_at TIMESTAMP,
    CONSTRAINT team_invitations_token_key UNIQUE (token),
    CONSTRAINT chk_team_invitations_role CHECK (
        role IN ('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')
    )
);

CREATE INDEX idx_team_invitations_team ON team_invitations (team_id);

CREATE INDEX idx_team_invitations_token ON team_invitations (token)
WHERE
    accepted_at IS NULL;

CREATE INDEX idx_team_invitations_expires ON team_invitations (expires_at)
WHERE
    accepted_at IS NULL;

CREATE INDEX idx_team_invitations_email_pending ON team_invitations (team_id)
WHERE
    email_sent_at IS NULL
    AND accepted_at IS NULL;
