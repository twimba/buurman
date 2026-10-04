# Per-Country Addenda/Letters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a rent-increase letter for a DE contract render a structured, two-section §558-style legal justification, and an NL contract cite its legal basis with a heading — while every other country/document-type combination renders byte-for-byte identically to today.

**Architecture:** `LetterExporterHelper.legalVariables()` gains a `documentType` parameter (distinct from the existing `keyPrefix`, because `keyPrefix` is already shared between two exporters today) and returns a `legalClauses` list instead of a single `legalClause` string. A new static `CountryLetterClauseCatalog` maps `(documentType, countryCode)` to an ordered list of title/body message-key pairs; when it has no entry, the helper falls back to exactly today's single-key resolution wrapped as a one-item list. All 5 `generic.html` templates switch their single `th:if` block to a `th:each` loop over the same list shape, so the fallback path renders identically to today regardless of which template it's in.

**Tech Stack:** Java 25 / Spring Boot 4 (Thymeleaf + `MessageSource`), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-30-per-country-addenda-design.md`

## Corrections to the spec, found while grounding this plan

The spec assumed each document type has its own distinct `keyPrefix` and its own properties bundle. Reality, confirmed by reading the code:

- `RentIncreaseLetterExporter` and `ContractExtensionAddendumExporter` **both** call `helper.legalVariables(messageSource, "legal.", contract, locale)` — the identical literal prefix, resolving the identical message key (`legal.NL`, `legal.DE`) from the identical bundle (`document-extension.properties`, confirmed — that's where `legal.NL`/`legal.DE` are actually defined today, not `document-rent-change.properties` as the spec guessed). Today these two document types show the **same** legal clause text for the same country.
- `DepositStatementExporter` uses `"deposit.legal."`, `RentChangeDocumentExporter` uses `"rentchange.legal."`, `PaymentFormalNoticeExporter` uses `"notice.legal."` — each genuinely distinct.
- Because `keyPrefix` can't discriminate rent-increase-letter from extension-addendum, `legalVariables()` needs a **second**, new parameter carrying the document-type literal already passed to `documentTemplateService.renderToPdf(...)` at each call site (`"rent-increase-letter"`, `"extension-addendum"`, `"rent-change"`, `PaymentFormalNoticeExporter.DOCUMENT_TYPE` = `"payment-formal-notice"`, `DepositStatementExporter.DOCUMENT_TYPE` = `"deposit-statement"`). This is what actually keys the new catalog lookup — `keyPrefix` stays exactly as-is, still driving the fallback single-key resolution per call site, unchanged.
- New seed keys therefore belong in `document-extension.properties` (+ its 12 translation siblings), not `document-rent-change.properties`.
- `LetterExporterHelper` is package-private (`class LetterExporterHelper`, no `public`), confirmed — no visibility change needed for anything in this plan.
- `letterMessageSource` is one merged `ReloadableResourceBundleMessageSource` over 5 basenames (`document-letter-chrome`, `document-extension`, `document-rent-change`, `document-payment-notice`, `document-deposit-statement`) — a key resolves the same regardless of which basename file physically holds it, but new keys are still added to `document-extension.properties` to stay co-located with the sibling `legal.NL`/`legal.DE` keys they extend.
- No dedicated exporter test classes exist today. The directly-reusable precedent for a template-render test is `TenantLetterLocaleRenderTest` (`backend/buurman-letters/src/test/java/com/buurman/service/letters/TenantLetterLocaleRenderTest.java`): it builds a raw `SpringTemplateEngine` + `ReloadableResourceBundleMessageSource` (no exporter, no Spring context, no repositories) and renders a template against a hand-built variables map, asserting on the output string. It currently covers `payment-formal-notice`/`deposit-statement` only, with `v.put("legalClause", "Clause text")` in its fixture — **this fixture and its assertions must be updated in this plan**, since after this change the variable is `legalClauses` (a list), not `legalClause` (a string). This is a required maintenance step, not optional cleanup.
- `extension-addendum/generic.html`'s clause block already wraps in a `<strong>Legal References</strong>` heading (`th:if="${legalClause != null}"` → inner `<span th:text="${legalClause}">`) — the other four templates are a plain single `<div th:text="${legalClause}">`. The `th:each` conversion must preserve extension-addendum's existing wrapper, not flatten it to match the other four.

## Global Constraints

- Idiomatic `Optional` API only — never `if (x != null)` / unchecked `.get()` (project rule).
- All `if`/`for`/`while` bodies use curly braces (project rule; N/A in Thymeleaf templates, applies to the Java touched here).
- `CountryLetterClauseCatalog` is a static config class, not a DB table — no migration in this plan.
- Every document/country combination NOT explicitly seeded in Task 4 must render byte-for-byte identical visible text to before this plan, in every one of the 13 supported locales — this is the plan's core correctness property, verified explicitly in Task 3.

## Review Focus

- **A country with no catalog entry for its document type** (e.g. FR/rent-increase-letter, or every country for deposit-statement) must fall back to exactly today's single-clause rendering, not an empty/broken block. (Task 2 — `CountryLetterClauseCatalogTest` + `LetterExporterHelperTest`.)
- **`extension-addendum`'s existing "Legal References" heading wrapper** must survive the `th:each` conversion unchanged in the no-new-content case (this document type gets no new seeded content — see Task 4 — but its template still changes shape in Task 3). (Task 3.)
- **A contract with no `countryCode` at all** (`Optional<String>` empty) must still render without a NullPointerException — `resolveLegalClause` already handles this via `countryCode.flatMap(...)`; the catalog lookup path must handle it the same way, not assume a country code is always present. (Task 2.)
- **The existing `TenantLetterLocaleRenderTest` must still pass after its fixture is updated** — proves the fallback path is genuinely unbroken for the two document types that test already covers, in all 13 locales, not just English. (Task 3.)
- **DE's two clauses render in the declared order** (§558 section before comparison-method section) — order is meaningful in a legal document; a `Map`-based catalog lookup that doesn't preserve insertion order would silently reorder them. (Task 1 — `CountryLetterClauseCatalog` must use `List`, not rely on `Map` iteration order for the clause sequence itself, which it already doesn't per the spec's `List<LetterClauseKey>` value type — verified explicitly in Task 1's test.)

---

## Task 1: `CountryLetterClauseCatalog`

**Files:**
- Create: `backend/buurman-letters/src/main/java/com/buurman/service/letters/LetterClauseKey.java`
- Create: `backend/buurman-letters/src/main/java/com/buurman/service/letters/CountryLetterClauseCatalog.java`
- Test: `backend/buurman-letters/src/test/java/com/buurman/service/letters/CountryLetterClauseCatalogTest.java`

**Interfaces:**
- Produces: `LetterClauseKey(String titleKey, String bodyKey)`; `CountryLetterClauseCatalog.resolve(String documentType, String countryCode): List<LetterClauseKey>` — Task 2's `LetterExporterHelper.legalVariables` calls this directly.

- [ ] **Step 1: Write the failing test**

```java
package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CountryLetterClauseCatalog")
class CountryLetterClauseCatalogTest {

