-- Add photo support to documents table
ALTER TABLE documents ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT 'DOCUMENT';
ALTER TABLE documents ADD COLUMN IF NOT EXISTS is_main_photo BOOLEAN DEFAULT FALSE;

-- Add index for main photos
CREATE INDEX idx_documents_main_photo ON documents(entity_type, entity_id, is_main_photo) WHERE deleted_at IS NULL AND is_main_photo = TRUE;

-- Add constraint to ensure only one main photo per property
CREATE UNIQUE INDEX idx_documents_one_main_photo_per_entity
ON documents(entity_type, entity_id)
WHERE deleted_at IS NULL AND is_main_photo = TRUE;

-- Add check constraint for category values
ALTER TABLE documents ADD CONSTRAINT chk_document_category
CHECK (category IN ('DOCUMENT', 'PHOTO'));

-- Update existing photo documents to have category = 'PHOTO'
UPDATE documents
SET category = 'PHOTO'
WHERE mime_type LIKE 'image/%';
