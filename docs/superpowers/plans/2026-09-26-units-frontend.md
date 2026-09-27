# Units Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface units in the app so a single-unit landlord sees the screens they see today, while a multi-unit landlord gets a Units tab, per-unit detail pages, bulk unit creation, and unit-aware contract, expense and dashboard flows.

**Architecture:** The property detail page becomes adaptive: when `unitCount === 1` the Info tab renders the sole unit's dwelling fields inline, writing through to that unit; when `unitCount > 1` the dwelling fields leave Info, a Units tab appears, and each unit gets its own route. The dwelling form is extracted once into `UnitCharacteristicsForm` and rendered in both places, so the two modes can never drift.

**Tech Stack:** React 19, TypeScript, Vite 7, TanStack React Query 5, Tailwind CSS 4, react-i18next, Orval-generated API client, Vitest + jsdom, Playwright.

**Spec:** `docs/superpowers/specs/2026-09-26-units-multi-unit-buildings-design.md`

**Plan series:** Plan 2 of 3. **Depends on plan 1** (`2026-09-26-units-backend-foundation.md`) being merged — this plan consumes its endpoints and generated client. Plan 3 covers exports, booklets, takeout, letters, demo data and i18n for the 12 non-English locales.

## PANEL AMENDMENTS — read before executing any task

A nine-specialist review found five defects in this plan and several backend changes it does not
account for. These amendments override the task text below wherever they conflict.

