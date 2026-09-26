# Tenancy-rules Reference Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give `CatalogCountry` a display-only, region-scoped `tenancyRules[]` list so tenancy facts that are not rent-increase caps (notice periods, tenancy duration, deposits, lease formalities, registration duties, fixed-amount fees) stop being discarded on every catalog audit.

**Architecture:** A new `CatalogTenancyRule` record on `CatalogCountry`, persisted in a new `rent_regulation_tenancy_rules` table that mirrors `rent_regulation_regions` conventions (global reference data: no `team_id`, no `deleted_at`, destructive wipe-and-reseed). It flows JSON → loader → repository → backoffice reload/diff/export → DTO → OpenAPI → the regulations page. Nothing computes off it.

**Tech Stack:** Java 25, Spring Boot 4, JOOQ 3.20, Flyway, PostgreSQL 18, Lombok, JUnit 5 + AssertJ + Testcontainers; React 19 + TypeScript + Vitest; OpenAPI 3.0.1 + Orval.

**Spec:** `docs/superpowers/specs/2026-09-26-tenancy-rules-reference-design.md`

## Deviation from the spec

The spec listed `TERMINATION_GROUNDS` among the topics and flagged it as open question 1
("may be too broad to curate honestly"). **This plan drops it.** Termination grounds are
long prose lists that differ per jurisdiction and would be unmaintainable in a single
`value` string; the facts the audit actually found fit the remaining seven topics, and
`OTHER` absorbs anything unforeseen. Adding it later is a one-line enum change plus the
matching OpenAPI constant. Raise this at review if you disagree.

## Global Constraints

- Migration version is **V068** — `V067__reminder_delivery_and_credit_source.sql` is the current head. (`CLAUDE.md` says V066; it is stale. Do not renumber existing migrations.)
- **Never modify existing migrations.** Add V068 only.
- All `if`/`else`/`for`/`while` bodies MUST use curly braces — no brace-less single-statement bodies, ever.
- Use idiomatic `Optional` (`map`, `orElse`, `orElseThrow`, `ifPresent`, `flatMap`) — never `if (opt != null)` or `opt.get()` without an `isPresent()` check.
- Google Java Style (backend), Airbnb (frontend). Conventional commits: `feat:`, `fix:`, `docs:`, `chore:`.
- Reference tables carry **no `team_id`** and **no `deleted_at`** — this is global reference data, matching `rent_regulation_regions`. This is a deliberate departure from the "Adding a New Entity" checklist in `CLAUDE.md`.
- A new `TenancyRuleTopic` constant MUST also be added to `openapi/src/app.yaml` and re-bundled with `make bundle-openapi`, or the generated TS drifts. This exact gap once shipped a broken `CEILING_RENT`.
- Response DTOs expose `identifier` (Sid) only, never internal UUIDs.
- After editing any `openapi/src/` file run `make bundle-openapi`; after that run `cd frontend && yarn generate:api`.
- Regenerate JOOQ after the migration: `cd backend && mvn generate-sources -pl buurman-jooq -am`.
- Backend tests need Docker running (Testcontainers).

## Review Focus

1. **A `regionCode` naming an undeclared region must fail loudly at reload, not silently drop the rule** — `insertRule` already throws `IllegalStateException` for this; the tenancy-rule path must match. Test in Task 4.
2. **`exportCatalog()` silently dropping `tenancyRules`** — the classic failure when a new structure is added to the catalog; a seed → export round-trip must return them. Test in Task 4.
3. **`deleteAllReferenceData()` FK ordering** — the new table references both `rent_regulation_countries` and `rent_regulation_regions`, so it must be deleted *before* `REGIONS`; otherwise reload dies on a constraint violation the first time a region-scoped tenancy rule exists. Test in Task 3.
4. **An unknown `topic` string in the JSON** must fail the catalog parse, matching the `MaxIncreaseType` guard, rather than loading as null. Test in Task 1.
5. **A country with absent or empty `tenancyRules`** must reload, export and render normally — the page section is simply hidden. The bundled catalog ships with most countries empty, so this is the common case, not an edge case. Tests in Task 1 and Task 6.

---

### Task 1: Catalog domain — topic enum, rule record, parse guards

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/TenancyRuleTopic.java`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/regulation/CatalogTenancyRule.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/domain/regulation/CatalogCountry.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java`
- Test: `backend/buurman-backoffice/src/test/java/com/buurman/service/backoffice/RentRegulationCatalogTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `TenancyRuleTopic` enum; `CatalogTenancyRule(TenancyRuleTopic topic, String regionCode, String label, String value, String effectiveFrom, String legalBasis, String sourceUrl, String notes)`; `CatalogCountry.tenancyRules()` returning `List<CatalogTenancyRule>` (may be null); `EntityPrefix.RRT`.

- [ ] **Step 1: Write the failing tests**

Add to `RentRegulationCatalogTest`:

```java
  @Test
  @DisplayName("every tenancy rule carries a topic, label and value, and resolves its region")
  void tenancyRules_areWellFormed() {
    RentRegulationCatalog catalog = loader.load();

    for (CatalogCountry country : catalog.countries()) {
      Set<String> regionCodes = new HashSet<>();
      for (CatalogRegion region : safe(country.regions())) {
        regionCodes.add(region.regionCode());
      }
      for (CatalogTenancyRule rule : safe(country.tenancyRules())) {
        assertThat(rule.topic()).as("topic in %s", country.countryCode()).isNotNull();
        assertThat(rule.label()).as("label in %s", country.countryCode()).isNotBlank();
        assertThat(rule.value()).as("value in %s", country.countryCode()).isNotBlank();
        if (rule.regionCode() != null) {
          assertThat(regionCodes)
              .as(
                  "tenancy rule in %s references region '%s' that is not declared",
                  country.countryCode(), rule.regionCode())
              .contains(rule.regionCode());
        }
        if (rule.effectiveFrom() != null) {
          assertThat(rule.effectiveFrom())
              .as("effectiveFrom in %s", country.countryCode())
              .matches("\\d{4}-\\d{2}-\\d{2}");
        }
      }
    }
  }

  @Test
  @DisplayName("a country with no tenancy rules loads cleanly")
  void tenancyRules_areOptional() {
    RentRegulationCatalog catalog = loader.load();

    assertThat(catalog.countries())
        .as("the bundled catalog ships most countries without tenancy rules")
        .anySatisfy(c -> assertThat(safe(c.tenancyRules())).isEmpty());
  }

  @Test
  @DisplayName("an unknown tenancy-rule topic fails the parse")
  void unknownTopic_failsParse() {
    String json =
        """
        {"version":"t","generatedAt":"2026-01-01","countries":[
          {"countryCode":"XX","countryName":"X","hasRegionalRegulations":false,
           "tenancyRules":[{"topic":"NOT_A_TOPIC","label":"l","value":"v"}]}]}
        """;

    assertThatThrownBy(() -> new ObjectMapper().readValue(json, RentRegulationCatalog.class))
        .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
  }