  @Test
  @DisplayName("DE/rent-increase-letter resolves the §558 section before the comparison-method section, in order")
  void deRentIncreaseLetterResolvesTwoOrderedClauses() {
    List<LetterClauseKey> clauses = CountryLetterClauseCatalog.resolve("rent-increase-letter", "DE");

    assertThat(clauses)
        .containsExactly(
            new LetterClauseKey("legal.DE.section558.title", "legal.DE.section558.body"),
            new LetterClauseKey(
                "legal.DE.comparisonMethod.title", "legal.DE.comparisonMethod.body"));
  }

  @Test
  @DisplayName("NL/rent-increase-letter resolves exactly one clause")
  void nlRentIncreaseLetterResolvesOneClause() {
    List<LetterClauseKey> clauses = CountryLetterClauseCatalog.resolve("rent-increase-letter", "NL");

    assertThat(clauses)
        .containsExactly(new LetterClauseKey("legal.NL.basis.title", "legal.NL.basis.body"));
  }

  @Test
  @DisplayName("an unconfigured document type/country combination resolves to no entries")
  void unconfiguredCombinationResolvesEmpty() {
    assertThat(CountryLetterClauseCatalog.resolve("rent-increase-letter", "FR")).isEmpty();
    assertThat(CountryLetterClauseCatalog.resolve("extension-addendum", "DE")).isEmpty();
    assertThat(CountryLetterClauseCatalog.resolve("deposit-statement", "DE")).isEmpty();
  }

