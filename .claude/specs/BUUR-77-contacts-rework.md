# BUUR-77: Contacts Rework — Final Implementation Spec

**Status**: FINAL — Ready for implementation
**Date**: 2026-03-21
**Branch**: `luissantos/buur-77-rework-the-tenants-feature`
**Next migration**: V038 (current latest is V037)

---

## Table of Contents

1. [Database Migration](#1-database-migration)
2. [New/Modified Enums](#2-newmodified-enums)
3. [Domain Classes](#3-domain-classes)
4. [DTOs](#4-dtos)
5. [Repository Layer](#5-repository-layer)
6. [Service Layer](#6-service-layer)
7. [Controller / API Endpoints](#7-controller--api-endpoints)
8. [OpenAPI Changes](#8-openapi-changes)
9. [Frontend Changes](#9-frontend-changes)
10. [Follow-up Issues](#10-follow-up-issues)

---

## 1. Database Migration

**File**: `backend/jooq/src/main/resources/db/migration/V038__contacts_rework.sql`

```sql
-- =============================================================================
-- V038__contacts_rework.sql
-- Rework tenants → contacts: rename tables, add new columns, create new tables
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Rename tenants → contacts
-- ---------------------------------------------------------------------------
ALTER TABLE tenants RENAME TO contacts;

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
    ADD COLUMN keycloak_user_id UUID,
    ADD COLUMN data_retention_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

-- Drop current_property_id column and its index
DROP INDEX IF EXISTS idx_tenants_property;
ALTER TABLE contacts DROP COLUMN IF EXISTS current_property_id;

-- Rename constraint
ALTER TABLE contacts RENAME CONSTRAINT uq_tenants_team_identifier TO uq_contacts_team_identifier;

-- Rename check constraint
ALTER TABLE contacts DROP CONSTRAINT IF EXISTS chk_tenants_phone_e164;
ALTER TABLE contacts ADD CONSTRAINT chk_contacts_phone_e164 CHECK (
    phone IS NULL
    OR phone ~ '^\+[1-9]\d{1,14}$'
);

-- Add new check constraints
ALTER TABLE contacts ADD CONSTRAINT chk_contacts_contact_type CHECK (
    contact_type IN ('INDIVIDUAL', 'COMPANY', 'SERVICE_PROVIDER')
);

ALTER TABLE contacts ADD CONSTRAINT chk_contacts_data_retention_status CHECK (
    data_retention_status IN ('ACTIVE', 'RETENTION_REQUESTED', 'ANONYMIZED')
);

ALTER TABLE contacts ADD CONSTRAINT chk_contacts_invoice_email CHECK (
    invoice_email IS NULL
    OR invoice_email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'
);

-- Compute display_name for existing rows
UPDATE contacts
SET display_name = CASE
    WHEN last_name IS NOT NULL AND last_name != '' THEN first_name || ' ' || last_name
    ELSE first_name
END;

-- Rename indexes
ALTER INDEX idx_tenants_team RENAME TO idx_contacts_team;
ALTER INDEX idx_tenants_email RENAME TO idx_contacts_email;
ALTER INDEX idx_tenants_team_email_unique RENAME TO idx_contacts_team_email_unique;

-- New indexes
CREATE INDEX idx_contacts_contact_type ON contacts (team_id, contact_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_display_name ON contacts (team_id, display_name) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_keycloak_user ON contacts (keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
CREATE INDEX idx_contacts_company_name ON contacts (team_id, company_name) WHERE company_name IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_contacts_phone ON contacts (team_id, phone) WHERE phone IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_contacts_tax_number ON contacts (team_id, tax_number) WHERE tax_number IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_contacts_id_number ON contacts (team_id, id_number) WHERE id_number IS NOT NULL AND deleted_at IS NULL;

-- Backfill identifiers: TEN → CTC
UPDATE contacts
SET identifier = 'CTC' || substring(identifier::text FROM 4)
WHERE identifier::text LIKE 'TEN%';

-- ---------------------------------------------------------------------------
-- 2. Rename tenant_addresses → contact_addresses
-- ---------------------------------------------------------------------------
ALTER TABLE tenant_addresses RENAME TO contact_addresses;
ALTER TABLE contact_addresses RENAME COLUMN tenant_id TO contact_id;

-- Add new address types for companies
ALTER TABLE contact_addresses DROP CONSTRAINT IF EXISTS tenant_addresses_address_type_check;
ALTER TABLE contact_addresses ADD CONSTRAINT chk_contact_addresses_address_type CHECK (
    address_type IN ('CURRENT', 'MAILING', 'RELATIVE', 'WORK', 'HISTORIC', 'REGISTERED_OFFICE', 'BRANCH')
);

-- Rename indexes
ALTER INDEX idx_tenant_addresses_team_identifier RENAME TO idx_contact_addresses_team_identifier;
ALTER INDEX idx_tenant_addresses_tenant_id RENAME TO idx_contact_addresses_contact_id;
ALTER INDEX idx_tenant_addresses_team_id RENAME TO idx_contact_addresses_team_id;
ALTER INDEX idx_tenant_addresses_status RENAME TO idx_contact_addresses_status;
ALTER INDEX idx_tenant_addresses_type RENAME TO idx_contact_addresses_type;
ALTER INDEX idx_tenant_addresses_deleted_at RENAME TO idx_contact_addresses_deleted_at;
ALTER INDEX idx_tenant_addresses_coordinates RENAME TO idx_contact_addresses_coordinates;
ALTER INDEX idx_tenant_addresses_unique_current_active RENAME TO idx_contact_addresses_unique_current_active;

-- Backfill address identifiers: TAD → CAD
UPDATE contact_addresses
SET identifier = 'CAD' || substring(identifier::text FROM 4)
WHERE identifier::text LIKE 'TAD%';

-- ---------------------------------------------------------------------------
-- 3. Rename property_tenant_history → property_contact_history
-- ---------------------------------------------------------------------------
ALTER TABLE property_tenant_history RENAME TO property_contact_history;
ALTER TABLE property_contact_history RENAME COLUMN tenant_id TO contact_id;

-- Rename indexes
ALTER INDEX idx_tenant_history_tenant RENAME TO idx_contact_history_contact;
ALTER INDEX idx_tenant_history_property RENAME TO idx_contact_history_property;
ALTER INDEX idx_tenant_history_team RENAME TO idx_contact_history_team;

-- ---------------------------------------------------------------------------
-- 4. Update contract_parties: rename tenant_id → contact_id, add new roles
-- ---------------------------------------------------------------------------
ALTER TABLE contract_parties RENAME COLUMN tenant_id TO contact_id;

-- Rename unique index for contract-contact
ALTER INDEX uq_contract_parties_contract_tenant RENAME TO uq_contract_parties_contract_contact;
ALTER INDEX idx_contract_parties_tenant_id RENAME TO idx_contract_parties_contact_id;

-- Expand CHECK constraint for roles
ALTER TABLE contract_parties DROP CONSTRAINT IF EXISTS contract_parties_role_check;
ALTER TABLE contract_parties ADD CONSTRAINT chk_contract_parties_role CHECK (
    role IN (
        'PRIMARY_TENANT', 'GUARANTOR', 'COSIGNER', 'EXTRA_TENANT',
        'SIGNER', 'CORPORATE_TENANT', 'AUTHORIZED_REPRESENTATIVE'
    )
);

-- ---------------------------------------------------------------------------
-- 5. Update notifications: rename recipient_tenant_id → recipient_contact_id
-- ---------------------------------------------------------------------------
ALTER TABLE notifications RENAME COLUMN recipient_tenant_id TO recipient_contact_id;
ALTER INDEX idx_notifications_recipient_tenant RENAME TO idx_notifications_recipient_contact;

-- ---------------------------------------------------------------------------
-- 6. Update calendar_feeds: rename tenant_id → contact_id
-- ---------------------------------------------------------------------------
ALTER TABLE calendar_feeds RENAME COLUMN tenant_id TO contact_id;
ALTER INDEX idx_calendar_feeds_tenant_id RENAME TO idx_calendar_feeds_contact_id;

-- Update the CHECK constraint to reference contact_id instead of tenant_id
ALTER TABLE calendar_feeds DROP CONSTRAINT IF EXISTS chk_calendar_feeds_entity_required;
ALTER TABLE calendar_feeds ADD CONSTRAINT chk_calendar_feeds_entity_required CHECK (
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
CREATE TABLE contact_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contact_id UUID NOT NULL REFERENCES contacts (id),
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
            'PHONE_CALL', 'MEETING', 'VIEWING', 'KEY_HANDOVER',
            'INSPECTION', 'NOTE', 'OTHER'
        )
    )
);

CREATE INDEX idx_contact_notes_contact ON contact_notes (contact_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contact_notes_team ON contact_notes (team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contact_notes_occurred_at ON contact_notes (occurred_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_contact_notes_follow_up ON contact_notes (follow_up_date)
    WHERE follow_up_date IS NOT NULL AND follow_up_reminder_sent = FALSE AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 8. New table: contact_relationships
-- ---------------------------------------------------------------------------
CREATE TABLE contact_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    source_contact_id UUID NOT NULL REFERENCES contacts (id),
    target_contact_id UUID NOT NULL REFERENCES contacts (id),
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
            'EMPLOYEE_OF', 'CONTACT_PERSON_FOR', 'LEGAL_REPRESENTATIVE_OF',
            'GUARANTOR_FOR', 'FAMILY_OF', 'PARTNER_OF', 'PARENT_OF', 'CHILD_OF'
        )
    ),
    CONSTRAINT uq_contact_relationships_pair UNIQUE (
        team_id, source_contact_id, target_contact_id, relationship_type
    )
);

CREATE INDEX idx_contact_relationships_source ON contact_relationships (source_contact_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contact_relationships_target ON contact_relationships (target_contact_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contact_relationships_team ON contact_relationships (team_id) WHERE deleted_at IS NULL;

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
            'VIP', 'PROSPECT', 'PROBLEM', 'LONG_TERM',
            'COMMERCIAL', 'RESIDENTIAL', 'LATE_PAYER',
            'REFERRED', 'PREFERRED_VENDOR', 'DO_NOT_CONTACT'
        )
    )
);

CREATE INDEX idx_contact_tags_contact ON contact_tags (contact_id);
CREATE INDEX idx_contact_tags_team ON contact_tags (team_id);
CREATE INDEX idx_contact_tags_tag ON contact_tags (team_id, tag);

-- ---------------------------------------------------------------------------
-- 10. Migrate additional_info to contact_notes for existing rows
-- ---------------------------------------------------------------------------
INSERT INTO contact_notes (id, identifier, team_id, contact_id, interaction_type, subject, body, occurred_at, created_at, updated_at, created_by, updated_by)
SELECT
    gen_random_uuid(),
    'CNT' || substring(gen_random_uuid()::text, 1, 26),
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
FROM contacts c
WHERE c.additional_info IS NOT NULL AND c.additional_info != '' AND c.deleted_at IS NULL;

-- Drop additional_info column after migration
ALTER TABLE contacts DROP COLUMN IF EXISTS additional_info;
```

### Tables affected by FK renames (summary)

| Table | Column renamed | From | To |
|-------|---------------|------|-----|
| `contacts` (was `tenants`) | n/a (table rename) | `tenants` | `contacts` |
| `contact_addresses` (was `tenant_addresses`) | `tenant_id` | `tenant_id` | `contact_id` |
| `property_contact_history` (was `property_tenant_history`) | `tenant_id` | `tenant_id` | `contact_id` |
| `contract_parties` | `tenant_id` | `tenant_id` | `contact_id` |
| `notifications` | `recipient_tenant_id` | `recipient_tenant_id` | `recipient_contact_id` |
| `calendar_feeds` | `tenant_id` | `tenant_id` | `contact_id` |

### Identifier prefix changes

| Old prefix | New prefix | Entity |
|-----------|-----------|--------|
| `TEN` | `CTC` | Contacts |
| `TAD` | `CAD` | Contact Addresses |
| (new) | `CNT` | Contact Notes |
| (new) | `CRL` | Contact Relationships |

---

## 2. New/Modified Enums

### New enums

**`ContactType`** (`com.buurman.domain.ContactType`)
```java
@Getter
@RequiredArgsConstructor
public enum ContactType {
    INDIVIDUAL("Individual"),
    COMPANY("Company"),
    SERVICE_PROVIDER("Service Provider");

    private final String displayName;
}
```

**`InteractionType`** (`com.buurman.domain.InteractionType`)
```java
@Getter
@RequiredArgsConstructor
public enum InteractionType {
    PHONE_CALL("Phone Call"),
    MEETING("Meeting"),
    VIEWING("Viewing"),
    KEY_HANDOVER("Key Handover"),
    INSPECTION("Inspection"),
    NOTE("Note"),
    OTHER("Other");

    private final String displayName;
}
```

**`RelationshipType`** (`com.buurman.domain.RelationshipType`)
```java
@Getter
@RequiredArgsConstructor
public enum RelationshipType {
    EMPLOYEE_OF("Employee of"),
    CONTACT_PERSON_FOR("Contact person for"),
    LEGAL_REPRESENTATIVE_OF("Legal representative of"),
    GUARANTOR_FOR("Guarantor for"),
    FAMILY_OF("Family of"),
    PARTNER_OF("Partner of"),
    PARENT_OF("Parent of"),
    CHILD_OF("Child of");

    private final String displayName;

    /**
     * Returns the inverse relationship type, or empty if the relationship is symmetric.
     */
    public Optional<RelationshipType> inverse() {
        return switch (this) {
            case EMPLOYEE_OF -> Optional.of(CONTACT_PERSON_FOR);
            case CONTACT_PERSON_FOR -> Optional.of(EMPLOYEE_OF);
            case LEGAL_REPRESENTATIVE_OF -> Optional.empty(); // no standard inverse
            case GUARANTOR_FOR -> Optional.empty();
            case FAMILY_OF -> Optional.of(FAMILY_OF);
            case PARTNER_OF -> Optional.of(PARTNER_OF);
            case PARENT_OF -> Optional.of(CHILD_OF);
            case CHILD_OF -> Optional.of(PARENT_OF);
        };
    }
}
```

**`ContactTag`** (`com.buurman.domain.ContactTag`)
```java
@Getter
@RequiredArgsConstructor
public enum ContactTag {
    VIP("VIP"),
    PROSPECT("Prospect"),
    PROBLEM("Problem"),
    LONG_TERM("Long-term"),
    COMMERCIAL("Commercial"),
    RESIDENTIAL("Residential"),
    LATE_PAYER("Late Payer"),
    REFERRED("Referred"),
    PREFERRED_VENDOR("Preferred Vendor"),
    DO_NOT_CONTACT("Do Not Contact");

    private final String displayName;
}
```

**`DataRetentionStatus`** (`com.buurman.domain.DataRetentionStatus`)
```java
public enum DataRetentionStatus {
    ACTIVE,
    RETENTION_REQUESTED,
    ANONYMIZED
}
```

### Modified enums

**`ContractPartyRole`** — add 3 new values:
```java
@Getter
@RequiredArgsConstructor
public enum ContractPartyRole {
    PRIMARY_TENANT("Primary tenant"),
    GUARANTOR("Guarantor"),
    COSIGNER("Co-signer"),
    EXTRA_TENANT("Additional tenant"),
    SIGNER("Signer"),
    CORPORATE_TENANT("Corporate tenant"),
    AUTHORIZED_REPRESENTATIVE("Authorized representative");

    private final String displayName;
}
```

**`EntityPrefix`** — replace TEN/TAD, add CNT/CRL:
```java
// Remove:
TEN("TEN", "Tenants"),
TAD("TAD", "Tenant Addresses"),

// Add:
CTC("CTC", "Contacts"),
CAD("CAD", "Contact Addresses"),
CNT("CNT", "Contact Notes"),
CRL("CRL", "Contact Relationships"),
```

**`AuditEntityType`** — rename TENANT:
```java
// Replace TENANT with CONTACT
CONTACT,
CONTACT_ADDRESS,   // was implicit via audit string "TENANT_ADDRESS"
CONTACT_NOTE,
CONTACT_RELATIONSHIP,
```

**`TenantAddress.AddressType`** → `ContactAddress.AddressType` — add 2 values:
```java
public enum AddressType {
    CURRENT,
    MAILING,
    RELATIVE,
    WORK,
    HISTORIC,
    REGISTERED_OFFICE,
    BRANCH
}
```

---

## 3. Domain Classes

### Renamed/Modified

**`Tenant.java` → `Contact.java`** (`com.buurman.domain.Contact`)

```java
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    private UUID id;
    @Builder.Default private Optional<Sid> identifier = Optional.empty();
    private UUID teamId;
    private ContactType contactType;
    private String firstName;
    @Builder.Default private Optional<String> lastName = Optional.empty();
    private String displayName;
    @Builder.Default private Optional<String> email = Optional.empty();
    @Builder.Default private Optional<String> phone = Optional.empty();
    @Builder.Default private Optional<String> taxNumber = Optional.empty();
    @Builder.Default private Optional<String> idNumber = Optional.empty();
    @Builder.Default private Optional<LocalDate> idExpiryDate = Optional.empty();
    @Builder.Default private Optional<LocalDate> dateOfBirth = Optional.empty();
    // Company-specific fields
    @Builder.Default private Optional<String> companyName = Optional.empty();
    @Builder.Default private Optional<String> tradeName = Optional.empty();
    @Builder.Default private Optional<String> industry = Optional.empty();
    @Builder.Default private Optional<String> website = Optional.empty();
    @Builder.Default private Optional<String> invoiceEmail = Optional.empty();
    // Rich text notes (long-form, for anything extra)
    @Builder.Default private Optional<String> notes = Optional.empty();
    // Future tenant portal
    @Builder.Default private Optional<UUID> keycloakUserId = Optional.empty();
    // GDPR
    private DataRetentionStatus dataRetentionStatus;
    // Audit
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

Key changes from `Tenant`:
- Removed `currentPropertyId` (dropped from DB)
- Removed `additionalInfo` (migrated to contact_notes, column dropped)
- Added `contactType`, `displayName`, `companyName`, `tradeName`, `industry`, `website`, `invoiceEmail`, `dateOfBirth`, `idExpiryDate`, `notes`, `keycloakUserId`, `dataRetentionStatus`

**`TenantAddress.java` → `ContactAddress.java`** (`com.buurman.domain.ContactAddress`)

```java
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactAddress {
    // AddressType enum gains REGISTERED_OFFICE, BRANCH
    // AddressStatus enum unchanged
    // Field tenantId → contactId
    private UUID id;
    @Builder.Default private Optional<Sid> identifier = Optional.empty();
    private UUID contactId;   // was tenantId
    private UUID teamId;
    private String street;
    private String city;
    private String postalCode;
    private String countryCode;
    private AddressType addressType;
    private AddressStatus status;
    @Builder.Default private Optional<Double> latitude = Optional.empty();
    @Builder.Default private Optional<Double> longitude = Optional.empty();
    @Builder.Default private Optional<String> geocodeAccuracy = Optional.empty();
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

**`PropertyTenantHistory.java` → `PropertyContactHistory.java`**

Field `tenantId` → `contactId`. Otherwise identical.

**`ContractParty.java`** — field rename only:

`tenantId` → `contactId` (type stays `Optional<UUID>`)

**`Notification.java`** — field rename only:

`recipientTenantId` → `recipientContactId`

**`CalendarFeed.java`** — field rename only:

`tenantId` → `contactId`

### New domain classes

**`ContactNote.java`** (`com.buurman.domain.ContactNote`)

```java
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactNote {
    private UUID id;
    @Builder.Default private Optional<Sid> identifier = Optional.empty();
    private UUID teamId;
    private UUID contactId;
    private InteractionType interactionType;
    @Builder.Default private Optional<String> subject = Optional.empty();
    private String body;   // rich text
    private Instant occurredAt;
    @Builder.Default private Optional<LocalDate> followUpDate = Optional.empty();
    private boolean followUpReminderSent;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

**`ContactRelationship.java`** (`com.buurman.domain.ContactRelationship`)

```java
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRelationship {
    private UUID id;
    @Builder.Default private Optional<Sid> identifier = Optional.empty();
    private UUID teamId;
    private UUID sourceContactId;
    private UUID targetContactId;
    private RelationshipType relationshipType;
    @Builder.Default private Optional<String> notes = Optional.empty();
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

### New typed identifiers

**File**: `com.buurman.domain.identifier`

| Class | Prefix | Replaces |
|-------|--------|----------|
| `ContactIdentifier` | CTC | `TenantIdentifier` |
| `ContactAddressIdentifier` | CAD | `TenantAddressIdentifier` |
| `ContactNoteIdentifier` | CNT | (new) |
| `ContactRelationshipIdentifier` | CRL | (new) |

`TenantIdentifier` and `TenantAddressIdentifier` should be **deleted** after all references are migrated.

Each follows the same pattern:
```java
public final class ContactIdentifier extends Sid {
    private ContactIdentifier(String value) { super(value); }
    @JsonCreator
    public static ContactIdentifier of(String value) { return new ContactIdentifier(value); }
}
```

### SidGenerator changes

Replace:
```java
public static TenantIdentifier newTenantId() { ... }
public static TenantAddressIdentifier newTenantAddressId() { ... }
```

With:
```java
public static ContactIdentifier newContactId() {
    return ContactIdentifier.of(generateRaw(EntityPrefix.CTC));
}
public static ContactAddressIdentifier newContactAddressId() {
    return ContactAddressIdentifier.of(generateRaw(EntityPrefix.CAD));
}
public static ContactNoteIdentifier newContactNoteId() {
    return ContactNoteIdentifier.of(generateRaw(EntityPrefix.CNT));
}
public static ContactRelationshipIdentifier newContactRelationshipId() {
    return ContactRelationshipIdentifier.of(generateRaw(EntityPrefix.CRL));
}
```

---

## 4. DTOs

### Request DTOs (all in `com.buurman.dto.request`)

**`CreateTenantRequest` → `CreateContactRequest`**

```java
public record CreateContactRequest(
    ContactType contactType,                    // required, defaults to INDIVIDUAL in frontend
    @NotBlank String firstName,
    Optional<String> lastName,
    Optional<@Email String> email,
    Optional<@Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String> phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<LocalDate> idExpiryDate,
    Optional<LocalDate> dateOfBirth,
    // Company fields
    Optional<String> companyName,
    Optional<String> tradeName,
    Optional<String> industry,
    Optional<@URL String> website,
    Optional<@Email String> invoiceEmail,
    // Notes (rich text)
    Optional<String> notes,
    // Tags
    Optional<List<ContactTag>> tags
) {}
```

**`UpdateContactRequest`** (replaces `UpdateTenantRequest`)

Same fields as `CreateContactRequest` but `contactType` is also required (non-optional).

**`CreateContactAddressRequest`** (replaces `CreateTenantAddressRequest`)

Identical to `CreateTenantAddressRequest` but references `ContactAddress.AddressType` (which now includes `REGISTERED_OFFICE`, `BRANCH`).

**`UpdateContactAddressRequest`** (replaces `UpdateTenantAddressRequest`)

Same as above.

**`LinkContactToPropertyRequest`** (replaces `LinkTenantToPropertyRequest`)

```java
public record LinkContactToPropertyRequest(
    @NotNull PropertyIdentifier propertyIdentifier,
    Optional<Instant> movedInAt
) {}
```

**`CreateContactNoteRequest`** (new)

```java
public record CreateContactNoteRequest(
    @NotNull InteractionType interactionType,
    Optional<String> subject,
    @NotBlank String body,     // rich text
    Optional<Instant> occurredAt,
    Optional<LocalDate> followUpDate
) {}
```

**`UpdateContactNoteRequest`** (new)

```java
public record UpdateContactNoteRequest(
    @NotNull InteractionType interactionType,
    Optional<String> subject,
    @NotBlank String body,
    Optional<Instant> occurredAt,
    Optional<LocalDate> followUpDate
) {}
```

**`CreateContactRelationshipRequest`** (new)

```java
public record CreateContactRelationshipRequest(
    @NotNull ContactIdentifier targetContactIdentifier,
    @NotNull RelationshipType relationshipType,
    Optional<String> notes
) {}
```

**`UpdateContactRelationshipRequest`** (new)

```java
public record UpdateContactRelationshipRequest(
    @NotNull RelationshipType relationshipType,
    Optional<String> notes
) {}
```

**`AddContactTagRequest`** (new)

```java
public record AddContactTagRequest(
    @NotNull ContactTag tag
) {}
```

**`ChangePrimaryTenantRequest`** — field rename:

`tenantIdentifier` → `contactIdentifier`, `newTenant` → `newContact`, references `CreateContactRequest`

### Response DTOs (all in `com.buurman.dto.response`)

**`TenantResponse` → `ContactResponse`**

```java
public record ContactResponse(
    Sid identifier,
    ContactType contactType,
    String firstName,
    String lastName,               // still nullable in JSON via Optional mapping
    String displayName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<LocalDate> idExpiryDate,
    Optional<LocalDate> dateOfBirth,
    // Company fields
    Optional<String> companyName,
    Optional<String> tradeName,
    Optional<String> industry,
    Optional<String> website,
    Optional<String> invoiceEmail,
    // Notes
    Optional<String> notes,
    // Photos
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,
    // Active property assignments (derived from contracts)
    List<ContactPropertyAssignment> activeProperties,
    // Tags
    List<ContactTag> tags,
    // GDPR
    DataRetentionStatus dataRetentionStatus,
    // Audit
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`TenantSummary` → `ContactSummary`**

```java
public record ContactSummary(
    Sid identifier,
    ContactType contactType,
    String firstName,
    String lastName,
    String displayName,
    Optional<String> email,
    Optional<String> phone
) {}
```

**`TenantPropertyAssignment` → `ContactPropertyAssignment`**

```java
public record ContactPropertyAssignment(
    PropertySummary property,
    Optional<String> role
) {}
```

**`TenantAddressResponse` → `ContactAddressResponse`**

Same fields as `TenantAddressResponse` but references `ContactAddress.AddressType` / `ContactAddress.AddressStatus`.

**`PropertyTenantHistoryResponse` → `PropertyContactHistoryResponse`**

Same structure, same fields (no domain field renames needed in the response — it already used `property`, `movedInAt`, etc.).

**`ContactNoteResponse`** (new)

```java
public record ContactNoteResponse(
    Sid identifier,
    InteractionType interactionType,
    Optional<String> subject,
    String body,
    Instant occurredAt,
    Optional<LocalDate> followUpDate,
    boolean followUpReminderSent,
    String createdByName,         // resolved user name
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`ContactRelationshipResponse`** (new)

```java
public record ContactRelationshipResponse(
    Sid identifier,
    ContactSummary sourceContact,
    ContactSummary targetContact,
    RelationshipType relationshipType,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`ContactTagResponse`** (new — or just use `List<ContactTag>` in ContactResponse)

Tags are embedded directly in `ContactResponse.tags` as `List<ContactTag>`.

**`DuplicateCheckResponse`** (new)

```java
public record DuplicateCheckResponse(
    List<DuplicateMatch> matches
) {}

public record DuplicateMatch(
    ContactSummary contact,
    String matchField,   // "email", "phone", "taxNumber", "idNumber", "name"
    String matchType     // "EXACT" or "FUZZY"
) {}
```

---

## 5. Repository Layer

### Renamed/Modified repositories

**`TenantRepository` → `ContactRepository`**

All methods keep the same signature pattern, but:
- Table reference: `TENANTS` → `CONTACTS`
- Record type: `TenantsRecord` → `ContactsRecord` (JOOQ generated)
- All domain types: `Tenant` → `Contact`
- Remove `findByCurrentPropertyId()` (column dropped)
- Add `findByContactType()` for filtering
- Add new sortable fields: `displayName`, `contactType`, `companyName`
- Search also covers `displayName`, `companyName`

New methods:
```java
Optional<Contact> findByPhoneAndTeamId(String phone, UUID teamId);
PaginatedResult<Contact> findAllByTeamIdPaginated(
    UUID teamId, Optional<String> search, Optional<ContactType> contactType,
    Optional<List<ContactTag>> tags, PageRequest pageRequest);
List<Contact> findDuplicates(UUID teamId, Optional<String> email, Optional<String> phone,
    Optional<String> taxNumber, Optional<String> idNumber, String firstName, Optional<String> lastName);
```

**`TenantAddressRepository` → `ContactAddressRepository`**

- Table: `TENANT_ADDRESSES` → `CONTACT_ADDRESSES`
- Field: `TENANT_ID` → `CONTACT_ID`
- Domain: `TenantAddress` → `ContactAddress`
- Method params: `tenantId` → `contactId`
- **Fix**: UPDATE now includes `team_id` in WHERE clause (known bug from review)

**`PropertyTenantHistoryRepository` → `PropertyContactHistoryRepository`**

- Table: `PROPERTY_TENANT_HISTORY` → `PROPERTY_CONTACT_HISTORY`
- Field: `TENANT_ID` → `CONTACT_ID`
- Domain: `PropertyTenantHistory` → `PropertyContactHistory`
- Methods: `findByTenantId` → `findByContactId`

**`ContractPartyRepository`** — field renames only:

- `TENANT_ID` field → `CONTACT_ID`
- All method names: `tenantId` param → `contactId`
- `findByTenantIdAndContractIdAndTeamId` → `findByContactIdAndContractIdAndTeamId`
- `existsByContractIdAndTenantIdAndTeamId` → `existsByContractIdAndContactIdAndTeamId`

### New repositories

**`ContactNoteRepository`** (`com.buurman.repository.ContactNoteRepository`)

```java
@Repository
@RequiredArgsConstructor
public class ContactNoteRepository {
    private final DSLContext dsl;
    private final Clock clock;

    ContactNote save(ContactNote note);
    List<ContactNote> findByContactIdAndTeamId(UUID contactId, UUID teamId);
    Optional<ContactNote> findByIdentifierAndTeamId(Sid identifier, UUID teamId);
    ContactNote getByIdentifierAndTeamId(Sid identifier, UUID teamId);
    void softDeleteByIdAndTeamId(UUID id, UUID teamId);
    List<ContactNote> findPendingFollowUps(LocalDate date);  // for Quartz job
}
```

**`ContactRelationshipRepository`** (`com.buurman.repository.ContactRelationshipRepository`)

```java
@Repository
@RequiredArgsConstructor
public class ContactRelationshipRepository {
    private final DSLContext dsl;
    private final Clock clock;

    ContactRelationship save(ContactRelationship relationship);
    List<ContactRelationship> findByContactIdAndTeamId(UUID contactId, UUID teamId);  // source OR target
    Optional<ContactRelationship> findByIdentifierAndTeamId(Sid identifier, UUID teamId);
    ContactRelationship getByIdentifierAndTeamId(Sid identifier, UUID teamId);
    void softDeleteByIdAndTeamId(UUID id, UUID teamId);
    boolean existsByPairAndType(UUID teamId, UUID sourceId, UUID targetId, RelationshipType type);
}
```

**`ContactTagRepository`** (`com.buurman.repository.ContactTagRepository`)

```java
@Repository
@RequiredArgsConstructor
public class ContactTagRepository {
    private final DSLContext dsl;

    void addTag(UUID contactId, UUID teamId, ContactTag tag, UUID createdBy);
    void removeTag(UUID contactId, ContactTag tag);
    List<ContactTag> findByContactId(UUID contactId);
    List<ContactTag> findByContactIds(Collection<UUID> contactIds);  // batch for list views
    Map<UUID, List<ContactTag>> findByContactIdsGrouped(Collection<UUID> contactIds);
}
```

---

## 6. Service Layer

### Renamed/Modified services

**`TenantService` → `ContactService`**

Core CRUD (same pattern, renamed):
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ContactService {

    // Dependencies (renamed)
    private final ContactRepository contactRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyContactHistoryRepository historyRepository;
    private final ContactMapper contactMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final PhotoService photoService;
    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final ContactAddressService addressService;
    private final ContactAddressRepository addressRepository;
    private final ContactNoteService noteService;
    private final ContactRelationshipService relationshipService;
    private final ContactTagRepository tagRepository;
    private final MetricsService metricsService;
    private final Clock clock;

    // --- CRUD ---
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse createContact(CreateContactRequest request, UserPrincipal principal);

    PageResponse<ContactResponse> getContactsPaginated(
        UserPrincipal principal, Optional<String> search, Optional<ContactType> contactType,
        Optional<List<ContactTag>> tags, PageRequest pageRequest);

    ContactResponse getContact(ContactIdentifier identifier, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse updateContact(ContactIdentifier identifier, UpdateContactRequest request, UserPrincipal principal);

    @Transactional @PreAuthorize("hasRole('TEAM_ADMIN')")
    void deleteContact(ContactIdentifier identifier, UserPrincipal principal);

    // --- Property linking (replaces link/unlink tenant) ---
    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse linkContactToProperty(ContactIdentifier identifier, LinkContactToPropertyRequest request, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse unlinkContactFromProperty(ContactIdentifier identifier, UserPrincipal principal);

    // --- History ---
    List<PropertyContactHistoryResponse> getContactHistory(ContactIdentifier identifier, UserPrincipal principal);

    // --- Audit ---
    List<RecentActivityResponse> getAuditLog(ContactIdentifier identifier, UserPrincipal principal);

    // --- Documents/Photos (same pattern as before, entity type "CONTACT") ---
    // uploadDocument, getDocuments, getDownloadUrl, deleteDocument
    // getPhotos, uploadPhoto, setMainPhoto

    // --- Addresses (delegate to ContactAddressService) ---
    // createAddress, getAddresses, getAddress, updateAddress, deleteAddress

    // --- Tags (inline, no separate service) ---
    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse addTag(ContactIdentifier identifier, AddContactTagRequest request, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse removeTag(ContactIdentifier identifier, ContactTag tag, UserPrincipal principal);

    // --- Duplicate check ---
    DuplicateCheckResponse checkDuplicates(CreateContactRequest request, UserPrincipal principal);
}
```

**Business rules for `createContact`**:
1. Compute `display_name`:
   - `COMPANY`/`SERVICE_PROVIDER` with `companyName` present: `companyName` (ignore first/last)
   - Otherwise: `firstName + " " + lastName` (trimmed)
2. Duplicate check: email uniqueness per team (existing), phone uniqueness per team (new)
3. If tags provided, insert into `contact_tags`
4. Increment metrics: `contact.total` (was `tenant.total`)

**Business rules for `updateContact`**:
1. Recompute `display_name` on every update
2. Email uniqueness check (existing pattern)
3. Tags: diffed — remove old tags not in request, add new ones

**`TenantAddressService` → `ContactAddressService`**

Same logic, renamed types. All `tenantId` params → `contactId`. Audit entity type: `"CONTACT_ADDRESS"`.

### New services

**`ContactNoteService`** (`com.buurman.service.ContactNoteService`)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ContactNoteService {

    private final ContactNoteRepository noteRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse createNote(UUID contactId, CreateContactNoteRequest request, UserPrincipal principal);
    // - Sets occurredAt to request.occurredAt or now()
    // - Generates CNT identifier
    // - Stores rich text body as-is

    List<ContactNoteResponse> getNotes(UUID contactId, UserPrincipal principal);
    // - Ordered by occurredAt DESC

    ContactNoteResponse getNote(UUID contactId, UUID noteId, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse updateNote(UUID contactId, UUID noteId, UpdateContactNoteRequest request, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    void deleteNote(UUID contactId, UUID noteId, UserPrincipal principal);
}
```

**`ContactRelationshipService`** (`com.buurman.service.ContactRelationshipService`)

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ContactRelationshipService {

    private final ContactRelationshipRepository relationshipRepository;
    private final ContactRepository contactRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactRelationshipResponse createRelationship(
        UUID sourceContactId, CreateContactRelationshipRequest request, UserPrincipal principal);
    // - Validates both contacts exist and belong to same team
    // - Validates no self-reference
    // - Checks uniqueness of (source, target, type) pair

    List<ContactRelationshipResponse> getRelationships(UUID contactId, UserPrincipal principal);
    // - Returns relationships where contact is source OR target

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactRelationshipResponse updateRelationship(
        UUID contactId, UUID relationshipId, UpdateContactRelationshipRequest request, UserPrincipal principal);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    void deleteRelationship(UUID contactId, UUID relationshipId, UserPrincipal principal);
}
```

### Quartz job (new)

**`ContactFollowUpReminderJob`** — runs daily, queries `contact_notes` with `follow_up_date = today AND follow_up_reminder_sent = FALSE`, creates notifications for the note creator. Marks `follow_up_reminder_sent = TRUE` after sending.

---

## 7. Controller / API Endpoints

**`TenantController` → `ContactController`** — implements `ContactsApi` (generated)

### Endpoint mapping

All paths change from `/tenants/...` to `/contacts/...`.

| Method | Old Path | New Path | Operation | Roles |
|--------|----------|----------|-----------|-------|
| POST | `/tenants` | `/contacts` | createContact | ADMIN, EDITOR |
| GET | `/tenants` | `/contacts` | getContacts | ALL |
| GET | `/tenants/{id}` | `/contacts/{id}` | getContact | ALL |
| PUT | `/tenants/{id}` | `/contacts/{id}` | updateContact | ADMIN, EDITOR |
| DELETE | `/tenants/{id}` | `/contacts/{id}` | deleteContact | ADMIN |
| POST | `/tenants/{id}/link-property` | `/contacts/{id}/link-property` | linkContactToProperty | ADMIN, EDITOR |
| POST | `/tenants/{id}/unlink-property` | `/contacts/{id}/unlink-property` | unlinkContactFromProperty | ADMIN, EDITOR |
| GET | `/tenants/{id}/history` | `/contacts/{id}/history` | getContactHistory | ALL |
| GET | `/tenants/{id}/audit-log` | `/contacts/{id}/audit-log` | getContactAuditLog | ALL |
| POST | `/tenants/{id}/documents` | `/contacts/{id}/documents` | uploadDocument | ADMIN, EDITOR |
| GET | `/tenants/{id}/documents` | `/contacts/{id}/documents` | getDocuments | ALL |
| GET | `/tenants/documents/{docId}/download` | `/contacts/documents/{docId}/download` | getDownloadUrl | ALL |
| DELETE | `/tenants/documents/{docId}` | `/contacts/documents/{docId}` | deleteContactDocument | ADMIN, EDITOR |
| POST | `/tenants/{id}/photos` | `/contacts/{id}/photos` | uploadPhoto | ADMIN, EDITOR |
| GET | `/tenants/{id}/photos` | `/contacts/{id}/photos` | getPhotos | ALL |
| PUT | `/tenants/{id}/photos/{photoId}/set-main` | `/contacts/{id}/photos/{photoId}/set-main` | setMainPhoto | ADMIN, EDITOR |
| POST | `/tenants/{tid}/addresses` | `/contacts/{cid}/addresses` | createAddress | ADMIN, EDITOR |
| GET | `/tenants/{tid}/addresses` | `/contacts/{cid}/addresses` | getAddresses | ALL |
| GET | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | getAddress | ALL |
| PUT | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | updateAddress | ADMIN, EDITOR |
| DELETE | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | deleteAddress | ADMIN, EDITOR |

### New endpoints

| Method | Path | Operation | Roles | Request/Response |
|--------|------|-----------|-------|------------------|
| POST | `/contacts/{id}/notes` | createNote | ADMIN, EDITOR | `CreateContactNoteRequest` → `ContactNoteResponse` |
| GET | `/contacts/{id}/notes` | getNotes | ALL | → `List<ContactNoteResponse>` |
| GET | `/contacts/{cid}/notes/{nid}` | getNote | ALL | → `ContactNoteResponse` |
| PUT | `/contacts/{cid}/notes/{nid}` | updateNote | ADMIN, EDITOR | `UpdateContactNoteRequest` → `ContactNoteResponse` |
| DELETE | `/contacts/{cid}/notes/{nid}` | deleteNote | ADMIN, EDITOR | → 204 |
| POST | `/contacts/{id}/relationships` | createRelationship | ADMIN, EDITOR | `CreateContactRelationshipRequest` → `ContactRelationshipResponse` |
| GET | `/contacts/{id}/relationships` | getRelationships | ALL | → `List<ContactRelationshipResponse>` |
| PUT | `/contacts/{cid}/relationships/{rid}` | updateRelationship | ADMIN, EDITOR | `UpdateContactRelationshipRequest` → `ContactRelationshipResponse` |
| DELETE | `/contacts/{cid}/relationships/{rid}` | deleteRelationship | ADMIN, EDITOR | → 204 |
| POST | `/contacts/{id}/tags` | addTag | ADMIN, EDITOR | `AddContactTagRequest` → `ContactResponse` |
| DELETE | `/contacts/{id}/tags/{tag}` | removeTag | ADMIN, EDITOR | → `ContactResponse` |
| POST | `/contacts/check-duplicates` | checkDuplicates | ADMIN, EDITOR | `CreateContactRequest` → `DuplicateCheckResponse` |

### Booklet paths update

`/booklets/tenant/{tenantIdentifier}` → `/booklets/contact/{contactIdentifier}`

---

## 8. OpenAPI Changes

### Path file rename

`openapi/src/paths/tenants.yaml` → `openapi/src/paths/contacts.yaml`

### Root file (`openapi/src/app.yaml`) changes

**Tags section**: Rename `Tenants` tag to `Contacts`

**All path entries**: Replace `/tenants/` with `/contacts/`, `tenants.yaml` with `contacts.yaml`

**Schema renames in `components/schemas`**:

| Old | New |
|-----|-----|
| `TenantIdentifier` | `ContactIdentifier` |
| `TenantAddressIdentifier` | `ContactAddressIdentifier` |
| `CreateTenantRequest` | `CreateContactRequest` |
| `UpdateTenantRequest` | `UpdateContactRequest` |
| `TenantResponse` | `ContactResponse` |
| `TenantSummary` | `ContactSummary` |
| `TenantAddressResponse` | `ContactAddressResponse` |
| `CreateTenantAddressRequest` | `CreateContactAddressRequest` |
| `UpdateTenantAddressRequest` | `UpdateContactAddressRequest` |
| `TenantPropertyAssignment` | `ContactPropertyAssignment` |
| `PageResponseTenantResponse` | `PageResponseContactResponse` |
| `PropertyTenantHistoryResponse` | `PropertyContactHistoryResponse` |
| `LinkTenantToPropertyRequest` | `LinkContactToPropertyRequest` |

**New schemas to add**:

- `ContactNoteResponse`
- `CreateContactNoteRequest`
- `UpdateContactNoteRequest`
- `ContactRelationshipResponse`
- `CreateContactRelationshipRequest`
- `UpdateContactRelationshipRequest`
- `AddContactTagRequest`
- `DuplicateCheckResponse`
- `DuplicateMatch`
- `ContactNoteIdentifier`
- `ContactRelationshipIdentifier`
- Enums: `ContactType`, `InteractionType`, `RelationshipType`, `ContactTag`, `DataRetentionStatus`

**New query parameters on GET `/contacts`**:

- `contactType` (optional, enum: INDIVIDUAL, COMPANY, SERVICE_PROVIDER)
- `tags` (optional, comma-separated list of ContactTag values)

**ContractPartyResponse schema**: `tenant` field → `contact` (references `ContactSummary`)

**ContractPartyRole enum in schema**: Add `SIGNER`, `CORPORATE_TENANT`, `AUTHORIZED_REPRESENTATIVE`

**AddContractPartyRequest / UpdateContractPartyRequest schemas**: `tenantIdentifier` → `contactIdentifier`, `newTenant` → `newContact`

**ChangePrimaryTenantRequest**: `tenantIdentifier` → `contactIdentifier`, `newTenant` → `newContact`

**CalendarFeed schemas**: `tenantIdentifier` → `contactIdentifier`

**After all edits**: Run `make bundle-openapi` then `cd frontend && yarn generate:api`

---

## 9. Frontend Changes

### File renames

| Old | New |
|-----|-----|
| `frontend/app/src/api/tenants.ts` | `frontend/app/src/api/contacts.ts` |
| `frontend/app/src/hooks/useTenantHooks.ts` | `frontend/app/src/hooks/useContactHooks.ts` |
| `frontend/app/src/pages/TenantListPage.tsx` | `frontend/app/src/pages/ContactListPage.tsx` |
| `frontend/app/src/pages/TenantDetailPage.tsx` | `frontend/app/src/pages/ContactDetailPage.tsx` |
| `frontend/app/src/pages/TenantCreatePage.tsx` | `frontend/app/src/pages/ContactCreatePage.tsx` |
| `frontend/app/src/pages/TenantEditPage.tsx` | `frontend/app/src/pages/ContactEditPage.tsx` |
| `frontend/app/src/pages/TenantsPage.tsx` | `frontend/app/src/pages/ContactsPage.tsx` |
| `frontend/app/src/components/tenants/` | `frontend/app/src/components/contacts/` |
| `frontend/app/src/components/tenants/TenantCard.tsx` | `frontend/app/src/components/contacts/ContactCard.tsx` |
| `frontend/app/src/components/tenants/TenantForm.tsx` | `frontend/app/src/components/contacts/ContactForm.tsx` |
| `frontend/app/src/components/tenants/TenantAddressList.tsx` | `frontend/app/src/components/contacts/ContactAddressList.tsx` |
| `frontend/app/src/components/tenants/AddressForm.tsx` | `frontend/app/src/components/contacts/AddressForm.tsx` |

### New frontend components

| Component | Purpose |
|-----------|---------|
| `ContactNoteList.tsx` | Timeline view of notes/interactions |
| `ContactNoteForm.tsx` | Create/edit note with rich text (RichTextEditor), interaction type picker, follow-up date |
| `ContactRelationshipList.tsx` | Display relationships with links to related contacts |
| `ContactRelationshipForm.tsx` | Create/edit relationship (contact picker + type dropdown) |
| `ContactTagPicker.tsx` | Multi-select tag chips (predefined list) |
| `ContactTypeSelector.tsx` | Radio/segmented control for INDIVIDUAL / COMPANY / SERVICE_PROVIDER |
| `ContactDuplicateWarning.tsx` | Shown during create when duplicates detected |
| `ContactQuickAdd.tsx` | Minimal form: name + optional email/phone (for prospect workflow) |

### Route changes

| Old Route | New Route |
|-----------|-----------|
| `/tenants` | `/contacts` |
| `/tenants/new` | `/contacts/new` |
| `/tenants/:id` | `/contacts/:id` |
| `/tenants/:id/edit` | `/contacts/:id/edit` |

### ContactForm updates

The form needs to be type-aware:
- When `contactType === 'COMPANY'` or `'SERVICE_PROVIDER'`: show company fields (companyName, tradeName, industry, website, invoiceEmail)
- When `contactType === 'INDIVIDUAL'`: hide company fields, show dateOfBirth, idExpiryDate
- Tag picker always visible
- Notes field uses `RichTextEditor` (per feedback rule)

### ContactDetailPage updates

Tabs/sections:
1. **Overview** — Contact card with name, type badge, tags, phone/email quick actions, top 3 contextual alerts + "see more"
2. **Properties** — Active property assignments (from contracts), link/unlink actions
3. **Notes & Timeline** — Interaction notes (rich text) with interaction type icons, follow-up dates
4. **Relationships** — Related contacts with relationship type labels
5. **Addresses** — Address list (existing)
6. **Documents** — Documents list (existing)
7. **Photos** — Photos grid (existing)
8. **History** — Property assignment history (existing)
9. **Audit Log** — System audit trail (existing)

### ContactCard (list view) updates

Shows: display_name (with type badge), active properties, balance, tags as chips, phone + email quick-action icons

### React Query hook additions

```typescript
// In useContactHooks.ts
useContacts(params)          // replaces useTenants
useContact(id)               // replaces useTenant
useCreateContact()           // replaces useCreateTenant
useUpdateContact()           // replaces useUpdateTenant
useDeleteContact()           // replaces useDeleteTenant
useContactNotes(contactId)
useCreateContactNote()
useUpdateContactNote()
useDeleteContactNote()
useContactRelationships(contactId)
useCreateContactRelationship()
useUpdateContactRelationship()
useDeleteContactRelationship()
useAddContactTag()
useRemoveContactTag()
useCheckDuplicates()
useLinkContactToProperty()
useUnlinkContactFromProperty()
```

### Navigation

Sidebar: "Tenants" → "Contacts"

### Service provider filtering

The `/contacts` list page needs a filter/tab for contact type. Service providers should be visually segregated (e.g., separate tab or filter option).

---

## 10. Follow-up Issues

These items are explicitly **out of scope** for this sprint but should be created as follow-up Linear issues:

1. **BUUR-xx: CSV Import for Contacts** — Bulk import from CSV/Excel. Supports column mapping, duplicate detection on import, dry-run preview. Separate sprint.

2. **BUUR-xx: GDPR Data Retention & Anonymization** — Implement automatic retention policies, anonymization workflow for `data_retention_status`, right-to-erasure flow. The column exists but no automation.

3. **BUUR-xx: Tenant Portal (Contact Login)** — Use `keycloak_user_id` column to link contacts to Keycloak accounts. Self-service portal for tenants to view contracts, make payments, submit maintenance requests.

4. **BUUR-xx: Contact Timeline Auto-Events** — Auto-populate contact timeline with system events: contract signed, payment received, document uploaded, notification sent. Currently manual notes only.

5. **BUUR-xx: Contact Follow-Up Reminder Notifications** — Implement the `ContactFollowUpReminderJob` Quartz job that sends reminders based on `follow_up_date` in `contact_notes`.

6. **BUUR-xx: Contact Merge/Deduplication Tool** — UI for reviewing and merging duplicate contacts. Requires careful handling of all FK references (contracts, payments, documents, notes, relationships).

7. **BUUR-xx: Demo Data Generator Update** — Update `DemoTenantGenerator` → `DemoContactGenerator` to create contacts with different types, tags, notes, and relationships.

8. **BUUR-xx: Backoffice Contact Management** — Update backoffice admin views to reflect contacts rename (if any backoffice tenant references exist).

9. **BUUR-xx: Booklet/Export Updates** — Update `BookletController` path from `/booklets/tenant/` to `/booklets/contact/` and update all PDF/CSV exporters.

10. **BUUR-xx: Contact Balance Display** — Show financial balance (outstanding payments) on contact card. Requires aggregation query across contracts → payments.

---

## Implementation Order

Recommended phased approach within this sprint:

### Phase 1: Database + JOOQ Regen (1 day)
1. Write and apply V038 migration
2. `mvn generate-sources -pl jooq -am` — regenerate JOOQ classes
3. Verify generated code compiles

### Phase 2: Backend Core Rename (2-3 days)
1. New enums (`ContactType`, `InteractionType`, `RelationshipType`, `ContactTag`, `DataRetentionStatus`)
2. New typed identifiers (`ContactIdentifier`, `ContactAddressIdentifier`, `ContactNoteIdentifier`, `ContactRelationshipIdentifier`)
3. Rename domain classes: `Tenant` → `Contact`, `TenantAddress` → `ContactAddress`, `PropertyTenantHistory` → `PropertyContactHistory`
4. Update `ContractParty`, `Notification`, `CalendarFeed` field renames
5. Rename all DTOs
6. Update `EntityPrefix` + `SidGenerator`
7. Rename repositories + update queries
8. Rename mappers
9. Rename services
10. Rename controller
11. Fix all compilation errors across all modules (demo-data, notifications, takeout, booklets, backoffice)
12. Build: `mvn clean install -DskipTests`

### Phase 3: New Features (2 days)
1. New domain classes: `ContactNote`, `ContactRelationship`
2. New repositories: `ContactNoteRepository`, `ContactRelationshipRepository`, `ContactTagRepository`
3. New services: `ContactNoteService`, `ContactRelationshipService`
4. New service methods on `ContactService`: `addTag`, `removeTag`, `checkDuplicates`
5. Duplicate detection logic
6. Display name computation
7. Update controller with new endpoints
8. Build + verify

### Phase 4: OpenAPI + Frontend (2 days)
1. Rename + update `openapi/src/paths/contacts.yaml`
2. Update `openapi/src/app.yaml` — all schema renames + new schemas + new paths
3. `make bundle-openapi`
4. `cd frontend && yarn generate:api`
5. Rename all frontend files
6. Update components, forms, pages
7. Add new components (notes, relationships, tags, duplicate warning)
8. Update routes + navigation
9. Build + verify: `cd frontend && yarn build`

### Phase 5: Polish (1 day)
1. Audit trail: update all `"TENANT"` → `"CONTACT"` strings in audit calls
2. Demo data: update generator (or defer to follow-up)
3. Metrics: rename `tenant.*` → `contact.*`
4. Test full flow end-to-end locally
5. Code review + PR

---

## Cross-cutting concerns

### Files that reference "tenant" (grep blast radius)

The following files/areas need tenant→contact renames beyond the core module:

**Backend core (`buurman/`)**:
- `ContractService.java` — references `TenantRepository`, `TenantMapper`, creates `ContractParty` with `tenantId`
- `ContractMapper.java` — maps `TenantSummary`
- `PropertyService.java` — references `TenantRepository` for property-tenant relationships
- `AuditLogRepository.java` — `"TENANT"` entity type string
- `SecurityUtils.java` or any principal references — no tenant-specific code expected

**Demo data (`buurman-demo-data/`)**:
- `DemoTenantGenerator.java` → rename to `DemoContactGenerator.java`
- `DemoContractGenerator.java` — creates contract parties with `tenantId`
- `DemoNotificationGenerator.java` — `recipientTenantId`
- `DemoDocumentGenerator.java` — `"TENANT"` entity type
- `DemoAuditLogGenerator.java` — `"TENANT"` entity type
- `DemoExpenseGenerator.java` — tenant references
- `DemoPaymentInstructionGenerator.java` — tenant references
- `DemoDataContext.java` — `tenantIdsByTeam`, `businessTenantFlag`
- `DemoDataService.java` — orchestrates tenant generation
- `ContractExtensionDemoDataGenerator.java` — tenant references

**Notifications (`buurman-notifications/`)**:
- `NotificationRepository.java` — `recipient_tenant_id` column reference
- `NotificationRecordMapper.java` — `recipientTenantId` field
- Notification templates referencing tenant — check Thymeleaf templates

**Takeout (`buurman-takeout/`)**:
- `DataTakeoutRepository.java` — may query tenants table

**Booklets (`buurman-booklets/`)**:
- `BookletController.java` — `/booklets/tenant/` path
- Exporter classes referencing tenant data

**Backoffice (`buurman-backoffice/`)**:
- Check for any tenant references in backoffice controllers/services
