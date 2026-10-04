# E-signature integration — design

BUUR-105 item 2 (of 6 sub-projects decomposed from the ticket). Date: 2026-09-29.

## Intended outcome

A landlord generates a letter or addendum in Buurman today (extension
addendum, rent-change, rent-increase letter) and has to get it signed outside
the product — print, email, chase, scan back in. This project lets them send
that same generated PDF for e-signature from inside Buurman, to all parties in
parallel, and get the signed PDF back into Documents automatically, with a
record of who signed when.

Success looks like: a landlord generates a rent-increase letter, clicks "Send
for signature", the tenant gets an email, signs it, and within a minute the
landlord sees "Signed" on the document with the countersigned PDF attached —
no manual follow-up, no external tool.

Tenants do not yet have a Buurman portal (no `TENANT` principal type exists —
confirmed absent from `TeamRole`). So for this project, tenants sign on the
signing provider's own hosted page via an emailed link, not inside a Buurman
page. The landlord side (send, track, view status) is inside the existing
Buurman app. Full in-app embedding for tenants becomes possible once a tenant
portal exists — out of scope here, and nothing in this design blocks it later
(see S2).

## Decomposition context

BUUR-105 bundles six independent sub-projects (termination workflow,
e-signature, lease-agreement generation, per-country addenda, list search,
timeline). This spec covers e-signature only. Two further scope cuts, agreed
during design:

- **Lease-agreement signing is out of scope.** The ticket's
  `PENDING_SIGNATURE → ACTIVE` contract-status transition is about signing the
  lease agreement itself, but lease-agreement generation (ticket item 3)
  doesn't exist yet. This project builds a document-type-agnostic "send for
  signature" capability and wires it to what already generates PDFs today
  (extension addenda, rent-change, rent-increase letters). No contract-status
  hook, no scaffolding for one — lease-agreement generation adds that when it
  ships.
- **Billing/paid-tier gating is out of scope.** Buurman has no billing
  system at all today (no plan/tier concept on `Team`, no Stripe integration —
  confirmed by grep and by a backoffice comment: "MRR ranking arrives with
  billing"). "Add to a paid tier + per-document fee" needs that foundation
  built first, as its own project. This project ships behind a feature flag
  only, default off, enabled per-team like any other flag today. Revisit
  usage capping/charging once billing exists.

## Provider decision

Evaluated Yousign, Signhost, Dropbox Sign, DocuSign (all paid SaaS, no free
API tier at any usable volume) against self-hosted open-source options.
**Documenso**, self-hosted, was chosen over DocuSeal: DocuSeal's self-hosted
OSS edition does not include API/embedding access at all — that requires a
paid Pro license key ($20/mo + $0.20/document) even when self-hosted.
Documenso's self-hosted Community Edition (AGPL-3.0) includes the API and
embedding SDKs for free; only cosmetic white-label (removing their branding,
deep editor theming) is Enterprise-gated. Given the "no subscription/per-doc
fee" and "consistent experience" requirements, Documenso is the only option
that satisfies both for free.

Documenso self-hosted vs. Documenso Cloud (their hosted SaaS) use the
identical API — switching later is a config change (base URL + API key), not
a rewrite. See S2.

## S1 — Schema

`V077__esignature.sql`:

```sql
CREATE TABLE signature_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    document_id UUID NOT NULL REFERENCES documents (id),
    signed_document_id UUID REFERENCES documents (id),
    provider VARCHAR(32) NOT NULL,
    provider_submission_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_signature_requests_team_identifier UNIQUE (team_id, identifier)
);
CREATE INDEX idx_signature_requests_team ON signature_requests (team_id);
CREATE INDEX idx_signature_requests_document ON signature_requests (document_id);
CREATE UNIQUE INDEX idx_signature_requests_provider_submission
    ON signature_requests (provider, provider_submission_id);

CREATE TABLE signature_signers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signature_request_id UUID NOT NULL REFERENCES signature_requests (id),
    contact_id UUID REFERENCES contacts (id),
    email VARCHAR NOT NULL,
    role VARCHAR(32) NOT NULL,
    provider_signer_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    signed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_signature_signers_request ON signature_signers (signature_request_id);
```

`status` on `signature_requests`: `PENDING`, `PARTIALLY_SIGNED`, `COMPLETED`,
`DECLINED`, `CANCELLED`, `FAILED`. `status` on `signature_signers`: `PENDING`,
`VIEWED`, `SIGNED`, `DECLINED`.

A separate pair of tables rather than columns on `documents`, because a
document can be sent for signature more than once (re-send after decline
creates a new request), and `documents` is shared by every module (photos,
expenses, contracts) that has no reason to carry e-signature columns.
`signature_signers` is the audit trail of who signed when — the existing
`audit_log` table doesn't fit (its `user_id` is `NOT NULL` and modeled for
internal-user actions; an external tenant signer has no Buurman user
identity).

`contact_id` is nullable because the landlord-side signer is a `TeamMember`,
not a `Contact` — `email`/`role` alone identify them.

## S2 — Provider abstraction & config

```java
public interface SignatureProviderClient {
  SignatureSubmission createSubmission(byte[] pdfBytes, String fileName, List<SignerRequest> signers);
  SignedDocument downloadCompleted(String providerSubmissionId);
  boolean verifyWebhookSignature(String payload, String signatureHeader);
}
```

- `DocumensoClient implements SignatureProviderClient` — the only
  implementation. Placed in `buurman-core` (`service/`, `config/`), since it's
  a generic capability and `buurman-letters` already depends on
  `buurman-core`. No new Maven module.
- `DocumensoProperties` (`baseUrl`, `apiKey`, `webhookSigningKey`) —
  `@ConfigurationProperties(prefix = "documenso")`, same shape as
  `MailgunProperties`. Switching self-hosted → Documenso Cloud later is
  changing `baseUrl`/`apiKey` in config, nothing else.
