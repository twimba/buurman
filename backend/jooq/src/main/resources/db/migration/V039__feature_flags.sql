-- Built-in feature flag tables (BUUR-87)
-- Note: feature_flags has NO team_id — flags are global platform capabilities, not tenant data.
CREATE TABLE feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(64) NOT NULL UNIQUE,
    value_type VARCHAR(16) NOT NULL,
    default_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_value TEXT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CHECK (value_type IN ('boolean', 'integer', 'string'))
);

CREATE INDEX idx_ff_key ON feature_flags (key)
WHERE
    deleted_at IS NULL;

CREATE TABLE feature_flag_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(64) NOT NULL REFERENCES feature_flags (key),
    scope VARCHAR(16) NOT NULL,
    segment_key VARCHAR(64),
    team_id UUID REFERENCES teams (id),
    user_id UUID REFERENCES users (id),
    enabled BOOLEAN NOT NULL,
    value TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    CHECK (scope IN ('user', 'team', 'segment')),
    CHECK (
        (
            scope = 'user'
            AND user_id IS NOT NULL
            AND team_id IS NOT NULL
            AND segment_key IS NULL
        )
        OR (
            scope = 'team'
            AND team_id IS NOT NULL
            AND user_id IS NULL
            AND segment_key IS NULL
        )
        OR (
            scope = 'segment'
            AND segment_key IS NOT NULL
            AND team_id IS NULL
            AND user_id IS NULL
        )
    )
);

-- Partial unique indexes (PostgreSQL NULLs are distinct in UNIQUE constraints)
CREATE UNIQUE INDEX uq_ffo_user ON feature_flag_overrides (flag_key, team_id, user_id)
WHERE
    scope = 'user'
    AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_ffo_team ON feature_flag_overrides (flag_key, team_id)
WHERE
    scope = 'team'
    AND user_id IS NULL
    AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_ffo_segment ON feature_flag_overrides (flag_key, segment_key)
WHERE
    scope = 'segment'
    AND deleted_at IS NULL;

CREATE INDEX idx_ffo_flag_key ON feature_flag_overrides (flag_key);

CREATE INDEX idx_ffo_team_id ON feature_flag_overrides (team_id)
WHERE
    team_id IS NOT NULL;

-- Seed the 9 existing feature flags
INSERT INTO
    feature_flags (
        key,
        value_type,
        default_enabled,
        default_value,
        description
    )
VALUES
    (
        'reports',
        'boolean',
        TRUE,
        NULL,
        'Enable financial reports'
    ),
    (
        'invitation_required',
        'boolean',
        TRUE,
        NULL,
        'Registration requires invitation'
    ),
    (
        'sms_notifications',
        'boolean',
        FALSE,
        NULL,
        'Global SMS channel enable'
    ),
    (
        'email_notifications',
        'boolean',
        TRUE,
        NULL,
        'Global email channel enable'
    ),
    (
        'block_email_notifications',
        'boolean',
        FALSE,
        NULL,
        'Block email notifications (demo accounts)'
    ),
    (
        'block_sms_notifications',
        'boolean',
        FALSE,
        NULL,
        'Block SMS notifications (demo accounts)'
    ),
    (
        'takeout_max_exports',
        'integer',
        TRUE,
        '3',
        'Maximum concurrent data exports'
    ),
    (
        'excel_export',
        'boolean',
        FALSE,
        NULL,
        'Excel export capability'
    ),
    (
        'swagger',
        'boolean',
        TRUE,
        NULL,
        'Swagger UI access'
    );

-- Segment overrides matching current Flagsmith setup
INSERT INTO
    feature_flag_overrides (
        flag_key,
        scope,
        segment_key,
        enabled,
        value,
        priority
    )
VALUES
    (
        'block_email_notifications',
        'segment',
        'demo_accounts',
        TRUE,
        NULL,
        0
    ),
    (
        'block_sms_notifications',
        'segment',
        'demo_accounts',
        TRUE,
        NULL,
        0
    );
