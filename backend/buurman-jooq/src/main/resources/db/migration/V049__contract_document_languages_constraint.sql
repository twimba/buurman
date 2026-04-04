-- Add CHECK constraint on document_languages array size (1-10 entries)
ALTER TABLE contracts
ADD CONSTRAINT chk_document_languages_size CHECK (
    array_length(document_languages, 1) BETWEEN 1 AND 10
);
