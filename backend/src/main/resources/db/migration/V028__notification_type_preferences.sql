-- Per-type notification channel preferences (user-level)
CREATE TABLE user_notification_type_preferences (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    email_enabled     BOOLEAN NOT NULL DEFAULT true,
    sms_enabled       BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_notif_type_pref UNIQUE (user_id, notification_type),
    CONSTRAINT chk_notif_type CHECK (notification_type IN (
        'PROPERTY_CREATED','CONTRACT_CREATED','CONTRACT_STATUS_CHANGED',
        'PAYMENT_REMINDER','CONTRACT_EXPIRY','PAYMENT_PAID',
        'PAYMENT_RECEIVAL','EXPENSE_CREATED'
    ))
);

CREATE INDEX idx_user_notif_type_pref_user ON user_notification_type_preferences(user_id);
