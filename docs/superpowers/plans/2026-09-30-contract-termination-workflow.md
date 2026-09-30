# Contract Termination Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a landlord terminate a contract through a computed-notice-period wizard — `terminate` endpoint transitions `ACTIVE → NOTICE_GIVEN`, generates a notice letter, sets the deposit-return deadline, and a daily sweep job auto-transitions `NOTICE_GIVEN → TERMINATED` once the effective end date passes.

**Architecture:** A new `contract_terminations` table (one row per contract, since termination is terminal) holds notice metadata. A new `rent_regulation_termination_rules` table extends the existing rent-regulation catalog with typed per-country/region notice-day rules. `TerminationRuleResolver` applies a three-tier precedence (catalog rule → contract's own `landlordNoticeDays`/`tenantNoticeDays` → hardcoded default) — the same pattern `PaymentFormalNoticeExporter.resolveDeadlineDays()` already uses for formal-notice deadlines. `ContractStatus` gains `NOTICE_GIVEN`, requiring updates to two existing exhaustive switches (`ContractService.validateStatusTransition`, `PaymentSchedulingService.handleContractStatusChange`). A new letter exporter follows the exact `RentIncreaseLetterExporter` shape. A new daily Quartz sweep job follows the exact `ContractExpiryCheckJob` shape.

**Tech Stack:** Java 25 / Spring Boot 4 / JOOQ 3.20 (backend), React 19 / TypeScript / TanStack Query 5 (frontend), Quartz Scheduler.

**Spec:** `docs/superpowers/specs/2026-09-30-contract-termination-workflow-design.md`

## Global Constraints

- Every query filters by `team_id`, except the rent-regulation catalog tables which are deliberately global reference data (no `team_id`) — matching the existing `rent_regulation_countries`/`rent_regulation_rules` convention.
- All `if`/`else`/`for`/`while` bodies use curly braces (CLAUDE.md).
- Use idiomatic `Optional` API — never `if (opt != null)` or unchecked `.get()` (CLAUDE.md).
- Response DTOs expose only `identifier` (Sid), never internal UUIDs (CLAUDE.md).
- Set `created_by`/`updated_by` manually in repository INSERT/UPDATE (CLAUDE.md).
- Sid identifier columns are `VARCHAR(29)` (3-char entity prefix + 26-char ULID) — confirmed against every existing identifier column; do not use the narrower `VARCHAR(26)` CLAUDE.md's own shorthand note states (this exact mistake was made and caught mid-implementation on this branch already for `signature_requests`).
- **Migration numbering is NOT fixed at V079/V080 as the spec sketches** — the spec explicitly says its numbers are tentative pending final cross-plan sequencing (five BUUR-105 sub-plans were drafted in parallel). **Before Task 1's first step, run `ls backend/buurman-jooq/src/main/resources/db/migration/ | sort -V | tail -5` to find the actual current head and use the next free number(s).** At the time this plan was written the head was `V078__esignature_feature_flag.sql`, making `V079`/`V080` next-free — but another sub-plan may land first.
- `contracts.status` is a plain `VARCHAR(50)` column with **no native Postgres enum type and no CHECK constraint** (confirmed in `V004__contracts_and_payments.sql`) — adding `NOTICE_GIVEN` to `Contract.ContractStatus` needs **no migration at all**, only Java enum + OpenAPI schema changes. Do not write an `ALTER TYPE` statement (the spec's S1 flagged this as unconfirmed; it's now confirmed unnecessary).
- `ContractService.validateStatusTransition` is a **switch expression** (`boolean isValid = switch (from) {...}`) — Java requires exhaustive coverage of all enum constants here, so adding `NOTICE_GIVEN` to the enum causes a **compile error** in this method until a case is added. `PaymentSchedulingService.handleContractStatusChange` is a **switch statement** (not assigned to a variable) — technically doesn't require exhaustiveness to compile, but add an explicit `case NOTICE_GIVEN` anyway, matching the existing `case PENDING_SIGNATURE -> log.debug(...)` no-op precedent, so the switch stays self-documenting.
- The `ContractStatus` enum appears **three separate times** as an inline (non-`$ref`) enum list in `openapi/src/app.yaml` (`ContractResponse.status`, `ChangeContractStatusRequest.status`, and a third schema at line ~6113) — all three need `NOTICE_GIVEN` added, or the generated TypeScript type and Java DTOs will disagree with the domain enum.
- `TerminationRuleResolver` and its repository live in `buurman-core`, package `com.buurman.repository`/`com.buurman.service` — confirmed by `RentRegulationRepository`'s actual location (the spec's S2 left this as "buurman-backoffice or buurman-core"; it's core, since `PaymentFormalNoticeExporter` in `buurman-letters` already depends on `buurman-core`'s `RentRegulationRepository`, and this resolver needs the same dependency).

## Review Focus

- **A contract from another team can't be terminated via a guessed/leaked identifier.** `ContractTerminationService.terminate` must resolve the contract via `contractRepository.getByIdentifierAndTeamId` (throws `NotFoundException`), not any team-agnostic lookup.
- **Override without a reason must be rejected, not silently accepted.** The "override with warning" requirement is enforced server-side as a required field, not just a UI nag — a request with `effectiveEndDate` earlier than computed and no `overrideReason` must 400.
- **A contract can only be terminated once.** The `uq_contract_terminations_contract` unique constraint plus a service-level check (contract already `NOTICE_GIVEN`/`TERMINATED`) must produce a clean 409, not a constraint-violation 500.
- **Payments must not stop the instant notice is given.** `PaymentSchedulingService`'s existing effective-end-date-based cutoff already handles this correctly by design — a test must prove that transitioning to `NOTICE_GIVEN` alone does NOT cancel payments due before the effective end date (only `handleContractStatusChange`'s existing `EXPIRED, TERMINATED, DRAFT` case cancels future payments; `NOTICE_GIVEN` must be a no-op there).
- **The sweep job must not double-transition or crash on an empty result set.** A termination already `TERMINATED` must never be picked up again; a day with zero due terminations must complete cleanly (matches `ContractExpiryCheckJob`'s error-wrapping shape).

---

## Task 1: Database schema

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V<NEXT>__contract_terminations.sql`
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V<NEXT+1>__termination_notice_rules.sql`

**Interfaces:**
- Produces: `contract_terminations` and `rent_regulation_termination_rules` tables, JOOQ-generated `Tables.CONTRACT_TERMINATIONS` / `Tables.RENT_REGULATION_TERMINATION_RULES` for Task 3's repositories.

- [ ] **Step 1: Confirm the real migration head**

Run: `ls backend/buurman-jooq/src/main/resources/db/migration/ | sort -V | tail -5`
Use the next two free version numbers for the two files below (this plan assumes `V079`/`V080` — substitute the real numbers if different).

- [ ] **Step 2: Write `V079__contract_terminations.sql`**

```sql
CREATE TABLE contract_terminations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    given_by VARCHAR(16) NOT NULL,
    notice_date DATE NOT NULL,
    ground_code VARCHAR(64),
    computed_end_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    override_reason TEXT,
    inspection_date DATE,
    notice_letter_document_id UUID REFERENCES documents (id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT uq_contract_terminations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_contract_terminations_contract UNIQUE (contract_id)
);

CREATE INDEX idx_contract_terminations_team ON contract_terminations (team_id);

CREATE INDEX idx_contract_terminations_effective_end_date ON contract_terminations (effective_end_date)
WHERE status = 'NOTICE_GIVEN';
```

- [ ] **Step 3: Write `V080__termination_notice_rules.sql`**

```sql
CREATE TABLE rent_regulation_termination_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    party_type VARCHAR(16) NOT NULL,
    min_tenancy_months INTEGER,
    notice_days INTEGER NOT NULL,
    grounds_required BOOLEAN NOT NULL DEFAULT FALSE,
    grounds_codes TEXT[],
    source_url TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rrt_country ON rent_regulation_termination_rules (country_id);

-- Seed data: notice-period figures explicitly stated in BUUR-105's own ticket
-- text (NL/DE/FR only) — example/starting data, not verified legal advice.
-- A landlord relying on this for an actual termination should confirm
-- current local requirements; this computes from stored data, it doesn't
-- certify the data's correctness.
INSERT INTO rent_regulation_termination_rules (
    country_id, party_type, min_tenancy_months, notice_days, grounds_required, grounds_codes, notes
)
SELECT id, 'LANDLORD', 0, 90, TRUE, ARRAY['OWN_USE', 'RENOVATION', 'BREACH', 'OTHER_LEGAL_GROUND'],
       'BUUR-105 ticket-stated figure (3 months minimum); example data, verify against current NL law before production use.'
FROM rent_regulation_countries WHERE country_code = 'NL'
UNION ALL
SELECT id, 'LANDLORD', 0, 90, FALSE, NULL,
       'BUUR-105 ticket-stated figure (3 months, <5yr tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM rent_regulation_countries WHERE country_code = 'DE'
UNION ALL
SELECT id, 'LANDLORD', 60, 180, FALSE, NULL,
       'BUUR-105 ticket-stated figure (6 months, 5-8yr tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM rent_regulation_countries WHERE country_code = 'DE'
UNION ALL
SELECT id, 'LANDLORD', 96, 270, FALSE, NULL,
       'BUUR-105 ticket-stated figure (9 months, 8yr+ tenancy); example data, verify against current DE (BGB §573c) law before production use.'
FROM rent_regulation_countries WHERE country_code = 'DE'
UNION ALL
SELECT id, 'LANDLORD', 0, 90, FALSE, NULL,
       'BUUR-105 ticket-stated figure (3 months); example data, verify against current FR law before production use.'
FROM rent_regulation_countries WHERE country_code = 'FR'
UNION ALL
SELECT id, 'TENANT', 0, 90, FALSE, NULL,
       'BUUR-105 ticket-stated figure (3 months); example data, verify against current FR law before production use.'
FROM rent_regulation_countries WHERE country_code = 'FR';
```

Note: if `rent_regulation_countries` doesn't yet have rows for NL/DE/FR by the
time this migration runs in a given environment (it's seeded separately via
`RentRegulationCatalogService`, not by this migration), the `SELECT ... FROM
rent_regulation_countries WHERE country_code = 'NL'` clauses simply insert
zero rows for that country — not an error. Confirm this is acceptable
(it is: the resolver's fallback tiers handle a missing catalog rule
correctly) rather than adding a hard dependency between migrations and the
catalog loader's runtime seeding order.

- [ ] **Step 4: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`
Expected: BUILD SUCCESS; `Tables.CONTRACT_TERMINATIONS` and `Tables.RENT_REGULATION_TERMINATION_RULES` exist under `backend/buurman-jooq/target/generated-sources/jooq/com/buurman/jooq/generated/`.

- [ ] **Step 5: Full install**

Run: `cd backend && mvn clean install -DskipTests`
Expected: BUILD SUCCESS across all 12 modules.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V079__contract_terminations.sql backend/buurman-jooq/src/main/resources/db/migration/V080__termination_notice_rules.sql
git commit -m "feat(db): add contract_terminations and rent_regulation_termination_rules tables"
```

(Adjust filenames/message if the real next-free version numbers from Step 1 differ.)

---

## Task 2: Domain model, ContractStatus.NOTICE_GIVEN, identifiers, DTOs

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/domain/Contract.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/SidGenerator.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/identifier/ContractTerminationIdentifier.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/ContractTermination.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/ContractTerminationStatus.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/TerminationGivenBy.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/regulation/TerminationNoticeRule.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/TerminateContractRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/ContractTerminationResponse.java`

**Interfaces:**
- Produces: `Contract.ContractStatus.NOTICE_GIVEN`, `ContractTermination` domain class, `SidGenerator.newContractTerminationId()` — Task 3's repositories and Task 5's service consume these.

- [ ] **Step 1: Add `NOTICE_GIVEN` to `Contract.ContractStatus`**

In `Contract.java`, edit the enum (currently lines 36-42):

```java
  public enum ContractStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    TERMINATED,
    PENDING_SIGNATURE,
    NOTICE_GIVEN
  }
