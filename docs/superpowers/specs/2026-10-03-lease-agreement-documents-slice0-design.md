# Lease agreement documents — Slice 0 (foundation + NL residential reference)

**Date:** 2026-10-03
**Status:** design, awaiting review
**Supersedes (content model only):** `2026-09-30-lease-agreement-generation-design.md` §S4 (placeholder seed text)

## Problem

Lease generation works end to end (clause library, `LeaseClauseResolver`, `LeaseAgreementExporter`,
e-signature), but the content is a placeholder: 7 one-sentence clauses per country, stored in
country-agnostic bundle keys (`lease.premises.body`, …) shared by BE/DE/ES/FR/GB/NL/PT. A German and a
Dutch lease render identical text. Clause bodies are static strings, so rent, deposit, dates and parties
cannot be interpolated into legal wording.

## Intended outcome

A landlord generates a lease that reflects their country's tenancy law, in the language they choose,
with the clause toggles they already have. Success for Slice 0: an NL residential lease renders in all
13 bundle languages from the new model, with the machinery proven for the later country/kind packs.

## Decisions taken (from brainstorming)

- **Scope overall:** BE, DE, ES, FR, GB, NL, PT × 13 languages, fully translated.
- **Model:** one document per (country, kind, language) containing the legal text inline; the document
  also drives clauses (each clause is a tagged block that can be included/excluded).
- **Kinds derived from the property**, not a new full-blown `lease_kind`:
  - `Property.PropertyCategory` `RESIDENTIAL` → residential (furnished vs unfurnished from
    `PropertyResidentialDetails.furnished`); `COMMERCIAL` and `INDUSTRIAL` → commercial;
    `MIXED_USE` → a dedicated **generic mixed-use** agreement; `AGRICULTURAL` → its own kind.
  - A small optional contract field **`lease_regime`** (`STANDARD` default, `SHORT_TERM`,
    `STUDENT_OR_MOBILITY`) covers what a property cannot express. Shown only for residential properties
    and only where the country has that regime.
- **Delivery in slices**, each with its own spec/plan/review; this spec is Slice 0 only.

## Non-goals (Slice 0)

- No content for BE, DE, ES, FR, GB, PT, nor for commercial, mixed-use, agricultural, short-term or
  student/mobility documents. The model must accommodate them; no text is written.
- No WYSIWYG clause editor. No change to e-signature.
- No claim of legal validity (see Legal content).

## Legal content — scope and disclaimer

The text is drafted from general knowledge of each country's tenancy law and is **not lawyer-vetted**.
Therefore:

1. Every generated lease PDF carries a localized footer notice: draft, not legal advice, have it reviewed.
2. Every document file carries a header comment listing its legal basis (statute and article) and a
   `reviewed-by: none` marker; counsel review flips it per file.
3. The backoffice banner introduced by the earlier spec stays.
4. Translations of legal text are marked `translation: machine-drafted` until reviewed by a native-speaking
   lawyer; the national-language document is the authoritative one and the PDF states so.

## Design

### D1 — Document selection

`LeaseAgreementExporter` resolves a template path
`lease-agreement/{CC}/{kind}/{lang}` where kind ∈ `residential`, `residential-furnished`,
`commercial`, `mixed-use`, `agricultural`, and regime (when not `STANDARD`) replaces the kind
(`short-term`, `student-mobility`). Fallback order: requested language → country's national language(s) →
English; the PDF notes when a fallback was used. A country/kind with no document at all makes the
tab show "no template available" and generation throws `BusinessRuleException` (as today for missing
templates). `LetterTemplateService.renderToHtml` currently hard-codes `/generic`; it gains an overload
taking the full template name. Other letter types are unaffected.

### D2 — Clauses live in the document

```html
<section th:if="${clauses.included('deposit')}" data-clause="deposit">
  <h2>Artikel 5 — Waarborgsom</h2>
  <p>De waarborgsom bedraagt [[${deposit}]] …</p>
</section>
```

- `lease_clause_templates` keeps structure only: add `lease_kind VARCHAR(32) NOT NULL DEFAULT
  'residential'`; `title_i18n_key` / `body_i18n_key` become nullable and unused for new rows. New unique
  key `(country_code, lease_kind, clause_key, version)`. Old placeholder rows are soft-deleted by a new
  migration (never edit V083); new rows are inserted with `version = 2`.
