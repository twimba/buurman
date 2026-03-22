-- =============================================================================
-- V038__contacts_rework.sql
-- Rework tenants -> contacts: rename tables, add new columns, create new tables
-- =============================================================================
-- ---------------------------------------------------------------------------
-- 1. Rename tenants -> contacts
-- ---------------------------------------------------------------------------
ALTER TABLE tenants
RENAME TO contacts;

-- Add new columns to contacts
ALTER TABLE contacts
ADD COLUMN contact_type VARCHAR(30) NOT NULL DEFAULT 'INDIVIDUAL',
ADD COLUMN display_name VARCHAR(510) NOT NULL DEFAULT '',
ADD COLUMN company_name VARCHAR(255),
ADD COLUMN trade_name VARCHAR(255),
ADD COLUMN industry VARCHAR(100),
ADD COLUMN website VARCHAR(500),
ADD COLUMN invoice_email VARCHAR(255),
ADD COLUMN date_of_birth DATE,
ADD COLUMN id_expiry_date DATE,
ADD COLUMN notes TEXT,
ADD COLUMN data_retention_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Drop current_property_id column and its index
DROP INDEX IF EXISTS idx_tenants_property;

ALTER TABLE contacts
DROP COLUMN IF EXISTS current_property_id;

-- Rename constraint
ALTER TABLE contacts
RENAME CONSTRAINT uq_tenants_team_identifier TO uq_contacts_team_identifier;

-- Rename check constraint
ALTER TABLE contacts
DROP CONSTRAINT IF EXISTS chk_tenants_phone_e164;

ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_phone_e164 CHECK (
    phone IS NULL
    OR phone ~ '^\+[1-9]\d{1,14}$'
);

-- Add new check constraints
ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_contact_type CHECK (
    contact_type IN ('INDIVIDUAL', 'COMPANY', 'SERVICE_PROVIDER')
);

ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_data_retention_status CHECK (
    data_retention_status IN ('ACTIVE', 'RETENTION_REQUESTED', 'ANONYMIZED')
);

ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_invoice_email CHECK (
    invoice_email IS NULL
    OR invoice_email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'
);

-- -------------------------------------------------------------------------
-- Gap 6 (additional): first_name nullable for COMPANY/SERVICE_PROVIDER
-- INDIVIDUAL requires first_name; COMPANY/SERVICE_PROVIDER requires company_name
-- -------------------------------------------------------------------------
ALTER TABLE contacts
ALTER COLUMN first_name
DROP NOT NULL;

ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_name_by_type CHECK (
    (
        contact_type = 'INDIVIDUAL'
        AND first_name IS NOT NULL
        AND first_name != ''
    )
    OR (
        contact_type IN ('COMPANY', 'SERVICE_PROVIDER')
        AND company_name IS NOT NULL
        AND company_name != ''
    )
);

-- Compute display_name for existing rows (all existing are INDIVIDUAL)
UPDATE contacts
SET
    display_name = CASE
        WHEN last_name IS NOT NULL
        AND last_name != '' THEN first_name || ' ' || last_name
        ELSE first_name
    END;

-- -------------------------------------------------------------------------
-- Gap 7 (additional): display_name must never be empty after computation
-- -------------------------------------------------------------------------
ALTER TABLE contacts
ADD CONSTRAINT chk_contacts_display_name_not_empty CHECK (
    display_name IS NOT NULL
    AND display_name != ''
);

-- Rename indexes
ALTER INDEX idx_tenants_team
RENAME TO idx_contacts_team;

ALTER INDEX idx_tenants_email
RENAME TO idx_contacts_email;

ALTER INDEX idx_tenants_team_email_unique
RENAME TO idx_contacts_team_email_unique;

-- New indexes
CREATE INDEX idx_contacts_contact_type ON contacts (team_id, contact_type)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contacts_display_name ON contacts (team_id, display_name)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contacts_company_name ON contacts (team_id, company_name)
WHERE
    company_name IS NOT NULL
    AND deleted_at IS NULL;

