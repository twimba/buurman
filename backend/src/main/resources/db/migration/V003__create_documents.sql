CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    -- Entity association (polymorphic)
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('PROPERTY', 'TENANT', 'CONTRACT', 'PAYMENT', 'EXPENSE')),
    entity_id UUID NOT NULL,

    -- File information
    file_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,

    -- Metadata
    title VARCHAR(255),
    notes TEXT,

    -- Audit
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,

    CHECK (file_size > 0)
);

-- Indexes
CREATE INDEX idx_documents_entity ON documents(team_id, entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_documents_team ON documents(team_id) WHERE deleted_at IS NULL;
