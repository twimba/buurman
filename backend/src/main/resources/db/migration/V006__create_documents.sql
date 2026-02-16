CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    -- Entity association (polymorphic)
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    -- File information
    file_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    -- Metadata
    title VARCHAR(255),
    notes TEXT,
    category VARCHAR(50) DEFAULT 'DOCUMENT',
    is_main_photo BOOLEAN DEFAULT FALSE,
    -- Audit
    uploaded_by UUID NOT NULL REFERENCES users (id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT chk_documents_entity_type CHECK (
        entity_type IN (
            'PROPERTY',
            'TENANT',
            'CONTRACT',
            'PAYMENT',
            'EXPENSE'
        )
    ),
    CONSTRAINT chk_documents_file_size CHECK (file_size > 0),
    CONSTRAINT chk_documents_category CHECK (category IN ('DOCUMENT', 'PHOTO'))
);

CREATE UNIQUE INDEX idx_documents_team_identifier ON documents (team_id, identifier);

CREATE INDEX idx_documents_entity ON documents (team_id, entity_type, entity_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_documents_team ON documents (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_documents_main_photo ON documents (entity_type, entity_id, is_main_photo)
WHERE
    deleted_at IS NULL
    AND is_main_photo = TRUE;

CREATE UNIQUE INDEX idx_documents_one_main_photo_per_entity ON documents (entity_type, entity_id)
WHERE
    deleted_at IS NULL
    AND is_main_photo = TRUE;
