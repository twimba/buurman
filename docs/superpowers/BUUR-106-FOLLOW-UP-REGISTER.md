# BUUR-106 — follow-up register

Findings from a nine-specialist review panel (Principal Engineer, Senior Architect, Security,
SRE, QA, Frontend, UI/UX, Engineering Manager, Director of Engineering) on the
`implement-buur-106` branch, triaged into what was fixed on the branch and what needs its own
issue. Each item below is written to be pasted into Linear as-is.

**Panel verdicts:** Security, Architect and Director — ship with conditions. Principal, SRE, QA,
Frontend and EM — do not ship. UX — do not ship without four conditions.

**Convergence is the strongest signal here.** Where three independent reviewers found the same
defect it is noted, because those are the ones least likely to be false positives.

---

## A. Release blockers — this branch cannot merge or deploy without these

### A1. Plan 2 (frontend) must merge in the same PR — `yarn build` fails with 138 errors
Severity: **blocker (merge)**

All 138 errors are caused by this branch, verified by two reviewers independently: checking out
the pre-branch `openapi/app.yaml`, regenerating and building gives **zero** errors; restoring the
branch bundle gives 138. An earlier claim that they were pre-existing was wrong — it regenerated
against a "base" file that was already the branch's spec.

Seven files, 137 errors from the 16-field dwelling move plus 1 from the deleted amenities client:
`PropertyForm.tsx` (45), `PropertyDetailPage.tsx` (44), `PropertyCharacteristicsForm.tsx` (39),
`PropertyCard.tsx` (5), `types/property.ts` (3), `PropertyListPage.tsx` (1),
`usePropertyHooks.ts` (1). None of the seven is among the three frontend files this branch
touched.

**Root cause is a planning contradiction, not a bug.** The spec chose "break the API cleanly in
one release — frontend, backoffice and OpenAPI updated in the same PR"; the work was then
decomposed into three plans with the frontend in plan 2. Those decisions are incompatible.

Do **not** ship a compile-only stub patch: it deletes 16 dwelling fields from the property page
for every existing landlord (the single-unit majority the implicit-unit design exists to shield),
is ~600 lines that plan 2 then rewrites, and V068 is irreversible so "revert if users complain" is
unavailable. Estimated: plan 2's minimum usable subset (Tasks 1–4, 6 partial, plus WWS repair
which plan 2 does not currently contain) ≈ 45–50h.

### A2. Split V068 into expand/contract before deploying
Severity: **blocker (deploy)**

V068 creates tables, backfills, **and drops 16 columns plus two tables in one transaction**, while
Flyway runs at application startup on a rolling-deploy platform. JOOQ never emits `SELECT *`, so
the old jar names `properties.status` explicitly. At the instant V068 commits, the still-serving
old container fails **every** property read. If the new container fails its healthcheck for any
unrelated reason there is no forward path and no backward path — only point-in-time recovery.

Fix: V068 (expand — create, backfill, add nullable `unit_id`, populate) → an app release tolerating
both schemas → V070 (contract — `SET NOT NULL`, re-scope GiST, drop). If that is refused, stop all
old containers before starting the new one and announce it as a planned maintenance window.

Two multipliers, both cheap:
- **No `lock_timeout` anywhere.** `ALTER TABLE properties` takes `ACCESS EXCLUSIVE` and holds it
  until COMMIT — through the backfill, three UPDATEs, a GiST rebuild and three index builds. A
  Quartz job holding `ACCESS SHARE` at the top of the hour makes the ALTER queue, and then *every*
  query on `properties` queues behind it, exhausting all 10 Hikari connections. Add
  `SET LOCAL lock_timeout = '3s'; SET LOCAL statement_timeout = '10min';` as the first statements.
- **Docker healthcheck kills the container at ~150s** (`start-period=60s`, `retries=3`,
  `interval=30s`), before Flyway finishes on a production-shaped database — producing a crash-loop
  that re-locks the tables in 150s bursts indefinitely. Raise `start-period` past a measured run,
  or move Flyway out of app startup into a one-shot pre-deploy job.

