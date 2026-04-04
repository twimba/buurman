# Buurman Frontend Codebase Assessment

**Date:** 2026-04-04
**Scope:** `frontend/` — `app/` (main tenant app), `backoffice/` (admin app), `packages/ui/` (shared library)
**Stack:** React 19, TypeScript, Vite 7, TanStack React Query 5, Tailwind CSS 4, Keycloak 26, Axios
**Codebase size:** ~94k lines of TypeScript/TSX across ~300 files

**Review panel:** React Engineer, Security Engineer (red+blue team), Distinguished Architect, Principal Engineer, SRE, Engineering Manager

---

## Executive Summary

The codebase is well-structured for its stage: TypeScript strict mode throughout, zero `any` usage, consistent React Query patterns, a growing shared UI library, and a clean API -> hooks -> components -> pages layering. The architecture is sound and the patterns are LLM-friendly.

However, the codebase is **not ready for public internet exposure** without addressing the security findings below. There are also significant opportunities to reduce duplication, improve resilience, and establish the tooling foundation needed for long-term team scaling.

**Hard blockers for public launch:** 7 items (2 critical XSS, no CSP, disabled OIDC nonce, exposed source maps, leaked API key, unsanitized iframe path)

---

## Priority Tiers

| Tier | Meaning | Count |
|------|---------|-------|
| **P0 — Launch Blockers** | Must fix before public internet exposure | 10 |
| **P1 — High Priority** | Should fix soon after launch / blocks team scaling | 18 |
| **P2 — Medium Priority** | Improves quality, DX, and long-term maintainability | 20 |
| **P3 — Nice to Have** | Polish, minor consistency improvements | 12 |

---

## P0 — Launch Blockers

These items must be resolved before the application is exposed to the public internet. They represent security vulnerabilities, missing safety infrastructure, or gaps that could lead to data exposure.

---

### SEC-01: Stored XSS via Unsanitized HTML in ImpersonationBanner
**Severity:** CRITICAL | **Effort:** S | **Agents:** Security, React

**File:** `app/src/components/ImpersonationBanner.tsx:64-67`

The `reason` field from the impersonation session is rendered with raw `__html` without DOMPurify sanitization. Every other similar site in the codebase uses DOMPurify — this one was missed.

**Attack scenario:** A backoffice admin sets a malicious `reason` string containing event-handler payloads. When the victim's browser renders the banner, the script exfiltrates the impersonation JWT from sessionStorage.

**Fix:** One-line change — wrap with `DOMPurify.sanitize(reason)`.

---

### SEC-02: Stored XSS via Unsanitized HTML in ExtensionTimeline
**Severity:** CRITICAL | **Effort:** S | **Agents:** Security, React, Principal

**File:** `app/src/components/contracts/ExtensionTimeline.tsx:537-541`

`extension.notes` (user-supplied rich text) is rendered as raw `__html` with zero sanitization.

**Attack scenario:** A TEAM_EDITOR saves a contract extension note containing script or event-handler payloads. Any team member viewing the extension timeline executes the payload under their session.

**Fix:** One-line change — wrap with `DOMPurify.sanitize(extension.notes)`, or use the existing `RichTextDisplay` component.

---

### SEC-03: No Content Security Policy (CSP)
**Severity:** HIGH | **Effort:** M | **Agents:** Security, SRE

**Files:** `app/nginx.conf`, `backoffice/nginx.conf`

Neither nginx config defines a `Content-Security-Policy` header. Without CSP, any XSS vulnerability has unlimited capability — inline scripts execute, any external origin can be fetched, data exfiltration is unrestricted. The other security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`) are all correctly present.

**Why it matters:** CSP is the last line of defense against XSS. Even after fixing SEC-01 and SEC-02, future code changes could introduce new XSS vectors. CSP ensures those vectors cannot execute arbitrary scripts or exfiltrate data.

**Fix:** Add CSP header to both nginx.conf files. Start strict and relax per-resource:
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' https://keycloak.* https://api.* https://eu.posthog.com; frame-ancestors 'none';" always;
```