CREATE INDEX idx_contacts_phone ON contacts (team_id, phone)
WHERE
    phone IS NOT NULL
    AND deleted_at IS NULL;

CREATE INDEX idx_contacts_tax_number ON contacts (team_id, tax_number)
WHERE
    tax_number IS NOT NULL
    AND deleted_at IS NULL;

CREATE INDEX idx_contacts_id_number ON contacts (team_id, id_number)
WHERE
    id_number IS NOT NULL
    AND deleted_at IS NULL;

-- Backfill identifiers: TEN -> CTC
UPDATE contacts
SET
    identifier = 'CTC' || substring(
        identifier::TEXT
        FROM
            4
    )
WHERE
    identifier::TEXT LIKE 'TEN%';

-- ---------------------------------------------------------------------------
-- 2. Rename tenant_addresses -> contact_addresses
-- ---------------------------------------------------------------------------
ALTER TABLE tenant_addresses
RENAME TO contact_addresses;

ALTER TABLE contact_addresses
RENAME COLUMN tenant_id TO contact_id;

-- Add new address types for companies
ALTER TABLE contact_addresses
DROP CONSTRAINT IF EXISTS tenant_addresses_address_type_check;

ALTER TABLE contact_addresses
ADD CONSTRAINT chk_contact_addresses_address_type CHECK (
    address_type IN (
        'CURRENT',
        'MAILING',
        'RELATIVE',
        'WORK',
        'HISTORIC',
        'REGISTERED_OFFICE',
        'BRANCH'
    )
);

-- Rename indexes
ALTER INDEX idx_tenant_addresses_team_identifier
RENAME TO idx_contact_addresses_team_identifier;

ALTER INDEX idx_tenant_addresses_tenant_id
RENAME TO idx_contact_addresses_contact_id;

ALTER INDEX idx_tenant_addresses_team_id
RENAME TO idx_contact_addresses_team_id;

ALTER INDEX idx_tenant_addresses_status
RENAME TO idx_contact_addresses_status;

ALTER INDEX idx_tenant_addresses_type
RENAME TO idx_contact_addresses_type;

ALTER INDEX idx_tenant_addresses_deleted_at
RENAME TO idx_contact_addresses_deleted_at;

ALTER INDEX idx_tenant_addresses_coordinates
RENAME TO idx_contact_addresses_coordinates;

ALTER INDEX idx_tenant_addresses_unique_current_active
RENAME TO idx_contact_addresses_unique_current_active;

-- Backfill address identifiers: TAD -> CAD
UPDATE contact_addresses
SET
    identifier = 'CAD' || substring(
        identifier::TEXT
        FROM
            4
    )
WHERE
    identifier::TEXT LIKE 'TAD%';

-- ---------------------------------------------------------------------------
-- 3. Rename property_tenant_history -> property_contact_history
-- ---------------------------------------------------------------------------
ALTER TABLE property_tenant_history
RENAME TO property_contact_history;

ALTER TABLE property_contact_history
RENAME COLUMN tenant_id TO contact_id;

-- Rename indexes
ALTER INDEX idx_tenant_history_tenant
RENAME TO idx_contact_history_contact;

ALTER INDEX idx_tenant_history_property
RENAME TO idx_contact_history_property;

ALTER INDEX idx_tenant_history_team
RENAME TO idx_contact_history_team;

-- ---------------------------------------------------------------------------
-- 4. Update contract_parties: rename tenant_id -> contact_id, add new roles
-- ---------------------------------------------------------------------------
ALTER TABLE contract_parties
RENAME COLUMN tenant_id TO contact_id;

-- -------------------------------------------------------------------------
-- Gap 2: Rename unique index (cheaper than drop+create). Use DO block to
-- handle the case where the index might not exist under the old name.
-- -------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'uq_contract_parties_contract_tenant') THEN
        ALTER INDEX uq_contract_parties_contract_tenant RENAME TO uq_contract_parties_contract_contact;
    END IF;