  @Test
  @DisplayName("case is normalized: a lowercase country code still resolves")
  void countryCodeIsCaseNormalized() {
    assertThat(CountryLetterClauseCatalog.resolve("rent-increase-letter", "de")).hasSize(2);
  }
}
```

- [ ] **Step 2: Run it, verify it fails to compile**

Run: `cd backend && mvn test -pl buurman-letters -am -Dtest=CountryLetterClauseCatalogTest`
Expected: compile error — `CountryLetterClauseCatalog`/`LetterClauseKey` don't exist yet.

- [ ] **Step 3: Write `LetterClauseKey`**

```java
package com.buurman.service.letters;

/** One ordered legal-clause section within a letter: a heading and its body, both message keys. */
record LetterClauseKey(String titleKey, String bodyKey) {}
```

Package-private record, matching `LetterExporterHelper`'s own package-private visibility — nothing outside `com.buurman.service.letters` needs this type.

- [ ] **Step 4: Write `CountryLetterClauseCatalog`**

```java
package com.buurman.service.letters;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structural country/document-type variation for letter legal clauses. A combination with no
 * entry here falls back to the single legacy {@code legal.{COUNTRY}}-style clause resolution in
 * {@link LetterExporterHelper#resolveLegalClause}, so adding a country/document-type here is
 * purely additive — nothing existing changes shape unless explicitly listed.
 *
 * <p>A static config class, not a database table: this changes with template structure (the
 * {@code th:each} loop in {@code generic.html} already requires a deploy to change), so letting
 * it be edited without a deploy would let content and structure drift out of sync.
 */
final class CountryLetterClauseCatalog {

  private static final Map<String, Map<String, List<LetterClauseKey>>> CLAUSES =
      Map.of(
          "rent-increase-letter",
          Map.of(
              "NL",
              List.of(new LetterClauseKey("legal.NL.basis.title", "legal.NL.basis.body")),
              "DE",
              List.of(
                  new LetterClauseKey("legal.DE.section558.title", "legal.DE.section558.body"),
                  new LetterClauseKey(
                      "legal.DE.comparisonMethod.title", "legal.DE.comparisonMethod.body"))));

  private CountryLetterClauseCatalog() {}

  /** Ordered clause keys for the given document type and country, or empty if not configured. */
  static List<LetterClauseKey> resolve(String documentType, String countryCode) {
    return CLAUSES
        .getOrDefault(documentType, Map.of())
        .getOrDefault(countryCode.toUpperCase(Locale.ROOT), List.of());
  }
}
```

- [ ] **Step 5: Run tests, verify pass**

Run: `mvn test -pl buurman-letters -am -Dtest=CountryLetterClauseCatalogTest`
Expected: 4/4 pass.

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-letters/src/main/java/com/buurman/service/letters/LetterClauseKey.java backend/buurman-letters/src/main/java/com/buurman/service/letters/CountryLetterClauseCatalog.java backend/buurman-letters/src/test/java/com/buurman/service/letters/CountryLetterClauseCatalogTest.java
git commit -m "feat(letters): add CountryLetterClauseCatalog for structured per-country legal clauses"
```

---

## Task 2: `LetterExporterHelper.legalVariables` + all 5 call sites

**Files:**
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/LetterExporterHelper.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/RentIncreaseLetterExporter.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/ContractExtensionAddendumExporter.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/RentChangeDocumentExporter.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/PaymentFormalNoticeExporter.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/DepositStatementExporter.java`
- Test: `backend/buurman-letters/src/test/java/com/buurman/service/letters/LetterExporterHelperTest.java`

**Interfaces:**
- Consumes: `CountryLetterClauseCatalog.resolve(String, String): List<LetterClauseKey>` (Task 1).
- Produces: `LetterExporterHelper.legalVariables(MessageSource, String keyPrefix, String documentType, Contract, Locale): Map<String, Object>` — the `"legalClauses"` entry is `List<Map<String,String>>`, each map `{"title": ..., "body": ...}` (nullable `title` entries permitted — see Step 3) — Task 3's templates iterate this directly.

- [ ] **Step 1: Write the failing test**

Add to `LetterExporterHelperTest.java` (new `@DisplayName`-grouped nested class or top-level methods, following the existing file's flat-methods style — the existing file only tests `premisesAddress`; add `legalVariables` coverage as new top-level test methods in the same class, since it's still testing the same helper):

```java
  @Test
  @DisplayName("legalVariables: a catalog-configured combination resolves multiple ordered clauses with titles")
  void legalVariablesResolvesCatalogClauses() {
    LetterExporterHelper helper =
        new LetterExporterHelper(null, null, null, null); // no repository calls on this path
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.of("DE")).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    assertThat(clauses).hasSize(2);
    assertThat(clauses.get(0)).containsKey("title").containsKey("body");
  }

  @Test
  @DisplayName("legalVariables: an unconfigured combination falls back to the single legacy clause, wrapped as a one-item list")
  void legalVariablesFallsBackToSingleClause() {
    LetterExporterHelper helper = new LetterExporterHelper(null, null, null, null);
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.of("FR")).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    // FR has no legal.FR key in document-extension.properties as of this branch — resolves empty,
    // exactly as `resolveLegalClause` returning Optional.empty() does today.
    assertThat(clauses).isEmpty();
  }

  @Test
  @DisplayName("legalVariables: a contract with no country code resolves no clauses, without throwing")
  void legalVariablesHandlesMissingCountryCode() {
    LetterExporterHelper helper = new LetterExporterHelper(null, null, null, null);
    ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
    messages.setBasenames("classpath:messages/document-extension");
    messages.setDefaultEncoding("UTF-8");
    messages.setUseCodeAsDefaultMessage(true);

    Contract contract = Contract.builder().countryCode(Optional.empty()).build();

    Map<String, Object> vars =
        helper.legalVariables(messages, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) vars.get("legalClauses");
    assertThat(clauses).isEmpty();
  }