```

- [ ] **Step 2: Attempt a build to surface the two exhaustiveness sites**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am 2>&1 | grep -A3 "validateStatusTransition\|switch"`
Expected: a compile error in `ContractService.java` pointing at the switch expression in `validateStatusTransition` (it's a switch **expression**, exhaustive-required). Do not fix it in this task — Task 4 owns that edit specifically, so it gets its own focused test. Revert nothing; just confirm the error exists and matches this expectation, then move on (Task 2 doesn't need a green build yet, since `buurman-core` won't compile until Task 4 lands — the domain-only pieces in `buurman-common` below build independently).

- [ ] **Step 3: Add entity prefix + SidGenerator method**

In `EntityPrefix.java`, append:

```java
  RCO("RCO", "Rent Components"),
  CTM("CTM", "Contract Terminations");
```

(`RCO` already exists at the end of the file per the e-signature work's own additions — append `CTM` after whatever the current last entry is; do not duplicate `RCO`.)

In `SidGenerator.java`, add:

```java
  public static ContractTerminationIdentifier newContractTerminationId() {
    return ContractTerminationIdentifier.of(generateRaw(EntityPrefix.CTM));
  }
```

- [ ] **Step 4: Add the identifier class**

```java
package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class ContractTerminationIdentifier extends Sid {

  private ContractTerminationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContractTerminationIdentifier of(String value) {
    return new ContractTerminationIdentifier(value);
  }
}
```

- [ ] **Step 5: Add enums**

```java
package com.buurman.domain;

public enum ContractTerminationStatus {
  NOTICE_GIVEN,
  TERMINATED
}
```

```java
package com.buurman.domain;

public enum TerminationGivenBy {
  LANDLORD,
  TENANT
}
```

- [ ] **Step 6: Add domain classes**

`ContractTermination.java`:

```java
package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTermination {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  private TerminationGivenBy givenBy;
  private LocalDate noticeDate;
  @Builder.Default private Optional<String> groundCode = Optional.empty();
  private LocalDate computedEndDate;
  private LocalDate effectiveEndDate;
  @Builder.Default private Optional<String> overrideReason = Optional.empty();
  @Builder.Default private Optional<LocalDate> inspectionDate = Optional.empty();
  @Builder.Default private Optional<UUID> noticeLetterDocumentId = Optional.empty();
  private ContractTerminationStatus status;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
```

`regulation/TerminationNoticeRule.java`:

```java
package com.buurman.domain.regulation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.buurman.domain.TerminationGivenBy;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminationNoticeRule {

  private UUID id;
  private UUID countryId;
  @Builder.Default private Optional<UUID> regionId = Optional.empty();
  private TerminationGivenBy partyType;
  @Builder.Default private Optional<Integer> minTenancyMonths = Optional.empty();
  private int noticeDays;
  private boolean groundsRequired;
  @Builder.Default private List<String> groundsCodes = List.of();
  @Builder.Default private Optional<String> sourceUrl = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
}
```

- [ ] **Step 7: Add DTOs**

`TerminateContractRequest.java`:

```java
package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.TerminationGivenBy;

public record TerminateContractRequest(
    TerminationGivenBy givenBy,
    LocalDate noticeDate,
    Optional<String> groundCode,
    Optional<LocalDate> effectiveEndDate,
    Optional<String> overrideReason,
    Optional<LocalDate> inspectionDate) {}
```

`ContractTerminationResponse.java`:

```java
package com.buurman.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;

public record ContractTerminationResponse(
    Sid identifier,
    Sid contractIdentifier,
    TerminationGivenBy givenBy,
    LocalDate noticeDate,
    Optional<String> groundCode,
    LocalDate computedEndDate,
    LocalDate effectiveEndDate,
    Optional<String> overrideReason,
    Optional<LocalDate> inspectionDate,
    Optional<Sid> noticeLetterDocumentIdentifier,
    ContractTerminationStatus status,
    Instant createdAt,
    Instant updatedAt) {}
```

- [ ] **Step 8: Build `buurman-common` in isolation**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-common -am`
Expected: BUILD SUCCESS (this module has no dependency on `buurman-core`, so it's unaffected by the `validateStatusTransition` compile error from Step 2).

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-common
git commit -m "feat(termination): add ContractTermination domain model, NOTICE_GIVEN status, and DTOs"
```

---

## Task 3: Repositories

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/ContractTerminationRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/TerminationNoticeRuleRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/ContractTerminationRepository.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/TerminationNoticeRuleRepository.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/ContractTerminationRepositoryIntegrationTest.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java` (cleanup line)

**Interfaces:**
- Consumes: `ContractTermination`/`TerminationNoticeRule` domain classes (Task 2), `Tables.CONTRACT_TERMINATIONS`/`Tables.RENT_REGULATION_TERMINATION_RULES` (Task 1).
- Produces: `ContractTerminationRepository.save/findByIdentifierAndTeamId/getByIdentifierAndTeamId/findByContractIdAndTeamId/findDueForTransition(LocalDate)`, `TerminationNoticeRuleRepository.findByCountryAndParty(UUID countryId, TerminationGivenBy)` — Task 5's service and Task 7's sweep job consume these.

**Note:** this task does NOT fix `buurman-core`'s pre-existing compile error from Task 2's `NOTICE_GIVEN` enum addition — that's Task 4's job specifically. Build only `buurman-common` and this task's own new files' compilation in isolation where possible; the full `buurman-core -am` build won't go green until Task 4 lands. If your tooling can't easily build a subset, proceed with Task 4 immediately after this task's repository code is written but before running its tests, or coordinate with the controller on task ordering — the important thing is that Task 4's specific switch-statement fix is its own reviewable, testable unit, not silently folded into this one.

- [ ] **Step 1: Write the mappers**

`ContractTerminationRecordMapper.java` — follow the exact `SignatureRequestRecordMapper` pattern from this branch's e-signature work (MapStruct `@Mapper(componentModel = "spring")`, `Optional.of`/`Optional.ofNullable` expressions for nullable columns, enum `.valueOf`/`.name()` via expression since these are plain VARCHAR columns not JOOQ-generated enum types):

```java
package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.jooq.generated.tables.records.ContractTerminationsRecord;

@Mapper(componentModel = "spring")
public interface ContractTerminationRecordMapper {

  @Mapping(target = "identifier", expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(target = "givenBy", expression = "java(TerminationGivenBy.valueOf(record.getGivenBy()))")
  @Mapping(target = "groundCode", expression = "java(java.util.Optional.ofNullable(record.getGroundCode()))")
  @Mapping(target = "overrideReason", expression = "java(java.util.Optional.ofNullable(record.getOverrideReason()))")
  @Mapping(target = "inspectionDate", expression = "java(java.util.Optional.ofNullable(record.getInspectionDate()))")
  @Mapping(
      target = "noticeLetterDocumentId",
      expression = "java(java.util.Optional.ofNullable(record.getNoticeLetterDocumentId()))")
  @Mapping(target = "status", expression = "java(ContractTerminationStatus.valueOf(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  ContractTermination toDomain(ContractTerminationsRecord record);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }
}
```

`TerminationNoticeRuleRecordMapper.java` — same pattern, no `identifier`/audit columns since this table (like `signature_signers` on this branch) has no independent identifier — it's global reference data, not a Sid-addressable entity:

```java
package com.buurman.mapper;

import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.jooq.generated.tables.records.RentRegulationTerminationRulesRecord;

@Mapper(componentModel = "spring")
public interface TerminationNoticeRuleRecordMapper {

  @Mapping(target = "regionId", expression = "java(java.util.Optional.ofNullable(record.getRegionId()))")
  @Mapping(target = "partyType", expression = "java(TerminationGivenBy.valueOf(record.getPartyType()))")
  @Mapping(
      target = "minTenancyMonths",
      expression = "java(java.util.Optional.ofNullable(record.getMinTenancyMonths()))")
  @Mapping(
      target = "groundsCodes",
      expression =
          "java(record.getGroundsCodes() == null ? java.util.List.<String>of() :"
              + " java.util.List.of(record.getGroundsCodes()))")
  @Mapping(target = "sourceUrl", expression = "java(java.util.Optional.ofNullable(record.getSourceUrl()))")
  @Mapping(target = "notes", expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  TerminationNoticeRule toDomain(RentRegulationTerminationRulesRecord record);
}
```

(Verify `RENT_REGULATION_TERMINATION_RULES.GROUNDS_CODES`'s generated Java type for the `TEXT[]` column against the actual generated record class before finalizing this mapping — JOOQ typically generates `String[]` for a Postgres `TEXT[]` column with the default binding, but confirm rather than assume.)

- [ ] **Step 2: Write `ContractTerminationRepository`**

Follow the exact `SignatureRequestRepository` shape from this branch's e-signature work (insert-or-update branch on `id == null`, team-scoped lookups):

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACT_TERMINATIONS;
import static com.buurman.util.SidGenerator.newContractTerminationId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractTermination;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContractTerminationRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractTerminationRepository {

  private final DSLContext dsl;
  private final ContractTerminationRecordMapper mapper;
  private final Clock clock;

  public ContractTermination save(ContractTermination termination) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (termination.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newContractTerminationId();

      dsl.insertInto(CONTRACT_TERMINATIONS)
          .set(CONTRACT_TERMINATIONS.ID, newId)
          .set(CONTRACT_TERMINATIONS.IDENTIFIER, identifier)
          .set(CONTRACT_TERMINATIONS.TEAM_ID, termination.getTeamId())
          .set(CONTRACT_TERMINATIONS.CONTRACT_ID, termination.getContractId())
          .set(CONTRACT_TERMINATIONS.GIVEN_BY, termination.getGivenBy().name())
          .set(CONTRACT_TERMINATIONS.NOTICE_DATE, termination.getNoticeDate())
          .set(CONTRACT_TERMINATIONS.GROUND_CODE, termination.getGroundCode().orElse(null))
          .set(CONTRACT_TERMINATIONS.COMPUTED_END_DATE, termination.getComputedEndDate())
          .set(CONTRACT_TERMINATIONS.EFFECTIVE_END_DATE, termination.getEffectiveEndDate())
          .set(CONTRACT_TERMINATIONS.OVERRIDE_REASON, termination.getOverrideReason().orElse(null))
          .set(CONTRACT_TERMINATIONS.INSPECTION_DATE, termination.getInspectionDate().orElse(null))
          .set(
              CONTRACT_TERMINATIONS.NOTICE_LETTER_DOCUMENT_ID,
              termination.getNoticeLetterDocumentId().orElse(null))
          .set(CONTRACT_TERMINATIONS.STATUS, termination.getStatus().name())
          .set(CONTRACT_TERMINATIONS.CREATED_AT, now)
          .set(CONTRACT_TERMINATIONS.UPDATED_AT, now)
          .set(CONTRACT_TERMINATIONS.CREATED_BY, termination.getCreatedBy())
          .set(CONTRACT_TERMINATIONS.UPDATED_BY, termination.getUpdatedBy())
          .execute();

      termination.setId(newId);
      termination.setIdentifier(Optional.of(identifier));
      termination.setCreatedAt(now.toInstant(UTC));
      termination.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(CONTRACT_TERMINATIONS)
          .set(CONTRACT_TERMINATIONS.STATUS, termination.getStatus().name())
          .set(
              CONTRACT_TERMINATIONS.NOTICE_LETTER_DOCUMENT_ID,
              termination.getNoticeLetterDocumentId().orElse(null))
          .set(CONTRACT_TERMINATIONS.UPDATED_AT, now)
          .set(CONTRACT_TERMINATIONS.UPDATED_BY, termination.getUpdatedBy())
          .where(
              CONTRACT_TERMINATIONS
                  .ID
                  .eq(termination.getId())
                  .and(CONTRACT_TERMINATIONS.TEAM_ID.eq(termination.getTeamId())))
          .execute();
      termination.setUpdatedAt(now.toInstant(UTC));
    }

    return termination;
  }

  public Optional<ContractTermination> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTRACT_TERMINATIONS)
        .where(CONTRACT_TERMINATIONS.IDENTIFIER.eq(identifier).and(CONTRACT_TERMINATIONS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public ContractTermination getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contract termination not found"));
  }

  public Optional<ContractTermination> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(CONTRACT_TERMINATIONS)
        .where(
            CONTRACT_TERMINATIONS
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_TERMINATIONS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  /** Team-agnostic: used only by the daily sweep job, which has no authenticated team context. */
  public List<ContractTermination> findDueForTransition(LocalDate onOrBefore) {
    return List.copyOf(
        dsl.selectFrom(CONTRACT_TERMINATIONS)
            .where(
                CONTRACT_TERMINATIONS
                    .STATUS
                    .eq("NOTICE_GIVEN")
                    .and(CONTRACT_TERMINATIONS.EFFECTIVE_END_DATE.le(onOrBefore)))
            .fetch()
            .map(mapper::toDomain));
  }
}
```

- [ ] **Step 3: Write `TerminationNoticeRuleRepository`**

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.RENT_REGULATION_TERMINATION_RULES;

import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TerminationGivenBy;
import com.buurman.mapper.TerminationNoticeRuleRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TerminationNoticeRuleRepository {

  private final DSLContext dsl;
  private final TerminationNoticeRuleRecordMapper mapper;

  public List<TerminationNoticeRule> findByCountryAndParty(UUID countryId, TerminationGivenBy partyType) {
    return List.copyOf(
        dsl.selectFrom(RENT_REGULATION_TERMINATION_RULES)
            .where(
                RENT_REGULATION_TERMINATION_RULES
                    .COUNTRY_ID
                    .eq(countryId)
                    .and(RENT_REGULATION_TERMINATION_RULES.PARTY_TYPE.eq(partyType.name())))
            .fetch()
            .map(mapper::toDomain));
  }
}
```

(Add the missing `import com.buurman.domain.regulation.TerminationNoticeRule;` — omitted above only for line-width readability in this plan; include it in the real file.)

- [ ] **Step 4: Add cleanup line to `AbstractRepositoryIntegrationTest`**

Insert before the `dsl.deleteFrom(DSL.table("contracts")).execute();` line (FK order):

```java
    dsl.deleteFrom(DSL.table("contract_terminations")).execute();
```

(`rent_regulation_termination_rules` is global reference data seeded by migration, not per-test fixture data — don't truncate it between tests; if a test needs isolated rule data, insert team-agnostic rows scoped by a unique `country_id` the test creates itself, following how other tests avoid colliding with seeded global data.)

- [ ] **Step 5: Write `ContractTerminationRepositoryIntegrationTest`**

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.mapper.ContractTerminationRecordMapperImpl;

@DisplayName("ContractTerminationRepository")
class ContractTerminationRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContractTerminationRepository repository;
  private UUID teamAContractId;
  private UUID teamBContractId;

  @BeforeEach
  void setUp() {
    repository = new ContractTerminationRepository(dsl, new ContractTerminationRecordMapperImpl(), CLOCK);
    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);
  }

  private ContractTermination newTermination(UUID teamId, UUID contractId) {
    return ContractTermination.builder()
        .teamId(teamId)
        .contractId(contractId)
        .givenBy(TerminationGivenBy.LANDLORD)
        .noticeDate(LocalDate.of(2026, 1, 1))
        .computedEndDate(LocalDate.of(2026, 4, 1))
        .effectiveEndDate(LocalDate.of(2026, 4, 1))
        .status(ContractTerminationStatus.NOTICE_GIVEN)
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName("saves and re-reads a termination, and a team-B lookup by a team-A identifier finds nothing")
  void teamIsolation() {
    ContractTermination saved = repository.save(newTermination(TEAM_A_ID, teamAContractId));
    assertThat(saved.getIdentifier()).isPresent();

    assertThat(repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID))
        .isPresent();
    assertThat(repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
        .isEmpty();
  }

  @Test
  @DisplayName("findDueForTransition finds NOTICE_GIVEN terminations on/before the given date, ignores later ones")
  void findsDueForTransition() {
    ContractTermination due =
        newTermination(TEAM_A_ID, teamAContractId).toBuilder().effectiveEndDate(LocalDate.of(2026, 1, 10)).build();
    repository.save(due);
    ContractTermination notDue =
        newTermination(TEAM_B_ID, teamBContractId).toBuilder().effectiveEndDate(LocalDate.of(2026, 12, 31)).build();
    repository.save(notDue);

    var results = repository.findDueForTransition(LocalDate.of(2026, 1, 15));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getContractId()).isEqualTo(teamAContractId);
  }

  @Test
  @DisplayName("findByContractIdAndTeamId is team-scoped")
  void findByContractIsTeamScoped() {
    repository.save(newTermination(TEAM_A_ID, teamAContractId));

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).isPresent();
    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_B_ID)).isEmpty();
  }
}
```

(`ContractTermination` needs `@Builder(toBuilder = true)` for the `.toBuilder()` calls above — add that to Task 2's domain class if not already present; `Deposit.java` already uses this exact pattern, confirmed.)

- [ ] **Step 6: Run it, verify pass**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=ContractTerminationRepositoryIntegrationTest`
Expected: this will only succeed once Task 4's switch-statement fix has also landed, since `buurman-core` won't compile otherwise (see the note above the steps). If Task 4 hasn't landed yet, confirm this repository's own code compiles by reading it carefully rather than via a full module build, and mark this step's test run as deferred until Task 4 completes — do not skip verifying it once Task 4 is done.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/mapper/ContractTerminationRecordMapper.java backend/buurman-core/src/main/java/com/buurman/mapper/TerminationNoticeRuleRecordMapper.java backend/buurman-core/src/main/java/com/buurman/repository/ContractTerminationRepository.java backend/buurman-core/src/main/java/com/buurman/repository/TerminationNoticeRuleRepository.java backend/buurman-core/src/test/java/com/buurman/repository/ContractTerminationRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java
git commit -m "feat(termination): add ContractTermination and TerminationNoticeRule repositories"
```

---

## Task 4: Fix the two exhaustive switches for `NOTICE_GIVEN`

**Files:**
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/ContractService.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/PaymentSchedulingService.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/ContractServiceTest.java` (extend)
- Test: `backend/buurman-core/src/test/java/com/buurman/service/PaymentSchedulingServiceTest.java` (extend)

**Interfaces:**
- Produces: `ACTIVE → NOTICE_GIVEN → TERMINATED` as valid transitions in `validateStatusTransition`; `handleContractStatusChange` treats `NOTICE_GIVEN` as a payment-generation no-op — Task 6's `ContractTerminationService` relies on both.

This is a small, surgical task, deliberately separated from Task 3 so this exact change — touching two existing, widely-used methods — gets its own focused review rather than being buried inside a larger repository-adding commit.

- [ ] **Step 1: Write the failing tests first**

Add to `ContractServiceTest.java` (exact existing test class — read it first to match its mocking/fixture setup style before adding):

```java
  @Test
  @DisplayName("ACTIVE contract can transition to NOTICE_GIVEN, and NOTICE_GIVEN can transition to TERMINATED")
  void activeToNoticeGivenToTerminatedIsValid() {
    // Arrange a contract in ACTIVE status via the existing test fixture pattern in this class,
    // then call changeContractStatus(..., NOTICE_GIVEN) and assert no exception;
    // then call changeContractStatus(..., TERMINATED) from NOTICE_GIVEN and assert no exception.
  }

  @Test
  @DisplayName("NOTICE_GIVEN cannot transition back to ACTIVE (no withdrawal in this feature)")
  void noticeGivenCannotRevertToActive() {
    // Assert changeContractStatus(..., ACTIVE) from a NOTICE_GIVEN contract throws IllegalArgumentException.
  }
```

Add to `PaymentSchedulingServiceTest.java`:

```java
  @Test
  @DisplayName("handleContractStatusChange(NOTICE_GIVEN) does not cancel future payments")
  void noticeGivenDoesNotCancelPayments() {
    // Arrange a contract with future-dated payments already generated;
    // call handleContractStatusChange(contractId, NOTICE_GIVEN, teamId, userId);
    // assert the future payments still exist (contrast with the existing test proving
    // TERMINATED/EXPIRED/DRAFT DO cancel them — read that existing test first to mirror its setup).
  }
```

- [ ] **Step 2: Run both, verify they fail to compile**

Run: `mvn test -pl buurman-core -am -Dtest=ContractServiceTest,PaymentSchedulingServiceTest`
Expected: compile error — `validateStatusTransition`'s switch expression doesn't cover `NOTICE_GIVEN` yet (this is the same error Task 2 Step 2 surfaced).

- [ ] **Step 3: Fix `validateStatusTransition`**

In `ContractService.java`, change:

```java
  private void validateStatusTransition(Contract.ContractStatus from, Contract.ContractStatus to) {
    boolean isValid =
        switch (from) {
          case DRAFT -> to == PENDING_SIGNATURE || to == ACTIVE;
          case PENDING_SIGNATURE -> to == DRAFT || to == ACTIVE;
          case ACTIVE -> to == TERMINATED || to == EXPIRED || to == NOTICE_GIVEN;
          case NOTICE_GIVEN -> to == TERMINATED;
          case EXPIRED, TERMINATED -> false;
        };

    if (!isValid) {
      throw new IllegalArgumentException(
          String.format("Invalid status transition from %s to %s", from, to));
    }
  }
```

(Only the `ACTIVE` case and the new `NOTICE_GIVEN` case change; `DRAFT`/`PENDING_SIGNATURE`/`EXPIRED, TERMINATED` arms are unchanged — copy them exactly as they exist today, don't rewrite them from memory.)

- [ ] **Step 4: Fix `handleContractStatusChange`**

In `PaymentSchedulingService.java`, add a no-op case matching the existing `PENDING_SIGNATURE` precedent exactly:

```java
    switch (newStatus) {
      case ACTIVE -> {
        int paymentsCreated = generateFuturePaymentsForContract(contractId, teamId, userId);
        log.info(
            "Contract {} activated: generated {} future payments", contractId, paymentsCreated);
      }
      case EXPIRED, TERMINATED, DRAFT -> {
        int paymentsCancelled = cancelFuturePaymentsForContract(contractId, teamId, userId);
        log.info(
            "Contract {} changed to {}: cancelled {} future payments",
            contractId,
            newStatus,
            paymentsCancelled);
      }
      case PENDING_SIGNATURE ->
          log.debug("Contract {} moved to PENDING_SIGNATURE, no payment action needed", contractId);
      case NOTICE_GIVEN ->
          log.debug(
              "Contract {} moved to NOTICE_GIVEN, no payment action needed (payments already stop"
                  + " based on effective end date, not status)",
              contractId);
    }
```

- [ ] **Step 5: Run the tests, verify pass**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=ContractServiceTest,PaymentSchedulingServiceTest`
Expected: all pass, including this task's new tests.

- [ ] **Step 6: Run the full `buurman-core` suite to confirm no regression**

Run: `mvn test -pl buurman-core -am`
Expected: all pass — this is the first point where the module compiles cleanly since Task 2's enum addition, so this is the real regression check for that change too.

- [ ] **Step 7: Go back and run Task 3's repository integration test now**

Run: `mvn test -pl buurman-core -am -Dtest=ContractTerminationRepositoryIntegrationTest`
Expected: passes (deferred from Task 3 Step 6 since the module didn't compile then).

- [ ] **Step 8: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/ContractService.java backend/buurman-core/src/main/java/com/buurman/service/PaymentSchedulingService.java backend/buurman-core/src/test/java/com/buurman/service/ContractServiceTest.java backend/buurman-core/src/test/java/com/buurman/service/PaymentSchedulingServiceTest.java
git commit -m "feat(termination): allow ACTIVE->NOTICE_GIVEN->TERMINATED transition, no-op payment handling for NOTICE_GIVEN"
```

---

## Task 5: `TerminationRuleResolver`

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/regulation/TerminationRuleResolver.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/regulation/TerminationRuleResolverTest.java`

**Interfaces:**
- Consumes: `TerminationNoticeRuleRepository.findByCountryAndParty` (Task 3), `RentRegulationRepository.findCountryByCode` (existing — confirmed real method name/signature by reading `PaymentFormalNoticeExporter`'s call site).
- Produces: `TerminationRuleResolver.resolve(Contract, TerminationGivenBy, LocalDate noticeDate): TerminationComputation` — Task 6's `ContractTerminationService` calls this directly.

- [ ] **Step 1: Write the failing test first**

```java
package com.buurman.service.regulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;
import com.buurman.util.MoneyAmount;

class TerminationRuleResolverTest {

  private final RentRegulationRepository rentRegulationRepository = mock(RentRegulationRepository.class);
  private final TerminationNoticeRuleRepository terminationNoticeRuleRepository =
      mock(TerminationNoticeRuleRepository.class);

  private final TerminationRuleResolver resolver =
      new TerminationRuleResolver(rentRegulationRepository, terminationNoticeRuleRepository);

  private static final UUID COUNTRY_ID = UUID.randomUUID();

  private Contract contractStartedYearsAgo(int years, Optional<String> countryCode) {
    return Contract.builder()
        .startDate(LocalDate.now().minusYears(years))
        .countryCode(countryCode)
        .landlordNoticeDays(30)
        .tenantNoticeDays(30)
        .rentAmount(MoneyAmount.of("1000.00", "EUR"))
        .build();
  }

  @Test
  void picksCatalogRuleWhenOneMatchesTenancyLength() {
    when(rentRegulationRepository.findCountryByCode("DE"))
        .thenReturn(Optional.of(RentRegulationCountry.builder().id(COUNTRY_ID).countryCode("DE").build()));
    when(terminationNoticeRuleRepository.findByCountryAndParty(COUNTRY_ID, TerminationGivenBy.LANDLORD))
        .thenReturn(
            List.of(
                TerminationNoticeRule.builder()
                    .countryId(COUNTRY_ID)
                    .partyType(TerminationGivenBy.LANDLORD)
                    .minTenancyMonths(Optional.of(0))
                    .noticeDays(90)
                    .build(),
                TerminationNoticeRule.builder()
                    .countryId(COUNTRY_ID)
                    .partyType(TerminationGivenBy.LANDLORD)
                    .minTenancyMonths(Optional.of(96))
                    .noticeDays(270)
                    .build()));

    var result =
        resolver.resolve(
            contractStartedYearsAgo(9, Optional.of("DE")), TerminationGivenBy.LANDLORD, LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(270);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.CATALOG_RULE);
  }

  @Test
  void fallsBackToContractFieldWhenNoCatalogRuleForCountry() {
    when(rentRegulationRepository.findCountryByCode("XX")).thenReturn(Optional.empty());

    var result =
        resolver.resolve(
            contractStartedYearsAgo(1, Optional.of("XX")), TerminationGivenBy.LANDLORD, LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(30);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.CONTRACT_FALLBACK);
  }

  @Test
  void fallsBackToHardcodedDefaultWhenContractHasNoCountryCode() {
    Contract contract =
        Contract.builder()
            .startDate(LocalDate.now().minusYears(1))
            .countryCode(Optional.empty())
            .landlordNoticeDays(null)
            .rentAmount(MoneyAmount.of("1000.00", "EUR"))
            .build();

    var result = resolver.resolve(contract, TerminationGivenBy.LANDLORD, LocalDate.now());

    assertThat(result.noticeDays()).isEqualTo(TerminationRuleResolver.DEFAULT_NOTICE_DAYS);
    assertThat(result.source()).isEqualTo(TerminationRuleResolver.Source.HARDCODED_DEFAULT);
  }
}
```

(Verify `MoneyAmount.of(String, String)` is the real factory signature by checking `MoneyAmount.java` before finalizing this test — if it differs, adjust the test fixture construction accordingly; this is exactly the kind of detail to confirm against the real file rather than assume.)

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `mvn test -pl buurman-core -am -Dtest=TerminationRuleResolverTest`
Expected: compile error — `TerminationRuleResolver` doesn't exist yet.

- [ ] **Step 3: Write `TerminationRuleResolver`**

```java
package com.buurman.service.regulation;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the notice period for a contract termination. Precedence: catalog rule (most specific
 * matching min-tenancy-months threshold) &gt; contract's own landlordNoticeDays/tenantNoticeDays &gt;
 * hardcoded default — the same three-tier pattern {@code PaymentFormalNoticeExporter.resolveDeadlineDays()}
 * already uses for formal-notice deadlines.
 *
 * <p>Catalog notice-day figures are example/starting data (see the seed migration's comments), not
 * verified legal advice — this class computes from stored data, it doesn't certify correctness.
 */
@Service
@RequiredArgsConstructor
public class TerminationRuleResolver {

  static final int DEFAULT_NOTICE_DAYS = 30;

  public enum Source {
    CATALOG_RULE,
    CONTRACT_FALLBACK,
    HARDCODED_DEFAULT
  }

  public record TerminationComputation(
      int noticeDays, boolean groundsRequired, List<String> groundsCodes, Source source) {}

  private final RentRegulationRepository rentRegulationRepository;
  private final TerminationNoticeRuleRepository terminationNoticeRuleRepository;

  public TerminationComputation resolve(Contract contract, TerminationGivenBy givenBy, LocalDate noticeDate) {
    Optional<RentRegulationCountry> country =
        contract.getCountryCode().flatMap(rentRegulationRepository::findCountryByCode);

    Optional<TerminationNoticeRule> matchingRule =
        country.flatMap(c -> bestMatchingRule(c.getId(), givenBy, contract.getStartDate(), noticeDate));

    if (matchingRule.isPresent()) {
      TerminationNoticeRule rule = matchingRule.get();
      return new TerminationComputation(
          rule.getNoticeDays(), rule.isGroundsRequired(), rule.getGroundsCodes(), Source.CATALOG_RULE);
    }

    Integer contractDays =
        givenBy == TerminationGivenBy.LANDLORD ? contract.getLandlordNoticeDays() : contract.getTenantNoticeDays();
    if (contractDays != null) {
      return new TerminationComputation(contractDays, false, List.of(), Source.CONTRACT_FALLBACK);
    }

    return new TerminationComputation(DEFAULT_NOTICE_DAYS, false, List.of(), Source.HARDCODED_DEFAULT);
  }

  private Optional<TerminationNoticeRule> bestMatchingRule(
      java.util.UUID countryId, TerminationGivenBy givenBy, LocalDate contractStart, LocalDate noticeDate) {
    int tenancyMonths = Period.between(contractStart, noticeDate).toTotalMonths() >= 0
        ? (int) Period.between(contractStart, noticeDate).toTotalMonths()
        : 0;

    return terminationNoticeRuleRepository.findByCountryAndParty(countryId, givenBy).stream()
        .filter(rule -> rule.getMinTenancyMonths().map(min -> tenancyMonths >= min).orElse(true))
        .max(Comparator.comparing(rule -> rule.getMinTenancyMonths().orElse(0)));
  }
}
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=TerminationRuleResolverTest`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/regulation/TerminationRuleResolver.java backend/buurman-core/src/test/java/com/buurman/service/regulation/TerminationRuleResolverTest.java
git commit -m "feat(termination): add TerminationRuleResolver with catalog/contract/default precedence"
```

---

## Task 6: Notice letter exporter + `ContractTerminationService` + API

**Files:**
- Create: `backend/buurman-letters/src/main/resources/templates/documents/contract-termination-notice/generic.html`
- Create: `backend/buurman-letters/src/main/java/com/buurman/service/letters/ContractTerminationLetterExporter.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/service/ContractTerminationService.java`
- Modify: `openapi/src/paths/contracts.yaml` (add `terminate` and `termination-preview` anchors)
- Modify: `openapi/src/app.yaml` (register both paths, add `ContractTerminationResponse`/`TerminateContractRequest`/`TerminationPreviewResponse` schemas, add `NOTICE_GIVEN` to all three inline `ContractStatus` enum occurrences)
- Modify: `backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/ContractTerminationServiceTest.java`

**Interfaces:**
- Consumes: `TerminationRuleResolver.resolve` (Task 5), `ContractTerminationRepository.save/findByContractIdAndTeamId` (Task 3), `ContractService.changeContractStatus` (existing, now accepting `NOTICE_GIVEN` per Task 4), `DepositService` (existing — read its actual update-method signature before use), `LetterExporterHelper` (existing).
- Produces: `GET /contracts/{identifier}/termination-preview` (pure computation, no persistence) and `POST /contracts/{identifier}/terminate` — Task 8's frontend wizard calls the preview endpoint from its review step and the terminate endpoint on final confirmation. The preview endpoint exists specifically so the wizard's "review computed date" step has something real to call before the landlord commits — a dry-run flag on the mutating endpoint was considered and rejected in favor of a separate `GET`, since a pure computation has no business being expressed as a POST that merely declines to persist.

- [ ] **Step 1: Add `NOTICE_GIVEN` to the three OpenAPI `ContractStatus` enum blocks**

In `openapi/src/app.yaml`, at each of the three locations (confirmed at approximately lines 3044-3051, 3912-3919, 6113-6120 — search for `- PENDING_SIGNATURE` under a `Current lifecycle status of the contract` description to find all three exactly, since line numbers shift as the file is edited), add `- NOTICE_GIVEN` after `- PENDING_SIGNATURE` in each enum list.

- [ ] **Step 2: Add the termination schemas**

In `openapi/src/app.yaml`, next to `ContractIdentifier`:

```yaml
    ContractTerminationIdentifier:
      type: string
      description: Contract termination identifier
```

Next to `ContractResponse`:

```yaml
    TerminateContractRequest:
      type: object
      properties:
        givenBy:
          type: string
          enum: [LANDLORD, TENANT]
        noticeDate:
          type: string
          format: date
        groundCode:
          type: string
        effectiveEndDate:
          type: string
          format: date
          description: Omit to accept the computed date; providing one earlier than computed requires overrideReason
        overrideReason:
          type: string
        inspectionDate:
          type: string
          format: date
      required:
        - givenBy
        - noticeDate
    ContractTerminationResponse:
      type: object
      properties:
        identifier:
          type: string
          example: CTM01HZQX7V8B3K5M2N4P6R9T0W
        contractIdentifier:
          type: string
        givenBy:
          type: string
          enum: [LANDLORD, TENANT]
        noticeDate:
          type: string
          format: date
        groundCode:
          type: string
        computedEndDate:
          type: string
          format: date
        effectiveEndDate:
          type: string
          format: date
        overrideReason:
          type: string
        inspectionDate:
          type: string
          format: date
        noticeLetterDocumentIdentifier:
          type: string
        status:
          type: string
          enum: [NOTICE_GIVEN, TERMINATED]
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - identifier
        - contractIdentifier
        - givenBy
        - noticeDate
        - computedEndDate
        - effectiveEndDate
        - status
        - createdAt
        - updatedAt
    TerminationPreviewResponse:
      type: object
      description: A pure computation, no persistence — used by the wizard's review step before commit
      properties:
        computedEndDate:
          type: string
          format: date
        noticeDays:
          type: integer
        groundsRequired:
          type: boolean
        groundsCodes:
          type: array
          items:
            type: string
        source:
          type: string
          enum: [CATALOG_RULE, CONTRACT_FALLBACK, HARDCODED_DEFAULT]
          description: Where the notice-day figure came from, so the landlord can judge how much to trust it
      required:
        - computedEndDate
        - noticeDays
        - groundsRequired
        - source
```

- [ ] **Step 3: Add the path anchors**

In `openapi/src/paths/contracts.yaml`, append two anchors (following the exact `reopen` anchor's structure read in this task's research — same tags, same response codes):

```yaml
termination-preview:
  get:
    tags:
      - Contracts
    summary: Preview a contract termination's computed notice period
    description: Pure computation, no persistence — resolves the notice period the same way `terminate` would, for the wizard's review step
    operationId: previewContractTermination
    parameters:
      - name: identifier
        in: path
        description: Contract identifier
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
      - name: givenBy
        in: query
        required: true
        schema:
          type: string
          enum: [LANDLORD, TENANT]
      - name: noticeDate
        in: query
        required: true
        schema:
          type: string
          format: date
    responses:
      "200":
        description: OK
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TerminationPreviewResponse'
      "401":
        description: Unauthorized - missing or invalid JWT token
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "403":
        description: Forbidden - insufficient permissions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "404":
        description: Resource not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "500":
        description: Internal server error
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
    security:
      - bearer-jwt: []

terminate:
  post:
    tags:
      - Contracts
    summary: Terminate contract
    description: Give notice on an ACTIVE contract, computing the legally-required notice period and transitioning to NOTICE_GIVEN
    operationId: terminateContract
    parameters:
      - name: identifier
        in: path
        description: Contract identifier
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/TerminateContractRequest'
    responses:
      "200":
        description: Termination recorded, contract moved to NOTICE_GIVEN
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ContractTerminationResponse'
      "400":
        description: Bad request - validation error or malformed input (e.g. override without overrideReason)
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "401":
        description: Unauthorized - missing or invalid JWT token
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "403":
        description: Forbidden - insufficient permissions
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "404":
        description: Resource not found
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "409":
        description: Conflict - contract already has a termination on record
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
      "500":
        description: Internal server error
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProblemDetail'
    security:
      - bearer-jwt: []
```

Register both paths in `openapi/src/app.yaml`'s `paths:` map:

```yaml
  /contracts/{identifier}/termination-preview:
    $ref: 'paths/contracts.yaml#/termination-preview'
  /contracts/{identifier}/terminate:
    $ref: 'paths/contracts.yaml#/terminate'
```

- [ ] **Step 4: Bundle and rebuild**

Run: `make bundle-openapi && cd backend && mvn clean install -DskipTests -pl buurman-core -am`
Expected: BUILD SUCCESS; generated `terminateContract` and `previewContractTermination` methods now exist on the `ContractsApi` (or equivalent) generated interface — find the exact file and confirm both method signatures before Step 8.

- [ ] **Step 5: Write the template**

`templates/documents/contract-termination-notice/generic.html` — copy the structure of `rent-increase-letter/generic.html` (header via `_letter-styles.html` fragment, `headerVariables`, addressee block, premises block, a body section stating who is giving notice, the notice date, the ground (if present), the effective end date, and the same `th:if="${legalClause != null}"` block at the end for country-specific legal text). Variables: `givenByLabel`, `noticeDate`, `groundLabel` (empty-safe), `effectiveEndDate`, `overrideReason` (empty-safe, rendered only `th:if` present).

- [ ] **Step 6: Write `ContractTerminationLetterExporter`**

Follow `RentIncreaseLetterExporter`'s exact shape (constructor-injected repositories, `@PreAuthorize` at the same tier, `LetterExporterHelper` calls for header/addressee/premises/legal variables, `documentTemplateService.renderToPdf("contract-termination-notice", locale, vars)`). Read `RentIncreaseLetterExporter.java` in full immediately before writing this file so field names and helper call signatures match exactly — do not paraphrase from this plan's earlier research summary.

- [ ] **Step 7: Write the failing `ContractTerminationServiceTest` first**

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.TerminateContractRequest;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.letters.ContractTerminationLetterExporter;
import com.buurman.service.regulation.TerminationRuleResolver;
import com.buurman.util.MoneyAmount;

class ContractTerminationServiceTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractTerminationRepository terminationRepository = mock(ContractTerminationRepository.class);
  private final TerminationRuleResolver ruleResolver = mock(TerminationRuleResolver.class);
  private final ContractService contractService = mock(ContractService.class);
  private final ContractTerminationLetterExporter letterExporter = mock(ContractTerminationLetterExporter.class);
  private final DepositService depositService = mock(DepositService.class);

  private ContractTerminationService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private final UserPrincipal principal =
      new UserPrincipal(USER_ID, "USR1", "kc-1", "landlord@example.com", "Landlord", TEAM_ID, "TEA1", null);

  @BeforeEach
  void setUp() {
    service =
        new ContractTerminationService(
            contractRepository, terminationRepository, ruleResolver, contractService, letterExporter,
            depositService);

    Contract contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .status(Contract.ContractStatus.ACTIVE)
            .startDate(LocalDate.now().minusYears(2))
            .landlordNoticeDays(30)
            .rentAmount(MoneyAmount.of("1000.00", "EUR"))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(contract);
    when(terminationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(Optional.empty());
    when(ruleResolver.resolve(any(), any(), any()))
        .thenReturn(
            new TerminationRuleResolver.TerminationComputation(
                90, false, java.util.List.of(), TerminationRuleResolver.Source.CATALOG_RULE));
  }

  @Test
  @DisplayName("override earlier than computed without a reason is rejected")
  void overrideWithoutReasonRejected() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD,
            LocalDate.now(),
            Optional.empty(),
            Optional.of(LocalDate.now().plusDays(10)),
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(
            () -> service.terminate(ContractIdentifier.of("CON00000000000000000000001"), request, principal))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("a contract already having a termination record is rejected with a business rule error")
  void alreadyTerminatedRejected() {
    when(terminationRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(ContractTermination.builder().id(UUID.randomUUID()).build()));

    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD, LocalDate.now(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());

    assertThatThrownBy(
            () -> service.terminate(ContractIdentifier.of("CON00000000000000000000001"), request, principal))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("a valid termination transitions the contract to NOTICE_GIVEN and persists the computed date")
  void validTerminationSucceeds() {
    var request =
        new TerminateContractRequest(
            TerminationGivenBy.LANDLORD, LocalDate.now(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());

    service.terminate(ContractIdentifier.of("CON00000000000000000000001"), request, principal);

    verify(contractService)
        .changeContractStatus(
            org.mockito.ArgumentMatchers.eq(ContractIdentifier.of("CON00000000000000000000001")),
            org.mockito.ArgumentMatchers.argThat(r -> r.status() == Contract.ContractStatus.NOTICE_GIVEN),
            org.mockito.ArgumentMatchers.eq(principal));
  }
}
```

(Verify `ChangeContractStatusRequest`'s actual record field name for the status argument — the plan assumes `.status()` matching `ContractService.java`'s own `request.status()` usage already confirmed in this plan's research — and `BadRequestException`/`BusinessRuleException`'s real constructors before finalizing.)

- [ ] **Step 8: Run it, verify it fails to compile**

Run: `mvn test -pl buurman-core -am -Dtest=ContractTerminationServiceTest`
Expected: compile error — `ContractTerminationService` doesn't exist.

- [ ] **Step 9: Write `ContractTerminationService`**

```java
package com.buurman.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.TerminateContractRequest;
import com.buurman.dto.response.ContractTerminationResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.letters.ContractTerminationLetterExporter;
import com.buurman.service.regulation.TerminationRuleResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractTerminationService {

  private final ContractRepository contractRepository;
  private final ContractTerminationRepository terminationRepository;
  private final TerminationRuleResolver ruleResolver;
  private final ContractService contractService;
  private final ContractTerminationLetterExporter letterExporter;
  private final DepositService depositService;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractTerminationResponse terminate(
      ContractIdentifier identifier, TerminateContractRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (terminationRepository.findByContractIdAndTeamId(contract.getId(), teamId).isPresent()) {
      throw new BusinessRuleException("This contract already has a termination on record");
    }

    var computation = ruleResolver.resolve(contract, request.givenBy(), request.noticeDate());
    LocalDate computedEndDate = request.noticeDate().plusDays(computation.noticeDays());
    LocalDate effectiveEndDate = request.effectiveEndDate().orElse(computedEndDate);

    if (effectiveEndDate.isBefore(computedEndDate) && request.overrideReason().isEmpty()) {
      throw new BadRequestException(
          "An effective end date earlier than the computed date requires an overrideReason");
    }

    ContractTermination termination =
        terminationRepository.save(
            ContractTermination.builder()
                .teamId(teamId)
                .contractId(contract.getId())
                .givenBy(request.givenBy())
                .noticeDate(request.noticeDate())
                .groundCode(request.groundCode())
                .computedEndDate(computedEndDate)
                .effectiveEndDate(effectiveEndDate)
                .overrideReason(effectiveEndDate.isBefore(computedEndDate) ? request.overrideReason() : java.util.Optional.empty())
                .inspectionDate(request.inspectionDate())
                .status(ContractTerminationStatus.NOTICE_GIVEN)
                .createdBy(principal.getUserId())
                .updatedBy(principal.getUserId())
                .build());

    contractService.changeContractStatus(
        identifier,
        new ChangeContractStatusRequest(Contract.ContractStatus.NOTICE_GIVEN, java.util.Optional.empty()),
        principal);

    // Notice letter generation and deposit-return-deadline population happen here in Step 10/11,
    // added after this minimal version compiles and its core state-machine test passes.

    log.info(
        "Contract {} terminated: notice given {}, effective end date {}",
        identifier.value(),
        request.noticeDate(),
        effectiveEndDate);

    return toResponse(termination, identifier);
  }

  private ContractTerminationResponse toResponse(ContractTermination termination, ContractIdentifier contractIdentifier) {
    return new ContractTerminationResponse(
        termination.getIdentifier().orElseThrow(),
        com.buurman.domain.Sid.of(contractIdentifier.value()),
        termination.getGivenBy(),
        termination.getNoticeDate(),
        termination.getGroundCode(),
        termination.getComputedEndDate(),
        termination.getEffectiveEndDate(),
        termination.getOverrideReason(),
        termination.getInspectionDate(),
        java.util.Optional.empty(),
        termination.getStatus(),
        termination.getCreatedAt(),
        termination.getUpdatedAt());
  }
}
```

(Verify `ChangeContractStatusRequest`'s real constructor/record shape — the plan assumes `(Contract.ContractStatus status, Optional<String> reason)` based on `request.status()`/`request.reason()` usage already confirmed in `ContractService.changeContractStatus`'s research — before finalizing this call. Note the letter-generation and deposit-deadline steps are deliberately deferred to Step 10/11 below rather than inlined here, so this task's core state-machine logic gets tested in isolation first.)

- [ ] **Step 9b: Add `previewTermination` (no persistence)**

Add to `ContractTerminationService`, alongside `terminate`:

```java
  public record TerminationPreview(
      LocalDate computedEndDate,
      int noticeDays,
      boolean groundsRequired,
      java.util.List<String> groundsCodes,
      TerminationRuleResolver.Source source) {}

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public TerminationPreview previewTermination(
      ContractIdentifier identifier, TerminationGivenBy givenBy, LocalDate noticeDate, UserPrincipal principal) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    var computation = ruleResolver.resolve(contract, givenBy, noticeDate);
    LocalDate computedEndDate = noticeDate.plusDays(computation.noticeDays());
    return new TerminationPreview(
        computedEndDate, computation.noticeDays(), computation.groundsRequired(), computation.groundsCodes(),
        computation.source());
  }
```

A `TEAM_VIEWER` can preview (it's a read, no side effects) even though only `TEAM_ADMIN`/`TEAM_EDITOR` can actually call `terminate` — matches the read/write authorization split already used elsewhere in this codebase (e.g. `SignatureService.getSignatureRequest` vs. `createSignatureRequest` on this same branch).

Add a test: `previewTermination` returns the resolver's computation without calling `terminationRepository.save`, `contractService.changeContractStatus`, or `letterExporter.generate` (verify zero interactions on all three via `verifyNoInteractions`/`verifyNoMoreInteractions` on the relevant mocks) — this is the actual guarantee the "no persistence" claim needs a test for, not just a docstring.

- [ ] **Step 10: Wire in letter generation**

Extend `terminate(...)` between the `contractService.changeContractStatus(...)` call and the final log line:

```java
    byte[] letterPdf = letterExporter.generate(contract, termination, principal.requireTeamId());
    // Persist letterPdf as a Document the same way RentChangeDocumentGenerationService does
    // (S3StorageService.uploadFile + DocumentRepository.save) — read that class's exact
    // upload-and-save block (already grounded in this branch's e-signature plan) and mirror it
    // here, then set termination.setNoticeLetterDocumentId(Optional.of(savedDocument.getId()))
    // and re-save the termination row via terminationRepository.save(termination).
```

Add a test proving the generated letter is persisted as a `Document` linked to the contract (`entityType = "CONTRACT"`, `entityId = contract.getId()`), following the exact assertion style Task 6 of the e-signature plan used for its own document-persistence test.

- [ ] **Step 11: Wire in the deposit-return deadline**

Extend `terminate(...)`: read `DepositService`'s actual update method (find it — the earlier e-signature-adjacent research didn't need this, so read `DepositService.java` directly now) and call it to set `returnDueDate` on the contract's deposit (if one exists — `DepositRepository.findByContractIdAndTeamId` or equivalent, confirm the real method name) to `effectiveEndDate.plusDays(30)` (the spec's stated default; a jurisdiction-specific figure is out of scope per the spec's own "Out of scope" section — don't invent one here).

- [ ] **Step 12: Run the full test, verify all pass**

Run: `mvn clean install -DskipTests -pl buurman-core -am -pl buurman-letters -am && mvn test -pl buurman-core -am -Dtest=ContractTerminationServiceTest`
Expected: all pass (including the letter-persistence and deposit-deadline tests added in Steps 10-11).

- [ ] **Step 13: Wire the controller**

Add `terminateContract` and `previewContractTermination` to `ContractController.java`, delegating to `ContractTerminationService.terminate`/`.previewTermination(...)` respectively, matching the existing thin-controller pattern (`SecurityUtils.getCurrentPrincipal()`, delegate, return). Map the preview method's query params (`givenBy`, `noticeDate`) straight through — the generated interface's exact parameter names/types were confirmed in Step 4, use those, not a guess.

- [ ] **Step 14: Full module build + test**

Run: `mvn clean install -DskipTests -pl buurman-core,buurman-letters -am && mvn test -pl buurman-core,buurman-letters -am`
Expected: BUILD SUCCESS, all pass.

- [ ] **Step 15: Commit**

```bash
git add openapi/src/paths/contracts.yaml openapi/src/app.yaml openapi/app.yaml backend/buurman-letters/src/main/resources/templates/documents/contract-termination-notice backend/buurman-letters/src/main/java/com/buurman/service/letters/ContractTerminationLetterExporter.java backend/buurman-core/src/main/java/com/buurman/service/ContractTerminationService.java backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java backend/buurman-core/src/test/java/com/buurman/service/ContractTerminationServiceTest.java
git commit -m "feat(termination): add terminate endpoint, notice letter, deposit deadline"
```

---

## Task 7: `ContractTerminationSweepJob`

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/job/ContractTerminationSweepJob.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/config/QuartzJobsConfig.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/ContractTerminationServiceTest.java` (add sweep method + test, or a new `ContractTerminationSweepServiceTest` if the sweep logic is factored into its own service method rather than living directly in the `Job` class — follow whatever this codebase's existing jobs do: `ContractExpiryCheckJob` delegates to `NotificationSchedulerService.checkContractExpiry()`, a plain method on an existing service, not a new dedicated service class. Do the same: add a `sweepDueTerminations()` method to `ContractTerminationService` rather than creating a new class just to hold sweep logic.)

**Interfaces:**
- Consumes: `ContractTerminationRepository.findDueForTransition(LocalDate)` (Task 3), `ContractService.changeContractStatus` (existing).
- Produces: the automatic `NOTICE_GIVEN → TERMINATED` transition the spec's S3 describes.

- [ ] **Step 1: Write the failing test**

Add to `ContractTerminationServiceTest.java`:

```java
  @Test
  @DisplayName("sweepDueTerminations transitions a NOTICE_GIVEN termination past its effective end date to TERMINATED")
  void sweepTransitionsDueTermination() {
    ContractTermination due =
        ContractTermination.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .status(ContractTerminationStatus.NOTICE_GIVEN)
            .effectiveEndDate(LocalDate.now().minusDays(1))
            .build();
    when(terminationRepository.findDueForTransition(any())).thenReturn(java.util.List.of(due));

    service.sweepDueTerminations();

    verify(terminationRepository)
        .save(org.mockito.ArgumentMatchers.argThat(t -> t.getStatus() == ContractTerminationStatus.TERMINATED));
  }

  @Test
  @DisplayName("sweepDueTerminations does nothing when no terminations are due")
  void sweepNoOpWhenNoneDue() {
    when(terminationRepository.findDueForTransition(any())).thenReturn(java.util.List.of());

    service.sweepDueTerminations(); // must not throw

    org.mockito.Mockito.verify(terminationRepository, org.mockito.Mockito.never()).save(any());
  }
```

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `mvn test -pl buurman-core -am -Dtest=ContractTerminationServiceTest`
Expected: compile error — `sweepDueTerminations` doesn't exist.

- [ ] **Step 3: Add `sweepDueTerminations` to `ContractTerminationService`**

```java
  public void sweepDueTerminations() {
    var due = terminationRepository.findDueForTransition(LocalDate.now());
    for (ContractTermination termination : due) {
      termination.setStatus(ContractTerminationStatus.TERMINATED);
      terminationRepository.save(termination);
      // Resolve the contract's identifier to call changeContractStatus — the termination row only
      // holds contractId (UUID); look it up via contractRepository.findByIdAndTeamId(...) and use
      // its identifier, following the exact resolution pattern used elsewhere in this service.
      log.info("Termination for contract {} swept to TERMINATED", termination.getContractId());
    }
  }
```

(Requires transitioning the underlying contract's status too, via `contractService.changeContractStatus`, not just the termination row — write this fully in the real file; the sketch above omits it only for plan brevity, and the test above should be extended to also verify `contractService.changeContractStatus` was called with `TERMINATED`.)

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=ContractTerminationServiceTest`
Expected: all pass.

- [ ] **Step 5: Write the Quartz job**

```java
package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.ContractTerminationService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ContractTerminationSweepJob implements Job {

  private final ContractTerminationService contractTerminationService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      contractTerminationService.sweepDueTerminations();
    } catch (Exception e) {
      throw new JobExecutionException("Contract termination sweep failed", e);
    }
  }
}
```

- [ ] **Step 6: Register the job**

In `QuartzJobsConfig.java`, following the exact `contractExpiryCheckJobDetail`/`contractExpiryCheckTrigger` bean pair's structure (read that pair's full code first, including its `@Value` cron property injection), add:

```java
  @Bean
  public JobDetail contractTerminationSweepJobDetail() {
    return JobBuilder.newJob(ContractTerminationSweepJob.class)
        .withIdentity("contractTerminationSweepJob", "scheduling")
        .storeDurably()
        .build();
  }

  @Bean
  public Trigger contractTerminationSweepTrigger(
      JobDetail contractTerminationSweepJobDetail,
      @Value("${buurman.jobs.contract-termination-sweep.cron:0 30 1 * * ?}") String cron) {
    return TriggerBuilder.newTrigger()
        .forJob(contractTerminationSweepJobDetail)
        .withIdentity("contractTerminationSweepTrigger", "scheduling")
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
  }
```

(Confirm `JobBuilder`/`.storeDurably()` and the `@Value` cron-property convention exactly against `contractExpiryCheckJobDetail`'s real code before finalizing — this plan's sketch follows the pattern described in this plan's own research but the implementer must verify the literal syntax, not just the shape.)

- [ ] **Step 7: Full build + test**

Run: `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am`
Expected: BUILD SUCCESS, all pass.

- [ ] **Step 8: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/job/ContractTerminationSweepJob.java backend/buurman-core/src/main/java/com/buurman/config/QuartzJobsConfig.java backend/buurman-core/src/main/java/com/buurman/service/ContractTerminationService.java backend/buurman-core/src/test/java/com/buurman/service/ContractTerminationServiceTest.java
git commit -m "feat(termination): add daily sweep job to auto-transition NOTICE_GIVEN to TERMINATED"
```

---

## Task 8: Frontend — termination wizard

**Files:**
- Create: `frontend/app/src/pages/TerminationWizardPage.tsx`
- Create: `frontend/app/src/components/contractTermination/WhoGivesNoticeStep.tsx`
- Create: `frontend/app/src/components/contractTermination/NoticeDateAndGroundStep.tsx`
- Create: `frontend/app/src/components/contractTermination/ReviewComputedDateStep.tsx`
- Create: `frontend/app/src/components/contractTermination/LetterPreviewStep.tsx`
- Create: `frontend/app/src/components/contractTermination/ConfirmationStep.tsx`
- Create: `frontend/app/src/hooks/useContractTerminationHooks.ts`
- Modify: `frontend/app/src/components/contracts/ContractStatusBadge.tsx`
- Modify: routing (wherever `RentIncreaseWizardPage` is routed — add a sibling route)
- Test: `frontend/app/src/pages/__tests__/TerminationWizardPage.test.tsx` (or wherever this codebase's wizard-page tests live — check for `RentIncreaseWizardPage.test.tsx` first and mirror its location/structure)

**Interfaces:**
- Consumes: generated `terminateContract(contractIdentifier, request)` (from Task 6's OpenAPI bundle, `yarn generate:api`).

- [ ] **Step 1: Regenerate the frontend API client**

Run: `cd frontend && yarn generate:api`
Expected: `frontend/app/src/generated/api/contracts/contracts.ts` gains `terminateContract`; `ContractResponseStatus`/generated types include `NOTICE_GIVEN`.

- [ ] **Step 2: Read `RentIncreaseWizardPage.tsx` and its step components in full**

Before writing anything, read the real files (not this plan's earlier research summary) to match the exact `WizardStep` union pattern, `STEPS` array construction, step-indicator markup, and how step components receive state/setters/`onNext`/`onBack` as props.

- [ ] **Step 3: Write `useContractTerminationHooks.ts`**

```ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  terminateContract,
  previewContractTermination,
} from '../generated/api/contracts/contracts';
import type { TerminateContractRequest, TerminationGivenBy } from '../generated/models';
import { queryKeys } from '../lib/queryKeys';

export const useTerminationPreview = (
  contractId: string | undefined,
  givenBy: TerminationGivenBy | undefined,
  noticeDate: string | undefined
) => {
  return useQuery({
    queryKey: queryKeys.contracts.terminationPreview(contractId, givenBy, noticeDate),
    queryFn: () =>
      previewContractTermination(contractId ?? '', {
        givenBy: givenBy as TerminationGivenBy,
        noticeDate: noticeDate ?? '',
      }),
    enabled: !!contractId && !!givenBy && !!noticeDate,
  });
};

export const useTerminateContract = (contractId: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Contract termination recorded',
    mutationFn: (request: TerminateContractRequest) =>
      terminateContract(contractId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.contracts.detail(contractId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.contracts.all() });
    },
  });
};
```

Add a `terminationPreview: (contractId?, givenBy?, noticeDate?) => k('terminationPreview', contractId, givenBy, noticeDate)` entry to `queryKeys.ts`'s `contracts` section, matching the existing key-builder convention (verify `queryKeys.contracts.detail`/`.all`'s real shape by reading the file — this plan assumes the pattern already confirmed elsewhere on this branch for contracts).

- [ ] **Step 4: Write the five step components + `TerminationWizardPage.tsx`**

Mirror `RentIncreaseWizardPage.tsx` structure exactly (per Step 2's reading): a `WizardStep` union (`'who-gives-notice' | 'notice-date-ground' | 'review' | 'letter-preview' | 'confirmation'`), `useState<WizardStep>`, a `STEPS` array driving the indicator pills, conditional rendering per step.

`ReviewComputedDateStep` is the load-bearing one per the spec: on entering this step (and whenever `givenBy`/`noticeDate` change on the prior step), call `useTerminationPreview(contractId, givenBy, noticeDate)` — a new read-only hook wrapping the generated `previewContractTermination(contractIdentifier, { givenBy, noticeDate })` (Task 6's `GET .../termination-preview`) — to fetch `computedEndDate`/`noticeDays`/`source` without any risk of side effects (it's a plain `useQuery`, not a mutation). Display the computed date prominently, the `source` value in plain language ("based on your country's notice-period rules" for `CATALOG_RULE`, "based on this contract's own notice setting" for `CONTRACT_FALLBACK`, "using a 30-day default" for `HARDCODED_DEFAULT`) so the landlord can judge how much to trust the figure, and only THEN let them optionally type an earlier `effectiveEndDate`, revealing the `overrideReason` field the moment that date is earlier than `computedEndDate`. The final `terminate` mutation (from `useTerminateContract`, `ConfirmationStep`) is a completely separate call, made once, only on final submission.

- [ ] **Step 5: `ContractStatusBadge` gains `NOTICE_GIVEN`**

```tsx
const statusConfig: Record<
  ContractStatus,
  { label: string; color: BadgeColorVariant }
> = {
  DRAFT: { label: 'Draft', color: 'gray' },
  PENDING_SIGNATURE: { label: 'Pending Signature', color: 'blue' },
  ACTIVE: { label: 'Active', color: 'emerald' },
  EXPIRED: { label: 'Expired', color: 'orange' },
  TERMINATED: { label: 'Terminated', color: 'red' },
  NOTICE_GIVEN: { label: 'Notice Given', color: 'amber' },
};
```

(Confirm `'amber'` is a real `BadgeColorVariant` value by checking `@buurman/ui`'s `StatusBadge` component before using it — substitute the closest available warning-toned color if not.)

Add the `statusChange.statuses.NOTICE_GIVEN` i18n key to all 13 locale bundles under the `contracts` namespace, guarded by this branch's existing i18n parity check.

- [ ] **Step 6: Wire the route**

Add a route for `TerminationWizardPage` next to wherever `RentIncreaseWizardPage` is routed (find it — likely `App.tsx` or a routes config file), and a "Terminate contract" action/button on the contract detail page that navigates to it (only shown for `ACTIVE` contracts, matching the state machine).

- [ ] **Step 7: Write tests**

Component test per step (renders correctly, calls `onNext`/`onBack`), and one integration-style test for `TerminationWizardPage` proving the override-reason field only appears after the landlord edits the date earlier than computed (the Review Focus item from the spec).

- [ ] **Step 8: Run tests + lint**

Run: `cd frontend && yarn test && yarn lint`
Expected: all pass, clean.

- [ ] **Step 9: Commit**

```bash
git add frontend/app/src
git commit -m "feat(termination): add termination wizard UI"
```

---

## Final verification (whole-branch)

- [ ] Run: `make test` — full backend suite.
- [ ] Run: `cd frontend && yarn test && yarn lint`.
- [ ] Run: `make bundle-openapi && git diff --exit-code openapi/app.yaml` — no diff.
- [ ] Manually confirm: a DRAFT contract cannot be terminated (state machine rejects it); an ACTIVE contract's termination flow produces a `NOTICE_GIVEN` status, a stored notice letter `Document`, and (if a deposit exists) an updated `returnDueDate`.