**A1. Task 1's import block does not compile.** The plan lists exports `listUnits, create,
bulkCreate, get, update, _delete`. The actual generated exports are `listUnits, createUnit,
bulkCreateUnits, getUnit, updateUnit, deleteUnit, getUnitResidentialDetails,
updateUnitResidentialDetails, getUnitAmenities, updateUnitAmenities`. Five of six names are wrong
and the `_delete` convention does not exist. Task 1 Step 1 also tells the implementer to suspect
plan 1 if the import fails — do not; check the generated file.

**A2. Task 1 must also add hooks and query keys for the four residential-details and amenities
endpoints.** Task 4 Step 4 requires both sections on `UnitDetailPage`, and they are the only paths
to bedroom/bathroom/furnished/pet-policy and amenity data — which `PropertyResponse` now returns
empty (see A6).

**A3. Task 3's `visibleTabs` test asserts the wrong order and would ship a regression.** The array
at `PropertyDetailPage.tsx:112-121` is `useTabState`'s *validation whitelist*; the rendered button
order (`:327-406`) is `info, dashboard, financials, photos, documents, contracts, expenses, audit`.
Driving both from one list, as Step 3 instructs, moves **Dashboard from tab 2 to tab 8** for every
existing landlord — contradicting this plan's own acceptance criterion 1. Use two constants:
`PROPERTY_TAB_IDS` (validation) and `PROPERTY_TAB_ORDER` (render, dashboard second), and assert
`visibleTabs` against the latter.

**A4. `UpdateUnitRequest` is PUT-replace: an omitted field CLEARS its column.** This is not a note,
it is the plan's largest hazard. Task 3 Step 6's specified test — *"editing the floor area and
submitting calls `onSubmit` with `{ areaValue: <new value> }`"* — codifies a destructive payload.
Backend work has since removed `allocationShare`/`wozSharePct` from the request (they had two
writers), but every remaining dwelling field is still clearable, and `heatingType`,
`energyEfficiencyRating` and `areaValue` all feed the WWS statutory rent ceiling.

**Required:** a single `unitToUpdateRequest(unit)` adapter is the *only* place an `UpdateUnitRequest`
is ever constructed, spreading the full `UnitResponse`, with a unit test that fails when a field is
added. Never construct a partial request. "The form always submits every field" is not safe in a
form with `CollapsibleSection`s, conditional category branches, and a `useEffect` seeding state
after first paint.

**A5. `UnitCharacteristicsForm` must be fully controlled, not self-submitting.** The plan's
signature `({ unit, onSubmit, disabled })` cannot compose into the property form on the Info tab —
a component owning its own submit cannot participate in a parent's. Use
`({ value, onChange, disabled })` and let each host own submission: `UnitDetailPage` wraps it in a
small self-submitting shell, `PropertyDetailPage` folds it into the existing form. The
"rendered in two places so they cannot drift" benefit survives.

Related, and unmentioned in the plan: on the Info tab one Save now fans out to
`PUT /properties/{id}` **and** `PUT /units/{id}` — two writes, no atomicity, no rollback. Either
give the tab two visually distinct cards with independent Saves ("Building" / "This home") so
partial success is legible, or add a combined endpoint. Do not hide two writes behind one button.

**A6. Backend changes this plan does not account for.** All landed after the plan was written:

- `PropertyResponse.residentialDetails` and `.amenities` are returned **empty for every property**,
  and the property-level write path is gone. Read them from the sole unit when `unitCount === 1`.
- `PropertyResponse.status` and `PropertySummary.status` no longer exist. `PropertyCard.tsx:56`
  still reads `property.status`. `PropertySummary` has no `unitCount`/`occupiedUnitCount`, so Task 6
  Step 4's occupancy badge is **unimplementable at its only call site** (`ExpensesPage.tsx:538`)
  until the backend adds them — raise that rather than working around it.
- WWS reads are now unit-scoped: `GET /units/{id}/wws/calculations` and `/latest`, with
  `/properties/{id}/wws/latest` returning **409** on a multi-unit property. The pre-fill hook and
  `WwsCalculatorModal` have been corrected backend-side and `UnitIdentifier` is now a branded type —
  so passing a property Sid is a compile error. The WWS panel currently degrades silently on a 409;
  give it the message the backend provides.
- `PropertyResponse` now carries `totalUnits`-style per-status counts; `occupied + vacant` still does
  **not** equal `unitCount` when a unit is `MAINTENANCE`/`UNDER_RENOVATION`/etc., so "3/4 let" reads
  as one unit available. Render three segments (let / available / unavailable), not a fraction.
- Unit creation and bulk-create are gated behind the **`MULTI_UNIT` feature flag**, default off.
  Read the flag and hide the Units tab and the split action rather than surfacing a 409 after a
  click.
- `POST /properties/{id}/units/bulk` — do **not** reimplement `numberingLabels` client-side for the
  preview as Task 5 does; it will drift from the server's `AA/AB` rollover and `%02d` widening. Ask
  the backend for a dry-run, or preview only the simple numeric case.

**A7. The locale term list in Global Constraints is obsolete.** "Unit localised to the local legal
term" yields a *dwelling* noun in 7 of 13 locales (nl *Woning*, de *Wohnung*, sv *Lägenhet*,
fi *Asunto*, el *Κατοικία*, da/nb *Bolig*) while `unit_type` includes `PARKING`, `STORAGE` and
`COMMERCIAL`. "Woningen (3)" on a building holding two flats and a parking space is false — and in
NL, *woning* is the term the WWS and the Huurcommissie use. Use a neutral **container** term
(nl *Eenheid*, de *Einheit*, fr *Lot*, sv/da/nb *Enhet*, fi *Yksikkö*, el *Μονάδα*, es *Unidad*;
pt *Fração*, it *Unità*, pl *Lokal* are already neutral) plus a per-type **member** label
(*Woning* / *Parkeerplaats* / *Berging* / *Bedrijfsruimte*). Fix this **before** 13 locale files are
written against the old list.

**A8. Query keys as specified are prefix-incompatible.** `k('unit', id)` and `k('units', propId)`
are separate families, so one `invalidateQueries({ queryKey: ['units'] })` will not match both. Use
`['units', propertyIdentifier]` and `['units', 'detail', unitIdentifier]`.

**A9. Avoid the three-hop waterfall.** Task 3 Step 5 goes property → `listUnits` → `getUnit` on the
single-unit page, i.e. the majority of page views. `PropertyResponse.units[0].identifier` is already
on the detail response — use it and drop `listUnits` from that path. Note `units` is `[]` on the
**list** endpoint, so guard for it rather than trusting the schema's `required`.

**A10. CI now gates this work.** `tsc --noEmit` and `yarn build` run on any change to `frontend/**`
**or** `openapi/**`. `yarn test` and `yarn lint` both pass today on a frontend with 138 type errors
(Vitest does not typecheck; ESLint is not type-aware), so they are not the gate. The 138 errors are
this branch's and clearing them is Task 0 below.

**A11. New Task 0 — clear the 138 existing type errors first.** They are all caused by the dwelling
field move: `PropertyForm.tsx` (45), `PropertyDetailPage.tsx` (44),
`PropertyCharacteristicsForm.tsx` (39), `PropertyCard.tsx` (5), `types/property.ts` (3),
`PropertyListPage.tsx` (1), `usePropertyHooks.ts` (1). Note `PropertyCharacteristicsForm` renders
every field **twice** (six view-mode plus six edit-mode `CollapsibleSection`s), so the deletion is
~550 lines, not ~150 — and those four files have **no test coverage at all** (5,063 lines, zero
test files). Before deleting, add a smoke render test per file asserting the section headings that
must survive (`detail.construction.title`, `detail.utilities.title`, `detail.parking.title`,
`detail.safety.title`). That is the only thing that will catch a stray `</CollapsibleSection>`
taking the parking section with it.

---

## Global Constraints

- Airbnb JS style; the repo is prettier-clean and must stay that way (`yarn lint --fix` before every commit).
- Generated API clients live in `frontend/app/src/generated/api/<tag>/<tag>.ts` and are **gitignored** — run `yarn generate:api` after any OpenAPI change; never hand-edit generated files.
- React Query keys come from `frontend/app/src/lib/queryKeys.ts`. Add new keys there, never inline literals.
- Mutations use `useMutationWithToast`, and invalidate every affected key — a unit mutation touches units, the parent property, the property list and dashboard stats.
- Locale files are `frontend/app/public/locales/<lng>/<ns>.json`, 13 languages: `en nl pt es fr de it sv fi el pl da nb`. This plan writes **`en` only**; plan 3 does the other 12.
- New i18n namespace `units` must be registered in the `ns` array in `frontend/app/src/i18n/index.ts`.
- Canonical unit terms (for plan 3, recorded here so they are decided once): en Unit, nl Woning, de Wohnung, fr Logement, pt Fração, es Vivienda, it Unità, sv Lägenhet, fi Asunto, el Κατοικία, pl Lokal, da Bolig, nb Bolig.
- Never render an internal UUID. Units are addressed by `identifier` (Sid).
- Tests: `cd frontend && yarn test`. Lint: `yarn lint`.

## Review Focus

Five input classes the spec implies but that no happy-path test would exercise. Each has its test assigned to the owning task.

1. **A property whose `unitCount` is 1 but whose sole unit is *not* implicit** — the landlord split into units then deleted back down to one. The Info tab must still render inline (driven by `unitCount`, never by `implicit`), otherwise dwelling fields vanish from the UI with no way to reach them. → Task 3.
2. **A 50-unit building** — the Units grid and the bulk-create modal must stay usable and must not fire one request per unit. The spec's positioning is "1 to 50 units", so 50 is a real case, not a stress test. → Task 4.
3. **Switching a property from 1 unit to many while the Info tab is open** — stale React Query cache would leave dwelling fields rendered inline against a now-multi-unit property, letting the landlord edit a unit the form no longer identifies. → Task 5.
4. **A contract create flow reached from a deep link with a pre-selected property that has several units** — no unit is chosen and submit must be blocked with a message naming the choice, not a backend 400 surfaced raw. → Task 6.
5. **A unit whose `areaValue` is absent under an AREA-basis building** — the allocation preview must show the zero share honestly rather than rendering `NaN%` or a blank cell. → Task 7.

---

## File Structure

**New**
- `frontend/app/src/hooks/useUnitHooks.ts` — queries and mutations for units.
- `frontend/app/src/types/unit.ts` — hand-written request/response types re-exported from generated ones, following `types/occupancyPeriod.ts`.
- `frontend/app/src/components/units/UnitCharacteristicsForm.tsx` — the dwelling form, rendered in two places.
- `frontend/app/src/components/units/PropertyUnitsTab.tsx` — the Units tab shell.
- `frontend/app/src/components/units/UnitGrid.tsx` — unit · tenant · rent · status · vacancy days.
- `frontend/app/src/components/units/UnitCell.tsx` — a unit's compact representation, mirroring `PropertyCell`.
- `frontend/app/src/components/units/BulkCreateUnitsModal.tsx` — count, numbering, type, promotion notice.
- `frontend/app/src/components/units/UnitAllocationSettings.tsx` — basis picker and per-unit custom shares.
- `frontend/app/src/pages/UnitDetailPage.tsx` — route `/properties/:id/units/:unitId`.

**Modified**
- `frontend/app/src/lib/queryKeys.ts` — a `units` key family.
- `frontend/app/src/i18n/index.ts` — register the `units` namespace.
- `frontend/app/public/locales/en/units.json` — new.
- `frontend/app/public/locales/en/properties.json` — dwelling labels move out; unit-count copy moves in.
- `frontend/app/src/pages/PropertyDetailPage.tsx` — adaptive tabs.
- `frontend/app/src/components/properties/PropertyCharacteristicsForm.tsx` — shrinks to building-only.
- `frontend/app/src/components/properties/PropertyForm.tsx`, `pages/PropertyCreatePage.tsx`, `pages/PropertyEditPage.tsx` — submit the inline `unit`.
- `frontend/app/src/components/properties/PropertyCard.tsx`, `PropertyCell.tsx`, `pages/PropertyListPage.tsx` — derived status.
- `frontend/app/src/pages/ContractCreatePage.tsx` — unit picker.
- `frontend/app/src/pages/ExpenseCreatePage.tsx`, `components/properties/PropertyExpensesTab.tsx` — building vs unit target, allocation display.
- `frontend/app/src/pages/RentIncreaseWizardPage.tsx` — lists units.
- `frontend/app/src/components/properties/dashboard/*`, `usePortfolioDashboard.ts` consumers — per-unit occupancy.
- `frontend/app/src/components/properties/SelfOccupancyCard.tsx` and the occupancy modals — unit-scoped.
- `frontend/app/src/App.tsx` — the unit detail route.

---

## Task 1: Generate the client, add query keys and unit hooks

**Files:**
- Create: `frontend/app/src/types/unit.ts`
- Create: `frontend/app/src/hooks/useUnitHooks.ts`
- Modify: `frontend/app/src/lib/queryKeys.ts`

**Interfaces:**
- Consumes: plan 1's `openapi/app.yaml` (the `units` paths).
- Produces:
  - `queryKeys.units.all(propertyIdentifier)`, `queryKeys.units.detail(unitIdentifier)`, `queryKeys.units.allocation(expenseIdentifier)`
  - `useUnits(propertyIdentifier?: string)` → `UseQueryResult<UnitGridRow[]>`
  - `useUnit(unitIdentifier?: string)` → `UseQueryResult<Unit>`
  - `useCreateUnit(propertyIdentifier: string)`, `useBulkCreateUnits(propertyIdentifier: string)`, `useUpdateUnit(propertyIdentifier: string, unitIdentifier: string)`, `useDeleteUnit(propertyIdentifier: string)`

- [ ] **Step 1: Regenerate the API client**

Run: `cd frontend && yarn install && yarn generate:api`

Expected: `frontend/app/src/generated/api/units/units.ts` exists and exports `listUnits`, `create`, `bulkCreate`, `get`, `update`, `_delete`. Confirm:

Run: `grep -o "export const [a-zA-Z_]*" frontend/app/src/generated/api/units/units.ts`

If the file is missing, plan 1's OpenAPI work is not merged — stop and resolve that first rather than hand-writing the client.

- [ ] **Step 2: Add the query keys**

In `frontend/app/src/lib/queryKeys.ts`, alongside the existing `properties` and `occupancyPeriods` families:

```ts
  units: {
    all: (propertyIdentifier?: string) => k('units', propertyIdentifier),
    detail: (unitIdentifier?: string) => k('unit', unitIdentifier),
    allocation: (expenseIdentifier?: string) =>
      k('unitAllocation', expenseIdentifier),
  },
```

- [ ] **Step 3: Write the hooks**

Follow `useOccupancyPeriodHooks.ts` exactly — same import style, same `useMutationWithToast` usage, same invalidation breadth.

```ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMutationWithToast } from './useMutationWithToast';
import {
  listUnits,
  get as getUnit,
  create as createUnit,
  bulkCreate as bulkCreateUnits,
  update as updateUnit,
  _delete as deleteUnit,
} from '../generated/api/units/units';
import type {
  CreateUnitRequest,
  UpdateUnitRequest,
  BulkCreateUnitsRequest,
} from '../types/unit';
import { queryKeys } from '../lib/queryKeys';

export const useUnits = (propertyIdentifier: string | undefined) =>
  useQuery({
    queryKey: queryKeys.units.all(propertyIdentifier),
    queryFn: () => listUnits(propertyIdentifier ?? ''),
    enabled: !!propertyIdentifier,
  });

export const useUnit = (unitIdentifier: string | undefined) =>
  useQuery({
    queryKey: queryKeys.units.detail(unitIdentifier),
    queryFn: () => getUnit(unitIdentifier ?? ''),
    enabled: !!unitIdentifier,
  });

/**
 * Invalidation is deliberately broad: adding or removing a unit changes the
 * parent property's unitCount (which drives whether the Units tab exists at
 * all), its occupancy figures, the property list badges and dashboard stats.
 */
