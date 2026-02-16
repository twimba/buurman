ALTER TABLE user_notification_type_preferences
DROP CONSTRAINT chk_notif_type,
ADD CONSTRAINT chk_notif_type CHECK (
    notification_type IN (
        'PROPERTY_CREATED',
        'CONTRACT_CREATED',
        'CONTRACT_STATUS_CHANGED',
        'CONTRACT_REOPENED',
        'PAYMENT_REMINDER',
        'CONTRACT_EXPIRY',
        'PAYMENT_PAID',
        'PAYMENT_RECEIVAL',
        'EXPENSE_CREATED'
    )
);
