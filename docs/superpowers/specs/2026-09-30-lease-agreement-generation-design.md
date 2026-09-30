# Lease agreement generation — design

BUUR-105 item 3. Date: 2026-09-30.

## Intended outcome

A landlord creates a contract and can generate the actual lease agreement
document from it — not just addenda and notices for a lease assumed to
already exist on paper somewhere — with country-appropriate clauses, in the
contract's language.

Success looks like: a landlord finishes entering an NL contract's details
(rent, metadata, parties) and clicks "Generate lease agreement," gets a PDF
built from the NL clause set (parties, premises, rent, deposit, duration,
house rules, termination reference), can toggle an optional clause
(furnished-vs-unfurnished addendum) off before generating, and the result
validates against `NlContractMetadata` the same way the rest of the
contract's data already does.

## Legal content — explicit scope and disclaimer

**Unlike the termination-workflow spec, this feature has no ticket-provided
legal text to seed from** — the ticket names the 7 target countries but not
clause language. This spec therefore seeds a small set of clearly-labeled
**example/placeholder clauses per country** (structural boilerplate: parties,
premises, rent, duration, deposit, maintenance, termination-reference — not
jurisdiction-specific legal wording), and the implementation plan must:
(a) mark every seeded clause body with a code comment and a backoffice UI
banner stating it is unvetted placeholder text, (b) never claim or imply
legal validity in any user-facing copy. This is scaffolding for a real
clause-authoring workflow, not a delivered legal product.

## Current state (evidence)

No lease-generation capability exists at all: `LetterController` implements
only `getDepositStatement`/`getPaymentFormalNotice`/`getExtensionAddendum`/
`getRentIncreaseLetter`/`getRentChangeDocument` plus two generate-and-persist
variants — nothing resembling a `generateLeaseAgreement`. No `Clause`/
`ClauseLibrary`/`ContractClause` entity exists anywhere in the domain
model. No backoffice template-management UI/API exists.

Every existing letter template is one static `generic.html` per document
type; country variation is a single opaque `legal.{COUNTRY}` message-bundle
paragraph resolved by `LetterExporterHelper.legalVariables()`. The 48
`ContractCountryMetadata` records (`NlContractMetadata`, `DeContractMetadata`,
etc., validated by `CountryMetadataValidator`) are rich, typed, per-country
domain data already captured on every contract — exactly the data a lease
template needs to reference, already present and validated, nothing new to
build there. `ContractRentComponent` gives the itemized rent breakdown a
lease needs to list.

The closest existing precedent for "backoffice-managed, versioned,
country-keyed content" is the rent-regulation catalog
(`RentRegulationCatalogService`): JSON-seeded, DB-persisted, versioned
(`catalog.version()`), full CRUD + diff + reload, backoffice-admin-gated.
Not a template system, but the right shape to borrow for clause management
instead of inventing a new one.

## Key architectural decision: clauses ARE the country variation

Rather than building 7 separate static HTML templates (one per target
country) — which would duplicate the entire "generic.html + legalClause
conditional" pattern seven times and not actually deliver "clause add/remove
per contract" as anything but a per-country toggle — **one generic
`lease-agreement/generic.html` template renders an ordered list of resolved
clauses**, and country variation emerges entirely from *which clauses a
country's clause set includes by default*, not from different HTML files.
This single mechanism satisfies both "country-specific templates" and
"clause library with per-contract add/remove" at once, and is the more
maintainable design: adding an 8th country later is a data-seeding task,
not a new template file.

## S1 — Schema

Migration numbers across all five BUUR-105 sub-specs are assigned in a
single fixed order: `V079` list UX, `V080`–`V081` termination workflow,
`V082`–`V083` this spec. Per-country addenda has no schema change. The
implementation plan re-derives the actual next-free version at
implementation time rather than trusting this number if specs land out of
order.

`V082__lease_clause_library.sql`:

```sql
CREATE TABLE lease_clause_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    clause_key VARCHAR(64) NOT NULL,       -- e.g. 'parties', 'premises', 'rent', 'deposit', 'duration', 'maintenance', 'termination-reference', 'house-rules'
    title_i18n_key VARCHAR(128) NOT NULL,  -- resolved via the existing MessageSource convention
    body_i18n_key VARCHAR(128) NOT NULL,   -- body is i18n-bundle text, same mechanism as legal.{COUNTRY} today — not a new content-storage mechanism
    default_included BOOLEAN NOT NULL DEFAULT TRUE,
    optional BOOLEAN NOT NULL DEFAULT FALSE,  -- if false, a landlord cannot remove it (e.g. 'parties' is never optional)
    sort_order INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_lease_clause_templates_identifier UNIQUE (identifier),
    CONSTRAINT uq_lease_clause_templates_country_key_version UNIQUE (country_code, clause_key, version)
);

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
```

`lease_clause_templates` has **no `team_id`** — it's global reference data
like the rent-regulation catalog, not tenant data (same reasoning as
`feature_flags`). `contract_lease_clauses` is the per-contract override
layer: rows only exist once a landlord has generated (or is generating) a
lease and touched the default selection; absence of a row for a given
template means "use the template's `default_included`."

