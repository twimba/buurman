# Lease Agreement Documents — Slice 0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the placeholder lease with a kind-aware, per-(country, kind, language) document model, ship the NL residential reference in 13 languages, and keep every other country working unchanged.

**Architecture:** A shared Thymeleaf shell (`lease-agreement/_shell.html`) loops over the resolved, ordered clauses and inserts the matching `clause-<key>` fragment from a per-language file `lease-agreement/{CC}/{kind}/{lang}.html`. `lease_clause_templates` keeps clause *structure* (kind, order, optional, pinned) and the short title/summary i18n keys the toggle UI needs; full legal text lives only in the language files. Where no new-model document exists for (country, kind), the exporter falls back to the legacy `generic.html`, so BE/DE/ES/FR/GB/PT keep working until their slices land.

**Tech Stack:** Java 25, Spring Boot 4, JOOQ 3.20, Flyway, Thymeleaf, MapStruct, JUnit 5/Mockito/Testcontainers; React 19 + TypeScript + Vitest + Orval; OpenAPI source in `openapi/src/`.

**Spec:** `docs/superpowers/specs/2026-10-03-lease-agreement-documents-slice0-design.md` (amended in Task 0 below).

## Global Constraints

- Migrations: never edit existing ones. Highest is **V086**, so Slice 0 adds **V087** (CLAUDE.md's "V084" is stale).
- Curly braces on every `if/else/for/while`. Idiomatic `Optional` only (no `.get()` without a check, no `== null` on Optionals).
- Templates are **global reference data**: no `team_id` filtering in `LeaseClauseTemplateRepository`. The new contract column is team-scoped through existing `ContractRepository` queries.
- Never expose internal UUIDs; DTOs stay records exposing `identifier`.
- OpenAPI: after editing `openapi/src/`, run `make bundle-openapi`, then `cd frontend && yarn generate:api`; the generated clients are gitignored.
- Backend tests: `make test` is the sanctioned entry; one class: `cd backend && mvn clean install -DskipTests -pl <module> -am && mvn test -pl <module> -am -Dtest=Class#method` (`-pl` always with `-am`). Docker must be running for integration tests.
- Languages: the 13 in `DocumentLanguages.ORDERED` = `en nl de fr pt es sv it fi el pl da nb`.
- National language(s) for fallback/authoritative marking: NL→`nl`; BE→`nl,fr`; DE→`de`; ES→`es`; FR→`fr`; GB→`en`; PT→`pt`.
- Legal text is **not lawyer-vetted**: every language file starts with a header comment `legal-basis:`, `reviewed-by: none`, `translation: authoritative|machine-drafted`; every PDF carries the localized "draft, have counsel review" footer.
- Frontend locales live in `frontend/app/public/locales/{lang}/contracts.json` (13 files) under `leaseAgreement.*`.
- Conventional commits; end commit messages with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Contract with no unit residential details / null furnished** → kind falls back to `residential` (unfurnished), never an NPE. Test in Task 3.
2. **Language file missing for the contract's language** → falls back to the national language, then English, and the PDF says so. Test in Task 4.
3. **Reorder that tries to move a pinned clause or injects a duplicate/colliding sortOrder** → pinned clauses keep their positions; article numbers stay 1…n contiguous. Test in Task 5.
4. **Cross-reference to an excluded optional clause** → reference omitted, no `null`/`[[...]]` leaks into the PDF. Test in Task 5.
5. **A country/kind that has only legacy rows (e.g. NL commercial, every BE contract)** → still generates via the legacy path instead of erroring. Test in Tasks 2 and 4.

---

## Rulings after branch refresh (2026-10-03) — these OVERRIDE conflicting text below

The branch gained work after this plan was written (migrations V087–V090, backoffice template CRUD, resolver refactors). Where the task text below disagrees, these rulings win:

1. **Migration numbers:** highest existing is **V090**. Wherever the tasks say `V087` use **`V091`** (`V091__lease_kind_and_regime.sql`); wherever they say `V088` use **`V092`** (`V092__seed_nl_residential_lease_clauses.sql`). Task 0 must also change CLAUDE.md's "currently at V084" note to the real number after V092.
2. **V090's partial unique index** `uq_lease_clause_templates_active_country_key ON (country_code, clause_key) WHERE deleted_at IS NULL` would reject NL RESIDENTIAL rows next to the LEGACY NL rows with the same keys. V091 must `DROP INDEX uq_lease_clause_templates_active_country_key` and recreate it as `uq_lease_clause_templates_active_country_kind_key ON (country_code, lease_kind, clause_key) WHERE deleted_at IS NULL` (in addition to the table-level constraint swap already in V091).
3. **Audit columns:** `lease_clause_templates.created_by/updated_by` are now `NOT NULL` (V087) and `LeaseClauseTemplate` has `createdBy/updatedBy`. Every test builder and the V092 seed must set them; the seed uses `'00000000-0000-0000-0000-000000000001'` (Constants.SYSTEM_USER_ID). Test builders use `Constants.SYSTEM_USER_ID` or an existing helper — read how the existing tests do it.
4. **`LeaseClauseTemplateRepositoryIntegrationTest` already exists** (it uses `findByCountryCode`). Task 1 *extends* it instead of creating it. Keep `findByCountryCode(String)` (all kinds; the backoffice list and existing tests use it) and *add* `findByCountryAndKind`. Existing assertions that count NL seed rows (`migrationSeedsNlClauseTemplates`) must be adjusted to the LEGACY kind once V092 adds NL RESIDENTIAL rows — not deleted.
5. **Resolver/service shape:** the current code already has `LeaseClauseResolver.resolve(contract, locale, templates)` and `LeaseClauseService.updateClauses` fetches templates once (perf commit d5f3daa9). Keep that: add `LeaseClauseResolver.templatesFor(Contract, LeaseKind): List<LeaseClauseTemplate>` (kind lookup with LEGACY fallback, throws the existing `BusinessRuleException` when empty), keep the 3-arg overload as the core, and add `resolve(Contract, Locale, LeaseKind)` as `resolve(contract, locale, templatesFor(contract, kind))`. The service calls `templatesFor` once and reuses the list.
6. **Backoffice template CRUD exists** (`BackofficeLeaseClauseTemplateService/Controller`, `UpsertLeaseClauseTemplateRequest`, `LeaseClauseTemplateResponse`, `frontend/backoffice/src/pages/LeaseClauseTemplatesPage.tsx`, `useLeaseClauseTemplateHooks.ts`). New **Task 10** (below, executes right after Task 1) makes it kind- and pinned-aware so admins cannot create rows that silently collide or lose the kind.
7. **Workspace hygiene:** `docker/traefik/dynamic/local-dev.yml`, `keycloak/*.json` have unrelated uncommitted edits. Never `git add -A`/`git add .`; stage only files you changed.

### Task 10 (executes right after Task 1): Backoffice template CRUD becomes kind/pinned-aware

**Files:** Modify `UpsertLeaseClauseTemplateRequest.java`, `LeaseClauseTemplateResponse.java`, `BackofficeLeaseClauseTemplateService.java`, `BackofficeLeaseClauseTemplateController.java`, the backoffice OpenAPI source if the controller is generated from it (check `grep -rn "LeaseClauseTemplate" openapi/src`), `frontend/backoffice/src/pages/LeaseClauseTemplatesPage.tsx`, `frontend/backoffice/src/hooks/useLeaseClauseTemplateHooks.ts`; Test: `BackofficeLeaseClauseTemplateServiceTest`, the backoffice page test if one exists.

**Interfaces:** Consumes Task 1's `LeaseKind`, `LeaseClauseTemplate.leaseKind/pinned`. Produces: request gains `LeaseKind leaseKind` (nullable → `RESIDENTIAL`) and `boolean pinned`; response gains `LeaseKind leaseKind`, `boolean pinned`; `list(String countryCode, Optional<LeaseKind> kind)` filters when present, otherwise returns all kinds; creation passes `leaseKind`/`pinned` to the entity; `update` may change `pinned` but **not** `leaseKind` (changing kind would orphan contract overrides → `BadRequestException("Lease kind of an existing template cannot be changed")` when the request's non-null kind differs).