```

Add the required new imports (`Contract`, `List`, `Map`, `Locale`,
`ReloadableResourceBundleMessageSource`) to the top of `LetterExporterHelperTest.java`
alongside the existing ones — check the file's current import block first and
add only what's missing.

- [ ] **Step 2: Run tests, verify they fail to compile**

Run: `mvn test -pl buurman-letters -am -Dtest=LetterExporterHelperTest`
Expected: compile error — `legalVariables` doesn't accept a `documentType` parameter yet.

- [ ] **Step 3: Change `legalVariables` and `resolveLegalClause`**

Replace in `LetterExporterHelper.java`:

```java
  /** Country code plus the country-specific legal clauses under the given key prefix. */
  Map<String, Object> legalVariables(
      MessageSource messageSource,
      String keyPrefix,
      String documentType,
      Contract contract,
      Locale locale) {
    Map<String, Object> vars = new HashMap<>();
    Optional<String> countryCode = contract.getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put("legalClauses", resolveLegalClauses(messageSource, keyPrefix, documentType, countryCode, locale));
    return vars;
  }

  /**
   * Resolves the ordered legal clauses for a letter: a {@link CountryLetterClauseCatalog} entry
   * for {@code documentType}/country if one exists (each clause pre-resolved to {@code {title,
   * body}} string maps, {@code title} omitted when the catalog gives no title key), else the
   * single legacy {@code keyPrefix + COUNTRY} clause wrapped as a one-item list, else empty.
   */
  List<Map<String, String>> resolveLegalClauses(
      MessageSource messageSource,
      String keyPrefix,
      String documentType,
      Optional<String> countryCode,
      Locale locale) {
    return countryCode
        .map(
            code -> {
              List<LetterClauseKey> catalogClauses =
                  CountryLetterClauseCatalog.resolve(documentType, code);
              if (!catalogClauses.isEmpty()) {
                return catalogClauses.stream()
                    .map(
                        clause -> {
                          Map<String, String> resolved = new HashMap<>();
                          resolved.put("title", messageSource.getMessage(clause.titleKey(), null, locale));
                          resolved.put("body", messageSource.getMessage(clause.bodyKey(), null, locale));
                          return resolved;
                        })
                    .toList();
              }
              return resolveLegalClause(messageSource, keyPrefix, Optional.of(code), locale)
                  .map(body -> List.of(Map.of("body", body)))
                  .orElse(List.<Map<String, String>>of());
            })
        .orElse(List.of());
  }
```

Keep the existing `resolveLegalClause` method exactly as-is (Step 254-266 of the
current file) — it's still the fallback resolver, unchanged, just called from
inside `resolveLegalClauses` now instead of directly from `legalVariables`.

- [ ] **Step 4: Update all 5 call sites**

Each exporter's single `helper.legalVariables(messageSource, "<prefix>", contract, locale)`
call gains the document-type literal as a new second-to-last argument
(placed before `contract`, matching the method's new parameter order):

`RentIncreaseLetterExporter.java:142`:
```java
    vars.putAll(helper.legalVariables(messageSource, "legal.", "rent-increase-letter", contract, locale));
```

`ContractExtensionAddendumExporter.java:176`:
```java
    vars.putAll(helper.legalVariables(messageSource, "legal.", "extension-addendum", contract, locale));
