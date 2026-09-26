# Units Exports, Letters, Demo Data and i18n Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make everything that leaves the app unit-aware — booklets, CSV/Excel exports, takeout archives and tenant letters — generate realistic multi-unit demo data, and translate the unit vocabulary into the remaining twelve locales.

**Architecture:** Exports read dwelling values from the unit rather than the property, and a multi-unit property's booklet gains a Units section. Letters thread the unit designation through a single address helper that all five letter types already share. Demo data grows a `DemoUnitGenerator` that runs before contracts, and building-level demo expenses flow through the real allocation service so the demo exercises production code.

**Tech Stack:** Java 25, Spring Boot 4.0.2, JOOQ, Thymeleaf, Gotenberg (PDF), OpenCSV, Apache POI, Quartz, react-i18next.

**Spec:** `docs/superpowers/specs/2026-09-26-units-multi-unit-buildings-design.md`

**Plan series:** Plan 3 of 3. **Depends on plan 1** (backend model and allocation engine) being merged. Independent of plan 2 except for Task 6, which finishes the locale files plan 2 started in English.

## Global Constraints

- Every repository query filters by `team_id`. No exceptions.
- Soft delete via `deleted_at`; set `created_by` / `updated_by` in every write.
- All `if`, `else`, `for`, `while` bodies use curly braces.
- Use idiomatic `Optional` — never `opt.get()` without `isPresent()`, never `if (opt != null)`.
- Typed identifiers extend `Sid` with private constructors: build with `UnitIdentifier.of(...)`, never `new`. They *are* `Sid`s — no `.toSid()`.
- PDFs render via the **Gotenberg** sidecar (`http://localhost:3000`), the only PDF engine. There is no in-JVM iText. PDF tests need Gotenberg running or must assert on the HTML handed to it.
- Letter text lives in `backend/buurman-letters/src/main/resources/messages/document-<type>[_<lang>].properties` — 13 languages per type. Templates are one `generic.html` per letter type, **not** one per locale.
- App locale files: `frontend/app/public/locales/<lng>/<ns>.json` for `en nl pt es fr de it sv fi el pl da nb`.
- Canonical unit terms, decided once: en Unit, nl Woning, de Wohnung, fr Logement, pt Fração, es Vivienda, it Unità, sv Lägenhet, fi Asunto, el Κατοικία, pl Lokal, da Bolig, nb Bolig.
- Demo data must be realistic, not filler — it is what a prospective landlord sees first.

## Review Focus

1. **A letter for a single-unit property must not gain a stray unit designation.** "Keizersgracht 12, unit 1" on a whole-house tenancy reads as an error to the tenant. Only a genuinely multi-unit property names the unit. → Task 3.
2. **A booklet for a 50-unit building** must not produce 50 pages of near-empty detail blocks or blow the Gotenberg render timeout. → Task 1.
3. **Takeout for a team whose property was soft-deleted** — its implicit unit still exists in the table by design, so `units.csv` must not leak rows for properties the landlord deleted. → Task 2.
4. **A demo team regenerated twice** must not accumulate duplicate units; the Quartz regeneration job truncates first, and `units` must be in that truncation list in the right FK order. → Task 4.
5. **A locale whose plural rules differ from English** (pl has three plural forms, el and fi differ again) — the unit-count strings must use the correct i18next plural suffixes, not a copy of the English `_other` pair. → Task 6.

---

## Task 1: Booklets, summary cards and tabular exports

Owns Review Focus item 2.