- [ ] **Step 1: Failing tests** — create with kind COMMERCIAL persists COMMERCIAL; omitted kind → RESIDENTIAL; update with a different kind → `BadRequestException`; list with kind filters via `findByCountryAndKind`, list without kind uses `findByCountryCode`; existing required-but-excluded rule still enforced.
- [ ] **Step 2:** run `mvn test -pl buurman-backoffice -am -Dtest=BackofficeLeaseClauseTemplateServiceTest` → FAIL. **Step 3:** implement; regenerate OpenAPI/clients if applicable (`make bundle-openapi`, `cd frontend && yarn generate:api`); backoffice page gets a kind select (create only) and a kind column. **Step 4:** run backend test + `cd frontend && yarn test` (backoffice workspace) + `yarn lint` → PASS. **Step 5: Commit** `feat(backoffice): make lease clause template CRUD kind- and pinned-aware`.

---

## File Structure

| File | Responsibility |
|---|---|
| `backend/buurman-jooq/.../db/migration/V087__lease_kind_and_regime.sql` | `lease_kind`, `pinned` on templates; `lease_regime` on contracts; relabel legacy rows; seed NL residential structure |
| `backend/buurman-common/.../domain/LeaseKind.java` | enum `LEGACY, RESIDENTIAL, RESIDENTIAL_FURNISHED, COMMERCIAL, MIXED_USE, AGRICULTURAL, SHORT_TERM, STUDENT_MOBILITY` + path segment |
| `backend/buurman-common/.../domain/LeaseRegime.java` | enum `STANDARD, SHORT_TERM, STUDENT_OR_MOBILITY` |
| `backend/buurman-core/.../service/LeaseKindResolver.java` | derives `LeaseKind` from property category, unit furnished flag, contract regime |
| `backend/buurman-core/.../service/LeaseClauseResolver.java` (modify) | kind-aware lookup with legacy fallback, pinned ordering, article numbers |
| `backend/buurman-letters/.../service/letters/LeaseDocumentLocator.java` | picks document path + language chain + authoritative flag |
| `backend/buurman-letters/.../service/letters/LeaseAgreementExporter.java` (modify) | new-model vs legacy rendering |
| `backend/buurman-letters/src/main/resources/templates/documents/lease-agreement/_shell.html` | shared layout/loop/signatures/disclaimer |
| `.../lease-agreement/NL/residential/{lang}.html` ×13 | NL residential clause fragments |
| `backend/buurman-letters/src/main/resources/messages/document-lease-agreement[_lang].properties` | add shell chrome + NL clause titles/summaries |
| Frontend: `ContractLeaseAgreementTab.tsx`, `contracts.json` ×13, contract form | reorder controls, regime select (hidden in Slice 0) |

