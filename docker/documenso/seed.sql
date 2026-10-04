-- Seeds a fixed, zero-config dev identity (user + personal org + team + API token + login
-- password) for the self-hosted Documenso sidecar, so `make up`/`make dev` boots with a working
-- DOCUMENSO_API_KEY and a usable UI login, instead of requiring an interactive first-run signup.
--
-- Every row here mirrors the exact shape Documenso's own signup flow creates (verified against
-- the "Service Account" row its own migrations seed) — same tables, same required columns, same
-- token hashing (see apps/remix/build/server/hono/packages/lib/server-only/auth/hash.js:
-- `crypto.createHash('sha512').update(token).digest('hex')`, unsalted) and the same password
-- hashing (@node-rs/bcrypt, 12 salt rounds — pgcrypto's crypt()/gen_salt('bf', 12) produces a
-- cross-compatible hash). Safe only because these fixed credentials are public (committed to
-- git) and this stack never carries real traffic.
--
-- Runs once per fresh Documenso schema via the one-shot `documenso-seed` compose service, after
-- Documenso's own `prisma migrate deploy` has created these tables. Idempotent: every insert is
-- guarded by ON CONFLICT DO NOTHING keyed on the same unique column re-run would collide on.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

\set dev_api_key 'api_buurman_dev_seed_token_insecure'
\set dev_email 'buurmy@buurman.io'
\set dev_password 'buurmy'
\set dev_team_url 'buurman-dev-team'

-- Migrates an already-seeded workspace from the identity this script used before the dev
-- login became buurmy@buurman.io — a no-op (0 rows) on a fresh schema or one already migrated.
UPDATE "User" SET email = :'dev_email' WHERE email = 'dev@buurman.local';