**Files:**
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/PropertySummaryAssembler.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/PropertyBookletExporter.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/PropertyCsvExporter.java`, `PropertyExcelExporter.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/ExpenseCsvExporter.java`, `ExpenseExcelExporter.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/PropertyDashboardCsvExporter.java`, `PropertyDashboardExcelExporter.java`, `PropertyDashboardPdfExporter.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/PortfolioDashboardCsvExporter.java`, `PortfolioDashboardExcelExporter.java`, `PortfolioDashboardPdfExporter.java`
- Modify: `backend/buurman-booklets/src/main/java/com/buurman/service/export/EnumLabelResolver.java`
- Create: `backend/buurman-booklets/src/main/java/com/buurman/service/export/UnitCsvExporter.java`, `UnitExcelExporter.java`
- Modify: the property booklet Thymeleaf template under `backend/buurman-booklets/src/main/resources/templates/`
- Create: `backend/buurman-booklets/src/test/java/com/buurman/service/export/UnitCsvExporterTest.java`

**Interfaces:**
- Consumes: `UnitRepository`, `ExpenseAllocationRepository` (plan 1).
- Produces:
  - `UnitCsvExporter.export(List<Unit>, DocumentLocale) : byte[]`
  - `PropertySummaryAssembler` output gains `List<UnitSummary> units()`, `int unitCount()`, `List<UnitSummary> detailedUnits()` (capped at 20) and `int remainingUnitCount()`
  - `EnumLabelResolver` resolves `UnitType`, `UnitStatus`, `AllocationBasis`

- [ ] **Step 1: Write the failing unit-export test**

Follow an existing exporter test — read `backend/buurman-booklets/src/test/java/com/buurman/service/export/` for the established shape first.

```java
@Test
@DisplayName("writes one row per unit with translated type and status")
void writesUnitRows() {
  byte[] csv =
      exporter.export(
          List.of(unit("1", UnitType.APARTMENT, UnitStatus.OCCUPIED, new BigDecimal("85.50")),
                  unit("2", UnitType.PARKING, UnitStatus.VACANT, null)),
          DocumentLocale.EN);

  String text = new String(csv, StandardCharsets.UTF_8);
  assertThat(text).contains("Apartment").contains("Parking space");
  assertThat(text).contains("85.50");
  // A unit with no area must render an empty cell, never "null".
  assertThat(text).doesNotContain("null");
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && mvn test -pl buurman-booklets -Dtest='UnitCsvExporterTest'`

Expected: FAIL — `UnitCsvExporter` does not exist.

- [ ] **Step 3: Add the enum labels**

`EnumLabelResolver` gains `UnitType`, `UnitStatus` and `AllocationBasis` cases. The label keys live in the booklet message bundles — find the existing bundle for property enums (`grep -rn "PropertyCategory\|propertyCategory" backend/buurman-booklets/src/main/resources/messages/`) and add unit keys in all 13 languages alongside, using the canonical terms from Global Constraints.

- [ ] **Step 4: Write the exporters**

`UnitCsvExporter` and `UnitExcelExporter` follow the existing `PropertyCsvExporter` / `PropertyExcelExporter` pair. Columns: unit number, name, floor, type, status, area, energy label, bedrooms, bathrooms, furnished, cost share, tenant, monthly rent.

`ExpenseCsvExporter` and `ExpenseExcelExporter` gain allocation columns — allocated unit, amount, basis. A building-level expense produces one row per allocation; a unit-level expense produces one row naming its unit.

- [ ] **Step 5: Add the booklet Units section**

`PropertySummaryAssembler` loads the property's units and exposes `unitCount` plus a summary list. The booklet template renders a Units table whenever `unitCount > 1`, then one detail block per unit.

**Guard for Review Focus item 2:** cap the per-unit detail blocks at 20 and render a "and N more units" line beyond that, so a 50-unit building produces a readable booklet within Gotenberg's render budget. The Units *table* still lists all of them — only the verbose detail blocks are capped.

- [ ] **Step 6: Write the large-building test**

```java
@Test
@DisplayName("caps verbose per-unit detail blocks for a large building but lists every unit")
void capsDetailBlocksForLargeBuilding() {
  PropertySummary summary = assembler.assemble(propertyWithUnits(50), locale);

  assertThat(summary.units()).hasSize(50);
  assertThat(summary.detailedUnits()).hasSize(20);
  assertThat(summary.remainingUnitCount()).isEqualTo(30);
}
```

- [ ] **Step 7: Run the module suite**

Run: `cd backend && mvn test -pl buurman-booklets`

Expected: PASS.

- [ ] **Step 8: Render a real booklet**

With `make dev` running (Gotenberg included), generate a booklet for a multi-unit property through the API and open the PDF. Confirm the Units table lists every unit and the document is not visually broken. A passing unit test does not prove the PDF renders.

- [ ] **Step 9: Commit**

```bash
git add backend/buurman-booklets/
git commit -m "feat(booklets): unit-aware booklets, exports and enum labels (BUUR-106)"
```

---

## Task 2: Takeout

Owns Review Focus item 3.

**Files:**
- Modify: `backend/buurman-takeout/src/main/java/com/buurman/service/TakeoutService.java`
- Modify: `backend/buurman-takeout/src/main/java/com/buurman/repository/DataTakeoutRepository.java`
- Create: `backend/buurman-takeout/src/test/java/com/buurman/service/TakeoutServiceTest.java` if absent, else modify

**Interfaces:**
- Produces: `units.csv`, `unit-residential-details.csv`, `unit-amenities.csv`, `expense-allocations.csv` in the archive; `properties.csv` without dwelling columns; `contracts.csv` with a unit identifier column.

- [ ] **Step 1: Write the failing tests**

```java
@Test
@DisplayName("includes the unit CSVs in the manifest")
void includesUnitFiles() {
  assertThat(service.manifest(TEAM_ID))
      .extracting(TakeoutFile::name)
      .contains("units.csv", "unit-residential-details.csv",
                "unit-amenities.csv", "expense-allocations.csv");
}

@Test
@DisplayName("omits units belonging to a soft-deleted property")
void omitsUnitsOfDeletedProperties() {
  UUID deletedPropertyId = seedProperty(TEAM_ID);
  seedUnit(deletedPropertyId, TEAM_ID, "1");
  softDeleteProperty(deletedPropertyId);

  assertThat(repository.fetchUnits(TEAM_ID)).isEmpty();
}
```

The second test is Review Focus item 3: the V068 backfill deliberately created implicit units for soft-deleted properties to keep contract FKs valid, so an unguarded takeout query would hand the landlord rows for properties they deleted.

- [ ] **Step 2: Run to verify they fail, then implement**

Run: `cd backend && mvn test -pl buurman-takeout`

Every new fetch joins `properties` on `deleted_at IS NULL`, matching `UnitRepository` from plan 1 Task 5. Update the `amenities.csv` description text (it currently says amenities attach to properties) and the `properties.csv` description, which currently reads "All properties (units, buildings, etc.)" — that parenthetical predates the real Unit entity and is now actively misleading.

- [ ] **Step 3: Generate a real takeout**

Trigger a takeout through the API for a team with a multi-unit property, download the archive, and confirm the new CSVs are present and populated.

- [ ] **Step 4: Run and commit**

Run: `cd backend && mvn test -pl buurman-takeout`

```bash
git add backend/buurman-takeout/
git commit -m "feat(takeout): export units, unit details and expense allocations (BUUR-106)"
```

---

## Task 3: Letters name the dwelling

A formal notice addressed to "Keizersgracht 12" when the tenant rents apartment 2 is legally defective. Owns Review Focus item 1.

**Good news from reading the code:** all five letter exporters share one helper, `LetterExporterHelper.propertyAddress(Property)`, and the templates interpolate a single `${propertyAddress}` variable into localized message bundles (`#{notice.body.overdue(${propertyAddress}, ...)}`). So this is **one helper change plus one optional bundle key**, not an edit to 13 locale templates as the spec's wording implied.

**Files:**
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/LetterExporterHelper.java`
- Modify: `backend/buurman-letters/src/main/java/com/buurman/service/letters/PaymentFormalNoticeExporter.java`, `RentChangeDocumentExporter.java`, `RentIncreaseLetterExporter.java`, `DepositStatementExporter.java`, `ContractExtensionAddendumExporter.java`
- Modify: `backend/buurman-letters/src/main/resources/messages/document-payment-notice*.properties` and the other four families — add `*.summary.unit`
- Create: `backend/buurman-letters/src/test/java/com/buurman/service/letters/LetterExporterHelperTest.java`

**Interfaces:**
- Produces: `LetterExporterHelper.premisesAddress(Property property, Unit unit, int propertyUnitCount) : String`

- [ ] **Step 1: Write the failing helper tests**

```java
package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LetterExporterHelper.premisesAddress")
class LetterExporterHelperTest {

  @Test
  @DisplayName("names the unit for a multi-unit building")
  void namesUnitForMultiUnitBuilding() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Keizersgracht 12", "1015 CJ", "Amsterdam"), unit("2", null), 4))
        .isEqualTo("Keizersgracht 12, unit 2, 1015 CJ Amsterdam");
  }

  @Test
  @DisplayName("prefers the unit's own name when it has one")
  void prefersUnitName() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Keizersgracht 12", "1015 CJ", "Amsterdam"),
                unit("2", "Garden apartment"),
                4))
        .isEqualTo("Keizersgracht 12, Garden apartment, 1015 CJ Amsterdam");
  }

  @Test
  @DisplayName("omits the unit entirely for a single-unit property")
  void omitsUnitForSingleUnitProperty() {
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Dorpsstraat 5", "3451 AB", "Utrecht"), unit("1", null), 1))
        .isEqualTo("Dorpsstraat 5, 3451 AB Utrecht");
  }

  @Test
  @DisplayName("omits the unit designation even when the sole unit is no longer implicit")
  void omitsUnitWhenSoleUnitIsExplicit() {
    Unit explicitSoleUnit = unit("A", null);
    assertThat(
            LetterExporterHelper.premisesAddress(
                property("Dorpsstraat 5", "3451 AB", "Utrecht"), explicitSoleUnit, 1))
        .doesNotContain("A");
  }
}
```

The last two tests are Review Focus item 1 — the decision keys on `propertyUnitCount`, never on `unit.isImplicit()`, because a landlord who split and deleted back to one unit still has a single-dwelling tenancy.

- [ ] **Step 2: Run to verify they fail**

Run: `cd backend && mvn test -pl buurman-letters -Dtest='LetterExporterHelperTest'`

Expected: FAIL — `premisesAddress` does not exist.

- [ ] **Step 3: Implement the helper**

```java
  /**
   * The address of the let premises, as it must appear on a legal notice. A multi-unit building
   * names the specific dwelling — a notice naming only the building is defective when the tenant
   * rents one apartment of several.
   *
   * <p>Keys on {@code propertyUnitCount}, not on {@link Unit#isImplicit()}: a landlord who split a
   * property into units and later deleted back down to one still has a single-dwelling tenancy, and
   * "Dorpsstraat 5, unit A" would read as an error to that tenant.
   */
  static String premisesAddress(Property property, Unit unit, int propertyUnitCount) {
    StringBuilder address = new StringBuilder(property.getStreet());
    if (propertyUnitCount > 1) {
      address
          .append(", ")
          .append(unit.getName().orElseGet(() -> "unit " + unit.getUnitNumber()));
    }
    return address
        .append(", ")
        .append(property.getPostalCode())
        .append(" ")
        .append(property.getCity())
        .toString();
  }