- `DocumensoConfig` — `@Configuration @Profile("!local")`, builds the HTTP
  client bean, same shape as `MailgunConfig`/`TwilioConfig`.
- `provider` column on `signature_requests` is a string, not an enum, in case
  a second provider is ever added — but no second implementation is being
  built now (YAGNI); it costs nothing to leave the column general.

## S3 — API & webhook

```
POST /contracts/{identifier}/documents/{documentId}/signature-requests
GET  /contracts/{identifier}/documents/{documentId}/signature-requests/{id}
```

New `SignatureController` in `buurman-letters` (co-located with the letter
generation endpoints that are its first callers). Creates a
`SignatureRequest` + `SignatureSigner` rows, calls
`DocumensoClient.createSubmission`, returns the request's status.

`POST /webhooks/documenso/events` — new method on the existing
`WebhookController` in `buurman-notifications`, same inline
verify-then-delegate shape as the Mailgun/Twilio handlers: verify via
`DocumensoClient.verifyWebhookSignature` before touching the body, catch and
log processing errors without rethrowing (always ACK 200, so Documenso
doesn't retry-storm a transient bug), delegate business logic to a new
`SignatureWebhookService`.

`SignatureWebhookService`:
- Maps Documenso's per-signer event to `SignatureSigner.status`, using the
  same forward-only `STATUS_RANK` idempotency guard as `WebhookService`'s
  Mailgun/Twilio handling, so a late/duplicate `VIEWED` event can't regress an
  already-`SIGNED` signer.
- When every signer on a request reaches `SIGNED`: calls
  `DocumensoClient.downloadCompleted`, uploads the returned PDF (+ audit
  certificate, stored as a second `Document` row or an attachment field —
  implementation detail for the plan) to S3 via `S3StorageService`, creates
  the `signed_document_id` `Document` row, sets `signature_requests.status =
  COMPLETED`.
- Any signer reaching `DECLINED` sets the whole request to `DECLINED`.

## S4 — Feature flag & rollout

New boolean flag `esignature_enabled` in `feature_flags`
(`default_enabled = false`), checked in `SignatureController` before creating
a request and in the frontend to hide the "Send for signature" action.
Enabled per-team via `feature_flag_overrides`, identical to how other
in-progress features are rolled out today. No usage cap — there's no billing
to attach a cap's rationale to yet (see Decomposition context); a
`FeatureFlags.ESIGNATURE_MAX_REQUESTS`-style integer flag can be added later
using the same pattern `TakeoutService` already uses for export limits,
without a schema change.

## Local dev / infra

- `documenso` service added to `docker-compose.yml`, own database on the
  shared Postgres instance (`documenso` DB, separate from `buurman`), one-time
  signing-certificate generation step added to `scripts/setup-local-certs.sh`
  (Documenso refuses to sign documents without a certificate present at
  startup). No Traefik route needed for outbound backend→Documenso calls
  (same as Gotenberg).
- Webhook reachability gotcha: in the `local` profile, infra runs in Docker
  but the backend runs on the host (`mvn spring-boot:run`), so Documenso's
  webhook POSTs can't reach `localhost:8081` from inside its container. Fix:
  point Documenso's configured webhook URL at `host.docker.internal:8081`
  (Docker Desktop resolves this to the host) rather than a Traefik hostname.
  In the `docker` profile (`make up`), both containers are on the same Docker
  network and the existing service-name DNS (`backend:8081`) works
  unmodified.

## Error handling

- Documenso API unreachable or errors on submission creation →
  `ExternalServiceException` (same convention as `DocumentRenderer`), surfaced
  to the landlord as a retryable failure; `signature_requests.status = FAILED`
  rather than left `PENDING`.
- Webhook idempotency/out-of-order delivery → forward-only status-rank guard
  (S3).
- Declined signer → request `CANCELLED`, landlord notified via existing
  notification infra, original document remains available to send as a new
  request.

## Frontend

"Send for signature" action on a document row (Documents tab / letter
generation results) → status badge (Pending / Partially signed / Signed /
Declined) → expandable signer list with per-signer status. One React Query
hook wrapping the two new generated endpoints, polling while
`PENDING`/`PARTIALLY_SIGNED` (webhook is authoritative; polling covers the gap
until the request completes, no websockets/SSE in this project).

Gated by `esignature_enabled` in `frontend/app/src/constants/featureFlags.ts`,
same pattern as every other flag-gated UI element today.

## Testing

- `DocumensoClientTest` — mocked HTTP client, request/response shape for
  submission creation, download, and signature verification.
- `SignatureWebhookServiceTest` — status-rank idempotency (late/duplicate
  events don't regress state), completion triggers download+upload+status
  update, decline triggers cancellation.
- `SignatureRequestRepositoryIntegrationTest` /
  `SignatureSignerRepositoryIntegrationTest` — team isolation (a request from
  another team is invisible), per the CLAUDE.md-noted multi-tenant test gap.
- `SignatureController` — feature-flag gate returns 404/403 when disabled.
- Frontend: status badge component test, one case per status; polling
  stops once terminal.

## Out of scope

- Lease-agreement generation and its contract-status transition (ticket item
  3 — separate sub-project).
- Per-country addenda/letter templates (ticket item 4 — separate sub-project,
  orthogonal to signing).
- Billing, paid-tier gating, per-document charging (no billing foundation
  exists — separate project, to be filed).
- Tenant-portal-embedded signing (no tenant portal exists at all).
- Sequential signing order (parallel only, per design decision).
- A second signature provider / multi-provider abstraction beyond the single
  `provider` string column already allowing for one later.
