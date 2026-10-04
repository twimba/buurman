# Contract timeline — design

BUUR-105 item 6. Date: 2026-09-30.

## Intended outcome

A landlord opening a contract sees one chronological feed of everything
that happened to it — created, extended, rent changed, documents generated
and signed — instead of piecing it together from four separate tabs plus a
raw audit log.

Success looks like: a landlord fields a tenant's "when did my rent last
change and did I ever get that addendum signed" question by opening one
tab and scrolling, not by cross-referencing the Extensions tab, the
Payments tab, and the Documents tab separately.

## Current state (evidence)

`ContractHistoryTab.tsx` shows only `audit_log` rows for the `CONTRACT`
entity (`GET /contracts/{identifier}/audit-log`) — creates/updates/deletes
logged by `ContractService`, with a special-cased description builder for a
handful of known field changes (`documentAdded`, `partyAdded`,
`primaryContactChanged`, etc.). Rent-period changes, extensions, and
documents are **separate tabs backed by separate tables**
(`ContractExtensionsTab`, `ContractPaymentsTab`/rent periods,
`ContractDocumentsTab`) — none of that data reaches the history tab today.
Signature requests (this branch, just merged) are a fifth data source with
no representation in any timeline at all.

## Scope decision: read-time aggregation, not a migration

Two ways to build a unified feed: (a) migrate every mutation to also write
a normalized `timeline_events` row, or (b) query each existing source at
read time and merge. (a) touches every call site that currently writes to
`audit_log`, rent periods, extensions, and documents — a much larger,
riskier change for a "read differently" feature. (b) adds one new read-only
service that queries five already-existing, already-team-scoped
repositories and merges their results by timestamp — nothing about how or
when those tables get written changes. Going with (b).

No pagination: a communications-timeline feature built earlier on this
branch's history made the same call for the same reason — "the realistic
count per [entity] is bounded" (a contract's lifetime is finite, and each
source is individually small). A true paginated cross-source merge (fetch
enough from each source to guarantee correctness across a page boundary)
is real complexity this ticket doesn't need yet.

## S1 — Backend: `ContractTimelineService`

New service, `buurman-core` (co-located with `ContractService`, since it's
a read-aggregation over contract-scoped data, not a new bounded context):

```java
public record TimelineEvent(
    TimelineEventType type,
    Instant timestamp,
    String title,
    Optional<String> description,
    Optional<Sid> relatedIdentifier) {}

public enum TimelineEventType {
  CONTRACT_CREATED, CONTRACT_STATUS_CHANGED, RENT_CHANGED,
  EXTENSION_CREATED, EXTENSION_ACTIVATED, EXTENSION_DECLINED,
  DOCUMENT_UPLOADED, DOCUMENT_GENERATED,
  SIGNATURE_SENT, SIGNATURE_COMPLETED, SIGNATURE_DECLINED,
  AUDIT_OTHER
}
```

`getTimeline(ContractIdentifier, UserPrincipal): List<TimelineEvent>`:
resolves the contract team-scoped, then queries and maps each source:

- `AuditLogRepository.findByTeamIdAndEntityTypeAndEntityId(teamId, "CONTRACT", contract.getId())`
  → `AUDIT_OTHER` for everything not already covered by a more specific
  source below (status changes map to `CONTRACT_STATUS_CHANGED` by
  inspecting the existing `changedFields` shape the audit description
  builder already special-cases — reuse that logic rather than
  duplicating it, since `AuditService.buildActivityDescription` already
  knows how to turn a raw audit row into a human description).
- `ContractRentPeriodRepository.findByContractIdAndTeamId` → `RENT_CHANGED`
  per period, titled with the new rent amount and effective date.
- `ContractExtensionRepository.findByContractIdAndTeamId` → one event per
  extension state transition available on the stored row (created,
  activated/declined — using whatever timestamps the extension row already
  carries; no new columns).
- `DocumentRepository.findByEntityAndTeamId("CONTRACT", contract.getId(), teamId)`
  → `DOCUMENT_UPLOADED`/`DOCUMENT_GENERATED` (distinguish by whether the
  document was created via the generate-and-persist letter flow or a plain
  upload — if that distinction isn't already recoverable from the
  `Document` row itself, default all to `DOCUMENT_UPLOADED` rather than
  inventing a new column; a cosmetic label difference isn't worth a
  migration).
- `SignatureRequestRepository.findByDocumentIdAndTeamId` for each document
  found above → `SIGNATURE_SENT` (request created), `SIGNATURE_COMPLETED`/
  `SIGNATURE_DECLINED` (from the request's current status and
  `updatedAt` — this branch's e-signature work didn't add per-transition
  timestamps beyond `signed_at` on individual signers, so a status-based
  single event per request is what the data actually supports, not a
  fabricated multi-event history).

All events sorted by `timestamp` descending. `@PreAuthorize` at
`TEAM_VIEWER`+ (a read, same tier as the existing audit-log endpoint).

## S2 — API

```
GET /contracts/{identifier}/timeline
```

Returns `List<TimelineEventResponse>` (mirrors `TimelineEvent` with `Sid`
types on the wire, matching the existing `identifier`-only DTO convention).
New endpoint rather than extending the existing `/audit-log` response
shape, since the existing endpoint is used elsewhere as-is (the plain audit
log, e.g. possibly reused by a future generic entity-audit view) and
changing its response shape would be a breaking change for no reason — a
sibling endpoint is strictly additive.

## S3 — Frontend

`ContractHistoryTab.tsx` is replaced (not extended in place — its current
audit-only fetch/render logic is fully superseded) by a call to the new
`useContractTimeline(contractId)` hook and a single vertical list,
icon+color per `TimelineEventType` (following the existing icon-per-type
convention already used in `CommunicationsTimeline`, if that component
exists on this branch per the earlier communications-timeline work — reuse
its visual pattern rather than inventing a new one). Each row: icon,
title, relative time, and a link to the related tab/document where
`relatedIdentifier` is present (e.g. a `SIGNATURE_COMPLETED` event links to
the Documents tab).

The tab is renamed from "History" to "Timeline" in the tab label (matches
the ticket's own terminology) — a copy change across all 13 locale bundles,
guarded by this branch's existing i18n parity check.

## Testing

- `ContractTimelineServiceTest` — mocked repositories, one test per source
  type proving it maps into the right `TimelineEventType`, one test proving
  cross-source sort-by-timestamp interleaves correctly (e.g. an extension
  event between two audit events sorts in the right position), one test
  proving a signature request with no completed signers yet produces
  `SIGNATURE_SENT` and not a fabricated `SIGNATURE_COMPLETED`.
- `ContractTimelineServiceTest` — cross-team: a contract identifier from
  another team resolves to `NotFoundException`, not another team's
  timeline (same pattern as every other contract-scoped endpoint).
- Frontend: `ContractTimeline` component test — one case per event type
  renders the right icon/title, empty state when a contract has no events
  beyond creation.

## Out of scope

- Termination/notice events (ticket item 1 — not built yet on this branch;
  the `TimelineEventType` enum leaves room for a future `NOTICE_GIVEN`
  value, but no code produces it yet — no scaffolding beyond the enum
  slot, matching this branch's established YAGNI discipline).
- Pagination (see Scope decision).
- Migrating existing writers to a normalized event table.
- Filtering/searching within the timeline itself.