### A3. Add a `multi_unit` feature flag, default off
Severity: **blocker (exposure)** · Effort: **~2 hours**

The single highest-value item in the panel. Buurman already has a mature per-team flag system
(`FeatureFlags`, `FeatureFlagOverrideRepository`) and this branch uses none of it.

The migration and the implicit unit are invisible and need no flag. But **every** deferred
legal-exposure item below manifests only on a property with more than one unit. Gating unit
creation and bulk-create converts all of them from live risk to pilot-only. This is what makes a
conditional ship defensible rather than optimistic.

### A4. Pre-flight queries against a restored production snapshot
Severity: **blocker (deploy)**

V068 has been verified only against an empty database, which exercises none of the backfill, none
of the lock behaviour and none of the duration. Before deploying, run and record:

```sql
SELECT DISTINCT status FROM properties;                       -- must be a subset of the 8 UnitStatus values
SELECT relname, n_live_tup FROM pg_stat_user_tables
  WHERE relname IN ('properties','contracts','property_occupancy_periods','wws_calculations',
                    'photos','documents','expenses','property_amenities',
                    'property_residential_details') ORDER BY n_live_tup DESC;
SELECT count(*) FROM properties WHERE area_unit IS NOT NULL AND area_unit NOT IN ('sqm','sqft');
SELECT unit_id FROM contracts WHERE status = 'ACTIVE' AND deleted_at IS NULL
  GROUP BY unit_id HAVING count(*) > 1;                       -- pre-existing duplicates (see C4)
```

`properties.status` never had a CHECK constraint while `units.status` does, so a single out-of-band
value aborts an irreversible migration. The risk is low (the enum was only ever appended to) but
the query costs five seconds. Also write and test a **forward-recovery V069** that re-creates the
two dropped tables and 16 columns from `units WHERE is_implicit` — for the single-unit majority it
is lossless and converts "restore from backup" into "roll forward to the old schema".

---

## B. Legal-exposure items — must precede plan 3, which ships the documents

### B1. WWS reads are still property-scoped — a landlord can price one unit from another's ceiling
Severity: **high** · Found by **three** reviewers

WWS (Woningwaarderingsstelsel) sets a landlord's **legal maximum rent**. Writes are per-unit;
reads are not. `WwsCalculationRepository` has no `findLatestByUnitId` at all, and
`WwsCalculationResponse` carries **no unit field** — so the history view is N visually identical
rows the frontend cannot label even if it wants to. `PropertyDetailPage:176` renders
`useLatestWwsCalculation(propertyId)` in the pricing panel.

Failure: a landlord calculates WWS for a 28 m² studio (92 points), then opens the building's panel
while pricing a 95 m² maisonette and reads 92 points → ~€680 ceiling. Roughly €400/month foregone
for the contract's life. Mirrored, they set a rent **above** the legal ceiling — in NL a
Huurcommissie ruling and retroactive repayment.

Fix: add `unitIdentifier` + `unitNumber` to `WwsCalculationResponse` as required; add
`findLatestByUnitId`/`findByUnitId`; expose `GET /units/{sid}/wws/latest`. Interim guard: have
`/properties/{sid}/wws/latest` return 409 when `unitCount > 1` — a refusal is correct, a plausible
wrong number is not.

### B2. The WWS pre-fill is silently broken today
Severity: **high** · Found by **three** reviewers

`useWwsHooks` passes a **property** Sid to an endpoint that moved to
`/wws/units/{unitIdentifier}/pre-fill`. Both generated identifier types are `export type X = string`
— structural aliases — so `tsc` is happy, eslint is happy, all 156 tests pass. The landlord opens
the calculator, it 404s, the form opens **blank**, and they hand-type the floor area and energy
label that determine the legal ceiling.

Fix: correct the call site, and **brand the identifier types** in `orval.config.ts`
(`type UnitIdentifier = string & { readonly __brand: 'UnitIdentifier' }`). ~20 lines that convert
an entire class of silent entity-rescoping breakage into compile errors.

