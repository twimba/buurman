# Lease Agreement Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a landlord generate the initial lease agreement PDF for a contract, composed from a country-keyed, backoffice-managed clause library that a landlord can toggle per contract — replacing "no way to generate the lease itself" with a working (if legally-unvetted-placeholder-content) generation flow.

**Architecture:** One generic Thymeleaf template (`lease-agreement/generic.html`) renders an ordered list of resolved clauses; country variation is entirely which clauses a country's clause set includes by default, not separate template files. `lease_clause_templates` (global reference data, no `team_id`, mirrors the rent-regulation catalog's shape) holds the clause structure; `contract_lease_clauses` holds per-contract include/exclude overrides. Clause body/title text lives in the existing `.properties`/`MessageSource` i18n mechanism, not a new content column.

**Tech Stack:** Java 25 / Spring Boot 4 / JOOQ (backend), Thymeleaf + Gotenberg (PDF), React 19 / TypeScript / TanStack Query (frontend).

**Spec:** `docs/superpowers/specs/2026-09-30-lease-agreement-generation-design.md`

## Global Constraints

- Every query filters by `team_id`, **except** `lease_clause_templates` — it is deliberately global reference data with no `team_id` column, same reasoning as `feature_flags`. Do not add one.
- All `if`/`else`/`for`/`while` bodies use curly braces (CLAUDE.md).
- Idiomatic `Optional` API only — no `if (opt != null)`, no unchecked `.get()` (CLAUDE.md).
- Response DTOs expose only `identifier` (Sid), never internal UUIDs (CLAUDE.md).
- **Migration numbering must be re-derived at implementation time.** This plan's code uses `V079`/`V080` as its expected assignment (confirmed against the real migration directory when this plan was written — head was `V078`), but three other BUUR-105 sub-plans may land migrations first. Task 1's first step is: run `ls backend/buurman-jooq/src/main/resources/db/migration/ | sort -V | tail -5` and use whatever is actually next-free. If it isn't `V079`, renumber every migration filename and every in-plan reference to it accordingly, and say so in the task's commit message and report.
- **Every seeded clause body is placeholder legal content, not vetted legal text.** Every task that writes clause body copy (Task 8) must mark it with a code comment stating this, and the backoffice UI (Task 5's frontend half, folded into Task 9) must show a persistent banner saying the same. No user-facing copy anywhere may imply the generated lease is legally reviewed.
- Adding a new `.properties` message-bundle family requires updating the **pinned** family list in `backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java`'s `families()` method (`containsExactlyInAnyOrder(...)`) — this test fails deliberately on an unlisted family, by design (it exists to make a missing bundle a build failure, not a silent gap). Task 8 must edit both the bundle files and this pinned list together.
- Backoffice authorization is `@PreAuthorize("hasRole('BACKOFFICE_ADMIN')")` at the **service** layer (confirmed in `RentRegulationCatalogService`), not the controller — the `/backoffice/**` Spring Security filter chain only enforces "is an authenticated backoffice principal," not the admin role.

## Review Focus

- **A non-optional clause excluded via the PUT endpoint**: the spec requires server-side rejection (400), not just a disabled checkbox in the UI — a request that includes `included: false` for a clause with `optional = false` must be rejected, and no test in a hastily-written plan would catch a UI-only enforcement gap. (Task 7 — `LeaseClauseServiceTest`.)
- **A contract whose country has zero seeded clause templates**: generating a lease for e.g. a GB contract before GB's clauses are seeded (or any future 8th country with none yet) must produce a clear 4xx, not an empty/broken PDF or an NPE. (Task 4 — `LeaseClauseResolverTest`.)
- **Cross-team leakage via `contract_lease_clauses`**: a landlord from Team B must never see or affect Team A's per-contract clause overrides, even though `lease_clause_templates` itself is intentionally team-agnostic. (Task 3 — `ContractLeaseClauseRepositoryIntegrationTest`.)
- **The i18n parity test must actually pass after Task 8**: a plan that seeds English-only clause text and forgets the other 12 language files will pass code review but fail `mvn test` on an existing, unrelated-looking test class. (Task 8 — explicit step running `I18nBundleParityTest`.)
- **Re-generating a lease agreement after a clause selection change**: the second `POST .../lease-agreement` call must reflect the landlord's latest include/exclude choices, not a cached/stale resolution from the first generation. (Task 6 — `LeaseAgreementExporterTest`.)

---

## Task 1: Database schema

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V079__lease_clause_library.sql` (or the renumbered equivalent — see Global Constraints)

**Interfaces:**
- Produces: tables `lease_clause_templates`, `contract_lease_clauses`; JOOQ-generated `Tables.LEASE_CLAUSE_TEMPLATES` / `Tables.CONTRACT_LEASE_CLAUSES` for Task 3's repositories.

- [ ] **Step 1: Confirm the real next-free migration version**

Run: `ls backend/buurman-jooq/src/main/resources/db/migration/ | sort -V | tail -5`
If the highest version isn't `V078`, every version number in this task (and this plan's later reference to `V080` seed data in Task 8) shifts accordingly — substitute throughout.

- [ ] **Step 2: Write the migration**

```sql
CREATE TABLE lease_clause_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    clause_key VARCHAR(64) NOT NULL,
    title_i18n_key VARCHAR(128) NOT NULL,
    body_i18n_key VARCHAR(128) NOT NULL,
    default_included BOOLEAN NOT NULL DEFAULT TRUE,
    optional BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_lease_clause_templates_identifier UNIQUE (identifier),
    CONSTRAINT uq_lease_clause_templates_country_key_version UNIQUE (country_code, clause_key, version)
);

CREATE INDEX idx_lease_clause_templates_country ON lease_clause_templates (country_code)
WHERE deleted_at IS NULL;

CREATE TABLE contract_lease_clauses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    clause_template_id UUID NOT NULL REFERENCES lease_clause_templates (id),
    included BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT uq_contract_lease_clauses_contract_template UNIQUE (contract_id, clause_template_id)
);

CREATE INDEX idx_contract_lease_clauses_contract ON contract_lease_clauses (contract_id);
CREATE INDEX idx_contract_lease_clauses_team ON contract_lease_clauses (team_id);
```

- [ ] **Step 3: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`
Expected: BUILD SUCCESS; `Tables.LEASE_CLAUSE_TEMPLATES`/`Tables.CONTRACT_LEASE_CLAUSES` and their `*Record` classes exist under `backend/buurman-jooq/target/generated-sources/jooq/`.

- [ ] **Step 4: Full install**

Run: `cd backend && mvn clean install -DskipTests`
Expected: BUILD SUCCESS across all 12 modules.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V079__lease_clause_library.sql
git commit -m "feat(db): add lease_clause_templates/contract_lease_clauses tables"
```

---

## Task 2: Domain model, identifiers, DTOs (buurman-common)

**Files:**
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/SidGenerator.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/identifier/LeaseClauseTemplateIdentifier.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/LeaseClauseTemplate.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/ContractLeaseClause.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/LeaseClauseTemplateResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/ResolvedLeaseClauseResponse.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/UpdateContractLeaseClausesRequest.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/request/backoffice/UpsertLeaseClauseTemplateRequest.java`

**Interfaces:**
- Produces: `SidGenerator.newLeaseClauseTemplateId(): LeaseClauseTemplateIdentifier`; `LeaseClauseTemplate`/`ContractLeaseClause` domain classes (Task 3's repositories); response/request DTOs (Task 5/7's controllers).

- [ ] **Step 1: Add entity prefix**