- `LeaseClauseResolver` returns the structure (key, included, optional, order) and **also** the clause
  title for the toggle UI, read from a small per-document metadata block (`<meta data-clause-title>` or a
  sidecar bundle `lease-clauses_{lang}.properties` — decided in the plan after checking Thymeleaf's
  fragment API). Required clauses stay forced-included (V084 constraint and resolver rule unchanged).
- **Reordering is kept.** The per-contract `sort_order` override (already persisted by
  `LeaseClauseService`) drives render order: the document renders clause blocks in the resolved order via
  a loop over `clauses.ordered()` that includes each block by key (blocks are Thymeleaf fragments
  `th:fragment="clause-deposit"` in the document). To keep reordering legally safe:
  - **Auto-numbering:** article numbers are generated (`Artikel 1…n`) from the final order, never
    hard-coded in the text.
  - **No hard-coded cross-references:** clause text refers to other clauses via `clauses.ref('rent')`,
    which renders the current article number, or omits the reference if that clause is excluded.
  - **Pinned clauses:** clauses flagged `pinned` (parties, premises, signatures; the required core)
    keep fixed positions; only the remaining clauses can be moved. Pinning is a new boolean
    `lease_clause_templates.pinned`, default false.
  - The clause tab gains up/down controls (disabled for pinned clauses); `sortOrder` already round-trips
    through the API, so no API change is needed.

### D3 — Template variables

The existing variables stay (`clauses` now = an `IncludedClauses` helper, `signatureBlocks`, addressee,
premises) plus a typed `lease` object: parties (names, addresses), property address and description,
start/end date, contract type, notice periods, rent components with amounts formatted per locale,
deposit, payment day/instructions, indexation reference (from the rent-regulation catalog), and the
country's `ContractCountryMetadata` record. Statutory facts (deposit caps, notice periods, indexation
limits) come from the existing regulation catalog / tenancy-rules reference where available, so the
document and the regulations page cannot disagree; otherwise the text states the rule without a number.

### D4 — Contract regime field

Migration adds `contracts.lease_regime VARCHAR(32) NOT NULL DEFAULT 'STANDARD'` with a CHECK
constraint. OpenAPI (`openapi/src/`, then `make bundle-openapi` and `yarn generate:api`), create/update
DTOs, mapper, and a select in the contract form shown only when a document exists for another regime in
that country (served by a new endpoint `GET /lease-agreements/availability?country=&propertyId=`).
Slice 0 ships the field and plumbing; only `STANDARD` has content for NL.

### D5 — NL residential reference

Dutch main-residence lease (Boek 7 BW titel 4; Wet betaalbare huur; Wet vaste huurcontracten, as in
force on the implementation date — verified against sources during the plan, not from memory) with the
clause set: parties, premises, term, rent, rent adjustment, service costs, deposit (max 2 months' base
rent), payment, use, subletting, maintenance and repairs, energy label, inspection/handover, termination,
data protection, disputes, signatures. Optional/required flags follow the legal necessity. Written in
`nl`, then all 12 other bundle languages, each with the D1 fallback and the Legal content markers.

### D6 — Tests

- **Parity test** (extends `I18nBundleParityTest`'s philosophy): for each (country, kind) every language
  document declares the same set of `data-clause` keys, and every key exists in `lease_clause_templates`.
- **Render test per document**: renders with a fixture contract, asserts no unresolved `${…}`/`#{…}`,
  every included clause present, excluded clause absent, required clause cannot be excluded.
- **Reorder test**: a shuffled order renders articles numbered 1…n with cross-references pointing to the
  right numbers; a reference to an excluded clause is omitted; pinned clauses cannot move.
- **Fallback test**: missing language → national language; missing country/kind → clear error.
- **Migration/repository test**: kind-aware `findByCountryCode` is team-agnostic (templates are global),
  contract `lease_regime` defaults to `STANDARD`; multi-tenant assertions for the contract field.
- Existing `LeaseAgreementExporterTest` and `ContractLeaseAgreementTab` tests updated, not deleted.

## Slice roadmap (not part of this spec)

1. **Slice 0 (this):** foundation + NL residential.
2. NL remaining kinds (commercial, mixed-use, agricultural, furnished, short-term, student/kamerverhuur).
3. Per country, in order BE, DE, FR, ES, PT, GB: each its own spec; counsel review of NL in parallel.
4. Mixed-use generic and agricultural *templates* across all countries.

## Resolved at review

- The national-language document is authoritative; other languages are marked as courtesy translations
  in the PDF.
- Per-contract clause reordering is kept (see D2), with auto-numbering, `clauses.ref()` cross-references
  and pinned core clauses.
- `INDUSTRIAL` → commercial template.