const invalidateUnitScope = (
  queryClient: ReturnType<typeof useQueryClient>,
  propertyIdentifier: string
) => {
  queryClient.invalidateQueries({
    queryKey: queryKeys.units.all(propertyIdentifier),
  });
  queryClient.invalidateQueries({
    queryKey: queryKeys.properties.detail(propertyIdentifier),
  });
  queryClient.invalidateQueries({ queryKey: queryKeys.properties.all() });
  queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.stats() });
};

export const useCreateUnit = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit created',
    mutationFn: (data: CreateUnitRequest) =>
      createUnit(propertyIdentifier, data),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};

export const useBulkCreateUnits = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Units created',
    mutationFn: (data: BulkCreateUnitsRequest) =>
      bulkCreateUnits(propertyIdentifier, data),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};

export const useUpdateUnit = (
  propertyIdentifier: string,
  unitIdentifier: string
) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit updated',
    mutationFn: (data: UpdateUnitRequest) => updateUnit(unitIdentifier, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.units.detail(unitIdentifier),
      });
      invalidateUnitScope(queryClient, propertyIdentifier);
    },
  });
};

export const useDeleteUnit = (propertyIdentifier: string) => {
  const queryClient = useQueryClient();
  return useMutationWithToast({
    successMessage: 'Unit deleted',
    mutationFn: (unitIdentifier: string) => deleteUnit(unitIdentifier),
    onSuccess: () => invalidateUnitScope(queryClient, propertyIdentifier),
  });
};
```

`types/unit.ts` re-exports the generated types under stable names, following `types/occupancyPeriod.ts`.

- [ ] **Step 4: Typecheck and lint**

Run: `cd frontend && yarn build && yarn lint`

Expected: both clean.

- [ ] **Step 5: Commit**

```bash
git add frontend/app/src/hooks/useUnitHooks.ts frontend/app/src/types/unit.ts frontend/app/src/lib/queryKeys.ts
git commit -m "feat(frontend): add unit query keys and React Query hooks (BUUR-106)"
```

---

## Task 2: The `units` i18n namespace (English)

Done before any component so no component invents an inline string.

**Files:**
- Create: `frontend/app/public/locales/en/units.json`
- Modify: `frontend/app/src/i18n/index.ts`
- Modify: `frontend/app/public/locales/en/properties.json`

**Interfaces:**
- Produces: the `units` namespace, consumed by every later task via `useTranslation('units')`.

- [ ] **Step 1: Register the namespace**

In `frontend/app/src/i18n/index.ts`, add `'units'` to the `ns` array after `'properties'`.

- [ ] **Step 2: Write the English namespace**

Create `frontend/app/public/locales/en/units.json`. Keys are grouped the way `properties.json` groups its own:

```json
{
  "term": {
    "one": "Unit",
    "other": "Units"
  },
  "tab": "Units",
  "grid": {
    "unit": "Unit",
    "tenant": "Tenant",
    "rent": "Rent",
    "status": "Status",
    "vacancyDays": "Vacant for",
    "vacancyDaysValue": "{{count}} day",
    "vacancyDaysValue_other": "{{count}} days",
    "noTenant": "—",
    "empty": "No units yet.",
    "occupancySummary": "{{occupied}} of {{total}} let"
  },
  "detail": {
    "title": "Unit {{number}}",
    "backToProperty": "Back to {{street}}",
    "characteristics": "Characteristics",
    "amenities": "Amenities",
    "residential": "Rooms & furnishing"
  },
  "fields": {
    "unitNumber": "Unit number",
    "name": "Name",
    "floor": "Floor",
    "unitType": "Type",
    "status": "Status",
    "areaValue": "Floor area",
    "allocationShare": "Cost share",
    "wozSharePct": "WOZ share",
    "energyEfficiencyRating": "Energy label",
    "heatingType": "Heating",
    "bedrooms": "Bedrooms",
    "bathrooms": "Bathrooms",
    "furnished": "Furnished",
    "petPolicy": "Pet policy"
  },
  "type": {
    "APARTMENT": "Apartment",
    "PARKING": "Parking space",
    "STORAGE": "Storage",
    "COMMERCIAL": "Commercial space"
  },
  "status": {
    "VACANT": "Vacant",
    "OCCUPIED": "Let",
    "SELF_OCCUPIED": "Self-occupied",
    "MAINTENANCE": "Maintenance",
    "UNAVAILABLE": "Unavailable",
    "UNDER_RENOVATION": "Under renovation",
    "FALLOW": "Fallow",
    "LISTED": "Listed"
  },
  "split": {
    "action": "Split into multiple units",
    "title": "Split {{street}} into units",
    "count": "How many units?",
    "numbering": "Numbering",
    "numberingPattern": {
      "NUMERIC": "1, 2, 3 …",
      "ALPHABETIC": "A, B, C …",
      "FLOOR_DOT_INDEX": "1.01, 1.02, 1.03 …"
    },
    "startFloor": "Starting floor",
    "unitType": "Unit type",
    "keepsHistory": "Your existing unit becomes #1 and keeps its contract, photos and history.",
    "submit": "Create units",
    "movedExplainer": "Dwelling details now live on each unit. Open the Units tab to edit them."
  },
  "allocation": {
    "title": "Cost allocation",
    "basis": "Split building costs by",
    "basisOption": {
      "AREA": "Floor area",
      "EQUAL": "Equally",
      "CUSTOM": "Custom percentages",
      "MANUAL": "Manually per expense"
    },
    "share": "Share",
    "noArea": "No floor area set",
    "preview": "Each unit's share of {{amount}}",
    "customTotalError": "Custom shares must add up to 100%. They currently total {{total}}%."
  },
  "delete": {
    "confirmTitle": "Delete unit {{number}}?",
    "lastUnitBlocked": "A property must keep at least one unit, so this one cannot be deleted.",
    "activeContractBlocked": "This unit has an active contract. End the contract first."
  },
  "picker": {
    "label": "Unit",
    "placeholder": "Choose a unit",
    "required": "Choose which unit this contract is for."
  }
}
```

- [ ] **Step 3: Move dwelling labels out of properties.json**

`frontend/app/public/locales/en/properties.json` keeps building labels. Add the unit-count copy it now needs:

```json
    "unitCount": "{{count}} unit",
    "unitCount_other": "{{count}} units",
    "occupancyBadge": "{{occupied}}/{{total}} let"