```

Add the imports `com.buurman.domain.regulation.CatalogTenancyRule` and `static org.assertj.core.api.Assertions.assertThatThrownBy`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl buurman-backoffice -am -Dtest=RentRegulationCatalogTest -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: FAIL — `CatalogTenancyRule` and `CatalogCountry.tenancyRules()` do not exist (compilation error).

**Note:** always pass `-Dmaven.build.cache.enabled=false` when checking a test result in this repo. The local build cache will otherwise replay a previous run's surefire report and show green against code you just changed.

- [ ] **Step 3: Write the implementation**

`TenancyRuleTopic.java`:

```java
package com.buurman.domain;

/**
 * Topic of a display-only tenancy-law reference entry on a catalogue country.
 *
 * <p>New values added here MUST also be added to {@code openapi/src/app.yaml}'s {@code
 * TenancyRuleTopic} schema (and re-bundled with {@code make bundle-openapi}) so the frontend's
 * generated TS stays in sync.
 */
public enum TenancyRuleTopic {
  NOTICE_PERIOD,
  TENANCY_DURATION,
  DEPOSIT,
  LEASE_FORM,
  REGISTRATION,
  FEES_AND_PENALTIES,
  OTHER
}
```

`CatalogTenancyRule.java`:

```java
package com.buurman.domain.regulation;

import com.buurman.domain.TenancyRuleTopic;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A display-only tenancy-law reference entry within a {@link CatalogCountry}.
 *
 * <p>These are facts that are not rent-increase caps — notice periods, tenancy duration, deposits,
 * lease formalities, registration duties, fixed-amount fees. Nothing computes off them; they are
 * rendered as reference material.
 *
 * <p>{@code regionCode} links the entry to a {@link CatalogRegion} of the same country; {@code null}
 * denotes a national rule. Only the CURRENT rule is stored — a superseded entry is replaced, not
 * retained, so {@code effectiveFrom} dates the present fact rather than opening a history.
 *
 * <p>{@code value} is free text ("5 years", "DKK 344", "2 months' rent", "Mandatory, written")
 * because the facts are heterogeneous and no unit system fits them; a topic that later needs to
 * drive behaviour is promoted to a typed column instead, as {@code formalNoticeDays} is.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogTenancyRule(
    TenancyRuleTopic topic,
    String regionCode,
    String label,
    String value,
    String effectiveFrom,
    String legalBasis,
    String sourceUrl,
    String notes) {}
```

In `CatalogCountry.java`, add `List<CatalogTenancyRule> tenancyRules` as the final component, after `formalNoticeDays`:

```java
public record CatalogCountry(
    String countryCode,
    String countryName,
    boolean hasRegionalRegulations,
    String summary,
    String lastReviewedAt,
    List<CatalogRegion> regions,
    List<CatalogRule> rules,
    CatalogLateFee lateFee,
    Integer formalNoticeDays,
    List<CatalogTenancyRule> tenancyRules) {}
```

In `EntityPrefix.java`, add after the `RRL` entry:

```java
  RRT("RRT", "Rent Regulation Tenancy Rules"),
```

- [ ] **Step 4: Fix every `CatalogCountry` construction site**

Adding a record component breaks all callers. Find them:

Run: `cd backend && grep -rn 'new CatalogCountry(' --include='*.java' . | grep -v target`

Pass `null` for `tenancyRules` at each site except the catalog-export builder, which Task 4 fills in.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && mvn test -pl buurman-backoffice -am -Dtest=RentRegulationCatalogTest -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: PASS — 9 tests (6 existing + 3 new).

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/domain/TenancyRuleTopic.java \
        backend/buurman-common/src/main/java/com/buurman/domain/regulation/CatalogTenancyRule.java \
        backend/buurman-common/src/main/java/com/buurman/domain/regulation/CatalogCountry.java \
        backend/buurman-common/src/main/java/com/buurman/util/EntityPrefix.java \
        backend/buurman-backoffice/src/test/java/com/buurman/service/backoffice/RentRegulationCatalogTest.java
git commit -m "feat(regulation): add tenancy-rule catalog domain and parse guards"
```

---

### Task 2: Migration V068 and JOOQ regeneration

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V068__rent_regulation_tenancy_rules.sql`

**Interfaces:**
- Consumes: nothing.
- Produces: table `rent_regulation_tenancy_rules`.

**Note on JOOQ:** `RentRegulationRepository` addresses these tables with the *string-based* DSL
(`table("rent_regulation_regions")`, `field("region_code", String.class)`) rather than generated
classes, so Task 3 does **not** depend on codegen output. Regenerate anyway for module consistency,
but do not expect a `RentRegulationTenancyRules` generated class to be required.

- [ ] **Step 1: Write the migration**

```sql
-- Display-only tenancy-law reference entries per regulation country.
-- Facts that are not rent-increase caps: notice periods, tenancy duration, deposits,
-- lease formalities, registration duties, fixed-amount fees.
-- Global reference data: no team_id, no deleted_at — reload wipes and re-seeds,
-- matching rent_regulation_regions.
CREATE TABLE rent_regulation_tenancy_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    topic VARCHAR(40) NOT NULL,
    label VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    effective_from DATE,
    legal_basis TEXT,
    source_url TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, identifier),
    CONSTRAINT chk_rr_tenancy_rules_label_not_blank CHECK (btrim(label) <> ''),
    CONSTRAINT chk_rr_tenancy_rules_value_not_blank CHECK (btrim(value) <> '')
);

