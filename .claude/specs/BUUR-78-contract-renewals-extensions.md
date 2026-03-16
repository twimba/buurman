# BUUR-78: Contract Renewals & Extensions — Full Specification (v2)

> Consolidated from: Product Owner, Real Estate Expert, Business Analyst, Software Architect, Principal Engineer
> Updated: 2026-03-17 — Scope expansion + resolved open questions

---

## Table of Contents

1. [Design Decisions](#1-design-decisions)
2. [Domain Model](#2-domain-model)
3. [State Machine](#3-state-machine)
4. [Business Rules](#4-business-rules)
5. [Effective End Date Impact Analysis](#5-effective-end-date-impact-analysis)
6. [User Stories](#6-user-stories)
7. [UX Flows](#7-ux-flows)
8. [Edge Cases](#8-edge-cases)
9. [Permission Matrix](#9-permission-matrix)
10. [Database Migration](#10-database-migration)
11. [OpenAPI Spec](#11-openapi-spec)
12. [Module Placement & File Inventory](#12-module-placement--file-inventory)
13. [Service Layer Design](#13-service-layer-design)
14. [Quartz Jobs](#14-quartz-jobs)
15. [Notifications](#15-notifications)
16. [Jurisdiction Defaults](#16-jurisdiction-defaults)
17. [Implementation Plan](#17-implementation-plan)
18. [Sub-Issues](#18-sub-issues)
19. [Risk Register](#19-risk-register)
20. [Testing Strategy](#20-testing-strategy)
21. [Deferred / Future Work](#21-deferred--future-work)

---

## 1. Design Decisions

### Original Questions

| Question | Decision |
|----------|----------|
| Extension vs. new contract? | Keep original contract immutable. Create extension record. Contract's `end_date` is **NEVER mutated** — effective end date computed from latest ACTIVE extension. |
| Rent period integration? | Auto-create rent period in **DRAFT** status. User must accept to make it effective. |
| Contract status during extension? | Contract stays **ACTIVE**. Extension has own lifecycle: `DRAFT → ACTIVE → SUPERSEDED`. |
| Retroactive extensions? | Allowed for **any** time period. |

### Resolved Open Questions

| # | Question | Answer |
|---|----------|--------|
| 1 | Immutable end_date vs. denormalized? | **Immutable**. Effective end date always computed. All queries updated. |
| 2 | Drop auto_renewal/renewal_notice_days? | **Clean break**. Drop in V034 migration after data migration. |
| 3 | System user for Quartz jobs? | **Existing** `SYSTEM_USER_ID` (`00000000-0000-0000-0000-000000000001`) from `Constants.java`. Already used by PaymentSchedulingService. |
| 4 | Rollover to indefinite? | **MVP scope**. |
| 5 | PDF addendum? | **MVP scope**. |
| 6 | Extension numbers after decline? | **Monotonic, never reused**. Declined #2 → next is #3. |
| 7 | Dashboard panel? | **Included** in this issue. |

### Codebase Discoveries (from agents)

- `SYSTEM_USER_ID` already exists in `Constants.java` — no migration needed
- `furnished` already exists on `property_residential_details` table (V002) — no schema change needed for properties
- `region_code` already exists on `properties` table (V029) — only needed on `contracts` table

---

## 2. Domain Model

### 2.1 Renewal Configuration (on `contracts` table)

| Field | Type | Description | Default |
|-------|------|-------------|---------|
| `renewal_mode` | VARCHAR(20) | `NONE`, `AUTOMATIC`, `MANUAL` (replaces `auto_renewal`) | `NONE` |
| `renewal_term_months` | INTEGER | Duration of each extension (e.g., 12) | NULL |
| `max_renewals` | INTEGER (nullable) | Max extensions allowed. NULL = unlimited | NULL |
| `landlord_notice_days` | INTEGER | Days before end date landlord must act | 30 |
| `tenant_notice_days` | INTEGER | Days before end date tenant must act | 30 |
| `requires_tenant_confirmation` | BOOLEAN | If true, extensions stay DRAFT until confirmed | FALSE |
| `rent_adjustment_type` | VARCHAR(20) | `NONE`, `FIXED_PERCENTAGE`, `FIXED_AMOUNT`, `MANUAL` | `NONE` |
| `rent_adjustment_value` | DECIMAL(15,4) | Percentage or fixed amount (minor units) | NULL |
| `landlord_type` | VARCHAR(20) | `NATURAL_PERSON`, `LEGAL_ENTITY` (affects ES mandatory period) | NULL |
| `region_code` | VARCHAR(10) | Region for jurisdiction defaults (e.g., `BRU`, `VLG`, `WAL` for BE) | NULL |

### 2.2 Contract Extensions (new `contract_extensions` table)

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | PK |
| `identifier` | VARCHAR(29) | Sid (prefix `CEX`) |
| `team_id` | UUID | FK → teams |
| `contract_id` | UUID | FK → contracts |
| `extension_number` | INTEGER | Sequential per contract (1, 2, 3...), never reused |
| `previous_end_date` | DATE | Contract's effective end date before this extension |
| `new_end_date` | DATE | New end date (NULL = rollover to indefinite) |
| `previous_rent_amount` | BIGINT | Rent before (minor units) |
| `previous_rent_currency` | VARCHAR(3) | Currency |
| `new_rent_amount` | BIGINT | Rent after (minor units) |
| `new_rent_currency` | VARCHAR(3) | Currency |
| `rent_adjustment_type` | VARCHAR(20) | How rent was determined for *this* extension |
| `rent_adjustment_value` | DECIMAL(15,4) | The value used |
| `status` | VARCHAR(20) | `DRAFT`, `ACTIVE`, `SUPERSEDED`, `CANCELLED`, `DECLINED` |
| `trigger_type` | VARCHAR(10) | `MANUAL`, `AUTO` |
| `rent_period_id` | UUID (nullable) | FK → contract_rent_periods (set on activation) |
| `notes` | TEXT | Free-text |
| `declined_reason` | TEXT | Reason for decline |
| `activated_at` | TIMESTAMP | When DRAFT → ACTIVE |
| `activated_by` | UUID | Who activated |
| `confirmed_at` | TIMESTAMP | When tenant confirmed |
| `confirmed_by` | UUID | Who confirmed |
| `superseded_at` | TIMESTAMP | When ACTIVE → SUPERSEDED |
| Standard audit | | `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at` |

### 2.3 Jurisdiction Defaults (new `jurisdiction_defaults` table)

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | PK |
| `country_code` | VARCHAR(2) | ISO country code (required) |
| `region_code` | VARCHAR(10) | Regional subdivision (nullable) |
| `landlord_type` | VARCHAR(20) | NATURAL_PERSON / LEGAL_ENTITY (nullable) |
| `furnished` | BOOLEAN | Furnished property variant (nullable) |
| `field_name` | VARCHAR(50) | Config field name (e.g., `landlord_notice_days`) |
| `value` | VARCHAR(100) | The default value |
| `valid_from` | DATE | Effective from |
| `valid_until` | DATE | Effective until (NULL = currently active) |
| `notes` | TEXT | Legal reference |

### 2.4 Computed Fields (not stored)

| Field | Formula | Where Used |
|-------|---------|------------|
| `effective_end_date` | `COALESCE((SELECT new_end_date FROM contract_extensions WHERE contract_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL ORDER BY extension_number DESC LIMIT 1), contracts.end_date)` | **Everywhere** — contract detail, dashboard, payment generation, expiry checks, reports, calendar, booklets |
| `effective_rent` | Latest ACTIVE extension's `new_rent_amount`, else current rent period | Contract detail, payments |
| `extension_count` | Count of non-deleted extensions (excl. CANCELLED/DECLINED) | Contract response |
| `extensions_remaining` | `max_renewals - count(ACTIVE + SUPERSEDED)`. NULL if unlimited | Contract detail |
| `in_notice_window` | `today >= effective_end_date - landlord_notice_days` | Auto-extension job |

### 2.5 Entity Prefix & Identifier

- Prefix: `CEX` ("Contract Extensions")
- Typed identifier: `ContractExtensionIdentifier extends Sid`
- SidGenerator method: `newContractExtensionId()`

---

## 3. State Machine

### 3.1 Extension Lifecycle

```
                  ┌───────────────┐
   ┌──────────────│     DRAFT     │──────────────┐
   │              └───┬───────┬───┘              │
   │                  │       │                  │
   │  [cancel]        │       │     [decline]    │
   │                  │       │                  │
   ▼                  │       │                  ▼
┌──────────┐    [activate]    │          ┌───────────┐
│ CANCELLED│          │       │          │ DECLINED  │
└──────────┘          ▼       │          └───────────┘
              ┌───────────────┘
              ▼
      ┌───────────────┐
      │    ACTIVE     │────[new extension activated]──→ SUPERSEDED
      └───────────────┘
```

### 3.2 Transition Table

| From | To | Trigger | Who | Side Effects |
|------|----|---------|-----|-------------|
| DRAFT | ACTIVE | User activates; or auto-activate when `!requires_tenant_confirmation` and `trigger_type = AUTO` | TEAM_ADMIN, TEAM_EDITOR, System | 1. Create DRAFT rent period. 2. Supersede previous ACTIVE extension. 3. Set `activated_at/by`. 4. Send notification. |
| DRAFT | CANCELLED | User cancels | TEAM_ADMIN, TEAM_EDITOR | Set `deleted_at`. Audit log. |
| DRAFT | DECLINED | User declines | TEAM_ADMIN, TEAM_EDITOR | Set `declined_reason`. Audit log. No auto-recreation. |
| ACTIVE | SUPERSEDED | Newer extension activated | System | Set `superseded_at`. Linked rent period remains. |

### 3.3 Contract Status Interaction

Contract stays ACTIVE. `contracts.end_date` is **NEVER mutated**. Effective end date always computed via `EffectiveEndDateHelper`.

---

## 4. Business Rules

### Validation Rules (BR-01 to BR-10)

| # | Rule | Enforcement |
|---|------|-------------|
| BR-01 | `renewal_term_months > 0` when provided | DB CHECK + service |
| BR-02 | `renewal_term_months` required when `renewal_mode != NONE` and contract is `FIXED_TERM` | Service |
| BR-03 | `max_renewals > 0` when provided. NULL = unlimited | DB CHECK |
| BR-04 | `landlord_notice_days >= 0` and `tenant_notice_days >= 0` | DB CHECK |
| BR-05 | `rent_adjustment_value NOT NULL` when `rent_adjustment_type NOT IN (NONE, MANUAL)` | DB CHECK |
| BR-06 | `rent_adjustment_value NULL` when `rent_adjustment_type IN (NONE, MANUAL)` | DB CHECK |
| BR-07 | For `FIXED_PERCENTAGE`: value in range `[-100, 100]` | DB CHECK |
| BR-08 | `new_end_date > previous_end_date` | DB CHECK |
| BR-09 | `new_rent_amount > 0` (minor units) | DB CHECK |
| BR-10 | `extension_number` unique per contract, monotonically increasing, never reused | DB UNIQUE |

### Constraint Rules (BR-11 to BR-15)

| # | Rule | Enforcement |
|---|------|-------------|
| BR-11 | At most ONE extension in DRAFT or ACTIVE status per contract | DB partial unique index |
| BR-12 | Extensions only for contracts with status = ACTIVE | Service |
| BR-13 | Extensions only for FIXED_TERM contracts | Service |
| BR-14 | `max_renewals` check: count(ACTIVE + SUPERSEDED) < max_renewals | Service |
| BR-15 | Declined extensions do NOT count toward `max_renewals` | Service |

### Computation Rules (BR-16 to BR-22)

| # | Rule | Formula |
|---|------|---------|
| BR-16 | `previous_end_date` = contract's current effective end date | Computed via `EffectiveEndDateHelper` |
| BR-17 | `new_end_date` = `previous_end_date + renewal_term_months` | Service |
| BR-18 | `extension_number` = MAX(extension_number for contract) + 1, default 1 | Service |
| BR-19 | `NONE`: `new_rent = previous_rent` | Service |
| BR-20 | `FIXED_AMOUNT`: `new_rent = previous_rent + value` | Service |
| BR-21 | `FIXED_PERCENTAGE`: `new_rent = previous_rent * (1 + value/100)`, HALF_UP | Service |
| BR-22 | `MANUAL`: user provides `new_rent` directly | Service |

### Rent Period Rules (BR-23 to BR-27)

| # | Rule |
|---|------|
| BR-23 | When extension → ACTIVE: create `contract_rent_period` in DRAFT status |
| BR-24 | Rent period `effective_from` = `extension.previous_end_date + 1 day` |
| BR-25 | Rent period `amount` = `extension.new_rent_amount` |
| BR-26 | Link back: `extension.rent_period_id` = new rent period's ID |
| BR-27 | Previous rent period's `effective_to` closed at `extension.previous_end_date` |

### Auto-Extension Job Rules (BR-28 to BR-34)

| # | Rule |
|---|------|
| BR-28 | Runs daily (cron configurable, default `0 0 2 * * ?`) |
| BR-29 | Queries ACTIVE contracts: `renewal_mode = AUTOMATIC`, effective_end_date within notice window, no existing DRAFT/ACTIVE extension |
| BR-30 | Creates DRAFT extension with `trigger_type = AUTO` |
| BR-31 | If `!requires_tenant_confirmation`: immediately activates |
| BR-32 | If `requires_tenant_confirmation`: stays DRAFT, sends pending notification |
| BR-33 | Each contract in own transaction |
| BR-34 | Idempotent — checks for existing extension before creating |

### Rollover Rules (BR-35 to BR-37)

| # | Rule |
|---|------|
| BR-35 | When `max_renewals` reached + `renewal_mode = AUTOMATIC`: create final extension with `new_end_date = NULL` |
| BR-36 | Set contract's `renewal_mode` to `NONE` after rollover |
| BR-37 | Send `CONTRACT_ROLLED_OVER_TO_INDEFINITE` notification |

### Immutable End Date Rules (BR-38 to BR-41)

| # | Rule |
|---|------|
| BR-38 | `contracts.end_date` is NEVER mutated after contract creation. Stores the original contractual end date. |
| BR-39 | `effective_end_date` is always computed, never stored. Uses COALESCE subquery. |
| BR-40 | All user-facing dates show `effective_end_date`, not `contracts.end_date` |
| BR-41 | Original `end_date` visible in contract detail as "Original End Date" when it differs from effective |

### New Field Rules (BR-42 to BR-51)

| # | Rule |
|---|------|
| BR-42 | `landlord_type` nullable. `NATURAL_PERSON` or `LEGAL_ENTITY` |
| BR-43 | `landlord_type` affects jurisdiction defaults lookup for ES (5y vs 7y mandatory period) |
| BR-44 | When `country_code = 'ES'` and `landlord_type` unset: UI shows info banner |
| BR-45 | `region_code` nullable, currently relevant for BE (`BRU`, `VLG`, `WAL`) |
| BR-46 | When `country_code = 'BE'` and `region_code` unset: UI prompts |
| BR-47 | `region_code` affects jurisdiction defaults lookup, falls back to country-level |
| BR-48 | `furnished` read from `property_residential_details.furnished` (already exists) |
| BR-49 | `furnished` affects FR jurisdiction defaults (1y vs 3y terms) |
| BR-50 | Jurisdiction defaults are **suggestions, not enforcement**. User can always override. |
| BR-51 | Defaults pre-fill renewal config form when fields are empty |

### System User & Dashboard Rules (BR-52 to BR-57)

| # | Rule |
|---|------|
| BR-52 | Auto-extension job uses `Constants.SYSTEM_USER_ID` for `created_by/activated_by` |
| BR-53 | Extensions created by system show "System" in UI; `trigger_type = AUTO` flag distinguishes |
| BR-54 | Dashboard "Upcoming Renewals": `renewal_mode != NONE`, ACTIVE contract, effective_end_date within 90 days, no existing DRAFT/ACTIVE extension |
| BR-55 | Dashboard sorted by effective_end_date ASC, limited to 10 results |
| BR-56 | Jurisdiction defaults lookup cascades: `(country, region, landlord_type, furnished)` → `(country)` |
| BR-57 | Temporal validity: `valid_from <= today AND (valid_until IS NULL OR valid_until > today)` |

---

## 5. Effective End Date Impact Analysis

### Strategy: Shared JOOQ Field Expression + Service Helper

**Selected approach**: `EffectiveEndDateHelper` class in buurman-core providing:

1. **JOOQ `Field<LocalDate>`** for SQL queries:
```java
public static Field<LocalDate> effectiveEndDate() {
    return DSL.coalesce(
        DSL.field(DSL.select(CE.NEW_END_DATE)
            .from(CE)
            .where(CE.CONTRACT_ID.eq(CONTRACTS.ID))
            .and(CE.STATUS.eq("ACTIVE"))
            .and(CE.DELETED_AT.isNull())
            .orderBy(CE.EXTENSION_NUMBER.desc())
            .limit(1)),
        CONTRACTS.END_DATE);
}
```

2. **Java helper** for service code with already-loaded entities:
```java
public static Optional<LocalDate> computeEffectiveEndDate(
    Contract contract, List<ContractExtension> extensions)
```

**Alternatives rejected**: DB VIEW (not updatable), generated column (can't reference other tables), trigger (hidden side effects).

### Files Requiring Changes (14 backend + 5 frontend)

#### Repository Layer

| File | Method/Line | Current | Change |
|------|-------------|---------|--------|
| `ContractRepository` | `findAllByTeamIdPaginated()` L342 | Sort by `CONTRACTS.END_DATE` | Use `effectiveEndDate()` |
| `ContractRepository` | `findExpiringContracts()` L401 | Filter `CONTRACTS.END_DATE.le(beforeDate)` | Use `effectiveEndDate()` |

#### Service Layer

| File | Method/Line | Current | Change |
|------|-------------|---------|--------|
| `ContractService` | `toResponse()` L908 | `contract.getEndDate()` | Add `effectiveEndDate` + `originalEndDate` to response |
| `ContractRentPeriodService` | `validateEffectiveFrom()` L337 | `contract.getEndDate()` | Use effective end date for validation |
| `PaymentSchedulingService` | Payment generation L156 | `contract.getEndDate()` stop condition | Use effective end date |
| `NotificationSchedulerService` | `checkContractExpiry()` L72-78 | `contract.getEndDate()` | Use effective end date; skip `AUTOMATIC` mode |
| `ReportService` | `getOccupancyTrend()` L489, L532, L737 | `c.getEndDate().orElse(LocalDate.MAX)` | Use effective end date |
| `PropertyDashboardService` | Occupancy chart L576, L628-629, L712 | `c.getEndDate()` | Use effective end date |
| `CalendarFeedService` | iCal events L278-283 | `contract.getEndDate()` | Use effective end date |
| `OccupancyPeriodService` | Overlap detection L318 | `c.getEndDate()` | Use effective end date |

#### Booklet Exporters

| File | Line | Change |
|------|------|--------|
| `ContractBookletExporter` | L235, L286 | Show both original and effective end date |
| `PropertyBookletExporter` | L1196 | Use effective end date |
| `TenantBookletExporter` | L403 | Use effective end date |

#### Demo Data

| File | Lines | Change |
|------|-------|--------|
| `DemoContractGenerator` | L196, L206-207, L300, L310 | Remove `AUTO_RENEWAL`/`RENEWAL_NOTICE_DAYS`, add new columns |
| `DemoPaymentGenerator` | L72, L79 | Use effective end date SQL expression |

#### Frontend (5 files)

| File | Change |
|------|--------|
| `ContractDetailPage.tsx` L523 | Use `effectiveEndDate` |
| `ContractCard.tsx` L74 | Use `effectiveEndDate` |
| `PropertyDetailPage.tsx` L1868-1869 | Use `effectiveEndDate` |
| `TenantDetailPage.tsx` L849-850 | Use `effectiveEndDate` |
| `types/contract.ts` | Add `effectiveEndDate` to type definitions |

---

## 6. User Stories

### Configuring Renewals

**US-1**: As a TEAM_ADMIN/EDITOR creating a contract, I can configure renewal_mode, renewal_term_months, max_renewals, landlord/tenant notice days, requires_tenant_confirmation, rent adjustment, landlord_type, and region_code.

**US-2**: As a TEAM_ADMIN/EDITOR on an existing ACTIVE contract, I can update renewal settings. Changes apply to future extensions only.

**US-3**: As a TEAM_VIEWER, renewal settings are read-only.

### Automatic Extensions

**US-4**: AUTOMATIC contracts generate extensions via scheduled job when notice window passes.

**US-5**: System sends CONTRACT_RENEWAL_REMINDER when entering notice window.

### Manual Extensions

**US-6**: Manual extension creation with pre-filled values from config. User can override.

### Status Transitions

**US-7**: Activate DRAFT → ACTIVE: creates DRAFT rent period, supersedes previous ACTIVE.

**US-8**: Decline DRAFT with optional reason. AUTOMATIC mode won't re-create for same period.

**US-9**: Cancel DRAFT (soft delete).

### History & Display

**US-10**: Extension history on contract detail (all statuses, chronological).

**US-11**: Contract header shows effective end date + "Extended (N times)" label.

### Dashboard

**US-12**: "Upcoming Renewals" panel: contracts approaching renewal within 90 days.

**US-13**: "Pending Extensions" panel: DRAFT extensions with inline confirm/decline.

### Retroactive & Jurisdiction

**US-14**: Retroactive extensions allowed. Contract reactivates. Banner warns.

**US-15**: When selecting country_code on contract form, system suggests jurisdiction defaults for renewal config. User can accept or override.

### PDF Generation

**US-16**: Generate extension addendum PDF from extension detail page.

**US-17**: Generate rent increase letter PDF with old/new rent, effective date, legal reference.

---

## 7. UX Flows

### 7.1 Renewal Config on Contract Form

1. "Renewal Settings" section — collapsed by default
2. Renewal Mode dropdown (NONE, AUTOMATIC, MANUAL)
3. If not NONE:
   - Renewal Term (months), Max Renewals, Landlord/Tenant Notice Days
   - Require Tenant Confirmation toggle
   - Rent Adjustment type + value
   - Landlord Type (for ES), Region Code (for BE) — shown contextually
4. When country_code changes, call jurisdiction defaults API to pre-fill values

### 7.2 Extensions Tab on Contract Detail

```
┌───────────────────────────────────────────────────────────┐
│ Extensions                           [+ Create] [PDF ↓]  │
├───────────────────────────────────────────────────────────┤
│ #3  DRAFT     Jan 2028 → Jan 2029   €950 → €975 (+2.6%) │
│     Created 15 Dec 2027              [Activate] [Cancel]  │
├───────────────────────────────────────────────────────────┤
│ #2  ACTIVE    Jan 2027 → Jan 2028   €925 → €950 (+2.7%) │
│     Activated 10 Dec 2026 by L. Santos    [Addendum PDF]  │
├───────────────────────────────────────────────────────────┤
│ #1  SUPERSEDED Jan 2026 → Jan 2027  €900 → €925 (+2.8%) │
│     Activated 08 Dec 2025 by L. Santos                    │
├───────────────────────────────────────────────────────────┤
│ —   DECLINED  Jan 2026 → Jan 2027   €900 → €940          │
│     Declined 05 Dec 2025 — "Tenant requested lower adj."  │
└───────────────────────────────────────────────────────────┘
```

### 7.3 Create Extension Modal

Pre-fills from contract config. User can override. Summary line shows delta.

---

## 8. Edge Cases

| # | Scenario | Behavior |
|---|----------|----------|
| E-01 | Contract expired, no extension | Retroactive allowed. Auto-extension skips expired. |
| E-02 | Max renewals reached | AUTOMATIC: rollover to indefinite. MANUAL: disabled with tooltip. |
| E-03 | Extension declined | AUTOMATIC: no re-creation. MANUAL: new extension anytime. |
| E-04 | Multiple DRAFT extensions | Blocked (DB partial unique index). |
| E-05 | Confirmation deadline missed | DRAFT stays. Contract expires. Can confirm retroactively. |
| E-06 | Contract terminated while DRAFT | DRAFT auto-cancelled. |
| E-07 | Config changed while DRAFT pending | DRAFT NOT modified. Warning shown. |
| E-08 | Zero rent change | Extension still created. Rent period with same amount. |
| E-09 | Indefinite contract | Auto-extension skips. Warning on save. |
| E-10 | Multiple tenants | Extension applies to contract. All parties notified. |
| E-11 | Concurrent modification | Optimistic locking. First write wins. |

---

## 9. Permission Matrix

| Action | TEAM_ADMIN | TEAM_EDITOR | TEAM_VIEWER |
|--------|:----------:|:-----------:|:-----------:|
| View renewal settings | Yes | Yes | Yes (read-only) |
| Configure renewal settings | Yes | Yes | No |
| View extensions list/history | Yes | Yes | Yes |
| Create/activate/decline/cancel extension | Yes | Yes | No |
| Download PDF addendum/rent letter | Yes | Yes | Yes |
| View dashboard panels | Yes | Yes | Yes |
| Dashboard inline actions | Yes | Yes | No |

---

## 10. Database Migration

**Version**: `V034__contract_extensions.sql`

```sql
-- V034__contract_extensions.sql
-- Contract Renewals & Extensions (BUUR-78)

-- =====================================================================
-- 1. New renewal config columns on contracts
-- =====================================================================

ALTER TABLE contracts
    ADD COLUMN renewal_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN renewal_term_months INTEGER,
    ADD COLUMN max_renewals INTEGER,
    ADD COLUMN landlord_notice_days INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN tenant_notice_days INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN requires_tenant_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rent_adjustment_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN rent_adjustment_value DECIMAL(15, 4),
    ADD COLUMN landlord_type VARCHAR(20),
    ADD COLUMN region_code VARCHAR(10);

ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_renewal_mode
        CHECK (renewal_mode IN ('NONE', 'AUTOMATIC', 'MANUAL')),
    ADD CONSTRAINT chk_contracts_renewal_term_positive
        CHECK (renewal_term_months IS NULL OR renewal_term_months > 0),
    ADD CONSTRAINT chk_contracts_max_renewals_positive
        CHECK (max_renewals IS NULL OR max_renewals > 0),
    ADD CONSTRAINT chk_contracts_landlord_notice_non_negative
        CHECK (landlord_notice_days >= 0),
    ADD CONSTRAINT chk_contracts_tenant_notice_non_negative
        CHECK (tenant_notice_days >= 0),
    ADD CONSTRAINT chk_contracts_rent_adj_type
        CHECK (rent_adjustment_type IN ('NONE', 'FIXED_PERCENTAGE', 'FIXED_AMOUNT', 'MANUAL')),
    ADD CONSTRAINT chk_contracts_rent_adj_value_required
        CHECK (
            (rent_adjustment_type IN ('NONE', 'MANUAL') AND rent_adjustment_value IS NULL)
            OR (rent_adjustment_type NOT IN ('NONE', 'MANUAL') AND rent_adjustment_value IS NOT NULL)
        ),
    ADD CONSTRAINT chk_contracts_rent_adj_percentage_range
        CHECK (rent_adjustment_type != 'FIXED_PERCENTAGE'
            OR (rent_adjustment_value >= -100 AND rent_adjustment_value <= 100)),
    ADD CONSTRAINT chk_contracts_landlord_type
        CHECK (landlord_type IS NULL OR landlord_type IN ('NATURAL_PERSON', 'LEGAL_ENTITY'));

CREATE INDEX idx_contracts_landlord_type
    ON contracts (team_id, landlord_type) WHERE deleted_at IS NULL AND landlord_type IS NOT NULL;
CREATE INDEX idx_contracts_region_code
    ON contracts (team_id, country_code, region_code) WHERE deleted_at IS NULL AND region_code IS NOT NULL;

-- =====================================================================
-- 2. Data migration: auto_renewal + renewal_notice_days → new columns
-- =====================================================================

UPDATE contracts
SET renewal_mode = 'AUTOMATIC',
    landlord_notice_days = COALESCE(renewal_notice_days, 30),
    tenant_notice_days = COALESCE(renewal_notice_days, 30),
    renewal_term_months = 12
WHERE auto_renewal = TRUE AND deleted_at IS NULL;

UPDATE contracts
SET landlord_notice_days = COALESCE(renewal_notice_days, 30),
    tenant_notice_days = COALESCE(renewal_notice_days, 30)
WHERE (auto_renewal = FALSE OR auto_renewal IS NULL) AND deleted_at IS NULL;

-- =====================================================================
-- 3. Drop deprecated columns (clean break)
-- =====================================================================

ALTER TABLE contracts DROP COLUMN IF EXISTS auto_renewal;
ALTER TABLE contracts DROP COLUMN IF EXISTS renewal_notice_days;

-- =====================================================================
-- 4. Contract extensions table
-- =====================================================================

CREATE TABLE contract_extensions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    extension_number INTEGER NOT NULL,
    previous_end_date DATE NOT NULL,
    new_end_date DATE,
    previous_rent_amount BIGINT NOT NULL,
    previous_rent_currency VARCHAR(3) NOT NULL,
    new_rent_amount BIGINT NOT NULL,
    new_rent_currency VARCHAR(3) NOT NULL,
    rent_adjustment_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    rent_adjustment_value DECIMAL(15, 4),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    trigger_type VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
    rent_period_id UUID REFERENCES contract_rent_periods (id),
    notes TEXT,
    declined_reason TEXT,
    activated_at TIMESTAMP,
    activated_by UUID REFERENCES users (id),
    confirmed_at TIMESTAMP,
    confirmed_by UUID REFERENCES users (id),
    superseded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_extensions_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_extensions_contract_number UNIQUE (contract_id, extension_number),
    CONSTRAINT chk_extensions_number_positive CHECK (extension_number > 0),
    CONSTRAINT chk_extensions_rent_positive CHECK (new_rent_amount > 0 AND previous_rent_amount > 0),
    CONSTRAINT chk_extensions_dates CHECK (new_end_date IS NULL OR new_end_date > previous_end_date),
    CONSTRAINT chk_extensions_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'CANCELLED', 'DECLINED')),
    CONSTRAINT chk_extensions_trigger_type CHECK (trigger_type IN ('MANUAL', 'AUTO')),
    CONSTRAINT chk_extensions_rent_adj_type CHECK (rent_adjustment_type IN ('NONE', 'FIXED_PERCENTAGE', 'FIXED_AMOUNT', 'MANUAL'))
);

CREATE INDEX idx_extensions_team_id ON contract_extensions (team_id);
CREATE INDEX idx_extensions_contract_id ON contract_extensions (contract_id);
CREATE INDEX idx_extensions_status ON contract_extensions (status);
CREATE INDEX idx_extensions_contract_status ON contract_extensions (contract_id, status) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_extensions_contract_active_draft ON contract_extensions (contract_id) WHERE status IN ('ACTIVE', 'DRAFT') AND deleted_at IS NULL;
CREATE INDEX idx_contracts_renewal_candidates ON contracts (team_id, end_date, renewal_mode) WHERE deleted_at IS NULL AND status = 'ACTIVE' AND renewal_mode IN ('AUTOMATIC', 'MANUAL');

-- =====================================================================
-- 5. Jurisdiction defaults table
-- =====================================================================

CREATE TABLE jurisdiction_defaults (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    region_code VARCHAR(10),
    landlord_type VARCHAR(20),
    furnished BOOLEAN,
    field_name VARCHAR(50) NOT NULL,
    value VARCHAR(100) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_jd_landlord_type CHECK (landlord_type IS NULL OR landlord_type IN ('NATURAL_PERSON', 'LEGAL_ENTITY')),
    CONSTRAINT chk_jd_valid_dates CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT chk_jd_field_name CHECK (field_name IN (
        'landlord_notice_days', 'tenant_notice_days', 'renewal_term_months',
        'mandatory_term_years', 'max_rent_increase_percent', 'notice_method'))
);

CREATE UNIQUE INDEX uq_jd_active ON jurisdiction_defaults
    (country_code, COALESCE(region_code, ''), COALESCE(landlord_type, ''), COALESCE(furnished, FALSE), field_name)
    WHERE valid_until IS NULL;
CREATE INDEX idx_jd_lookup ON jurisdiction_defaults (country_code, field_name, valid_from);

-- =====================================================================
-- 6. Seed jurisdiction defaults (35 rows across 7 countries)
-- =====================================================================

-- NL
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('NL', 'landlord_notice_days', '90', '2020-01-01', 'Art. 7:271 BW — missed notice converts to indefinite'),
    ('NL', 'tenant_notice_days', '30', '2020-01-01', 'Art. 7:271 BW'),
    ('NL', 'renewal_term_months', '12', '2020-01-01', 'Typical yearly renewal');

-- DE
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('DE', 'landlord_notice_days', '90', '2020-01-01', '§ 573c BGB — increases by tenancy length'),
    ('DE', 'tenant_notice_days', '90', '2020-01-01', '§ 573c BGB'),
    ('DE', 'renewal_term_months', '0', '2020-01-01', 'Fixed-term rare, usually indefinite');

-- FR unfurnished
INSERT INTO jurisdiction_defaults (country_code, furnished, field_name, value, valid_from, notes) VALUES
    ('FR', FALSE, 'landlord_notice_days', '180', '2020-01-01', 'Loi du 6 juillet 1989'),
    ('FR', FALSE, 'tenant_notice_days', '90', '2020-01-01', '3 months (1 month in tensioned zones)'),
    ('FR', FALSE, 'renewal_term_months', '36', '2020-01-01', '3-year renewal blocks');

-- FR furnished
INSERT INTO jurisdiction_defaults (country_code, furnished, field_name, value, valid_from, notes) VALUES
    ('FR', TRUE, 'landlord_notice_days', '90', '2020-01-01', 'Loi ALUR'),
    ('FR', TRUE, 'tenant_notice_days', '30', '2020-01-01', '1 month'),
    ('FR', TRUE, 'renewal_term_months', '12', '2020-01-01', '1-year renewal blocks');

-- ES natural person
INSERT INTO jurisdiction_defaults (country_code, landlord_type, field_name, value, valid_from, notes) VALUES
    ('ES', 'NATURAL_PERSON', 'landlord_notice_days', '120', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'NATURAL_PERSON', 'tenant_notice_days', '30', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'NATURAL_PERSON', 'renewal_term_months', '12', '2020-01-01', 'Annual extensions'),
    ('ES', 'NATURAL_PERSON', 'mandatory_term_years', '5', '2020-01-01', 'LAU Art. 9');

-- ES legal entity
INSERT INTO jurisdiction_defaults (country_code, landlord_type, field_name, value, valid_from, notes) VALUES
    ('ES', 'LEGAL_ENTITY', 'landlord_notice_days', '120', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'LEGAL_ENTITY', 'tenant_notice_days', '30', '2020-01-01', 'LAU Art. 10'),
    ('ES', 'LEGAL_ENTITY', 'renewal_term_months', '12', '2020-01-01', 'Annual extensions'),
    ('ES', 'LEGAL_ENTITY', 'mandatory_term_years', '7', '2020-01-01', 'LAU Art. 9');

-- BE Brussels
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'BRU', 'landlord_notice_days', '180', '2020-01-01', 'Brussels Housing Code'),
    ('BE', 'BRU', 'tenant_notice_days', '90', '2020-01-01', 'Brussels Housing Code'),
    ('BE', 'BRU', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- BE Flanders
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'VLG', 'landlord_notice_days', '180', '2020-01-01', 'Vlaams Woninghuurdecreet'),
    ('BE', 'VLG', 'tenant_notice_days', '90', '2020-01-01', 'Vlaams Woninghuurdecreet'),
    ('BE', 'VLG', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- BE Wallonia
INSERT INTO jurisdiction_defaults (country_code, region_code, field_name, value, valid_from, notes) VALUES
    ('BE', 'WAL', 'landlord_notice_days', '180', '2020-01-01', 'Decret wallon relatif au bail'),
    ('BE', 'WAL', 'tenant_notice_days', '90', '2020-01-01', 'Decret wallon relatif au bail'),
    ('BE', 'WAL', 'renewal_term_months', '36', '2020-01-01', '3-year renewal');

-- PT
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('PT', 'landlord_notice_days', '120', '2020-01-01', 'NRAU Art. 1097'),
    ('PT', 'tenant_notice_days', '60', '2020-01-01', 'NRAU Art. 1098'),
    ('PT', 'renewal_term_months', '12', '2020-01-01', 'Typical yearly');

-- AT
INSERT INTO jurisdiction_defaults (country_code, field_name, value, valid_from, notes) VALUES
    ('AT', 'landlord_notice_days', '90', '2020-01-01', 'MRG § 30'),
    ('AT', 'tenant_notice_days', '30', '2020-01-01', 'MRG § 30'),
    ('AT', 'renewal_term_months', '36', '2020-01-01', '3-year standard');
```

After migration: `cd backend && mvn generate-sources -pl jooq -am`

---

## 11. OpenAPI Spec

### New Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/contracts/{id}/extensions` | List extensions (paginated) |
| POST | `/contracts/{id}/extensions` | Create DRAFT extension |
| GET | `/contracts/{id}/extensions/{extId}` | Get extension detail |
| POST | `/contracts/{id}/extensions/{extId}/activate` | DRAFT → ACTIVE |
| POST | `/contracts/{id}/extensions/{extId}/confirm` | Record tenant confirmation |
| POST | `/contracts/{id}/extensions/{extId}/decline` | Decline with reason |
| DELETE | `/contracts/{id}/extensions/{extId}` | Cancel (soft delete) DRAFT |
| GET | `/contracts/{id}/extensions/{extId}/addendum` | Download PDF addendum |
| GET | `/contracts/{id}/extensions/{extId}/rent-increase-letter` | Download rent increase letter PDF |
| GET | `/dashboard/upcoming-renewals` | Upcoming renewals panel data |
| GET | `/jurisdiction-defaults` | Lookup defaults by country/region/type/furnished |

### New Schemas

`RenewalMode`, `RentAdjustmentType`, `ExtensionStatus`, `LandlordType`, `CreateContractExtensionRequest`, `DeclineContractExtensionRequest`, `ContractExtensionResponse`, `PageResponseContractExtensionResponse`, `UpcomingRenewalResponse`, `JurisdictionDefaultsResponse`

### Modified Schemas

- `ContractResponse`: add `effectiveEndDate`, `renewalMode`, `renewalTermMonths`, `maxRenewals`, `landlordNoticeDays`, `tenantNoticeDays`, `requiresTenantConfirmation`, `rentAdjustmentType`, `rentAdjustmentValue`, `extensionCount`, `landlordType`, `regionCode`. Remove deprecated `autoRenewal`/`renewalNoticeDays`.
- `ContractSummary`: add `effectiveEndDate`
- `CreateContractRequest` / `UpdateContractRequest`: add renewal config fields + `landlordType`, `regionCode`

---

## 12. Module Placement & File Inventory

### New Files (36 total)

| # | Module | Path | Type |
|---|--------|------|------|
| 1 | jooq | `db/migration/V034__contract_extensions.sql` | Migration |
| 2 | common | `domain/ContractExtension.java` | Domain POJO |
| 3 | common | `domain/JurisdictionDefault.java` | Domain POJO |
| 4 | common | `domain/identifier/ContractExtensionIdentifier.java` | Typed ID |
| 5 | common | `dto/request/CreateContractExtensionRequest.java` | Request DTO |
| 6 | common | `dto/request/DeclineContractExtensionRequest.java` | Request DTO |
| 7 | common | `dto/response/ContractExtensionResponse.java` | Response DTO |
| 8 | common | `dto/response/JurisdictionDefaultResponse.java` | Response DTO |
| 9 | common | `dto/response/UpcomingRenewalResponse.java` | Response DTO |
| 10 | buurman-core | `repository/ContractExtensionRepository.java` | Repository |
| 11 | buurman-core | `repository/JurisdictionDefaultRepository.java` | Repository |
| 12 | buurman-core | `mapper/ContractExtensionRecordMapper.java` | Manual mapper |
| 13 | buurman-core | `mapper/ContractExtensionMapper.java` | MapStruct |
| 14 | buurman-core | `service/ContractExtensionService.java` | Service |
| 15 | buurman-core | `service/JurisdictionDefaultService.java` | Service |
| 16 | buurman-core | `service/EffectiveEndDateHelper.java` | Shared helper |
| 17 | buurman-core | `controller/ContractExtensionController.java` | Controller |
| 18 | buurman-core | `controller/JurisdictionDefaultController.java` | Controller |
| 19 | buurman-core | `job/AutoExtensionJob.java` | Quartz job |
| 20 | buurman-notifications | `job/RenewalReminderJob.java` | Quartz job |
| 21 | app/resources | `templates/email/contract-extended.html` | Email template |
| 22 | app/resources | `templates/email/contract-renewal-reminder.html` | Email template |
| 23 | buurman-demo-data | `service/demo/ContractExtensionDemoDataGenerator.java` | Demo data |
| 24 | buurman-booklets | `service/export/ContractExtensionAddendumExporter.java` | PDF export |
| 25 | buurman-booklets | `service/export/RentIncreaseLetterExporter.java` | PDF export |
| 26 | openapi | `src/paths/contract-extensions.yaml` | OpenAPI paths |
| 27 | openapi | `src/paths/jurisdiction-defaults.yaml` | OpenAPI paths |
| 28 | frontend | `app/src/api/contractExtensions.ts` | API module |
| 29 | frontend | `app/src/hooks/useContractExtensionHooks.ts` | React Query |
| 30 | frontend | `app/src/hooks/useJurisdictionDefaultHooks.ts` | React Query |
| 31 | frontend | `components/contracts/ExtensionStatusBadge.tsx` | Component |
| 32 | frontend | `components/contracts/CreateExtensionModal.tsx` | Component |
| 33 | frontend | `components/contracts/ExtensionTimeline.tsx` | Component |
| 34 | frontend | `components/contracts/RenewalConfigForm.tsx` | Component |
| 35 | frontend | `components/dashboard/UpcomingRenewalsPanel.tsx` | Component |
| 36 | frontend | `components/dashboard/PendingExtensionsPanel.tsx` | Component |

### Modified Files (33 total)

| # | Module | File | Change |
|---|--------|------|--------|
| 1 | common | `util/EntityPrefix.java` | Add `CEX` |
| 2 | common | `util/SidGenerator.java` | Add `newContractExtensionId()` |
| 3 | common | `domain/Contract.java` | Add enums + renewal fields + `landlordType` + `regionCode` |
| 4 | common | `domain/NotificationType.java` | Add 4 notification types |
| 5 | common | `dto/request/CreateContractRequest.java` | Add renewal config + `landlordType` + `regionCode` |
| 6 | common | `dto/request/UpdateContractRequest.java` | Same |
| 7 | common | `dto/response/ContractResponse.java` | Add `effectiveEndDate`, renewal config, `extensionCount`, `landlordType`, `regionCode` |
| 8 | buurman-core | `mapper/ContractRecordMapper.java` | Map new columns, remove `autoRenewal`/`renewalNoticeDays` |
| 9 | buurman-core | `mapper/ContractMapper.java` | Map new fields |
| 10 | buurman-core | `repository/ContractRepository.java` | New columns in save(), use `effectiveEndDate()` in queries |
| 11 | buurman-core | `service/ContractService.java` | Enrich response with effectiveEndDate + extensionCount |
| 12 | buurman-core | `service/ContractRentPeriodService.java` | Use effective end date for validation |
| 13 | buurman-core | `service/PaymentSchedulingService.java` | Use effective end date for payment generation |
| 14 | buurman-core | `service/CalendarFeedService.java` | Use effective end date |
| 15 | buurman-core | `service/ReportService.java` | Use effective end date in occupancy/income |
| 16 | buurman-core | `service/OccupancyPeriodService.java` | Use effective end date |
| 17 | buurman-core | `service/PropertyDashboardService.java` | Use effective end date |
| 18 | buurman-core | `service/DashboardService.java` | Add upcoming renewals query |
| 19 | buurman-core | `controller/DashboardController.java` | Add endpoint |
| 20 | buurman-core | `config/QuartzJobsConfig.java` | Add auto-extension job beans |
| 21 | buurman-notifications | `config/NotificationQuartzConfig.java` | Add renewal reminder beans |
| 22 | buurman-notifications | `service/NotificationSchedulerService.java` | Use effective end date; skip AUTOMATIC contracts |
| 23 | buurman-notifications | `service/notification/channel/LocalEmailSender.java` | New templates |
| 24 | buurman-notifications | `service/notification/channel/SendGridEmailSender.java` | New templates |
| 25 | buurman-notifications | `service/notification/channel/TwilioSmsSender.java` | New templates |
| 26 | buurman-notifications | `service/notification/channel/LocalSmsSender.java` | New templates |
| 27 | buurman-demo-data | `service/demo/DemoDataService.java` | Wire extension generator |
| 28 | buurman-demo-data | `service/demo/DemoContractGenerator.java` | Remove auto_renewal, add new columns |
| 29 | buurman-demo-data | `service/demo/DemoPaymentGenerator.java` | Use effective end date |
| 30 | buurman-booklets | `service/export/ContractBookletExporter.java` | Show effective end date |
| 31 | buurman-booklets | `service/export/PropertyBookletExporter.java` | Use effective end date |
| 32 | buurman-booklets | `service/export/TenantBookletExporter.java` | Use effective end date |
| 33 | app/resources | `application.yml` | Add scheduling crons |

---

## 13. Service Layer Design

### EffectiveEndDateHelper (new, shared)

Provides reusable JOOQ `Field<LocalDate>` for SQL + Java helper for loaded entities. Used by every service/repo that needs the effective end date.

### ContractExtensionService (new)

Separate from ContractService (~900 lines). Key methods:
- `createExtension()`, `activateExtension()`, `confirmExtension()`, `declineExtension()`, `cancelExtension()`
- `processAutoExtensions()`, `processRenewalReminders()` (Quartz-invoked)

**activateExtension flow** (immutable end_date — NO contract mutation):
1. Validate DRAFT status
2. Check tenant confirmation if required
3. Supersede previous ACTIVE extension
4. Set status = ACTIVE, timestamps
5. Create DRAFT rent period
6. Link rent_period_id
7. Audit log + notification
8. **No mutation of contracts.end_date** — all consumers use `EffectiveEndDateHelper`

### JurisdictionDefaultService (new)

- Cascading specificity lookup: `(country, region, landlord_type, furnished)` → `(country)`
- Returns `Map<String, String>` of field_name → value
- API endpoint: `GET /jurisdiction-defaults?countryCode=FR&furnished=true`

### DashboardService (updated)

- `getUpcomingRenewals(teamId)`: ACTIVE contracts, `renewal_mode != NONE`, effective_end_date within 90 days, no DRAFT/ACTIVE extension

---

## 14. Quartz Jobs

| Job | Module | Cron | Delegates to |
|-----|--------|------|-------------|
| `AutoExtensionJob` | buurman-core | `0 0 2 * * ?` | `ContractExtensionService.processAutoExtensions()` |
| `RenewalReminderJob` | buurman-notifications | `0 0 8 * * ?` | `ContractExtensionService.processRenewalReminders()` |

Uses `Constants.SYSTEM_USER_ID` for `created_by`. `@DisallowConcurrentExecution`.

---

## 15. Notifications

| Type | Trigger | Configurable |
|------|---------|-------------|
| `CONTRACT_RENEWAL_REMINDER` | Contract enters notice window | Yes |
| `CONTRACT_EXTENDED` | Extension activated | Yes |
| `CONTRACT_EXTENSION_PENDING` | Auto-extension awaiting confirmation | Yes |
| `CONTRACT_ROLLED_OVER_TO_INDEFINITE` | Max renewals reached | Yes |

Existing `ContractExpiryCheckJob` must skip `renewal_mode = AUTOMATIC` contracts.

---

## 16. Jurisdiction Defaults

### Lookup Cascade (most specific wins)

1. `country + region + landlord_type + furnished`
2. `country + region + landlord_type`
3. `country + region`
4. `country + landlord_type + furnished`
5. `country + landlord_type`
6. `country` (least specific)

### Supported Fields

| field_name | Type | Description |
|-----------|------|-------------|
| `landlord_notice_days` | INTEGER | Days of notice for landlord |
| `tenant_notice_days` | INTEGER | Days of notice for tenant |
| `renewal_term_months` | INTEGER | Standard renewal term (0 = indefinite) |
| `mandatory_term_years` | INTEGER | Minimum mandatory duration (ES: 5/7) |
| `max_rent_increase_percent` | DECIMAL | Advisory cap |
| `notice_method` | STRING | Required delivery method |

### Seed Data Coverage

| Country | Variants | Rows |
|---------|----------|------|
| NL | — | 3 |
| DE | — | 3 |
| FR | furnished/unfurnished | 6 |
| ES | NATURAL_PERSON/LEGAL_ENTITY | 8 |
| BE | BRU/VLG/WAL regions | 9 |
| PT | — | 3 |
| AT | — | 3 |
| **Total** | | **35** |

### Legal Disclaimer

System MUST display: "These are suggested defaults based on general legislation. They are not legal advice. Consult a local legal professional."

---

## 17. Implementation Plan

### 13 Phases — 72-99 hours total

```
Phase 1: Database Migration + JOOQ Regen                      [M, 3-4h]
    ↓
Phase 2: Drop auto_renewal/renewalNoticeDays Refactor          [M, 3-4h]
    ↓
Phase 3 + Phase 4: Domain/DTOs + OpenAPI                       [parallel, 4-5h each]
    ↓
Phase 5: Core Backend (Extensions CRUD + State Machine)        [XL, 12-16h]
    ↓
Phase 5b: Immutable end_date Refactor (14 backend files)       [L, 6-8h]
    ↓
Phase 6: Jurisdiction Defaults Backend                         [M, 4-5h]
    ↓
Phase 7 + Phase 8 + Phase 10: Jobs + Notifications + Demo      [parallel, 3-5h each]
    ↓
Phase 9: PDF Generation (Addendum + Rent Increase Letter)      [L, 6-8h]
    ↓
Phase 11: Frontend                                             [XL, 14-18h]
    ↓
Phase 12: Dashboard Panel                                      [M, 4-5h]
    ↓
Phase 13: Testing & Verification                               [L, 5-7h]
```

| Phase | Size | Hours |
|-------|------|-------|
| 1. Database + JOOQ | M | 3-4h |
| 2. Drop auto_renewal refactor | M | 3-4h |
| 3. Domain & DTOs | M | 4-5h |
| 4. OpenAPI | M | 4-5h |
| 5. Core Backend | XL | 12-16h |
| 5b. Immutable end_date refactor | L | 6-8h |
| 6. Jurisdiction Defaults | M | 4-5h |
| 7. Quartz Jobs | M | 4-5h |
| 8. Notifications | M | 3-4h |
| 9. PDF Generation | L | 6-8h |
| 10. Demo Data | M | 3-4h |
| 11. Frontend | XL | 14-18h |
| 12. Dashboard Panel | M | 4-5h |
| 13. Testing | L | 5-7h |
| **Total** | | **72-99h** |

---

## 18. Sub-Issues

Recommended split for parallel execution:

### BUUR-78a: Core Extensions Foundation [28-38h]
Phases 1-5. Migration, domain/DTOs, OpenAPI, CRUD + state machine. **Must go first.**

### BUUR-78b: Immutable end_date Refactor [6-8h]
Phase 5b. Touches 19 files. **High-risk, clean PR.** After 78a.

### BUUR-78c: Jurisdiction Defaults + Contract Metadata [8-10h]
Phase 6 + partial Phase 4. **Can parallel with 78b.**

### BUUR-78d: PDF Generation [6-8h]
Phase 9. Extension addendum + rent increase letter. **Can parallel with 78b/78c.**

### BUUR-78e: Frontend + Dashboard + Jobs + Notifications [24-35h]
Phases 7, 8, 10, 11, 12, 13. **After 78a + 78b.**

**Two engineers in parallel: ~52-73h calendar time (down from 72-99h sequential).**

---

## 19. Risk Register

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R-01 | Immutable end_date query blast radius (19 files) | HIGH | Dedicated Phase 5b. `EffectiveEndDateHelper`. Grep audit. |
| R-02 | auto_renewal column drop breaks build | MEDIUM | Dedicated Phase 2. Code and migration ship together. |
| R-03 | Jurisdiction defaults data accuracy | MEDIUM | `valid_from`/`valid_until`. Legal disclaimer. |
| R-04 | PDF legal sufficiency | MEDIUM | MVP is informational, not legally binding. Disclaimer. |
| R-05 | Extension number race condition | LOW | DB UNIQUE constraint. |
| R-06 | Effective end_date query performance | LOW | Subquery hits indexed columns. 0-1 extensions per contract. |
| R-07 | Scope creep | HIGH | Split into sub-issues. Strict MVP boundaries. |

---

## 20. Testing Strategy

### Critical Flows

1. Full lifecycle: DRAFT → ACTIVE → SUPERSEDED
2. Decline flow: no auto-recreation
3. Auto-extension: create + auto-activate
4. Rent adjustment: each type
5. Retroactive: expired contract reactivates
6. Max renewals → rollover to indefinite
7. Idempotency: job runs twice → no duplicate
8. Multi-tenancy isolation
9. Effective end_date: verify ALL 19 files use computed date correctly
10. Jurisdiction defaults: cascade lookup correctness
11. PDF generation: addendum + rent increase letter render correctly

### SQL Verification

```sql
\d contract_extensions;
\d+ contracts;
SELECT indexname FROM pg_indexes WHERE tablename = 'contract_extensions';
SELECT * FROM jurisdiction_defaults WHERE country_code = 'NL';
```

---

## 21. Deferred / Future Work

| Item | Rationale |
|------|-----------|
| CPI/Index-linked rent adjustment | Requires external index data source per country |
| Tenant self-service portal | Tenant confirmation by landlord for now |
| Market rate adjustment | Requires appraisal workflow |
| `rent_adjustment_reference_index` | Specific index per country (IRL, VPI, etc.) |
| `rent_adjustment_cap_percentage` | Legal caps per jurisdiction |
| Records migration to Java records | Deferred per BUUR-49 |