```

`RentChangeDocumentExporter.java:224`:
```java
    vars.putAll(helper.legalVariables(messageSource, "rentchange.legal.", "rent-change", contract, locale));
```

`PaymentFormalNoticeExporter.java:159` (note this call site passes `data.contract()`,
not a bare `contract` variable — read the surrounding method first to confirm the
exact receiver expression before editing):
```java
    vars.putAll(helper.legalVariables(messageSource, "notice.legal.", DOCUMENT_TYPE, data.contract(), locale));
```
(`DOCUMENT_TYPE` is the exporter's own existing `static final String DOCUMENT_TYPE = "payment-formal-notice"` constant — reuse it rather than repeating the literal, since the exporter already has it in scope.)

`DepositStatementExporter.java:132`:
```java
    vars.putAll(helper.legalVariables(messageSource, "deposit.legal.", DOCUMENT_TYPE, contract, locale));
```
(same reasoning — `DepositStatementExporter.DOCUMENT_TYPE = "deposit-statement"` already exists.)

- [ ] **Step 5: Run tests, verify pass**

Run: `mvn clean install -DskipTests -pl buurman-letters -am && mvn test -pl buurman-letters -am -Dtest=LetterExporterHelperTest`
Expected: 7/7 pass (4 existing `premisesAddress` tests + 3 new).

- [ ] **Step 6: Full module compile check**

Run: `mvn clean install -DskipTests -pl buurman-letters -am`
Expected: BUILD SUCCESS — confirms all 5 call sites compile against the new signature (a stale call site anywhere would fail this step, which is exactly why every call site was updated in this one task rather than split across tasks).

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-letters/src/main/java/com/buurman/service/letters/LetterExporterHelper.java backend/buurman-letters/src/main/java/com/buurman/service/letters/RentIncreaseLetterExporter.java backend/buurman-letters/src/main/java/com/buurman/service/letters/ContractExtensionAddendumExporter.java backend/buurman-letters/src/main/java/com/buurman/service/letters/RentChangeDocumentExporter.java backend/buurman-letters/src/main/java/com/buurman/service/letters/PaymentFormalNoticeExporter.java backend/buurman-letters/src/main/java/com/buurman/service/letters/DepositStatementExporter.java backend/buurman-letters/src/test/java/com/buurman/service/letters/LetterExporterHelperTest.java
git commit -m "feat(letters): widen legalVariables to resolve multiple ordered clauses per document type"
```

---

## Task 3: Templates — `th:each` loop across all 5 `generic.html` files, plus the render-test fixture

**Files:**
- Modify: `backend/buurman-letters/src/main/resources/templates/documents/rent-increase-letter/generic.html`
- Modify: `backend/buurman-letters/src/main/resources/templates/documents/extension-addendum/generic.html`
- Modify: `backend/buurman-letters/src/main/resources/templates/documents/rent-change/generic.html`
- Modify: `backend/buurman-letters/src/main/resources/templates/documents/payment-formal-notice/generic.html`
- Modify: `backend/buurman-letters/src/main/resources/templates/documents/deposit-statement/generic.html`
- Modify: `backend/buurman-letters/src/test/java/com/buurman/service/letters/TenantLetterLocaleRenderTest.java`
- Create: `backend/buurman-letters/src/test/java/com/buurman/service/letters/LandlordLetterLocaleRenderTest.java`

**Interfaces:**
- Consumes: the `legalClauses: List<Map<String,String>>` shape Task 2 produces (each map has a `"body"` key always, a `"title"` key only when the catalog supplied one).

- [ ] **Step 1: Update `rent-increase-letter/generic.html`, `rent-change/generic.html`, `payment-formal-notice/generic.html`, `deposit-statement/generic.html`**

Each currently has (identical across these 4 files, confirmed):
```html
    <div th:if="${legalClause != null}" class="legal-clause" th:text="${legalClause}"></div>
```

Replace with:
```html
    <div th:each="clause : ${legalClauses}" class="legal-clause">
      <div th:if="${clause.title != null}" class="legal-clause-title" th:text="${clause.title}"></div>
      <div th:text="${clause.body}"></div>
    </div>
```

`th:each` over an empty list renders nothing, matching the old `th:if="${legalClause != null}"`'s "renders nothing when absent" behavior exactly — no separate empty-check needed.

- [ ] **Step 2: Update `extension-addendum/generic.html`**

