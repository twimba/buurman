-- A reminder row records the send request; the linked notification carries delivery state
-- (queued / sent / delivered / failed) maintained by the outbox and provider webhooks.
ALTER TABLE payment_reminders
    ADD COLUMN notification_id UUID REFERENCES notifications (id);
CREATE INDEX idx_payment_reminders_notification
    ON payment_reminders (notification_id)
WHERE
    notification_id IS NOT NULL;

-- An overpayment credit remembers the receival that produced it, so reversing the receival
-- can reverse (or refuse to reverse) the credit.
ALTER TABLE contact_credits
    ADD COLUMN source_receival_id UUID REFERENCES payment_receivals (id);
CREATE INDEX idx_contact_credits_source_receival
    ON contact_credits (source_receival_id)
WHERE
    source_receival_id IS NOT NULL;