---

### Task 0: Amend the spec to match what the code showed

**Files:** Modify `docs/superpowers/specs/2026-10-03-lease-agreement-documents-slice0-design.md`

Findings that change the spec: (a) `furnished` lives on `UnitResidentialDetails` (unit-level, via `UnitResidentialDetailsRepository.findByUnitIdAndTeamId`), not only `PropertyResidentialDetails`; (b) migrations are at V086; (c) keeping `title_i18n_key`/`body_i18n_key` (title + one-line summary for the toggle UI) is simpler than a new metadata mechanism — full text lives in documents; (d) legacy rows are relabelled `lease_kind='legacy'` and kept as a fallback instead of soft-deleted, so other countries and unsupported kinds keep working; (e) the availability endpoint is deferred — Slice 0 has no content for any non-STANDARD regime, so the regime select stays hidden.

- [ ] **Step 1:** Edit D2 ("title/body i18n keys stay: title + one-line summary for the toggle UI, full text only in documents"), D4 (drop the availability endpoint; regime select present in code but rendered only when a later slice enables a regime), and add the V087 / legacy-fallback notes.
- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-10-03-lease-agreement-documents-slice0-design.md
git commit -m "docs: amend lease documents slice 0 spec after code review"
```

---

### Task 1: Migration V087 + enums + domain/repository plumbing

**Files:**
- Create: `backend/buurman-jooq/src/main/resources/db/migration/V087__lease_kind_and_regime.sql`
- Create: `backend/buurman-common/src/main/java/com/buurman/domain/LeaseKind.java`, `.../LeaseRegime.java`
- Modify: `backend/buurman-common/.../domain/LeaseClauseTemplate.java`, `.../domain/Contract.java`
- Modify: `backend/buurman-core/.../repository/LeaseClauseTemplateRepository.java`, `.../repository/ContractRepository.java`, `.../mapper/LeaseClauseTemplateRecordMapper.java`
- Test: `backend/buurman-core/src/test/java/com/buurman/repository/LeaseClauseTemplateRepositoryIntegrationTest.java` (create), extend `ContractRepositoryIntegrationTest`

**Interfaces:**
- Produces: `enum LeaseKind { LEGACY, RESIDENTIAL, RESIDENTIAL_FURNISHED, COMMERCIAL, MIXED_USE, AGRICULTURAL, SHORT_TERM, STUDENT_MOBILITY; String pathSegment() }` where `pathSegment()` is the lowercase-hyphen form (`residential-furnished`); `enum LeaseRegime { STANDARD, SHORT_TERM, STUDENT_OR_MOBILITY }`; `LeaseClauseTemplate.leaseKind: LeaseKind`, `.pinned: boolean`; `Contract.leaseRegime: LeaseRegime` (builder default `STANDARD`); `LeaseClauseTemplateRepository.findByCountryAndKind(String countryCode, LeaseKind kind): List<LeaseClauseTemplate>`.

- [ ] **Step 1: Write the failing repository test** (global templates, kind filtering, soft-delete respected):

```java
@DisplayName("LeaseClauseTemplateRepository")
class LeaseClauseTemplateRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {
  private LeaseClauseTemplateRepository repository;