### B3. Allocation rows record their outputs but none of their inputs
Severity: **high** · Found by **two** reviewers

`expense_allocations` stores `amount` and `basis` only. `AREA` depends on `units.area_value` *as it
was then* and `CUSTOM` on `allocation_share` *as it was then*; both are mutated in place with no
history. A tenant disputing a 2026 statement in 2028, after the landlord corrected a unit's area,
cannot be shown what was charged and why — the statement is undefendable and unreproducible.

Fix (additive, NULL for pre-existing rows): add `weight`, `weight_total` and
`expense_total_amount` to `expense_allocations`, stamped at compute time. Also add logging and a
`buurman.allocations.computed` counter — the service currently has **zero** log statements and
zero metrics despite its Javadoc calling recomputation "an auditable, opt-in act".

*Note: a reviewer confirmed no settlement-statement document exists in the codebase yet —
`expense_allocations` is read only by its own API. So this is latent, and its deadline is plan 3.*

### B4. Data takeout lost every dwelling attribute — GDPR Art. 20
Severity: **high**

`TakeoutService` exports none of `units`, `unit_residential_details`, `unit_amenities`,
`expense_allocations`. Because it projects the live JOOQ meta-model, V068's `DROP COLUMN`s silently
removed area, energy rating, heating and ten more from the `properties` sheet, and the two dropped
tables' sheets vanished with them. A landlord exercising data portability, or leaving for a
competitor, receives an export with **no dwelling data** — and `property_occupancy_periods` ships a
`unit_id` pointing at a table that isn't in the export.

Fix: add the four tables, plus a test asserting the exported set covers every team-scoped table in
the meta-model, so the next migration cannot silently drop something from the export.

---

## C. Correctness and data-integrity items

### C1. `MoneyMinorUnitConverter` hard-codes scale 2 while the allocation engine is currency-aware
Severity: **high**

The converter comments "all supported currencies currently use 2 fractional digits (see BUUR-51)" —
`BUUR-51` appears nowhere in the codebase. The `forcedType` regex now pulls
`expense_allocations.amount` **and** `units.woz_value` into it, while the engine uses
`CurrencyUtils.getFractionalDigits`. So a write path and its own persistence layer scale at
different exponents.

`ReferenceController` offers all ~150 JDK currencies, `OnboardingService` accepts any ISO code,
`team_preferences.default_currency` has no CHECK, and `CountryMetadataRegistry` **recommends CLP for
Chile**. A BHD/KWD/TND team gets `ArithmeticException: Rounding necessary` → unhandled → **500 on
every building-level expense**; a CLP/JPY team stores WOZ 100× too large.

Fix: make the conversion currency-aware per repository (the pattern already exists for
`contact_credits.remaining_amount` and five others). Stopgap: reject any currency whose
`getDefaultFractionDigits() != 2` at onboarding and currency-change, and drop CLP from the
recommendations until the converter is fixed.

### C2. `units.woz_value_currency` bypasses `CurrencyEnforcement`, and WOZ is silently discarded
Severity: **medium**

Every other money write validates the currency against the team; the unit path does not. And
`wozValue` present with `wozValueCurrency` absent yields `Optional.empty()` — the value is
**silently discarded at 200 OK**, while WOZ feeds the WWS calculation.

### C3. No optimistic locking on `units` — a concurrent promote + edit can resurrect `is_implicit`
Severity: **medium**

`save()`'s UPDATE writes all 27 columns from a snapshot, with no `version` and no
`DELETED_AT IS NULL` predicate. T1 promotes the implicit unit; T2's PUT read it while still
implicit and writes `true` back. The unique index does not trip (one row is implicit). The property
now has two real units with #1 still flagged implicit — so the next bulk-create treats it as the
stand-in and **renumbers** it, and the UI hides implicit units: unit #1 disappears from the grid
while keeping all its contracts. That is the exact invariant the branch exists to protect.

