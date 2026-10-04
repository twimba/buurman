# Contract termination workflow — design

BUUR-105 item 1. Date: 2026-09-30.

## Intended outcome

A landlord ending a tenancy gets the legally-required notice period computed
for them instead of guessing, a notice letter generated in the tenant's
language, the deposit-return deadline and payments handled automatically,
and a clear two-step status (`NOTICE_GIVEN` → `TERMINATED`) instead of an
instant, irreversible-looking status flip.

Success looks like: a landlord in Germany with a tenant of 6 years opens
"Terminate contract," the wizard proposes the 9-month statutory notice
(§573c BGB-derived, per this ticket's own stated figures — see Legal
content below), the landlord confirms, a German-language notice letter is
generated, the deposit return deadline is set, and payment generation stops
after the computed end date.

## Legal content — explicit scope and disclaimer

**This spec seeds only the notice-period figures already stated in the
BUUR-105 ticket itself** (NL: 3–6 months, legal grounds required; DE: 3–9
months by tenancy length; FR: 3/6 months) — it does not add new legal
research. This is existing project-provided content, not content this spec
invents. Nonetheless: **the seeded data is example/starting data, not
verified legal advice, and the implementation plan must say so in code
comments and in the backoffice UI that edits it** — the same posture this
branch already took with self-hosted Documenso's SES/AES-vs-QES distinction.
A landlord relying on this feature for an actual termination should still
confirm current local requirements; this feature computes from stored data,
it doesn't certify the data's correctness.

## Current state (evidence)

`Contract.ContractStatus` is `DRAFT, ACTIVE, EXPIRED, TERMINATED,
PENDING_SIGNATURE` with `ACTIVE → TERMINATED` a single direct transition in
`ContractService.validateStatusTransition`'s exhaustive switch. No
intermediate "notice given" state exists. `terminationNoticeDays`/
`landlordNoticeDays`/`tenantNoticeDays` are stored on every contract but
**read nowhere** — copied on create/renew, never used to compute anything.

The rent-regulation catalog (`rent-regulations.json`, loaded via
`RentRegulationCatalogService`) has a `TenancyRuleTopic.NOTICE_PERIOD`
enum value already, but every country's `tenancyRules[]` is empty and the
type is explicitly documented as "display-only free text, promote to a
typed field when something needs to compute off it" — termination notice
periods are exactly that promotion.