```

Leave the existing dwelling keys in place for now — Task 3 removes each one as its field moves, so nothing is deleted before its last reader is gone.

- [ ] **Step 4: Verify the namespace loads**

Run: `cd frontend && yarn dev`, open a property page, and confirm the browser network tab fetches `/locales/en/units.json` with a 200. Stop the dev server.

- [ ] **Step 5: Commit**

```bash
git add frontend/app/public/locales/en/units.json frontend/app/public/locales/en/properties.json frontend/app/src/i18n/index.ts
git commit -m "feat(frontend): add English units i18n namespace (BUUR-106)"
```

---

## Task 3: Extract UnitCharacteristicsForm and make the Info tab adaptive

The heart of the "existing teams see identical screens" criterion. Owns Review Focus item 1.

**Files:**
- Create: `frontend/app/src/components/units/UnitCharacteristicsForm.tsx`
- Create: `frontend/app/src/components/units/__tests__/UnitCharacteristicsForm.test.tsx`
- Modify: `frontend/app/src/components/properties/PropertyCharacteristicsForm.tsx`
- Modify: `frontend/app/src/pages/PropertyDetailPage.tsx`
- Create: `frontend/app/src/pages/__tests__/PropertyDetailPage.tabs.test.tsx`

**Interfaces:**
- Consumes: `useUnit`, `useUpdateUnit` (Task 1); the `units` namespace (Task 2).
- Produces:
  - `UnitCharacteristicsForm({ unit, onSubmit, disabled }: { unit: Unit; onSubmit: (values: UpdateUnitRequest) => void; disabled?: boolean })`
  - `visibleTabs(unitCount: number): readonly TabId[]` exported from `PropertyDetailPage.tsx` for direct testing.

- [ ] **Step 1: Write the failing tab-visibility test**

The assertion that matters is that `unitCount` alone drives the tab list — never the `implicit` flag. That is Review Focus item 1.

```tsx
import { describe, expect, it } from 'vitest';
import { visibleTabs } from '../PropertyDetailPage';

