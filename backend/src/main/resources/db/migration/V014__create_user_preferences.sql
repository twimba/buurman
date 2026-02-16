-- Global user preferences
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    theme VARCHAR(20) DEFAULT 'system',
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    date_format VARCHAR(20) DEFAULT 'DD/MM/YYYY',
    currency_format VARCHAR(10) DEFAULT 'EUR',
    email_notifications BOOLEAN DEFAULT TRUE,
    in_app_notifications BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_preferences_user UNIQUE (user_id),
    CONSTRAINT chk_user_preferences_theme CHECK (theme IN ('light', 'dark', 'system'))
);

CREATE INDEX idx_user_preferences_user ON user_preferences (user_id);

-- Per-team notification preferences
CREATE TABLE user_team_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    payment_reminders BOOLEAN DEFAULT TRUE,
    contract_expiry_alerts BOOLEAN DEFAULT TRUE,
    new_member_notifications BOOLEAN DEFAULT TRUE,
    weekly_summary BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_team_notif_prefs UNIQUE (user_id, team_id)
);

CREATE INDEX idx_user_team_notif_prefs_user ON user_team_notification_preferences (user_id);

CREATE INDEX idx_user_team_notif_prefs_team ON user_team_notification_preferences (team_id);