Currently (lines 221-224):
```html
  <div th:if="${legalClause != null}" class="legal-clause">
    <strong th:text="#{addendum.section.legalClauses}">Legal References</strong><br/>
    <span th:text="${legalClause}"></span>
  </div>
```

Replace with (preserving the existing heading wrapper, looping only the clause content inside it):
```html
  <div th:if="${not #lists.isEmpty(legalClauses)}" class="legal-clause">
    <strong th:text="#{addendum.section.legalClauses}">Legal References</strong><br/>
    <div th:each="clause : ${legalClauses}">
      <div th:if="${clause.title != null}" class="legal-clause-title" th:text="${clause.title}"></div>
      <span th:text="${clause.body}"></span>
    </div>
  </div>
```

This file needs the outer `th:if` (unlike the other four) because the "Legal References" heading itself must not render when there are no clauses — the other four templates don't have a shared heading to guard.

- [ ] **Step 3: Update `TenantLetterLocaleRenderTest`'s fixture**

In `allVars()` (line 117 of the current file), replace:
```java
    v.put("legalClause", "Clause text");
```
with:
```java
    v.put("legalClauses", List.of(Map.of("body", "Clause text")));
```

The test's `noRawKeys` assertions (`doesNotContain("notice.")`/`.doesNotContain("deposit.")`/`.doesNotContain("letter.")`) are unaffected — they check for leaked message keys, not the `legalClause`/`legalClauses` variable name itself.

- [ ] **Step 4: Run the updated test, verify it still passes**

Run: `mvn test -pl buurman-letters -am -Dtest=TenantLetterLocaleRenderTest`
Expected: pass in all 13 locales × 2 templates (26 cases) — proves `payment-formal-notice`
and `deposit-statement` (the two document types this ticket adds no new content
for) render correctly with the new list-shaped variable, i.e. the fallback path
is genuinely unbroken.

- [ ] **Step 5: Write `LandlordLetterLocaleRenderTest`**

Same technique as `TenantLetterLocaleRenderTest`, covering the three document
types that test doesn't (`rent-increase-letter`, `extension-addendum`,
`rent-change`), plus the specific DE/NL structured-clause assertions this
ticket is actually about:

```java
package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * i18n gate for the landlord-initiated letters (rent-increase, extension addendum, rent-change):
 * renders each in every supported document language and asserts no raw message key leaks into
 * the output. Also proves the per-country-addenda feature's two named cases (DE's two-section
 * §558 clause, NL's one-section legal-basis clause) render with the right structure and order.
 */
@DisplayName("landlord letter multi-locale render gate")
class LandlordLetterLocaleRenderTest {

  private static final List<String> LOCALES =
      List.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  private SpringTemplateEngine engine;
  private ReloadableResourceBundleMessageSource messages;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames(
        "classpath:messages/document-letter-chrome",
        "classpath:messages/document-extension",
        "classpath:messages/document-rent-change");
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(true);
    messages = ms;
    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
  }

  static Stream<Arguments> templatesAndLocales() {
    List<String> templates =
        List.of("rent-increase-letter/generic", "extension-addendum/generic", "rent-change/generic");
    return templates.stream().flatMap(t -> LOCALES.stream().map(l -> Arguments.of(t, l)));
  }

  @ParameterizedTest(name = "{0} [{1}]")
  @MethodSource("templatesAndLocales")
  @DisplayName("renders with no raw message-key leakage, legalClauses empty")
  void noRawKeysWithoutClauses(String template, String locale) {
    Context ctx = new Context(Locale.forLanguageTag(locale));
    ctx.setVariables(allVars(List.of()));
    String html = engine.process(template, ctx);

    assertThat(html).as("%s [%s] must not leak raw i18n keys", template, locale).doesNotContain("letter.");
  }

  @Test
  @DisplayName("DE two-clause catalog entry renders both sections in order, with headings")
  void deTwoClauseCatalogEntryRendersInOrder() {
    List<Map<String, String>> clauses =
        List.of(
            Map.of("title", "Section 558 Justification", "body", "Body one"),
            Map.of("title", "Comparison Method", "body", "Body two"));
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(allVars(clauses));
    String html = engine.process("rent-increase-letter/generic", ctx);

    assertThat(html)
        .containsSubsequence(
            "Section 558 Justification", "Body one", "Comparison Method", "Body two");
  }

  @Test
  @DisplayName("a single title-less clause (the legacy fallback shape) renders the body with no empty heading")
  void singleTitlelessClauseRendersBodyOnly() {
    List<Map<String, String>> clauses = List.of(Map.of("body", "Legacy clause text"));
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(allVars(clauses));
    String html = engine.process("rent-increase-letter/generic", ctx);

    assertThat(html).contains("Legacy clause text").doesNotContain("legal-clause-title");
  }

  private static Map<String, Object> allVars(List<Map<String, String>> legalClauses) {
    Map<String, Object> v = new HashMap<>();
    v.put("generatedDate", "24 Sep 2026");
    v.put("extensionIdentifier", "cex_01TEST");
    v.put("extensionNumber", 1);
    v.put("contractIdentifier", "ctr_01TEST");
    v.put("primaryContactName", "Alex Tenant");
    v.put(
        "contactAddress",
        Map.of(
            "street", "Main St 1", "postalCode", "1000", "city", "Amsterdam", "countryCode", "NL"));
    v.put("propertyAddress", "Canal 2, Amsterdam");
    v.put("hasMultipleUnits", false);
    v.put("unitDesignation", null);
    v.put("previousRent", "EUR 1,000.00");
    v.put("newRent", "EUR 1,050.00");
    v.put("rentEffectiveDate", "1 Nov 2026");
    v.put("adjustmentType", "Fixed Percentage");
    v.put("adjustmentBasis", "a 5% increase");
    v.put("adjustmentValue", "5%");
    v.put("newEndDate", null);
    v.put("countryCode", "DE");
    v.put("legalClauses", legalClauses);
    return v;
  }
}
```