CREATE INDEX idx_rr_tenancy_rules_country ON rent_regulation_tenancy_rules (country_id);
CREATE INDEX idx_rr_tenancy_rules_topic ON rent_regulation_tenancy_rules (country_id, topic);
CREATE INDEX idx_rr_tenancy_rules_region ON rent_regulation_tenancy_rules (region_id);
```

- [ ] **Step 2: Regenerate JOOQ**

Run: `cd backend && mvn generate-sources -pl buurman-jooq -am`
Expected: BUILD SUCCESS. (Required by `CLAUDE.md` after any migration; the repository code in
Task 3 uses the string DSL and does not consume the generated class.)

- [ ] **Step 3: Verify the migration applies cleanly**

The integration test in Task 3 runs Flyway against a `postgres:18-alpine` Testcontainer, which is
the real check. For a fast smoke test now:

Run: `cd backend && mvn -q -pl buurman-jooq -am generate-sources -Dmaven.build.cache.enabled=false && echo MIGRATION_OK`
Expected: `MIGRATION_OK`. A Flyway syntax error fails here. Docker must be running.

- [ ] **Step 4: Commit**

```bash
git add backend/buurman-jooq/src/main/resources/db/migration/V068__rent_regulation_tenancy_rules.sql
git commit -m "feat(db): add rent_regulation_tenancy_rules table (V068)"
```

---

### Task 3: Domain POJO, repository persistence, delete ordering

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/RentRegulationTenancyRule.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/repository/RentRegulationRepository.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/RentRegulationRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `EntityPrefix.RRT` (Task 1); JOOQ `RENT_REGULATION_TENANCY_RULES` (Task 2).
- Produces:
  - `RentRegulationTenancyRule` Lombok `@Data @Builder` POJO with fields `UUID id, Optional<Sid> identifier, UUID countryId, Optional<UUID> regionId, TenancyRuleTopic topic, String label, String value, Optional<LocalDate> effectiveFrom, Optional<String> legalBasis, Optional<String> sourceUrl, Optional<String> notes, Instant createdAt, Instant updatedAt, Optional<String> createdBy, Optional<String> updatedBy`
  - `RentRegulationRepository.saveTenancyRule(RentRegulationTenancyRule) -> RentRegulationTenancyRule`
  - `RentRegulationRepository.findTenancyRulesByCountryId(UUID) -> List<RentRegulationTenancyRule>`
  - `RentRegulationRepository.findAllTenancyRules() -> List<RentRegulationTenancyRule>`
  - `deleteAllReferenceData()` deletes tenancy rules first

- [ ] **Step 1: Write the failing test**

Add to `RentRegulationRepositoryIntegrationTest` (if the class does not exist, create it extending `AbstractRepositoryIntegrationTest`, following the pattern of the other `*RepositoryIntegrationTest` classes):

```java
  @Test
  @DisplayName("a region-scoped tenancy rule round-trips and does not block reference wipe")
  void tenancyRule_roundTripsAndDeletes() {
    RentRegulationCountry country =
        repository.saveCountry(
            RentRegulationCountry.builder()
                .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRC)))
                .countryCode("XT")
                .countryName("Testland")
                .hasRegionalRegulations(true)
                .lateFeePolicy(LateFeePolicy.FORBIDDEN)
                .build());
    RentRegulationRegion region =
        repository.saveRegion(
            RentRegulationRegion.builder()
                .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRG)))
                .countryId(country.getId())
                .regionCode("XT-1")
                .regionName("Region One")
                .build());

    repository.saveTenancyRule(
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRT)))
            .countryId(country.getId())
            .regionId(Optional.of(region.getId()))
            .topic(TenancyRuleTopic.TENANCY_DURATION)
            .label("Minimum fixed term")
            .value("5 years")
            .effectiveFrom(Optional.of(LocalDate.parse("2026-01-01")))
            .legalBasis(Optional.of("MRG § 29"))
            .build());

    List<RentRegulationTenancyRule> found =
        repository.findTenancyRulesByCountryId(country.getId());
    assertThat(found).hasSize(1);
    assertThat(found.getFirst().getTopic()).isEqualTo(TenancyRuleTopic.TENANCY_DURATION);
    assertThat(found.getFirst().getValue()).isEqualTo("5 years");
    assertThat(found.getFirst().getRegionId()).contains(region.getId());
    assertThat(found.getFirst().getEffectiveFrom()).contains(LocalDate.parse("2026-01-01"));

    // The FK to regions means the wipe must delete tenancy rules FIRST or this throws.
    repository.deleteAllReferenceData();
    assertThat(repository.findAllTenancyRules()).isEmpty();
  }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn test -pl buurman-core -am -Dtest=RentRegulationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: FAIL — `saveTenancyRule` does not exist (compilation error). Docker must be running.

- [ ] **Step 3: Write the domain POJO**

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

