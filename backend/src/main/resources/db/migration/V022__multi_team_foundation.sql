-- V022: Multi-team foundation
-- Enables users to belong to multiple teams with team switching capability

-- Add is_owner to team_members to track team ownership
ALTER TABLE team_members ADD COLUMN is_owner BOOLEAN NOT NULL DEFAULT false;

-- Add default_team_id and active_team_id to users for multi-team support
ALTER TABLE users ADD COLUMN default_team_id UUID REFERENCES teams(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN active_team_id UUID REFERENCES teams(id) ON DELETE SET NULL;

-- Indexes for efficient team lookups
CREATE INDEX idx_users_default_team ON users(default_team_id);
CREATE INDEX idx_users_active_team ON users(active_team_id);
CREATE INDEX idx_team_members_is_owner ON team_members(team_id) WHERE is_owner = true;

-- Data migration: set existing team creators as owners
UPDATE team_members tm
SET is_owner = true
FROM teams t
WHERE tm.team_id = t.id AND tm.user_id = t.created_by;

-- For teams without a created_by, set the first member as owner
UPDATE team_members tm
SET is_owner = true
WHERE tm.id IN (
    SELECT DISTINCT ON (team_id) id
    FROM team_members
    WHERE team_id NOT IN (
        SELECT team_id FROM team_members WHERE is_owner = true
    )
    ORDER BY team_id, invited_at ASC
);

-- Set default_team_id and active_team_id for existing users based on their first membership
UPDATE users u
SET default_team_id = tm.team_id,
    active_team_id = tm.team_id
FROM (
    SELECT DISTINCT ON (user_id) user_id, team_id
    FROM team_members
    ORDER BY user_id, invited_at ASC
) tm
WHERE tm.user_id = u.id;