Note: this fixture's variable set is sized for `rent-increase-letter/generic`
(the template with the most variables among the three covered) — Thymeleaf
ignores variables a template doesn't reference, so the same map works for
`extension-addendum/generic` and `rent-change/generic` too, matching how
`TenantLetterLocaleRenderTest`'s single `allVars()` already serves two
different templates.

- [ ] **Step 6: Run it, verify pass**

Run: `mvn test -pl buurman-letters -am -Dtest=LandlordLetterLocaleRenderTest`
Expected: 39 parameterized cases (3 templates × 13 locales) + 2 explicit tests, all pass.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-letters/src/main/resources/templates/documents/rent-increase-letter/generic.html backend/buurman-letters/src/main/resources/templates/documents/extension-addendum/generic.html backend/buurman-letters/src/main/resources/templates/documents/rent-change/generic.html backend/buurman-letters/src/main/resources/templates/documents/payment-formal-notice/generic.html backend/buurman-letters/src/main/resources/templates/documents/deposit-statement/generic.html backend/buurman-letters/src/test/java/com/buurman/service/letters/TenantLetterLocaleRenderTest.java backend/buurman-letters/src/test/java/com/buurman/service/letters/LandlordLetterLocaleRenderTest.java
git commit -m "feat(letters): render legalClauses as an ordered loop across all 5 letter templates"
```

---

## Task 4: Seed content — NL legal-basis clause, DE §558 two-section clause

**Files:**
- Modify: `backend/buurman-letters/src/main/resources/messages/document-extension.properties`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-extension_de.properties`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-extension_nl.properties`
- (11 more `document-extension_{da,el,es,fi,fr,it,nb,pl,pt,sv}.properties` files — same two new key pairs, English placeholder text, per Step 3 below)

**Interfaces:**
- Consumes: `CountryLetterClauseCatalog`'s hardcoded key names from Task 1 (`legal.NL.basis.title`/`.body`, `legal.DE.section558.title`/`.body`, `legal.DE.comparisonMethod.title`/`.body`) — this task is what makes those keys resolve to real text instead of `useCodeAsDefaultMessage` echoing the raw key back.

- [ ] **Step 1: Read the current `legal.NL`/`legal.DE` entries in `document-extension.properties`**

Confirm the exact existing lines (found during planning: `legal.NL=This extension is governed by Dutch tenancy law (huurrecht). Parties may refer disputes to the Huurcommissie (Rent Tribunal).` and `legal.DE=This extension agreement is made in accordance with §557 BGB (Mieterhöhungsvereinbarung). Applicable rent control regulations (Mietpreisbremse) may apply.`) — leave both untouched; they still serve `extension-addendum`'s fallback path, since no catalog entry exists for `extension-addendum` in Task 1.

- [ ] **Step 2: Add the new keys to `document-extension.properties` (English base)**

Append:

```properties
# BUUR-105 per-country addenda: rent-increase-letter structured clauses (Task 1's
# CountryLetterClauseCatalog). Example content grounded in the ticket's own stated
# requirements, not independently researched legal text -- verify against current
# local requirements before relying on this for a real notice.
legal.NL.basis.title=Legal Basis
legal.NL.basis.body=This rent increase is made in accordance with Dutch tenancy law (huurrecht) and the applicable rent indexation rules. You may refer disputes over this increase to the Huurcommissie (Rent Tribunal) within the statutory period.
legal.DE.section558.title=Section 558 BGB Justification
legal.DE.section558.body=This rent increase is made in accordance with §558 BGB (Mieterhöhung bis zur ortsüblichen Vergleichsmiete). The new rent does not exceed the locally customary comparative rent (ortsübliche Vergleichsmiete) for comparable dwellings.
legal.DE.comparisonMethod.title=Comparison Method
legal.DE.comparisonMethod.body=The comparative rent has been determined with reference to the local rent index (Mietspiegel) where available. Applicable statutory caps (Kappungsgrenze) on the rate of increase within the relevant period have been observed.
```

(Match the file's existing `\uXXXX`-escaped-non-ASCII convention for `.properties`
files, confirmed from the existing `legal.DE` line's `§`/`ö` escapes —
run the project's actual properties-file encoding tool/convention if one exists,
rather than hand-escaping, to avoid a mismatched encoding the build would flag.)

- [ ] **Step 3: Add the same 6 keys to all 12 translation files**

For `document-extension_de.properties`, provide real German translations (this
is the one language where the content is *about* German law, so leaving it in
English would be the most visible gap — translate at minimum this file properly,
not as a placeholder). For the other 11 (`_da`, `_el`, `_es`, `_fi`, `_fr`, `_it`,
`_nb`, `_nl`, `_pl`, `_pt`, `_sv`), add the same English text as the base file
with a `# TODO: translate` comment above the block — check how this codebase's
existing i18n parity guard (referenced elsewhere on this branch as already
enforcing key-set parity across all 13 language files) actually behaves before
assuming English-as-placeholder passes it; if the guard requires distinct
non-English text per file to pass, translate all 12 instead of stubbing 11 —
confirm this by running the guard (see Step 4) rather than assuming.