Fix: add `version INTEGER NOT NULL DEFAULT 0` and make the UPDATE conditional on it. Interim:
exclude `is_implicit` from the generic UPDATE and give promotion its own targeted statement.

### C4. Two concurrent activations can put two ACTIVE contracts on one unit, then 500 forever
Severity: **medium**

`findActiveByUnitId` uses `fetchOptional()`, which throws `TooManyRowsException` → unhandled →
500, on a state `UnitService` elsewhere explicitly documents as possible. The guard is a
read-then-write with no lock, so two concurrent `PATCH …/status → ACTIVE` both pass. From then on
`POST /contracts` and every status change on that unit 500 — and the only escape is a status
change, which also 500s. **Unrecoverable through the API.**

Fix: partial unique index
`ON contracts (unit_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL` (making the race a 409),
and `fetchAny()` so a legacy duplicate degrades instead of 500ing. Needs the A4 pre-flight query
first.

### C5. `deleteProperty` does not cascade the soft delete to units
Severity: **medium**

V068 itself copies `p.deleted_at` onto every backfilled unit, establishing the invariant — but
`deleteProperty` breaks it. Since `UnitRepository`'s single-row lookups deliberately omit the
`properties` join, `PUT /units/{id}/amenities` and `…/residential-details` still **accept writes**
on units of a deleted property, and `DELETE` returns a nonsensical "cannot delete the last unit".

### C6. Unit deletion has no escape hatch once an expense touches the building
Severity: **medium**

`deleteUnit` refuses while allocation rows exist — and allocation rows are created
**automatically** for every unit whenever any building-level expense is saved (annual insurance,
HOA: the first things a landlord records). So a landlord who types `60` instead of `6` and then
records the insurance has 60 permanently undeletable units and no recovery path. Compounding: bulk
create is `TEAM_EDITOR`, delete is `TEAM_ADMIN` — an editor can create a mess they cannot clean up.

Fix: when a unit's only allocation rows are non-`MANUAL` and it has no contract/occupancy/WWS
history, soft-delete it and recompute the affected expenses in the same transaction. Add
`deletable` + `blockedBy[]` to `UnitGridRowResponse` so the UI disables the action with a reason
instead of a 409 after the click. Add `POST …/units/bulk?dryRun=true` returning the labels the
server *would* create — the frontend plan currently reimplements the numbering client-side and will
drift from the `AA/AB` rollover and the `%02d` widening.

### C7. Six independent definitions of "the unit set that counts"
Severity: **medium**

`UnitRepository`, `PropertyRepository` (×2), `BackofficeTeamStatsRepository`,
`DatabaseMetricsRepository` and `ExpenseAllocationService` each re-derive the countable-unit
predicate by hand. They agree today only because a fix made two of them agree after they diverged.
Under a future `parent_unit_id` (ROOM/HMO) a 4-bedroom HMO becomes 5 rows: billing counts 5, an
EQUAL split gives the parent a share alongside its own rooms, and status counts double.

Fix: one `UnitScope` condition factory every rollup composes. ~1 hour, zero behaviour change, and
it makes the spec's "the model stays extensible" true of the code rather than only the schema.

### C8. `buurman-booklets` imports `buurman-core`'s repositories
Severity: **medium**

Twelve of them, directly. V068 deleted two and the module's output degraded in place: residential
details `null`, amenities empty, five CSV columns dropped from `properties.csv`/`.xlsx`, and one
assembler throwing. Any core schema change is an N-module refactor.

Fix: give `core` a published read surface (`PropertyReadModel`, `UnitView`). Best done **during**
plan 3, which rewrites those exporters anyway — marginal cost near zero, versus re-establishing the
same coupling by hand.

---

## D. UX and product-correctness items

- **D1 (high).** Per-property occupancy reports **100% whenever any single unit is let** —
  `tenantDays` is capped at days-in-month, so 25% and 100% are indistinguishable. This makes the
  ticket's acceptance criterion 2 fail outright. Divide by `unitCount × daysInMonth`.
