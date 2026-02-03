-- V024: Enhanced invitations
-- Track email delivery, support for pending users, and resend functionality

-- Track when invitation email was sent
ALTER TABLE team_invitations ADD COLUMN email_sent_at TIMESTAMP;

-- Store any email delivery errors
ALTER TABLE team_invitations ADD COLUMN email_error TEXT;

-- Support for inviting users who don't have accounts yet
ALTER TABLE team_invitations ADD COLUMN pending_first_name VARCHAR(255);
ALTER TABLE team_invitations ADD COLUMN pending_last_name VARCHAR(255);

-- Track resend attempts
ALTER TABLE team_invitations ADD COLUMN resent_at TIMESTAMP;
ALTER TABLE team_invitations ADD COLUMN resent_count INTEGER DEFAULT 0;

-- Index for finding pending invitations by team
CREATE INDEX idx_team_invitations_email_pending
ON team_invitations(team_id)
WHERE email_sent_at IS NULL AND accepted_at IS NULL;