`document-extension_nl.properties` should get a real Dutch translation of the
`legal.NL.basis.*` pair specifically (same reasoning as German above — this is
the language most relevant to that clause's content); the two `legal.DE.*` pairs
in the Dutch file can follow the same-as-base-English-with-TODO approach as the
other 10 files.

- [ ] **Step 4: Run the i18n parity check**

Run whatever this codebase's existing locale-parity guard test/command is
(search for it — likely a test class asserting all `.properties` siblings of a
base bundle have the same key set; this branch's earlier work referenced
"i18n parity guards" as already existing) against `document-extension*.properties`.
Expected: passes — confirms all 13 files now have the 6 new keys, none missing.

- [ ] **Step 5: Run the full letters module test suite**

Run: `mvn clean install -DskipTests -pl buurman-letters -am && mvn test -pl buurman-letters -am`
Expected: all tests pass, including `LandlordLetterLocaleRenderTest`'s DE/NL
structured-clause assertions now resolving real (not `useCodeAsDefaultMessage`
raw-key) text — re-check `deTwoClauseCatalogEntryRendersInOrder`'s assertions
still hold with real content in place of the test's own hardcoded `"Body one"`/
`"Body two"` stand-ins (that test passes its own explicit `legalClauses` list,
not values resolved from the properties file, so it's unaffected by this task's
content — this step just re-confirms no regression, it doesn't need new
assertions here).

- [ ] **Step 6: Commit**

```bash
git add backend/buurman-letters/src/main/resources/messages/document-extension*.properties
git commit -m "feat(letters): seed NL legal-basis and DE §558 structured rent-increase clauses"
```

---

## Final verification

- [ ] Run: `cd backend && mvn clean install -DskipTests -pl buurman-letters -am && mvn test -pl buurman-letters -am`
Expected: all tests pass, including both render-gate test classes across all 13 locales.
- [ ] Manually confirm (read the generated HTML from a `LandlordLetterLocaleRenderTest` debug run, or add a temporary print) that `extension-addendum`'s "Legal References" heading still appears exactly once when a legacy single clause is present, and not at all when `legalClauses` is empty — this is the one template whose structure this plan touches without this ticket giving it any new content, so it's the highest-risk regression surface.