/** A display-only tenancy-law reference entry for a regulation country. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentRegulationTenancyRule {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID countryId;
  @Builder.Default private Optional<UUID> regionId = Optional.empty();
  private TenancyRuleTopic topic;
  private String label;
  private String value;
  @Builder.Default private Optional<LocalDate> effectiveFrom = Optional.empty();
  @Builder.Default private Optional<String> legalBasis = Optional.empty();
  @Builder.Default private Optional<String> sourceUrl = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
```

- [ ] **Step 4: Add repository persistence**

In `RentRegulationRepository`, add these static aliases next to the existing `R_*` block (which
starts at `private static final Table<?> REGIONS = table("rent_regulation_regions");`):

```java
  private static final Table<?> TENANCY_RULES = table("rent_regulation_tenancy_rules");
  private static final Field<UUID> T_ID = field("id", UUID.class);
  private static final Field<String> T_IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> T_COUNTRY_ID = field("country_id", UUID.class);
  private static final Field<UUID> T_REGION_ID = field("region_id", UUID.class);
  private static final Field<String> T_TOPIC = field("topic", String.class);
  private static final Field<String> T_LABEL = field("label", String.class);
  private static final Field<String> T_VALUE = field("value", String.class);
  private static final Field<LocalDate> T_EFFECTIVE_FROM = field("effective_from", LocalDate.class);
  private static final Field<String> T_LEGAL_BASIS = field("legal_basis", String.class);
  private static final Field<String> T_SOURCE_URL = field("source_url", String.class);
  private static final Field<String> T_NOTES = field("notes", String.class);
  private static final Field<Timestamp> T_CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> T_UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<String> T_CREATED_BY = field("created_by", String.class);
  private static final Field<String> T_UPDATED_BY = field("updated_by", String.class);
```

Note `value` is a reserved word in some dialects but is fine as an unquoted PostgreSQL column here;
the string DSL quotes identifiers as needed. Then add:

```java
  public RentRegulationTenancyRule saveTenancyRule(RentRegulationTenancyRule rule) {
    Timestamp now = Timestamp.from(clock.instant());
    UUID id = UUID.randomUUID();

    dsl.insertInto(TENANCY_RULES)
        .set(T_ID, id)
        .set(T_IDENTIFIER, rule.getIdentifier().orElseThrow().value())
        .set(T_COUNTRY_ID, rule.getCountryId())
        .set(T_REGION_ID, rule.getRegionId().orElse(null))
        .set(T_TOPIC, rule.getTopic().name())
        .set(T_LABEL, rule.getLabel())
        .set(T_VALUE, rule.getValue())
        .set(T_EFFECTIVE_FROM, rule.getEffectiveFrom().orElse(null))
        .set(T_LEGAL_BASIS, rule.getLegalBasis().orElse(null))
        .set(T_SOURCE_URL, rule.getSourceUrl().orElse(null))
        .set(T_NOTES, rule.getNotes().orElse(null))
        .set(T_CREATED_AT, now)
        .set(T_UPDATED_AT, now)
        .set(T_CREATED_BY, rule.getCreatedBy().orElse(null))
        .set(T_UPDATED_BY, rule.getUpdatedBy().orElse(null))
        .execute();

    rule.setId(id);
    return rule;
  }

  public List<RentRegulationTenancyRule> findTenancyRulesByCountryId(UUID countryId) {
    return List.copyOf(
        dsl.select()
            .from(TENANCY_RULES)
            .where(T_COUNTRY_ID.eq(countryId))
            .orderBy(T_TOPIC, T_LABEL)
            .fetch(this::toTenancyRuleDomain));
  }

  public List<RentRegulationTenancyRule> findAllTenancyRules() {
    return List.copyOf(dsl.select().from(TENANCY_RULES).fetch(this::toTenancyRuleDomain));
  }

  private RentRegulationTenancyRule toTenancyRuleDomain(org.jooq.Record record) {
    RentRegulationTenancyRule rule = new RentRegulationTenancyRule();
    rule.setId(record.get(T_ID));
    rule.setIdentifier(Optional.ofNullable(record.get(T_IDENTIFIER)).map(Sid::of));
    rule.setCountryId(record.get(T_COUNTRY_ID));
    rule.setRegionId(Optional.ofNullable(record.get(T_REGION_ID)));
    rule.setTopic(TenancyRuleTopic.valueOf(record.get(T_TOPIC)));
    rule.setLabel(record.get(T_LABEL));
    rule.setValue(record.get(T_VALUE));
    rule.setEffectiveFrom(Optional.ofNullable(record.get(T_EFFECTIVE_FROM)));
    rule.setLegalBasis(Optional.ofNullable(record.get(T_LEGAL_BASIS)));
    rule.setSourceUrl(Optional.ofNullable(record.get(T_SOURCE_URL)));
    rule.setNotes(Optional.ofNullable(record.get(T_NOTES)));
    rule.setCreatedAt(
        Optional.ofNullable(record.get(T_CREATED_AT)).map(Timestamp::toInstant).orElse(null));
    rule.setUpdatedAt(
        Optional.ofNullable(record.get(T_UPDATED_AT)).map(Timestamp::toInstant).orElse(null));
    rule.setCreatedBy(Optional.ofNullable(record.get(T_CREATED_BY)));
    rule.setUpdatedBy(Optional.ofNullable(record.get(T_UPDATED_BY)));
    return rule;
  }
```

Then change `deleteAllReferenceData()` so tenancy rules go first:

```java
  public void deleteAllReferenceData() {
    dsl.deleteFrom(TENANCY_RULES).execute();
    dsl.deleteFrom(RULES).execute();
    dsl.deleteFrom(REGIONS).execute();
    dsl.deleteFrom(COUNTRIES).execute();
  }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend && mvn test -pl buurman-core -am -Dtest=RentRegulationRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-common/src/main/java/com/buurman/domain/RentRegulationTenancyRule.java \
        backend/buurman-core/src/main/java/com/buurman/repository/RentRegulationRepository.java \
        backend/buurman-core/src/test/java/com/buurman/repository/RentRegulationRepositoryIntegrationTest.java
git commit -m "feat(regulation): persist tenancy rules and wipe them before regions"
```

---

### Task 4: Backoffice reload, diff and export round-trip

**Files:**
- Modify: `backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/RentRegulationCatalogService.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/response/RentRegulationReloadResult.java`
- Test: `backend/buurman-backoffice/src/test/java/com/buurman/service/backoffice/RentRegulationCatalogDiffTest.java`

**Interfaces:**
- Consumes: `CatalogTenancyRule` (Task 1), `saveTenancyRule` / `findTenancyRulesByCountryId` / `findAllTenancyRules` (Task 3).
- Produces: `RentRegulationReloadResult` gains a trailing `int tenancyRulesLoaded`; `diff()` emits country field diffs keyed `tenancyRule[<TOPIC>/<label>]`; `toCatalogCountry(...)` takes a fifth argument `List<RentRegulationTenancyRule>` and populates `CatalogCountry.tenancyRules`.

**Key structural fact:** `toCatalogCountry(...)` is the single DB→Catalog mapper, called from
`currentCountries()`, which feeds **both** the export endpoint (line ~127) and `diff()` (line ~165).
Changing it once covers the diff's "current" side *and* the export round-trip — do not write two
mapping paths.

- [ ] **Step 1: Write the failing tests**

Add to `RentRegulationCatalogDiffTest`, mirroring `diff_reportsLateFeeChanges` exactly — mock the
loader and repository, call `service.diff()`, then read `entry.fields()`. Note the accessors are
`field()` / `before()` / `after()`.

Because `country(...)` and `domainCountry(...)` are the existing builders, add the new
`findAllTenancyRules()` stub to **every** existing test's mock set too, or Mockito returns null and
the existing tests NPE. The existing tests already stub `findAllRegions()` and `findAllRules()`;
add `when(repository.findAllTenancyRules()).thenReturn(List.of());` beside them in each.

```java
  @Test
  @DisplayName("a tenancy rule the catalogue adds is reported as a country change")
  void diff_detectsAddedTenancyRule() {
    CatalogCountry at =
        new CatalogCountry(
            "AT", "Austria", false, null, null, null, List.of(), null, 14,
            List.of(
                new CatalogTenancyRule(
                    TenancyRuleTopic.TENANCY_DURATION,
                    null,
                    "Minimum fixed term",
                    "5 years",
                    "2026-01-01",
                    "MRG § 29",
                    "https://ris.bka.gv.at",
                    null)));
    when(loader.load())
        .thenReturn(new RentRegulationCatalog("2026.6", "2026-09-26", "desc", List.of(at)));
    when(repository.findAllCountries())
        .thenReturn(List.of(domainCountry(NL_ID, "AT", "Austria")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules()).thenReturn(List.of());
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    RentRegulationDiffEntry entry =
        diff.byCountry().getFirst().changes().stream()
            .filter(e -> e.entity().equals("COUNTRY"))
            .findFirst()
            .orElseThrow();
    assertThat(entry.fields())
        .anySatisfy(
            f -> {
              assertThat(f.field()).isEqualTo("tenancyRule[TENANCY_DURATION/Minimum fixed term]");
              assertThat(f.before()).isEmpty();
              assertThat(f.after()).contains("5 years");
            });
  }

  @Test
  @DisplayName("a country with no tenancy rules on either side reports no tenancy diff")
  void diff_noTenancyRules_yieldsNoTenancyField() {
    CatalogCountry at =
        new CatalogCountry(
            "AT", "Austria", false, null, null, null, List.of(), null, 14, List.of());
    when(loader.load())
        .thenReturn(new RentRegulationCatalog("2026.6", "2026-09-26", "desc", List.of(at)));
    when(repository.findAllCountries())
        .thenReturn(List.of(domainCountry(NL_ID, "AT", "Austria")));
    when(repository.findAllRegions()).thenReturn(List.of());
    when(repository.findAllRules()).thenReturn(List.of());
    when(repository.findAllTenancyRules()).thenReturn(List.of());

    RentRegulationCatalogDiff diff = service.diff();

    assertThat(diff.byCountry().getFirst().changes())
        .allSatisfy(
            e ->
                assertThat(e.fields())
                    .noneSatisfy(f -> assertThat(f.field()).startsWith("tenancyRule[")));
  }
```

Also update the existing `country(...)` builder for the new record component:

```java
  private static CatalogCountry country(String code, String name, List<CatalogRule> rules) {
    return new CatalogCountry(code, name, false, null, null, null, rules, null, null, null);
  }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl buurman-backoffice -am -Dtest=RentRegulationCatalogDiffTest -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: FAIL — no `tenancyRule[...]` field is produced.

- [ ] **Step 3: Seed tenancy rules on reload**

In `reload()`, add a counter and an insert loop after the rules loop, inside the same country iteration:

```java
      for (CatalogTenancyRule tenancyRule : safe(country.tenancyRules())) {
        insertTenancyRule(tenancyRule, countryId, regionIds, actor);
        tenancyRules++;
      }
```

Declare `int tenancyRules = 0;` beside `int rules = 0;`, include it in the `log.warn` call and in the returned `RentRegulationReloadResult`.

Add the insert helper, mirroring `insertRule`'s region resolution so an undeclared region fails loudly:

```java
  private void insertTenancyRule(
      CatalogTenancyRule rule, UUID countryId, Map<String, UUID> regionIds, String actor) {
    Optional<UUID> regionId =
        Optional.ofNullable(rule.regionCode())
            .map(
                code ->
                    Optional.ofNullable(regionIds.get(code))
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Catalog tenancy rule references unknown region '"
                                        + code
                                        + "' in country "
                                        + countryId)));

    repository.saveTenancyRule(
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(SidGenerator.generate(EntityPrefix.RRT)))
            .countryId(countryId)
            .regionId(regionId)
            .topic(rule.topic())
            .label(rule.label())
            .value(rule.value())
            .effectiveFrom(Optional.ofNullable(rule.effectiveFrom()).map(LocalDate::parse))
            .legalBasis(Optional.ofNullable(rule.legalBasis()))
            .sourceUrl(Optional.ofNullable(rule.sourceUrl()))
            .notes(Optional.ofNullable(rule.notes()))
            .createdBy(Optional.of(actor))
            .updatedBy(Optional.of(actor))
            .build());
  }
```

- [ ] **Step 4: Add tenancy rules to the shared DB→Catalog mapper**

This single change supplies both the diff's "current" side and the export round-trip.

In `currentCountries()`, add a grouping beside `rulesByCountry`:

```java
    Map<UUID, List<RentRegulationTenancyRule>> tenancyByCountry =
        repository.findAllTenancyRules().stream()
            .collect(Collectors.groupingBy(RentRegulationTenancyRule::getCountryId));
```

and pass `tenancyByCountry.getOrDefault(c.getId(), List.of())` as a new fifth argument to
`toCatalogCountry`.

In `toCatalogCountry`, accept that argument and map it, resolving `regionId` back to its code via
the `regionCodeById` map the method already receives:

```java
    List<CatalogTenancyRule> catalogTenancyRules =
        tenancyRules.stream()
            .map(
                t ->
                    new CatalogTenancyRule(
                        t.getTopic(),
                        t.getRegionId().map(regionCodeById::get).orElse(null),
                        t.getLabel(),
                        t.getValue(),
                        t.getEffectiveFrom().map(LocalDate::toString).orElse(null),
                        t.getLegalBasis().orElse(null),
                        t.getSourceUrl().orElse(null),
                        t.getNotes().orElse(null)))
            .sorted(
                Comparator.comparing((CatalogTenancyRule t) -> t.topic().name())
                    .thenComparing(CatalogTenancyRule::label))
            .toList();
```

Pass `catalogTenancyRules` as the final argument to the `CatalogCountry` construction, replacing the
`null` left by Task 1 Step 4.

- [ ] **Step 5: Add the diff**

In the country field-diff method, after the `formalNoticeDays` diff, compare tenancy rules keyed by topic + label so an added, removed or changed entry is reported:

```java
    Map<String, CatalogTenancyRule> curTenancy = new java.util.LinkedHashMap<>();
    for (CatalogTenancyRule t : safe(cur.tenancyRules())) {
      curTenancy.put(t.topic().name() + "/" + t.label(), t);
    }
    Map<String, CatalogTenancyRule> tgtTenancy = new java.util.LinkedHashMap<>();
    for (CatalogTenancyRule t : safe(tgt.tenancyRules())) {
      tgtTenancy.put(t.topic().name() + "/" + t.label(), t);
    }
    Set<String> keys = new java.util.LinkedHashSet<>(curTenancy.keySet());
    keys.addAll(tgtTenancy.keySet());
    for (String key : keys) {
      addFieldDiff(
          fields,
          "tenancyRule[" + key + "]",
          Optional.ofNullable(curTenancy.get(key)).map(this::describeTenancyRule).orElse(""),
          Optional.ofNullable(tgtTenancy.get(key)).map(this::describeTenancyRule).orElse(""));
    }
```

```java
  private String describeTenancyRule(CatalogTenancyRule rule) {
    StringBuilder sb = new StringBuilder(rule.value());
    Optional.ofNullable(rule.effectiveFrom()).ifPresent(d -> sb.append(" (from ").append(d).append(")"));
    Optional.ofNullable(rule.legalBasis()).ifPresent(b -> sb.append(" [").append(b).append("]"));
    return sb.toString();
  }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn test -pl buurman-backoffice -am -Dtest='RentRegulationCatalog*Test' -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: PASS — all catalog and diff tests green, including the pre-existing ones you added the
`findAllTenancyRules()` stub to.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-backoffice backend/buurman-common/src/main/java/com/buurman/dto/response/RentRegulationReloadResult.java
git commit -m "feat(backoffice): seed, diff and export tenancy rules"
```

---

### Task 5: API surface — DTO, mapper, OpenAPI, client regen

**Files:**
- Create: `backend/buurman-common/src/main/java/com/buurman/dto/response/RentRegulationTenancyRuleResponse.java`
- Modify: `backend/buurman-common/src/main/java/com/buurman/dto/response/RentRegulationCountryDetailResponse.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/mapper/RentRegulationMapper.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/RentRegulationService.java`
- Modify: `openapi/src/app.yaml`

**Interfaces:**
- Consumes: `RentRegulationTenancyRule` (Task 3), `TenancyRuleTopic` (Task 1).
- Produces: `RentRegulationTenancyRuleResponse(Sid identifier, TenancyRuleTopic topic, Optional<String> regionCode, String label, String value, Optional<LocalDate> effectiveFrom, Optional<String> legalBasis, Optional<String> sourceUrl, Optional<String> notes)`; `RentRegulationCountryDetailResponse.tenancyRules()`; generated TS type `RentRegulationTenancyRuleResponse`.

- [ ] **Step 1: Write the DTO**

```java
package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.TenancyRuleTopic;
import com.buurman.util.SkipTestCoverage;

/** A display-only tenancy-law reference entry for a regulation country. */
@SkipTestCoverage
public record RentRegulationTenancyRuleResponse(
    Sid identifier,
    TenancyRuleTopic topic,
    Optional<String> regionCode,
    String label,
    String value,
    Optional<LocalDate> effectiveFrom,
    Optional<String> legalBasis,
    Optional<String> sourceUrl,
    Optional<String> notes) {}
