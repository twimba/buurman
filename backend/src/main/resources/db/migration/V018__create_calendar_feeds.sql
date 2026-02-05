CREATE TABLE calendar_feeds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier      VARCHAR(29) NOT NULL,
    team_id         UUID NOT NULL REFERENCES teams(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    feed_token      VARCHAR(52) NOT NULL,
    feed_type       VARCHAR(20) NOT NULL,
    contract_id     UUID REFERENCES contracts(id),
    property_id     UUID REFERENCES properties(id),
    tenant_id       UUID REFERENCES tenants(id),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      UUID NOT NULL REFERENCES users(id),
    updated_by      UUID NOT NULL REFERENCES users(id),
    deleted_at      TIMESTAMP,

    CONSTRAINT uq_calendar_feeds_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_calendar_feeds_feed_token UNIQUE (feed_token),
    CONSTRAINT chk_calendar_feeds_feed_type CHECK (feed_type IN ('ALL_PAYMENTS', 'CONTRACT', 'PROPERTY_PAYMENTS', 'TENANT_PAYMENTS')),
    CONSTRAINT chk_calendar_feeds_entity_required CHECK (
        (feed_type = 'CONTRACT' AND contract_id IS NOT NULL AND property_id IS NULL AND tenant_id IS NULL) OR
        (feed_type = 'PROPERTY_PAYMENTS' AND property_id IS NOT NULL AND contract_id IS NULL AND tenant_id IS NULL) OR
        (feed_type = 'TENANT_PAYMENTS' AND tenant_id IS NOT NULL AND contract_id IS NULL AND property_id IS NULL) OR
        (feed_type = 'ALL_PAYMENTS' AND contract_id IS NULL AND property_id IS NULL AND tenant_id IS NULL)
    )
);

CREATE INDEX idx_calendar_feeds_team_id ON calendar_feeds(team_id);
CREATE INDEX idx_calendar_feeds_user_id ON calendar_feeds(user_id);
CREATE INDEX idx_calendar_feeds_feed_token ON calendar_feeds(feed_token) WHERE deleted_at IS NULL;
CREATE INDEX idx_calendar_feeds_contract_id ON calendar_feeds(contract_id) WHERE contract_id IS NOT NULL;
CREATE INDEX idx_calendar_feeds_property_id ON calendar_feeds(property_id) WHERE property_id IS NOT NULL;
CREATE INDEX idx_calendar_feeds_tenant_id ON calendar_feeds(tenant_id) WHERE tenant_id IS NOT NULL;