`PaymentFormalNoticeExporter.resolveDeadlineDays()` already establishes the
precedence pattern this feature needs: contract-level override → regulation
catalog default → hardcoded fallback. `PaymentSchedulingService.handleContractStatusChange`
already stops payment generation on `TERMINATED`/`EXPIRED`/`DRAFT` and is
invoked from every status change — a `NOTICE_GIVEN` case just needs to
exist in that switch (as a no-op; payments already stop based on
**effective end date**, not status, via `EffectiveEndDateHelper`, so notice
alone shouldn't stop payments before the computed end date).

`Deposit.returnDueDate` already exists and is settable — no new "deposit
deadline" entity is needed, just automatic population. **No "check-out
inspection" concept exists anywhere** — building a full new inspection
entity/workflow is disproportionate to what the ticket asks ("schedule");
scoped down below. No one-time (as opposed to recurring cron-sweep) Quartz
trigger pattern exists in this codebase — "scheduling" here means a new
daily sweep job, matching `ContractExpiryCheckJob`'s existing shape, not a
per-contract Quartz trigger.

`RentIncreaseWizardPage.tsx` establishes the multi-step wizard convention
this feature's frontend follows exactly: a `WizardStep` union driving
`useState`, a step-indicator pill row, one component per step under a new
`frontend/app/src/components/contractTermination/` directory.

## Scope decisions

- **No check-out-inspection entity.** The ticket says "schedule check-out
  inspection" — scoped to a single `inspection_date` field on the
  termination record (landlord-entered, optional) plus a reminder via the
  new sweep job, not a new inspection domain object with its own status
  machine. If a real inspection-scheduling feature is wanted later, this
  field is exactly where it would attach.
- **No notice withdrawal.** `NOTICE_GIVEN → ACTIVE` isn't in the ticket and
  adds real state-machine complexity (undoing a sent notice letter, an
  already-notified tenant); out of scope, flagged explicitly rather than
  silently absent.
- **Override is a warning, not a hard block.** The ticket says "override
  with warning" — the landlord can pick an earlier end date than the
  computed one; the API accepts it with a required `overrideReason` string
  and the letter/audit trail record that an override happened.

## S1 — Schema

Migration numbers across all five BUUR-105 sub-specs are assigned in a
single fixed order to avoid collisions: `V079` saved-contract-filters
(list UX), `V080`–`V081` this spec, `V082`–`V083` lease-clause-library.
Per-country addenda has no schema change. The implementation plan is the
authority if any of these specs' migrations end up implemented out of this
order — it must re-derive the actual next-free version at implementation
time rather than trust this number, the same lesson learned earlier on
this branch when a hand-written column width went stale.

`V080__contract_terminations.sql`:

```sql
ALTER TYPE contract_status ADD VALUE IF NOT EXISTS 'NOTICE_GIVEN';
-- (or, if contract_status is a VARCHAR CHECK constraint rather than a
-- native enum type — confirm against V004's actual column definition in
-- the implementation plan — adjust accordingly; this schema assumes the
-- convention already used by every other status column in this codebase.)

CREATE TABLE contract_terminations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    contract_id UUID NOT NULL REFERENCES contracts (id),
    given_by VARCHAR(16) NOT NULL,          -- LANDLORD, TENANT
    notice_date DATE NOT NULL,
    ground_code VARCHAR(64),                -- nullable: not every jurisdiction requires one
    computed_end_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,       -- the accepted date: computed, or the override
    override_reason TEXT,                   -- set only when effective_end_date < computed_end_date
    inspection_date DATE,
    notice_letter_document_id UUID REFERENCES documents (id),
    status VARCHAR(32) NOT NULL,            -- NOTICE_GIVEN, TERMINATED
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT uq_contract_terminations_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT uq_contract_terminations_contract UNIQUE (contract_id)
);

CREATE INDEX idx_contract_terminations_team ON contract_terminations (team_id);
CREATE INDEX idx_contract_terminations_effective_end_date ON contract_terminations (effective_end_date)
WHERE status = 'NOTICE_GIVEN';
```

`uq_contract_terminations_contract` enforces one termination record per
contract, matching the state machine (`TERMINATED` is terminal — a contract
can't be re-terminated). A separate table rather than columns on
`contracts`, matching the same reasoning already used for `signature_requests`
on this branch: keeps `contracts` free of feature-specific columns, and this
data is genuinely a distinct sub-record, not a property of the contract
itself.

`V081__termination_notice_rules.sql` extends the rent-regulation catalog's
domain (not the bundled JSON loader mechanism itself — this data changes
per the catalog's existing versioned-reload pattern, described in S2):

```sql
CREATE TABLE rent_regulation_termination_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    party_type VARCHAR(16) NOT NULL,          -- LANDLORD, TENANT
    min_tenancy_months INTEGER,               -- nullable: no minimum
    notice_days INTEGER NOT NULL,
    grounds_required BOOLEAN NOT NULL DEFAULT FALSE,
    grounds_codes TEXT[],                     -- e.g. {OWN_USE, RENOVATION, BREACH} — only meaningful when grounds_required
    source_url TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_rrt_country ON rent_regulation_termination_rules (country_id);
```

Mirrors `rent_regulation_rules`' shape (country/region-scoped, typed
columns) rather than the free-text `rent_regulation_tenancy_rules` table —
this data must be computed off, so it needs the typed-field treatment the
existing `TenancyRuleTopic` Javadoc already anticipates.

## S2 — Backend: notice-days resolution

`TerminationRuleResolver` (`buurman-backoffice` or `buurman-core` — same
module as `RentRegulationCatalogService`, since it's a read over the same
catalog): `resolve(Contract, GivenBy, LocalDate noticeDate): TerminationComputation`
— looks up `rent_regulation_termination_rules` by country (+ region if the
contract has one) + `party_type`, picks the rule matching the contract's
current tenancy length in months against `min_tenancy_months` (highest
matching threshold wins, same "most specific wins" resolution already used
elsewhere in the regulation catalog for rent-increase rules), falls back to
`contract.landlordNoticeDays`/`tenantNoticeDays` if no catalog rule exists
for that country, then a hardcoded 30-day default if neither exists —
exactly `PaymentFormalNoticeExporter.resolveDeadlineDays()`'s three-tier
precedence, reused as a pattern not as shared code (different domain
objects).

Seed data for `V080`'s initial rows: NL (landlord: 3–6 months by category,
`grounds_required=true`, grounds from the ticket's own "legal grounds"
framing), DE (3/6/9 months by `min_tenancy_months` 0/60/96 — the statutory
5-year/8-year thresholds), FR (3/6 months landlord/tenant) — **the three
countries the ticket itself names with figures**; no other countries seeded
(scope decision: don't invent notice-period data for countries the ticket
didn't specify numbers for — those countries fall through to the
contract-level `landlordNoticeDays`/`tenantNoticeDays` fallback, which is
still correct behavior, just less precise).

## S3 — Backend: terminate endpoint + state machine

```
POST /contracts/{identifier}/terminate
```

Request: `givenBy`, `noticeDate`, `groundCode` (optional), `effectiveEndDate`
(optional — omit to accept the computed date; providing one earlier than
computed requires `overrideReason`), `inspectionDate` (optional).

`ContractTerminationService.terminate(...)`:
1. Resolve the computed end date via `TerminationRuleResolver`.
2. Validate: if `effectiveEndDate` provided and earlier than computed,
   require `overrideReason` (400 if missing) — "override with warning" is
   enforced as "the API requires you to say why," which is the backend's
   version of a UI warning the landlord had to acknowledge.
3. Insert the `contract_terminations` row, status `NOTICE_GIVEN`.
4. Transition contract status `ACTIVE → NOTICE_GIVEN` via the existing
   `ContractService.changeContractStatus` path (reusing its audit-logging
   and `handleContractStatusChange` hook — `validateStatusTransition` gains
   the new case).
5. Generate the notice letter (S4) and store its `Document` id on the
   termination row.
6. Set `deposit.returnDueDate` from `effectiveEndDate` + the jurisdiction's
   deposit-return-window figure if the catalog has one, else a 30-day
   default from `effectiveEndDate` — reuses the existing `DepositService`
   update path, no new deposit logic.

A new daily sweep job, `ContractTerminationSweepJob` (same package/pattern
as `ContractExpiryCheckJob`), transitions `NOTICE_GIVEN → TERMINATED`
automatically once `effective_end_date` has passed — this is the "status →
NOTICE_GIVEN → TERMINATED" progression from the ticket; the second step
isn't a landlord action, it's the notice period elapsing.

## S4 — Notice letter

New `ContractTerminationLetterExporter`, following the exact
`RentIncreaseLetterExporter` shape (§7 of the research): loads contract +
property + addressee via `LetterExporterHelper`, resolves locale, builds
variables (`givenBy`, `noticeDate`, `groundLabel` — i18n-keyed from
`groundCode`, `effectiveEndDate`, `overrideReason` if present), calls
`documentTemplateService.renderToPdf("contract-termination-notice", locale, variables)`.
New template `templates/documents/contract-termination-notice/generic.html`,
following the existing single-template-plus-`legalClause`-conditional
pattern (this spec doesn't restructure that mechanism — see the separate
per-country-addenda spec for that).

## S5 — Frontend

`TerminationWizardPage.tsx`, mirroring `RentIncreaseWizardPage.tsx`
exactly: steps `who-gives-notice → notice-date-and-ground → review-computed-date → letter-preview → confirmation`.
The review step is load-bearing UX: shows the computed date prominently,
lets the landlord type an earlier date (revealing the required override-
reason field only when they do — never defaulting to override), and shows
the resolved notice-days source (catalog rule vs. contract fallback vs.
hardcoded default) so the landlord knows how much to trust the number.

`ContractStatusBadge` gains a `NOTICE_GIVEN` entry (a distinct color from
`TERMINATED`'s red — e.g. amber, consistent with the "expiring soon"
indicator introduced in the list-UX spec, since both mean "action pending,"
though they're visually separate components).

## Testing

- `TerminationRuleResolverTest` — the three-tier precedence (catalog rule →
  contract fallback → hardcoded default), the tenancy-length threshold
  matching for DE's 3/6/9-month bands, a country with no catalog data at
  all falls through correctly.
- `ContractTerminationServiceTest` — override without a reason is rejected;
  override with a reason succeeds and is recorded; the state transition
  writes `NOTICE_GIVEN` and stops future payment generation only after the
  effective end date, not immediately; cross-team contract identifier
  resolves `NotFoundException`.
- `ContractTerminationSweepJobTest` — a termination past its effective end
  date transitions to `TERMINATED`; one not yet due is untouched.
- Frontend: wizard review step shows the override-reason field only after
  the landlord edits the date below the computed one.

## Out of scope

- Notice withdrawal (`NOTICE_GIVEN → ACTIVE`).
- A full check-out-inspection entity/workflow beyond a single date field.
- Notice-period data for any country beyond NL/DE/FR (the ticket's own
  named figures) — other countries use the existing contract-level
  fallback fields, which is correct, just less precise.
- A per-contract one-time Quartz trigger — the daily sweep job pattern is
  this codebase's established mechanism and is sufficient at day-level
  granularity for a legal deadline.