describe('visibleTabs', () => {
  it('omits the Units tab for a single-unit property', () => {
    expect(visibleTabs(1)).not.toContain('units');
  });

  it('includes the Units tab once a property has more than one unit', () => {
    expect(visibleTabs(4)).toContain('units');
  });

  it('places Units directly after Info', () => {
    const tabs = visibleTabs(4);
    expect(tabs.indexOf('units')).toBe(tabs.indexOf('info') + 1);
  });

  it('keeps every pre-existing tab in its original order', () => {
    expect(visibleTabs(1)).toEqual([
      'info',
      'financials',
      'photos',
      'documents',
      'contracts',
      'expenses',
      'audit',
      'dashboard',
    ]);
  });
});
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd frontend && yarn test --run UnitCharacteristicsForm PropertyDetailPage.tabs`

Expected: FAIL — `visibleTabs` is not exported.

- [ ] **Step 3: Implement visibleTabs and wire the adaptive tab bar**

In `PropertyDetailPage.tsx`, export the helper and drive both `useTabState` and the rendered tab buttons from it:

```tsx
export const PROPERTY_TAB_IDS = [
  'info',
  'financials',
  'photos',
  'documents',
  'contracts',
  'expenses',
  'audit',
  'dashboard',
] as const;

export type TabId = (typeof PROPERTY_TAB_IDS)[number] | 'units';

/**
 * Driven by unitCount, never by the implicit flag: a landlord who split a
 * property into units and later deleted back down to one must still reach the
 * dwelling fields on the Info tab.
 */
