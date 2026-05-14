# Google Sheets Export — Setup Guide

End-to-end instructions for configuring the Google Sheets export feature (BUUR-94). Cover three
audiences:

- **Operators** standing up the feature in a fresh environment.
- **Developers** wiring it into their local dev setup.
- **Future engineers** who need to re-create the Cloud project (e.g. account migration).

The feature uses **OAuth 2.0 with the `drive.file` scope** — a non-sensitive Google scope that
does **not** require app verification or an annual security audit.

---

## 1. Prerequisites

- Access to the **twimba.com** Google Workspace as an admin or as a project owner. The Google
  Cloud project **must** be created under the twimba.com organization (not under a personal
  Google account) so it survives staff turnover.
- A Buurman environment to configure (dev / staging / production).
- The custom domain(s) the app is served from. Defaults:
  - Production: `https://app.buurman.io`
  - Local dev: `https://app.local.buurman.io`

---

## 2. Create the Google Cloud project (one-time)

### 2.1 Create the project

1. Open [console.cloud.google.com](https://console.cloud.google.com/) signed in as a
   **twimba.com** account.
2. Click the project picker (top bar) → **New Project**.
3. Set:
   - **Project name**: `Buurman` (or `Buurman Production` / `Buurman Staging` if you want a
     separate project per environment — see §6).
   - **Organization**: `twimba.com`.
   - **Location**: `twimba.com` (or a sub-folder if your org uses them).
4. Click **Create**. Wait ~30 s for provisioning, then switch the project picker to the new
   project.

### 2.2 Enable the required APIs

1. **APIs & Services** → **Library**.
2. Search for and enable each of:
   - **Google Sheets API**
   - **Google Drive API**

Each enable click takes a few seconds. The page shows "API enabled" once done.

### 2.3 Configure the OAuth consent screen

1. **APIs & Services** → **OAuth consent screen**.
2. Choose **User Type**:
   - Pick **External** so any Google account can use the feature (this is what Buurman customers
     have). Internal would limit consent to twimba.com accounts only.
3. **Create**, then fill the app information:

   | Field | Value |
   |---|---|
   | App name | `Buurman` |
   | User support email | `hello@buurman.io` (or a shared inbox you control) |
   | App logo | Upload `assets/logo/logo_horizontal.png` (≤ 1 MB, square recommended) |
   | App home page | `https://buurman.io` |
   | App privacy policy link | `https://buurman.io/privacy.html` |
   | App terms of service link | `https://buurman.io/terms.html` |
   | Authorized domains | `buurman.io` |
   | Developer contact information | A monitored email at twimba.com |

4. **Save and Continue** → **Scopes** step.
5. Click **Add or Remove Scopes**. In the filter, paste:
   `https://www.googleapis.com/auth/drive.file`
6. Tick **only** that scope. **Do NOT add** `drive`, `drive.readonly`, `spreadsheets`, or any
   other Drive/Sheets scope — those are *sensitive* and trigger app verification.
7. **Update** → **Save and Continue**.
8. **Test users** step: while the project is in **Testing** publishing status, add the email
   addresses you want to use during initial setup (yours and a couple of teammates'). Up to 100
   testers allowed.
9. **Save and Continue** → review summary → **Back to Dashboard**.

> **Publishing status note.** Keep the consent screen in **Testing** mode during initial QA. To
> ship to all users, click **Publish App** on the OAuth consent screen. Because the only scope
> is `drive.file` (non-sensitive), **no Google review is required** — publishing is instant. If
> you ever add a sensitive scope, expect a multi-week verification process plus an annual
> third-party security assessment.

### 2.4 Create the OAuth 2.0 Client ID

1. **APIs & Services** → **Credentials**.
2. **+ Create Credentials** → **OAuth client ID**.
3. **Application type**: `Web application`.
4. **Name**: `Buurman Web` (or `Buurman Production` / `Buurman Staging`).
5. **Authorized JavaScript origins** — add an entry for **each** environment the client will be
   used in:

   ```
   https://app.buurman.io
   https://app.local.buurman.io
   ```

   (Add more entries if you have staging or per-PR review domains, e.g.
   `https://app.staging.buurman.io`.)

6. **Authorized redirect URIs** — leave **empty**. We use the GIS popup token-client flow, not the
   server-side redirect flow.
7. Click **Create**.
8. Copy the **Client ID** from the dialog (looks like
   `1234567890-abc...xyz.apps.googleusercontent.com`). **You won't see the secret** — we don't
   need it for the popup flow, since the client is public. Even if Google shows a client secret,
   ignore it for this feature.

> **Store the Client ID in 1Password / your secret manager** under "Buurman / Google OAuth Client
> ID". It's not a secret strictly speaking (it ships to browsers), but tracking ownership is
> useful for the next engineer.

---

## 3. Set `VITE_GOOGLE_OAUTH_CLIENT_ID` in the frontend

The frontend reads the OAuth client id from `import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID` (see
`frontend/app/src/hooks/useGoogleAccessToken.ts`). It must be defined at **build time**, since
Vite inlines `import.meta.env.VITE_*` constants into the bundle.

### 3.1 Local development

Create `frontend/app/.env.local` (gitignored — never commit):

```env
# Google Sheets export — OAuth client id for the local dev environment.
# Get this from the GCP project's OAuth 2.0 Client ID list. See docs/GOOGLE_SHEETS_SETUP.md.
VITE_GOOGLE_OAUTH_CLIENT_ID=1234567890-abc...xyz.apps.googleusercontent.com
```

Restart the dev server (`yarn dev`) to pick up the new env var.

If the variable is not set, the export button will still render, but clicking it will show a
toast ("Google Sheets export is not configured for this environment") and abort cleanly — no
crash, no broken UI.

### 3.2 Docker dev / staging / production

`VITE_*` env vars must be set when the frontend Docker image is **built**, not just at runtime
(Vite is a build-time bundler). In our CI pipeline this means setting the variable in the
Buildkite / GitHub Actions job that runs `yarn build`:

```yaml
# .github/workflows/deploy.yml (or equivalent)
env:
  VITE_GOOGLE_OAUTH_CLIENT_ID: ${{ secrets.GOOGLE_OAUTH_CLIENT_ID_PROD }}
```

Set the secret in GitHub Actions / Buildkite / wherever your build env vars live. Same for
staging with a separate client ID if you use one (recommended — see §6).

If you bake the production frontend through a Dockerfile, pass it as a `--build-arg`:

```dockerfile
ARG VITE_GOOGLE_OAUTH_CLIENT_ID=""
ENV VITE_GOOGLE_OAUTH_CLIENT_ID=$VITE_GOOGLE_OAUTH_CLIENT_ID
RUN yarn build
```

Build invocation:

```bash
docker build \
  --build-arg VITE_GOOGLE_OAUTH_CLIENT_ID="1234567890-...apps.googleusercontent.com" \
  -t buurman-app:latest .
```

### 3.3 Verifying it's wired

After deploying, open the built `index.html` in DevTools → **Network** → **JS bundle** → search
for the first 10 chars of your client ID. It should appear in the minified bundle as a string
literal. If it's missing or shows `undefined`, the build didn't pick up the env var.

---

## 4. Enable the feature flag

The backend ships with `google_sheets_export = false` (see
`backend/buurman-common/src/main/java/com/buurman/util/FeatureFlags.java`).

To enable it for a team, use the backoffice flag-override UI (same UI used to flip `excel_export`):

1. Backoffice → **Feature flags**.
2. Find `google_sheets_export`.
3. Add a per-team override → set to `true` for the target team.

No Flyway migration is needed (per the locked spec — flag enablement is operator-driven for v1;
plan-driven defaults are a follow-up).

---

## 5. Smoke test the end-to-end flow

After steps 2–4 are done in a target environment:

1. Sign in as a user in a team with `google_sheets_export = true`.
2. Open **Contacts** → click the export dropdown → **Google Sheets**.
3. A Google popup appears. Sign in (use a tester account if the consent screen is still in
   Testing mode).
4. Click **Continue** to grant the `drive.file` scope.
5. After a moment, a success toast appears with the sheet URL.
6. Open your Google Drive → confirm a new folder **Buurman exports** exists with a sheet named
   `Buurman — Contacts — {today}`.
7. Open the sheet — confirm headers are bold + tinted, the first row is frozen, dates render as
   dates, and rows have banded striping.

Repeat for: Transactions (Reports page), Property Dashboard, Portfolio Dashboard, and the full
**Takeout → Export to Google Sheets** option.

If the popup fails:
- "popup blocked" — instruct the user to allow popups for `app.buurman.io`.
- "redirect_uri_mismatch" — the JS origin in §2.4 doesn't match the URL the user is on.
- "access blocked: this app is blocked" — the consent screen is still in Testing mode and the
  user isn't a registered tester. Either add them as a tester or publish the app.

---

## 6. Recommended: separate projects per environment

For real separation between dev / staging / production, create three Cloud projects:

- `Buurman` (production)
- `Buurman Staging`
- `Buurman Dev` (shared by all devs for `app.local.buurman.io`)

Each gets its own OAuth Client ID, JS origins, and consent screen. Benefits:

- Mistakes during testing can't leak production telemetry.
- Disabling/rotating the dev client doesn't break production.
- Per-environment quotas don't interfere.

If only one project is acceptable, list **all** environment origins under a single OAuth Client
ID and share the same `VITE_GOOGLE_OAUTH_CLIENT_ID` everywhere.

---

## 7. Rotation / disabling

- **Rotate the OAuth Client ID**: create a new client in Credentials, deploy the new
  `VITE_GOOGLE_OAUTH_CLIENT_ID`, then delete the old client in GCP. Users on existing tabs will
  hit an error on next export and silently re-consent against the new client.
- **Kill the feature globally**: flip the `google_sheets_export` flag to `false` in the
  backoffice. Frontend hides the buttons; backend rejects calls with 403.
- **User wants to revoke access**: send them to
  [myaccount.google.com/permissions](https://myaccount.google.com/permissions). They click
  **Buurman** → **Remove Access**. Next export re-prompts.

---

## 8. Quotas and limits

Per the [Google Sheets API quotas](https://developers.google.com/sheets/api/limits):

- **300 read requests / minute / project** — never hit; we only write.
- **300 write requests / minute / project** — divided across all users. Will likely cap us
  around 60 simultaneous exports per minute. If we breach this we'll see 429s; the backend maps
  them to a retryable error (HTTP 503) with a friendly message.
- **60 requests / minute / user** — per-user; rarely hit.
- **10M cells per spreadsheet** — relevant for the takeout export of a very large team. The
  backend caps rows per tab (see `TakeoutGoogleSheetService.MAX_ROWS_PER_TAB`) and adds a
  truncation note when the cap is reached.

To raise project-level quotas, file a quota increase request from the Google Cloud Console under
**APIs & Services** → **Sheets API** → **Quotas**.

---

## 9. Privacy / compliance summary

- The OAuth scope is `drive.file` only — non-sensitive, no app verification needed.
- The user's OAuth access token is never persisted by Buurman; it lives only on the request
  stack for a single export call.
- The privacy policy at `https://buurman.io/privacy.html` discloses the integration explicitly.
- Buurman's logging excludes the access token: see
  `backend/buurman-app/src/main/resources/logback-spring.xml` (the `com.google.api.client.http`
  logger is pinned to `WARN`).

---

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Button hidden | Flag `google_sheets_export` is `false` for the team | Enable via backoffice |
| Toast: "Google Sheets export is not configured for this environment" | `VITE_GOOGLE_OAUTH_CLIENT_ID` missing at build time | Set the env var and **rebuild** the frontend |
| Popup: "Access blocked: this app is blocked" | Consent screen in Testing mode + user not a tester | Add tester OR publish consent screen |
| Popup: "redirect_uri_mismatch" / origin error | JS origin in §2.4 doesn't list the current URL | Add the URL under Authorized JavaScript origins, save, wait ≈ 5 min for propagation |
| Popup: **"Error 400: origin_mismatch"** / "You can't sign in to this app because it doesn't comply with Google's OAuth 2.0 policy" | The exact `window.location.origin` of the page launching the popup isn't in **Authorized JavaScript origins** on the OAuth client | DevTools → Console → run `window.location.origin` to get the exact string Google expects. Copy verbatim (no trailing slash, exact protocol/host/port) into GCP Console → APIs & Services → Credentials → OAuth client → Authorized JavaScript origins → Save → **wait 5–10 minutes** for propagation → hard-refresh (Cmd-Shift-R). Common gotchas: running Vite directly on `http://localhost:5173` instead of through Traefik (`https://app.local.buurman.io`) — register both if devs use both. |
| Toast: "Google sign-in expired" | User revoked access OR token expired (Google's tokens last ~1 hour) | User re-clicks the export button; popup re-prompts |
| Toast: "Google API quota exceeded" | 429 from Google | Wait a minute and retry; if persistent, file a quota increase |
| Sheet has wrong tab order | Order is the app sidebar; verify against `TakeoutGoogleSheetService.TABS` | Sidebar-menu order is canonical per spec |
| Sheet rows truncated with a note | Takeout exceeded `MAX_ROWS_PER_TAB` for that entity | Use per-entity export for the affected entity, or request a higher cap |

For unhandled errors, check `/Users/luis.santos/.superset/worktrees/db325d38-efda-455c-9d5d-86639253ecc8/google-sheets-export/grafana` dashboards
for the `buurman_google_sheets_export_*` Prometheus metrics — they'll show outcome distribution
by entity and surface any new failure modes.
