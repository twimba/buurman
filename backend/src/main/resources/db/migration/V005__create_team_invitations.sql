CREATE TABLE team_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(36) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id),
    invited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMP,
    accepted_by UUID REFERENCES users(id),
    CHECK (role IN ('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER'))
);

CREATE INDEX idx_team_invitations_team ON team_invitations(team_id);
CREATE INDEX idx_team_invitations_token ON team_invitations(token) WHERE accepted_at IS NULL;
CREATE INDEX idx_team_invitations_expires ON team_invitations(expires_at) WHERE accepted_at IS NULL;
