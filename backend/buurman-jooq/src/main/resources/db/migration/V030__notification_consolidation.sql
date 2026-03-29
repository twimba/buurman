-- Denormalized grouping columns on notification_outbox (self-standing outbox)
ALTER TABLE notification_outbox
ADD COLUMN notification_type VARCHAR(50);

ALTER TABLE notification_outbox
ADD COLUMN recipient_user_id UUID;

ALTER TABLE notification_outbox
ADD COLUMN recipient_email VARCHAR(255);

ALTER TABLE notification_outbox
ADD COLUMN team_id UUID;

-- Urgency column on notification_outbox
ALTER TABLE notification_outbox
ADD COLUMN urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Consolidation tracking columns
ALTER TABLE notification_outbox
ADD COLUMN consolidation_group_id UUID;

ALTER TABLE notification_outbox
ADD COLUMN is_consolidated BOOLEAN NOT NULL DEFAULT FALSE;

-- Urgency column on notifications
ALTER TABLE notifications
ADD COLUMN urgency VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Backfill existing outbox rows from notifications
UPDATE notification_outbox o
SET
    notification_type = n.notification_type,
    recipient_user_id = n.recipient_user_id,
    recipient_email = n.recipient_email,
    team_id = n.team_id
FROM
    notifications n
WHERE
    o.notification_id = n.id;

-- Consolidation group index (for retry/failure tracking)
CREATE INDEX idx_outbox_consolidation_group ON notification_outbox (consolidation_group_id)
WHERE
    consolidation_group_id IS NOT NULL;

-- Grouping index (for consolidation queries during batch processing)
CREATE INDEX idx_outbox_grouping ON notification_outbox (
    notification_type,
    recipient_user_id,
    team_id,
    channel
)
WHERE
    status IN ('PENDING', 'FAILED');
