-- Add channel preferences to user_team_notification_preferences
ALTER TABLE user_team_notification_preferences
ADD COLUMN preferred_channels VARCHAR(50) NOT NULL DEFAULT 'EMAIL';

-- Add SMS notifications toggle to global user_preferences
ALTER TABLE user_preferences
ADD COLUMN sms_notifications BOOLEAN NOT NULL DEFAULT FALSE;