Body text stays in `.properties` i18n bundles (`document-lease-agreement*.properties`),
**not** a new free-text column — this reuses the exact existing
`MessageSource`/13-language mechanism every other letter already uses,
rather than building a second content-storage system. `body_i18n_key` is
the pointer into that bundle.

## S2 — Backend: clause resolution + generation

`LeaseClauseTemplateRepository`/`ContractLeaseClauseRepository` (standard
JOOQ repos, `lease_clause_templates` queries have no team filter by design;
`contract_lease_clauses` queries do).

`LeaseClauseResolver.resolveForContract(Contract): List<ResolvedClause>` —
loads all templates for `contract.getCountryCode()`, left-joins any
`contract_lease_clauses` overrides, applies `included` (override if present,
else `default_included`), sorts by `sort_order`, resolves each clause's
title/body via the country's `MessageSource` at the contract's document
locale (same `DocumentLocale` resolution every other letter uses).

`LeaseAgreementExporter` (new, `buurman-letters`), following the
`RentIncreaseLetterExporter` shape: loads contract + property + rent
components (`ContractRentComponentRepository`) + addressee, resolves
clauses via `LeaseClauseResolver`, builds variables (`clauses: List<ResolvedClause>`,
`rentComponents`, standard header/premises/addressee blocks from
`LetterExporterHelper`), renders `lease-agreement/generic.html`.

```
GET  /contracts/{identifier}/lease-clauses          resolved clause list + inclusion state, for the toggle UI
PUT  /contracts/{identifier}/lease-clauses           bulk-set included/excluded + reorder (writes contract_lease_clauses)
POST /contracts/{identifier}/lease-agreement          generate-and-persist (same generate-and-persist Document pattern as extension/rent-change documents)
```

Non-optional clauses (`optional = false`) are rejected if a `PUT` tries to
exclude them (400) — enforced server-side, not just hidden in the UI,
since this is data integrity for a legal document, not just UX polish.

## S3 — Backoffice: clause template management

New backoffice controller/service/pages, following the rent-regulation
catalog's existing pattern (CRUD + a version bump on edit, no diff/reload
machinery — that's more than this needs, since clause templates aren't
bulk-reloaded from a bundled file the way the regulation catalog is):

```
GET    /backoffice/lease-clause-templates?countryCode=NL
POST   /backoffice/lease-clause-templates
PUT    /backoffice/lease-clause-templates/{identifier}
DELETE /backoffice/lease-clause-templates/{identifier}
```

`@PreAuthorize("hasRole('BACKOFFICE_ADMIN')")` throughout, matching every
other backoffice-admin-gated write in this codebase. Frontend:
`frontend/backoffice/src/pages/LeaseClauseTemplatesPage.tsx`, a simple
country-filtered table + edit form (title key, body key, sort order,
optional/default-included toggles) — **a prominent banner stating seeded
clause bodies are unvetted placeholder text** (per the Legal content
section above), not a rich-text/WYSIWYG editor (the body is an i18n key
reference, not inline text — editing the actual prose happens in the
`.properties` bundles, same as every other letter today; this page manages
which clauses exist and their structure, not their translated text).

## S4 — Seed data

`V083__seed_lease_clause_templates.sql` — 7 countries × ~7 clauses each
(parties, premises, rent, duration, deposit, maintenance,
termination-reference — the last one intentionally cross-references the
termination-workflow feature's letter, not duplicating its content),
`optional = false` for parties/premises/rent/duration (a lease without
those isn't a lease), `optional = true` for the rest. Corresponding
`.properties` keys added with clearly-placeholder English text (and machine
"same text, marked TODO-translate" for the other 12 languages, matching
how new keys are typically bootstrapped in this codebase before real
translation — confirmed against the implementation plan's actual i18n
parity-guard mechanism).

## S5 — Frontend

New `ContractLeaseAgreementTab` (or a section within the existing
`ContractDocumentsTab`, decided in the implementation plan based on how
much UI a clause-toggle list actually needs) — checkbox list of resolved
clauses (non-optional ones shown but disabled/checked), a "Generate lease
agreement" button, generated documents appear in the existing
`ContractDocumentsTab` (same `Document` row mechanism every other generated
letter uses — no new document-listing UI).

## Testing

- `LeaseClauseResolverTest` — default inclusion respected when no override
  exists; an override flips inclusion; a non-optional clause can't be
  excluded via the repository layer either (defense in depth, not just the
  service-layer 400).
- `LeaseAgreementExporterTest` — renders successfully for a contract with
  full `NlContractMetadata`, all required (non-optional) clauses present in
  the output HTML.
- Backoffice: clause-template CRUD integration test, team-agnostic (no
  team_id) confirmed not to leak between backoffice sessions inappropriately
  (it's global data, so "isolation" here means confirming it's NOT
  accidentally team-scoped, the opposite check from every other repository
  test on this branch).

## Out of scope

- Real vetted legal clause text for any country (see Legal content section).
- A rich-text/WYSIWYG clause editor.
- Bulk reload-from-file / diff machinery for the clause catalog (unlike the
  rent-regulation catalog, this data isn't expected to change via bulk
  external re-seeding).
- Clause versioning beyond the `version` column existing for future use —
  no UI to browse historical versions in v1.