```

The literal `"unit "` prefix must come from the letter's message bundle rather than being hardcoded English, since these documents render in 13 languages. Add a `premises.unitPrefix` key to each `document-letter-chrome[_<lang>].properties` file (that family is already shared chrome across letter types) and resolve it through the same `MessageSource` the exporters already hold. Adjust the helper signature to accept the resolved prefix:

```java
  static String premisesAddress(
      Property property, Unit unit, int propertyUnitCount, String unitPrefix)
```

and update the four test call sites to pass `"unit "`.

- [ ] **Step 4: Thread it through the five exporters**

Each exporter already loads the contract, which now carries `unitId`. Load the unit and the property's unit count, then replace `LetterExporterHelper.propertyAddress(property)` with `premisesAddress(...)`. `ContractExtensionAddendumExporter` builds its own `propertyAddress` inline at line 110 — route that through the helper too rather than leaving a second definition of "the address".

Add a `*.summary.unit` row to the summary tables in the templates, shown only when `propertyUnitCount > 1`, with the key translated in all 13 bundle files per letter family.

- [ ] **Step 5: Run the tests**

Run: `cd backend && mvn test -pl buurman-letters`

Expected: PASS.

- [ ] **Step 6: Render a real notice**

With Gotenberg running, generate a formal notice for a tenant of a multi-unit building and one for a single-unit property. Read both PDFs: the first must name the unit, the second must not mention a unit at all.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-letters/
git commit -m "feat(letters): name the let dwelling on tenant letters (BUUR-106)

A notice naming only the building is legally defective when the tenant rents
one apartment of several. Single-unit tenancies are unchanged."
```