END $$;

ALTER INDEX idx_contract_parties_tenant_id
RENAME TO idx_contract_parties_contact_id;

-- Expand CHECK constraint for roles
ALTER TABLE contract_parties
DROP CONSTRAINT IF EXISTS contract_parties_role_check;

ALTER TABLE contract_parties
ADD CONSTRAINT chk_contract_parties_role CHECK (
    role IN (
        'PRIMARY_TENANT',
        'GUARANTOR',
        'COSIGNER',
        'EXTRA_TENANT',
        'SIGNER',
        'CORPORATE_TENANT',
        'AUTHORIZED_REPRESENTATIVE'
    )
);

-- -------------------------------------------------------------------------
-- Gap 4: contract_parties.contact_id stays NOT NULL.
-- Decision: All contract party roles (including SIGNER) must reference a
-- contact that exists in the system. The Optional<UUID> in the domain is a
-- pre-existing inconsistency to be fixed separately.
-- -------------------------------------------------------------------------
-- ---------------------------------------------------------------------------
-- 5. Update notifications: rename recipient_tenant_id -> recipient_contact_id
-- ---------------------------------------------------------------------------
ALTER TABLE notifications
RENAME COLUMN recipient_tenant_id TO recipient_contact_id;

ALTER INDEX idx_notifications_recipient_tenant
RENAME TO idx_notifications_recipient_contact;

-- ---------------------------------------------------------------------------
-- 6. Update calendar_feeds: rename tenant_id -> contact_id
-- ---------------------------------------------------------------------------
ALTER TABLE calendar_feeds
RENAME COLUMN tenant_id TO contact_id;

ALTER INDEX idx_calendar_feeds_tenant_id
RENAME TO idx_calendar_feeds_contact_id;

-- -------------------------------------------------------------------------
-- Gap 1: Drop and recreate CHECK constraint with contact_id.
-- PostgreSQL ALTER RENAME COLUMN does NOT auto-update CHECK constraint text,
-- so we must drop and recreate it referencing the new column name.
-- -------------------------------------------------------------------------
ALTER TABLE calendar_feeds
DROP CONSTRAINT IF EXISTS chk_calendar_feeds_entity_required;

ALTER TABLE calendar_feeds
ADD CONSTRAINT chk_calendar_feeds_entity_required CHECK (
    (
        feed_type = 'CONTRACT'
        AND contract_id IS NOT NULL
        AND property_id IS NULL
        AND contact_id IS NULL
    )
    OR (
        feed_type = 'PROPERTY_PAYMENTS'
        AND property_id IS NOT NULL
        AND contract_id IS NULL
        AND contact_id IS NULL
    )
    OR (
        feed_type = 'TENANT_PAYMENTS'
        AND contact_id IS NOT NULL
        AND contract_id IS NULL
        AND property_id IS NULL
    )
    OR (
        feed_type = 'ALL_PAYMENTS'
        AND contract_id IS NULL
        AND property_id IS NULL
        AND contact_id IS NULL
    )
);

-- ---------------------------------------------------------------------------
-- 7. New table: contact_notes (timeline/interaction entries)
-- ---------------------------------------------------------------------------
-- Gap 3: contact_id FK uses ON DELETE CASCADE. In the soft-delete model,
-- contacts are never hard-deleted in normal operation. CASCADE is a safety
-- net for exceptional hard deletes (e.g., GDPR data purge) to avoid orphans.
CREATE TABLE contact_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contact_id UUID NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    interaction_type VARCHAR(30) NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT now(),
    follow_up_date DATE,
    follow_up_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_contact_notes_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contact_notes_interaction_type CHECK (
        interaction_type IN (
            'PHONE_CALL',
            'MEETING',
            'VIEWING',
            'KEY_HANDOVER',
            'INSPECTION',
            'NOTE',
            'OTHER'
        )
    )
);

