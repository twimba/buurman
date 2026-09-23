-- BUUR-101: Rent collection maturity — payment reminder log.
-- Records every reminder sent to a tenant about an outstanding payment, whether triggered
-- manually by a team member or automatically by the dunning scheduler. Drives the
-- per-payment "communications" timeline and the arrears view's "last reminded" column.
CREATE TABLE payment_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    payment_id UUID NOT NULL REFERENCES payments (id),
    contact_id UUID REFERENCES contacts (id),
    -- MANUAL = sent by a team member from the app; AUTOMATIC = sent by the scheduler
    reminder_type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(255),
    -- Snapshot of the payment at send time so the timeline stays meaningful after edits
    days_overdue INTEGER NOT NULL DEFAULT 0,
    outstanding_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    notes TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_payment_reminders_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_payment_reminders_type CHECK (reminder_type IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT chk_payment_reminders_channel CHECK (channel IN ('EMAIL', 'SMS')),
    CONSTRAINT chk_payment_reminders_days CHECK (days_overdue >= 0),
    CONSTRAINT chk_payment_reminders_amount CHECK (outstanding_amount >= 0)
);

CREATE INDEX idx_payment_reminders_team_id ON payment_reminders (team_id);

CREATE INDEX idx_payment_reminders_payment_id ON payment_reminders (payment_id);

CREATE INDEX idx_payment_reminders_team_sent_at ON payment_reminders (team_id, sent_at DESC);

CREATE INDEX idx_payment_reminders_deleted_at ON payment_reminders (deleted_at);