---

## Task 4: Demo data

Owns Review Focus item 4.

**Files:**
- Create: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoUnitGenerator.java`
- Modify: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoDataService.java`
- Modify: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoPropertyGenerator.java`
- Modify: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoContractGenerator.java`
- Modify: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoExpenseGenerator.java`
- Modify: `backend/buurman-demo-data/src/main/java/com/buurman/service/demo/DemoDataContext.java`

**Interfaces:**
- Consumes: `UnitRepository`, `ExpenseAllocationService` (plan 1).
- Produces: `DemoUnitGenerator.generate(DemoDataContext) : void`, populating `context.unitsByProperty()`.

- [ ] **Step 1: Fix the truncation list first**

`DemoDataService` currently imports `PROPERTY_AMENITIES` and `PROPERTY_RESIDENTIAL_DETAILS`, both dropped by V068. Plan 1 Task 16 should already have made this compile; verify and correct the truncation order so it reads, before `PROPERTIES`:

```java
        .add(EXPENSE_ALLOCATIONS)
        .add(UNIT_AMENITIES)
        .add(UNIT_RESIDENTIAL_DETAILS)
        .add(UNITS)
```

matching however the existing list is built. This is Review Focus item 4 — without `UNITS` in the list, the Quartz regeneration job accumulates duplicate units on every run and eventually trips `uq_units_property_number`.

- [ ] **Step 2: Write the failing regeneration test**

```java
@Test
@DisplayName("regenerating a demo team twice leaves no duplicate units")
void regenerationIsIdempotent() {
  service.regenerate(DEMO_TEAM_ID);
  int afterFirst = unitRepository.countActiveByTeamId(DEMO_TEAM_ID);

  service.regenerate(DEMO_TEAM_ID);
  int afterSecond = unitRepository.countActiveByTeamId(DEMO_TEAM_ID);

  assertThat(afterSecond).isEqualTo(afterFirst);
}
```

- [ ] **Step 3: Write DemoUnitGenerator**

Ordered in `DemoDataService` **after** `DemoPropertyGenerator` and **before** `DemoContractGenerator`, because contracts need a `unit_id`. The portfolio shape is chosen so every acceptance criterion is visible in demo data:

| Demo property | Units |
|---|---|
| 8 existing-style properties | 1 implicit unit each — proves single-unit landlords see no change |
| "Keizersgracht 12", Amsterdam | 6 APARTMENT, 5 let / 1 vacant → a non-trivial occupancy rate |
| A 4-unit building | 4 APARTMENT, 1 let → the ticket's 25%-occupancy criterion, literally |
| A mixed building | 2 APARTMENT + 1 PARKING + 1 STORAGE |
| A commercial building | 2 COMMERCIAL |

Units carry realistic dwelling data: floor areas that differ per unit (so `AREA` allocation produces uneven, believable splits), different energy labels within one building (so per-unit WWS is visibly different), and plausible bedroom counts. The 8 single-unit properties keep `is_implicit = true` so they render exactly as they did before the change.

- [ ] **Step 4: Assign contracts to units**

`DemoContractGenerator` reads `context.unitsByProperty()` and attaches each contract to a specific unit, deliberately leaving the vacancies above unlet. It must not let every unit — a 100%-occupied demo portfolio shows none of the vacancy features.

- [ ] **Step 5: Building-level demo expenses that really allocate**

`DemoExpenseGenerator` adds, on the 4-unit building, a **€1,200 roof repair** dated in the current year, plus annual insurance and an HOA fee. These are created with `unit_id = null` and passed through the real `ExpenseAllocationService`, so the demo exercises the production allocation path rather than hand-written rows. With `EQUAL` basis the roof repair shows €300 per unit — the ticket's criterion, visible without the reviewer creating anything.

Also give the 6-unit building `AREA` basis so the demo shows an uneven split too.

- [ ] **Step 6: Run the tests and regenerate for real**

Run: `cd backend && mvn test -pl buurman-demo-data`

Then boot the app with demo data enabled and open the demo team in the UI. Confirm: a single-unit property looks untouched; "Keizersgracht 12" shows 6 units at 5/6 let; the 4-unit building shows 25% occupancy; the roof repair shows €300 against each of its units.

- [ ] **Step 7: Commit**

```bash
git add backend/buurman-demo-data/
git commit -m "feat(demo): generate multi-unit buildings and allocated building costs (BUUR-106)"
```

---

## Task 5: Backoffice unit visibility

**Files:**
- Modify: the backoffice team-listing service and controller under `backend/buurman-backoffice/src/main/java/com/buurman/service/backoffice/`
- Modify: `frontend/backoffice/src/` — the team list table
- Modify: `openapi/` backoffice spec if the backoffice has its own, else the shared one

**Interfaces:**
- Consumes: `billableUnitCount` from plan 1 Task 15.

- [ ] **Step 1: Add the column**

The backoffice team row gains a Units figure beside its Properties figure. Plan 1 already exposes the number; this is display only. A team with 3 properties and 7 units reads `Properties 3 | Units 7`.

- [ ] **Step 2: Run the backoffice tests**

Run: `cd backend && mvn test -pl buurman-backoffice && cd ../frontend && yarn test`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/buurman-backoffice/ frontend/backoffice/
git commit -m "feat(backoffice): show unit count per team (BUUR-106)"
```