export const visibleTabs = (unitCount: number): readonly TabId[] => {
  if (unitCount <= 1) {
    return PROPERTY_TAB_IDS;
  }
  const [info, ...rest] = PROPERTY_TAB_IDS;
  return [info, 'units', ...rest];
};
```

`useTabState('info', visibleTabs(property?.unitCount ?? 1))`. When the active tab disappears (the landlord deleted units back to one while on the Units tab), fall back to `'info'`.

- [ ] **Step 4: Extract the dwelling form**

Move the dwelling field groups out of `PropertyCharacteristicsForm.tsx` into `UnitCharacteristicsForm.tsx`: floor area, energy label and certificate expiry, heating/cooling/hot water/insulation, flooring and window type, smoke/CO detectors and fire extinguisher, adapted bathroom and accessibility notes, plus bedrooms/bathrooms/furnished/pet policy. Keep the existing `CollapsibleSection` layout and the `enums.characteristics.*` label lookup so the rendered output is unchanged for a single-unit property.

`PropertyCharacteristicsForm` keeps: construction and structure, connections, building safety (sprinkler/alarm/cameras/secure entry), elevator and step-free entrance, parking count and type.

- [ ] **Step 5: Render it in both modes**

In `PropertyDetailPage.tsx`'s Info tab:

```tsx
{property.unitCount === 1 && soleUnit && (
  <UnitCharacteristicsForm
    unit={soleUnit}
    onSubmit={(values) => updateUnit.mutate(values)}
    disabled={!canEditData}
  />
)}
```

where `soleUnit` comes from `useUnits(id)` — the single grid row's identifier fed to `useUnit`. On `UnitDetailPage` (Task 4) the same component renders with the routed unit.

- [ ] **Step 6: Write the form test**

`UnitCharacteristicsForm.test.tsx` asserts that editing the floor area and submitting calls `onSubmit` with `{ areaValue: <new value> }`, that `disabled` renders every input disabled, and that a unit with `areaValue: null` renders an empty input rather than `NaN` or `"null"`.

- [ ] **Step 7: Run the tests**

Run: `cd frontend && yarn test --run`

Expected: PASS, including the pre-existing suite.

- [ ] **Step 8: Lint and commit**

```bash
cd frontend && yarn lint --fix
git add frontend/app/src/components/units/ frontend/app/src/components/properties/PropertyCharacteristicsForm.tsx frontend/app/src/pages/PropertyDetailPage.tsx frontend/app/src/pages/__tests__/
git commit -m "feat(frontend): adaptive Info tab and extracted UnitCharacteristicsForm (BUUR-106)"
```

---

## Task 4: Units tab, unit grid and unit detail page

Owns Review Focus item 2 (a 50-unit building).

**Files:**
- Create: `frontend/app/src/components/units/UnitGrid.tsx`
- Create: `frontend/app/src/components/units/UnitCell.tsx`
- Create: `frontend/app/src/components/units/PropertyUnitsTab.tsx`
- Create: `frontend/app/src/pages/UnitDetailPage.tsx`
- Create: `frontend/app/src/components/units/__tests__/UnitGrid.test.tsx`
- Modify: `frontend/app/src/App.tsx`
- Modify: `frontend/app/src/pages/PropertyDetailPage.tsx`

**Interfaces:**
- Consumes: `useUnits`, `useUnit`, `useDeleteUnit` (Task 1); `UnitCharacteristicsForm` (Task 3).
- Produces: route `/properties/:id/units/:unitId`; `UnitGrid({ propertyIdentifier, rows, onRowClick })`.

- [ ] **Step 1: Write the failing grid test**

```tsx
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { UnitGrid } from '../UnitGrid';

const row = (overrides = {}) => ({
  identifier: 'UNT01HQJK4B2X5M3N7P8Q9R0S1T',
  unitNumber: '1',
  name: null,
  unitType: 'APARTMENT',
  status: 'VACANT',
  tenantName: null,
  monthlyRent: null,
  vacancyDays: 42,
  ...overrides,
});

