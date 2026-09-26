# Units & multi-unit buildings — design

**Linear:** [BUUR-106](https://linear.app/buurman/issue/BUUR-106/units-and-multi-unit-buildings-introduce-a-unit-entity-under-property)
**Date:** 2026-09-26
**Status:** Approved for planning

## Intent

Buurman is positioned for "landlords with 1 to 50 units", but `properties` is the
atomic rentable object. A landlord owning one building with six apartments must
create six properties and loses every building-level fact: one mortgage, one WOZ
value, one insurance policy, one roof. This is the most common urban portfolio
shape in NL/DE/FR/PT, and every competitor models Property → Unit.

This design introduces a `Unit` entity under `Property`. A property becomes the
**building** (address, structure, financing, building-level costs). A unit becomes
the **dwelling** (area, rooms, energy label, status, its own contract and WWS
points).

Success criteria, from the ticket:

- Existing teams see identical screens after deploy; the implicit unit is hidden.
- A landlord creates "Keizersgracht 12" with 4 units, signs a contract on unit 2,
  and the building shows 25% occupancy plus vacancy cost for the other three.
- A €1,200 roof repair on the building allocates €300 to each unit's performance
  report.
- The rent increase wizard lists units, not buildings.

### Scope note

The ticket carries seven scope items. It was flagged that this is more than one
spec should hold and that a three-way decomposition was available; the decision
was to keep all items in a single spec. One item was subsequently deferred
(ROOM/HMO, see Out of scope) and one is blocked by a missing dependency
(BUUR-104, see Out of scope).

## Decisions

| Question | Decision |
|---|---|
| Implicit default unit | **Materialize a real row.** Every property always has ≥1 unit. `unit_id` is `NOT NULL` on contracts, occupancy and WWS. One code path everywhere. |
| Dwelling attributes | **Move to `units`.** Property becomes building-only. Required for per-unit WWS. |
| API compatibility | **Break cleanly in one release.** Single-consumer internal API; no deprecation layer, no dual-write. |
| Property detail UI | **Adaptive.** `unitCount == 1` renders dwelling fields inline on Info as today; `> 1` adds a Units tab and per-unit detail pages. |
| Cost allocation | **Stored allocation rows** per expense. Settlement statements are legal documents in NL; frozen numbers matter. |
| ROOM / HMO | **Deferred** to a follow-up. Model stays extensible; no nesting. |
| Plan limits | **Expose a countable metric, no enforcement.** No billing system exists to hook into. |
| Naming | **"Unit", localised to each market's legal term** (nl Woning, de Wohnung, fr Logement, pt Fração, es Vivienda, …). |

### Prior-art warning

`V020__multi_unit_detail_fields.sql` already exists and is **unrelated** — there
"multi unit" means *units of measure* (`ceiling_height_m` → value + unit). Do not
mistake it for rental-unit work.

## 1. Data model

### The property/unit split

`properties` holds ~90 columns mixing building and dwelling concerns. They divide
as follows.

**Stays on `properties` (building):**

- Identity and location: `identifier`, `team_id`, `property_category`,
  `property_type`, `street`, `city`, `postal_code`, `country`, `region_code`,
  `latitude`, `longitude`, `geocode_accuracy`
- Structure: `year_built`, `year_last_renovated`, `construction_type`,
  `foundation_type`, `roof_type`, `wall_construction`, `number_of_floors`,
  `structural_notes`
- Connections: `electricity_connection_type`, `electricity_capacity_value`/`_unit`,
  `water_connection_type`, `has_gas_connection`, `sewage_type`,
  `internet_connection_type`, `internet_max_speed_value`/`_unit`,
  `internet_status`
- Building safety: `has_sprinkler_system`, `has_alarm_system`,
  `has_security_cameras`, `has_secure_entry`, `safety_notes`
- Building accessibility: `has_elevator`, `has_step_free_entrance`,
  `is_wheelchair_accessible`
- Parking: `parking_spaces`, `parking_type`
- Financial: `purchase_price*`, `purchase_date`, `current_market_value*`,
  `market_value_date`, `mortgage_*`, `monthly_mortgage_payment*`, `land_value*`,
  `depreciation_method`, `depreciation_years`, `annual_property_tax*`,
  `annual_insurance*`, `annual_hoa_fee*`, `annual_management_fee*`,
  `annual_maintenance_reserve*`

**Moves to `units` (dwelling):**

- `status`
- `area_value`, `area_unit`
- `energy_efficiency_rating`, `energy_certificate_expiry_date`
- `heating_type`, `cooling_type`, `hot_water_system`, `insulation_notes`
- `flooring_type`, `window_type`
- `has_smoke_detectors`, `has_co_detectors`, `has_fire_extinguisher`
- `has_adapted_bathroom`, `accessibility_notes`

Satellite tables follow the same rule:

- `property_residential_details` → **`unit_residential_details`** (`bedrooms`,
  `bathrooms`, `furnished`, `pet_policy`)
- `property_amenities` → **`unit_amenities`**

These stay at property level, per the ticket: `property_valuations`,
`property_taxes`, `property_insurances`, `property_financings`, `property_fees`,
`property_acquisitions`, `property_outdoor_areas`.

### New tables

```
units                                    (EntityPrefix.UNT, identifier VARCHAR(29))
  id UUID PK DEFAULT gen_random_uuid()
  identifier VARCHAR(29) NOT NULL
  team_id UUID NOT NULL REFERENCES teams (id)
  property_id UUID NOT NULL REFERENCES properties (id)
  name VARCHAR(255)
  unit_number VARCHAR(50) NOT NULL
  floor INTEGER
  sort_order INTEGER NOT NULL DEFAULT 0
  unit_type VARCHAR(30) NOT NULL       -- APARTMENT | PARKING | STORAGE | COMMERCIAL
  status VARCHAR(50) NOT NULL          -- UnitStatus, see below
  is_implicit BOOLEAN NOT NULL DEFAULT false
  woz_value BIGINT, woz_value_currency VARCHAR(3)
  woz_share_pct NUMERIC(6,3)           -- WOZ/valuation apportionment
  allocation_share NUMERIC(6,3)        -- cost-allocation share, CUSTOM basis only
  ... all dwelling columns listed above ...
  created_at, updated_at, created_by, updated_by, deleted_at
  UNIQUE (team_id, identifier)
  UNIQUE (property_id, unit_number) WHERE deleted_at IS NULL

unit_residential_details                 (mirrors property_residential_details)
  unit_id, team_id, bedrooms, bathrooms, furnished, pet_policy, audit

unit_amenities                           (mirrors property_amenities)
  unit_id, amenity_id, team_id, notes, audit, deleted_at
  UNIQUE (unit_id, amenity_id, team_id)

expense_allocations                      (EntityPrefix.EAL)
  id, identifier, team_id
  expense_id UUID NOT NULL REFERENCES expenses (id)
  unit_id UUID NOT NULL REFERENCES units (id)
  amount BIGINT NOT NULL, amount_currency VARCHAR(3) NOT NULL
  basis VARCHAR(20) NOT NULL              -- AREA | EQUAL | CUSTOM | MANUAL
  audit, deleted_at
  UNIQUE (expense_id, unit_id) WHERE deleted_at IS NULL
```

`properties` gains `allocation_basis VARCHAR(20)`.

**`UnitStatus` is a verbatim move of `Property.PropertyStatus`** — `VACANT`,
`OCCUPIED`, `SELF_OCCUPIED`, `MAINTENANCE`, `UNAVAILABLE`, `UNDER_RENOVATION`,
`FALLOW`, `LISTED`. Same values, so the backfill copies `properties.status`
directly with no mapping. `PropertyStatus` is then deleted from `Property.java`,
since the column it described is dropped.

`woz_share_pct` and `allocation_share` are deliberately separate: the first
apportions the building's WOZ value (valuation and tax), the second drives
`CUSTOM`-basis cost allocation. A landlord may well want a legally-fixed WOZ split
and a different practical cost split.

### Tables gaining `unit_id`

**`NOT NULL`:**

- `contracts`
- `property_occupancy_periods` — its GiST no-overlap exclusion constraint
  re-scopes from `property_id` to `unit_id`
- `wws_calculations`

**Nullable:**

- `photos`, `documents`
- `expenses` — set means the expense targets one unit directly; null means it is
  building-level and is split via `expense_allocations`

**Unchanged, reached through the contract:** `payments`, `deposits`,
`payment_plans`, `contract_parties`, `contract_rent_components`,
`contract_rent_periods`, `contract_extensions`.

### Consequences

1. **`properties.status` is dropped.** Status is per unit. Property-level status
   becomes derived (`occupiedUnitCount / unitCount`). Property list filters that
   today match `status = 'VACANT'` become "has at least one vacant unit".
2. **Ambiguous attributes, resolved:** `parking_spaces` / `parking_type` stay at
   building level (a PARKING unit is a separately let object, which is a different
   thing from a building's parking count); `is_wheelchair_accessible` stays
   building; `property_outdoor_areas` stays at property level.

## 2. Migration & backfill

One migration, `V068__units.sql`. Postgres DDL is transactional, so it is
all-or-nothing. Ordered steps:

1. Create `units`, `unit_residential_details`, `unit_amenities`,
   `expense_allocations`; add `properties.allocation_basis`.
2. Backfill one implicit unit per property, **including soft-deleted properties**,
   so no contract loses its FK target:

   ```sql
   INSERT INTO units (identifier, team_id, property_id, is_implicit,
                      unit_number, sort_order, unit_type, status,
                      allocation_share, area_value, area_unit,
                      energy_efficiency_rating, ...)
   SELECT 'UNT' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 26)),
          p.team_id, p.id, true, '1', 0,
          CASE p.property_category WHEN 'COMMERCIAL' THEN 'COMMERCIAL'
                                   ELSE 'APARTMENT' END,
          p.status, 100, p.area_value, p.area_unit,
          p.energy_efficiency_rating, ...
   FROM properties p;
   ```

   The Sid pattern reuses the established one from
   `V013__migrate_property_financials.sql`; hex is a subset of ULID's Crockford
   base32 alphabet, so generated identifiers validate.
3. Copy `property_residential_details` → `unit_residential_details` and
   `property_amenities` → `unit_amenities`, joining through the implicit unit.
4. Add `unit_id` **nullable** to `contracts`, `property_occupancy_periods`,
   `wws_calculations`; populate from the implicit unit via `property_id`; then
   `SET NOT NULL`, add FK, add index.
5. Add nullable `unit_id` to `photos`, `documents`, `expenses`.
6. Drop `excl_occupancy_periods_no_overlap` and recreate it on `unit_id`.
7. Drop the moved columns from `properties`; drop `properties.status`; drop
   `property_residential_details` and `property_amenities`.

Then `cd backend && mvn generate-sources -pl buurman-jooq -am`.

### Service-layer invariants

- **A property always has ≥1 unit.** Deleting the last unit returns `409`.
- **Implicit units are promoted in place, never replaced.** Adding units to a
  single-unit property flips `is_implicit = false` on the existing row and makes it
  #1 of the batch, so its contracts, payments, occupancy history, photos and WWS
  calculations stay attached. Bulk-creating "6 apartments numbered 1–6" on a
  property with an implicit unit promotes that unit to #1 and inserts 5 more.
- **`is_implicit` is a UI-visibility hint only.** It must never appear as a filter
  in an aggregation; every rollup counts all non-deleted units.

### Rollback

Flyway has no down-migration and step 7 destroys data, so a bad deploy requires a
database restore. Mitigation is test coverage, specified in section 6.

## 3. Backend API & services

All new code lives in `buurman-core` (units are core domain) except DTOs and
domain types, which live in `buurman-common`.

### New files

**`buurman-common`:** `Unit`, `UnitType`, `UnitStatus`, `AllocationBasis`,
`UnitIdentifier`, `ExpenseAllocationIdentifier`, `EntityPrefix.UNT` and
`EntityPrefix.EAL`, plus `SidGenerator` entries. DTOs: `CreateUnitRequest`,
`UpdateUnitRequest`, `BulkCreateUnitsRequest`, `UnitResponse`,
`UnitGridRowResponse`, `ExpenseAllocationResponse`, `AllocationPreviewResponse`.

**`buurman-core`:** `UnitRepository`, `ExpenseAllocationRepository` (manual
`team_id` in every WHERE clause), `UnitMapper` (MapStruct, `componentModel =
"spring"`), `UnitService`, `ExpenseAllocationService`, `UnitController`.

### Endpoints

```
GET     /properties/{sid}/units            grid: unit, tenant, rent, status, vacancy days
POST    /properties/{sid}/units            create one
POST    /properties/{sid}/units/bulk       {count, numberingPattern, type, defaults}
GET     /units/{sid}
PUT     /units/{sid}
DELETE  /units/{sid}                       409 if it is the last unit
GET/PUT /units/{sid}/residential-details
GET/PUT /units/{sid}/amenities
PUT     /properties/{sid}/allocation       basis + per-unit custom shares
GET     /expenses/{sid}/allocations
PUT     /expenses/{sid}/allocations        manual override
POST    /expenses/{sid}/allocations/recompute
```

Authorization mirrors `PropertyService`: reads team-scoped and unannotated, writes
`@PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")`, delete
`@PreAuthorize("hasRole('TEAM_ADMIN')")`.

### Changed contracts

- **`PropertyResponse`** drops dwelling fields; gains `unitCount`,
  `occupiedUnitCount`, `vacantUnitCount`, `allocationBasis`, and `units[]`
  (summary rows).
- **`CreatePropertyRequest`** drops dwelling fields, gains an optional inline
  `unit` object so the existing single-submit create-property form keeps working.
  Absent → the server creates an implicit unit with defaults.
- **Contract create:** `unitSid` is **optional**. Omitted with exactly one unit on
  the property → the server resolves it. Omitted with more than one → `400`. This
  deliberately softens "break cleanly" to keep imports and the single-unit path
  ergonomic.
- **`TeamResponse`** gains `billableUnitCount` — all non-deleted units, implicit
  included. The backoffice team list gains a Units column.

### Allocation engine

`ExpenseAllocationService.allocate(expense)` runs on expense create and update
when `unit_id IS NULL`, reading `properties.allocation_basis`:

| Basis | Share |
|---|---|
| `AREA` | unit `area_value` ÷ sum of sibling `area_value` |
| `EQUAL` | 1 ÷ unit count |
| `CUSTOM` | unit `allocation_share` ÷ 100 |
| `MANUAL` | caller-supplied amounts, validated to sum to the total |

Rounding uses **largest-remainder in minor units**, so allocations sum exactly to
the expense with no drifting cent. `AREA` with any sibling missing `area_value`
falls back to `EQUAL` and records that on the row's `basis`.

Changing a property's allocation basis does **not** rewrite existing allocations.
`POST /expenses/{sid}/allocations/recompute` does it explicitly.

### OpenAPI

New `openapi/src/paths/units.yaml`; edits to `properties.yaml`, `contracts.yaml`,
`expenses.yaml`, `teams.yaml`. Then `make bundle-openapi` and
`cd frontend && yarn generate:api`.

## 4. Frontend

### New files

```
pages/UnitDetailPage.tsx                  route /properties/:propertySid/units/:unitSid
components/units/PropertyUnitsTab.tsx     grid: unit · tenant · rent · status · vacancy days
components/units/UnitGrid.tsx
components/units/UnitCharacteristicsForm.tsx
components/units/BulkCreateUnitsModal.tsx
components/units/UnitAllocationSettings.tsx
components/units/UnitCell.tsx
hooks/useUnits.ts                         React Query hooks + cache invalidation
```

`UnitCharacteristicsForm` is extracted **once** and rendered in two places: inline
on the property Info tab when `unitCount === 1`, and on `UnitDetailPage`
otherwise. One component, one set of validations, no divergence between the two
render modes. `PropertyCharacteristicsForm` shrinks to building-only fields.

### Changed files

| File | Change |
|---|---|
| `pages/PropertyDetailPage.tsx` | tab array gains `'units'` when `unitCount > 1`; Info renders the dwelling section only when `unitCount === 1` |
| `components/properties/PropertyCharacteristicsForm.tsx` | dwelling fields extracted to `UnitCharacteristicsForm` |
| `components/properties/PropertyForm.tsx`, `pages/PropertyCreatePage.tsx`, `pages/PropertyEditPage.tsx` | dwelling fields submit as the inline `unit` object |
| `components/properties/PropertyCard.tsx`, `PropertyCell.tsx`, `pages/PropertyListPage.tsx` | status derived: "3/4 let" badge for multi-unit, plain status for single |
| `pages/ContractCreatePage.tsx` | property → unit picker; auto-resolved and hidden when the property has one unit |
| `pages/ExpenseCreatePage.tsx`, `components/properties/PropertyExpensesTab.tsx` | building-level vs unit-level target; building expenses show their split |
| `pages/RentIncreaseWizardPage.tsx` | lists units, not buildings |
| WWS calculator | reads unit attributes; one calculation per unit |
| `components/properties/dashboard/*`, `PortfolioDashboard*` | occupancy per unit, building vacancy rate, comparison at both levels |
| `components/properties/SelfOccupancyCard.tsx` + occupancy modals | scoped to a unit |

### The transition moment

The one genuinely new interaction is a single-unit property becoming multi-unit.
The Info tab's dwelling section header carries a quiet **"Split into multiple
units"** action, opening `BulkCreateUnitsModal`:

- count
- numbering pattern (`1–6`, `A–F`, `1.01–1.06`)
- unit type
- a read-only line: *"Your existing unit becomes #1 and keeps its contract, photos
  and history."*

On save the page re-renders in multi-unit mode with a one-time explainer that
dwelling details have moved to the Units tab. That sentence is the only place the
landlord is told their data did not vanish somewhere unfindable.

### i18n

New `units` namespace across all 13 locales (~40 keys), registered in the `ns`
array in `frontend/app/src/i18n/index.ts`; plus edits to the existing `properties`
namespace where dwelling labels move. Canonical terms: en Unit, nl Woning, de
Wohnung, fr Logement, pt Fração, es Vivienda, it Unità, sv Lägenhet, fi, el, pl,
da, nb to follow the same "local legal term" rule.

## 5. Exports, booklets, takeout, demo data

### Booklets and tabular exports

- `PropertySummaryAssembler` and `PropertyBookletExporter` resolve dwelling fields
  through units. Multi-unit properties gain a **Units section**: a summary table
  plus one detail block per unit. No separate unit booklet in this spec.
- New `UnitCsvExporter` and `UnitExcelExporter`, following the existing exporter
  pair pattern.
- `ExpenseCsvExporter` and `ExpenseExcelExporter` gain allocation columns
  (allocated unit, amount, basis).
- `PropertyDashboard*` and `PortfolioDashboard*` exporters carry per-unit
  occupancy and unit counts.
- `EnumLabelResolver` gains `UnitType`, `UnitStatus` and `AllocationBasis` labels
  in all 13 locales.

### Takeout

New `units.csv`, `unit-residential-details.csv`, `unit-amenities.csv`,
`expense-allocations.csv`. `properties.csv` loses dwelling columns.
`contracts.csv` gains the unit identifier. The `amenities.csv` description text
updates.

### Letters

Formal notices, rent-change notices and deposit statements are legal documents
that must identify the **dwelling**, not just the building. A notice addressed to
"Keizersgracht 12" when the tenant rents apartment 2 is defective. The letter
exporters in `buurman-letters` and their Thymeleaf templates thread the unit
designation into the address block, across all 13 document locales.

This is not in the ticket's scope list; it is a correctness consequence of the
model change and is therefore in scope here.

### Demo data

New `DemoUnitGenerator`, ordered in `DemoDataService` **before**
`DemoContractGenerator` (contracts need a `unit_id`). Portfolio shape, chosen to
exercise every path the acceptance criteria name:

| Demo property | Units |
|---|---|
| 8 × existing-style properties | 1 implicit unit each — proves single-unit landlords see no change |
| "Keizersgracht 12", Amsterdam | 6 APARTMENT, 5 let / 1 vacant → non-trivial occupancy rate |
| A 4-unit building | 4 APARTMENT, 1 let → hits the 25% occupancy criterion exactly |
| A mixed building | 2 APARTMENT + 1 PARKING + 1 STORAGE |
| A commercial building | 2 COMMERCIAL |

`DemoExpenseGenerator` grows building-level expenses: a **€1,200 roof repair** on
the 4-unit building, so the €300-per-unit criterion is literally visible in demo
data, plus annual insurance and HOA fees. Each runs through the real
`ExpenseAllocationService` rather than hand-written rows, so the demo exercises
the production allocation path.

## 6. Testing

| Layer | Coverage |
|---|---|
| `UnitServiceTest` | last-unit delete → 409; implicit promotion preserves contract/photo/WWS FKs; bulk numbering patterns |
| `ExpenseAllocationServiceTest` | all four bases; largest-remainder sums exactly to the total; zero-area fallback; single-unit case |
| `UnitRepositoryIntegrationTest` | team A's units invisible to a team-B-scoped query |
| `UnitBackfillMigrationIntegrationTest` | see below |
| Vitest | `useUnits` hooks; `UnitGrid`; adaptive tab logic at `unitCount === 1` vs `> 1` |

### The migration-test wrinkle

`AbstractRepositoryIntegrationTest` applies *all* migrations once at container
start, so it cannot seed pre-V068 state. The backfill test needs its own
Testcontainer running Flyway with `target=V067`, seeding properties with dwelling
data plus contracts, occupancy periods and WWS calculations, then migrating to
V068 and asserting:

- every property has exactly one unit, flagged `is_implicit`
- every contract has a `unit_id` pointing at a unit of its own property
- no dwelling value was lost in the move
- team A's backfilled units are invisible to a team-B-scoped query

This is a new test base class, not a reuse of the existing one. Budget for it.

## Build sequence

1. Migration `V068__units.sql` + JOOQ regen + backfill integration test
2. Backend domain / DTOs / repository / mapper / service / controller + OpenAPI +
   unit tests
3. Allocation engine + tests
4. `make bundle-openapi`, `yarn generate:api`; extract `UnitCharacteristicsForm`;
   adaptive Info tab; Units tab; unit detail page; bulk create
5. Contract, expense, WWS and rent-increase flows
6. Dashboards + portfolio comparison at both levels
7. Exports, booklets, takeout, letters address block
8. Demo data generators
9. i18n across 13 locales
10. `billableUnitCount` + backoffice Units column

## Out of scope

Each needs a follow-up Linear issue.

- **ROOM / HMO** (ticket scope item 5). Deferred by decision. The model stays
  extensible — `unit_type` is a widenable enum and no nesting is introduced — but
  per-room contracts, shared common-area cost splits and student-housing flows are
  not built here.
- **Plan limit enforcement.** No billing or subscription system exists in the
  codebase: no plan tier, no limit checks anywhere. This spec settles the
  question — each unit counts, implicit units included — and exposes
  `billableUnitCount`. Enforcement belongs to a billing project.
- **`unit_id` on maintenance requests and meter readings** (part of ticket scope
  item 1). Neither `maintenance_requests` nor `meter_readings` exists in the
  schema; both are introduced by **BUUR-104**. That issue should add `unit_id`
  when it creates those tables. It cannot be done here.