---

## Task 6: The remaining twelve locales

Owns Review Focus item 5.

**Files:**
- Create: `frontend/app/public/locales/{nl,pt,es,fr,de,it,sv,fi,el,pl,da,nb}/units.json`
- Modify: the same twelve languages' `properties.json`

**Interfaces:**
- Consumes: `frontend/app/public/locales/en/units.json` from plan 2 Task 2 as the key source of truth.

- [ ] **Step 1: Confirm the English namespace is final**

Run: `cd frontend && yarn test --run` and check that no test references a `units` key absent from `en/units.json`. Translating before the English keys settle means twelve files to redo.

- [ ] **Step 2: Translate, respecting each language's plural rules**

Copy `en/units.json` to each locale and translate. The canonical noun per language is in Global Constraints.

**Plural forms are the trap** (Review Focus item 5). i18next uses CLDR plural categories, and they are not all `one`/`other`:

- `pl` has **three** forms — `units_one`, `units_few`, `units_many`. `1 lokal`, `2 lokale`, `5 lokali`.
- `el`, `fi`, `sv`, `da`, `nb`, `de`, `nl`, `it`, `es`, `pt`, `fr` use `one`/`other`, but several put the noun before the number differently — do not assume the English word order survives interpolation.

So `grid.vacancyDaysValue` and `properties.json`'s `unitCount` need the right suffix set per file, not a mechanical copy of the English `_other` pair. For `pl/units.json`:

