-- Database-backed segments (replaces Segment enum)
-- Like feature_flags, segments are global platform concepts, NOT tenant data.
CREATE TABLE segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_segments_key ON segments (key)
WHERE
    deleted_at IS NULL;

CREATE TABLE segment_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id UUID NOT NULL REFERENCES segments (id),
    attribute VARCHAR(64) NOT NULL,
    operator VARCHAR(16) NOT NULL,
    value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CHECK (attribute IN ('is_demo', 'is_owner', 'role')),
    CHECK (operator IN ('eq', 'neq', 'in', 'not_in'))
);

CREATE INDEX idx_sc_segment_id ON segment_conditions (segment_id);

-- Seed existing enum segments as DB rows
INSERT INTO
    segments (key, name, description, priority)
VALUES
    (
        'demo_accounts',
        'Demo Accounts',
        'Demo team accounts',
        0
    ),
    (
        'team_admins',
        'Team Admins',
        'Users with TEAM_ADMIN role',
        10
    ),
    (
        'team_editors',
        'Team Editors',
        'Users with TEAM_EDITOR role',
        10
    ),
    (
        'team_viewers',
        'Team Viewers',
        'Users with TEAM_VIEWER role',
        10
    ),
    ('owners', 'Owners', 'Team owners', 5);

-- Seed conditions for default segments
INSERT INTO
    segment_conditions (segment_id, attribute, operator, value)
SELECT
    id,
    'is_demo',
    'eq',
    'true'
FROM
    segments
WHERE
    key = 'demo_accounts';

INSERT INTO
    segment_conditions (segment_id, attribute, operator, value)
SELECT
    id,
    'role',
    'eq',
    'TEAM_ADMIN'
FROM
    segments
WHERE
    key = 'team_admins';

INSERT INTO
    segment_conditions (segment_id, attribute, operator, value)
SELECT
    id,
    'role',
    'eq',
    'TEAM_EDITOR'
FROM
    segments
WHERE
    key = 'team_editors';

INSERT INTO
    segment_conditions (segment_id, attribute, operator, value)
SELECT
    id,
    'role',
    'eq',
    'TEAM_VIEWER'
FROM
    segments
WHERE
    key = 'team_viewers';

INSERT INTO
    segment_conditions (segment_id, attribute, operator, value)
SELECT
    id,
    'is_owner',
    'eq',
    'true'
FROM
    segments
WHERE
    key = 'owners';
