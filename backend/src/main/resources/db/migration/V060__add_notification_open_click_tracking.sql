-- Add open/click tracking columns for SendGrid event webhooks
ALTER TABLE notifications
    ADD COLUMN open_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN click_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN first_opened_at TIMESTAMP,
    ADD COLUMN first_clicked_at TIMESTAMP;
