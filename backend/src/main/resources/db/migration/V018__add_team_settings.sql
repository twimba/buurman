-- Add JSONB settings column to teams table for flexible configuration
ALTER TABLE teams ADD COLUMN settings JSONB DEFAULT '{}'::jsonb;

-- Create GIN index for efficient JSONB queries
CREATE INDEX idx_teams_settings ON teams USING gin(settings);

-- Set default settings for existing teams
UPDATE teams
SET settings = '{
  "payments": {
    "paymentsAheadCount": 3,
    "autoGenerationEnabled": true
  }
}'::jsonb
WHERE settings = '{}'::jsonb OR settings IS NULL;

-- Ensure settings is never NULL
ALTER TABLE teams ALTER COLUMN settings SET NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN teams.settings IS 'Team configuration in JSONB format (payments, notifications, etc.)';