---

### SEC-04: `useNonce: false` Disables OIDC Replay Protection
**Severity:** HIGH | **Effort:** S | **Agents:** Security, SRE

**Files:** `app/src/config/keycloak.ts:32`, `backoffice/src/config/keycloak.ts:31`

Both Keycloak configs explicitly disable nonce validation with a comment saying it's a development workaround. The nonce check is the primary defense against authorization code replay attacks.

**Why it matters:** An attacker who intercepts an authorization code response can replay it without the nonce check catching the substitution. This is especially concerning with PKCE S256 (which is correctly enabled) because nonce and PKCE are orthogonal protections.

**Fix:** Remove `useNonce: false` from both files. Debug any resulting validation errors against Keycloak 26 rather than disabling the security control.

---

### SEC-05: Source Maps Published to Production
**Severity:** HIGH | **Effort:** S | **Agents:** Security, SRE

**Files:** `app/vite.config.ts:39`, `backoffice/vite.config.ts:21`

Both Vite configs set `sourcemap: true` unconditionally. Source maps are served by nginx alongside JS bundles, exposing the complete original TypeScript source to anyone who downloads `*.js.map` files.

**Why it matters:** Exposes all business logic, API endpoint paths, internal identifiers, error handling, and any accidentally compiled secrets. Combined with hardcoded URLs like `GITHUB_REPO = "https://github.com/twimba/buurman"` in `backoffice/src/pages/SystemInfoPage.tsx:33`, this substantially reduces attacker reconnaissance cost.