- **D2 (high).** `PropertyResponse.residentialDetails` and `.amenities` are hardcoded empty for
  every property while still advertised in the schema, and the property edit form submits →
  200 OK → data gone. This inverts the spec's headline promise ("existing teams see identical
  screens"). Populate the read path from the sole unit when `unitCount == 1`, and reject
  property-level writes with a 400 naming the new endpoint — a visible rejection beats a silent
  loss.
- **D3 (medium).** "3/4 let" excludes `MAINTENANCE`, `SELF_OCCUPIED`, `UNDER_RENOVATION`, `LISTED`,
  `UNAVAILABLE` and `FALLOW`, so `occupied + vacant ≠ unitCount`. A 4-unit building with 3 let and
  1 in maintenance renders "3/4 let", reading as one unit available. Return the full status map;
  render three segments.
- **D4 (medium).** The naming decision is false in 7 of 13 locales: nl *Woning*, de *Wohnung*,
  sv *Lägenhet*, fi *Asunto*, el *Κατοικία*, da/nb *Bolig* all mean **dwelling**, while `unit_type`
  includes `PARKING`, `STORAGE` and `COMMERCIAL`. "Woningen (3)" on a building with two flats and a
  parking space is false — and in NL, *woning* is the term the WWS and the Huurcommissie use. Use a
  neutral container term (nl *Eenheid*, de *Einheit*, fr *Lot*) plus a per-type member label
  (*Woning* / *Parkeerplaats* / *Berging*). **Fix before 13 locales are written against the current
  list.**
- **D5 (medium).** All five interpolated error messages are structurally untranslatable: the
  handler puts raw English in `detail` and the frontend maps by **exact string**, so a Polish or
  Greek landlord gets English forever. One message emits `1200.00` to a Dutch landlord whose every
  other number reads `1.200,00`; another exposes a raw Sid. Give `BusinessRuleException` a stable
  code plus typed args and interpolate client-side.
- **D6 (low).** The property status filter silently changed meaning — "Vacant" now matches a
  building with *at least one* vacant unit, so a 5/6-let building appears in the Vacant list.
  Relabel to "Has a vacant unit".

---

## E. Observability and operations

- **E1 (medium).** `buurman_properties_by_status` now reports **units** while keeping its name and
  description. `sum(by_status)` equalled `properties_count` before deploy and will not after, so
  every vacancy-rate panel and alert jumps discontinuously and reads as a business event rather
  than a deploy artifact. Emit a new `buurman.units.by.status` series and annotate the dashboards.
- **E2 (medium).** No `buurman.units.count` metric exists at all — for the entity the product now
  **bills on**.
- **E3 (medium).** `findUnitCountsByTeamId` aggregates the **entire team** on every page of the
  properties list, not the page's ids: a 1,000-property team with 50 units each scans ~50,000 rows
  and discards 98%, on the primary authenticated landing page, on every page change. The N+1 was
  designed out and replaced with an unbounded aggregate. Pass the page ids, and add
  `units (team_id, property_id, status) WHERE deleted_at IS NULL` — which also fixes the
  correlated-EXISTS status filter that is evaluated twice per request.
- **E4 (low).** Index hygiene: `idx_units_property_id` is a redundant prefix of
  `uq_units_property_number`; `expense_allocations.team_id` has `ON DELETE CASCADE` with **no
  index**, so a GDPR erasure sequential-scans an unbounded table; the three `unit_id` indexes on
  `photos`/`documents`/`expenses` index columns that are 100% NULL and should be partial.
- **E5 (low).** `toResponseWithMainPhoto` is an N+1 sitting on the same line range as the one this
  branch fixed — one photo query plus up to two presigns per property, 50 per page.
- **E6 (low).** No `lock_timeout`, `statement_timeout` or `idle_in_transaction_session_timeout`
  anywhere; only `connection-timeout`. This is what turns a slow `ALTER` into a fleet-wide 500.

---

## F. Test coverage — QA's required list

Blocking (1–5), ship-with (6–12):

