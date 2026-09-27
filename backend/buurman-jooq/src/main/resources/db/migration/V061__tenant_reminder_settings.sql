-- BUUR-101: tenant reminder consent + dunning ladder.
-- Tenant-facing reminder emails are OFF by default. They are only sent when explicitly
-- enabled on the contract or on the tenant contact, and never while a contract is paused.
ALTER TABLE contacts
ADD COLUMN payment_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE contracts
ADD COLUMN tenant_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN reminders_paused_until DATE;

-- Team-level dunning ladder: automatic reminders off by default, steps as an ordered JSON array
-- of {offsetDays, tone, enabled} relative to the payment due date (negative = before due).
ALTER TABLE team_preferences
ADD COLUMN automatic_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN payment_reminder_steps JSONB NOT NULL DEFAULT '[]'::JSONB;

-- Which ladder step (if any) produced a reminder, so the scheduler never sends a step twice.
ALTER TABLE payment_reminders
ADD COLUMN step_offset_days INTEGER,
ADD COLUMN tone VARCHAR(20);

CREATE INDEX idx_payment_reminders_payment_step ON payment_reminders (payment_id, reminder_type, step_offset_days)
WHERE
    deleted_at IS NULL;