Append to `EntityPrefix.java` (non-alphabetical file, append at end — matches this branch's established convention):

```java
  LCT("LCT", "Lease Clause Templates");
```

- [ ] **Step 2: Add identifier class**

`LeaseClauseTemplateIdentifier.java` — follow `SignatureRequestIdentifier`'s exact shape (a `Sid` subclass, private constructor, `@JsonCreator static of(String)`):

```java
package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class LeaseClauseTemplateIdentifier extends Sid {

  private LeaseClauseTemplateIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static LeaseClauseTemplateIdentifier of(String value) {
    return new LeaseClauseTemplateIdentifier(value);
  }
}
```

`contract_lease_clauses` has no `identifier` column (it's a per-contract override row, never addressed directly by its own Sid — same reasoning as `signature_signers` on this branch) — no identifier class for it.

- [ ] **Step 3: Add SidGenerator method**

```java
  public static LeaseClauseTemplateIdentifier newLeaseClauseTemplateId() {
    return LeaseClauseTemplateIdentifier.of(generateRaw(EntityPrefix.LCT));
  }
```

- [ ] **Step 4: Add domain classes**

`LeaseClauseTemplate.java`:

```java
package com.buurman.domain;

import java.time.Instant;
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
public class LeaseClauseTemplate {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String countryCode;
  private String clauseKey;
  private String titleI18nKey;
  private String bodyI18nKey;
  private boolean defaultIncluded;
  private boolean optional;
  private int sortOrder;
  private int version;
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
```

`ContractLeaseClause.java`:

```java
package com.buurman.domain;

import java.time.Instant;
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
public class ContractLeaseClause {

  private UUID id;
  private UUID teamId;
  private UUID contractId;
  private UUID clauseTemplateId;
  private boolean included;
  private int sortOrder;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
```

- [ ] **Step 5: Add response/request DTOs**

`ResolvedLeaseClauseResponse.java` — this is the shape both the `GET .../lease-clauses` endpoint and the template-rendering variable map use (title/body already resolved to the contract's locale, not raw keys):

```java
package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record ResolvedLeaseClauseResponse(
    Sid templateIdentifier,
    String clauseKey,
    String title,
    String body,
    boolean included,
    boolean optional,
    int sortOrder) {}
```

`LeaseClauseTemplateResponse.java` (backoffice CRUD response — exposes the raw i18n keys, not resolved text, since backoffice manages structure not translated copy):

```java
package com.buurman.dto.response;

public record LeaseClauseTemplateResponse(
    Sid identifier,
    String countryCode,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    int sortOrder,
    int version) {}
```

(Add the `import com.buurman.domain.Sid;` — omitted above only for brevity of the diff; include it in the actual file.)

`UpdateContractLeaseClausesRequest.java`:

```java
package com.buurman.dto.request;

import java.util.List;

public record UpdateContractLeaseClausesRequest(List<ClauseSelection> clauses) {
  public record ClauseSelection(String templateIdentifier, boolean included, int sortOrder) {}
}
```

`UpsertLeaseClauseTemplateRequest.java` (`backend/buurman-common/src/main/java/com/buurman/dto/request/backoffice/`):

```java
package com.buurman.dto.request.backoffice;

public record UpsertLeaseClauseTemplateRequest(
    String countryCode,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    int sortOrder) {}
```

- [ ] **Step 6: Build**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-common -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-common
git commit -m "feat(domain): add LeaseClauseTemplate/ContractLeaseClause domain model and DTOs"
```

---

## Task 3: Repositories (buurman-core)

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/LeaseClauseTemplateRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/mapper/ContractLeaseClauseRecordMapper.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/LeaseClauseTemplateRepository.java`
- Create: `backend/buurman-core/src/main/java/com/buurman/repository/ContractLeaseClauseRepository.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/LeaseClauseTemplateRepositoryIntegrationTest.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/ContractLeaseClauseRepositoryIntegrationTest.java`
- Modify: `backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java` (add cleanup lines for both new tables, in FK order — `contract_lease_clauses` before `lease_clause_templates`, both before `contracts`)

**Interfaces:**
- Consumes: `LeaseClauseTemplate`/`ContractLeaseClause` domain classes (Task 2).
- Produces: `LeaseClauseTemplateRepository.findByCountryCode/save/findByIdentifier/softDeleteByIdentifier`, `ContractLeaseClauseRepository.findByContractIdAndTeamId/replaceForContract` — Task 4 (resolver) and Task 5/7 (controllers) call these directly.

- [ ] **Step 1: Write the mappers**

`LeaseClauseTemplateRecordMapper.java` — MapStruct interface, following `SignatureRequestRecordMapper`'s exact shape (`@Mapper(componentModel = "spring")`, `Optional.of(record.getIdentifier())`, boolean/int columns map straight through with no expression needed):

```java
package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.jooq.generated.tables.records.LeaseClauseTemplatesRecord;

@Mapper(componentModel = "spring")
public interface LeaseClauseTemplateRecordMapper {

  @Mapping(target = "identifier", expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(
      target = "deletedAt",
      expression =
          "java(java.util.Optional.ofNullable(record.getDeletedAt()).map(dt ->"
              + " dt.toInstant(java.time.ZoneOffset.UTC)))")
  @Mapping(target = "createdAt", expression = "java(record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
  @Mapping(target = "updatedAt", expression = "java(record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))")
  LeaseClauseTemplate toDomain(LeaseClauseTemplatesRecord record);
}
```

`ContractLeaseClauseRecordMapper.java` — same shape, no `identifier`/`deletedAt` fields:

```java
package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.jooq.generated.tables.records.ContractLeaseClausesRecord;

@Mapper(componentModel = "spring")
public interface ContractLeaseClauseRecordMapper {

  @Mapping(target = "createdAt", expression = "java(record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
  @Mapping(target = "updatedAt", expression = "java(record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))")
  ContractLeaseClause toDomain(ContractLeaseClausesRecord record);
}
```

(If JOOQ's generated `getCreatedAt()`/`getDeletedAt()` return `LocalDateTime` rather than needing the `.toInstant(...)` call shape shown — confirm against the actual generated record class from Task 1's codegen output before finalizing; match whatever the real generated getter signature is, following the exact pattern already used by `SignatureRequestRecordMapper`/`DocumentRecordMapper` for the same column types.)

- [ ] **Step 2: Write `LeaseClauseTemplateRepository`**

No `team_id` filtering anywhere in this repository — deliberately, per Global Constraints.

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.LEASE_CLAUSE_TEMPLATES;
import static com.buurman.util.SidGenerator.newLeaseClauseTemplateId;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.LeaseClauseTemplateRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LeaseClauseTemplateRepository {

  private final DSLContext dsl;
  private final LeaseClauseTemplateRecordMapper mapper;
  private final Clock clock;

  public List<LeaseClauseTemplate> findByCountryCode(String countryCode) {
    return List.copyOf(
        dsl.selectFrom(LEASE_CLAUSE_TEMPLATES)
            .where(
                LEASE_CLAUSE_TEMPLATES
                    .COUNTRY_CODE
                    .eq(countryCode)
                    .and(LEASE_CLAUSE_TEMPLATES.DELETED_AT.isNull()))
            .orderBy(LEASE_CLAUSE_TEMPLATES.SORT_ORDER.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<LeaseClauseTemplate> findByIdentifier(Sid identifier) {
    return dsl.selectFrom(LEASE_CLAUSE_TEMPLATES)
        .where(
            LEASE_CLAUSE_TEMPLATES
                .IDENTIFIER
                .eq(identifier)
                .and(LEASE_CLAUSE_TEMPLATES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public LeaseClauseTemplate getByIdentifier(Sid identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Lease clause template not found"));
  }

  public LeaseClauseTemplate save(LeaseClauseTemplate template) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (template.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newLeaseClauseTemplateId();

      dsl.insertInto(LEASE_CLAUSE_TEMPLATES)
          .set(LEASE_CLAUSE_TEMPLATES.ID, newId)
          .set(LEASE_CLAUSE_TEMPLATES.IDENTIFIER, identifier)
          .set(LEASE_CLAUSE_TEMPLATES.COUNTRY_CODE, template.getCountryCode())
          .set(LEASE_CLAUSE_TEMPLATES.CLAUSE_KEY, template.getClauseKey())
          .set(LEASE_CLAUSE_TEMPLATES.TITLE_I18N_KEY, template.getTitleI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.BODY_I18N_KEY, template.getBodyI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.DEFAULT_INCLUDED, template.isDefaultIncluded())
          .set(LEASE_CLAUSE_TEMPLATES.OPTIONAL, template.isOptional())
          .set(LEASE_CLAUSE_TEMPLATES.SORT_ORDER, template.getSortOrder())
          .set(LEASE_CLAUSE_TEMPLATES.VERSION, template.getVersion())
          .set(LEASE_CLAUSE_TEMPLATES.CREATED_AT, now)
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_AT, now)
          .execute();

      template.setId(newId);
      template.setIdentifier(Optional.of(identifier));
    } else {
      dsl.update(LEASE_CLAUSE_TEMPLATES)
          .set(LEASE_CLAUSE_TEMPLATES.TITLE_I18N_KEY, template.getTitleI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.BODY_I18N_KEY, template.getBodyI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.DEFAULT_INCLUDED, template.isDefaultIncluded())
          .set(LEASE_CLAUSE_TEMPLATES.OPTIONAL, template.isOptional())
          .set(LEASE_CLAUSE_TEMPLATES.SORT_ORDER, template.getSortOrder())
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_AT, now)
          .where(LEASE_CLAUSE_TEMPLATES.ID.eq(template.getId()))
          .execute();
    }
    return template;
  }

  public void softDeleteByIdentifier(Sid identifier) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(LEASE_CLAUSE_TEMPLATES)
        .set(LEASE_CLAUSE_TEMPLATES.DELETED_AT, now)
        .where(LEASE_CLAUSE_TEMPLATES.IDENTIFIER.eq(identifier))
        .execute();
  }
}
```

- [ ] **Step 3: Write `ContractLeaseClauseRepository`**

```java
package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACT_LEASE_CLAUSES;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.mapper.ContractLeaseClauseRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractLeaseClauseRepository {

  private final DSLContext dsl;
  private final ContractLeaseClauseRecordMapper mapper;
  private final Clock clock;

  public List<ContractLeaseClause> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTRACT_LEASE_CLAUSES)
            .where(
                CONTRACT_LEASE_CLAUSES
                    .CONTRACT_ID
                    .eq(contractId)
                    .and(CONTRACT_LEASE_CLAUSES.TEAM_ID.eq(teamId)))
            .fetch()
            .map(mapper::toDomain));
  }

  /** Replaces all per-contract overrides in one call — the PUT endpoint always sends the full set. */
  public void replaceForContract(
      UUID contractId, UUID teamId, UUID actorId, List<ContractLeaseClause> clauses) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.deleteFrom(CONTRACT_LEASE_CLAUSES)
        .where(
            CONTRACT_LEASE_CLAUSES.CONTRACT_ID.eq(contractId).and(CONTRACT_LEASE_CLAUSES.TEAM_ID.eq(teamId)))
        .execute();

    for (ContractLeaseClause clause : clauses) {
      dsl.insertInto(CONTRACT_LEASE_CLAUSES)
          .set(CONTRACT_LEASE_CLAUSES.ID, UUID.randomUUID())
          .set(CONTRACT_LEASE_CLAUSES.TEAM_ID, teamId)
          .set(CONTRACT_LEASE_CLAUSES.CONTRACT_ID, contractId)
          .set(CONTRACT_LEASE_CLAUSES.CLAUSE_TEMPLATE_ID, clause.getClauseTemplateId())
          .set(CONTRACT_LEASE_CLAUSES.INCLUDED, clause.isIncluded())
          .set(CONTRACT_LEASE_CLAUSES.SORT_ORDER, clause.getSortOrder())
          .set(CONTRACT_LEASE_CLAUSES.CREATED_AT, now)
          .set(CONTRACT_LEASE_CLAUSES.UPDATED_AT, now)
          .set(CONTRACT_LEASE_CLAUSES.CREATED_BY, actorId)
          .set(CONTRACT_LEASE_CLAUSES.UPDATED_BY, actorId)
          .execute();
    }
  }
}
```

Delete-then-reinsert (not upsert-per-row) matches this table's actual usage pattern: the PUT endpoint always sends the complete desired clause set for a contract, so replace-wholesale is simpler and avoids orphaned rows from a clause the landlord un-excluded back to default (no row at all, vs. a stale `included=true` row) — same reasoning `ContractRentComponentRepository.replaceForRentPeriod` already uses in this codebase (confirmed by reading it during planning).

- [ ] **Step 4: Add cleanup lines to `AbstractRepositoryIntegrationTest`**

Insert before the existing `contracts` cleanup line:

```java
    dsl.deleteFrom(DSL.table("contract_lease_clauses")).execute();
    dsl.deleteFrom(DSL.table("lease_clause_templates")).execute();
```

- [ ] **Step 5: Write `LeaseClauseTemplateRepositoryIntegrationTest`**

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.mapper.LeaseClauseTemplateRecordMapperImpl;

@DisplayName("LeaseClauseTemplateRepository")
class LeaseClauseTemplateRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private LeaseClauseTemplateRepository repository;

  private LeaseClauseTemplate newTemplate(String country, String key, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .countryCode(country)
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(true)
        .optional(false)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  @DisplayName("saves and finds templates by country, ordered by sort_order")
  void findsByCountryOrdered() {
    repository = new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);

    repository.save(newTemplate("NL", "rent", 2));
    repository.save(newTemplate("NL", "parties", 1));
    repository.save(newTemplate("DE", "parties", 1));

    var nlTemplates = repository.findByCountryCode("NL");
    assertThat(nlTemplates).hasSize(2);
    assertThat(nlTemplates).extracting(LeaseClauseTemplate::getClauseKey).containsExactly("parties", "rent");
  }

  @Test
  @DisplayName("soft-deleted templates are excluded from findByCountryCode")
  void excludesSoftDeleted() {
    repository = new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);

    LeaseClauseTemplate saved = repository.save(newTemplate("FR", "parties", 1));
    repository.softDeleteByIdentifier(saved.getIdentifier().orElseThrow());

    assertThat(repository.findByCountryCode("FR")).isEmpty();
  }
}
```

- [ ] **Step 6: Write `ContractLeaseClauseRepositoryIntegrationTest`**

```java
package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.mapper.ContractLeaseClauseRecordMapperImpl;
import com.buurman.mapper.LeaseClauseTemplateRecordMapperImpl;

@DisplayName("ContractLeaseClauseRepository")
class ContractLeaseClauseRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContractLeaseClauseRepository repository;
  private UUID teamAContractId;
  private UUID teamBContractId;
  private UUID templateId;

  @BeforeEach
  void setUp() {
    repository = new ContractLeaseClauseRepository(dsl, new ContractLeaseClauseRecordMapperImpl(), CLOCK);
    LeaseClauseTemplateRepository templateRepository =
        new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);

    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);

    LeaseClauseTemplate template =
        templateRepository.save(
            LeaseClauseTemplate.builder()
                .countryCode("NL")
                .clauseKey("house-rules")
                .titleI18nKey("lease.house-rules.title")
                .bodyI18nKey("lease.house-rules.body")
                .defaultIncluded(true)
                .optional(true)
                .sortOrder(5)
                .version(1)
                .build());
    templateId = template.getId();
  }

  @Test
  @DisplayName("replaceForContract is team-scoped — team B never sees or affects team A's overrides")
  void teamIsolation() {
    repository.replaceForContract(
        teamAContractId,
        TEAM_A_ID,
        USER_ID,
        List.of(
            ContractLeaseClause.builder()
                .clauseTemplateId(templateId)
                .included(false)
                .sortOrder(5)
                .build()));

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).hasSize(1);
    assertThat(repository.findByContractIdAndTeamId(teamBContractId, TEAM_B_ID)).isEmpty();
    // A team-B lookup using team A's contract id (wrong team) also finds nothing.
    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_B_ID)).isEmpty();
  }

  @Test
  @DisplayName("replaceForContract deletes the previous set before inserting the new one")
  void replaceWholesale() {
    repository.replaceForContract(
        teamAContractId,
        TEAM_A_ID,
        USER_ID,
        List.of(
            ContractLeaseClause.builder().clauseTemplateId(templateId).included(false).sortOrder(5).build()));
    repository.replaceForContract(teamAContractId, TEAM_A_ID, USER_ID, List.of());

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).isEmpty();
  }
}
```

- [ ] **Step 7: Run both integration tests**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest='LeaseClauseTemplateRepositoryIntegrationTest,ContractLeaseClauseRepositoryIntegrationTest'`
Expected: 4 tests pass. (Docker must be running.)

- [ ] **Step 8: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/mapper/LeaseClauseTemplateRecordMapper.java backend/buurman-core/src/main/java/com/buurman/mapper/ContractLeaseClauseRecordMapper.java backend/buurman-core/src/main/java/com/buurman/repository/LeaseClauseTemplateRepository.java backend/buurman-core/src/main/java/com/buurman/repository/ContractLeaseClauseRepository.java backend/buurman-core/src/test/java/com/buurman/repository/LeaseClauseTemplateRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/ContractLeaseClauseRepositoryIntegrationTest.java backend/buurman-core/src/test/java/com/buurman/repository/AbstractRepositoryIntegrationTest.java
git commit -m "feat(lease-agreement): add clause template and per-contract override repositories"
```

---

## Task 4: LeaseClauseResolver

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/LeaseClauseResolver.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/service/LeaseClauseResolverTest.java`

**Interfaces:**
- Consumes: `LeaseClauseTemplateRepository.findByCountryCode`, `ContractLeaseClauseRepository.findByContractIdAndTeamId` (Task 3), `Contract.getCountryCode(): Optional<String>` (existing).
- Produces: `LeaseClauseResolver.resolve(Contract, Locale): List<ResolvedLeaseClauseResponse>` — Task 6 (exporter) and Task 7 (GET endpoint) both call this; it's the single source of truth for "what clauses apply to this contract right now," so generation and the toggle UI never disagree.

- [ ] **Step 1: Write the failing test**

```java
package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;

class LeaseClauseResolverTest {

  private final LeaseClauseTemplateRepository templateRepository = mock(LeaseClauseTemplateRepository.class);
  private final ContractLeaseClauseRepository overrideRepository = mock(ContractLeaseClauseRepository.class);
  private final MessageSource messageSource = mock(MessageSource.class);

  private final LeaseClauseResolver resolver =
      new LeaseClauseResolver(templateRepository, overrideRepository, messageSource);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();

  private Contract contract(String countryCode) {
    return Contract.builder().id(CONTRACT_ID).teamId(TEAM_ID).countryCode(Optional.of(countryCode)).build();
  }

  private LeaseClauseTemplate template(String key, boolean defaultIncluded, boolean optional, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("LCT0000000000000000000000001")))
        .countryCode("NL")
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(defaultIncluded)
        .optional(optional)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  void defaultInclusionAppliesWhenNoOverrideExists() {
    LeaseClauseTemplate parties = template("parties", true, false, 1);
    when(templateRepository.findByCountryCode("NL")).thenReturn(List.of(parties));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH);

    assertThat(resolved).hasSize(1);
    assertThat(resolved.get(0).included()).isTrue();
  }

  @Test
  void overrideFlipsInclusion() {
    LeaseClauseTemplate houseRules = template("house-rules", true, true, 5);
    when(templateRepository.findByCountryCode("NL")).thenReturn(List.of(houseRules));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractLeaseClause.builder()
                    .clauseTemplateId(houseRules.getId())
                    .included(false)
                    .sortOrder(5)
                    .build()));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("resolved text");

    var resolved = resolver.resolve(contract("NL"), Locale.ENGLISH);

    assertThat(resolved.get(0).included()).isFalse();
  }

  @Test
  void countryWithNoTemplatesRejectsWithBusinessRuleException() {
    when(templateRepository.findByCountryCode("GB")).thenReturn(List.of());

    assertThatThrownBy(() -> resolver.resolve(contract("GB"), Locale.ENGLISH))
        .isInstanceOf(BusinessRuleException.class);
  }
}
```

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `mvn test -pl buurman-core -am -Dtest=LeaseClauseResolverTest`
Expected: compile error — `LeaseClauseResolver` doesn't exist.

- [ ] **Step 3: Write `LeaseClauseResolver`**

Uses an explicit constructor, not `@RequiredArgsConstructor` — Lombok does
not copy arbitrary annotations (including `@Qualifier`) from a field onto
the constructor parameter it generates unless the project's `lombok.config`
lists it under `lombok.copyableAnnotations`, which this codebase's existing
`@Qualifier`-needing classes don't rely on (confirmed:
`RentIncreaseLetterExporter` hand-writes its constructor for exactly this
reason). Match that established pattern, not `@RequiredArgsConstructor`.

```java
package com.buurman.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;