**Fix:** Use `sourcemap: 'hidden'` (generates maps for error tracking upload but doesn't reference them from bundles) or gate on environment:
```ts
sourcemap: process.env.VITE_ENVIRONMENT === 'production' ? 'hidden' : true
```

---

### SEC-06: Google Maps API Key Baked into Bundle
**Severity:** HIGH | **Effort:** S | **Agents:** Security

**Files:** `app/src/components/common/AddressMap.tsx:35`, `app/src/components/properties/PropertyMap.tsx:33`

These two components use `import.meta.env.VITE_GOOGLE_MAPS_API_KEY` (build-time baked-in) instead of the runtime `env()` helper used everywhere else. The key is compiled into the production JS bundle. A third component (`InteractiveMap.tsx:123`) correctly uses `env('VITE_GOOGLE_MAPS_API_KEY')`.

**Why it matters:** The API key is visible to anyone inspecting the bundle or source maps. If the key has no HTTP referrer restrictions in Google Cloud Console, it can be used by anyone for any Google Maps API calls, billed to your account.

**Fix:** Replace `import.meta.env.VITE_GOOGLE_MAPS_API_KEY` with `env('VITE_GOOGLE_MAPS_API_KEY')` in both files. Ensure the production key has HTTP referrer restrictions.

---

### SEC-07: Unsanitized `path` Parameter in Backoffice ToolEmbedPage Iframe
**Severity:** MEDIUM | **Effort:** S | **Agents:** Security

**File:** `backoffice/src/pages/ToolEmbedPage.tsx:50-51`

The `path` query parameter is appended directly to a trusted base URL and loaded in an iframe. The iframe sandbox includes `allow-same-origin allow-scripts` together, which negates most sandbox protections.

**Fix:** Validate `path` against an allowlist of known safe paths per tool. Remove `allow-same-origin` from the sandbox if possible.

---

### SRE-01: No Error Reporting Service (Sentry or Equivalent)
**Severity:** HIGH | **Effort:** M | **Agents:** SRE, Security, React

**Files:** `app/src/components/ErrorBoundary.tsx:22`, `backoffice/` (no ErrorBoundary at all)

`ErrorBoundary.componentDidCatch` only calls `console.error`. No external error reporting service is configured. In production, user-facing crashes are completely invisible to the team.

**Why it matters:** Without crash reporting, you cannot detect, diagnose, or prioritize production errors. Users will silently churn instead of reporting issues. This is the #1 observability gap.

**Fix:** Integrate Sentry (or equivalent). Add `Sentry.captureException(error, { extra: errorInfo })` in `componentDidCatch`. Upload source maps to Sentry (which pairs well with SEC-05's `'hidden'` mode).

---

### SRE-02: No Axios Request Timeout
**Severity:** HIGH | **Effort:** S | **Agents:** SRE

**Files:** `app/src/api/client.ts`, `backoffice/src/api/client.ts`

Neither Axios instance configures a `timeout`. A slow or hung backend call will pend indefinitely with no user feedback.

**Why it matters:** A single hung backend endpoint can make the entire UI appear frozen. Users will rage-click, creating cascading requests.

**Fix:** Add `timeout: 15000` to the `axios.create` config in both clients.

---

### SRE-03: ErrorBoundary Exposes Raw Error Messages in Production
**Severity:** MEDIUM | **Effort:** S | **Agents:** SRE, Security, Principal

**File:** `app/src/components/ErrorBoundary.tsx:37-46`

The `ErrorBoundary` renders `this.state.error.message` directly in a collapsible `<details>` element visible to all users. Error messages can contain internal stack traces, component names, API URLs, or data payloads.

**Fix:** In production, replace with a generic user-facing message. Keep detailed errors only in development. Send full errors to Sentry instead.

---

## P1 — High Priority

These should be addressed soon after launch. They impact team scaling, developer productivity, code correctness, and long-term maintainability.

---

### ARCH-01: Refactor `App.tsx` to Nested Layout Routing
**Effort:** M | **Agents:** Architect, React, EM, Principal

**File:** `app/src/App.tsx` (660 lines)

The app wraps every route individually in `<ProtectedRoute><Layout>...</Layout></ProtectedRoute>`, repeated ~30 times. The backoffice correctly uses React Router v7's nested layout pattern (`<Route element={<Layout />}>...children</Route>`) and is only 194 lines.

**Why it matters:** New routes require 7 lines instead of 1. The file is a merge conflict hotspot. LLMs must navigate 660 lines of repetitive structure to insert a route. The backoffice already demonstrates the correct pattern.

---

### ARCH-02: Introduce `queryKeys.ts` Registry
**Effort:** M | **Agents:** Architect, React, EM

**Affected:** All 35+ hook files in `app/src/hooks/`

Query keys are raw string literals scattered across ~40 files (`'properties'`, `'contracts'`, `'dashboard'`, `'propertyDashboard'`). No central registry exists. A typo silently creates cache isolation bugs. Rename refactors require grepping the entire codebase.

**Why it matters:** TanStack Query's own docs recommend a key factory pattern. Cache invalidation bugs from mismatched keys are silent and hard to diagnose. LLMs adding new hooks must search the codebase to discover existing key strings.

**Fix:** Create `app/src/lib/queryKeys.ts`:
```ts
export const queryKeys = {
  properties: { all: () => ['properties'], detail: (id: string) => ['property', id] },
  contracts: { all: (params?: object) => ['contracts', params], detail: (id: string) => ['contract', id] },
  // ...
}
```

---

### ARCH-03: Merge `contexts/` into `context/`
**Effort:** S | **Agents:** Architect, EM, React

**File:** `app/src/contexts/AuthContext.tsx` -> move to `app/src/context/AuthContext.tsx`

`AuthContext` lives in `contexts/` (plural) while all other contexts live in `context/` (singular). This causes import path inconsistency and is an LLM footgun — generated imports will be wrong ~50% of the time.

---

### ARCH-04: Decide on API Strategy (Manual vs. Generated)
**Effort:** M (decision) + L (migration) | **Agents:** Architect, EM, Principal

**Files:** `app/src/api/*.ts` (32 manual modules) vs `app/src/generated/api/` (Orval-generated)

Two parallel API layers coexist. The Orval-generated client is largely unused — only ~24 files import from `generated/`. The manual `api/*.ts` modules are used by all hooks. The manual `types/*.ts` files re-export some enums from generated models but define response/request interfaces manually, creating a drift risk.

**Why it matters:** Adding a new endpoint requires touching both layers (or knowing to ignore the generated one). The manual `UpdatePropertyRequest` with 40+ fields must stay in sync with the OpenAPI spec by hand. Each field mismatch is a silent runtime bug.

**Options:**
1. **Commit to Orval-generated clients** — migrate hand-written modules one domain at a time, eliminate manual type interfaces
2. **Keep manual layer** — remove Orval generation, own the types explicitly
3. **Status quo** — document which layer is authoritative (least recommended)

---

### DUP-01: Migrate `RichTextEditor` + `RichTextDisplay` to `@buurman/ui`
**Effort:** M | **Agents:** Architect, React, Principal

**Files:** `app/src/components/common/RichTextEditor.tsx` (395 lines) <-> `backoffice/src/components/RichTextEditor.tsx` (396 lines)

The largest duplication in the codebase. Near-identical Tiptap extension setup, DOMPurify sanitization, keyboard shortcuts. Any bug fix must be applied twice. **Divergence in sanitization logic is a security risk** (see SEC-01/SEC-02 for what happens when sanitization is missed).

Additionally, `RichTextDisplay` has **three** copies: `app/src/components/common/`, `app/src/components/ui/`, and `backoffice/src/components/`. The two app copies use different prop names (`content` vs `html`).

---

### DUP-02: Migrate `usePagination` to `@buurman/ui`
**Effort:** S | **Agents:** Architect, React, Principal

**Files:** `app/src/hooks/usePagination.ts` <-> `backoffice/src/hooks/usePagination.ts`

Byte-for-byte identical logic. The companion `useFilterState` was already correctly placed in `packages/ui`. Apply the same treatment to `usePagination`.

---

### DUP-03: Replace App's `ToastContext` with `@buurman/ui` Toast
**Effort:** M | **Agents:** Architect, Principal

**Files:** `app/src/context/ToastContext.tsx` (92 lines, custom) vs `packages/ui/src/components/Toast.tsx` (shared)

The app uses a custom `ToastContext` while `@buurman/ui` exports a strictly better version with `useCallback`, `aria-live`, and `role="status"`. The custom version has no ARIA attributes — screen readers cannot announce toasts.

---

### QUAL-01: Fix `undefined as unknown as number` Type Casts
**Effort:** S | **Agents:** Principal

**Files:** `app/src/components/properties/financials/modals/InsuranceFormModal.tsx:47`, `FinancingFormModal.tsx:47`, `FinancingPaymentFormModal.tsx:66`

The pattern `existing?.annualPremium ?? (undefined as unknown as number)` defeats TypeScript's ability to catch missing required fields. The request DTOs declare required `number` fields but form state legitimately needs them to be `undefined` initially.

**Fix:** Create separate form-state types with `number | undefined`, or use `Partial<CreateInsuranceRequest>` for form state.

---

### QUAL-02: Decompose God-Components (PropertyDetailPage, ContractDetailPage)
**Effort:** L | **Agents:** React, Principal, Architect

**Files:** `app/src/pages/PropertyDetailPage.tsx` (2,736 lines), `ContractDetailPage.tsx` (1,818 lines), `ContractForm.tsx` (1,703 lines)

`PropertyDetailPage` manages ~15 `useState` items, 3 embedded sub-tables with inline sort/filter/pagination, 10+ mutation calls, and renders 8 tabs inline. A render error anywhere kills the entire page.

**Why it matters:** These files exceed most LLM context windows. Any feature change requires reading thousands of lines to find the right section. The inline contract/expense tables duplicate logic that already exists in dedicated hooks.

**Fix:** Extract `PropertyContractsTab`, `PropertyExpensesTab`, `PropertyOccupancyTab` as self-contained tab components. Each tab owns its own query hooks. The `useTabState` hook is already in place.

---

### QUAL-03: Remove Direct `client` Imports from Page Components
**Effort:** S | **Agents:** Architect, Principal

**Files:** `PropertyDetailPage.tsx:63`, `ContractDetailPage.tsx:41`, `ContactDetailPage.tsx:73`, `TransactionHistoryPage.tsx:18`

These pages import the Axios `client` directly for binary downloads (PDF/CSV/Excel), bypassing the API module layer. The download logic uses `alert()` for errors instead of `showToast`, creating inconsistent UX.

**Fix:** Create download functions in the relevant `api/*.ts` modules. Replace `alert()` with `showToast`.

---

### QUAL-04: `ContractForm` Fetches ALL Properties for Single Lookup
**Effort:** S | **Agents:** React

**File:** `app/src/components/contracts/ContractForm.tsx:342-350`

`useProperties()` is called without params — fetching ALL properties for the team — just to `Array.find()` one matching `formData.propertyIdentifier`. For a landlord with 100+ properties, this is a full-page query for a single lookup.

**Fix:** Use `useProperty(formData.propertyIdentifier)` (the single-entity hook that already exists).

---

### TOOL-01: Delete Legacy `.eslintrc.json` (Duplicate Config)
**Effort:** S | **Agents:** Architect, EM

**Files:** `app/.eslintrc.json` (legacy) AND `app/eslint.config.mjs` (flat config) both exist

Two ESLint configs can cause ESLint to silently use one and ignore the other. Additionally, `eslint-plugin-react-hooks` is installed but never activated in the legacy config.

**Fix:** Delete `app/.eslintrc.json`. Keep only the flat config. Ensure `react-hooks` rules are active.

---

### TOOL-02: Add ESLint Config to Backoffice
**Effort:** S | **Agents:** EM

**File:** `backoffice/` has no ESLint config file

The backoffice has a `lint` script but no config file — linting is effectively a no-op. All backoffice code goes unchecked.

---

### TOOL-03: Add Pre-Commit Hooks
**Effort:** S | **Agents:** EM

No Husky, lint-staged, or lefthook configuration exists. Nothing enforces linting or formatting before commits.

**Why it matters:** Bad code reaches `main` unchecked. Formatting diverges between developers. CI (when added) will reject commits that passed locally.

---

### TOOL-04: Add CI Pipeline (Lint + Typecheck + Build)
**Effort:** M | **Agents:** EM

No CI configuration files exist in `frontend/`. Build health is entirely manual with no automated gate on PRs.

---

### TOOL-05: Add `noUnusedLocals`/`noUnusedParameters` to Backoffice tsconfig
**Effort:** S | **Agents:** Architect

**File:** `backoffice/tsconfig.json` — missing these flags that `app/tsconfig.json` has

---

### SRE-04: Add HSTS Header
**Effort:** S | **Agents:** SRE, Security

**Files:** `app/nginx.conf`, `backoffice/nginx.conf`

Neither nginx config includes `Strict-Transport-Security`. Verify if Traefik adds it; if not, add:
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

---

### SEC-08: Demo Credentials in Production Bundle
**Effort:** S | **Agents:** Security, SRE

**File:** `app/src/pages/LoginPage.tsx:18-19`

`DEMO_EMAIL` and `DEMO_PASSWORD` are hardcoded constants compiled into the bundle. Even though they're only displayed when `?demo=true`, the password is visible in the JS source.

**Fix:** Move to runtime config via `env('VITE_DEMO_EMAIL')` / `env('VITE_DEMO_PASSWORD')`, set only in non-production environments.

---

### SEC-09: Open Redirect via `?redirect=` Parameter
**Effort:** S | **Agents:** Security

**File:** `app/src/pages/LoginPage.tsx:21-31`

The `sanitizeRedirect` function uses a blocklist approach (blocks `//`, `@`). While good, a positive allowlist would be stronger.

**Fix:** After authentication, verify the final redirect target:
```ts
const url = new URL(target, window.location.origin);
if (url.origin !== window.location.origin) return null;
```

---

## P2 — Medium Priority

Improvements to code quality, developer experience, performance, and resilience. Important for long-term health but not blocking.

---

### PERF-01: Replace `window.location.reload()` with `queryClient.clear()` on Team Switch
**Files:** `app/src/context/TeamContext.tsx:82`, `MyTeamsSection.tsx:150`, `PendingInvitationsPanel.tsx:91`

Full page reload destroys React state, pending queries, and cache. Loses any unsaved form data with no confirmation. Use `queryClient.resetQueries()` + `navigate('/dashboard')` instead.

### PERF-02: Client-Side Sort/Filter in PropertyDetailPage Should Use Server-Side
**File:** `app/src/pages/PropertyDetailPage.tsx:224-300+`

Contracts fetched for a property are filtered, sorted, and paginated in JavaScript via `useMemo`. The `useContracts` hook already supports server-side pagination.

### PERF-03: Add `retryDelay` with Exponential Backoff to React Query
**File:** `app/src/main.tsx:11`

`retry: 1` with no `retryDelay` — immediate retry on failure creates potential retry storms during backend restarts.
```ts
retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 30000)
```

### PERF-04: Source Map Warning Limit Too Permissive
**File:** `app/vite.config.ts:41`

`chunkSizeWarningLimit: 1300` suppresses legitimate warnings about oversized chunks.

### PERF-05: Add `loading="lazy"` to Photo/Document Images
No `<img loading="lazy">` anywhere. Photo pages can render dozens of images.

### DUP-04: Extract Shared `AuthContext` Factory
**Files:** `app/src/contexts/AuthContext.tsx` <-> `backoffice/src/contexts/AuthContext.tsx`

~115 lines of identical Keycloak init logic. Only differences: SSO check and default redirect path. Extract `createAuthContext(options)` to shared package.

### DUP-05: Extract Shared Analytics Base
**Files:** `app/src/utils/analytics.ts` <-> `backoffice/src/utils/analytics.ts`

Same PostHog init, same `trackEvent`/`resetAnalytics`. Differences: `app_name` and `identifyUser` signature. Shared `createAnalytics(appName)` factory would eliminate ~60 lines of duplication.

### QUAL-05: Remove `console.log` from Production Code
**Files:** `app/src/contexts/AuthContext.tsx:57,62,67,74,80`, `backoffice/src/contexts/AuthContext.tsx:50,55,60`, `PaymentHistorySection.tsx:143`

Auth lifecycle events logged to browser console in production. Leaks session state info and sets wrong precedent for LLM-generated code.

### QUAL-06: `errorMessages.ts` Leaks Backend Error Strings to Users
**File:** `app/src/utils/errorMessages.ts:72-82`

If `data.detail`, `data.message`, or `data.error` are present, they're returned verbatim. Backend ProblemDetail messages like "Unique constraint violation on (team_id, identifier)" would pass through to the UI.

### QUAL-07: ThemeContext Mutates State During Render
**File:** `app/src/context/ThemeContext.tsx:58-72`

Calls `setThemePreference` and `setPrevBackendTheme` synchronously inside the render body. Causes guaranteed double render. Use `useEffect` for backend preference sync.

### QUAL-08: `InlineContactForm` Defined Inside `ContractForm.tsx`
**File:** `app/src/components/contracts/ContractForm.tsx:71`

Component defined inside another component's module gets recreated as a new function reference on each render — React unmounts and remounts it on every parent render.

### QUAL-09: `BulkDataGrid` Module-Level Mutable State
**File:** `app/src/components/common/BulkDataGrid.tsx:28`

`let nextRowId = 1` is a module-level counter. In StrictMode or HMR this causes issues. Use `useRef`.

### QUAL-10: `BulkDataGrid.ColumnDef` Name Collision with `@buurman/ui`
**File:** `app/src/components/common/BulkDataGrid.tsx:14`

Exports `ColumnDef` — incompatible type with same name as `DataTable`'s `ColumnDef<TRow>`. Rename to `BulkColumnDef`.

### QUAL-11: `formatDistanceToNow` Direct Usage Bypasses Timezone
**Files:** 8 detail/list pages import `formatDistanceToNow` directly instead of using `useFormatDate`

`useFormatDate` exists with timezone-aware `formatAppRelativeDate` for exactly this purpose.

### QUAL-12: Replace Full-Page Spinners with Skeleton Layouts
The `Skeleton` component exists in `packages/ui` but is used in exactly 1 file. All 22 pages with loading states use a full-page `<LoadingSpinner>`.

### QUAL-13: Add `useDebounce` Hook (4 Manual Implementations)
**Files:** `PropertyListPage.tsx` (useState-based, buggy), `ContactListPage.tsx` (useRef, correct), `DuplicateContactWarning.tsx` (useRef, correct), `AuditLogPage.tsx`

`PropertyListPage` stores the timeout handle in `useState`, causing extra re-renders per keystroke.

### QUAL-14: `useTabState` Should Be in `packages/ui`
**File:** `app/src/hooks/useTabState.ts`

Well-designed URL search params-based tab state hook. Should be shared before backoffice re-implements it.

### QUAL-15: `useTeamDefaults` Chains 3 Waterfall Queries
**File:** `app/src/hooks/useTeamDefaults.ts`

`useTeam()` -> `useCurrentTeam()` -> `useTeamSettings(team?.identifier)` — 3 subscriptions per form component. `useCurrentTeam()` is redundant since `activeTeam.identifier` is already in `TeamContext`.

### SRE-05: Show Error When Keycloak Init Fails
**File:** `app/src/contexts/AuthContext.tsx:85-88`

Keycloak init failure silently leaves user on a broken login page. Set an `initError` state and render a dedicated "Authentication service unavailable" screen.

### SRE-06: Handle 429 Responses in Axios Interceptor
**File:** `app/src/api/client.ts`

Rate limit errors surface as generic toasts. Detect `429`, read `Retry-After` header, show specific guidance.

### SRE-07: Wire `setImpersonating` into ImpersonationContext on Page Reload
**File:** `app/src/utils/analytics.ts:74`

Stub comment says "wire into ImpersonationContext when implemented" — but it IS implemented. PostHog captures events during impersonation sessions, polluting analytics.

### SEC-10: Impersonation Token in URL Leaks to Browser History
**File:** `app/src/pages/ImpersonatePage.tsx:11`

Call `history.replaceState({}, '', '/impersonate')` after token exchange to remove from history.

### SEC-11: Invitation Token in localStorage (Should Be sessionStorage)
**File:** `app/src/pages/InvitationPage.tsx:81,87`

`localStorage` persists across tabs and sessions. `sessionStorage` is sufficient for surviving the login redirect.

### SEC-12: `build-info.json` CORS Wildcard
**File:** `app/nginx.conf:38-42`

Restrict to the backoffice origin instead of `*`.

### TOOL-06: Add Lint and Typecheck Scripts to `packages/ui`
`@buurman/ui` has no `lint` or `tsc --noEmit` script. `yarn lint` skips the shared package entirely.

---

## P3 — Nice to Have

Polish items and minor consistency improvements.

---

| ID | Description | File(s) |
|----|-------------|---------|
| QUAL-16 | Remove `@react-keycloak/web` unused dependency | `app/package.json` |
| QUAL-17 | Delete zombie pages (`ReportsPage.tsx`, `TeamSettingsPage.tsx`) | `app/src/pages/` |
| QUAL-18 | Standardize hook file suffix (all `use*Hooks.ts` or all `use*.ts`) | `app/src/hooks/` |
| QUAL-19 | Delete `LoadingSpinner.tsx` re-export shim | `app/src/components/LoadingSpinner.tsx` |
| QUAL-20 | Pin `posthog-js` to exact version | `app/package.json`, `backoffice/package.json` |
| QUAL-21 | Add `engines` field to root `package.json` | `frontend/package.json` |
| QUAL-22 | Align Prettier quote style across workspaces | `app/`, `backoffice/` |
| QUAL-23 | Add `displayName` to app components (already done in `packages/ui`) | `app/src/components/` |
| QUAL-24 | Use `crypto.randomUUID()` for toast IDs instead of `Math.random()` | `app/src/context/ToastContext.tsx:30` |
| QUAL-25 | Move `/payment-instructions` route to `/admin/payment-instructions` | `app/src/App.tsx` |
| PERF-06 | Reduce `FeatureFlagContext` stale time from 15s to 60s | `app/src/context/FeatureFlagContext.tsx:21` |
| SRE-08 | Fix `vite.svg` placeholder favicon | `app/index.html:4` |

---

## Zero Test Coverage (Separate Track)

**Agents:** Principal, EM

No `.test.ts`, `.test.tsx`, `.spec.ts`, or `.spec.tsx` files exist anywhere in the frontend. No Vitest config exists. No test dependencies in `package.json`.

This is the most critical long-term risk but is a separate workstream from the security/quality items above.

**Highest-impact first test targets:**
1. `getErrorMessage()` in `errorMessages.ts` — pure function, high usage, complex branching
2. `usePagination` hook — pure state machine, easy `renderHook` test
3. `useFormatDate` + `dateFormatting.ts` — timezone handling is subtle
4. `sanitizeRedirect()` in `LoginPage.tsx` — security-critical input validation
5. Auth flow integration test — Keycloak init, token refresh, 401 handling

---

## SubscriptionSection Stub (Remove Before Launch)

**File:** `app/src/components/settings/SubscriptionSection.tsx`

Contains `// TODO: API call to upgrade subscription` and fake payment method UI with `// TODO: Implement add payment method`. This is rendered in the Settings page. Either hide it behind a feature flag or remove it entirely before public launch.

---

## Quick Wins (< 1 hour each, high impact)

| # | Item | Effort |
|---|------|--------|
| 1 | Fix SEC-01: DOMPurify in ImpersonationBanner | 5 min |
| 2 | Fix SEC-02: DOMPurify in ExtensionTimeline | 5 min |
| 3 | Fix SEC-04: Remove `useNonce: false` | 5 min |
| 4 | Fix SEC-06: `env()` for Maps API key | 10 min |
| 5 | Fix SRE-02: Add Axios timeout | 10 min |
| 6 | Fix TOOL-01: Delete legacy `.eslintrc.json` | 5 min |
| 7 | Fix ARCH-03: Rename `contexts/` to `context/` | 15 min |
| 8 | Fix TOOL-05: Add strict flags to backoffice tsconfig | 5 min |
| 9 | Fix QUAL-05: Remove `console.log` from AuthContext | 10 min |
| 10 | Fix SEC-05: Hidden source maps | 5 min |

---

## Recommended Execution Order

### Phase 1: Security Hardening (Before Launch)
1. SEC-01, SEC-02 — Fix XSS (5 min each)
2. SEC-03 — Add CSP headers
3. SEC-04 — Re-enable OIDC nonce
4. SEC-05 — Hide source maps
5. SEC-06 — Runtime env for Maps key
6. SEC-07 — Sanitize ToolEmbedPage path
7. SRE-01 — Integrate Sentry
8. SRE-02 — Add Axios timeout
9. SRE-03 — Hide error details in production

### Phase 2: Foundation (First Sprint After Launch)
10. ARCH-01 — Refactor App.tsx routing
11. ARCH-02 — Query keys registry
12. ARCH-03 — Merge contexts/ -> context/
13. DUP-01 — RichTextEditor to @buurman/ui
14. DUP-02 — usePagination to @buurman/ui
15. TOOL-01-05 — Linting, CI, pre-commit hooks

### Phase 3: Code Quality (Ongoing)
16. ARCH-04 — API strategy decision
17. QUAL-02 — Decompose god-components
18. DUP-03 — Toast consolidation
19. QUAL-01 — Fix type casts
20. QUAL-13 — useDebounce hook
21. Remaining P2 items

### Phase 4: Testing (Parallel Track)
22. Set up Vitest + React Testing Library
23. Test highest-impact targets (error handling, pagination, date formatting, auth)
24. Add component tests for shared UI library

---

*Generated by a panel of 6 specialized AI reviewers analyzing ~94k lines of frontend code.*
