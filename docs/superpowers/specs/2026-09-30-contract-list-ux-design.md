# Contract list UX — design

BUUR-105 item 5. Date: 2026-09-30.

## Intended outcome

A landlord with dozens of contracts can find one by typing a tenant's name,
an address, or a contract ID, narrow the list to contracts ending soon, sort
by end date, and export what they're looking at — instead of scrolling a
card grid with only a status filter.

Success looks like: a landlord types "Jansen" and the contract for that
tenant appears; they add an "ending within 90 days" filter before a renewal
push and see exactly the contracts that need attention, each flagged with an
expiring-soon indicator; they export that filtered list to CSV for their
accountant.

## Current state (evidence)

`ContractsPage.tsx` manages one piece of filter state (`statusFilter`) and
renders `ContractCard`s in a grid, not a table. The backend list endpoint
(`GET /contracts`) supports `status`/`propertyIdentifier`/`contactIdentifier`
plus generic `sort`/`direction`, but **when `propertyIdentifier` or
`contactIdentifier` is set, the controller bypasses pagination and sorting
entirely**, falling back to a non-paginated "get all for this property/
contact" path (`ContractController.getContracts`). Any change here must not
regress that combination further, and should ideally stop it from silently
dropping `status`/`sort`.

`ContractRepository.findAllByTeamIdPaginated` already builds optional
`Condition`s for `status`/`propertyId`/`contactId` and delegates to
`PaginationHelper.paginate` with an allowlisted sortable-fields map
(`createdAt`, `startDate`, `endDate`, `rentAmount`, `status`) — `endDate`
already resolves through `EffectiveEndDateHelper.effectiveEndDate()`, the
same computed expression `findExpiringContracts` uses for the existing
contract-expiry notification job. No free-text search exists on contracts.
`DocumentRepository.findAllByTeamIdPaginated` is the established pattern for
adding one (`lower(field).like('%term%')`, ORed across fields) — no
Postgres full-text/trigram infra exists anywhere in this codebase, so this
project doesn't introduce any.

"Saved filters" and "export what's currently filtered" are both genuinely
absent. Every existing bulk-export call (`EntityExportControls` on
`ContractsPage`) exports the entire team's contracts, unfiltered — the
`ExportServiceImpl.generateContractsCSV/Excel(UUID teamId)` methods take no
filter parameters at all.

## Scope decisions

- **Bulk export means "export the current filter," not row selection.**
  `ContractsPage` renders cards, not a table — adding checkbox-based
  multi-select would mean building new selection UI with no precedent in
  this codebase for contracts (the only precedent, `DocumentList`, is
  table-based). Extending the existing CSV/XLSX exporters to accept the same
  filter parameters as the list query is a small, precedented change
  (mirrors how the list query itself is built) and serves the more common
  real want — "export what I'm looking at" — without new UI.
- **No pagination fix for the property/contact bypass beyond not making it
  worse.** Fixing `ContractController`'s non-paginated fallback path is a
  pre-existing bug, not in this ticket's scope; the new search/expiry filter
  is added to the paginated path only. Flagged as a known gap, not silently
  worked around.
- **Saved filters are per-user, name + serialized criteria, nothing more.**
  No sharing, no defaults, no team-wide saved filters — the ticket asks for
  the capability, not a filter-management product.

## S1 — Schema

`V079__saved_contract_filters.sql`:

```sql
CREATE TABLE saved_contract_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    user_id UUID NOT NULL REFERENCES users (id),
    name VARCHAR(100) NOT NULL,
    criteria JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    CONSTRAINT uq_saved_contract_filters_team_identifier UNIQUE (team_id, identifier)
);

CREATE INDEX idx_saved_contract_filters_user ON saved_contract_filters (team_id, user_id)
WHERE deleted_at IS NULL;
```

`criteria` stores the same query params the list endpoint accepts
(`search`, `status`, `endingWithinDays`, `sort`, `direction`) as a JSON
object — no separate columns per filter field, so adding a new filter later
doesn't need a migration. `user_id` (not team-wide) matches the "per-user"
scope decision; `identifier` follows the Sid convention (VARCHAR(29), 3-char
prefix + 26-char ULID — the actual convention, not the narrower one
CLAUDE.md's own shorthand note states — confirmed against every other
identifier column, including the fix already made once on this branch for
`signature_requests`).

## S2 — Backend: search + expiry filter

`ContractRepository.findAllByTeamIdPaginated` gains two new optional
parameters, `search: Optional<String>` and `endingWithinDays: Optional<Integer>`,
added as `Condition`s the same way `status`/`propertyId`/`contactId` already
are:

