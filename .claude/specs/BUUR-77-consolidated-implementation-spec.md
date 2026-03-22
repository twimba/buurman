# BUUR-77: Tenants → Contacts Mini-CRM — Consolidated Implementation Spec

**Status**: FINAL v3 — Implementation-ready. Single authoritative document.
**Date**: 2026-03-21
**Branch**: `luissantos/buur-77-rework-the-tenants-feature`
**Linear**: BUUR-77

> **This is the ONLY spec document for BUUR-77.** It consolidates and supersedes:
> - `BUUR-77-contacts-rework.md` (original)
> - `BUUR-77-contacts-rework-amendments.md` (v2+v3 amendments)
>
> All conflicts are resolved. No cross-referencing needed.

---

## Table of Contents

1. [Scope & Goals](#1-scope--goals)
2. [Database Migrations](#2-database-migrations)
3. [Enums](#3-enums)
4. [Domain Classes](#4-domain-classes)
5. [DTOs](#5-dtos)
6. [Repository Layer](#6-repository-layer)
7. [Service Layer](#7-service-layer)
8. [Controller / API Endpoints](#8-controller--api-endpoints)
9. [OpenAPI Changes](#9-openapi-changes)
10. [Frontend Changes](#10-frontend-changes)
11. [GDPR Erase](#11-gdpr-erase)
12. [Demo Data Generator](#12-demo-data-generator)
13. [Backoffice Rename](#13-backoffice-rename)
14. [Booklet / Export Updates](#14-booklet--export-updates)
15. [PostHog Analytics](#15-posthog-analytics)
16. [Empty States](#16-empty-states)
17. [Cross-Cutting Concerns](#17-cross-cutting-concerns)
18. [Implementation Phases](#18-implementation-phases)
19. [Follow-Up Issues (Out of Scope)](#19-follow-up-issues-out-of-scope)

---

## 1. Scope & Goals

Transform the "Tenants" feature into a lightweight Contacts mini-CRM for small Dutch landlords (1-50 properties).

### IN Scope (this sprint)

- **Core rename**: Tenants → Contacts across DB (6 tables), backend (10 modules), OpenAPI, frontend
- **Contact types**: INDIVIDUAL, COMPANY, SERVICE_PROVIDER with type-aware forms/validation
- **Notes & Activity**: Rich-text notes with interaction types, auto-generated activity feed (UNION ALL), follow-up reminders
- **Tags**: 8 predefined tags (VIP, PROSPECT, LATE_PAYER, LONG_TERM, KEY_HOLDER, DO_NOT_CONTACT, FORMER_TENANT, REFERRED)
- **Relationships**: 6 types with single-record dual-display, inverse labels
- **Duplicate detection**: Warning during creation (not a blocker)
- **5-tab detail page**: Overview, Activity, Relationships, Files, Addresses
- **GDPR erasure**: `POST /contacts/{id}/erase` — anonymize PII, delete S3 files
- **Demo data**: Ultra-realistic Dutch data for all 3 contact types
- **Backoffice**: Mechanical rename across buurman-backoffice module
- **Booklet/Export**: Professional A4 PDF + CSV with all contact fields

### NOT in Scope

| Issue | ID | Notes |
|-------|----|-------|
| CSV Import for Contacts | BUUR-82 | Bulk import from spreadsheets |
| Tenant Portal | Future epic | Login via email, no extra columns |
| Contact Balance Display | BUUR-83 | Outstanding payment aggregation on card |

---

## 2. Database Migrations

### V038 (already committed)

**File**: `backend/jooq/src/main/resources/db/migration/V038__contacts_rework.sql`

Already applied. Summary of changes:
- Renames `tenants` → `contacts`, adds columns: `contact_type`, `display_name`, `company_name`, `trade_name`, `industry`, `website`, `invoice_email`, `date_of_birth`, `id_expiry_date`, `notes`, `data_retention_status`
- Drops `current_property_id` column
- Makes `first_name` nullable with conditional CHECK by contact_type
- Adds `display_name` NOT NULL with backfill computation
- Renames `tenant_addresses` → `contact_addresses` (column `tenant_id` → `contact_id`)
- Renames `property_tenant_history` → `property_contact_history` (column `tenant_id` → `contact_id`)
- Renames `contract_parties.tenant_id` → `contact_id`, expanded role CHECK (7 roles)
- Renames `notifications.recipient_tenant_id` → `recipient_contact_id`
- Renames `calendar_feeds.tenant_id` → `contact_id` with CHECK constraint recreated
- Creates new tables: `contact_notes`, `contact_relationships`, `contact_tags`
- Backfills identifiers: TEN→CTC, TAD→CAD
- Migrates `additional_info` to `contact_notes`, drops column

### V039 (new — write this file)

**File**: `backend/jooq/src/main/resources/db/migration/V039__contacts_amendments.sql`

```sql
-- =============================================================================
-- V039__contacts_amendments.sql
-- Post-review amendments: refined tags, relationship types, contact_tags ON DELETE CASCADE
-- =============================================================================

-- 1. Refine contact_tags CHECK: 10 tags -> 8 tags
ALTER TABLE contact_tags DROP CONSTRAINT IF EXISTS chk_contact_tags_tag;
ALTER TABLE contact_tags ADD CONSTRAINT chk_contact_tags_tag CHECK (
    tag IN (
        'VIP', 'PROSPECT', 'LATE_PAYER', 'LONG_TERM',
        'KEY_HOLDER', 'DO_NOT_CONTACT', 'FORMER_TENANT', 'REFERRED'
    )
);
DELETE FROM contact_tags WHERE tag NOT IN (
    'VIP', 'PROSPECT', 'LATE_PAYER', 'LONG_TERM',
    'KEY_HOLDER', 'DO_NOT_CONTACT', 'FORMER_TENANT', 'REFERRED'
);

-- 2. Refine contact_relationships CHECK: 8 types -> 6 types
UPDATE contact_relationships SET relationship_type = 'WORKS_FOR'
WHERE relationship_type = 'EMPLOYEE_OF';
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

-- 3. Add ON DELETE CASCADE to contact_tags
ALTER TABLE contact_tags DROP CONSTRAINT IF EXISTS contact_tags_contact_id_fkey;
ALTER TABLE contact_tags ADD CONSTRAINT contact_tags_contact_id_fkey
    FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE;

-- 4. Add pinned column to contact_notes
ALTER TABLE contact_notes ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_contact_notes_pinned ON contact_notes (contact_id, pinned)
    WHERE pinned = TRUE AND deleted_at IS NULL;
```

After both migrations: `cd backend && mvn generate-sources -pl jooq -am`

### Tables affected by FK renames (summary)

| Table | Column renamed | From | To |
|-------|---------------|------|-----|
| `contacts` (was `tenants`) | — (table rename) | `tenants` | `contacts` |
| `contact_addresses` (was `tenant_addresses`) | `tenant_id` → `contact_id` | `tenant_addresses` | `contact_addresses` |
| `property_contact_history` (was `property_tenant_history`) | `tenant_id` → `contact_id` | `property_tenant_history` | `property_contact_history` |
| `contract_parties` | `tenant_id` → `contact_id` | — | — |
| `notifications` | `recipient_tenant_id` → `recipient_contact_id` | — | — |
| `calendar_feeds` | `tenant_id` → `contact_id` | — | — |

### Identifier prefix changes

| Old prefix | New prefix | Entity |
|-----------|-----------|--------|
| `TEN` | `CTC` | Contacts |
| `TAD` | `CAD` | Contact Addresses |
| (new) | `CNT` | Contact Notes |
| (new) | `CRL` | Contact Relationships |

---

## 3. Enums

### New Enums

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
    GUARANTOR_FOR("Guarantor for"),
    FAMILY_OF("Family of"),
    PARTNER_OF("Partner of"),
    WORKS_FOR("Works for"),
    CONTACT_PERSON_FOR("Contact person for"),
    OTHER("Other");

    private final String displayName;

    /**
     * Returns the inverse relationship type (same enum value).
     * Use inverseDisplayName() for the label shown on the target contact's page.
     */
    public RelationshipType inverse() {
        return switch (this) {
            case GUARANTOR_FOR -> GUARANTOR_FOR;
            case FAMILY_OF -> FAMILY_OF;
            case PARTNER_OF -> PARTNER_OF;
            case WORKS_FOR -> WORKS_FOR;
            case CONTACT_PERSON_FOR -> CONTACT_PERSON_FOR;
            case OTHER -> OTHER;
        };
    }

    /**
     * Display name when viewing from the target contact's perspective.
     */
    public String inverseDisplayName() {
        return switch (this) {
            case GUARANTOR_FOR -> "Guaranteed by";
            case FAMILY_OF -> "Family of";
            case PARTNER_OF -> "Partner of";
            case WORKS_FOR -> "Employer of";
            case CONTACT_PERSON_FOR -> "Has contact person";
            case OTHER -> "Other";
        };
    }

    public boolean isSymmetric() {
        return this == FAMILY_OF || this == PARTNER_OF;
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
    LATE_PAYER("Late Payer"),
    LONG_TERM("Long-term"),
    KEY_HOLDER("Key Holder"),
    DO_NOT_CONTACT("Do Not Contact"),
    FORMER_TENANT("Former Tenant"),
    REFERRED("Referred");

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

### Modified Enums

**`ContractPartyRole`** — add 3 new values:
```java
@Getter
@RequiredArgsConstructor
public enum ContractPartyRole {
    PRIMARY_TENANT("Primary tenant"),
    GUARANTOR("Guarantor"),
    COSIGNER("Co-signer"),
    EXTRA_TENANT("Additional tenant"),
    SIGNER("Signer"),                              // NEW
    CORPORATE_TENANT("Corporate tenant"),           // NEW
    AUTHORIZED_REPRESENTATIVE("Authorized representative"); // NEW

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
// Replace TENANT with:
CONTACT,
CONTACT_ADDRESS,
CONTACT_NOTE,
CONTACT_RELATIONSHIP,
```

**`ContactAddress.AddressType`** (was `TenantAddress.AddressType`) — add 2 values:
```java
public enum AddressType {
    CURRENT, MAILING, RELATIVE, WORK, HISTORIC,
    REGISTERED_OFFICE,  // NEW — company registered office
    BRANCH              // NEW — company branch
}
```

---

## 4. Domain Classes

### Renamed / Modified

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
    @Builder.Default private Optional<String> firstName = Optional.empty(); // required for INDIVIDUAL only
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
    // Rich text notes
    @Builder.Default private Optional<String> notes = Optional.empty();
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
- Removed `keycloakUserId` (tenant portal uses email)
- `firstName` is now `Optional<String>` (nullable for companies)
- Added: `contactType`, `displayName`, `companyName`, `tradeName`, `industry`, `website`, `invoiceEmail`, `dateOfBirth`, `idExpiryDate`, `notes`, `dataRetentionStatus`

**`TenantAddress.java` → `ContactAddress.java`** (`com.buurman.domain.ContactAddress`)

Field `tenantId` → `contactId`. AddressType enum gains `REGISTERED_OFFICE`, `BRANCH`. Otherwise identical.

**`PropertyTenantHistory.java` → `PropertyContactHistory.java`**

Field `tenantId` → `contactId`. Otherwise identical.

**`ContractParty.java`** — field rename: `tenantId` → `contactId`

**`Notification.java`** — field rename: `recipientTenantId` → `recipientContactId`

**`CalendarFeed.java`** — field rename: `tenantId` → `contactId`

### New Domain Classes

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
    private boolean pinned;
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

### New Typed Identifiers

Package: `com.buurman.domain.identifier`

| Class | Prefix | Replaces |
|-------|--------|----------|
| `ContactIdentifier` | CTC | `TenantIdentifier` (delete after migration) |
| `ContactAddressIdentifier` | CAD | `TenantAddressIdentifier` (delete after migration) |
| `ContactNoteIdentifier` | CNT | (new) |
| `ContactRelationshipIdentifier` | CRL | (new) |

Each follows the standard pattern:
```java
public final class ContactIdentifier extends Sid {
    private ContactIdentifier(String value) { super(value); }
    @JsonCreator
    public static ContactIdentifier of(String value) { return new ContactIdentifier(value); }
}
```

### SidGenerator Changes

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

## 5. DTOs

### Request DTOs (`com.buurman.dto.request`)

**`CreateContactRequest`** (replaces `CreateTenantRequest`)
```java
public record CreateContactRequest(
    @NotNull ContactType contactType,
    Optional<@Size(max = 255) String> firstName,        // required for INDIVIDUAL
    Optional<@Size(max = 255) String> lastName,
    Optional<@Email String> email,
    Optional<@Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String> phone,
    Optional<@Size(max = 100) String> taxNumber,
    Optional<@Size(max = 100) String> idNumber,
    Optional<LocalDate> idExpiryDate,
    Optional<LocalDate> dateOfBirth,
    Optional<@Size(max = 255) String> companyName,      // required for COMPANY/SERVICE_PROVIDER
    Optional<@Size(max = 255) String> tradeName,
    Optional<@Size(max = 100) String> industry,
    Optional<@URL String> website,
    Optional<@Email String> invoiceEmail,
    Optional<String> notes,
    Optional<List<ContactTag>> tags
) {}
```

**`UpdateContactRequest`** — same fields as CreateContactRequest (contactType required, non-optional).

**`CreateContactAddressRequest`** (replaces `CreateTenantAddressRequest`) — identical structure, references `ContactAddress.AddressType` (includes REGISTERED_OFFICE, BRANCH).

**`UpdateContactAddressRequest`** (replaces `UpdateTenantAddressRequest`) — same as above.

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

**`UpdateContactNoteRequest`** (new) — same fields as CreateContactNoteRequest.

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

**`ChangePrimaryContactRequest`** (was `ChangePrimaryTenantRequest`)
- `tenantIdentifier` → `contactIdentifier`
- `newTenant` → `newContact` (references `CreateContactRequest`)

**DELETED DTOs:**
- `LinkContactToPropertyRequest` / `LinkTenantToPropertyRequest` — property linking is through contracts only

### Response DTOs (`com.buurman.dto.response`)

**`ContactResponse`** (replaces `TenantResponse`) — used for detail page only
```java
public record ContactResponse(
    Sid identifier,
    ContactType contactType,
    Optional<String> firstName,
    Optional<String> lastName,
    String displayName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<LocalDate> idExpiryDate,
    Optional<LocalDate> dateOfBirth,
    Optional<String> companyName,
    Optional<String> tradeName,
    Optional<String> industry,
    Optional<String> website,
    Optional<String> invoiceEmail,
    Optional<String> notes,
    Optional<String> mainPhotoUrl,
    Optional<String> mainPhotoThumbnailUrl,
    List<ContactPropertyAssignment> activeProperties,
    List<ContactTag> tags,
    DataRetentionStatus dataRetentionStatus,
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`ContactListItemResponse`** (new — used for list page, N+1 fix)
```java
public record ContactListItemResponse(
    Sid identifier,
    ContactType contactType,
    String displayName,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> companyName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> mainPhotoThumbnailUrl,
    List<ContactTag> tags,
    int activeContractCount,
    Instant createdAt
) {}
```

**Important**: `GET /contacts` list endpoint returns `PageResponse<ContactListItemResponse>` (NOT `ContactResponse`). Full `ContactResponse` is only for `GET /contacts/{id}`.

**`ContactSummary`** (replaces `TenantSummary`)
```java
public record ContactSummary(
    Sid identifier,
    ContactType contactType,
    String displayName,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<String> phone
) {}
```

**`ContactPropertyAssignment`** (replaces `TenantPropertyAssignment`)
```java
public record ContactPropertyAssignment(
    PropertySummary property,
    Optional<String> role
) {}
```

**`ContactAddressResponse`** (replaces `TenantAddressResponse`) — same fields, references ContactAddress types.

**`PropertyContactHistoryResponse`** (replaces `PropertyTenantHistoryResponse`) — same fields.

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
    boolean pinned,
    String createdByName,
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`ContactRelationshipResponse`** (new — perspective-aware)
```java
public record ContactRelationshipResponse(
    Sid identifier,
    ContactSummary relatedContact,    // the OTHER contact (not the one being viewed)
    RelationshipType relationshipType,
    String displayLabel,              // "Works for" or "Employer of" depending on perspective
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt
) {}
```

**`ContactActivityItem`** (new — for activity feed)
```java
public record ContactActivityItem(
    String eventType,       // "NOTE", "CONTRACT_CREATED", "CONTRACT_ACTIVATED",
                            // "CONTRACT_TERMINATED", "CONTRACT_EXPIRED",
                            // "PAYMENT_RECEIVED", "PAYMENT_OVERDUE",
                            // "DOCUMENT_UPLOADED", "NOTIFICATION_SENT",
                            // "CONTACT_UPDATED"
    Instant occurredAt,
    String description,     // human-readable, e.g. "Contract CON01ABC... activated"
    Optional<Sid> relatedEntityIdentifier,
    Optional<String> relatedEntityType,     // "CONTRACT", "PAYMENT", "DOCUMENT", "NOTIFICATION"
    // Only populated for NOTE events:
    Optional<Sid> noteIdentifier,
    Optional<InteractionType> interactionType,
    Optional<String> noteBody,
    Optional<String> noteSubject,
    Optional<Boolean> pinned,
    Optional<String> createdByName
) {}
```

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

## 6. Repository Layer

### Renamed / Modified Repositories

**`TenantRepository` → `ContactRepository`**

- Table: `TENANTS` → `CONTACTS`, Record: `TenantsRecord` → `ContactsRecord`
- All domain types: `Tenant` → `Contact`
- Remove `findByCurrentPropertyId()` (column dropped)
- Add `findByContactType()` for filtering
- Add new sortable/searchable fields: `displayName`, `contactType`, `companyName`

New methods:
```java
Optional<Contact> findByPhoneAndTeamId(String phone, UUID teamId);
PaginatedResult<Contact> findAllByTeamIdPaginated(
    UUID teamId, Optional<String> search, Optional<ContactType> contactType,
    Optional<List<ContactTag>> tags, PageRequest pageRequest);
List<Contact> findDuplicates(UUID teamId, Optional<String> email, Optional<String> phone,
    Optional<String> taxNumber, Optional<String> idNumber,
    Optional<String> firstName, Optional<String> lastName);
void anonymize(UUID contactId, UUID teamId);  // GDPR erase
```

List query for `ContactListItemResponse`:
- Single LEFT JOIN on `contract_parties` + `contracts` with GROUP BY to compute `activeContractCount` per contact
- Tags batch-fetched via `ContactTagRepository.findByContactIdsGrouped(contactIds)`
- `mainPhotoThumbnailUrl` batch-fetched via single query on `photos` WHERE `entity_type = 'CONTACT' AND is_main = TRUE AND entity_id IN (:contactIds)`

**`TenantAddressRepository` → `ContactAddressRepository`**

- Table: `TENANT_ADDRESSES` → `CONTACT_ADDRESSES`
- Field: `TENANT_ID` → `CONTACT_ID`
- Domain: `TenantAddress` → `ContactAddress`
- **Fix**: UPDATE now includes `team_id` in WHERE clause (known bug from review)
- New method: `void hardDeleteByContactId(UUID contactId);` — for GDPR erase

**`PropertyTenantHistoryRepository` → `PropertyContactHistoryRepository`**

- Table: `PROPERTY_TENANT_HISTORY` → `PROPERTY_CONTACT_HISTORY`
- Field: `TENANT_ID` → `CONTACT_ID`
- Methods: `findByTenantId` → `findByContactId`

**`ContractPartyRepository`** — field renames:
- `TENANT_ID` → `CONTACT_ID`
- `findByTenantIdAndContractIdAndTeamId` → `findByContactIdAndContractIdAndTeamId`
- `existsByContractIdAndTenantIdAndTeamId` → `existsByContractIdAndContactIdAndTeamId`

### New Repositories

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
    void markFollowUpReminderSent(UUID noteId);
    void setPinned(UUID noteId, UUID teamId, boolean pinned);
    void anonymizeByContactId(UUID contactId);  // GDPR: body → "Content erased per GDPR request", subject → NULL
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
    // IMPORTANT: returns relationships where contact is EITHER source OR target
    List<ContactRelationship> findByContactIdAndTeamId(UUID contactId, UUID teamId);
    Optional<ContactRelationship> findByIdentifierAndTeamId(Sid identifier, UUID teamId);
    ContactRelationship getByIdentifierAndTeamId(Sid identifier, UUID teamId);
    void softDeleteByIdAndTeamId(UUID id, UUID teamId);
    boolean existsByPairAndType(UUID teamId, UUID sourceId, UUID targetId, RelationshipType type);
    void softDeleteByContactId(UUID contactId, UUID teamId);  // GDPR erase
}
```

The `findByContactIdAndTeamId` query MUST be bidirectional:
```sql
SELECT * FROM contact_relationships
WHERE team_id = :teamId
  AND deleted_at IS NULL
  AND (source_contact_id = :contactId OR target_contact_id = :contactId)
ORDER BY created_at DESC
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

**`ContactActivityRepository`** (new — or method in `ContactRepository`)
```java
PageResponse<ContactActivityItem> findActivityByContactId(
    UUID contactId, UUID teamId, PageRequest pageRequest);
```

UNION ALL across 6 sources:
1. `contact_notes` — `SELECT 'NOTE', occurred_at, body, identifier, ...`
2. `audit_log WHERE entity_type = 'CONTACT' AND entity_id = contact.id` — `SELECT 'CONTACT_UPDATED', created_at, description, ...`
3. `contracts via contract_parties` — contract lifecycle events (CREATED, ACTIVATED, TERMINATED, EXPIRED)
4. `payments via contract → contract_parties` — `PAYMENT_RECEIVED` / `PAYMENT_OVERDUE`
5. `documents WHERE entity_type = 'CONTACT' AND entity_id = contact.id` — `DOCUMENT_UPLOADED`
6. `notifications WHERE recipient_contact_id = contact.id` — `NOTIFICATION_SENT`

All branches: `ORDER BY occurred_at DESC LIMIT :size OFFSET :page * :size`

Performance: <10ms for typical data volume.

---

## 7. Service Layer

### Renamed / Modified Services

**`TenantService` → `ContactService`**

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final PropertyContactHistoryRepository historyRepository;
    private final ContactMapper contactMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final PhotoService photoService;
    private final PhotoRepository photoRepository;
    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final ContactAddressService addressService;
    private final ContactAddressRepository addressRepository;
    private final ContactNoteService noteService;
    private final ContactNoteRepository noteRepository;
    private final ContactRelationshipService relationshipService;
    private final ContactRelationshipRepository relationshipRepository;
    private final ContactTagRepository tagRepository;
    private final MetricsService metricsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    // --- CRUD ---
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse createContact(CreateContactRequest request, UserAuthentication auth);

    PageResponse<ContactListItemResponse> getContactsPaginated(
        UserAuthentication auth, Optional<String> search, Optional<ContactType> contactType,
        Optional<List<ContactTag>> tags, PageRequest pageRequest);

    ContactResponse getContact(ContactIdentifier identifier, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse updateContact(ContactIdentifier identifier, UpdateContactRequest request, UserAuthentication auth);

    @Transactional @PreAuthorize("hasRole('TEAM_ADMIN')")
    void deleteContact(ContactIdentifier identifier, UserAuthentication auth);

    // --- Tags ---
    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse addTag(ContactIdentifier identifier, AddContactTagRequest request, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactResponse removeTag(ContactIdentifier identifier, ContactTag tag, UserAuthentication auth);

    // --- Duplicate check ---
    DuplicateCheckResponse checkDuplicates(CreateContactRequest request, UserAuthentication auth);

    // --- Activity feed ---
    PageResponse<ContactActivityItem> getActivity(ContactIdentifier identifier, UserAuthentication auth, PageRequest pageRequest);

    // --- History ---
    List<PropertyContactHistoryResponse> getContactHistory(ContactIdentifier identifier, UserAuthentication auth);

    // --- Documents/Photos (same pattern as before, entity type "CONTACT") ---
    // uploadDocument, getDocuments, getDownloadUrl, deleteDocument
    // getPhotos, uploadPhoto, setMainPhoto

    // --- Addresses (delegate to ContactAddressService) ---

    // --- GDPR Erase ---
    @Transactional @PreAuthorize("hasRole('TEAM_ADMIN')")
    void eraseContactData(ContactIdentifier identifier, UserAuthentication auth);
}
```

**REMOVED methods** (no link/unlink — property association is through contracts):
- ~~`linkContactToProperty()`~~
- ~~`unlinkContactFromProperty()`~~

#### Business Rules for `createContact`

1. **Validate by type**:
   ```java
   private void validateContactRequest(CreateContactRequest request) {
       switch (request.contactType()) {
           case INDIVIDUAL -> {
               if (request.firstName().isEmpty() || request.firstName().get().isBlank()) {
                   throw new ValidationException("firstName is required for INDIVIDUAL contacts");
               }
           }
           case COMPANY, SERVICE_PROVIDER -> {
               if (request.companyName().isEmpty() || request.companyName().get().isBlank()) {
                   throw new ValidationException("companyName is required for COMPANY/SERVICE_PROVIDER contacts");
               }
           }
       }
   }
   ```

2. **Compute display_name**:
   ```java
   private String computeDisplayName(ContactType type, Optional<String> firstName,
                                      Optional<String> lastName, Optional<String> companyName) {
       return switch (type) {
           case INDIVIDUAL -> {
               String first = firstName.orElse("");
               String last = lastName.orElse("");
               yield (first + " " + last).trim();
           }
           case COMPANY, SERVICE_PROVIDER -> companyName.orElseThrow(
               () -> new BusinessException("Company name is required for " + type));
       };
   }
   ```

3. Duplicate check: email + phone uniqueness per team
4. If tags provided, insert into `contact_tags`
5. Increment metrics: `contact.total`

#### Business Rules for `updateContact`

1. Recompute `display_name` on every update
2. Email uniqueness check
3. Tags: diffed — remove old tags not in request, add new ones
4. **Type change field clearing**:

```java
private Contact applyTypeChangeClearing(Contact existing, UpdateContactRequest request) {
    if (existing.getContactType() == request.contactType()) {
        return existing;
    }

    ContactType from = existing.getContactType();
    ContactType to = request.contactType();

    if (from == ContactType.INDIVIDUAL && to != ContactType.INDIVIDUAL) {
        // Individual -> Company/ServiceProvider: clear individual-only fields
        existing.setDateOfBirth(Optional.empty());
        existing.setIdNumber(Optional.empty());
        existing.setIdExpiryDate(Optional.empty());
    } else if (from != ContactType.INDIVIDUAL && to == ContactType.INDIVIDUAL) {
        // Company/ServiceProvider -> Individual: clear company-only fields
        existing.setCompanyName(Optional.empty());
        existing.setTradeName(Optional.empty());
        existing.setIndustry(Optional.empty());
        existing.setWebsite(Optional.empty());
        existing.setInvoiceEmail(Optional.empty());
    }

    return existing;
}
```

**`TenantAddressService` → `ContactAddressService`**

Same logic, renamed types. All `tenantId` → `contactId`. Audit entity type: `"CONTACT_ADDRESS"`.

### New Services

**`ContactNoteService`** (`com.buurman.service.ContactNoteService`)
```java
@Service @Slf4j @RequiredArgsConstructor
public class ContactNoteService {
    private final ContactNoteRepository noteRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse createNote(ContactIdentifier contactId, CreateContactNoteRequest request, UserAuthentication auth);
    // Sets occurredAt to request.occurredAt or now(). Generates CNT identifier.

    List<ContactNoteResponse> getNotes(ContactIdentifier contactId, UserAuthentication auth);
    // Ordered by occurredAt DESC

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse updateNote(ContactIdentifier contactId, ContactNoteIdentifier noteId,
        UpdateContactNoteRequest request, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    void deleteNote(ContactIdentifier contactId, ContactNoteIdentifier noteId, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse pinNote(ContactIdentifier contactId, ContactNoteIdentifier noteId, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactNoteResponse unpinNote(ContactIdentifier contactId, ContactNoteIdentifier noteId, UserAuthentication auth);
}
```

**`ContactRelationshipService`** (`com.buurman.service.ContactRelationshipService`)
```java
@Service @Slf4j @RequiredArgsConstructor
public class ContactRelationshipService {
    private final ContactRelationshipRepository relationshipRepository;
    private final ContactRepository contactRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactRelationshipResponse createRelationship(
        ContactIdentifier sourceContactId, CreateContactRelationshipRequest request, UserAuthentication auth);
    // Validates: both contacts exist + same team, no self-reference, unique (source, target, type)

    List<ContactRelationshipResponse> getRelationships(ContactIdentifier contactId, UserAuthentication auth);
    // Returns relationships where contact is source OR target
    // Determines perspective for displayLabel:
    //   if viewing from source → relatedContact = target, displayLabel = type.getDisplayName()
    //   if viewing from target → relatedContact = source, displayLabel = type.inverseDisplayName()

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    ContactRelationshipResponse updateRelationship(
        ContactIdentifier contactId, ContactRelationshipIdentifier relationshipId,
        UpdateContactRelationshipRequest request, UserAuthentication auth);

    @Transactional @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    void deleteRelationship(ContactIdentifier contactId, ContactRelationshipIdentifier relationshipId,
        UserAuthentication auth);
    // Single record soft-deleted → disappears from both sides
}
```

#### Contract Party Validation by Contact Type

Add to `ContractPartyService.addContractParty()`:

| ContactType | Allowed Roles |
|-------------|--------------|
| INDIVIDUAL | PRIMARY_TENANT, GUARANTOR, COSIGNER, EXTRA_TENANT, SIGNER |
| COMPANY | CORPORATE_TENANT, AUTHORIZED_REPRESENTATIVE, SIGNER, GUARANTOR |
| SERVICE_PROVIDER | *cannot be a contract party at all* |

```java
private void validateContactTypeForRole(Contact contact, ContractPartyRole role) {
    switch (contact.getContactType()) {
        case SERVICE_PROVIDER -> throw new BusinessException(
            "Service providers cannot be added as contract parties");
        case INDIVIDUAL -> {
            if (role == ContractPartyRole.CORPORATE_TENANT
                    || role == ContractPartyRole.AUTHORIZED_REPRESENTATIVE) {
                throw new BusinessException(
                    "Individual contacts cannot have role: " + role.getDisplayName());
            }
        }
        case COMPANY -> {
            if (role == ContractPartyRole.PRIMARY_TENANT
                    || role == ContractPartyRole.EXTRA_TENANT) {
                throw new BusinessException(
                    "Company contacts cannot have role: " + role.getDisplayName()
                    + ". Use CORPORATE_TENANT instead.");
            }
        }
    }
}
```

### Quartz Job: ContactFollowUpReminderJob

**Module**: `buurman-notifications`

```java
@Slf4j
@RequiredArgsConstructor
public class ContactFollowUpReminderJob implements Job {
    private final ContactNoteRepository contactNoteRepository;
    private final NotificationCenterService notificationCenterService;
    private final Clock clock;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate today = LocalDate.now(clock);
        List<ContactNote> pendingFollowUps = contactNoteRepository.findPendingFollowUps(today);

        for (ContactNote note : pendingFollowUps) {
            notificationCenterService.createInAppNotification(
                note.getCreatedBy(),
                "Follow-up reminder",
                "You have a follow-up due for a contact note",
                note.getContactId()
            );
            contactNoteRepository.markFollowUpReminderSent(note.getId());
        }

        log.info("Processed {} follow-up reminders", pendingFollowUps.size());
    }
}
```

**Quartz config**: Add to `NotificationQuartzConfig` (or new `ContactQuartzConfig` in buurman-notifications):
- CRON: `0 0 8 * * ?` (daily at 08:00)
- Misfire: `MISFIRE_INSTRUCTION_FIRE_ONCE_NOW`

---

## 8. Controller / API Endpoints

**`TenantController` → `ContactController`** — implements `ContactsApi` (generated)

All paths change from `/tenants/...` to `/contacts/...`.

### Renamed endpoints

| Method | Old Path | New Path | Operation | Roles |
|--------|----------|----------|-----------|-------|
| POST | `/tenants` | `/contacts` | createContact | ADMIN, EDITOR |
| GET | `/tenants` | `/contacts` | getContacts | ALL |
| GET | `/tenants/{id}` | `/contacts/{id}` | getContact | ALL |
| PUT | `/tenants/{id}` | `/contacts/{id}` | updateContact | ADMIN, EDITOR |
| DELETE | `/tenants/{id}` | `/contacts/{id}` | deleteContact | ADMIN |
| GET | `/tenants/{id}/history` | `/contacts/{id}/history` | getContactHistory | ALL |
| GET | `/tenants/{id}/audit-log` | `/contacts/{id}/audit-log` | getContactAuditLog | ALL |
| POST | `/tenants/{id}/documents` | `/contacts/{id}/documents` | uploadDocument | ADMIN, EDITOR |
| GET | `/tenants/{id}/documents` | `/contacts/{id}/documents` | getDocuments | ALL |
| GET | `/tenants/documents/{docId}/download` | `/contacts/documents/{docId}/download` | getDownloadUrl | ALL |
| DELETE | `/tenants/documents/{docId}` | `/contacts/documents/{docId}` | deleteDocument | ADMIN, EDITOR |
| POST | `/tenants/{id}/photos` | `/contacts/{id}/photos` | uploadPhoto | ADMIN, EDITOR |
| GET | `/tenants/{id}/photos` | `/contacts/{id}/photos` | getPhotos | ALL |
| PUT | `/tenants/{id}/photos/{photoId}/set-main` | `/contacts/{id}/photos/{photoId}/set-main` | setMainPhoto | ADMIN, EDITOR |
| POST | `/tenants/{tid}/addresses` | `/contacts/{cid}/addresses` | createAddress | ADMIN, EDITOR |
| GET | `/tenants/{tid}/addresses` | `/contacts/{cid}/addresses` | getAddresses | ALL |
| GET | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | getAddress | ALL |
| PUT | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | updateAddress | ADMIN, EDITOR |
| DELETE | `/tenants/{tid}/addresses/{aid}` | `/contacts/{cid}/addresses/{aid}` | deleteAddress | ADMIN, EDITOR |

### REMOVED endpoints (property linking is through contracts only)

| Method | Path | Reason |
|--------|------|--------|
| ~~POST~~ | ~~/contacts/{id}/link-property~~ | Removed — property association via contracts |
| ~~POST~~ | ~~/contacts/{id}/unlink-property~~ | Removed — same |

### New endpoints

| Method | Path | Operation | Roles | Request → Response |
|--------|------|-----------|-------|--------------------|
| POST | `/contacts/{id}/notes` | createNote | ADMIN, EDITOR | `CreateContactNoteRequest` → `ContactNoteResponse` |
| GET | `/contacts/{id}/notes` | getNotes | ALL | → `List<ContactNoteResponse>` |
| PUT | `/contacts/{cid}/notes/{nid}` | updateNote | ADMIN, EDITOR | `UpdateContactNoteRequest` → `ContactNoteResponse` |
| DELETE | `/contacts/{cid}/notes/{nid}` | deleteNote | ADMIN, EDITOR | → 204 |
| PUT | `/contacts/{cid}/notes/{nid}/pin` | pinNote | ADMIN, EDITOR | → `ContactNoteResponse` |
| PUT | `/contacts/{cid}/notes/{nid}/unpin` | unpinNote | ADMIN, EDITOR | → `ContactNoteResponse` |
| POST | `/contacts/{id}/relationships` | createRelationship | ADMIN, EDITOR | `CreateContactRelationshipRequest` → `ContactRelationshipResponse` |
| GET | `/contacts/{id}/relationships` | getRelationships | ALL | → `List<ContactRelationshipResponse>` |
| PUT | `/contacts/{cid}/relationships/{rid}` | updateRelationship | ADMIN, EDITOR | `UpdateContactRelationshipRequest` → `ContactRelationshipResponse` |
| DELETE | `/contacts/{cid}/relationships/{rid}` | deleteRelationship | ADMIN, EDITOR | → 204 |
| POST | `/contacts/{id}/tags` | addTag | ADMIN, EDITOR | `AddContactTagRequest` → `ContactResponse` |
| DELETE | `/contacts/{id}/tags/{tag}` | removeTag | ADMIN, EDITOR | → `ContactResponse` |
| POST | `/contacts/check-duplicates` | checkDuplicates | ADMIN, EDITOR | `CreateContactRequest` → `DuplicateCheckResponse` |
| GET | `/contacts/{id}/activity` | getActivity | ALL | → `PageResponse<ContactActivityItem>` |
| POST | `/contacts/{id}/erase` | eraseContactData | ADMIN | empty → 204 |

### Contract endpoint renames

- `PUT /contracts/{id}/change-primary-tenant` → `PUT /contracts/{id}/change-primary-contact`
- `ChangePrimaryTenantRequest` → `ChangePrimaryContactRequest`

### Booklet path rename

`/booklets/tenant/{tenantIdentifier}` → `/booklets/contact/{contactIdentifier}`

---

## 9. OpenAPI Changes

### Path file rename

`openapi/src/paths/tenants.yaml` → `openapi/src/paths/contacts.yaml`

### Root file (`openapi/src/app.yaml`)

**Tags**: Rename `Tenants` → `Contacts`

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
| `PageResponseTenantResponse` | `PageResponseContactListItemResponse` |
| `PropertyTenantHistoryResponse` | `PropertyContactHistoryResponse` |
| `LinkTenantToPropertyRequest` | DELETED |
| `ChangePrimaryTenantRequest` | `ChangePrimaryContactRequest` |

**New schemas to add**:
- `ContactListItemResponse` (for list endpoint)
- `ContactNoteResponse`, `CreateContactNoteRequest`, `UpdateContactNoteRequest`
- `ContactRelationshipResponse`, `CreateContactRelationshipRequest`, `UpdateContactRelationshipRequest`
- `ContactActivityItem`
- `AddContactTagRequest`
- `DuplicateCheckResponse`, `DuplicateMatch`
- `ContactNoteIdentifier`, `ContactRelationshipIdentifier`
- Enums: `ContactType`, `InteractionType`, `RelationshipType`, `ContactTag`, `DataRetentionStatus`

**New query parameters on GET `/contacts`**:
- `contactType` (optional, enum)
- `tags` (optional, comma-separated)

**ContractPartyResponse schema**: `tenant` field → `contact` (references `ContactSummary`)

**ContractPartyRole enum**: Add `SIGNER`, `CORPORATE_TENANT`, `AUTHORIZED_REPRESENTATIVE`

**AddContractPartyRequest / UpdateContractPartyRequest**: `tenantIdentifier` → `contactIdentifier`

**CalendarFeed schemas**: `tenantIdentifier` → `contactIdentifier`

**After all edits**: `make bundle-openapi` then `cd frontend && yarn generate:api`

---

## 10. Frontend Changes

### File Renames

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
| All files in `components/tenants/*` | Renamed to `components/contacts/*` with Contact prefix |

### Route Changes

| Old | New |
|-----|-----|
| `/tenants` | `/contacts` |
| `/tenants/new` | `/contacts/new` |
| `/tenants/:id` | `/contacts/:id` |
| `/tenants/:id/edit` | `/contacts/:id/edit` |

### Navigation

Sidebar: "Tenants" → "Contacts"

### 5-Tab Detail Page Structure

**Tab 1: Overview**
- Contact info card (type-aware fields):
  - INDIVIDUAL: name, email, phone, tax number, ID number, date of birth
  - COMPANY: company name, trade name, industry, website, invoice email, contact person
  - SERVICE_PROVIDER: same as COMPANY
- Active contracts & properties section (from `contract_parties` JOIN `contracts` JOIN `properties`)
- Contextual alerts (top 3): overdue follow-ups (red), expiring contracts (30 days), missing required fields
- Follow-ups section: upcoming follow-up dates from contact_notes, overdue in red
- Metadata (expandable): created at, updated at, created by, "View audit log" link

**Tab 2: Activity**
- "New note" form at top: interaction type icon buttons (default NOTE), rich text editor, "Save" button
  - Below editor: "Set date & follow-up" collapsible section (collapsed by default)
- Pinned notes section (pinned = TRUE, shown in "Pinned" card above feed)
- Chronological feed (reverse chrono):
  - Manual notes: interaction type badge, body preview, author, timestamp, pin/edit/delete actions
  - Auto events: compact single-line card, icon + description + timestamp + link. NOT editable.

**Tab 3: Relationships**
- Relationship list: related contact display_name, relationship type label (perspective-aware), notes preview, edit/delete
- "Add relationship" button → form: contact picker + type dropdown + optional notes

**Tab 4: Files**
- Merged documents + photos
- Documents section with upload/download/delete
- Photos section with upload/set-main/delete
- Single "Upload file" action (auto-detect: image → photos, other → documents)

**Tab 5: Addresses**
- Existing address list, unchanged. Map view if coordinates available.

### Contact Card (List View)

Shows:
- Avatar/initials + display_name (heading)
- Contact type badge: "Individual" / "Company" / "Service Provider" (color-coded)
- Tags: max 3 chips + "+N" overflow
- Active property names: max 2 + "+N more"
- Phone (tel:) + Email (mailto:) icon buttons
- Created date (muted)
- **No balance** this sprint (visual space reserved for future)

### ContactForm (type-aware)

- `INDIVIDUAL`: "First Name" (required) / "Last Name" (optional) / show dateOfBirth, idExpiryDate
- `COMPANY`: "Company Name" (required) / "Contact Person First Name" (optional) / "Contact Person Last Name" (optional) / company fields
- `SERVICE_PROVIDER`: "Business Name" (required) / same as company
- Tag picker always visible
- Notes field uses `RichTextEditor` (per project feedback rules)
- **Type change confirmation dialog**: when contactType changes in edit form, show dialog listing fields that will be cleared

### New Frontend Components

| Component | Purpose |
|-----------|---------|
| `ContactNoteList.tsx` | Timeline view of notes/interactions |
| `ContactNoteForm.tsx` | Create/edit note with RichTextEditor, interaction type picker, follow-up date |
| `ContactRelationshipList.tsx` | Display relationships with links to related contacts |
| `ContactRelationshipForm.tsx` | Create/edit relationship (contact picker + type dropdown) |
| `ContactTagPicker.tsx` | Multi-select tag chips (predefined list) |
| `ContactTypeSelector.tsx` | Radio/segmented control for 3 types |
| `ContactDuplicateWarning.tsx` | Warning during create with "Go to existing" / "Create anyway" |
| `ContactQuickAdd.tsx` | Minimal form: name + optional email/phone |
| `ActivityFeed.tsx` | Combined manual notes + auto events feed |
| `ContactTypeChangeDialog.tsx` | Confirmation dialog listing fields to be cleared |

### React Query Hooks

```typescript
// useContactHooks.ts
useContacts(params)          // replaces useTenants
useContact(id)               // replaces useTenant
useCreateContact()           // replaces useCreateTenant
useUpdateContact()           // replaces useUpdateTenant
useDeleteContact()           // replaces useDeleteTenant
useContactNotes(contactId)
useCreateContactNote()
useUpdateContactNote()
useDeleteContactNote()
usePinContactNote()
useUnpinContactNote()
useContactRelationships(contactId)
useCreateContactRelationship()
useUpdateContactRelationship()
useDeleteContactRelationship()
useAddContactTag()
useRemoveContactTag()
useCheckDuplicates()
useContactActivity(contactId, page)
useEraseContactData()
```

### Service Provider Filtering

The `/contacts` list page needs a filter/tab for contact type. Service providers should be visually segregated (separate tab or filter option).

### Contract Party Picker Changes

- Filter to INDIVIDUAL + COMPANY only (exclude SERVICE_PROVIDER)
- When COMPANY selected, default role to CORPORATE_TENANT
- Optional "Add Authorized Representative" sub-flow for companies

---

## 11. GDPR Erase

**Endpoint**: `POST /contacts/{id}/erase` — TEAM_ADMIN only, irreversible.

### Anonymization Rules

| Field | Anonymized Value |
|-------|-----------------|
| `first_name` | `"ERASED"` |
| `last_name` | `NULL` |
| `email` | `NULL` |
| `phone` | `NULL` |
| `tax_number` | `NULL` |
| `id_number` | `NULL` |
| `id_expiry_date` | `NULL` |
| `date_of_birth` | `NULL` |
| `company_name` | `"ERASED ENTITY"` (if non-null) |
| `trade_name` | `NULL` |
| `industry` | `NULL` |
| `website` | `NULL` |
| `invoice_email` | `NULL` |
| `display_name` | `"Erased Contact"` |
| `data_retention_status` | `'ANONYMIZED'` |

**Preserved** (referential integrity + fiscal law):
- `id`, `identifier`, `team_id`, `contact_type`, `created_at`
- All financial records via `contract_parties` → `contracts` → `payments`

### Related Data Cleanup

| Data | Action |
|------|--------|
| `contact_notes` | Set body → "Content erased per GDPR request", subject → NULL |
| `contact_addresses` | Hard delete rows |
| S3 documents | Delete S3 objects, then hard delete DB rows |
| S3 photos | Delete S3 objects, then hard delete DB rows |
| `contact_tags` | Hard delete (CASCADE handles this) |
| `contact_relationships` | Soft delete (set deleted_at) |
| `property_contact_history` | Preserved (contact row still exists but anonymized) |

### Service Implementation

```java
@Transactional
@PreAuthorize("hasRole('TEAM_ADMIN')")
public void eraseContactData(ContactIdentifier identifier, UserAuthentication auth) {
    Contact contact = contactRepository.findByIdentifierAndTeamId(identifier, auth.getTeamId())
        .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

    if (contact.getDataRetentionStatus() == DataRetentionStatus.ANONYMIZED) {
        throw new ConflictException("Contact data has already been erased");
    }

    // 1. Delete S3 objects
    documentService.deleteAllS3ObjectsForEntity("CONTACT", contact.getId());
    photoService.deleteAllS3ObjectsForEntity("CONTACT", contact.getId());

    // 2. Hard delete DB rows for documents, photos, addresses
    documentRepository.hardDeleteByEntityTypeAndEntityId("CONTACT", contact.getId());
    photoRepository.hardDeleteByEntityTypeAndEntityId("CONTACT", contact.getId());
    contactAddressRepository.hardDeleteByContactId(contact.getId());

    // 3. Anonymize notes
    contactNoteRepository.anonymizeByContactId(contact.getId());

    // 4. Soft delete relationships
    contactRelationshipRepository.softDeleteByContactId(contact.getId(), auth.getTeamId());

    // 5. Anonymize the contact record
    contactRepository.anonymize(contact.getId(), auth.getTeamId());

    // 6. Audit log
    auditLogService.log(auth.getTeamId(), auth.getUserId(),
        "CONTACT", contact.getId(),
        "Contact data erased per GDPR request by " + auth.getDisplayName());
}
```

### Frontend

- Location: Contact detail page, "Danger Zone" section (or kebab menu → "Erase personal data")
- **Double confirmation dialog**:
  - Title: "Permanently erase all personal data?"
  - Body: "This will permanently erase all personal data for this contact. Financial records linked via contracts are retained for legal compliance. This action cannot be undone."
  - Input: "Type ERASE to confirm" — submit button disabled until input === "ERASE"
  - Buttons: [Cancel] [Erase Data] (red, destructive)
- After success: redirect to contacts list with toast "Contact data erased"
- Response: 204 on success, 403 if not TEAM_ADMIN, 404 if not found, 409 if already ANONYMIZED

---

## 12. Demo Data Generator

**Module**: `buurman-demo-data`

### Rename

`DemoTenantGenerator` → `DemoContactGenerator`

### Generation Mix (per team)

| Type | % | Count |
|------|---|-------|
| INDIVIDUAL | ~60% | ~30 |
| COMPANY | ~25% | ~12 |
| SERVICE_PROVIDER | ~15% | ~8 |

### INDIVIDUAL Contacts

- Dutch names: Jan, Piet, Klaas, Marieke, Fatima, Mohammed, Sven, Anouk, Daan, Fleur, Bram, Sophie, etc.
- Last names: De Vries, Jansen, Van den Berg, Bakker, Visser, Smit, De Boer, Mulder, De Groot, Bos, etc.
- Email: `firstname.lastname@gmail.com`, `f.lastname@outlook.nl`
- Phone: `+316XXXXXXXX`
- Dutch addresses: Amsterdam, Rotterdam, Den Haag, Utrecht, Eindhoven, Groningen; postal codes `"1234 AB"`; streets: Keizersgracht, Vondelstraat, Oudegracht, etc.

### COMPANY Contacts

```
"Van der Berg Vastgoed B.V."      — Real Estate
"Café De Hoek V.O.F."             — Hospitality
"Jansen Techniek B.V."            — Engineering
"Bakkerij Het Broodje B.V."       — Food & Beverage
"De Wit Administratie B.V."       — Accounting
"Hollands Schoon B.V."            — Cleaning Services
"Rotterdam Logistics B.V."        — Transport & Logistics
"Bloemen Van Dijk V.O.F."         — Retail
"Digitaal Bureau Amsterdam B.V."  — IT & Digital
"Groen & Tuin B.V."               — Landscaping
"Makelaardij Prins B.V."          — Real Estate
"Het Gouden Ei V.O.F."            — Hospitality
```

Each has: KvK number (8 digits), contact person, invoice email (`factuur@...`), website

### SERVICE_PROVIDER Contacts

```
"Loodgietersbedrijf Smit"        — plumber
"Elektra Amsterdam"               — electrician
"Schildersbedrijf De Vries"       — painter
"Slotenmaker 24/7 Utrecht"        — locksmith
"Dakdekkersbedrijf Jansen"        — roofer
"Timmerwerk Van Dijk"             — carpenter
"Schoonmaakbedrijf Blitz"        — cleaning
"CV Montage Rotterdam"            — heating/HVAC
```

Each has: contact person, business phone (`+3120XXXXXXX`), email (`info@...`)

### Per-Contact Generated Data

| Data | Distribution |
|------|-------------|
| Notes | 1-3 per contact, realistic interaction types |
| Tags | 1-3 (weighted: LONG_TERM ~40%, PROSPECT ~20%, VIP ~5%, LATE_PAYER ~10%, KEY_HOLDER ~10%, REFERRED ~10%, DO_NOT_CONTACT ~2%, FORMER_TENANT ~3%) |
| Relationships | 0-2 (guarantor pairs, WORKS_FOR/CONTACT_PERSON_FOR for company↔individual) |
| Addresses | 0-2 (Dutch cities, real postal codes) |
| Follow-ups | ~20% of notes have follow_up_date (mix of overdue + upcoming 14 days) |

### Note Content Examples

- `PHONE_CALL`: "Called regarding lease renewal. Tenant confirmed they want to extend for another year."
- `MEETING`: "Met at the property for annual inspection. Kitchen faucet leaking — scheduled repair."
- `VIEWING`: "Showed apartment 2B to prospective tenant. Very interested."
- `INSPECTION`: "Quarterly inspection completed. Property in good condition."
- `NOTE`: "Received email about parking space allocation."
- `KEY_HANDOVER`: "Key handover completed. All keys accounted for. Meter readings documented."

### DemoDataContext Update

Add: `List<Contact> generatedContacts;` and `Map<UUID, List<ContactTag>> contactTags;`

### DemoDataService Update

Call `DemoContactGenerator` instead of `DemoTenantGenerator`. Ensure contacts generated before contracts.

---

## 13. Backoffice Rename

**Module**: `buurman-backoffice`

Mechanical rename of all tenant references:

1. Controllers: `BackofficeTenantController` → `BackofficeContactController`, path `/backoffice/tenants` → `/backoffice/contacts`
2. Services: `BackofficeTenantService` → `BackofficeContactService`
3. Repositories: tenant repos → contact repos
4. DTOs: update request/response types
5. OpenAPI (`openapi/src/paths/backoffice-*.yaml`): rename tenant paths and schemas

**Checklist**: Search all files in `buurman-backoffice/` for `tenant`/`Tenant`, rename classes/methods/variables/SQL references.

---

## 14. Booklet / Export Updates

**Module**: `buurman-booklets`

### Path Rename

`/booklets/tenant/{tenantIdentifier}` → `/booklets/contact/{contactIdentifier}`

Update `BookletController` path variable type: `TenantIdentifier` → `ContactIdentifier`

### PDF Exporter (A4 Professional Layout)

1. **Header**: Buurman logo (left), team name (right), export date, horizontal rule
2. **Contact Overview** (type-aware):
   - INDIVIDUAL: full name (large), type badge, two-column grid (email, phone, tax, ID, DOB), tag chips
   - COMPANY: company name (large), trade name (subtitle), type badge, grid (industry, website, invoice email, contact person)
   - SERVICE_PROVIDER: business name (large), type badge, grid (contact person, phone, email)
3. **Addresses**: type label, formatted multi-line
4. **Active Contracts**: table (Property | Role | Start | End | Status)
5. **Notes**: interaction type badge, date, author, body (first 500 chars), max 20 most recent
6. **Relationships**: table (Related Contact | Relationship | Notes)
7. **Footer**: "Generated by Buurman on [date]", page numbers

Typography: professional sans-serif (Helvetica), Buurman brand color for headings, 11pt body / 14pt headings.

### CSV Exporter Update

Columns:
```
identifier, contact_type, display_name, first_name, last_name, company_name,
trade_name, industry, email, phone, tax_number, id_number, id_expiry_date,
date_of_birth, website, invoice_email, tags, active_contract_count,
data_retention_status, created_at, updated_at
```

- `tags` column: comma-separated (e.g., `"VIP,LONG_TERM"`)
- `contact_type`: `INDIVIDUAL`, `COMPANY`, or `SERVICE_PROVIDER`

---

## 15. PostHog Analytics

### Events (frontend-side)

| Event | Properties | Trigger |
|-------|-----------|---------|
| `CONTACT_CREATED` | `{ contactType, source: "full_form" \| "quick_add" }` | After creation |
| `CONTACT_TYPE_CHANGED` | `{ fromType, toType }` | After type change update |
| `CONTACT_NOTE_CREATED` | `{ interactionType, hasFollowUp: boolean }` | After note creation |
| `CONTACT_RELATIONSHIP_CREATED` | `{ relationshipType }` | After relationship creation |
| `CONTACT_TAG_ADDED` | `{ tag }` | After tag addition |
| `CONTACT_TAG_REMOVED` | `{ tag }` | After tag removal |
| `CONTACT_QUICK_ADD_USED` | `{}` | Quick-add form used |
| `CONTACT_DUPLICATE_WARNING_SHOWN` | `{ matchCount }` | Duplicate check returns >0 |
| `CONTACT_DUPLICATE_WARNING_IGNORED` | `{}` | User proceeds despite warning |
| `SERVICE_PROVIDER_TAB_VIEWED` | `{}` | Service provider filter selected |
| `CONTACT_DATA_ERASED` | `{ contactType }` | After GDPR erase |

### KPIs (first 60 days)

- % teams with ≥1 COMPANY contact
- % teams with ≥1 SERVICE_PROVIDER
- Avg notes per contact (target: >1.5)
- Quick-add vs full-form ratio (target: >30% quick-add)
- Tag adoption: % contacts with ≥1 tag

---

## 16. Empty States

| Location | Icon | Title | Body | CTA |
|----------|------|-------|------|-----|
| Contact list (no contacts) | People outline | "No contacts yet" | "Add your tenants, companies, and service providers to keep everything organized." | "Add first contact" → create form |
| Service providers filter (empty) | Wrench outline | "No service providers yet" | "Add plumbers, electricians, and other professionals you work with." | "Add service provider" → create form (type=SERVICE_PROVIDER) |
| Activity tab (no activity) | Clock outline | "No activity yet" | "Activity will appear here as you interact with this contact." | "Add a note" → focus note form |
| Notes inline prompt | — | — | "Record your first interaction — a phone call, meeting, or just a note." | — |
| Relationships tab (empty) | Link outline | "No relationships yet" | "Connect this contact to other people or companies in your system." | "Add relationship" |
| Files tab (empty) | Folder outline | "No files yet" | "Upload contracts, ID copies, photos, and other documents." | "Upload file" |
| Addresses tab (empty) | Map pin outline | "No addresses yet" | "Add a current address, mailing address, or office location." | "Add address" |
| Tags (none assigned) | — | No empty state | Show tag picker with all options unselected | — |
| Follow-ups (none) | — | Inline prompt | "Set a follow-up date to get reminded about this contact." | — |

---

## 17. Cross-Cutting Concerns

### Files that reference "tenant" (grep blast radius)

**Backend core (`buurman/`)**:
- `ContractService.java` — references TenantRepository, TenantMapper, creates ContractParty with tenantId
- `ContractMapper.java` — maps TenantSummary
- `PropertyService.java` — references TenantRepository
- `AuditLogRepository.java` — `"TENANT"` entity type string

**Demo data (`buurman-demo-data/`)**:
- `DemoTenantGenerator.java`, `DemoContractGenerator.java`, `DemoNotificationGenerator.java`, `DemoDocumentGenerator.java`, `DemoAuditLogGenerator.java`, `DemoExpenseGenerator.java`, `DemoPaymentInstructionGenerator.java`, `DemoDataContext.java`, `DemoDataService.java`, `ContractExtensionDemoDataGenerator.java`

**Notifications (`buurman-notifications/`)**:
- `NotificationRepository.java`, `NotificationRecordMapper.java`, Thymeleaf templates

**Takeout (`buurman-takeout/`)**:
- `DataTakeoutRepository.java`

**Booklets (`buurman-booklets/`)**:
- `BookletController.java`, exporter classes

**Backoffice (`buurman-backoffice/`)**:
- Any tenant references in controllers/services/repos

### Naming / Localization

- Ship with "Contacts" globally
- Sidebar label: "Contacts" (was "Tenants")
- Page titles: "Contacts", "Contact Details", "New Contact"
- A/B copy test planned within 30 days post-launch

### Audit Trail

Update all `"TENANT"` → `"CONTACT"` strings in audit calls.

### Metrics

Rename `tenant.*` → `contact.*` counters/gauges.

---

## 18. Implementation Phases

### Phase 1: Database + JOOQ Regen (1 day)

1. V038 migration already committed — verify it compiles
2. Write and apply V039 migration (already written to repo)
3. `cd backend && mvn generate-sources -pl jooq -am`
4. Verify generated code compiles

### Phase 2: Backend Core Rename (2-3 days)

1. New enums: `ContactType`, `InteractionType`, `RelationshipType` (6 types + inverse methods), `ContactTag` (8 tags), `DataRetentionStatus`
2. New typed identifiers: `ContactIdentifier`, `ContactAddressIdentifier`, `ContactNoteIdentifier`, `ContactRelationshipIdentifier`
3. Delete `TenantIdentifier`, `TenantAddressIdentifier`
4. Rename domain classes: `Tenant` → `Contact`, `TenantAddress` → `ContactAddress`, `PropertyTenantHistory` → `PropertyContactHistory`
5. Update field renames on `ContractParty`, `Notification`, `CalendarFeed`
6. Rename DTOs — `CreateContactRequest` with `Optional<String> firstName`
7. Add `ContactListItemResponse` DTO
8. Update `EntityPrefix` + `SidGenerator`
9. Rename repositories + update queries
10. Rename mappers
11. Rename services — **remove** `linkContactToProperty` / `unlinkContactFromProperty`
12. Add type change field clearing to `ContactService.updateContact()`
13. Add contract party validation by contact type to `ContractPartyService`
14. Rename controller — remove link/unlink endpoints
15. Fix all compilation errors across all 10 modules
16. Build: `mvn clean install -DskipTests`

### Phase 3: New Features (2-3 days)

1. New domain classes: `ContactNote` (with `pinned`), `ContactRelationship`
2. New repositories: `ContactNoteRepository`, `ContactRelationshipRepository` (bidirectional), `ContactTagRepository` (batch `findByContactIdsGrouped`)
3. New services: `ContactNoteService` (with pin/unpin), `ContactRelationshipService` (with inverse display)
4. `ContactActivityRepository` — UNION ALL activity feed query
5. `ContactFollowUpReminderJob` in `buurman-notifications`
6. Quartz config for follow-up reminder job
7. Duplicate detection logic, display name computation
8. Contact list query with JOIN for `activeContractCount`
9. Controller: new endpoints (notes, relationships, tags, duplicates, activity feed, pin/unpin, erase)
10. Build + verify

### Phase 4: OpenAPI + Frontend (3 days)

1. Rename `openapi/src/paths/contacts.yaml` — remove link/unlink, add activity + erase endpoints
2. Update `openapi/src/app.yaml` — all schema renames + new schemas
3. `make bundle-openapi`
4. `cd frontend && yarn generate:api`
5. Rename all frontend files
6. 5-tab detail page layout
7. Activity feed component (manual notes + auto events)
8. Note form with collapsed date section
9. Contact card (no balance, reserved space)
10. Empty states for all sections
11. Type change confirmation dialog
12. Contract party picker filtering by contact type
13. PostHog event tracking (11 events)
14. Duplicate warning component
15. Quick-add form
16. GDPR erase dialog (double confirmation with "ERASE" input)
17. Update routes + navigation
18. Build + verify: `cd frontend && yarn build`

### Phase 5: Polish + Remaining Items (2-3 days)

**Day 1: Backend additions**
1. Repository methods for GDPR: `hardDeleteByEntityTypeAndEntityId` (documents, photos), `hardDeleteByContactId` (addresses), `anonymizeByContactId` (notes), `anonymize` (contact)
2. S3 object deletion service methods
3. `DemoContactGenerator` rewrite with Dutch data
4. Backoffice rename: controllers, services, repos, OpenAPI paths
5. Booklet path rename + CSV exporter update

**Day 2: Frontend + PDF**
6. Professional contact PDF export (iText7, type-aware layout)
7. Backoffice frontend updates
8. OpenAPI schema additions for erase + booklet paths

**Day 3: Integration + polish**
9. Audit trail: `"TENANT"` → `"CONTACT"` strings
10. Metrics: `tenant.*` → `contact.*`
11. Verify empty states, follow-up reminder job, GDPR erase flow, demo data
12. Full E2E test: create contact (all 3 types), add notes, relationships, tags, check activity feed, change type, verify field clearing
13. Bundle OpenAPI: `make bundle-openapi`
14. Code review + PR

**Total estimate: 10-13 working days**

---

## 19. Follow-Up Issues (Out of Scope)

| # | Issue | ID | Notes |
|---|-------|----|-------|
| 1 | CSV Import for Contacts | BUUR-82 | Bulk import with duplicate detection |
| 2 | Tenant Portal | Future epic | Login via email, no `keycloak_user_id` needed |
| 3 | Contact Balance Display | BUUR-83 | Outstanding payment aggregation on card |
