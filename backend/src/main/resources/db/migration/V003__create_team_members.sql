CREATE TABLE team_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(50) NOT NULL,
    is_owner    BOOLEAN NOT NULL DEFAULT false,
    invited_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    invited_by  UUID REFERENCES users(id),
    joined_at   TIMESTAMP,
    deleted_at  TIMESTAMP,

    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id),
    CONSTRAINT chk_team_members_role CHECK (role IN ('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER'))
);

CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_is_owner ON team_members(team_id) WHERE is_owner = true;