CREATE INDEX idx_contact_notes_contact ON contact_notes (contact_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contact_notes_team ON contact_notes (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contact_notes_occurred_at ON contact_notes (occurred_at DESC)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contact_notes_follow_up ON contact_notes (follow_up_date)
WHERE
    follow_up_date IS NOT NULL
    AND follow_up_reminder_sent = FALSE
    AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 8. New table: contact_relationships
-- ---------------------------------------------------------------------------
-- Gap 3: source_contact_id and target_contact_id FKs use ON DELETE CASCADE.
-- Same rationale as contact_notes: soft-delete model means hard delete is
-- exceptional (GDPR purge). CASCADE prevents orphaned relationship rows.
CREATE TABLE contact_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    source_contact_id UUID NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    target_contact_id UUID NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    relationship_type VARCHAR(40) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_contact_relationships_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_contact_relationships_no_self CHECK (source_contact_id != target_contact_id),
    CONSTRAINT chk_contact_relationships_type CHECK (
        relationship_type IN (
            'EMPLOYEE_OF',
            'CONTACT_PERSON_FOR',
            'LEGAL_REPRESENTATIVE_OF',
            'GUARANTOR_FOR',
            'FAMILY_OF',
            'PARTNER_OF',
            'PARENT_OF',
            'CHILD_OF'
        )
    ),
    CONSTRAINT uq_contact_relationships_pair UNIQUE (
        team_id,
        source_contact_id,
        target_contact_id,
        relationship_type
    )
);

CREATE INDEX idx_contact_relationships_source ON contact_relationships (source_contact_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contact_relationships_target ON contact_relationships (target_contact_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_contact_relationships_team ON contact_relationships (team_id)
WHERE
    deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 9. New table: contact_tags
-- ---------------------------------------------------------------------------
CREATE TABLE contact_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id),
    contact_id UUID NOT NULL REFERENCES contacts (id),
    tag VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users (id),
    CONSTRAINT uq_contact_tags_contact_tag UNIQUE (contact_id, tag),
    CONSTRAINT chk_contact_tags_tag CHECK (
        tag IN (
            'VIP',
            'PROSPECT',
            'PROBLEM',
            'LONG_TERM',
            'COMMERCIAL',
            'RESIDENTIAL',
            'LATE_PAYER',
            'REFERRED',
            'PREFERRED_VENDOR',
            'DO_NOT_CONTACT'
        )
    )
);

CREATE INDEX idx_contact_tags_contact ON contact_tags (contact_id);

CREATE INDEX idx_contact_tags_team ON contact_tags (team_id);

CREATE INDEX idx_contact_tags_tag ON contact_tags (team_id, tag);

-- ---------------------------------------------------------------------------
-- 10. Migrate additional_info to contact_notes for existing rows
-- ---------------------------------------------------------------------------
-- Migration-only identifiers: 'CNT' + 26 uppercase hex chars from UUID.
-- Uses replace(gen_random_uuid()::text, '-', '') to get 32 hex chars without
-- requiring pgcrypto extension. All new records use SidGenerator at runtime.
INSERT INTO
    contact_notes (
        id,
        identifier,
        team_id,
        contact_id,
        interaction_type,
        subject,
        body,
        occurred_at,
        created_at,
        updated_at,
        created_by,
        updated_by
    )
SELECT
    gen_random_uuid(),
    'CNT' || upper(
        substring(
            replace(gen_random_uuid()::TEXT, '-', '')
            FROM
                1 FOR 26
        )
    ),
    c.team_id,
    c.id,
    'NOTE',
    'Migrated notes',
    c.additional_info,
    c.created_at,
    c.created_at,
    c.updated_at,
    c.created_by,
    c.updated_by
FROM
    contacts c
WHERE
    c.additional_info IS NOT NULL
    AND c.additional_info != ''
    AND c.deleted_at IS NULL;

-- Drop additional_info column after migration
ALTER TABLE contacts
DROP COLUMN IF EXISTS additional_info;
