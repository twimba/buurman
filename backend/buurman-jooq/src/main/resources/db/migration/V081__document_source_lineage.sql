-- ===== from V086__document_source_lineage.sql =====
-- Links a generated document (e.g. a signed copy or signing certificate produced by the
-- e-signature flow) back to the original document it was produced from, so the UI can group
-- them together instead of showing them as unrelated flat rows.
ALTER TABLE documents
ADD COLUMN source_document_id UUID REFERENCES documents (id);

CREATE INDEX idx_documents_source_document ON documents (source_document_id)
WHERE
    deleted_at IS NULL;