  @BeforeEach
  void setUp() {
    repository = new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);
  }

  @Test
  @DisplayName("findByCountryAndKind returns only that kind, ordered by sort_order")
  void filtersByKind() {
    repository.save(template("NL", LeaseKind.RESIDENTIAL, "zzz-test-b", 2));
    repository.save(template("NL", LeaseKind.RESIDENTIAL, "zzz-test-a", 1));
    repository.save(template("NL", LeaseKind.COMMERCIAL, "zzz-test-c", 1));

    assertThat(repository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .extracting(LeaseClauseTemplate::getClauseKey)
        .containsSubsequence("zzz-test-a", "zzz-test-b")
        .doesNotContain("zzz-test-c");
  }

  @Test
  @DisplayName("legacy placeholder rows are labelled LEGACY by V087")
  void legacyRowsRelabelled() {
    assertThat(repository.findByCountryAndKind("DE", LeaseKind.LEGACY)).hasSize(7);
    assertThat(repository.findByCountryAndKind("DE", LeaseKind.RESIDENTIAL)).isEmpty();
  }

  private static LeaseClauseTemplate template(String cc, LeaseKind kind, String key, int order) {
    return LeaseClauseTemplate.builder().countryCode(cc).leaseKind(kind).clauseKey(key)
        .titleI18nKey("lease.x.title").bodyI18nKey("lease.x.body")
        .defaultIncluded(true).optional(true).pinned(false).sortOrder(order).version(1).build();
  }
}
```

Add to `ContractRepositoryIntegrationTest`: save a contract, reload, assert `getLeaseRegime() == LeaseRegime.STANDARD`; save with `SHORT_TERM`, reload, assert it round-trips; assert team B cannot load team A's contract (existing helper pattern).

- [ ] **Step 2: Run to verify failure** — `mvn clean install -DskipTests -pl buurman-core -am && mvn test -pl buurman-core -am -Dtest=LeaseClauseTemplateRepositoryIntegrationTest` → compile FAIL (`LeaseKind` missing).
- [ ] **Step 3: Write V087**

```sql
ALTER TABLE lease_clause_templates
ADD COLUMN lease_kind VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing V083 rows are placeholder text: keep them as the fallback for every country/kind
-- that has no real document yet.
UPDATE lease_clause_templates SET lease_kind = 'LEGACY';

ALTER TABLE lease_clause_templates
ADD CONSTRAINT chk_lease_clause_templates_kind CHECK (
    lease_kind IN ('LEGACY','RESIDENTIAL','RESIDENTIAL_FURNISHED','COMMERCIAL','MIXED_USE',
                   'AGRICULTURAL','SHORT_TERM','STUDENT_MOBILITY'));

ALTER TABLE lease_clause_templates DROP CONSTRAINT uq_lease_clause_templates_country_key_version;
ALTER TABLE lease_clause_templates
ADD CONSTRAINT uq_lease_clause_templates_country_kind_key_version
UNIQUE (country_code, lease_kind, clause_key, version);

