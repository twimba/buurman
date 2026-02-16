-- Notifications: the "golden registry" of all notifications ever sent
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    -- What was sent
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(20),
    recipient_user_id UUID REFERENCES users (id),
    recipient_tenant_id UUID REFERENCES tenants (id),
    -- Channel & content
    channel VARCHAR(20) NOT NULL,
    content_template VARCHAR(100) NOT NULL,
    content_variables JSONB,
    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    provider_status VARCHAR(50),
    provider_error TEXT,
    status_updated_at TIMESTAMP,
    -- Resend tracking
    resent_from_id UUID REFERENCES notifications (id),
    resend_reason VARCHAR(255),
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users (id),
    CONSTRAINT uq_notifications_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'SMS')),
    CONSTRAINT chk_notifications_status CHECK (
        status IN (
            'PENDING',
            'QUEUED',
            'SENT',
            'DELIVERED',
            'FAILED',
            'BOUNCED',
            'REJECTED'
        )
    )
);

CREATE INDEX idx_notifications_team ON notifications (team_id);

CREATE INDEX idx_notifications_team_type ON notifications (team_id, notification_type);

CREATE INDEX idx_notifications_team_status ON notifications (team_id, status);

CREATE INDEX idx_notifications_team_channel ON notifications (team_id, channel);

CREATE INDEX idx_notifications_team_created ON notifications (team_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_user ON notifications (recipient_user_id)
WHERE
    recipient_user_id IS NOT NULL;

CREATE INDEX idx_notifications_recipient_tenant ON notifications (recipient_tenant_id)
WHERE
    recipient_tenant_id IS NOT NULL;

CREATE INDEX idx_notifications_provider_msg ON notifications (provider_message_id)
WHERE
    provider_message_id IS NOT NULL;

CREATE INDEX idx_notifications_resent_from ON notifications (resent_from_id)
WHERE
    resent_from_id IS NOT NULL;

-- Outbox table: transactional outbox for reliable delivery
CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications (id),
    channel VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    processed_at TIMESTAMP,
    CONSTRAINT chk_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')
    )
);

CREATE INDEX idx_outbox_pending ON notification_outbox (status, next_retry_at)
WHERE
    status IN ('PENDING', 'FAILED');

CREATE INDEX idx_outbox_notification ON notification_outbox (notification_id);