```

Add `List<RentRegulationTenancyRuleResponse> tenancyRules` as the final component of `RentRegulationCountryDetailResponse`, then fix its construction sites:

Run: `cd backend && grep -rn 'new RentRegulationCountryDetailResponse(' --include='*.java' . | grep -v target`

- [ ] **Step 2: Map domain to DTO**

In `RentRegulationMapper`, add a method following the existing region/rule mapping style:

```java
  public RentRegulationTenancyRuleResponse toTenancyRuleResponse(
      RentRegulationTenancyRule rule, Optional<String> regionCode) {
    return new RentRegulationTenancyRuleResponse(
        rule.getIdentifier().orElseThrow(),
        rule.getTopic(),
        regionCode,
        rule.getLabel(),
        rule.getValue(),
        rule.getEffectiveFrom(),
        rule.getLegalBasis(),
        rule.getSourceUrl(),
        rule.getNotes());
  }
```

In `RentRegulationService`, where the country detail response is assembled, fetch `findTenancyRulesByCountryId` and map each entry, resolving `regionId` to its `regionCode` from the regions already loaded for that country.

- [ ] **Step 3: Add the OpenAPI schemas**

In `openapi/src/app.yaml`, add beside `LateFeePolicy`:

```yaml
    TenancyRuleTopic:
      type: string
      enum:
        - NOTICE_PERIOD
        - TENANCY_DURATION
        - DEPOSIT
        - LEASE_FORM
        - REGISTRATION
        - FEES_AND_PENALTIES
        - OTHER

    RentRegulationTenancyRuleResponse:
      type: object
      required: [identifier, topic, label, value]
      properties:
        identifier:
          type: string
        topic:
          $ref: '#/components/schemas/TenancyRuleTopic'
        regionCode:
          type: string
          nullable: true
        label:
          type: string
        value:
          type: string
        effectiveFrom:
          type: string
          format: date
          nullable: true
        legalBasis:
          type: string
          nullable: true
        sourceUrl:
          type: string
          nullable: true
        notes:
          type: string
          nullable: true