1. Assert every field copied by V068's two `INSERT … SELECT` statements, on **two** properties with
   different values — they immediately precede `DROP TABLE` and have zero coverage.
2. Replace three `isNotNull()` assertions with per-property identity: each backfilled `unit_id`
   belongs to **its own** property and the two differ. Today a backfill missing
   `u.property_id = c.property_id` would reattach every tenancy to an arbitrary building and stay
   green.
3. `UnitMapperTest` against the **real** `UnitMapperImpl`: `Optional.empty()` clears each of the 20
   optional columns; `status`/`wozValue` retain theirs. The PUT-clears invariant plan 2 depends on
   is currently unverified in either direction.
4. `UnitRepository.save()`'s UPDATE branch — 28 columns, never executed against PostgreSQL, with no
   `NOT NULL` to expose a wrong binding.
5. The active-contract guard via **public** methods. It is currently tested by reflection on a
   private helper while the `@DisplayName` claims otherwise, so deleting the public call sites
   still passes.
6. `ExpenseAllocationService` integration: basis read from the property and persisted, `recompute`
   after adding a unit, `overrideManual`, duplicate → 409.
7. `ExpenseRepository` `unit_id` through both `save()` branches.
8. `unitId` added to four existing `*RecordMapperTest`s (~4 lines each) — catches
   `setUnitId(record.getPropertyId())`.
9. A real duplicate `(property_id, unit_number)`, asserting the exception jOOQ **actually** throws,
   plus `allocationShare = 150` must not yield the duplicate-number message.
10. `resetDemoData()` + `generateAll()` twice against a Flyway-migrated Testcontainer, in
    `buurman-app`. Would have caught both `unit_id` regressions and covers the 20-step FK-ordered
    cleanup this branch reordered.
11. Add `buurman-app` + `buurman-demo-data` to CI. *(fixed in wave 1)*
12. `openapi/**` in the frontend path filters + a real frontend workflow. *(fixed in wave 1)*

Also: **cross-tenant writes are untested everywhere** — every isolation test covers reads only,
while `replaceForExpense` with a foreign `teamId` and `UnitRepository.save()` moving a unit to
another team's property are both unguarded.

---

## G. Process changes

1. **A plan document needs a spec-conformance review before execution.** ~11 of the defects found
   across this work originated in the plan, not the implementations — including four caught by a
   pre-flight scan before any code was written. Sixteen implementers were paid to rediscover the
   same plan bugs.
2. **When a finding is accepted, grep for the rest of its class in the same commit.** Three defect
   classes on this branch each recurred a second and third time after being "fixed": a
   property-scoped guard needing unit scope, a `required` schema field returning null, and a rule
   implemented two different ways in two places. One reviewer noted this accounts for five separate
   defects. A codified invariant (a composite FK, a shared predicate, a build-time check) beats a
   repeated review.
3. **A documented guarantee must name the test that proves it.** `ExpenseAllocationService`'s
   Javadoc promises recomputation is "opt-in"; the code recomputes unconditionally. `is_implicit` is
   documented as "a UI-visibility hint only" while being a state-machine flag load-bearing in six
   branches.
4. **The per-task review gate is not optional for a task touching a published contract.** Tasks 1–8
   were reviewed and 9–15 were not; both final Criticals landed in the unreviewed set.
5. **Verification claims must carry the command and its output.** Two claims on this branch were
   wrong and load-bearing: "the 138 errors are pre-existing" (refuted in two commands) and
   "BUILD SUCCESS" (measured with `-Pquick` and `mvn test`, both of which skip the format gate that
   fails).
6. **Automate the `required`-vs-`Optional` cross-check.** Four instances of that one defect class on
   this branch — two caught, two not. A build-time check comparing each response record's
   non-`Optional` components against its schema's `required` array would have caught all four.
7. **A plan decomposition that contradicts the spec's release strategy is a spec amendment** and
   needs the same sign-off, not a plan-authoring decision. That contradiction is this branch's merge
   blocker.
