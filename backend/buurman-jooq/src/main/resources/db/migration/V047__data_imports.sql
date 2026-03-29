-- Data imports tracking for batch import operations
CREATE TYPE data_import_status AS ENUM(
    'PROCESSING',
    'COMPLETED',
    'PARTIALLY_COMPLETED',
    'FAILED',
    'REVERTED'
);

CREATE TYPE data_import_entity_type AS ENUM('CONTACT');

CREATE TABLE data_imports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    file_name VARCHAR(255) NOT NULL,
    file_format VARCHAR(10) NOT NULL,
    entity_type data_import_entity_type NOT NULL DEFAULT 'CONTACT',
    status data_import_status NOT NULL DEFAULT 'PROCESSING',
    total_rows INTEGER NOT NULL DEFAULT 0,
    imported_rows INTEGER NOT NULL DEFAULT 0,
    skipped_rows INTEGER NOT NULL DEFAULT 0,
    error_rows INTEGER NOT NULL DEFAULT 0,
    column_mapping JSONB NOT NULL DEFAULT '{}',
    error_report JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    reverted_at TIMESTAMP,
    reverted_by UUID,
    CONSTRAINT uq_data_imports_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_data_imports_team_id ON data_imports (team_id);

CREATE INDEX idx_data_imports_status ON data_imports (status);

CREATE INDEX idx_data_imports_created_at ON data_imports (created_at);

CREATE TABLE data_import_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_id UUID NOT NULL REFERENCES data_imports (id) ON DELETE CASCADE,
    entity_type data_import_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_data_import_items_import_id ON data_import_items (import_id);

CREATE INDEX idx_data_import_items_entity ON data_import_items (entity_type, entity_id);

-- Add import_id to contacts for traceability
ALTER TABLE contacts
ADD COLUMN import_id UUID REFERENCES data_imports (id);

CREATE INDEX idx_contacts_import_id ON contacts (import_id);
