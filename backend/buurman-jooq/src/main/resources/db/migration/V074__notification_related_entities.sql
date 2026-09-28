-- Which payment or contract a notification is about, so a landlord can see the
-- delivery trail on the entity itself rather than in the admin log.
-- A payment reminder sets BOTH: it is about the payment and about its contract,
-- and the contract timeline should show it without joining through payments.
-- Nullable: most notification types (welcome, verification, invitations) are
-- about neither, and historical rows cannot be attributed retroactively.
ALTER TABLE notifications
ADD COLUMN payment_id UUID REFERENCES payments (id),
ADD COLUMN contract_id UUID REFERENCES contracts (id);

CREATE INDEX idx_notifications_payment ON notifications (payment_id)
WHERE
    payment_id IS NOT NULL;

CREATE INDEX idx_notifications_contract ON notifications (contract_id)
WHERE
    contract_id IS NOT NULL;