```json
  "grid": {
    "vacancyDaysValue_one": "{{count}} dzień",
    "vacancyDaysValue_few": "{{count}} dni",
    "vacancyDaysValue_many": "{{count}} dni"
  }
```

- [ ] **Step 3: Write a key-parity test**

Create `frontend/app/src/i18n/__tests__/localeParity.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const LOCALES_DIR = join(import.meta.dirname, '../../../public/locales');
const BASE = 'en';

/** Collects dotted key paths, collapsing i18next plural suffixes to their stem. */
const keyPaths = (obj: unknown, prefix = ''): string[] => {
  if (typeof obj !== 'object' || obj === null) {
    return [prefix.replace(/_(one|two|few|many|other|zero)$/, '')];
  }
  return Object.entries(obj).flatMap(([k, v]) =>
    keyPaths(v, prefix ? `${prefix}.${k}` : k)
  );
};

const load = (lng: string, ns: string) =>
  JSON.parse(readFileSync(join(LOCALES_DIR, lng, `${ns}.json`), 'utf8'));

describe('locale parity', () => {
  const locales = readdirSync(LOCALES_DIR).filter((l) => l !== BASE);

  it.each(locales)('%s defines every units key that en does', (lng) => {
    const expected = new Set(keyPaths(load(BASE, 'units')));
    const actual = new Set(keyPaths(load(lng, 'units')));
    const missing = [...expected].filter((k) => !actual.has(k));
    expect(missing).toEqual([]);
  });

  it.each(locales)('%s leaves no string identical to English', (lng) => {
    // A handful of terms legitimately match (proper nouns, "Parking"); list them
    // explicitly so a genuinely untranslated file still fails.
    const allowed = new Set(['type.PARKING']);
    const base = load(BASE, 'units');
    const translated = load(lng, 'units');
    const untranslated: string[] = [];
    const walk = (a: unknown, b: unknown, path = '') => {
      if (typeof a === 'string') {
        if (a === b && a.length > 3 && !allowed.has(path)) {
          untranslated.push(path);
        }
        return;
      }
      if (typeof a === 'object' && a !== null && typeof b === 'object' && b !== null) {
        Object.keys(a).forEach((k) =>
          walk(
            (a as Record<string, unknown>)[k],
            (b as Record<string, unknown>)[k],
            path ? `${path}.${k}` : k
          )
        );
      }
    };
    walk(base, translated);
    expect(untranslated).toEqual([]);
  });
});
```

