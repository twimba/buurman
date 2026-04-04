-- Add document_languages column to contracts table
-- Stores the preferred language(s) for generated documents (ISO 639-1 codes)
ALTER TABLE contracts
ADD COLUMN document_languages VARCHAR[] NOT NULL DEFAULT '{en}';
