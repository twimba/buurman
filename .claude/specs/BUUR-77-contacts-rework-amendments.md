# BUUR-77: Contacts Rework — Spec Amendments

**Status**: FINAL — Applies on top of `BUUR-77-contacts-rework.md`
**Date**: 2026-03-21
**Branch**: `luissantos/buur-77-rework-the-tenants-feature`
**Context**: Post-review amendments. All blocking issues resolved, all iterate-later items addressed.

---

## How to Use This Document

This document amends the original spec (`BUUR-77-contacts-rework.md`). Where a section here contradicts the original, **this document takes precedence**. Sections not mentioned here remain unchanged from the original spec.

---

## Table of Contents

1. [Migration Patch (V039)](#1-migration-patch-v039)
2. [Enum Changes](#2-enum-changes)
3. [Removed Endpoints: Link/Unlink Property](#3-removed-endpoints-linkunlink-property)
4. [Contact Type Change Field Clearing](#4-contact-type-change-field-clearing)
5. [Contract Party Validation by Contact Type](#5-contract-party-validation-by-contact-type)
6. [Detail Page: 5-Tab Structure](#6-detail-page-5-tab-structure)
7. [Activity Tab: Auto-Generated Events](#7-activity-tab-auto-generated-events)
8. [Contact Card (List View) Design](#8-contact-card-list-view-design)
9. [ContactListItemResponse (N+1 Fix)](#9-contactlistitemresponse-n1-fix)
10. [Follow-Up Reminder Job (Ships This Sprint)](#10-follow-up-reminder-job-ships-this-sprint)
11. [Note Form UX: Collapsed Date Section](#11-note-form-ux-collapsed-date-section)
12. [Empty States](#12-empty-states)
13. [Inverse Relationships: Single Record, Dual Display](#13-inverse-relationships-single-record-dual-display)
14. [Display Name Computation](#14-display-name-computation)
15. [CreateContactRequest Validation](#15-createcontactrequest-validation)
16. [PostHog Analytics Events](#16-posthog-analytics-events)
17. [Naming & Localization](#17-naming--localization)
18. [Updated Follow-Up Issues](#18-updated-follow-up-issues)
19. [Updated Implementation Order](#19-updated-implementation-order)
20. [Spec v3 Amendments](#20-spec-v3-amendments)
    - 20.1 [GDPR Erase Endpoint](#201-gdpr-erase-endpoint-ships-this-sprint)
    - 20.2 [Remove keycloak_user_id](#202-remove-keycloak_user_id-from-contacts)
    - 20.3 [Duplicate Detection Scope](#203-duplicate-detection-scope-clarification)
    - 20.4 [Demo Data Generator](#204-demo-data-generator-ships-this-sprint)
    - 20.5 [Backoffice Contact Management](#205-backoffice-contact-management-ships-this-sprint)
    - 20.6 [Booklet/Export Updates](#206-bookletexport-updates-ships-this-sprint)
    - 20.7 [Updated Follow-Up Issues](#207-updated-follow-up-issues-replaces-section-18)
    - 20.8 [Updated Implementation Phases](#208-updated-implementation-phases-replaces-section-19-phase-5)

---

## 1. Migration Patch (V039)

V038 is already committed and correct for the base schema. A new V039 migration applies the tag and relationship type refinements from this amendment.

**File**: `backend/jooq/src/main/resources/db/migration/V039__contacts_amendments.sql`

```sql
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
-- 2. Refine contact_relationships CHECK: 8 types -> 6+1 types
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
```

After applying: `cd backend && mvn generate-sources -pl jooq -am`

---

## 2. Enum Changes

### ContactTag (replaces original spec)

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

Removed from original: `PROBLEM`, `COMMERCIAL`, `RESIDENTIAL`, `PREFERRED_VENDOR`.
Added: `KEY_HOLDER`, `FORMER_TENANT`.

### RelationshipType (replaces original spec)

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
     * Returns the inverse relationship label for display on the target contact's page.
     * Symmetric types return themselves. OTHER has no structured inverse.
     */
    public RelationshipType inverse() {
        return switch (this) {
            case GUARANTOR_FOR -> GUARANTOR_FOR;  // displayed as "Guaranteed by" via inverseDisplayName()
            case FAMILY_OF -> FAMILY_OF;
            case PARTNER_OF -> PARTNER_OF;
            case WORKS_FOR -> WORKS_FOR;          // displayed as "Employer of" via inverseDisplayName()
            case CONTACT_PERSON_FOR -> CONTACT_PERSON_FOR; // displayed as "Has contact person" via inverseDisplayName()
            case OTHER -> OTHER;
        };
    }

    /**
     * Returns the display name when viewing this relationship from the target contact's perspective.
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

Key changes from original:
- `EMPLOYEE_OF` renamed to `WORKS_FOR` (clearer from the individual's perspective).
- `LEGAL_REPRESENTATIVE_OF`, `PARENT_OF`, `CHILD_OF` removed (can be added later via migration).
- `OTHER` added with freetext notes for context.
- `inverse()` returns the same enum value; use `inverseDisplayName()` for the display label on the target side.
- No `Optional` return on `inverse()` -- every type has an inverse display.

### ContractPartyRole (unchanged values, add validation context)

The enum values are unchanged from the original spec. The new contract party validation rules (Issue 8) are enforced in `ContractPartyService`, not in the enum itself. See [Section 5](#5-contract-party-validation-by-contact-type).

---

## 3. Removed Endpoints: Link/Unlink Property

**DELETE the following from the original spec's endpoint table (Section 7):**

| Method | Path | Reason |
|--------|------|--------|
| POST | `/contacts/{id}/link-property` | Property association is exclusively through contracts |
| POST | `/contacts/{id}/unlink-property` | Same |

**DELETE from service layer (Section 6):**
- `ContactService.linkContactToProperty()` -- remove entirely
- `ContactService.unlinkContactFromProperty()` -- remove entirely

**DELETE from DTOs (Section 4):**
- `LinkContactToPropertyRequest` -- remove entirely

**DELETE from OpenAPI (Section 8):**
- Remove `link-property` and `unlink-property` path entries from `contacts.yaml`
- Remove `LinkContactToPropertyRequest` schema from `app.yaml`

**DELETE from frontend hooks:**
- `useLinkContactToProperty()` -- remove
- `useUnlinkContactFromProperty()` -- remove

**What replaces this workflow:**
- "Assign contact to property" = Create a contract with the contact as a party
- The contact detail page "Overview" tab shows active properties derived from `contract_parties`
- The `property_contact_history` table remains for historical audit trail

**Impact on `PropertyContactHistoryRepository`:**
- The repository stays -- it is read-only for displaying history
- No new records are written to `property_contact_history` via link/unlink
- Future: consider whether contract creation should auto-insert a history row (follow-up issue)

---

## 4. Contact Type Change Field Clearing

Add to `ContactService.updateContact()` (amends Section 6):

When the `contactType` in the update request differs from the existing contact's type, apply these clearing rules **before** saving:

**FROM INDIVIDUAL TO COMPANY or SERVICE_PROVIDER:**
- `dateOfBirth` -> cleared (set to `Optional.empty()`)
- `idNumber` -> cleared
- `idExpiryDate` -> cleared
- `firstName` -> kept (becomes "contact person")
- `lastName` -> kept (becomes "contact person last name")
- Validation: `companyName` is required in the request

**FROM COMPANY or SERVICE_PROVIDER TO INDIVIDUAL:**
- `companyName` -> cleared
- `tradeName` -> cleared
- `industry` -> cleared
- `website` -> cleared
- `invoiceEmail` -> cleared
- Validation: `firstName` is required in the request

**Implementation in `ContactService`:**

```java
private Contact applyTypeChangeClearing(Contact existing, UpdateContactRequest request) {
    if (existing.getContactType() == request.contactType()) {
        return existing; // no type change, no clearing needed
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

**Frontend confirmation dialog:**

When the user changes the contact type in the edit form, show a confirmation dialog before submitting:

> **Change contact type?**
>
> Changing from [Individual] to [Company] will clear the following fields:
> - Date of birth
> - ID number
> - ID expiry date
>
> This cannot be undone. Continue?
>
> [Cancel] [Change Type]

Track event: `CONTACT_TYPE_CHANGED` with `{ fromType, toType }`.

---

## 5. Contract Party Validation by Contact Type

Add a new validation in `ContractPartyService` (amends Section 6 of original spec):

**Allowed roles by ContactType:**

| ContactType | Allowed ContractPartyRoles |
|-------------|---------------------------|
| INDIVIDUAL | PRIMARY_TENANT, GUARANTOR, COSIGNER, EXTRA_TENANT, SIGNER |
| COMPANY | CORPORATE_TENANT, AUTHORIZED_REPRESENTATIVE, SIGNER, GUARANTOR |
| SERVICE_PROVIDER | *cannot be a contract party at all* |

**Validation logic in `ContractPartyService.addContractParty()`:**

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

**Frontend contract party picker changes:**
- Filter contact picker to INDIVIDUAL + COMPANY only (exclude SERVICE_PROVIDER)
- When a COMPANY contact is selected, default the role dropdown to `CORPORATE_TENANT`
- When a COMPANY is selected, offer an optional "Add Authorized Representative" sub-flow (selects a second INDIVIDUAL contact with role `AUTHORIZED_REPRESENTATIVE`)

**Contract endpoint rename:**
- `PUT /contracts/{id}/change-primary-tenant` -> `PUT /contracts/{id}/change-primary-contact`
- `ChangePrimaryTenantRequest` -> `ChangePrimaryContactRequest`
- Update OpenAPI path and schema accordingly
- Frontend label stays "Change Primary Tenant" for familiar terminology

---

## 6. Detail Page: 5-Tab Structure

**Replaces Section 9's detail page structure entirely (was 9 tabs, now 5).**

### Tab 1: Overview

Content:
- **Contact info card** -- type-aware fields:
  - INDIVIDUAL: name, email, phone, tax number, ID number, date of birth
  - COMPANY: company name, trade name, industry, website, invoice email, contact person (first/last name)
  - SERVICE_PROVIDER: same as COMPANY
- **Addresses** section (inline, not a separate tab) -- NO. Addresses remain a separate tab. See below.
- **Active contracts & properties** section -- derived from `contract_parties` JOIN `contracts` JOIN `properties`. Shows: property name, contract status, role, date range.
- **Contextual alerts** (top 3):
  - Overdue follow-ups (red)
  - Expiring contracts (within 30 days)
  - Missing required fields (e.g., no email, no phone)
  - "See all" link if >3 alerts
- **Follow-ups** section -- upcoming follow-up dates from `contact_notes`, with note preview. Overdue ones in red.
- **Metadata** (expandable/collapsible) -- created at, updated at, created by, updated by. "View audit log" link opens expandable audit log section.

### Tab 2: Activity

The merged timeline tab. Renamed from "Notes & Timeline" to **"Activity"**.

Content:
- **"New note" form** at the top -- interaction type picker (icon buttons, default NOTE), rich text editor, "Save" button. Below editor: "Set date & follow-up" collapsible section (see [Section 11](#11-note-form-ux-collapsed-date-section)).
- **Pinned notes section** -- notes with `pinned = TRUE`, shown in a "Pinned" card above the feed. Each pinned note has an unpin action.
- **Chronological feed** -- all events in reverse chronological order:
  - Manual notes (from `contact_notes`) -- shown with interaction type badge, body preview, author, timestamp. Pin/unpin action. Edit/delete actions.
  - Auto-generated events (from UNION ALL query, see [Section 7](#7-activity-tab-auto-generated-events)) -- shown as compact event cards with icon, description, timestamp. Not editable.

### Tab 3: Relationships

Content:
- **Relationship list** -- each row shows: related contact avatar + display name, relationship type label (from source perspective) or inverse label (from target perspective), notes preview, actions (edit, delete).
- **"Add relationship" button** -- opens form: contact picker + relationship type dropdown + optional notes.

### Tab 4: Files

Merged documents + photos.

Content:
- **Documents section** -- existing document list with upload, download, delete actions.
- **Photos section** -- existing photo gallery with upload, set-main, delete actions.
- Single "Upload file" action that detects file type (image -> photos, other -> documents).

### Tab 5: Addresses

Existing address list functionality, unchanged. Map view if coordinates available.

### Removed as standalone tabs (merged into the above):
- "Properties" -> merged into Overview (active contracts section)
- "History" -> merged into Activity (auto-generated events from `property_contact_history`)
- "Audit Log" -> expandable section in Overview metadata
- "Photos" -> merged into Files
- "Documents" -> merged into Files

---

## 7. Activity Tab: Auto-Generated Events

The Activity tab includes auto-generated events alongside manual notes. This is a **read-only computed aggregation**, not a separate events table.

### Backend: Activity Feed Endpoint

**New endpoint** (add to Section 7):

| Method | Path | Operation | Roles |
|--------|------|-----------|-------|
| GET | `/contacts/{id}/activity` | getContactActivity | ALL |

**Query parameters:**
- `page` (int, default 0)
- `size` (int, default 25)

**Response**: `PageResponse<ContactActivityItem>`

**New DTO:**

```java
public record ContactActivityItem(
    String eventType,       // "NOTE", "CONTRACT_CREATED", "CONTRACT_ACTIVATED",
                            // "CONTRACT_TERMINATED", "CONTRACT_EXPIRED",
                            // "PAYMENT_RECEIVED", "PAYMENT_OVERDUE",
                            // "DOCUMENT_UPLOADED", "NOTIFICATION_SENT",
                            // "CONTACT_UPDATED"
    Instant occurredAt,
    String description,     // human-readable summary, e.g. "Contract CON01ABC... activated"
    Optional<Sid> relatedEntityIdentifier,  // link to the related entity
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

### Repository: UNION ALL Query

**New method in `ContactRepository` or dedicated `ContactActivityRepository`:**

```java
PageResponse<ContactActivityItem> findActivityByContactId(
    UUID contactId, UUID teamId, PageRequest pageRequest);
```

The query performs a UNION ALL across 6 sources:

1. **contact_notes** -- `SELECT 'NOTE', occurred_at, body, identifier, ...`
2. **audit_log WHERE entity_type = 'CONTACT' AND entity_id = contact.id** -- `SELECT 'CONTACT_UPDATED', created_at, description, ...`
3. **contracts via contract_parties** -- `SELECT CASE status ... END, contracts.created_at, ...` for contract lifecycle events (CREATED, ACTIVATED, TERMINATED, EXPIRED)
4. **payments via contract -> contract_parties** -- `SELECT CASE WHEN overdue THEN 'PAYMENT_OVERDUE' ELSE 'PAYMENT_RECEIVED' END, ...`
5. **documents WHERE entity_type = 'CONTACT' AND entity_id = contact.id** -- `SELECT 'DOCUMENT_UPLOADED', created_at, title, ...`
6. **notifications WHERE recipient_contact_id = contact.id** -- `SELECT 'NOTIFICATION_SENT', created_at, subject, ...`

All branches: `ORDER BY occurred_at DESC LIMIT :size OFFSET :page * :size`

Performance: For <200 contacts per team with <50 events each, this is <10ms. No materialized view needed.

### Frontend: Activity Feed Component

The `ActivityFeed` component renders:
- NOTE events: full note card with interaction type badge, body (rich text), author, pin/edit/delete actions
- Auto events: compact single-line card with icon + description + timestamp + optional link to related entity

Auto events are **not editable or deletable** -- they are system-generated.

---

## 8. Contact Card (List View) Design

**Replaces the card description in Section 9.**

The contact card in the list view shows:

- **Avatar / initials** + **display_name** (as main heading)
- **Contact type badge**: "Individual" / "Company" / "Service Provider" (color-coded)
- **Tags**: max 3 tag chips + "+N" overflow indicator
- **Active property names**: derived from contracts, max 2 + "+N more"
- **Phone** (tel: link) + **Email** (mailto: link) as icon buttons
- **Created date** (subtle, muted text)

**Removed from card for this sprint:**
- `outstandingBalance` -- not computed yet, deferred to follow-up issue
- Layout reserves visual space on the right side for the balance field so adding it later does not require a layout redesign

---

## 9. ContactListItemResponse (N+1 Fix)

**New DTO (add to Section 4):**

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

**Usage:**
- The `GET /contacts` list endpoint returns `PageResponse<ContactListItemResponse>` (NOT `PageResponse<ContactResponse>`)
- The full `ContactResponse` is only used on the detail page (`GET /contacts/{id}`)

**Repository query changes:**
- The list query uses a single LEFT JOIN on `contract_parties` + `contracts` with GROUP BY to compute `activeContractCount` per contact, avoiding N+1
- Tags are batch-fetched via `ContactTagRepository.findByContactIdsGrouped(contactIds)` for all contacts in the current page
- `mainPhotoThumbnailUrl` is fetched via a single batch query on `photos` WHERE `entity_type = 'CONTACT' AND is_main = TRUE AND entity_id IN (:contactIds)`

**OpenAPI schema update:**
- Rename `PageResponseContactResponse` to `PageResponseContactListItemResponse` for the list endpoint
- Add `ContactListItemResponse` schema to `app.yaml`
- The list endpoint's 200 response references `PageResponseContactListItemResponse`

**Mapper:**
- New method in `ContactMapper`: `ContactListItemResponse toListItem(Contact contact, List<ContactTag> tags, int activeContractCount, Optional<String> mainPhotoThumbnailUrl)`

---

## 10. Follow-Up Reminder Job (Ships This Sprint)

**Moves from Follow-up Issue #5 to in-scope for this sprint.**

**Module**: `buurman-notifications` (alongside existing `ContractExpiryCheckJob` and `PaymentReminderCheckJob`)

### ContactFollowUpReminderJob

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
                note.getCreatedBy(),  // notify the note creator
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

### Quartz Configuration

Add to `NotificationQuartzConfig` (or new `ContactQuartzConfig` in `buurman-notifications`):

- Job: `ContactFollowUpReminderJob`
- Trigger: CRON `0 0 8 * * ?` (daily at 08:00)
- Misfire instruction: `MISFIRE_INSTRUCTION_FIRE_ONCE_NOW`

### Frontend: Overdue Follow-Up Indicators

- On the contact Overview tab, overdue follow-ups (past date, `follow_up_reminder_sent = FALSE` or `TRUE` but follow-up not "completed") show in red
- On the note card in the Activity feed, a follow-up badge shows: green for future date, red for overdue
- No explicit "complete follow-up" action in this sprint -- the reminder fires once and the user is expected to add a new note documenting the follow-up outcome

### ContactNote domain class amendment

Add to `ContactNote.java`:

```java
private boolean pinned;  // new field from V039
```

### ContactNoteRepository amendment

Add:
```java
void markFollowUpReminderSent(UUID noteId);
void setPinned(UUID noteId, UUID teamId, boolean pinned);
```

### New endpoints for pin/unpin

| Method | Path | Operation | Roles |
|--------|------|-----------|-------|
| PUT | `/contacts/{cid}/notes/{nid}/pin` | pinNote | ADMIN, EDITOR |
| PUT | `/contacts/{cid}/notes/{nid}/unpin` | unpinNote | ADMIN, EDITOR |

Both return `ContactNoteResponse`.

---

## 11. Note Form UX: Collapsed Date Section

**Amends the note form described in Section 9.**

The note creation/edit form layout:

1. **Interaction type buttons** (icon row, default: NOTE)
   - `[ Note ] [ Call ] [ Meeting ] [ Handover ] [ Inspection ] [ Viewing ] [ Other ]`
   - Selected type is visually highlighted. One click to change.
2. **Rich text editor** (body) -- uses `RichTextEditor` component per project feedback rules
3. **"Save" button**
4. **"Set date & follow-up" link** (subtle, below editor) -- click to expand:
   - `occurred_at` date picker (defaults to today, label: "Date of interaction")
   - `follow_up_date` date picker (optional, label: "Follow-up date")
   - Collapsed by default for fast note-taking

When a note is saved with a `follow_up_date`, the note card in the Activity feed shows a follow-up badge.

The saved note in the Activity feed shows the selected `interactionType` as a badge/icon.

---

## 12. Empty States

Add these empty state definitions for the frontend. Each has: icon, title, body text, and CTA button.

### Contacts list page (no contacts)
- **Icon**: People outline
- **Title**: "No contacts yet"
- **Body**: "Add your tenants, companies, and service providers to keep everything organized."
- **CTA**: "Add first contact" -> opens create form

### Service providers tab/filter (no service providers)
- **Icon**: Wrench outline
- **Title**: "No service providers yet"
- **Body**: "Add plumbers, electricians, and other professionals you work with."
- **CTA**: "Add service provider" -> opens create form with `contactType` pre-set to `SERVICE_PROVIDER`

### Activity tab (no activity)
- **Icon**: Clock outline
- **Title**: "No activity yet"
- **Body**: "Activity will appear here as you interact with this contact -- notes, contracts, payments, and more."
- **CTA**: "Add a note" -> focuses the note form

### Notes section within Activity (no manual notes, but auto-events exist)
- **Inline prompt** (not a full empty state): "Record your first interaction -- a phone call, meeting, or just a note."

### Relationships tab (no relationships)
- **Icon**: Link outline
- **Title**: "No relationships yet"
- **Body**: "Connect this contact to other people or companies in your system."
- **CTA**: "Add relationship" -> opens relationship form

### Files tab (no documents or photos)
- **Icon**: Folder outline
- **Title**: "No files yet"
- **Body**: "Upload contracts, ID copies, photos, and other documents."
- **CTA**: "Upload file" -> opens upload dialog

### Addresses tab (no addresses)
- **Icon**: Map pin outline
- **Title**: "No addresses yet"
- **Body**: "Add a current address, mailing address, or office location."
- **CTA**: "Add address" -> opens address form

### Tags (no tags assigned)
- No empty state needed. Show the tag picker with all options unselected.

### Follow-ups section in Overview (no follow-ups)
- **Inline prompt**: "Set a follow-up date to get reminded about this contact."

---

## 13. Inverse Relationships: Single Record, Dual Display

**Amends the relationship handling in Section 5 (repository) and Section 6 (service).**

### Storage model

One record per relationship. No inverse record is stored.

Example: A works for B.
- Record: `source_contact_id = A, target_contact_id = B, relationship_type = WORKS_FOR`
- Display on A's page: "Works for B"
- Display on B's page: "Employer of A" (via `RelationshipType.inverseDisplayName()`)

For symmetric types (FAMILY_OF, PARTNER_OF): one record, displayed identically on both sides.

For OTHER: display the relationship notes on both sides as context.

### ContactRelationshipRepository.findByContactIdAndTeamId

This query must return relationships where the contact is **either** source OR target:

```sql
SELECT * FROM contact_relationships
WHERE team_id = :teamId
  AND deleted_at IS NULL
  AND (source_contact_id = :contactId OR target_contact_id = :contactId)
ORDER BY created_at DESC
```

### ContactRelationshipResponse amendment

Add a field to indicate the viewing perspective:

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

The service determines `relatedContact` and `displayLabel` based on whether the viewing contact is the source or target:

```java
if (relationship.getSourceContactId().equals(viewingContactId)) {
    // Viewing from source: show target, use forward label
    relatedContact = resolve(relationship.getTargetContactId());
    displayLabel = relationship.getRelationshipType().getDisplayName();
} else {
    // Viewing from target: show source, use inverse label
    relatedContact = resolve(relationship.getSourceContactId());
    displayLabel = relationship.getRelationshipType().inverseDisplayName();
}
```

### Deletion

When deleting a relationship from either contact's page, the single record is soft-deleted. Both sides see it disappear.

---

## 14. Display Name Computation

**Amends the display_name logic in Section 6.**

The `display_name` column is computed in `ContactService` on every create and update, based on `contactType`:

| ContactType | Display Name Formula |
|-------------|---------------------|
| INDIVIDUAL | `trim(firstName + " " + lastName)` (lastName may be empty) |
| COMPANY | `companyName` |
| SERVICE_PROVIDER | `companyName` |

For COMPANY/SERVICE_PROVIDER, `firstName` and `lastName` (if present) are stored as the "contact person" but do NOT contribute to `display_name`.

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

---

## 15. CreateContactRequest Validation

**Amends Section 4 (DTOs).**

`firstName` becomes `Optional<String>` (was `@NotBlank String`). Conditional validation replaces the blanket `@NotBlank`:

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

**Custom validator** (or manual validation in `ContactService.createContact()`):

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

**Frontend form adaptation:**
- INDIVIDUAL: "First Name" (required, red asterisk) / "Last Name" (optional)
- COMPANY: "Company Name" (required, red asterisk) / "Contact Person First Name" (optional) / "Contact Person Last Name" (optional)
- SERVICE_PROVIDER: "Business Name" (required, red asterisk) / "Contact Person First Name" (optional) / "Contact Person Last Name" (optional)

The `UpdateContactRequest` follows the same pattern.

---

## 16. PostHog Analytics Events

Track the following events via PostHog (frontend-side):

| Event Name | Properties | Trigger |
|------------|-----------|---------|
| `CONTACT_CREATED` | `{ contactType, source: "full_form" \| "quick_add" }` | After successful contact creation |
| `CONTACT_TYPE_CHANGED` | `{ fromType, toType }` | After successful update that changes type |
| `CONTACT_NOTE_CREATED` | `{ interactionType, hasFollowUp: boolean }` | After successful note creation |
| `CONTACT_RELATIONSHIP_CREATED` | `{ relationshipType }` | After successful relationship creation |
| `CONTACT_TAG_ADDED` | `{ tag }` | After successful tag addition |
| `CONTACT_TAG_REMOVED` | `{ tag }` | After successful tag removal |
| `CONTACT_QUICK_ADD_USED` | `{}` | When quick-add form is used (subset of CONTACT_CREATED with source=quick_add) |
| `CONTACT_DUPLICATE_WARNING_SHOWN` | `{ matchCount }` | When duplicate check returns >0 matches |
| `CONTACT_DUPLICATE_WARNING_IGNORED` | `{}` | When user proceeds despite duplicate warning |
| `SERVICE_PROVIDER_TAB_VIEWED` | `{}` | When service provider filter/tab is selected on list page |

**KPIs (first 60 days post-launch):**
- % of teams with >=1 COMPANY contact
- % of teams with >=1 SERVICE_PROVIDER
- Avg notes per contact (target: >1.5 for active contacts)
- Quick-add vs full-form ratio (target: >30% quick-add)
- Tag adoption: % of contacts with >=1 tag

---

## 17. Naming & Localization

**Decision for this sprint:**
- Ship with "Contacts" / "Contacten" (Dutch) globally
- Do NOT create locale-specific navigation labels
- Plan an A/B copy test within 30 days of first users
- Sidebar label: "Contacts" (was "Tenants")
- Page titles: "Contacts", "Contact Details", "New Contact", etc.

---

## 18. Updated Follow-Up Issues

The following items **move from follow-up to in-scope** for this sprint (no longer follow-up issues):

| Original Follow-up | Status |
|---------------------|--------|
| #4 Contact Timeline Auto-Events | IN-SCOPE -- Activity tab with UNION ALL query |
| #5 Contact Follow-Up Reminder Notifications | IN-SCOPE -- `ContactFollowUpReminderJob` ships |

The following remain as **follow-up issues** (out of scope):

1. **CSV Import for Contacts** -- bulk import with duplicate detection
2. **GDPR Data Retention & Anonymization** -- automatic retention policies
3. **Tenant Portal (Contact Login)** -- `keycloak_user_id` self-service portal
4. **Contact Merge/Deduplication Tool** -- UI for reviewing and merging duplicates
5. **Demo Data Generator Update** -- `DemoContactGenerator` with types, tags, notes, relationships
6. **Backoffice Contact Management** -- backoffice admin view updates
7. **Booklet/Export Updates** -- path and exporter renames
8. **Contact Balance Display** -- outstanding payment aggregation on card

---

## 19. Updated Implementation Order

Amends the original Section "Implementation Order". Additions marked with **(NEW)**.

### Phase 1: Database + JOOQ Regen (1 day)
1. V038 migration already committed -- verify
2. Write and apply V039 migration (tag/relationship refinements, pinned column, CASCADE)
3. `mvn generate-sources -pl jooq -am`
4. Verify generated code compiles

### Phase 2: Backend Core Rename (2-3 days)
1. New enums: `ContactType`, `InteractionType`, updated `RelationshipType` (6 types + inverse methods), updated `ContactTag` (8 tags), `DataRetentionStatus`
2. New typed identifiers
3. Rename domain classes, update field renames
4. Rename DTOs -- `CreateContactRequest` with `Optional<String> firstName` **(AMENDED)**
5. **NEW**: `ContactListItemResponse` DTO
6. Update `EntityPrefix` + `SidGenerator`
7. Rename repositories + update queries
8. Rename mappers
9. Rename services -- **remove** `linkContactToProperty` / `unlinkContactFromProperty` **(AMENDED)**
10. **NEW**: Add type change field clearing logic to `ContactService.updateContact()`
11. **NEW**: Add contract party validation by contact type to `ContractPartyService`
12. Rename controller -- remove link/unlink endpoints **(AMENDED)**
13. Fix all compilation errors across all modules
14. Build: `mvn clean install -DskipTests`

### Phase 3: New Features (2-3 days)
1. New domain classes: `ContactNote` (with `pinned` field), `ContactRelationship`
2. New repositories: `ContactNoteRepository`, `ContactRelationshipRepository` (with bidirectional query), `ContactTagRepository` (with batch `findByContactIdsGrouped`)
3. New services: `ContactNoteService` (with pin/unpin), `ContactRelationshipService` (with inverse display logic)
4. **NEW**: `ContactActivityRepository` or method in `ContactRepository` -- UNION ALL activity feed query
5. **NEW**: `ContactFollowUpReminderJob` in `buurman-notifications` module
6. **NEW**: Quartz config for follow-up reminder job
7. Duplicate detection logic, display name computation
8. Contact list query with JOIN for `activeContractCount`
9. Controller: new endpoints (notes, relationships, tags, duplicates, activity feed, pin/unpin)
10. Build + verify

### Phase 4: OpenAPI + Frontend (3 days)
1. Rename + update `openapi/src/paths/contacts.yaml` -- remove link/unlink, add activity endpoint
2. Update `openapi/src/app.yaml` -- all schema renames + new schemas (`ContactListItemResponse`, `ContactActivityItem`, etc.)
3. `make bundle-openapi`
4. `cd frontend && yarn generate:api`
5. Rename all frontend files
6. **NEW**: 5-tab detail page layout (Overview, Activity, Relationships, Files, Addresses)
7. **NEW**: Activity feed component (manual notes + auto events)
8. **NEW**: Note form with collapsed date section
9. **NEW**: Contact card with no balance, reserved space
10. **NEW**: Empty states for all sections
11. **NEW**: Type change confirmation dialog
12. **NEW**: Contract party picker filtering by contact type
13. **NEW**: PostHog event tracking
14. Update routes + navigation ("Tenants" -> "Contacts")
15. Build + verify: `cd frontend && yarn build`

### Phase 5: Polish (1 day)
1. Audit trail: update all `"TENANT"` -> `"CONTACT"` strings
2. Metrics: rename `tenant.*` -> `contact.*`
3. Verify empty states render correctly
4. Verify follow-up reminder job fires in dev
5. Test full flow end-to-end locally: create contact (all 3 types), add notes, add relationships, add tags, check activity feed, change type, verify field clearing
6. Code review + PR

---

## 20. Spec v3 Amendments

**Date**: 2026-03-21
**Context**: Founder decisions — 6 items pulled into scope, data model simplification, GDPR now ships.

---

### 20.1 GDPR Erase Endpoint (Ships This Sprint)

**Moved from follow-up to in-scope.** No automatic retention policies — manual trigger only.

#### Behavior

- **Soft delete** remains the default for normal "delete contact" (`deleted_at` set, contact hidden from UI).
- **New "Permanently erase" action** — GDPR Art. 17 Right to Erasure. TEAM_ADMIN only, irreversible.

#### Anonymization Rules

When erase is triggered, the following fields on the `contacts` row are overwritten:

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

**Preserved fields** (must NOT be erased):
- `id`, `identifier` — referential integrity
- `team_id` — multi-tenancy
- `contact_type` — aggregate analytics
- `created_at` — audit trail
- All financial records linked via `contract_parties` → `contracts` → `payments` — Dutch fiscal law requires 7-year retention

#### Related Data Cleanup

| Data | Action |
|------|--------|
| `contact_notes` for this contact | Set `body` → `"Content erased per GDPR request"`, `subject` → `NULL` |
| `contact_addresses` for this contact | Hard delete rows (no fiscal requirement) |
| S3 documents (`documents` where `entity_type = 'CONTACT'` and `entity_id = contact.id`) | Delete S3 objects, then hard delete DB rows |
| S3 photos (`photos` where `entity_type = 'CONTACT'` and `entity_id = contact.id`) | Delete S3 objects, then hard delete DB rows |
| `contact_tags` for this contact | Hard delete rows (CASCADE handles this) |
| `contact_relationships` (source or target) | Soft delete rows (`deleted_at` set) |
| `property_contact_history` entries | Preserved — the contact row still exists but is anonymized |

#### Audit

Insert an audit log entry: `"Contact data erased per GDPR request by [admin display name]"` with `entity_type = 'CONTACT'`, `entity_id = contact.id`.

#### Endpoint

| Method | Path | Operation | Roles |
|--------|------|-----------|-------|
| POST | `/contacts/{id}/erase` | eraseContactData | TEAM_ADMIN |

**Request**: Empty body.
**Response**: `204 No Content` on success. `403` if not TEAM_ADMIN. `404` if contact not found. `409` if contact is already `ANONYMIZED`.

#### Service Implementation Sketch

```java
@Transactional
@PreAuthorize("hasRole('TEAM_ADMIN')")
public void eraseContactData(ContactIdentifier identifier, UserAuthentication auth) {
    Contact contact = contactRepository.findByIdentifierAndTeamId(identifier, auth.getTeamId())
        .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

    if (contact.getDataRetentionStatus() == DataRetentionStatus.ANONYMIZED) {
        throw new ConflictException("Contact data has already been erased");
    }

    // 1. Delete S3 objects (documents + photos)
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

    // 5. Anonymize the contact record itself
    contactRepository.anonymize(contact.getId(), auth.getTeamId());

    // 6. Audit log
    auditLogService.log(auth.getTeamId(), auth.getUserId(),
        "CONTACT", contact.getId(), "Contact data erased per GDPR request by " + auth.getDisplayName());
}
```

#### Frontend

- **Location**: Contact detail page, "Danger Zone" section (or kebab menu → "Erase personal data").
- **Double confirmation dialog**:
  - Title: "Permanently erase all personal data?"
  - Body: "This will permanently erase all personal data for this contact. Financial records linked via contracts are retained for legal compliance. This action cannot be undone."
  - Input: "Type ERASE to confirm" — submit button disabled until input === `"ERASE"`
  - Buttons: `[Cancel]` `[Erase Data]` (red, destructive)
- PostHog event: `CONTACT_DATA_ERASED` with `{ contactType }`
- After success: redirect to contacts list with toast "Contact data erased"

---

### 20.2 Remove `keycloak_user_id` from Contacts

**Decision**: Tenant portal login will use the contact's email. No extra column needed.

#### Migration Impact

Amend V038 — remove the following from the contacts table definition:

```sql
-- REMOVE from V038:
keycloak_user_id UUID,
-- REMOVE index:
CREATE INDEX idx_contacts_keycloak_user ON contacts (keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
```

If V038 is already applied in dev environments, add to V039 (or a new V040):

```sql
-- Drop keycloak_user_id if it exists (dev cleanup)
ALTER TABLE contacts DROP COLUMN IF EXISTS keycloak_user_id;
```

#### Domain/DTO Impact

- Remove `keycloak_user_id` from `Contact` domain class (if added)
- Remove from `ContactResponse` / `CreateContactRequest` / `UpdateContactRequest` (if added)
- Remove from mapper methods
- No JOOQ codegen impact after column is dropped

#### Tenant Portal (Future)

The tenant portal authenticates via email:
- Keycloak matches on `email` attribute (standard OIDC claim)
- Contact lookup: `SELECT * FROM contacts WHERE email = :email AND team_id = :teamId`
- No additional column required

---

### 20.3 Duplicate Detection Scope Clarification

**Decision**: No separate merge/dedup tool. S07 (Duplicate Detection) is the full mechanism.

- `POST /contacts/check-duplicates` endpoint ships in Phase 3 (already in-scope as S07/P1)
- Frontend shows warning during create and quick-add:
  - "A similar contact already exists: [display_name] ([email/phone])"
  - User can: "Go to existing contact" or "Create anyway"
- **No merge functionality.** If a duplicate was created, the user manually soft-deletes one.
- **Remove** "Contact Merge/Deduplication Tool" from follow-up issues (Section 18, item #4)

---

### 20.4 Demo Data Generator (Ships This Sprint)

**Moved from follow-up to in-scope.** Module: `buurman-demo-data`.

#### Rename

`DemoTenantGenerator` → `DemoContactGenerator`

#### Generation Mix

| Contact Type | % | Count (per team) |
|-------------|---|------------------|
| INDIVIDUAL | ~60% | ~30 contacts |
| COMPANY | ~25% | ~12 contacts |
| SERVICE_PROVIDER | ~15% | ~8 contacts |

#### INDIVIDUAL Contacts

- Dutch names: mix of common Dutch first names (Jan, Piet, Klaas, Marieke, Fatima, Mohammed, Sven, Anouk, Daan, Fleur, Bram, Sophie, etc.) and last names (De Vries, Jansen, Van den Berg, Bakker, Visser, Smit, De Boer, Mulder, De Groot, Bos, etc.)
- Realistic email: `firstname.lastname@gmail.com`, `f.lastname@outlook.nl`, `firstname@hotmail.com`
- Dutch mobile phone: `+316XXXXXXXX` format
- Dutch addresses: real city names (Amsterdam, Rotterdam, Den Haag, Utrecht, Eindhoven, Groningen, Tilburg, Almere, Breda, Nijmegen), postal codes in `"1234 AB"` format, street names (Keizersgracht, Vondelstraat, Oudegracht, Mariaplaats, etc.)

#### COMPANY Contacts

Realistic Dutch company names and metadata:

```
"Van der Berg Vastgoed B.V."      — industry: "Real Estate", trade name: "VdB Vastgoed"
"Café De Hoek V.O.F."             — industry: "Hospitality"
"Jansen Techniek B.V."            — industry: "Engineering"
"Bakkerij Het Broodje B.V."       — industry: "Food & Beverage"
"De Wit Administratie B.V."       — industry: "Accounting"
"Hollands Schoon B.V."            — industry: "Cleaning Services"
"Rotterdam Logistics B.V."        — industry: "Transport & Logistics"
"Bloemen Van Dijk V.O.F."         — industry: "Retail"
"Digitaal Bureau Amsterdam B.V."  — industry: "IT & Digital"
"Groen & Tuin B.V."               — industry: "Landscaping"
"Makelaardij Prins B.V."          — industry: "Real Estate"
"Het Gouden Ei V.O.F."            — industry: "Hospitality"
```

Each company has:
- KvK-style number (8 digits, e.g., `"12345678"`)
- Contact person (first/last name)
- Invoice email: `factuur@companyname.nl` or `administratie@companyname.nl`
- Website: `www.companyname.nl`

#### SERVICE_PROVIDER Contacts

Realistic Dutch service provider names:

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

Each service provider has:
- Contact person name
- Business phone: `+3120XXXXXXX` or `+3110XXXXXXX` (landline format)
- Email: `info@businessname.nl`

#### Per-Contact Generated Data

Each contact gets:

| Data | Distribution |
|------|-------------|
| Notes | 1-3 per contact, with realistic interaction types and content |
| Tags | 1-3 tags (weighted: LONG_TERM most common ~40%, PROSPECT ~20% of individuals, VIP ~5%, LATE_PAYER ~10%, KEY_HOLDER ~10%, REFERRED ~10%, DO_NOT_CONTACT ~2%, FORMER_TENANT ~3%) |
| Relationships | 0-2 per contact (guarantor pairs for individuals, WORKS_FOR/CONTACT_PERSON_FOR for company↔individual links) |
| Addresses | 0-2 per contact (Dutch cities, real postal code format `"1012 AB"`) |
| Follow-up dates | ~20% of notes have `follow_up_date` set — mix of overdue (past) and upcoming (next 14 days) |

#### Note Content Examples

Realistic note content by interaction type:

- `PHONE_CALL`: "Called regarding lease renewal. Tenant confirmed they want to extend for another year. Will send updated contract next week."
- `MEETING`: "Met at the property for annual inspection. Kitchen faucet leaking — scheduled repair with Loodgietersbedrijf Smit for Friday."
- `VIEWING`: "Showed apartment 2B to prospective tenant. Very interested, will confirm by end of week."
- `INSPECTION`: "Quarterly inspection completed. Property in good condition. Minor paint touch-up needed in hallway."
- `NOTE`: "Received email about parking space allocation. Forwarded to property manager."
- `HANDOVER`: "Key handover completed. All keys accounted for. Meter readings documented."

#### DemoDataContext Update

Add to `DemoDataContext`:

```java
private List<Contact> generatedContacts;
private Map<UUID, List<ContactTag>> contactTags;
```

#### DemoDataService Update

Update `DemoDataService` to call `DemoContactGenerator` instead of `DemoTenantGenerator`. Ensure contacts are generated before contracts (contracts reference contacts as parties).

---

### 20.5 Backoffice Contact Management (Ships This Sprint)

**Moved from follow-up to in-scope.** Module: `buurman-backoffice`.

#### Scope

Mechanical rename of all tenant references in the backoffice module:

- **Controllers**: Rename any `BackofficeTenantController` → `BackofficeContactController`, update path mappings from `/backoffice/tenants` → `/backoffice/contacts`
- **Services**: Rename `BackofficeTenantService` → `BackofficeContactService`
- **Repositories**: Rename any backoffice tenant repos → contact repos
- **DTOs**: Update request/response types to reference contacts
- **OpenAPI** (`openapi/src/paths/backoffice-*.yaml`): Rename tenant paths and schemas

#### Checklist

1. Search all files in `buurman-backoffice/` for `tenant`/`Tenant` references
2. Rename classes, methods, variables, SQL column references
3. Update OpenAPI backoffice paths
4. Bundle: `make bundle-openapi`
5. Build: `mvn clean install -DskipTests`

---

### 20.6 Booklet/Export Updates (Ships This Sprint)

**Moved from follow-up to in-scope.** Module: `buurman-booklets`.

#### Path Rename

`/booklets/tenant/{tenantIdentifier}` → `/booklets/contact/{contactIdentifier}`

Update `BookletController`, path variable type from `TenantIdentifier` → `ContactIdentifier`.

#### PDF Exporter Redesign

The contact PDF export must be professional and type-aware.

**Layout (A4 format):**

1. **Header**
   - Buurman logo (left), team name (right), export date (right, below team name)
   - Horizontal rule

2. **Contact Overview Section** (type-aware)

   For INDIVIDUAL:
   - Full name (large), contact type badge
   - Two-column grid: email, phone, tax number, ID number, date of birth
   - Tags as colored chips

   For COMPANY:
   - Company name (large), trade name (subtitle), contact type badge
   - Two-column grid: industry, website, invoice email, contact person name
   - Tags as colored chips

   For SERVICE_PROVIDER:
   - Business name (large), contact type badge
   - Two-column grid: contact person, phone, email
   - Tags as colored chips

3. **Addresses Section**
   - Each address: type label, formatted multi-line address

4. **Active Contracts Section**
   - Table: Property Name | Role | Start Date | End Date | Status
   - Only active/pending contracts shown

5. **Notes Section**
   - Each note: interaction type badge, date, author, body text (first 500 chars)
   - Sorted by `occurred_at` DESC
   - Max 20 most recent notes

6. **Relationships Section**
   - Table: Related Contact | Relationship | Notes

7. **Footer**
   - "Generated by Buurman on [date]" (centered, muted)
   - Page numbers

**Typography & Branding:**
- Font: professional sans-serif (Helvetica or similar iText7-compatible)
- Buurman brand color for headings and accents
- Clean whitespace, readable font sizes (11pt body, 14pt headings)
- Suitable for printing or emailing as attachment

#### CSV Exporter Update

Update CSV columns to include all contact fields:

```
identifier, contact_type, display_name, first_name, last_name, company_name,
trade_name, industry, email, phone, tax_number, id_number, id_expiry_date,
date_of_birth, website, invoice_email, tags, active_contract_count,
data_retention_status, created_at, updated_at
```

- `tags` column: comma-separated tag values (e.g., `"VIP,LONG_TERM"`)
- `contact_type` column: `INDIVIDUAL`, `COMPANY`, or `SERVICE_PROVIDER`

---

### 20.7 Updated Follow-Up Issues (Replaces Section 18)

After sections 20.1–20.6, the follow-up list reduces to **3 items**:

| # | Follow-Up Issue | Notes |
|---|----------------|-------|
| 1 | **CSV Import for Contacts** (BUUR-82) | Bulk import with duplicate detection |
| 2 | **Tenant Portal** (future epic) | Uses contact email for login — no `keycloak_user_id` needed |
| 3 | **Contact Balance Display** (BUUR-83) | Outstanding payment aggregation on contact card |

**Removed from follow-up** (now in-scope per this amendment):
- ~~GDPR Data Retention & Anonymization~~ → 20.1
- ~~Contact Merge/Deduplication Tool~~ → 20.3 (not needed, S07 is sufficient)
- ~~Demo Data Generator Update~~ → 20.4
- ~~Backoffice Contact Management~~ → 20.5
- ~~Booklet/Export Updates~~ → 20.6

---

### 20.8 Updated Implementation Phases (Replaces Section 19 Phase 5)

Phase 5 expands from 1 day to 2-3 days. Replace Section 19's Phase 5 with:

### Phase 5: Polish + Newly In-Scope Items (2-3 days)

**Day 1: Backend additions**
1. GDPR erase endpoint: `ContactService.eraseContactData()`, `POST /contacts/{id}/erase`
2. Repository methods: `hardDeleteByEntityTypeAndEntityId` (documents, photos), `hardDeleteByContactId` (addresses), `anonymizeByContactId` (notes), `anonymize` (contact)
3. S3 object deletion service methods: `deleteAllS3ObjectsForEntity`
4. `DemoContactGenerator` rewrite (replaces `DemoTenantGenerator`) with Dutch names, companies, service providers, notes, tags, relationships, addresses
5. Backoffice rename: controllers, services, repos, OpenAPI paths
6. Booklet path rename + CSV exporter update

**Day 2: Frontend + PDF**
7. GDPR erase dialog (double confirmation with "ERASE" input)
8. Professional contact PDF export (iText7, type-aware layout, Buurman branding)
9. Backoffice frontend updates (if applicable)
10. OpenAPI schema additions: erase endpoint, updated booklet paths

**Day 3: Integration + polish**
11. Audit trail: update all `"TENANT"` → `"CONTACT"` strings
12. Metrics: rename `tenant.*` → `contact.*`
13. Verify empty states render correctly
14. Verify follow-up reminder job fires in dev
15. Verify GDPR erase flow end-to-end (anonymization, S3 deletion, audit log)
16. Verify demo data generates correctly for all 3 contact types
17. Test full flow end-to-end locally
18. Bundle OpenAPI: `make bundle-openapi`
19. Code review + PR

**Total sprint estimate**: Phase 1 (1d) + Phase 2 (2-3d) + Phase 3 (2-3d) + Phase 4 (3d) + Phase 5 (2-3d) = **10-13 days**