@Service
public class LeaseClauseResolver {

  private final LeaseClauseTemplateRepository templateRepository;
  private final ContractLeaseClauseRepository overrideRepository;
  private final MessageSource messageSource;

  public LeaseClauseResolver(
      LeaseClauseTemplateRepository templateRepository,
      ContractLeaseClauseRepository overrideRepository,
      @Qualifier("letterMessageSource") MessageSource messageSource) {
    this.templateRepository = templateRepository;
    this.overrideRepository = overrideRepository;
    this.messageSource = messageSource;
  }

  public List<ResolvedLeaseClauseResponse> resolve(Contract contract, Locale locale) {
    String countryCode =
        contract
            .getCountryCode()
            .orElseThrow(() -> new BusinessRuleException("Contract has no country code set"));

    List<LeaseClauseTemplate> templates = templateRepository.findByCountryCode(countryCode);
    if (templates.isEmpty()) {
      throw new BusinessRuleException(
          "No lease clause templates are configured for country " + countryCode);
    }

    Map<UUID, ContractLeaseClause> overridesByTemplateId =
        overrideRepository.findByContractIdAndTeamId(contract.getId(), contract.getTeamId()).stream()
            .collect(Collectors.toMap(ContractLeaseClause::getClauseTemplateId, o -> o));

    return templates.stream()
        .map(
            t -> {
              ContractLeaseClause override = overridesByTemplateId.get(t.getId());
              boolean included = override != null ? override.isIncluded() : t.isDefaultIncluded();
              int sortOrder = override != null ? override.getSortOrder() : t.getSortOrder();
              String title = messageSource.getMessage(t.getTitleI18nKey(), null, locale);
              String body = messageSource.getMessage(t.getBodyI18nKey(), null, locale);
              return new ResolvedLeaseClauseResponse(
                  t.getIdentifier().orElseThrow(), t.getClauseKey(), title, body, included, t.isOptional(), sortOrder);
            })
        .sorted((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()))
        .toList();
  }
}
```

`@Qualifier("letterMessageSource")` matches how every existing letter exporter resolves country/legal text (confirmed in `RentIncreaseLetterExporter`'s constructor) — the lease clause bundle lives in the same message-source bean, just a new `.properties` family within it (Task 8 adds the family; no new Spring bean/qualifier is created here).

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn test -pl buurman-core -am -Dtest=LeaseClauseResolverTest`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-core/src/main/java/com/buurman/service/LeaseClauseResolver.java backend/buurman-core/src/test/java/com/buurman/service/LeaseClauseResolverTest.java
git commit -m "feat(lease-agreement): add LeaseClauseResolver (default + per-contract override resolution)"
```

---

## Task 5: Backoffice clause template management

**Files:**
- Modify: `openapi/backoffice.yaml` (add `/backoffice/lease-clause-templates` paths + schemas, mirroring the existing `/backoffice/rent-regulations/*` shape in the same file)
- Create: `backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/BackofficeLeaseClauseTemplateService.java`
- Create: `backend/buurman-backoffice/src/main/java/com/buurman/controller/backoffice/BackofficeLeaseClauseTemplateController.java`
- Test: `backend/buurman-backoffice/src/test/java/com/buurman/service/backoffice/BackofficeLeaseClauseTemplateServiceTest.java`

**Interfaces:**
- Consumes: `LeaseClauseTemplateRepository` (Task 3).
- Produces: generated `BackofficeLeaseClauseTemplatesApi` interface (from the new OpenAPI paths) that `BackofficeLeaseClauseTemplateController` implements.

- [ ] **Step 1: Read the existing pattern before writing anything**

Read `backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/RentRegulationCatalogService.java`'s CRUD methods (not its diff/reload machinery — this spec explicitly excludes that, per its Out of scope) and `backend/buurman-backoffice/src/main/java/com/buurman/controller/backoffice/BackofficeRentRegulationController.java`, to confirm the exact `@PreAuthorize("hasRole('BACKOFFICE_ADMIN')")` placement (service layer) and `SecurityUtils.getBackofficePrincipal()` actor-id pattern (confirmed during planning via `BackofficeFeatureFlagController`) before writing this task's controller/service.

- [ ] **Step 2: Add OpenAPI paths**

In `openapi/backoffice.yaml`, add (following the file's existing style — read a neighboring `/backoffice/rent-regulations/*` path block first and match its `responses`/`security` block shape exactly):

```yaml
  /backoffice/lease-clause-templates:
    get:
      tags: [Lease Clause Templates]
      operationId: listLeaseClauseTemplates
      parameters:
        - name: countryCode
          in: query
          required: true
          schema:
            type: string
      responses:
        "200":
          description: Templates for the given country
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/LeaseClauseTemplateResponse'
      security:
        - bearer-jwt: []
    post:
      tags: [Lease Clause Templates]
      operationId: createLeaseClauseTemplate
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertLeaseClauseTemplateRequest'
      responses:
        "200":
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LeaseClauseTemplateResponse'
      security:
        - bearer-jwt: []
  /backoffice/lease-clause-templates/{identifier}:
    put:
      tags: [Lease Clause Templates]
      operationId: updateLeaseClauseTemplate
      parameters:
        - name: identifier
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertLeaseClauseTemplateRequest'
      responses:
        "200":
          description: Updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LeaseClauseTemplateResponse'
      security:
        - bearer-jwt: []
    delete:
      tags: [Lease Clause Templates]
      operationId: deleteLeaseClauseTemplate
      parameters:
        - name: identifier
          in: path
          required: true
          schema:
            type: string
      responses:
        "204":
          description: Deleted
      security:
        - bearer-jwt: []
```

Add `LeaseClauseTemplateResponse` and `UpsertLeaseClauseTemplateRequest` schemas under `components/schemas` (mirror the field lists from Task 2's Java DTOs exactly — `identifier`/`countryCode`/`clauseKey`/`titleI18nKey`/`bodyI18nKey`/`defaultIncluded`/`optional`/`sortOrder`/`version` for the response; the same minus `identifier`/`version` for the request).

- [ ] **Step 3: Build to generate `BackofficeLeaseClauseTemplatesApi`**

Run: `cd backend && mvn clean install -DskipTests -pl buurman-backoffice -am`
Expected: BUILD SUCCESS; confirm the generated interface exists under `backend/buurman-backoffice/target/generated-sources/` and read its actual method signatures before writing the controller (don't guess parameter order — same discipline this branch already used for the e-signature `SignaturesApi`).

- [ ] **Step 4: Write `BackofficeLeaseClauseTemplateService`**

```java
package com.buurman.service.backoffice;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.repository.LeaseClauseTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackofficeLeaseClauseTemplateService {

  private final LeaseClauseTemplateRepository repository;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public List<LeaseClauseTemplateResponse> list(String countryCode) {
    return repository.findByCountryCode(countryCode).stream().map(this::toResponse).toList();
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LeaseClauseTemplateResponse create(UpsertLeaseClauseTemplateRequest request, UUID actorId) {
    LeaseClauseTemplate saved =
        repository.save(
            LeaseClauseTemplate.builder()
                .countryCode(request.countryCode())
                .clauseKey(request.clauseKey())
                .titleI18nKey(request.titleI18nKey())
                .bodyI18nKey(request.bodyI18nKey())
                .defaultIncluded(request.defaultIncluded())
                .optional(request.optional())
                .sortOrder(request.sortOrder())
                .version(1)
                .build());
    return toResponse(saved);
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LeaseClauseTemplateResponse update(
      Sid identifier, UpsertLeaseClauseTemplateRequest request, UUID actorId) {
    LeaseClauseTemplate existing = repository.getByIdentifier(identifier);
    existing.setTitleI18nKey(request.titleI18nKey());
    existing.setBodyI18nKey(request.bodyI18nKey());
    existing.setDefaultIncluded(request.defaultIncluded());
    existing.setOptional(request.optional());
    existing.setSortOrder(request.sortOrder());
    return toResponse(repository.save(existing));
  }

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void delete(Sid identifier, UUID actorId) {
    repository.softDeleteByIdentifier(identifier);
  }

  private LeaseClauseTemplateResponse toResponse(LeaseClauseTemplate t) {
    return new LeaseClauseTemplateResponse(
        t.getIdentifier().orElseThrow(),
        t.getCountryCode(),
        t.getClauseKey(),
        t.getTitleI18nKey(),
        t.getBodyI18nKey(),
        t.isDefaultIncluded(),
        t.isOptional(),
        t.getSortOrder(),
        t.getVersion());
  }
}
```

- [ ] **Step 5: Write `BackofficeLeaseClauseTemplateController`**

Implement the generated `BackofficeLeaseClauseTemplatesApi` interface, following `BackofficeFeatureFlagController`'s exact shape (`SecurityUtils.getBackofficePrincipal().getKeycloakId()` for the actor id — match its `currentActorId()` private helper verbatim). Write this only after Step 3's real generated interface is in hand; match its actual method names/parameter order.

- [ ] **Step 6: Write `BackofficeLeaseClauseTemplateServiceTest`**

Unit test (Mockito, no Spring context — matches this branch's `SignatureServiceTest` convention): `create` persists and returns the right response shape; `update` on a nonexistent identifier throws `NotFoundException` (via the repository's `getByIdentifier`); `list` returns templates for the requested country only (mocked repository call assertion).

- [ ] **Step 7: Run tests, verify pass**

Run: `mvn test -pl buurman-backoffice -am -Dtest=BackofficeLeaseClauseTemplateServiceTest`

- [ ] **Step 8: Commit**

```bash
git add openapi/backoffice.yaml backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/BackofficeLeaseClauseTemplateService.java backend/buurman-backoffice/src/main/java/com/buurman/controller/backoffice/BackofficeLeaseClauseTemplateController.java backend/buurman-backoffice/src/test/java/com/buurman/service/backoffice/BackofficeLeaseClauseTemplateServiceTest.java
git commit -m "feat(lease-agreement): add backoffice lease clause template CRUD"
```

---

## Task 6: LeaseAgreementExporter + template

**Files:**
- Create: `backend/buurman-letters/src/main/resources/templates/documents/lease-agreement/generic.html`
- Create: `backend/buurman-letters/src/main/java/com/buurman/service/letters/LeaseAgreementExporter.java`
- Test: `backend/buurman-letters/src/test/java/com/buurman/service/letters/LeaseAgreementExporterTest.java`

**Interfaces:**
- Consumes: `LeaseClauseResolver.resolve(Contract, Locale)` (Task 4), `LetterExporterHelper` (existing — `headerVariables`, `addressee`, `premisesInfo`/`premisesAddress`), `ContractRentComponentRepository.findByContractIdAndTeamId` (existing), `LetterTemplateService.renderToPdf` (existing).
- Produces: `LeaseAgreementExporter.generate(ContractIdentifier, UUID teamId, String lang): byte[]` — Task 7's generate-and-persist service calls this.

- [ ] **Step 1: Write the template**

`templates/documents/lease-agreement/generic.html` — follow `extension-addendum/generic.html`'s structure (the `_letter-styles` fragment include, header block using `generatedDate`/`contractIdentifier`, addressee block using `primaryContactName`/`contactAddress`, premises block using `propertyAddress`) but replace its body with a clause loop:

```html
<!DOCTYPE html>
<html lang="${#locale.language}" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8" />
  <th:block th:replace="~{_letter-styles :: letter-css}" />
  <title>Lease Agreement</title>
</head>
<body>
  <div class="letter">
    <div class="header" th:text="${generatedDate}"></div>
    <h1 th:text="#{lease.title}">Lease Agreement</h1>
    <p th:text="${contractIdentifier}"></p>

    <div class="addressee" th:if="${primaryContactName}">
      <p th:text="${primaryContactName}"></p>
      <p th:if="${contactAddress}" th:text="${contactAddress.street}"></p>
    </div>

    <p class="premises" th:text="${propertyAddress}"></p>

    <table class="rent-components" th:if="${rentComponents}">
      <tr th:each="component : ${rentComponents}">
        <td th:text="${component.type}"></td>
        <td th:text="${component.amount}"></td>
      </tr>
    </table>

    <section th:each="clause : ${clauses}" class="clause">
      <h2 th:text="${clause.title}"></h2>
      <p th:text="${clause.body}"></p>
    </section>
  </div>
</body>
</html>
```

(`clauses` is only the **included** subset by the time it reaches the template — filtering happens in the exporter, per Step 2, so the template never needs an `th:if="${clause.included}"` check; a template that has to know about exclusion logic is a template doing the resolver's job twice.)

- [ ] **Step 2: Write `LeaseAgreementExporter`**

```java
package com.buurman.service.letters;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.util.CurrencyUtils;

@Component
public class LeaseAgreementExporter {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRentComponentRepository rentComponentRepository;
  private final LeaseClauseResolver clauseResolver;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public LeaseAgreementExporter(
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      ContractRentComponentRepository rentComponentRepository,
      LeaseClauseResolver clauseResolver,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.rentComponentRepository = rentComponentRepository;
    this.clauseResolver = clauseResolver;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(ContractIdentifier contractIdentifier, UUID teamId, String lang) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    Locale locale = LetterTemplateService.resolveLocale(lang);

    List<ResolvedLeaseClauseResponse> allClauses = clauseResolver.resolve(contract, locale);
    List<Map<String, String>> includedClauses =
        allClauses.stream()
            .filter(ResolvedLeaseClauseResponse::included)
            .map(c -> Map.of("title", c.title(), "body", c.body()))
            .toList();
    if (includedClauses.isEmpty()) {
      throw new BusinessRuleException(
          "No clauses are included for this contract — at least the required clauses must remain");
    }

    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    Map<String, Object> vars =
        new java.util.HashMap<>(LetterExporterHelper.headerVariables(contract, LocalDate.now(clock), dateFmt));
    vars.putAll(helper.addressee(contract.getId(), teamId).variables());

    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    vars.put("propertyAddress", LetterExporterHelper.premisesAddress(property, premisesInfo));
    vars.put("clauses", includedClauses);

    List<ContractRentComponent> components =
        rentComponentRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    vars.put(
        "rentComponents",
        components.stream()
            .map(
                c ->
                    Map.of(
                        "type", c.getComponentType().name(),
                        "amount", CurrencyUtils.formatCurrency(c.getAmount().value(), c.getAmount().currency())))
            .toList());

    return documentTemplateService.renderToPdf("lease-agreement", locale, vars);
  }
}
```

- [ ] **Step 3: Write `LeaseAgreementExporterTest`**

Mockito unit test (matches `SignatureServiceTest`'s style — no Spring context): renders successfully for a contract with clauses resolved (mock `LeaseClauseResolver` to return a mix of included/excluded, assert only included clause titles/bodies reach the `renderToPdf` call's variable map — capture the `Map<String,Object>` argument via an `ArgumentCaptor` and assert its `clauses` entry's size and content); a second call with a different mocked resolver result (simulating a changed clause selection) produces a different `clauses` list in the captured variables, proving regeneration isn't cached (Review Focus item); `includedClauses.isEmpty()` throws `BusinessRuleException` before calling `renderToPdf` at all.

- [ ] **Step 4: Run it, verify pass**

Run: `mvn clean install -DskipTests -pl buurman-letters -am && mvn test -pl buurman-letters -am -Dtest=LeaseAgreementExporterTest`

- [ ] **Step 5: Commit**

```bash
git add backend/buurman-letters/src/main/resources/templates/documents/lease-agreement/generic.html backend/buurman-letters/src/main/java/com/buurman/service/letters/LeaseAgreementExporter.java backend/buurman-letters/src/test/java/com/buurman/service/letters/LeaseAgreementExporterTest.java
git commit -m "feat(lease-agreement): add LeaseAgreementExporter and generic template"
```

---

## Task 7: Contract-facing API (clause toggle + generate-and-persist)

**Files:**
- Modify: `openapi/src/paths/contracts.yaml` (add `lease-clauses` GET/PUT anchors and a `lease-agreement` POST anchor, mirroring `signature-requests`'s existing anchor style on this same file from the e-signature work)
- Modify: `openapi/src/app.yaml` (register the three new paths + `ResolvedLeaseClauseResponse`/`UpdateContractLeaseClausesRequest` schemas)
- Create: `backend/buurman-core/src/main/java/com/buurman/service/LeaseClauseService.java`
- Create: `backend/buurman-letters/src/main/java/com/buurman/service/letters/LeaseAgreementGenerationService.java` (generate-and-persist wrapper, same shape as `RentChangeDocumentGenerationService`)
- Modify: `backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java` (add the two `lease-clauses` methods)
- Modify: `backend/buurman-letters/src/main/java/com/buurman/controller/LetterController.java` (add the `lease-agreement` generate method)
- Test: `backend/buurman-core/src/test/java/com/buurman/service/LeaseClauseServiceTest.java`

**Interfaces:**
- Consumes: `LeaseClauseResolver` (Task 4), `ContractLeaseClauseRepository.replaceForContract` (Task 3), `LeaseAgreementExporter.generate` (Task 6), `DocumentRepository`/`S3StorageService` (existing — same generate-and-persist pattern as `RentChangeDocumentGenerationService`).
- Produces: `GET/PUT /contracts/{identifier}/lease-clauses`, `POST /contracts/{identifier}/lease-agreement`.

- [ ] **Step 1: Add OpenAPI paths**

Follow `signature-requests`'s anchor pattern in `openapi/src/paths/contracts.yaml` exactly (own anchor per HTTP verb combination, `tags: [Lease Agreement]`, full `responses` block with 400/401/403/404/409/500 `ProblemDetail` refs, `security: [bearer-jwt: []]`):

```yaml
lease-clauses:
  get:
    operationId: getLeaseClauses
    parameters:
      - name: contractIdentifier
        in: path
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
    responses:
      "200":
        description: Resolved clause list with inclusion state
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/ResolvedLeaseClauseResponse'
      # ... 400/401/403/404/500 ProblemDetail, matching signature-requests' existing block
  put:
    operationId: updateLeaseClauses
    parameters:
      - name: contractIdentifier
        in: path
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/UpdateContractLeaseClausesRequest'
    responses:
      "200":
        description: Updated clause list
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/ResolvedLeaseClauseResponse'
      # ... same ProblemDetail block, plus 400 doubling as "tried to exclude a non-optional clause"

lease-agreement:
  post:
    operationId: generateLeaseAgreement
    parameters:
      - name: contractIdentifier
        in: path
        required: true
        schema:
          $ref: '#/components/schemas/ContractIdentifier'
    responses:
      "200":
        description: Generated document
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DocumentResponse'
      # ... same ProblemDetail block
```

Register both under `/contracts/{contractIdentifier}/lease-clauses` and `/contracts/{contractIdentifier}/lease-agreement` in `openapi/src/app.yaml`'s `paths:` map, and add `ResolvedLeaseClauseResponse`/`UpdateContractLeaseClausesRequest` schemas (mirror Task 2's Java DTOs field-for-field, same discipline as every prior OpenAPI task on this branch).

- [ ] **Step 2: Bundle and rebuild**

Run: `make bundle-openapi && cd backend && mvn clean install -DskipTests -pl buurman-core,buurman-letters -am`
Expected: BUILD SUCCESS; confirm the generated `ContractsApi`/`LettersApi` interfaces now declare the new methods — read their actual signatures before Step 4/6.

- [ ] **Step 3: Write `LeaseClauseService`**

```java
package com.buurman.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.document.DocumentLocale;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaseClauseService {

  private final ContractRepository contractRepository;
  private final LeaseClauseTemplateRepository templateRepository;
  private final ContractLeaseClauseRepository overrideRepository;
  private final LeaseClauseResolver resolver;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<ResolvedLeaseClauseResponse> getClauses(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    return resolver.resolve(contract, DocumentLocale.resolveOrEnglish(contract.getDocumentLanguages()));
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ResolvedLeaseClauseResponse> updateClauses(
      ContractIdentifier contractIdentifier,
      UpdateContractLeaseClausesRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    String countryCode =
        contract.getCountryCode().orElseThrow(() -> new BadRequestException("Contract has no country code"));

    List<LeaseClauseTemplate> templates = templateRepository.findByCountryCode(countryCode);

    List<ContractLeaseClause> toSave =
        request.clauses().stream()
            .map(
                selection -> {
                  LeaseClauseTemplate template =
                      templates.stream()
                          .filter(
                              t ->
                                  t.getIdentifier()
                                      .map(id -> id.value().equals(selection.templateIdentifier()))
                                      .orElse(false))
                          .findFirst()
                          .orElseThrow(
                              () -> new BadRequestException(
                                  "Unknown clause template: " + selection.templateIdentifier()));
                  // Server-side enforcement — a non-optional clause cannot be excluded,
                  // regardless of what the client sends. This is the Review Focus item:
                  // a UI-only checkbox-disable is not sufficient for a legal document.
                  if (!template.isOptional() && !selection.included()) {
                    throw new BadRequestException(
                        "Clause '" + template.getClauseKey() + "' is required and cannot be excluded");
                  }
                  return ContractLeaseClause.builder()
                      .clauseTemplateId(template.getId())
                      .included(selection.included())
                      .sortOrder(selection.sortOrder())
                      .build();
                })
            .toList();

    overrideRepository.replaceForContract(contract.getId(), teamId, principal.getUserId(), toSave);
    return resolver.resolve(contract, DocumentLocale.resolveOrEnglish(contract.getDocumentLanguages()));
  }
}
```

(Confirm `DocumentLocale.resolveOrEnglish(List<String>)`'s exact signature against the real file before finalizing — it was referenced during spec research as the lenient-fallback locale resolver; if its actual parameter type differs, e.g. it takes the contract's `documentLanguages` field directly rather than a plain `List<String>`, adjust the two call sites above to match.)

- [ ] **Step 4: Wire `LeaseClauseService` into `ContractController`**

Add the two new methods (`getLeaseClauses`, `updateLeaseClauses`) delegating to `LeaseClauseService`, following `ContractController`'s existing thin-delegation style (`SecurityUtils.getCurrentPrincipal()` then one delegating call — same as every other method already in that file).

- [ ] **Step 5: Write `LeaseAgreementGenerationService`**

Follow `RentChangeDocumentGenerationService.generateAndPersist`'s exact shape (already read in full during planning): `@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")`, calls `LeaseAgreementExporter.generate(...)`, uploads via `S3StorageService.uploadFile(pdf, MediaType.APPLICATION_PDF_VALUE, teamSid, "CONTRACT", contractSid, filename)`, persists a `Document` row, returns `DocumentResponse` with a presigned download URL — this task does not introduce a new pattern, it's a direct copy of an existing one with `LeaseAgreementExporter` in place of `RentChangeDocumentExporter`.

- [ ] **Step 6: Wire into `LetterController`**

Add `generateLeaseAgreement` delegating to `LeaseAgreementGenerationService`, same one-line-delegation style as `generateRentChangeDocuments`.

- [ ] **Step 7: Write `LeaseClauseServiceTest`**

Mockito unit test: `updateClauses` with a non-optional clause's `included=false` throws `BadRequestException` before calling `overrideRepository.replaceForContract` at all (`verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any())` — this is the Review Focus item, and the assertion must prove the rejection happens before any write, not just that it eventually throws); an unknown `templateIdentifier` in the request throws `BadRequestException`; a valid all-optional-clauses-unchanged request succeeds and calls `replaceForContract` with the expected list.

- [ ] **Step 8: Run tests**

Run: `mvn clean install -DskipTests -pl buurman-core,buurman-letters -am && mvn test -pl buurman-core -am -Dtest=LeaseClauseServiceTest`

- [ ] **Step 9: Commit**

```bash
git add openapi/src/paths/contracts.yaml openapi/src/app.yaml openapi/app.yaml backend/buurman-core/src/main/java/com/buurman/service/LeaseClauseService.java backend/buurman-core/src/main/java/com/buurman/controller/ContractController.java backend/buurman-letters/src/main/java/com/buurman/service/letters/LeaseAgreementGenerationService.java backend/buurman-letters/src/main/java/com/buurman/controller/LetterController.java backend/buurman-core/src/test/java/com/buurman/service/LeaseClauseServiceTest.java
git commit -m "feat(lease-agreement): add clause toggle API and generate-and-persist endpoint"
```

---

## Task 8: Seed data + i18n bundle

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V080__seed_lease_clause_templates.sql` (version per Task 1's Global Constraints re-derivation)
- Create: `backend/buurman-letters/src/main/resources/messages/document-lease-agreement.properties` + 12 language variants (`_nl`, `_de`, `_fr`, `_pt`, `_es`, `_sv`, `_it`, `_fi`, `_el`, `_pl`, `_da`, `_nb`)
- Modify: `backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java` (add `"document-lease-agreement"` to the pinned `containsExactlyInAnyOrder(...)` family list)

**Interfaces:**
- Produces: seed rows in `lease_clause_templates` for NL/DE/FR/ES/PT/BE/GB, consumed by `LeaseClauseResolver` (Task 4).

- [ ] **Step 1: Write the seed migration**

7 countries × 7 clause keys each (`parties`, `premises`, `rent`, `duration`, `deposit`, `maintenance`, `termination-reference`), `optional = FALSE` for `parties`/`premises`/`rent`/`duration`, `TRUE` for `deposit`/`maintenance`/`termination-reference`. Every `INSERT` row's `clause_key`/`title_i18n_key`/`body_i18n_key` follows `lease.{clauseKey}.title`/`lease.{clauseKey}.body` — **country-agnostic keys**, since the placeholder body text doesn't actually vary by country in this seed (a real per-country body would need per-country keys like the per-country-addenda spec's `legal.DE.section558.body`; this seed is structural scaffolding, not real jurisdictional content, so one shared placeholder body per clause key across all 7 countries is honest about what it is — seed data proving the mechanism, not implying 7 different vetted texts exist).

```sql
-- Seed data is placeholder/example content, NOT vetted legal text for any jurisdiction.
-- See docs/superpowers/specs/2026-09-30-lease-agreement-generation-design.md,
-- "Legal content — explicit scope and disclaimer".
INSERT INTO lease_clause_templates
    (id, identifier, country_code, clause_key, title_i18n_key, body_i18n_key, default_included, optional, sort_order, version)
VALUES
    (gen_random_uuid(), 'LCT00000000000000000000NL1', 'NL', 'parties', 'lease.parties.title', 'lease.parties.body', TRUE, FALSE, 1, 1),
    (gen_random_uuid(), 'LCT00000000000000000000NL2', 'NL', 'premises', 'lease.premises.title', 'lease.premises.body', TRUE, FALSE, 2, 1),
    -- ... rent (3), duration (4), deposit (5, optional), maintenance (6, optional),
    -- termination-reference (7, optional) for NL, then the same 7 clause_keys repeated
    -- for DE, FR, ES, PT, BE, GB with country-specific identifier suffixes.
    (gen_random_uuid(), 'LCT00000000000000000000GB7', 'GB', 'termination-reference', 'lease.termination-reference.title', 'lease.termination-reference.body', TRUE, TRUE, 7, 1);
```

Write out all 49 rows explicitly in the actual migration file — the `-- ...` above is this plan's shorthand for "same pattern repeated," not something to leave abbreviated in the real SQL. Hand-written `identifier` values (not `gen_random_uuid()`-style generation) are fine here since this is fixed seed data with no runtime `SidGenerator` call — confirm the literal strings are valid Sid shape (3-letter prefix + 26 chars) matching the `LCT` prefix from Task 2; adjust the padding scheme above to produce exactly 26 characters after `LCT`, not the illustrative shorthand shown.

- [ ] **Step 2: Write the base `.properties` bundle**

`document-lease-agreement.properties`:

```properties
# Placeholder/example clause copy — NOT vetted legal text. See the design spec's
# "Legal content — explicit scope and disclaimer" section. Do not present this
# text as legally reviewed in any user-facing copy.
lease.title=Lease Agreement
lease.parties.title=Parties
lease.parties.body=This agreement is between the landlord and tenant named above.
lease.premises.title=Premises
lease.premises.body=The landlord lets to the tenant the premises described above.
lease.rent.title=Rent
lease.rent.body=The tenant shall pay the rent set out in this agreement.
lease.duration.title=Duration
lease.duration.body=This agreement runs for the term set out in this agreement.
lease.deposit.title=Deposit
lease.deposit.body=A security deposit may be required as set out in this agreement.
lease.maintenance.title=Maintenance
lease.maintenance.body=Each party's maintenance obligations are as set out in this agreement.
lease.termination-reference.title=Termination
lease.termination-reference.body=This agreement may be terminated as provided by applicable law and this agreement's termination terms.
```

- [ ] **Step 3: Write all 12 translated variants**

Create `document-lease-agreement_nl.properties`, `_de`, `_fr`, `_pt`, `_es`, `_sv`, `_it`, `_fi`, `_el`, `_pl`, `_da`, `_nb` — each with **exactly the same key set** as the base file (the parity test checks this exactly, per Global Constraints) and placeholder-quality text per language (does not need to be a professional translation — the parity test checks key/placeholder parity, not translation quality; mark each file's header comment the same way as the base file).

- [ ] **Step 4: Update the pinned family list**

In `I18nBundleParityTest.java`, add `"document-lease-agreement"` to the `containsExactlyInAnyOrder(...)` list in `families()` (insert alphabetically among the existing `document-*` entries, matching the list's existing sort order).

- [ ] **Step 5: Run the parity test — this is the load-bearing check for this task**

Run: `mvn clean install -DskipTests -pl buurman-app -am && mvn test -pl buurman-app -am -Dtest=I18nBundleParityTest`
Expected: all parameterized cases pass, including the new `document-lease-agreement` family's `carriesEveryLanguage` and `everyLanguageCarriesEveryKey` cases. If this fails on a missing language file or a key mismatch, fix the bundle files — do not modify the test's assertions to work around a real gap.

- [ ] **Step 6: Run the migration + resolver test**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am && mvn clean install -DskipTests -pl buurman-jooq -am`
Run a quick manual sanity check (or extend `LeaseClauseResolverTest` with an integration-style case) confirming `templateRepository.findByCountryCode("NL")` against the real seeded data returns 7 rows once this migration is applied — via a new assertion in `LeaseClauseTemplateRepositoryIntegrationTest` (Task 3) rather than a new test file, since it's the same test class's natural home: add a test there now that seed data exists, asserting the real migration seeded exactly 7 NL rows with the expected `optional` flags.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V080__seed_lease_clause_templates.sql backend/buurman-letters/src/main/resources/messages/document-lease-agreement*.properties backend/buurman-app/src/test/java/com/buurman/I18nBundleParityTest.java backend/buurman-core/src/test/java/com/buurman/repository/LeaseClauseTemplateRepositoryIntegrationTest.java
git commit -m "feat(lease-agreement): seed placeholder clause templates for 7 countries"
```

---

## Task 9: Frontend

**Files:**
- Modify: `frontend/app/src/constants/featureFlags.ts` (none needed — this feature isn't flag-gated per the spec; skip if no flag is introduced, confirm against the spec before adding one gratuitously)
- Create: `frontend/app/src/hooks/useLeaseAgreementHooks.ts`
- Create: `frontend/app/src/components/contracts/ContractLeaseAgreementTab.tsx`
- Modify: `frontend/app/src/pages/ContractDetailPage.tsx` (add the new tab to the existing tabs array, alongside `ContractExtensionsTab`/`ContractDocumentsTab`)
- Create: `frontend/backoffice/src/pages/LeaseClauseTemplatesPage.tsx`
- Modify: `frontend/backoffice/src/App.tsx` or router config (add the new backoffice route — check the real router file structure first)
- Test: `frontend/app/src/components/contracts/__tests__/ContractLeaseAgreementTab.test.tsx`

**Interfaces:**
- Consumes: generated `getLeaseClauses`/`updateLeaseClauses`/`generateLeaseAgreement` (from Task 7's OpenAPI bundle), generated backoffice `listLeaseClauseTemplates`/`createLeaseClauseTemplate`/`updateLeaseClauseTemplate`/`deleteLeaseClauseTemplate` (from Task 5).

- [ ] **Step 1: Regenerate both API clients**

Run: `cd frontend && yarn generate:api`
Expected: `frontend/app/src/generated/api/contracts/contracts.ts` gains the three new functions; `frontend/backoffice/src/generated/api/` gains the backoffice CRUD functions (confirm the exact generated module path by checking how `backoffice-rent-regulations` is organized, since backoffice codegen may follow a different Orval config than the app's).

- [ ] **Step 2: Write `useLeaseAgreementHooks.ts`**

Three hooks: `useLeaseClauses(contractId)` (query), `useUpdateLeaseClauses(contractId)` (mutation, invalidates the clauses query key on success), `useGenerateLeaseAgreement(contractId)` (mutation, invalidates `queryKeys.contracts.documents(contractId)` on success — same invalidation target the e-signature work's `useCreateSignatureRequest` already uses, so a newly generated lease agreement shows up in the Documents tab without a manual refresh).

- [ ] **Step 3: Write `ContractLeaseAgreementTab`**

Checkbox list of resolved clauses (from `useLeaseClauses`) — non-optional clauses rendered as checked+disabled, optional ones toggleable, a "Save selection" button calling `useUpdateLeaseClauses`, and a "Generate lease agreement" button calling `useGenerateLeaseAgreement`. On generation success, show a toast (matching `useMutationWithToast`'s existing convention) and let the landlord know the document is in the Documents tab.

- [ ] **Step 4: Wire the new tab into `ContractDetailPage`**

Add to the existing tabs array (read the file's real current tab-registration shape first — it was described during planning as a `useTabState('overview', [...])` array — match its exact entry shape).

- [ ] **Step 5: Write `LeaseClauseTemplatesPage` (backoffice)**

Country-filtered table (dropdown selecting one of the 7 seeded countries) + inline edit form for `titleI18nKey`/`bodyI18nKey`/`sortOrder`/`optional`/`defaultIncluded`. **A persistent banner at the top of the page** (not dismissible, not a one-time toast) stating: "Clause bodies reference placeholder i18n keys with example legal-boilerplate text. This is not vetted legal content — review with qualified counsel before relying on generated leases in production." This banner is load-bearing per the spec's Legal content disclaimer — do not ship this page without it.

- [ ] **Step 6: Write the frontend test**

`ContractLeaseAgreementTab.test.tsx` — renders the resolved clause list with correct checked state (mocked API response); a non-optional clause's checkbox is disabled; clicking "Generate lease agreement" calls the generate mutation and shows a success toast (mirrors the e-signature `SignatureRequestPanel.test.tsx`'s established pattern on this branch — use whatever shared test-wrapper helper that test settled on, `renderWithProviders`).

- [ ] **Step 7: Run tests + lint**

Run: `cd frontend && yarn test && yarn lint`

- [ ] **Step 8: Commit**

```bash
git add frontend/app/src/hooks/useLeaseAgreementHooks.ts frontend/app/src/components/contracts/ContractLeaseAgreementTab.tsx frontend/app/src/pages/ContractDetailPage.tsx frontend/backoffice/src/pages/LeaseClauseTemplatesPage.tsx frontend/app/src/components/contracts/__tests__/ContractLeaseAgreementTab.test.tsx
git commit -m "feat(lease-agreement): add clause toggle UI and backoffice template management page"
```

---

## Final verification (whole-branch)

- [ ] Run: `make test` — full backend suite, install-then-test. Expected: all pass, including `I18nBundleParityTest`'s new family.
- [ ] Run: `cd frontend && yarn test && yarn lint`
- [ ] Run: `make bundle-openapi && git diff --exit-code openapi/app.yaml` — no diff.
- [ ] Manual walk: generate an NL lease agreement, confirm the required clauses render, toggle off an optional clause, regenerate, confirm the PDF changed; confirm the backoffice disclaimer banner is visible on `LeaseClauseTemplatesPage`.
