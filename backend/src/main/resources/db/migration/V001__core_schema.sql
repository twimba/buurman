-- ---------------------------------------------------------------------------
-- 1. teams
-- ---------------------------------------------------------------------------
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    name VARCHAR(255) NOT NULL,
    demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CONSTRAINT teams_identifier_key UNIQUE (identifier)
);

CREATE INDEX idx_teams_identifier ON teams (identifier);

-- ---------------------------------------------------------------------------
-- 2. users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    default_team_id UUID REFERENCES teams (id) ON DELETE SET NULL,
    active_team_id UUID REFERENCES teams (id) ON DELETE SET NULL,
    email_verified_at TIMESTAMP,
    phone VARCHAR(20),
    phone_verified_at TIMESTAMP,
    disabled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT users_identifier_key UNIQUE (identifier),
    CONSTRAINT users_keycloak_id_key UNIQUE (keycloak_id),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE INDEX idx_users_identifier ON users (identifier);

CREATE INDEX idx_users_keycloak_id ON users (keycloak_id);

CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_default_team ON users (default_team_id);

CREATE INDEX idx_users_active_team ON users (active_team_id);

CREATE INDEX idx_users_phone ON users (phone)
WHERE
    phone IS NOT NULL;

CREATE INDEX idx_users_disabled_at ON users (disabled_at);

-- ---------------------------------------------------------------------------
-- 3. team_members
-- ---------------------------------------------------------------------------
CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    invited_at TIMESTAMP NOT NULL DEFAULT now(),
    invited_by UUID REFERENCES users (id),
    joined_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_team ON team_members (team_id);

CREATE INDEX idx_team_members_user ON team_members (user_id);

CREATE INDEX idx_team_members_is_owner ON team_members (team_id)
WHERE
    is_owner = TRUE;

-- ---------------------------------------------------------------------------
-- 4. team_invitations
-- ---------------------------------------------------------------------------
CREATE TABLE team_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invited_by UUID NOT NULL REFERENCES users (id),
    invited_at TIMESTAMP NOT NULL DEFAULT now(),
    accepted_at TIMESTAMP,
    accepted_by UUID REFERENCES users (id),
    email_sent_at TIMESTAMP,
    email_error TEXT,
    pending_first_name VARCHAR(255),
    pending_last_name VARCHAR(255),
    resent_at TIMESTAMP,
    resent_count INTEGER DEFAULT 0,
    deleted_at TIMESTAMP,
    CONSTRAINT team_invitations_token_key UNIQUE (token)
);

CREATE INDEX idx_team_invitations_team ON team_invitations (team_id);

CREATE INDEX idx_team_invitations_token ON team_invitations (token)
WHERE
    accepted_at IS NULL;

CREATE INDEX idx_team_invitations_expires ON team_invitations (expires_at)
WHERE
    accepted_at IS NULL;

CREATE INDEX idx_team_invitations_email_pending ON team_invitations (team_id)
WHERE
    email_sent_at IS NULL
    AND accepted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 5. team_preferences
-- ---------------------------------------------------------------------------
CREATE TABLE team_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    payments_ahead_count INTEGER NOT NULL DEFAULT 3,
    auto_generation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_currency VARCHAR(3),
    default_country VARCHAR(100) DEFAULT 'Netherlands',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Amsterdam',
    date_format VARCHAR(20) NOT NULL DEFAULT 'DD/MM/YYYY',
    fiscal_year_start_month VARCHAR(2) NOT NULL DEFAULT '01',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_preferences_team UNIQUE (team_id),
    CONSTRAINT chk_payments_ahead CHECK (payments_ahead_count BETWEEN 1 AND 12)
);

CREATE INDEX idx_team_preferences_team ON team_preferences (team_id);

-- ---------------------------------------------------------------------------
-- 6. registration_invitations
-- ---------------------------------------------------------------------------
CREATE TABLE registration_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    code VARCHAR(60) NOT NULL,
    max_usages INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(200),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(200) NOT NULL,
    CONSTRAINT uq_reg_invitation_code UNIQUE (code),
    CONSTRAINT uq_reg_invitation_identifier UNIQUE (identifier),
    CONSTRAINT chk_reg_invitation_max_usages CHECK (
        max_usages IS NULL
        OR max_usages > 0
    ),
    CONSTRAINT chk_reg_invitation_usage CHECK (usage_count >= 0)
);

CREATE INDEX idx_reg_inv_code ON registration_invitations (code);

CREATE INDEX idx_reg_inv_created ON registration_invitations (created_at DESC);

-- ---------------------------------------------------------------------------
-- 7. registration_invitation_usages
-- ---------------------------------------------------------------------------
CREATE TABLE registration_invitation_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL REFERENCES registration_invitations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    used_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reg_inv_usage_inv ON registration_invitation_usages (invitation_id);

CREATE INDEX idx_reg_inv_usage_user ON registration_invitation_usages (user_id);

-- ---------------------------------------------------------------------------
-- System user
-- ---------------------------------------------------------------------------
INSERT INTO
    users (
        id,
        identifier,
        keycloak_id,
        email,
        first_name,
        last_name,
        email_verified_at,
        created_at,
        updated_at
    )
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'USR000000000000000000SYSTEM',
        'system',
        'system@buurman.io',
        'System',
        'Buurman',
        now(),
        now(),
        now()
    )
ON CONFLICT (id) DO NOTHING;
