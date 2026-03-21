-- =============================================================================
-- V039__contacts_amendments.sql
-- Post-review amendments: refined tags, relationship types, contact_tags ON DELETE CASCADE
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Refine contact_tags CHECK: 10 tags -> 8 tags
-- Remove: PROBLEM, COMMERCIAL, RESIDENTIAL, PREFERRED_VENDOR
-- Add: KEY_HOLDER, FORMER_TENANT
-- ---------------------------------------------------------------------------
ALTER TABLE contact_tags DROP CONSTRAINT IF EXISTS chk_contact_tags_tag;
ALTER TABLE contact_tags ADD CONSTRAINT chk_contact_tags_tag CHECK (
    tag IN (
        'VIP', 'PROSPECT', 'LATE_PAYER', 'LONG_TERM',
        'KEY_HOLDER', 'DO_NOT_CONTACT', 'FORMER_TENANT', 'REFERRED'
    )
);

-- Remove any rows with now-invalid tags (none expected in fresh DB,
-- but safety net for dev environments with demo data)
DELETE FROM contact_tags WHERE tag NOT IN (
    'VIP', 'PROSPECT', 'LATE_PAYER', 'LONG_TERM',
    'KEY_HOLDER', 'DO_NOT_CONTACT', 'FORMER_TENANT', 'REFERRED'
);

-- ---------------------------------------------------------------------------
-- 2. Refine contact_relationships CHECK: 8 types -> 6 types
-- Remove: LEGAL_REPRESENTATIVE_OF, PARENT_OF, CHILD_OF
-- Rename: EMPLOYEE_OF -> WORKS_FOR
-- Add: OTHER
-- ---------------------------------------------------------------------------
-- First update any existing EMPLOYEE_OF rows to WORKS_FOR
UPDATE contact_relationships SET relationship_type = 'WORKS_FOR'
WHERE relationship_type = 'EMPLOYEE_OF';

-- Remove rows with types being dropped (safety net for dev)
DELETE FROM contact_relationships WHERE relationship_type IN (
    'LEGAL_REPRESENTATIVE_OF', 'PARENT_OF', 'CHILD_OF'
);

ALTER TABLE contact_relationships DROP CONSTRAINT IF EXISTS chk_contact_relationships_type;
ALTER TABLE contact_relationships ADD CONSTRAINT chk_contact_relationships_type CHECK (
    relationship_type IN (
        'WORKS_FOR', 'CONTACT_PERSON_FOR', 'GUARANTOR_FOR',
        'FAMILY_OF', 'PARTNER_OF', 'OTHER'
    )
);

-- ---------------------------------------------------------------------------
-- 3. Add ON DELETE CASCADE to contact_tags (matches contact_notes pattern)
-- contact_tags has no deleted_at (no soft delete), so CASCADE is the
-- correct cleanup strategy for exceptional hard deletes (GDPR purge).
-- ---------------------------------------------------------------------------
ALTER TABLE contact_tags DROP CONSTRAINT IF EXISTS contact_tags_contact_id_fkey;
ALTER TABLE contact_tags ADD CONSTRAINT contact_tags_contact_id_fkey
    FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 4. Add pinned column to contact_notes (for Activity tab pinned section)
-- ---------------------------------------------------------------------------
ALTER TABLE contact_notes ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_contact_notes_pinned ON contact_notes (contact_id, pinned)
    WHERE pinned = TRUE AND deleted_at IS NULL;