-- Buurman's own square icon mark (frontend/app/public/assets/logo/logo_square_no_text.png),
-- resized to 120x120 for email use. brandingLogo's column holds Documenso's own self-contained
-- "stored file" JSON shape, {"type":"BYTES_64","data":<base64>} — see
-- apps/remix/build/server/hono/packages/lib/universal/upload/put-file.server.js's
-- putFileInDatabase, which is exactly what writes this shape when NEXT_PUBLIC_UPLOAD_TRANSPORT
-- is unset (our case; no S3/Azure configured for Documenso's own uploads).
\set logo_b64 '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'

INSERT INTO "OrganisationClaim" (
  id, "createdAt", "updatedAt", "originalSubscriptionClaimId",
  "teamCount", "memberCount", flags, "envelopeItemCount",
  "apiQuota", "apiRateLimits", "documentQuota", "documentRateLimits",
  "emailQuota", "emailRateLimits", "recipientCount"
) VALUES (
  'buurman_dev_claim', now(), now(), 'free',
  -- hidePoweredBy: true drops the "This document was sent using Documenso." email footer line
  -- (see email/template-components/template-footer.js) — a plain claim flag, not gated behind
  -- IS_BILLING_ENABLED() the way brandingColors is, and billing is off entirely in this
  -- self-hosted instance, so there's no licensing reason to keep it.
  1, 1, '{"hidePoweredBy": true}'::jsonb, 5,
  NULL, '[]'::jsonb, NULL, '[]'::jsonb,
  NULL, '[]'::jsonb, 0
) ON CONFLICT (id) DO UPDATE SET flags = EXCLUDED.flags;

-- brandingColors.primary (#0284c7) and the logo match Buurman's own email header
-- (backend/buurman-app/src/main/resources/templates/email/_email-base.html) — signing emails
-- should read as coming from Buurman, not a third-party e-signature tool.
-- ON CONFLICT DO UPDATE (not DO NOTHING) so re-running the seed against an already-seeded
-- workspace still picks up branding changes made to this script.
INSERT INTO "OrganisationGlobalSettings" (
  id, "emailDocumentSettings", "brandingEnabled", "brandingLogo", "brandingCompanyDetails",
  "brandingColors"
) VALUES (
  'buurman_dev_org_settings',
  '{"documentDeleted": true, "documentPending": true, "recipientSigned": true, "recipientRemoved": true, "documentCompleted": true, "ownerDocumentCompleted": true, "recipientSigningRequest": true}'::jsonb,
  true,
  '{"type":"BYTES_64","data":"' || :'logo_b64' || '"}',
  'Buurman',
  '{"primary": "#0284c7", "primaryForeground": "#ffffff"}'::jsonb
) ON CONFLICT (id) DO UPDATE SET
  "brandingEnabled" = EXCLUDED."brandingEnabled",
  "brandingLogo" = EXCLUDED."brandingLogo",
  "brandingCompanyDetails" = EXCLUDED."brandingCompanyDetails",
  "brandingColors" = EXCLUDED."brandingColors";

INSERT INTO "OrganisationAuthenticationPortal" (id)
VALUES ('buurman_dev_org_sso')
ON CONFLICT (id) DO NOTHING;

-- password uses pgcrypto's bcrypt (crypt/gen_salt('bf', 12)) — cross-compatible with
-- Documenso's own @node-rs/bcrypt hashing (same algorithm, same 12 salt rounds; verified).
-- ON CONFLICT DO UPDATE (not DO NOTHING) so re-running the seed against an already-seeded
-- user still sets the fixed dev password — this is a disposable local-dev account, not one
-- anyone relies on keeping a manually-changed password.
INSERT INTO "User" (name, email, password, "emailVerified", roles, disabled)
VALUES (
  'Buurman Dev', :'dev_email', crypt(:'dev_password', gen_salt('bf', 12)), now(),
  ARRAY['USER']::"Role"[], false
) ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password;

SELECT id AS user_id FROM "User" WHERE email = :'dev_email' \gset

INSERT INTO "Organisation" (
  id, "createdAt", "updatedAt", type, name, url,
  "ownerUserId", "organisationClaimId", "organisationGlobalSettingsId", "organisationAuthenticationPortalId"
) VALUES (
  'buurman_dev_org', now(), now(), 'PERSONAL', 'Buurman Dev', 'buurman-dev',
  :user_id, 'buurman_dev_claim', 'buurman_dev_org_settings', 'buurman_dev_org_sso'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO "OrganisationMember" (id, "createdAt", "updatedAt", "userId", "organisationId")
VALUES ('buurman_dev_member', now(), now(), :user_id, 'buurman_dev_org')
ON CONFLICT (id) DO NOTHING;

-- Envelope creation checks team membership via Team -> TeamGroup -> OrganisationGroup ->
-- OrganisationGroupMember -> OrganisationMember (see utils/teams.js's buildTeamWhereQuery) —
-- an ApiToken resolving to a valid user/team is NOT sufficient on its own. This is the minimal
-- ADMIN-rooted triangle real signups get several of (3 org-level + 3 team-level default groups);
-- one is enough to satisfy the membership check.
INSERT INTO "OrganisationGroup" (id, type, "organisationRole", "organisationId")
VALUES ('buurman_dev_group_admin', 'INTERNAL_ORGANISATION', 'ADMIN', 'buurman_dev_org')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "TeamGlobalSettings" (id)
VALUES ('buurman_dev_team_settings')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "Team" (name, url, "organisationId", "teamGlobalSettingsId")
VALUES ('Buurman Dev', :'dev_team_url', 'buurman_dev_org', 'buurman_dev_team_settings')
ON CONFLICT (url) DO NOTHING;

SELECT id AS team_id FROM "Team" WHERE url = :'dev_team_url' \gset

INSERT INTO "TeamGroup" (id, "organisationGroupId", "teamRole", "teamId")
VALUES ('buurman_dev_team_group_admin', 'buurman_dev_group_admin', 'ADMIN', :team_id)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "OrganisationGroupMember" (id, "groupId", "organisationMemberId")
VALUES ('buurman_dev_group_member', 'buurman_dev_group_admin', 'buurman_dev_member')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "ApiToken" (name, token, algorithm, "userId", "teamId")
VALUES (
  'Buurman Dev Seed Token',
  encode(digest(:'dev_api_key', 'sha512'), 'hex'),
  'SHA512',
  :user_id,
  :team_id
) ON CONFLICT (token) DO NOTHING;

-- Without this, Buurman's backend never hears about a signature until someone happens to poll —
-- the "Documenso" UI shows progress immediately, but Buurman's own "Pending N/M signed" only
-- updates when this webhook fires (see SignatureWebhookService.processDocumensoEvent).
-- secret must match DOCUMENSO_WEBHOOK_SECRET on the backend side (DocumensoClient does a plain
-- constant-time string compare, not HMAC — see execute-webhook-call.js's
-- 'X-Documenso-Secret': secret header). Targets the host-run backend's workspace-specific port
-- (local/`make dev` profile); see NEXT_PRIVATE_WEBHOOK_SSRF_BYPASS_HOSTS in docker-compose.yml
-- for why host.docker.internal doesn't get rejected by Documenso's own SSRF guard.
-- ON CONFLICT DO UPDATE so re-running the seed (e.g. after a workspace's port changes) corrects
-- an already-registered webhook instead of leaving it stale.
INSERT INTO "Webhook" (
  id, "webhookUrl", "eventTriggers", secret, enabled, "userId", "teamId"
) VALUES (
  'buurman_dev_webhook',
  'http://host.docker.internal:' || :backend_webhook_port::text || '/webhooks/documenso/events',
  ARRAY['DOCUMENT_OPENED', 'DOCUMENT_SIGNED', 'DOCUMENT_COMPLETED', 'DOCUMENT_REJECTED',
        'DOCUMENT_CANCELLED']::"WebhookTriggerEvents"[],
  :'webhook_secret',
  true,
  :user_id,
  :team_id
) ON CONFLICT (id) DO UPDATE SET
  "webhookUrl" = EXCLUDED."webhookUrl",
  "eventTriggers" = EXCLUDED."eventTriggers",
  secret = EXCLUDED.secret,
  enabled = EXCLUDED.enabled;
