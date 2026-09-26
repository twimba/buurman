# Tenancy-rules reference in the rent-regulation catalog

**Date:** 2026-09-26
**Status:** design, awaiting review

## Problem

The 2026-09-26 catalog audit surfaced 337 verifier-reported gaps. Roughly 323 fit
`CatalogRule` and were handled. About 14 did not, because they are not rent-increase
caps:

| Country | Fact | Why it does not fit |
|---|---|---|
| AT | `MRG § 29` minimum fixed term raised 3 → 5 years (5. MILG, BGBl I 114/2025) | Tenancy duration, not a rent cap |
| DK | `lejeloven § 182 stk. 2` påkravsgebyr DKK 335 → 344 from 2027-01-01 | Fixed amount; `lateFee.maxPercentage` is a percentage |
| IE | Fixed payment notice €200 within 28 days, prescribable up to €1,000 | Penalty, not a cap |
| LU | Written lease mandatory *sous peine de nullité* with eight mandatory clauses (art. 5(1)) | Lease formality |
| US-DC | RentRegistry portal filing now mandatory | Registration duty |
| CZ | `zákon č. 175/2025 Sb.` housing-support framework, effective 2026-01-01 | Framework, no cap |
| FI | New `AHVL § 27(2)` notice timing from 2026-10-01 | Notice period |

These are country-level tenancy facts. `CatalogCountry` has nowhere to put them except
its single `summary` string, so today they are silently dropped on every audit.

## Intended outcome

Buurman's regulation dataset broadens from *rent-increase compliance* to a **tenancy-law
reference** a landlord can read per country. Success: an audit that finds "AT raised the
minimum fixed term to 5 years" has somewhere correct to record it, and the landlord sees
it on the regulations page with its legal basis and source.

**Decisions taken** (from brainstorming; overrule at review if wrong):

- **Display-only.** Nothing computes off this data. It renders; no service reads it.
- **Open structure, not typed columns.** One shape holds any topic, so a new topic needs
  no migration.
- **Region-scoped and dated, but no history.** Optional `regionCode` (null = national)
  and `effectiveFrom`; superseded entries are replaced, not retained.

## Non-goals

- No contract-level override (contrast `formalNoticeDays`, which has one).
- No computation, validation or letter/document generation from these values.
- No historical series, and no per-year dimension.
- Not a replacement for legal advice — the existing regulation disclaimer still applies.

## Design

### Domain

New record in `buurman-common`, `com.buurman.domain.regulation`:

```java
public record CatalogTenancyRule(
    TenancyRuleTopic topic,
    String regionCode,     // null = national; must match a declared region of the country
    String label,          // "Minimum fixed term"
    String value,          // "5 years" / "DKK 344" / "2 months' rent" / "Mandatory, written"
    String effectiveFrom,  // YYYY-MM-DD, nullable
    String legalBasis,     // "MRG § 29 (5. MILG, BGBl I 114/2025)"
    String sourceUrl,
    String notes) {}
```

`CatalogCountry` gains `List<CatalogTenancyRule> tenancyRules`.

New enum `com.buurman.domain.TenancyRuleTopic`:

```
NOTICE_PERIOD, TENANCY_DURATION, DEPOSIT, LEASE_FORM,
REGISTRATION, FEES_AND_PENALTIES, TERMINATION_GROUNDS, OTHER
```

**Why `value` is a String.** The facts are heterogeneous — a duration, a fixed sum in
local currency, a multiple of rent, a boolean obligation. Any unit system would fit none
of them well, and nothing computes off the value. If a topic later needs to drive
behaviour it gets promoted to a typed column following the `formalNoticeDays` pattern
(country default + contract override); that is an additive change, not a rework.

**Why `topic` is an enum.** The page groups and i18n-translates topic headings; free text
would fragment across countries. This follows `MaxIncreaseType` / `LateFeePolicy`, and
carries the same obligation: a new constant must be added to `openapi/src/app.yaml` and
re-bundled, or the frontend's generated TS drifts.

