CREATE TABLE broadcast_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP,
    show_on_login BOOLEAN NOT NULL DEFAULT FALSE,
    show_on_register BOOLEAN NOT NULL DEFAULT FALSE,
    show_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_broadcast_scope CHECK (scope IN ('GLOBAL', 'TEAMS', 'USERS'))
);

CREATE UNIQUE INDEX idx_broadcast_messages_identifier ON broadcast_messages (identifier);

CREATE INDEX idx_broadcast_messages_active ON broadcast_messages (start_at, end_at);

CREATE TABLE broadcast_message_teams (
    broadcast_message_id UUID NOT NULL REFERENCES broadcast_messages (id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    PRIMARY KEY (broadcast_message_id, team_id)
);

CREATE INDEX idx_broadcast_message_teams_team_id ON broadcast_message_teams (team_id);

CREATE TABLE broadcast_message_users (
    broadcast_message_id UUID NOT NULL REFERENCES broadcast_messages (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (broadcast_message_id, user_id)
);

CREATE INDEX idx_broadcast_message_users_user_id ON broadcast_message_users (user_id);

CREATE TABLE broadcast_message_dismissals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    broadcast_message_id UUID NOT NULL REFERENCES broadcast_messages (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    dismissed_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (broadcast_message_id, user_id)
);
