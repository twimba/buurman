-- =============================================================================
-- generated_reports
-- =============================================================================
CREATE TABLE generated_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL,
    parameters JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    file_key VARCHAR(500),
    error TEXT,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES users (id),
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_reports_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_reports_progress CHECK (
        progress >= 0
        AND progress <= 100
    )
);

CREATE INDEX idx_reports_team ON generated_reports (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_reports_status ON generated_reports (status)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_reports_expires ON generated_reports (expires_at)
WHERE
    status = 'COMPLETED'
    AND deleted_at IS NULL;

CREATE INDEX idx_reports_created_at ON generated_reports (team_id, created_at)
WHERE
    deleted_at IS NULL;

-- =============================================================================
-- calendar_feeds
-- =============================================================================
CREATE TABLE calendar_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    user_id UUID NOT NULL REFERENCES users (id),
    feed_token VARCHAR(52) NOT NULL,
    feed_type VARCHAR(20) NOT NULL,
    contract_id UUID REFERENCES contracts (id),
    property_id UUID REFERENCES properties (id),
    tenant_id UUID REFERENCES tenants (id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_calendar_feeds_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_calendar_feeds_feed_token UNIQUE (feed_token),
    CONSTRAINT chk_calendar_feeds_entity_required CHECK (
        (
            feed_type = 'CONTRACT'
            AND contract_id IS NOT NULL
            AND property_id IS NULL
            AND tenant_id IS NULL
        )
        OR (
            feed_type = 'PROPERTY_PAYMENTS'
            AND property_id IS NOT NULL
            AND contract_id IS NULL
            AND tenant_id IS NULL
        )
        OR (
            feed_type = 'TENANT_PAYMENTS'
            AND tenant_id IS NOT NULL
            AND contract_id IS NULL
            AND property_id IS NULL
        )
        OR (
            feed_type = 'ALL_PAYMENTS'
            AND contract_id IS NULL
            AND property_id IS NULL
            AND tenant_id IS NULL
        )
    )
);

CREATE INDEX idx_calendar_feeds_team_id ON calendar_feeds (team_id);

CREATE INDEX idx_calendar_feeds_user_id ON calendar_feeds (user_id);

CREATE INDEX idx_calendar_feeds_feed_token ON calendar_feeds (feed_token)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_calendar_feeds_contract_id ON calendar_feeds (contract_id)
WHERE
    contract_id IS NOT NULL;

CREATE INDEX idx_calendar_feeds_property_id ON calendar_feeds (property_id)
WHERE
    property_id IS NOT NULL;

CREATE INDEX idx_calendar_feeds_tenant_id ON calendar_feeds (tenant_id)
WHERE
    tenant_id IS NOT NULL;