### Storage

Migration **V068** (V067 is the current head; note `CLAUDE.md` still says V066 and is stale).

```sql
CREATE TABLE rent_regulation_tenancy_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),  -- NULL = national
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
    UNIQUE (country_id, identifier)
);
CREATE INDEX idx_rr_tenancy_rules_country ON rent_regulation_tenancy_rules (country_id);
CREATE INDEX idx_rr_tenancy_rules_topic ON rent_regulation_tenancy_rules (country_id, topic);
```

Follows the conventions of the sibling reference tables: **no `team_id`** (this is global
reference data, not tenant data) and **no `deleted_at`** (reload is a destructive
wipe-and-reseed via `deleteAllReferenceData()`). Both are deliberate departures from the
general entity checklist in `CLAUDE.md`, matching `rent_regulation_regions`.

### Flow

1. `rent-regulations.json` — each country gains an optional `tenancyRules[]`.
2. `RentRegulationCatalogLoader` parses it; unknown `topic` fails the parse, which is the
   existing enum guard.
3. `RentRegulationRepository` — seed on reload, and read back for export.
4. `RentRegulationCatalogService` — `reload()` seeds; `diff()` reports tenancy rules
   added / removed / changed; `exportCatalog()` rebuilds them so the round-trip is lossless.
5. `RentRegulationTenancyRuleResponse` DTO on `RentRegulationCountryDetailResponse`.
6. `openapi/src/app.yaml`: add `TenancyRuleTopic` and `RentRegulationTenancyRuleResponse`,
   then `make bundle-openapi` and `yarn generate:api`.
7. `RegulationSummary.tsx` renders a "Tenancy rules" section grouped by topic, each entry
   showing label, value, effective-from, legal basis and source link. Section hidden when
   the list is empty. i18n keys `rentRegulations.tenancyRules.topic.*`.

### Integrity and error handling

- Unknown `topic` → Jackson parse failure on load, caught by `RentRegulationCatalogTest`.
- `regionCode` must resolve to a region declared in the same country — the existing
  `regionScopedRules_resolveToDeclaredRegions` test is extended to cover tenancy rules.
- `effectiveFrom`, when present, must be `YYYY-MM-DD`.
- `label` and `value` must be non-blank; `topic` non-null.
- `deleteAllReferenceData()` currently deletes `RULES → REGIONS → COUNTRIES`. The new table
  has FKs to **both** `rent_regulation_countries` and `rent_regulation_regions`, so its
  delete must come **first**, before `REGIONS` — otherwise reload fails on a constraint
  violation the moment any region-scoped tenancy rule exists.

### Testing

- `RentRegulationCatalogTest` — topic enum validity, region resolution, required fields.
- `RentRegulationCatalogDiffTest` — diff detects added / removed / changed tenancy rules.
- Repository integration test — seed → export round-trip preserves tenancy rules. This is
  the regression that matters: the usual failure mode is `exportCatalog()` silently
  dropping a newly added structure.
- `RegulationSummary.test.tsx` — renders grouped topics; section absent when list empty.

### Rollout

The field starts empty; no backfill. The ~14 facts above become the first entries. The
regulations page renders nothing new until data exists, so backend and frontend can ship
independently.

**`.claude/skills/update-rent-regulations/SKILL.md` must be updated in the same change** to
document `tenancyRules`, its topic enum and the "no history, replace in place" rule.
Without that, future audits keep discarding these facts and the new structure stays empty —
which is the main way this work fails to pay off.

## Open questions for review

1. Is `TenancyRuleTopic` the right cut? `TERMINATION_GROUNDS` may be too broad to curate
   honestly, and could be dropped from the initial set.
2. Should tenancy rules also surface in the entity booklets (`buurman-booklets`), or only
   on the regulations page? The spec assumes page-only.
3. Confirm no-history is acceptable: an old contract signed under AT's 3-year rule will
   show today's 5-year value with its 2026 effective-from date, and nothing records the
   prior value.