- `search`: `lower(CONTACTS.display_name-equivalent).like(pattern)
  .or(lower(PROPERTIES.street).like(pattern))
  .or(lower(PROPERTIES.city).like(pattern))
  .or(lower(CONTRACTS.identifier).like(pattern))` — joined the same way
  `contactId` already joins through `contract_parties`; exact contact
  display-field and property address fields confirmed against the real
  schema in the implementation plan, not guessed here.
- `endingWithinDays`: `EffectiveEndDateHelper.effectiveEndDate().between(today, today.plusDays(n))`
  — reuses the exact expression `findExpiringContracts` already uses.

`GET /contracts` gains `search` and `endingWithinDays` query params
(OpenAPI + `ContractController` + `ContractService.getContractsPaginated`
threading them through). The existing property/contact-identifier bypass
path is untouched (see Scope decisions) — these two new filters only apply
on the paginated path.

## S3 — Backend: filtered export

`ExportServiceImpl.generateContractsCSV`/`generateContractsExcel` gain the
same filter parameters as the list query (`status`, `search`,
`endingWithinDays`) and pass them into a new
`ContractRepository.findAllByTeamId(teamId, status, search, endingWithinDays)`
(no pagination — full filtered result set, since a landlord exporting wants
everything that matches, not one page). `ContractCsvExporter`/
`ContractExcelExporter` themselves don't change — only what feeds them does.

## S4 — Backend: saved filters

`SavedContractFilterRepository` (standard CRUD, team+user scoped) and
`SavedContractFilterService` (`@PreAuthorize` at `TEAM_VIEWER`+ for all
operations — a saved filter is personal, not a permission-gated action).

```
GET    /saved-contract-filters              list current user's saved filters
POST   /saved-contract-filters               create (name + criteria)
DELETE /saved-contract-filters/{identifier}   delete (must be the creator)
```

No `PUT`/update — deleting and recreating a filter is simpler than a partial
update for a five-field JSON blob, and nothing in the ticket asks for
editing a saved filter's name independent of its criteria.

## S5 — Frontend

- `ContractsPage.tsx` gains: a search text input (debounced, matches the
  existing `useDebouncedValue`-style pattern used elsewhere in this
  codebase if one exists, else a small local debounce), an "ending within N
  days" numeric filter alongside the existing status `FilterSelectPopover`,
  and a sort control (dropdown: end date / start date / rent, asc/desc).
- A "Saved filters" dropdown next to the existing filter chips: select a
  saved filter to apply its criteria, a "Save current filters" action that
  prompts for a name, a delete affordance per saved filter.
- `EntityExportControls`' existing CSV/XLSX/Sheets adapters for contracts
  are updated to pass through the current `search`/`status`/
  `endingWithinDays` state, so "export" always matches what's on screen.
- New `ExpiringSoonBadge` component: renders when a contract's
  `effectiveEndDate` falls within a threshold (90 days — matches the
  existing contract-expiry-notification job's typical horizon; exact
  threshold is a constant, easy to change later, not user-configurable in
  v1) and the contract is `ACTIVE`. Rendered on `ContractCard` next to
  (not replacing) the existing `ContractStatusBadge`, in a color distinct
  from `EXPIRED`'s orange (e.g. amber) to avoid visual collision.

## Testing

- `ContractRepositoryIntegrationTest` (or a new test class) — search across
  contact name / property address / city / contract identifier each finds
  the right contract and excludes non-matches; `endingWithinDays` correctly
  includes a contract ending in 30 days when queried with 90, excludes one
  ending in 200 days; a team-B contract is never returned for a team-A
  query even when its fields would otherwise match (multi-tenant isolation,
  the CLAUDE.md-noted gap).
- `SavedContractFilterRepositoryIntegrationTest` — team+user isolation:
  user A's saved filter isn't visible to user B on the same team; deleting
  requires being the creator.
- Frontend: `ContractsPage` search input filters the rendered list (mocked
  API), the expiring-soon badge renders only for `ACTIVE` contracts inside
  the threshold, saved-filter selection re-applies the stored criteria to
  the URL/query state.

## Out of scope

- Row-selection-based bulk export (filtered export covers the real want,
  see Scope decisions).
- Fixing the pre-existing property/contact-identifier pagination bypass.
- Team-shared or default saved filters.
- Postgres full-text search / trigram indexing (ILIKE matches this
  codebase's existing convention and is sufficient at real per-team
  contract counts).