describe('UnitGrid', () => {
  it('renders one row per unit', () => {
    render(<UnitGrid propertyIdentifier="PRO1" rows={[row(), row({ unitNumber: '2' })]} />);
    expect(screen.getAllByRole('row')).toHaveLength(3); // header + 2
  });

  it('shows a dash rather than an empty cell when a unit has no tenant', () => {
    render(<UnitGrid propertyIdentifier="PRO1" rows={[row()]} />);
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('shows vacancy days only for unlet units', () => {
    render(
      <UnitGrid
        propertyIdentifier="PRO1"
        rows={[row({ status: 'OCCUPIED', tenantName: 'A. Tenant', vacancyDays: null })]}
      />
    );
    expect(screen.queryByText(/day/)).not.toBeInTheDocument();
  });

  it('renders 50 units without collapsing', () => {
    const rows = Array.from({ length: 50 }, (_, i) => row({ unitNumber: String(i + 1) }));
    render(<UnitGrid propertyIdentifier="PRO1" rows={rows} />);
    expect(screen.getAllByRole('row')).toHaveLength(51);
  });
});
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd frontend && yarn test --run UnitGrid`

Expected: FAIL — module not found.

- [ ] **Step 3: Implement the grid and tab**

`UnitGrid` is a plain table, horizontally scrollable on phone the way the tab bar already is. It takes rows as a prop and fires **no** requests of its own — `PropertyUnitsTab` owns the single `useUnits` call. That is what keeps a 50-unit building at one request.

`PropertyUnitsTab` renders the occupancy summary (`grid.occupancySummary`), an "Add unit" button, and the grid; row click navigates to the unit detail route.

- [ ] **Step 4: Add the unit detail page and route**

`UnitDetailPage` reads `:id` and `:unitId` from `useParams`, calls `useUnit`, and renders a back link to the property plus `UnitCharacteristicsForm`, the residential-details section and the amenities section. Register in `App.tsx` beside the existing property routes:

```tsx
<Route path="/properties/:id/units/:unitId" element={<UnitDetailPage />} />
```

- [ ] **Step 5: Run the tests, lint, commit**

Run: `cd frontend && yarn test --run && yarn lint --fix`

```bash
git add frontend/app/src/components/units/ frontend/app/src/pages/UnitDetailPage.tsx frontend/app/src/App.tsx frontend/app/src/pages/PropertyDetailPage.tsx
git commit -m "feat(frontend): units tab, unit grid and unit detail page (BUUR-106)"
```

---

## Task 5: Bulk-create modal and the split transition

Owns Review Focus item 3 (stale cache across the 1 → many transition).

**Files:**
- Create: `frontend/app/src/components/units/BulkCreateUnitsModal.tsx`
- Create: `frontend/app/src/components/units/__tests__/BulkCreateUnitsModal.test.tsx`
- Modify: `frontend/app/src/components/units/UnitCharacteristicsForm.tsx` — the split action in its header
- Modify: `frontend/app/src/pages/PropertyDetailPage.tsx`

**Interfaces:**
- Consumes: `useBulkCreateUnits` (Task 1).
- Produces: `BulkCreateUnitsModal({ propertyIdentifier, street, open, onClose, onCreated })`.

- [ ] **Step 1: Write the failing modal tests**

```tsx
describe('BulkCreateUnitsModal', () => {
  it('previews the numbering before submitting', async () => {
    renderModal();
    await userEvent.clear(screen.getByLabelText('How many units?'));
    await userEvent.type(screen.getByLabelText('How many units?'), '3');
    expect(screen.getByText(/1, 2, 3/)).toBeInTheDocument();
  });

  it('always states that the existing unit keeps its history', () => {
    renderModal();
    expect(
      screen.getByText(/becomes #1 and keeps its contract, photos and history/)
    ).toBeInTheDocument();
  });

  it('submits count, pattern and type in one request', async () => {
    const mutate = vi.fn();
    renderModal({ mutate });
    await userEvent.type(screen.getByLabelText('How many units?'), '6');
    await userEvent.click(screen.getByRole('button', { name: 'Create units' }));
    expect(mutate).toHaveBeenCalledTimes(1);
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ count: 6, numberingPattern: 'NUMERIC' })
    );
  });

  it('rejects a count below 1', async () => {
    renderModal();
    await userEvent.clear(screen.getByLabelText('How many units?'));
    await userEvent.type(screen.getByLabelText('How many units?'), '0');
    expect(screen.getByRole('button', { name: 'Create units' })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Run to verify it fails, then implement**

Run: `cd frontend && yarn test --run BulkCreateUnitsModal`

The modal fires **one** `bulkCreate` request, never a loop of `create` calls. The promotion notice (`split.keepsHistory`) is unconditional text, not a tooltip — it is the only place the landlord learns their contract survives.

- [ ] **Step 3: Handle the transition cleanly**

`onCreated` must, in order: invalidate via the Task 1 hook's `onSuccess`, then set the active tab to `'units'`, then show the `split.movedExplainer` notice once. Because `unitCount` is now > 1, `visibleTabs` re-renders with the Units tab and the Info tab stops rendering `UnitCharacteristicsForm` — which is exactly the stale-cache hazard in Review Focus item 3. Add a test asserting that after a successful bulk create the Info tab no longer renders the dwelling form:

```tsx
it('stops rendering dwelling fields inline once the property has several units', async () => {
  const { rerender } = renderPropertyDetail({ unitCount: 1 });
  expect(screen.getByLabelText('Floor area')).toBeInTheDocument();
  rerender(propertyDetailWith({ unitCount: 6 }));
  expect(screen.queryByLabelText('Floor area')).not.toBeInTheDocument();
});
```

- [ ] **Step 4: Run, lint, commit**

Run: `cd frontend && yarn test --run && yarn lint --fix`

```bash
git add frontend/app/src/components/units/ frontend/app/src/pages/PropertyDetailPage.tsx
git commit -m "feat(frontend): bulk-create units modal and split transition (BUUR-106)"
```

---

## Task 6: Property create/edit, list badges and the contract unit picker

Owns Review Focus item 4.

**Files:**
- Modify: `frontend/app/src/components/properties/PropertyForm.tsx`, `pages/PropertyCreatePage.tsx`, `pages/PropertyEditPage.tsx`
- Modify: `frontend/app/src/components/properties/PropertyCard.tsx`, `PropertyCell.tsx`, `pages/PropertyListPage.tsx`
- Modify: `frontend/app/src/pages/ContractCreatePage.tsx`
- Create: `frontend/app/src/pages/__tests__/ContractCreatePage.unitPicker.test.tsx`

**Interfaces:**
- Consumes: `useUnits` (Task 1); `UnitCharacteristicsForm` (Task 3).
- Produces: `CreatePropertyRequest.unit` populated from the create form; a unit `<select>` on the contract create page.

- [ ] **Step 1: Write the failing unit-picker tests**

```tsx
describe('ContractCreatePage unit picker', () => {
  it('hides the picker when the property has one unit', async () => {
    renderContractCreate({ units: [unit('1')] });
    expect(screen.queryByLabelText('Unit')).not.toBeInTheDocument();
  });

  it('shows the picker when the property has several units', async () => {
    renderContractCreate({ units: [unit('1'), unit('2')] });
    expect(await screen.findByLabelText('Unit')).toBeInTheDocument();
  });

  it('blocks submit with a message when no unit is chosen', async () => {
    renderContractCreate({ units: [unit('1'), unit('2'), unit('3')] });
    await userEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(
      screen.getByText('Choose which unit this contract is for.')
    ).toBeInTheDocument();
  });

  it('sends the chosen unitIdentifier', async () => {
    const mutate = vi.fn();
    renderContractCreate({ units: [unit('1'), unit('2')], mutate });
    await userEvent.selectOptions(await screen.findByLabelText('Unit'), 'UNT2');
    await userEvent.click(screen.getByRole('button', { name: /create/i }));
    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ unitIdentifier: 'UNT2' })
    );
  });
});
```

The third test is Review Focus item 4: the block is client-side with the `picker.required` copy, so a deep link with a pre-selected multi-unit property never reaches the backend to earn a raw 400.

- [ ] **Step 2: Run to verify they fail, then implement**

Run: `cd frontend && yarn test --run ContractCreatePage`

When `units.length === 1`, submit `unitIdentifier: units[0].identifier` and render no picker — the backend would resolve it anyway, but sending it explicitly keeps the request unambiguous.

- [ ] **Step 3: Property create and edit**

The create form's dwelling section now populates `request.unit` (a `CreateUnitRequest`) rather than top-level property fields. Reuse `UnitCharacteristicsForm`'s field set so the create form and the unit detail page ask for the same things in the same order.

The edit page drops dwelling fields entirely: for a single-unit property they are edited on the Info tab, for multi-unit on the unit page.

- [ ] **Step 4: Derived status on cards and list rows**

`PropertyCard` and `PropertyCell` render `occupancyBadge` (`{{occupied}}/{{total}} let`) when `unitCount > 1`, and the single unit's translated status otherwise. Any list filter previously keyed on property status now filters on "has a unit with this status" — the backend query from plan 1 Task 12 already does this; the frontend only passes the parameter.

- [ ] **Step 5: Run, lint, commit**

Run: `cd frontend && yarn test --run && yarn lint --fix`

```bash
git add frontend/app/src/components/properties/ frontend/app/src/pages/
git commit -m "feat(frontend): unit-aware property forms, list badges and contract picker (BUUR-106)"
```

---

## Task 7: Expenses, allocation settings and dashboards

Owns Review Focus item 5.

**Files:**
- Create: `frontend/app/src/components/units/UnitAllocationSettings.tsx`
- Create: `frontend/app/src/components/units/__tests__/UnitAllocationSettings.test.tsx`
- Modify: `frontend/app/src/pages/ExpenseCreatePage.tsx`
- Modify: `frontend/app/src/components/properties/PropertyExpensesTab.tsx`
- Modify: `frontend/app/src/components/properties/dashboard/*`
- Modify: `frontend/app/src/pages/RentIncreaseWizardPage.tsx`
- Modify: `frontend/app/src/components/properties/SelfOccupancyCard.tsx` and the occupancy modals

**Interfaces:**
- Consumes: `useUnits` (Task 1); plan 1's allocation endpoints.
- Produces: `UnitAllocationSettings({ propertyIdentifier, basis, units, onSave })`.

- [ ] **Step 1: Write the failing allocation-settings tests**

```tsx
describe('UnitAllocationSettings', () => {
  it('shows a share column only for CUSTOM basis', async () => {
    renderSettings({ basis: 'EQUAL' });
    expect(screen.queryByLabelText('Share')).not.toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText(/Split building costs by/), 'CUSTOM');
    expect(screen.getAllByLabelText('Share').length).toBeGreaterThan(0);
  });

  it('blocks saving CUSTOM shares that do not total 100', async () => {
    renderSettings({ basis: 'CUSTOM', units: [unitWithShare('1', 60), unitWithShare('2', 30)] });
    expect(
      screen.getByText('Custom shares must add up to 100%. They currently total 90%.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
  });

  it('labels a unit with no floor area under AREA basis instead of rendering NaN', () => {
    renderSettings({ basis: 'AREA', units: [unitWithArea('1', 100), unitWithArea('2', null)] });
    expect(screen.getByText('No floor area set')).toBeInTheDocument();
    expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
  });

  it('previews each unit share of a sample amount', () => {
    renderSettings({ basis: 'EQUAL', units: [unitWithArea('1', 50), unitWithArea('2', 50)] });
    expect(screen.getByText(/Each unit's share/)).toBeInTheDocument();
  });
});
```

The third test is Review Focus item 5.

- [ ] **Step 2: Run to verify they fail, then implement**

Run: `cd frontend && yarn test --run UnitAllocationSettings`

The share preview mirrors the backend's largest-remainder split closely enough to be honest, but it is labelled a preview — the stored rows from plan 1 are the source of truth, and `PropertyExpensesTab` displays those, not a recomputation.

- [ ] **Step 3: Expense create and display**

`ExpenseCreatePage` gains a target choice: *the whole building* (allocated) or *one unit* (direct). Choosing a unit sets `unitIdentifier` and suppresses allocation. `PropertyExpensesTab` shows, for each building-level expense, an expandable per-unit split read from `GET /expenses/{id}/allocations`.

- [ ] **Step 4: Dashboards, rent-increase wizard, occupancy**

- Property dashboard: occupancy per unit, plus building vacancy rate.
- Portfolio comparison: a unit-level view alongside the property-level one.
- `RentIncreaseWizardPage`: the selectable list becomes units, showing each unit's current rent and its property as context. A building with 6 units offers 6 rows, not 1.
- `SelfOccupancyCard` and the occupancy modals take a unit. On a single-unit property this is invisible; on a multi-unit one the card moves to the unit detail page.

- [ ] **Step 5: Run the full suite, lint, commit**

Run: `cd frontend && yarn test --run && yarn build && yarn lint`

Expected: all clean.

```bash
git add frontend/
git commit -m "feat(frontend): unit-level expenses, allocation settings and dashboards (BUUR-106)"
```

---

## Task 8: Visual regression and a real run-through

**Files:**
- Modify: `frontend/app/tests/visual/public-routes.spec.ts` if any snapshot shifted.

- [ ] **Step 1: Run the whole frontend suite**

Run: `cd frontend && yarn test && yarn build && yarn lint`

Expected: green. Record the test count against the 105 baseline in CLAUDE.md plus this plan's additions.

- [ ] **Step 2: Run the visual regression suite**

Run: `cd frontend/app && yarn playwright test tests/visual/public-routes.spec.ts`

Expected: PASS unchanged — these cover public routes only, and nothing in this plan touches them. If a snapshot shifted, that is a real unintended change to a public page: investigate before updating the snapshot.

- [ ] **Step 3: Drive the acceptance criteria by hand**

With `make dev` plus backend and `yarn dev` running, sign in and confirm each ticket criterion:

1. An existing single-unit property looks **identical** to before — dwelling fields on Info, no Units tab.
2. Create "Keizersgracht 12", split it into 4 units, sign a contract on unit 2. The building shows 25% occupancy and vacancy figures for the other three.
3. Add a €1,200 roof repair at building level with EQUAL basis. Each unit's expense view shows €300.
4. Open the rent increase wizard. It lists units, not buildings.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test(frontend): verify units acceptance criteria end to end (BUUR-106)"
```

---

## Done when

- `cd frontend && yarn test && yarn build && yarn lint` all pass.
- A pre-existing single-unit property renders exactly as it did before the change.
- The four ticket acceptance criteria pass by hand.
- No inline user-facing string — every label resolves through `units` or `properties`.
- Plan 3 (exports, letters, demo data, the 12 remaining locales) is unblocked.