ALTER TABLE contracts
ADD COLUMN lease_regime VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
ADD CONSTRAINT chk_contracts_lease_regime CHECK (lease_regime IN ('STANDARD','SHORT_TERM','STUDENT_OR_MOBILITY'));
```

NL residential structure rows are inserted in Task 6 (needs the clause list). Pinned rows must have a lower `sort_order` than every non-pinned row of the same (country, kind) — enforced by a test in Task 6.

- [ ] **Step 4: Implement** enums, add the fields to the two domain classes (`@Builder.Default private LeaseRegime leaseRegime = LeaseRegime.STANDARD;`, `@Builder.Default private LeaseKind leaseKind = LeaseKind.RESIDENTIAL;`, `private boolean pinned;`), regenerate JOOQ (`cd backend && mvn generate-sources -pl buurman-jooq -am`), map columns in `LeaseClauseTemplateRecordMapper` and the repository insert/update (`LEASE_KIND`, `PINNED`), add `findByCountryAndKind`, keep `findByCountryCode` only if still referenced (Task 2 removes the last use; delete it then), and add `CONTRACTS.LEASE_REGIME` to `ContractRepository` insert (line ~312) and update (line ~370) plus the record→domain mapping.
- [ ] **Step 5: Run** the two test classes → PASS.
- [ ] **Step 6: Commit** `feat(lease): add lease kind, pinned clauses and contract lease regime (V087)`.

---

### Task 2: LeaseKindResolver + kind-aware clause resolver with legacy fallback

**Files:**
- Create: `backend/buurman-core/src/main/java/com/buurman/service/LeaseKindResolver.java`
- Modify: `backend/buurman-core/src/main/java/com/buurman/service/LeaseClauseResolver.java`, `LeaseClauseService.java`
- Modify: `backend/buurman-common/.../dto/response/ResolvedLeaseClauseResponse.java` (add `boolean pinned`, `int articleNumber`)
- Test: `LeaseKindResolverTest`, update `LeaseClauseResolverTest` (find with `grep -rl LeaseClauseResolver backend/*/src/test`), `LeaseClauseServiceTest`

**Interfaces:**
- Consumes: Task 1 types.
- Produces: `LeaseKind LeaseKindResolver.resolve(Contract contract, Property property, Optional<UnitResidentialDetails> unitDetails)`; `LeaseClauseResolver.resolve(Contract, Locale): List<ResolvedLeaseClauseResponse>` now returns clauses in final render order with `articleNumber` 1…n **counting only included clauses** (excluded ones get `articleNumber = 0`); `ResolvedLeaseClauseResponse(Sid templateIdentifier, String clauseKey, String title, String body, boolean included, boolean optional, int sortOrder, boolean pinned, int articleNumber)`.

Resolution rules (all tested):
1. Kind: `regime = SHORT_TERM → SHORT_TERM`; `STUDENT_OR_MOBILITY → STUDENT_MOBILITY`; else by `property.getPropertyCategory()`: `RESIDENTIAL → furnished ? RESIDENTIAL_FURNISHED : RESIDENTIAL` (furnished = `unitDetails.map(UnitResidentialDetails::isFurnished).orElse(false)`), `COMMERCIAL|INDUSTRIAL → COMMERCIAL`, `MIXED_USE → MIXED_USE`, `AGRICULTURAL → AGRICULTURAL`. A `null` category → `RESIDENTIAL`.
2. Templates: `findByCountryAndKind(country, kind)`; if empty → `findByCountryAndKind(country, LEGACY)`; if still empty → existing `BusinessRuleException`.
3. Order: pinned first by template `sort_order` (override ignored), then the rest by effective `sortOrder` (override or template), ties broken by template `sort_order`.
4. `LeaseClauseService.updateClauses` looks templates up with the same kind/legacy rule (extract a package-private `templatesFor(Contract, LeaseKind)` in the resolver and call it from both). A pinned clause's `sortOrder` in the request is simply ignored (rule 3), so no rejection is needed; a test asserts the ignore.

- [ ] **Step 1: Failing tests.** `LeaseKindResolverTest` (parameterized): RESIDENTIAL+furnished→`RESIDENTIAL_FURNISHED`; RESIDENTIAL+empty unit details→`RESIDENTIAL`; INDUSTRIAL→`COMMERCIAL`; MIXED_USE; AGRICULTURAL; regime SHORT_TERM beats category; null category→`RESIDENTIAL`. Resolver tests: legacy fallback returns the 7 legacy clauses with contiguous article numbers; pinned clause ignores a conflicting override sortOrder; excluded optional clause gets `articleNumber 0` and later articles renumber; required clause forced-included (existing behaviour preserved).

```java
@Test
@DisplayName("pinned clause keeps its position even when an override tries to move it")
void pinnedIgnoresOverride() {
  var parties = template("parties", 1, false, true);
  var rent = template("rent", 2, false, false);
  when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL)).thenReturn(List.of(parties, rent));
  when(overrideRepository.findByContractIdAndTeamId(any(), any()))
      .thenReturn(List.of(override(parties, true, 99)));
  assertThat(resolver.resolve(contract(), Locale.ENGLISH))
      .extracting(ResolvedLeaseClauseResponse::clauseKey).containsExactly("parties", "rent");
}
```

- [ ] **Step 2:** run → FAIL. **Step 3:** implement per rules above (kind resolution is fetched by the exporter/service; the resolver receives `LeaseKind` through a new overload `resolve(Contract, Locale, LeaseKind)`, and the old two-arg method is removed — update callers: `LeaseClauseService`, `LeaseAgreementExporter`). `LeaseClauseService` loads `UnitResidentialDetailsRepository.findByUnitIdAndTeamId(contract.getUnitId(), teamId)` and the property via `PropertyRepository.getByIdAndTeamId` to call `LeaseKindResolver`. **Step 4:** run → PASS. **Step 5: Commit** `feat(lease): resolve lease kind from property and order clauses with pinned positions`.

---

### Task 3: API surface — contract `leaseRegime`, clause `pinned`/`articleNumber`

**Files:** Modify `openapi/src/app.yaml` (ContractResponse ~4125, CreateContractRequest ~3702, UpdateContractRequest ~6255, `ResolvedLeaseClauseResponse` 3209), `CreateContractRequest.java`, `UpdateContractRequest.java`, `ContractResponse.java`, `ContractMapper.java`, `frontend/app/src/types/contract.ts`; Test: `ContractMapperTest` (find with `grep -rl ContractMapper backend/*/src/test`).

**Interfaces:** Produces `leaseRegime` (enum `STANDARD|SHORT_TERM|STUDENT_OR_MOBILITY`, default `STANDARD`) on contract create/update/response; `pinned: boolean`, `articleNumber: integer` on `ResolvedLeaseClauseResponse` (both required).

- [ ] **Step 1: Failing test** — mapper: create without `leaseRegime` → `STANDARD`; update with `Optional.of(SHORT_TERM)` changes it; update with empty keeps the existing value (mirror the `documentLanguages` expressions in `ContractMapper` lines 32 and 71).
- [ ] **Step 2:** run → FAIL. **Step 3:** edit OpenAPI source (response `required` list gets `leaseRegime`), DTO records (`Optional<LeaseRegime> leaseRegime` in requests, `LeaseRegime leaseRegime` in response), mapper expressions, then `make bundle-openapi` and `cd frontend && yarn generate:api`. **Step 4:** run backend tests for `buurman-core` + `buurman-letters` (`make test` is slow; use `-pl buurman-core -am`) → PASS; `cd frontend && yarn tsc --noEmit` → PASS. **Step 5: Commit** `feat(api): expose lease regime on contracts and pinned/article number on clauses` (include regenerated `openapi/app.yaml`).

---

### Task 4: LeaseDocumentLocator + shared shell + exporter dual path

**Files:**
- Create: `backend/buurman-letters/.../service/letters/LeaseDocumentLocator.java`, `.../resources/templates/documents/lease-agreement/_shell.html`
- Modify: `LeaseAgreementExporter.java`, `LetterTemplateService.java` (add `renderToPdfTemplate(String templateName, Locale, Map)` that renders an explicit template name instead of `documentType + "/generic"`), `LeaseAgreementGenerationService.java` (filename carries the language actually used), bundles (shell chrome keys)
- Test: `LeaseDocumentLocatorTest`, update `LeaseAgreementExporterTest`, new `LeaseAgreementRenderTest` (real Thymeleaf engine, no PDF)

**Interfaces:**
- Produces: `record LeaseDocument(String templatePath, String languageUsed, boolean authoritative)`; `Optional<LeaseDocument> LeaseDocumentLocator.locate(String countryCode, LeaseKind kind, String requestedLang)` — chain: requested → each national language of the country (table in Global Constraints) → `en`; first existing classpath resource `templates/documents/lease-agreement/{CC}/{kind.pathSegment()}/{lang}.html` wins; `authoritative = nationalLanguages.contains(languageUsed)`; empty when no file at all exists. `templatePath` is the template name relative to `templates/documents/` **without** `.html` (e.g. `lease-agreement/NL/residential/nl`).
- Exporter contract: when `locate(...)` is present → render `lease-agreement/_shell` with variables `clauseSource` (= `templatePath`), `clauses` (ordered included `ResolvedLeaseClauseResponse`s), `refs` (`Map<String,Integer>` clauseKey→articleNumber for included clauses only), `authoritative`, `languageUsed`, `requestedLang`, `fallbackUsed` (`!languageUsed.equals(requestedLang)`), plus all existing variables and the typed values in Step 3. Otherwise render the legacy `generic` template exactly as today.

- [ ] **Step 1: Failing tests.**

```java
@Test @DisplayName("falls back requested -> national -> English, and flags non-authoritative")
void fallbackChain() {
  // fixtures on the test classpath: lease-agreement/NL/residential/{nl,en}.html
  assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "fr").orElseThrow().languageUsed()).isEqualTo("nl");
  assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "en").orElseThrow().authoritative()).isFalse();
  assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "nl").orElseThrow().authoritative()).isTrue();
  assertThat(locator.locate("BE", LeaseKind.RESIDENTIAL, "nl")).isEmpty();
}
```

`LeaseAgreementRenderTest` builds `DocumentTemplateSupport.templateEngine(...)` with a test fixture `lease-agreement/ZZ/residential/en.html` (fragments `clause-a`, `clause-b` where `clause-b` contains `<span th:if="${refs['a']}" th:text="#{lease.article.ref(${refs['a']})}"/>`) and asserts: shuffled order renders "1." then "2." headings in that order; cross-reference shows the right article number; excluding clause `a` omits the reference and leaves no `null`, `${`, `#{` or `[[` in the HTML; non-authoritative adds the courtesy-translation notice; the draft disclaimer is always present. Exporter test: legacy path used when `locate` is empty (existing assertions stay green), new path used when present, and `BusinessRuleException` when all clauses excluded still thrown before rendering.

- [ ] **Step 2:** run → FAIL. **Step 3: Implement.** Shell skeleton (shares existing chrome fragments):

```html
<div th:each="clause : ${clauses}" class="clause">
  <h2 class="clause-title"><span th:text="${clause.articleNumber}"></span>. <span th:text="${clause.title}"></span></h2>
  <div th:insert="~{${clauseSource} :: ${'clause-' + clause.clauseKey}}"></div>
</div>
```

plus the header/addressee/subject/signature blocks copied from `generic.html`, `th:if="${!authoritative}"` courtesy notice (`#{lease.notice.courtesy}`), `th:if="${fallbackUsed}"` fallback notice, and the always-on `#{lease.disclaimer}`. Typed template values the clause fragments may use (add to `buildTemplateVariables`, formatted with `CurrencyUtils.formatCurrency` / `LetterExporterHelper.letterDateFormatter`): `landlordName`, `tenantNames`, `startDate`, `endDate` (nullable), `contractTypeLabel`, `rentAmount`, `depositAmount` (nullable), `paymentDueDay` (nullable), `paymentFrequency`, `landlordNoticeDays`, `tenantNoticeDays`, `countryMetadata` (the `ContractCountryMetadata` record or `null`). Rename old placeholder `lease.disclaimer` copy in all 13 bundles to the localized "draft — have it reviewed by qualified counsel" text, keeping parity (`I18nBundleParityTest` must pass), and add `lease.article.ref`, `lease.notice.courtesy`, `lease.notice.fallback` in all 13.
- [ ] **Step 4:** run `mvn test -pl buurman-letters -am` and `-pl buurman-app -am -Dtest=I18nBundleParityTest` → PASS. **Step 5: Commit** `feat(lease): render lease agreements from per-language clause documents with legacy fallback`.

---

### Task 5: Frontend — reorder controls and hidden regime select

**Files:** Modify `ContractLeaseAgreementTab.tsx`, `frontend/app/public/locales/{13 langs}/contracts.json`, `ContractLeaseAgreementTab.test.tsx`; contract form component (find with `grep -rl documentLanguages frontend/app/src/components`) for the regime select.

- [ ] **Step 1: Failing tests** (extend the existing tab test): (a) up/down buttons exist for movable clauses and are `disabled` for `pinned` ones; (b) clicking "move down" on the first movable clause then Save sends the swapped `sortOrder` values for those two only, pinned clauses keep theirs; (c) first movable clause's up button and last clause's down button are disabled; (d) article numbers shown use `articleNumber` for included clauses and none for excluded ones.
- [ ] **Step 2:** `cd frontend && yarn vitest run app/src/components/contracts/__tests__/ContractLeaseAgreementTab.test.tsx` → FAIL.
- [ ] **Step 3: Implement** a local `order` state (array of `templateIdentifier`), initialised from server order, swapping adjacent non-pinned entries; Save maps entries to `sortOrder = index + 1`; pinned clauses are sent with their server `sortOrder`. Buttons use lucide `ChevronUp`/`ChevronDown` with `aria-label` from `t('leaseAgreement.moveUp'|'moveDown')`. Add those two keys plus `leaseAgreement.pinned` and `leaseRegime.*` labels to all 13 `contracts.json` (English text first, then native translation — files already exist, keep key parity with `en`). Regime select: render only when `availableRegimes.length > 1`, where `availableRegimes` is a constant `['STANDARD']` in Slice 0 (comment: later slices append regimes as content lands).
- [ ] **Step 4:** `yarn test`, `yarn lint`, `yarn build` → PASS. **Step 5: Commit** `feat(lease): clause reordering controls and lease regime plumbing in the UI`.

---

### Task 6: NL residential structure, shell wiring and English-complete reference document

**Files:** Modify `V087__lease_kind_and_regime.sql` is **not** edited again if already committed — instead create `V088__seed_nl_residential_lease_clauses.sql`; create `.../lease-agreement/NL/residential/nl.html`; add bundle keys `lease.nl.residential.<clause>.title|summary` to `document-lease-agreement*.properties`; Test: `LeaseDocumentCatalogTest` (in `buurman-letters`).

**Clause set** (key — required? — pinned? — sort): `parties` req pinned 1; `premises` req pinned 2; `term` req 3; `rent` req 4; `rent-adjustment` opt 5; `service-costs` opt 6; `deposit` opt 7; `payment` req 8; `use` opt 9; `subletting` opt 10; `maintenance` opt 11; `energy-label` req 12; `handover-inspection` opt 13; `termination` req 14; `data-protection` opt 15; `disputes` opt 16. (Signatures are in the shell, not a clause.)

- [ ] **Step 1: Failing catalog test.** For every directory `lease-agreement/{CC}/{kind}` found on the classpath: (a) every language in `DocumentLanguages.ORDERED` has a file; (b) the set of `th:fragment="clause-…"` names is identical across all languages; (c) every fragment key exists as a non-deleted row in `lease_clause_templates` for (CC, kind) — implemented as a unit-level check against a hard-coded expected key list in the test for Slice 0 plus a Testcontainers check in `AbstractRepositoryIntegrationTest`'s style for the DB side; (d) each file starts with the three header-comment markers; (e) every pinned template's `sort_order` is lower than every non-pinned one of the same (country, kind); (f) every required clause's bundle title and summary keys exist in all 13 bundles.
- [ ] **Step 2:** run → FAIL (no NL files).
- [ ] **Step 3: V088** inserts the 16 rows (`lease_kind='RESIDENTIAL'`, `country_code='NL'`, `version=1`, `title_i18n_key='lease.nl.residential.<key>.title'`, `body_i18n_key='lease.nl.residential.<key>.summary'`, `default_included=TRUE`, `optional` / `pinned` / `sort_order` per the table, identifiers `LCTNLRES` + zero-padded index to 29 chars, matching the V083 identifier style).
- [ ] **Step 4: Write `nl.html`** (authoritative). Header comment:

```html
<!--
  legal-basis: BW Boek 7 titel 4 (huur woonruimte); Wet betaalbare huur; Wet vaste huurcontracten;
               Besluit energieprestatie gebouwen; AVG — each article verified against
               wetten.overheid.nl on the implementation date (record the date here)
  reviewed-by: none
  translation: authoritative
-->
```

Each clause is a fragment, e.g. the style every other file must follow:

```html
<div th:fragment="clause-deposit" xmlns:th="http://www.thymeleaf.org">
  <p th:if="${depositAmount != null}">
    De huurder betaalt bij aanvang van de huur een waarborgsom van
    <strong th:text="${depositAmount}"></strong>. De waarborgsom bedraagt ten hoogste twee
    maanden basishuur en wordt binnen de wettelijke termijn na afloop van de huur en oplevering
    van het gehuurde terugbetaald, onder aftrek van wat de huurder ter zake van de huur
    verschuldigd is
    <span th:if="${refs['handover-inspection']}">(zie artikel
      <span th:text="${refs['handover-inspection']}"></span>)</span>.
  </p>
</div>
```

All numeric/statutory claims (deposit cap, repayment term, notice periods, indexation limits) must be checked against the primary source and, where the rent-regulation catalog has the value (`RentRegulationCountry` NL), must reference the catalog value instead of hard-coding it. The NL file contains every key in the clause table; `${...}` only uses variables defined in Task 4.
- [ ] **Step 5:** add NL title/summary bundle keys for all 16 clauses in the base bundle (English) and `_nl`; other 11 language bundles get translated titles/summaries in Task 7.
- [ ] **Step 6:** run the catalog test with only `nl.html` present → expected FAIL only on (a) language coverage; commit nothing yet — continue to Task 7 in the same working tree. (Tasks 6 and 7 form one atomic commit because the catalog test requires all 13 languages.)

---

### Task 7: Translate NL residential into the 12 other languages

**Files:** Create `.../lease-agreement/NL/residential/{en,de,fr,pt,es,sv,it,fi,el,pl,da,nb}.html`; modify the 12 non-`nl` `document-lease-agreement_*.properties` (NL clause titles/summaries).

Rules for every translation (enforced by the catalog test where mechanical, by review otherwise):
1. Same fragment names and same `th:` expressions as `nl.html` — only prose changes. Dutch legal terms keep the Dutch term in parentheses on first use (e.g. "security deposit (waarborgsom)", "Kaution (waarborgsom)") so the document stays tied to the authoritative text.
2. Header comment with `translation: machine-drafted` and `reviewed-by: none`.
3. No statutory number or article reference may differ from `nl.html`.
4. One task-sized batch each for: `en`; `de fr`; `es pt it`; `sv da nb fi`; `el pl`. Each batch ends by running the catalog test; the last batch must turn it fully green.

- [ ] **Step 1:** create `en.html` → run catalog test (language coverage still failing for the rest). **Step 2–5:** remaining batches as above.
- [ ] **Step 6: Render smoke test** (`LeaseNlRenderSmokeTest`): for each of the 13 languages render the shell with a fixture contract (all clauses included, then only required clauses) and assert no `${`, `#{`, `[[`, `null`, or unresolved placeholders in the HTML, `<html lang>`-free text length > 2,000 chars with all clauses, and that every language's HTML contains its own `depositAmount` formatted string.
- [ ] **Step 7:** `make test` (full backend) and `cd frontend && yarn test` → PASS.
- [ ] **Step 8: Commit** `feat(lease): NL residential lease agreement in 13 languages`.

---

### Task 8: End-to-end verification and cleanup

- [ ] **Step 1:** `make dev`, run backend + frontend, create an NL residential contract (unit unfurnished) with `documentLanguages: ['de']`; open the Lease Agreement tab, reorder two optional clauses, toggle one, generate; open the PDF from Documents. Verify: German text, articles renumbered, courtesy-translation notice, draft disclaimer, no placeholder leakage.
- [ ] **Step 2:** repeat for a BE contract → legacy placeholder PDF still generates (regression guard).
- [ ] **Step 3:** repeat for an NL contract on a COMMERCIAL property → legacy path, no error.
- [ ] **Step 4:** update `CLAUDE.md` migration note ("currently at V088"), and the backoffice banner text only if the NL rows are shown there (check `grep -rn "placeholder" frontend/backoffice/src | grep -i lease`).
- [ ] **Step 5: Commit** `docs: note lease document model and migration version`.

---

## Self-Review

- **Spec coverage:** D1 document selection → Tasks 2, 4; D2 clauses-in-document, reorder, pinning, auto-numbering, cross-refs → Tasks 1, 2, 4, 5; D3 variables → Task 4; D4 regime field → Tasks 1, 3, 5 (availability endpoint deferred, Task 0); D5 NL reference → Tasks 6–7; D6 tests (parity, render, reorder, fallback, migration/multi-tenant) → Tasks 1, 2, 4, 6, 7; Legal content markers → Global Constraints + Tasks 4, 6, 7.
- **Placeholder scan:** legal prose per clause is authored during Tasks 6–7 against primary sources; the plan fixes the clause set, fragment contract, variables, header markers and the mechanical gates that make an omission fail CI.
- **Type consistency:** `LeaseKind`, `LeaseRegime`, `findByCountryAndKind`, `resolve(Contract, Locale, LeaseKind)`, `ResolvedLeaseClauseResponse(... pinned, articleNumber)`, `LeaseDocument`, `LeaseDocumentLocator.locate`, `refs`, `clauseSource` are used identically in every task.
