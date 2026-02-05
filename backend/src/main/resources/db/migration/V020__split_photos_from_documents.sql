-- V020: Split photos into their own table, separate from documents

-- 1. Create photos table
CREATE TABLE photos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier      VARCHAR(29) NOT NULL,
    team_id         UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    -- Entity association (polymorphic)
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,

    -- File information
    file_key        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,

    -- Metadata
    title           VARCHAR(255),
    notes           TEXT,
    is_main_photo   BOOLEAN DEFAULT FALSE,

    -- Audit
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP,

    CONSTRAINT chk_photos_entity_type CHECK (entity_type IN ('PROPERTY', 'TENANT', 'CONTRACT', 'PAYMENT', 'EXPENSE')),
    CONSTRAINT chk_photos_file_size CHECK (file_size > 0)
);

CREATE UNIQUE INDEX idx_photos_team_identifier ON photos(team_id, identifier);
CREATE INDEX idx_photos_entity ON photos(team_id, entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_photos_team ON photos(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_photos_main_photo ON photos(entity_type, entity_id, is_main_photo) WHERE deleted_at IS NULL AND is_main_photo = TRUE;
CREATE UNIQUE INDEX idx_photos_one_main_photo_per_entity ON photos(entity_type, entity_id) WHERE deleted_at IS NULL AND is_main_photo = TRUE;

-- 2. Migrate existing photo records from documents to photos
INSERT INTO photos (id, identifier, team_id, entity_type, entity_id, file_key, file_name, file_size, mime_type, title, notes, is_main_photo, uploaded_by, uploaded_at, deleted_at)
SELECT id, identifier, team_id, entity_type, entity_id, file_key, file_name, file_size, mime_type, title, notes, is_main_photo, uploaded_by, uploaded_at, deleted_at
FROM documents
WHERE category = 'PHOTO';

-- 3. Delete migrated photos from documents
DELETE FROM documents WHERE category = 'PHOTO';

-- 4. Drop photo-specific columns and constraints from documents
DROP INDEX IF EXISTS idx_documents_main_photo;
DROP INDEX IF EXISTS idx_documents_one_main_photo_per_entity;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS chk_documents_category;
ALTER TABLE documents DROP COLUMN IF EXISTS category;
ALTER TABLE documents DROP COLUMN IF EXISTS is_main_photo;
