-- Add deleted_at column for soft deletes to core tables that were missing it
ALTER TABLE teams ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE team_members ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE team_invitations ADD COLUMN deleted_at TIMESTAMP;