The second test is what stops a copy-pasted English file from passing as a translation.

- [ ] **Step 4: Run the tests**

Run: `cd frontend && yarn test --run localeParity`

Expected: PASS for all twelve. A failure names the exact locale and key.

- [ ] **Step 5: Spot-check in the browser**

Run `yarn dev`, switch the language to Dutch and then Polish, and open a multi-unit property. Confirm the Units tab reads *Woningen* / *Lokale*, and that a unit count of 2 and of 5 both read correctly in Polish — that is the plural rule actually working.

- [ ] **Step 6: Commit**

```bash
git add frontend/app/public/locales/ frontend/app/src/i18n/
git commit -m "feat(i18n): translate the units namespace into twelve locales (BUUR-106)"
```

---

## Task 7: Whole-branch verification

- [ ] **Step 1: Full backend suite**

Run: `cd backend && mvn test`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Full frontend suite**

Run: `cd frontend && yarn test && yarn build && yarn lint`

Expected: all clean.

- [ ] **Step 3: Fresh-database boot**

Run: `make down-v && make dev`, wait ~30s, boot the backend, confirm Flyway applies V068 on an empty database and demo data generates without error.

- [ ] **Step 4: Walk the acceptance criteria one final time**

1. An existing single-unit property is visually identical to before — no Units tab, dwelling fields on Info, letters with no unit designation.
2. "Keizersgracht 12" with 4 units, a contract on unit 2 → 25% occupancy and vacancy cost for the other three.
3. A €1,200 roof repair allocates €300 to each unit's performance report.
4. The rent increase wizard lists units, not buildings.
5. A booklet, a CSV export and a takeout archive all contain units.
6. A formal notice for a multi-unit tenancy names the apartment.

- [ ] **Step 5: Request code review**

REQUIRED SUB-SKILL: use `superpowers:requesting-code-review` for a whole-branch review before merge.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test: verify units acceptance criteria across the branch (BUUR-106)"
```

---

## Done when

- `cd backend && mvn test` and `cd frontend && yarn test && yarn build && yarn lint` are green.
- All six acceptance criteria above pass by hand.
- `localeParity` passes for all twelve non-English locales, including Polish plurals.
- A formal notice for a multi-unit tenancy names the dwelling; one for a single-unit tenancy does not.
- Demo data shows a 6-unit building, a 4-unit building at 25% occupancy, and a €1,200 roof repair split €300 per unit.

## Follow-up issues to file

- **ROOM / HMO** — ticket scope item 5, deferred by decision. `unit_type` is a widenable enum and no nesting was introduced, so the model is ready for it.
- **Plan limit enforcement** — `billableUnitCount` is exposed; gating belongs to a billing project.
- **BUUR-104 dependency** — `unit_id` on `maintenance_requests` and `meter_readings`, which that issue creates.