```

Add to `RentRegulationCountryDetailResponse`'s properties:

```yaml
        tenancyRules:
          type: array
          items:
            $ref: '#/components/schemas/RentRegulationTenancyRuleResponse'
```

- [ ] **Step 4: Bundle and regenerate**

Run: `make bundle-openapi && cd frontend && yarn generate:api`
Expected: both succeed; `openapi/app.yaml` now contains `TenancyRuleTopic`.

Verify: `grep -c TenancyRuleTopic openapi/app.yaml`
Expected: a non-zero count.

- [ ] **Step 5: Run the backend tests**

Run: `cd backend && mvn test -pl buurman-core -am -Dtest='RentRegulation*Test' -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-common backend/buurman-core openapi/
git commit -m "feat(api): expose tenancy rules on the regulation country detail response"
```

---

### Task 6: Regulations page section and translations

**Files:**
- Modify: `frontend/app/src/components/rentRegulations/RegulationSummary.tsx`
- Modify: `frontend/app/src/types/rentRegulation.ts`
- Modify: `frontend/app/public/locales/en/contracts.json` (and the other 12 locales)
- Test: `frontend/app/src/components/rentRegulations/__tests__/RegulationSummary.test.tsx`

**Interfaces:**
- Consumes: generated `RentRegulationTenancyRuleResponse` (Task 5).
- Produces: a "Tenancy rules" section grouped by topic; hidden when the list is empty.

- [ ] **Step 1: Write the failing tests**

Add a new `describe` block to `RegulationSummary.test.tsx`. The file's existing helper is
`renderWithProviders` from `@/test/test-utils` and its fixture is the `base` object — reuse both.

```tsx
describe('RegulationSummary tenancy rules', () => {
  it('groups tenancy rules by topic and shows value, date and legal basis', () => {
    renderWithProviders(
      <RegulationSummary
        country={{
          ...base,
          tenancyRules: [
            {
              identifier: 'rrt_01TEST',
              topic: 'TENANCY_DURATION',
              label: 'Minimum fixed term',
              value: '5 years',
              effectiveFrom: '2026-01-01',
              legalBasis: 'MRG § 29',
            },
          ],
        }}
      />
    );
    expect(screen.getByText('Tenancy rules')).toBeInTheDocument();
    expect(screen.getByText('Tenancy duration')).toBeInTheDocument();
    expect(screen.getByText('Minimum fixed term')).toBeInTheDocument();
    expect(screen.getByText('5 years')).toBeInTheDocument();
    expect(screen.getByText(/MRG § 29/)).toBeInTheDocument();
  });

  it('hides the section when there are no tenancy rules', () => {
    renderWithProviders(<RegulationSummary country={{ ...base, tenancyRules: [] }} />);
    expect(screen.queryByText('Tenancy rules')).not.toBeInTheDocument();
  });

  it('hides the section when tenancyRules is absent', () => {
    renderWithProviders(<RegulationSummary country={base} />);
    expect(screen.queryByText('Tenancy rules')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && yarn test --run RegulationSummary`
Expected: FAIL — the section is not rendered.

- [ ] **Step 3: Re-export the type**

In `frontend/app/src/types/rentRegulation.ts`, add `RentRegulationTenancyRuleResponse` to the existing re-export block.

- [ ] **Step 4: Render the section**

In `RegulationSummary.tsx`, add `ScrollText` to the existing `lucide-react` import. Above the
component, add the grouping helper:

```tsx
const TOPIC_ORDER = [
  'NOTICE_PERIOD',
  'TENANCY_DURATION',
  'DEPOSIT',
  'LEASE_FORM',
  'REGISTRATION',
  'FEES_AND_PENALTIES',
  'OTHER',
] as const;
```

Then insert this block immediately after the late-fee `<div>` and before the `{/* Summary — rich
text */}` block, matching the surrounding class names:

```tsx
        {/* Tenancy-law reference */}
        {country.tenancyRules != null && country.tenancyRules.length > 0 && (
          <div className="px-6 py-5 border-b border-border-default">
            <div className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-text-muted mb-1">
              <ScrollText className="h-3.5 w-3.5" />
              {t('rentRegulations.tenancyRules.title')}
            </div>
            <p className="text-xs text-text-muted mb-3">
              {t('rentRegulations.tenancyRules.subtitle')}
            </p>
            {TOPIC_ORDER.filter((topic) =>
              country.tenancyRules?.some((r) => r.topic === topic)
            ).map((topic) => (
              <div key={topic} className="mb-4 last:mb-0">
                <div className="text-sm font-semibold text-text-primary mb-1.5">
                  {t(`rentRegulations.tenancyRules.topic.${topic}`)}
                </div>
                <ul className="space-y-1.5">
                  {country.tenancyRules
                    ?.filter((r) => r.topic === topic)
                    .map((rule) => (
                      <li key={rule.identifier} className="text-sm">
                        <span className="text-text-secondary">{rule.label}</span>
                        {': '}
                        <span className="text-text-primary font-medium">
                          {rule.value}
                        </span>
                        {rule.regionCode && (
                          <span className="text-text-muted"> ({rule.regionCode})</span>
                        )}
                        {rule.effectiveFrom && (
                          <span className="text-text-muted">
                            {' — '}
                            {t('rentRegulations.tenancyRules.effectiveFrom', {
                              date: rule.effectiveFrom,
                            })}
                          </span>
                        )}
                        {rule.legalBasis && (
                          <span className="text-text-muted"> [{rule.legalBasis}]</span>
                        )}
                        {rule.sourceUrl && (
                          <>
                            {' '}
                            <a
                              href={rule.sourceUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-text-link underline"
                            >
                              {t('rentRegulations.tenancyRules.source')}
                            </a>
                          </>
                        )}
                      </li>
                    ))}
                </ul>
              </div>
            ))}
          </div>
        )}
```

- [ ] **Step 5: Add the translations**

In `frontend/app/public/locales/en/contracts.json`, add under `rentRegulations`:

```json
    "tenancyRules": {
      "title": "Tenancy rules",
      "subtitle": "Reference only — not rent-increase limits. Verify locally.",
      "effectiveFrom": "From {{date}}",
      "source": "Source",
      "topic": {
        "NOTICE_PERIOD": "Notice periods",
        "TENANCY_DURATION": "Tenancy duration",
        "DEPOSIT": "Deposits",
        "LEASE_FORM": "Lease formalities",
        "REGISTRATION": "Registration duties",
        "FEES_AND_PENALTIES": "Fees and penalties",
        "OTHER": "Other"
      }
    }
```

Add the same block to the other 12 locales (`da de el es fi fr it nb nl pl pt sv`), translated. English strings are acceptable as an interim value only if a translation is genuinely unavailable — do not leave the key absent, because a missing key renders the raw key path to the user.

- [ ] **Step 6: Run tests and lint**

Run: `cd frontend && yarn test --run RegulationSummary && yarn lint`
Expected: PASS, no lint errors.

- [ ] **Step 7: Commit**

```bash
git add frontend/
git commit -m "feat(app): show tenancy-rule reference on the regulations page"
```

---

### Task 7: Teach the audit skill, and seed the first entries

**Files:**
- Modify: `.claude/skills/update-rent-regulations/SKILL.md`
- Modify: `backend/buurman-backoffice/src/main/resources/rent-regulations/rent-regulations.json`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: everything above.
- Produces: the bundled catalog carries its first `tenancyRules` entries; future audits populate them.

This task is what makes the feature pay off. Without it the structure ships empty and audits keep discarding these facts.

- [ ] **Step 1: Document the structure in the skill**

In `SKILL.md`, under "Step 3 — Update the JSON", add a subsection documenting: the `tenancyRules[]` array; each field; the `TenancyRuleTopic` constants; that `regionCode` must match a declared region; that it is **display-only** and must never receive rent-increase caps; and the **no-history rule** — a superseded entry is replaced in place, not retained, with `effectiveFrom` dating the current fact.

Also add to the "Goals (this run)" list: record tenancy facts that are not rent-increase caps in `tenancyRules` rather than discarding them.

- [ ] **Step 2: Seed the confirmed facts**

Add `tenancyRules` entries for the facts the audit already confirmed with sources. Each needs `topic`, `label`, `value`, `effectiveFrom`, `legalBasis`, `sourceUrl`:

- **AT** `TENANCY_DURATION` — "Minimum fixed term" / "5 years" / from `2026-01-01` / `MRG § 29 (5. MILG, BGBl I 114/2025)`
- **DK** `FEES_AND_PENALTIES` — "Reminder fee (påkravsgebyr)" / "DKK 344" / from `2027-01-01` / `lejeloven § 182 stk. 2 (VEJ nr 9959 af 08/09/2026)`
- **IE** `FEES_AND_PENALTIES` — "Fixed payment notice" / "EUR 200 within 28 days (prescribable up to EUR 1,000)"
- **LU** `LEASE_FORM` — "Written lease" / "Mandatory, sous peine de nullité, with eight mandatory clauses" / from `2024-08-01` / `art. 5(1), loi du 21 septembre 2006`
- **US** `REGISTRATION` — "DC RentRegistry filing" / "Mandatory" / regionCode `DC`
- **CZ** `OTHER` — "Housing-support framework" / "zákon č. 175/2025 Sb." / from `2026-01-01`
- **FI** `NOTICE_PERIOD` — "Increase takes effect" / "From the start of the rent period falling at least one month after notice" / from `2026-10-01` / `AHVL § 27(2)`

Do **not** invent values for facts not confirmed in the audit report (`docs/rent-regulations/2026-09-26-audit.md`).

Bump `version` to `2026.6` and `generatedAt` to the current date (`date +%F`).

- [ ] **Step 3: Fix the stale migration number in CLAUDE.md**

Change the two places that say migrations are "currently at V066" to **V068**.

- [ ] **Step 4: Validate**

Run:
```bash
python3 -m json.tool backend/buurman-backoffice/src/main/resources/rent-regulations/rent-regulations.json > /dev/null
cd backend && mvn test -pl buurman-backoffice -am -Dtest='RentRegulationCatalog*Test' -Dsurefire.failIfNoSpecifiedTests=false -Pquick -Dmaven.build.cache.enabled=false
```
Expected: JSON valid; all catalog tests PASS, including the new well-formedness and region-resolution checks against the real seeded data.

- [ ] **Step 5: Commit**

```bash
git add .claude/skills/update-rent-regulations/SKILL.md CLAUDE.md \
        backend/buurman-backoffice/src/main/resources/rent-regulations/rent-regulations.json
git commit -m "feat(regulation): seed first tenancy rules and teach the audit skill to keep them"
```

---

## Done when

- `mvn test` green across the backend; `yarn test` and `yarn lint` green in the frontend.
- A backoffice **Reload from catalog** seeds tenancy rules, and **Export** returns them unchanged.
- The regulations page shows a grouped "Tenancy rules" section for AT/DK/IE/LU/US/CZ/FI and nothing for the rest.
- `SKILL.md` documents `tenancyRules`, so the next audit fills it instead of dropping facts.
