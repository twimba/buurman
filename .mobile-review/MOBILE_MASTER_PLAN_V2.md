# Mobile Master Plan v2 — Buurman

Constraint: desktop pixel parity is non-negotiable. Every change is `md:hidden` or refactored at-parity above `md`.
Baseline: 5 screenshot bugs fixed in `01852f7b` (hamburger overlap, KPI truncation, range-chip overflow, date overflow, equity clip). This plan covers what's still pending.

## §1 Executive Summary

All four experts agree: dashboard is the largest remaining gap, `min-h-screen` legacy is everywhere, container queries are absent, the `MobileDashboardSummary`/`PortfolioDashboard` double-render is the root composition bug, and BottomTabBar interaction with keyboard + form footers is broken. They disagree on (a) whether to hide PortfolioDashboard entirely on phone (data-density: yes; UX: refactor in place) and (b) hero KPI choice (Income vs Cash Flow).

## §2 P0 — Must do, <4 hrs each

- **P0-1 Dashboard composition: hide `PortfolioDashboard` < md.** `DashboardPage.tsx:206` wrap in `hidden md:block`. Kills the desktop analytics rail being crammed into 358px (eliminates duplicate KPIs vs `MobileDashboardSummary`). [reports 1, 3, 4]
- **P0-2 BottomTabBar pinned height.** `BottomTabBar.tsx:34-39` set explicit `height: calc(var(--bottomnav-h) + var(--safe-bottom))`; bump `Layout.tsx:107` reservation +12px. Prevents content clip when system font scales. [report 3]
- **P0-3 Define `--bottomnav-h` reliably.** Report 4 claims var is undefined (zero defs); report 3 says it's defined in `theme.css:213`. Verify; if missing, add `:root { --bottomnav-h: 60px }` or write via `ResizeObserver` on mount in BottomTabBar. [reports 3, 4 — see §6]
- **P0-4 Hide BottomTabBar when `--kbd-inset > 0` AND on form-footer routes.** `BottomTabBar.tsx`. Today form pages render two competing bottom strips. [report 2]
- **P0-5 Drop `maximum-scale=5` from viewport meta.** `index.html:12`. WCAG 1.4.4 regression; 16px input rule already prevents zoom. [report 2]
- **P0-6 Body-scroll-lock on mobile drawer.** `Sidebar.tsx` mobileOpen effect: `document.body.style.overflow = 'hidden'`. Today body scrolls behind open drawer. [report 2]
- **P0-7 Bottom-sheet close animation.** `Sheet.tsx` add `data-[state=closed]:animate-slide-down` mirror keyframe. Currently pops out. [report 2]
- **P0-8 iOS zoom on date inputs.** `PortfolioDashboard.tsx:246-268` bump `text-xs` → `text-base` on `<input type="date">`. Latent bug independent of overflow fix. [report 3]
- **P0-9 Reorder phone dashboard: alerts → hero → unpaid payments → rail.** `DashboardPage.tsx:180-528`. Today unpaid payments live at L397 below analytics; hoist above. [reports 1, 4]
- **P0-10 Add `overscroll-y-contain` to `<main>`.** `Layout.tsx:100`. Stops app rubber-band bleed on iOS. [report 2]
- **P0-11 Add `var(--kbd-inset, 0px)` to main pb.** `Layout.tsx:107`. Form content can't scroll past keyboard today. [report 2]

## §3 P1 — Important, 1-2 days each

- **P1-1 Codemod `min-h-screen` → `min-h-[100dvh]`.** ~35 pages incl. `LoadingSpinner.tsx:21`, `createAuthProvider.tsx:131`, `WorkInProgress.tsx:15`, `DocumentPreviewModal.tsx:106,140`. Eliminates address-bar jump. [report 2]
- **P1-2 Migrate `DocumentsPage.tsx:349-529` to card list <md.** Only major page still using `<table>` on phone. [report 1]
- **P1-3 Unify Dashboard KPI strip via container queries.** Kill `MobileDashboardSummary` vs desktop-grid fork (~150 LOC dup) in `DashboardPage.tsx:209-295`. Single `<DashboardKpiStrip>` with `@container`. [reports 3, 4]
- **P1-4 Fluid type tokens.** Add `--text-display: clamp(1.5rem, …, 2.25rem)` etc. to `theme.css`; wire `ListPageHeader` h1, hero KPIs. Removes `text-2xl md:text-3xl` jumps. [report 3]
- **P1-5 Standardize filter affordance.** `Filters (count) ⌄` chip pattern across Properties/Contacts/Payments/Expenses/Documents. Today: 5 different patterns. [report 1]
- **P1-6 Pull-to-refresh primitive** on list scroll containers. `@use-gesture/react` already in bundle. Wire to TanStack `refetch()`. [reports 1, 2]
- **P1-7 Service worker via `vite-plugin-pwa`.** App-shell precache + `/offline.html` + `NetworkFirst /api/*`. Unblocks credible install + future web push. [report 2]
- **P1-8 Drag-to-dismiss on Sheet handle.** `Sheet.tsx:108-113` wire `@use-gesture/react`. Handle currently decorative. [report 2]
- **P1-9 View-transition page swap** (react-router v7 `unstable_viewTransition`, CSS-only slide-X on phone). [reports 1, 2]
- **P1-10 Swap hero KPI: Income → Cash Flow** (sign-colored). `MobileDashboardSummary.tsx:99`. Income demotes to rail tile. [report 4]
- **P1-11 Surface Top/Bottom 5 performers on phone dashboard** (inaccessible after P0-1). Two-tab section in `MobileDashboardSummary`. [report 4]
- **P1-12 iPad breakpoints.** `PortfolioSummaryCards.tsx:163` `lg:grid-cols-3` → `md:grid-cols-3`; `PortfolioDashboard.tsx:336,362` `lg:grid-cols-2` → `md:grid-cols-2`. Reclaims rail-mode real estate. [reports 3, 4]
- **P1-13 Contacts filter row reflow.** Hide sort `<select>` behind filter sheet on phone. [report 1]
- **P1-14 Property card identifier.** `PropertyCard.tsx:69-71` `hidden md:block` or truncate to last 6 chars. [report 1]
- **P1-15 Robust `useKeyboardInset`.** Capture baseline `innerHeight` at mount, delta vs `visualViewport`. Today's math fragile. [report 2]
- **P1-16 Manifest hardening.** Add `"id": "/"`, `"shortcuts"`, true maskable icon w/ safe zone, dark `theme_color`/`background_color`. [report 2]
- **P1-17 iOS splash screens** via `pwa-asset-generator` (~10 `apple-touch-startup-image` links). [report 2]
- **P1-18 `scrollIntoView` on `focusin` inside Sheet body.** [report 2]
- **P1-19 Payments hero collapse.** Combine Pending+Overdue into 50/50 row; demote 6-mo chart to disclosure. [report 4]
- **P1-20 Expenses hero collapse.** Single Total card + 3-bullet Top Categories; drop sparkline on phone. [report 4]
- **P1-21 "Portfolio analytics →" CTA** on phone dashboard routing to /reports. [report 4]

## §4 P2 — Polish

- **P2-1 Container queries on `<Card>`.** `container-type: inline-size` + `@container/card`. Rewrite 2 most-reused composites. [report 3]
- **P2-2 Haptic API stub** `useHaptic()` → `navigator.vibrate`. [report 2]
- **P2-3 Sheet snap-points** (peek/full) for long forms. [report 2]
- **P2-4 `active:scale-[0.98]` + spring** on Buttons/BottomTabBar/cards. Big native-feel delta. [report 2]
- **P2-5 Long-press selection** on Property/Contact cards → existing `SelectionBar`. [report 1]
- **P2-6 BeforeInstallPrompt capture + in-app install CTA** (Android/desktop). [report 2]
- **P2-7 `appinstalled` PostHog event.** [report 2]
- **P2-8 Snap-proximity on KPI rail** (replace mandatory). [report 4]
- **P2-9 Fade-right mask on KPI rail** for scroll affordance. [reports 1, 4]
- **P2-10 `formatMoneyCompact` precision** in 10K–1M (one decimal). `formatMoney.ts:59-84`. [report 4]
- **P2-11 Long-press tooltip on hero KPI** showing full precision + timestamp. [report 4]
- **P2-12 Chart color hook** reading CSS vars; remove `#14161f`-style off-palette literals in `EquityCompositionChart.tsx:56-122`. [report 3]
- **P2-13 Cancel button in stepper → `←` icon** on iOS. [report 1]
- **P2-14 Documents title shorten** "Document Library" → "Documents" on phone. [report 1]
- **P2-15 Payments primary glyph** replace calendar-check with explicit `+ Record`. [report 1]
- **P2-16 Tab bar label tightening** `text-[10px] leading-tight`. [report 1]
- **P2-17 Filter card sheet on iPad <lg.** [report 1]
- **P2-18 Audit `flex-wrap` rows for missing `min-w-0`** (RentTimeline:327,349,354; Contacts filters). [report 3]
- **P2-19 Standardize on `.pb-safe`** (codemod `CurrencyDropdown.tsx:286`). [report 3]
- **P2-20 Decide on `xs:` breakpoint** (used 2 places — commit or remove). [report 3]
- **P2-21 Status-bar `pt-safe` on drawer** under iOS notch. [report 2]
- **P2-22 Left-edge swipe-back gesture** (skip unless native shell on table). [reports 1, 2]
- **P2-23 Stage Manager smoke test** at 600/700/800px. [report 2]

## §5 Cross-cutting themes (independently flagged by ≥2 experts)

- **Zero container queries** anywhere — every responsive decision viewport-bound. Reports 3 + 4.
- **`min-h-screen` legacy** across ~35 files — every page picks up iOS address-bar jump. Reports 2 + 3.
- **Discrete typography jumps** (`text-2xl md:text-3xl`) instead of fluid `clamp()`. Report 3, implicit in reports 1+4.
- **Dashboard composition is a half-finished migration** — mobile + desktop renderers both fire. Reports 1, 3, 4.
- **`MobileMenuButton` registration pattern is brittle** — any page that forgets `ListPageHeader` gets the floating-hamburger bug. Reports 1, 3, 4.
- **Mobile/desktop forking** in 26 files w/ 64 branching classes — DashboardPage worst. Reports 3, 4.
- **No PTR, no haptics, no SW** — three biggest "feels like a website" tells. Reports 1, 2.
- **Filter affordance fragmentation** across 5 list pages. Report 1, echoed in report 3 "breakpoint inconsistency".
- **Token drift** — raw `env()`, hex literals, mixed semantic+numeric tints. Report 3.

## §6 Conflicts to resolve before execution

1. **`--bottomnav-h` definition.** Report 3 says defined at `theme.css:213` as `3.75rem`; report 4 says var is undefined, claiming the bug is silent `calc(0 + 0px)`. **Resolution:** read `theme.css:213` first. If defined, the bug is system-font scaling (report 3's diagnosis); if absent, add it. Either way P0-2 + P0-3 land together.
2. **PortfolioDashboard on phone: hide vs refactor.** Report 4: hide entirely (`hidden md:block`), route analytics to /reports. Reports 1+3: fix overflow in place. **Resolution:** ship report 4's hide as P0-1 (cheapest, eliminates 3 bugs at once); add P1-21 CTA to /reports. Refactor is unnecessary if hidden.
3. **Hero KPI choice.** Report 4 strongly: Cash Flow not Income. Report 1: silent. **Resolution:** product call. Default to report 4 (Cash Flow), schedule for P1.
4. **PTR scope.** Report 1: P2. Report 2: P1. **Resolution:** P1 — it's the #1 missing native gesture and `@use-gesture/react` is already shipped.
5. **`maximum-scale=5` drop.** Report 2 says drop. Reports 1+3+4 silent. **Resolution:** drop (accessibility wins, no countervailing argument).
6. **Page-transition strategy.** Report 1 P2 (framer-motion); report 2 P1 (view-transitions API). **Resolution:** view-transitions (cheaper, falls back).

## §7 Execution order

**Wave 0 (today, ~3 hrs)** — P0-1, P0-3, P0-2, P0-4, P0-5, P0-6, P0-7, P0-8, P0-10, P0-11. Independent; all `md:`-gated or chrome-level.
- P0-1 unblocks P1-3 (single KPI strip), P1-10 (cash-flow hero), P1-11 (Top/Bottom 5), P1-21 (analytics CTA).
- P0-3 unblocks P0-2.
- P0-4 unblocks future form-page polish.

**Wave 1 (this week)** — P0-9 (dashboard reorder, needs Wave 0 cleared).

**Wave 2 (sprint)** — P1-1 (codemod min-h, mechanical, do early to clear iOS jump from every page touched downstream). Then P1-3 → P1-10 → P1-11 → P1-21 as a coherent dashboard composition pass. P1-4 (fluid type) lands alongside P1-3. P1-12 (iPad breakpoints) parallel.

**Wave 3 (sprint)** — P1-7 (SW) unblocks P1-16, P1-17, P2-6, P2-7, future web push. P1-6 (PTR) parallel. P1-8 (Sheet drag) parallel. P1-9 (view transitions) parallel.

**Wave 4 (next sprint)** — P1-2 (Documents migration), P1-5 (filter standardization, touches 5 pages — gate behind P1-3's pattern), P1-13/14/19/20.

**Wave 5 (polish)** — P2 list; P2-1 (container queries on Card) is the highest-leverage refactor and should pair with any new composite added in waves 2-4.

---

**Counts:** P0 = 11 · P1 = 21 · P2 = 23 · Total = 55

---

## Execution status (rolling)

P0: 11/11 shipped (see commits `01852f7b`, `51fd249c`).

P1: 17/21 shipped. Deferred and why:
- **P1-3 Unify Dashboard KPI strip via container queries** — desktop pixel-parity risk; the proposed single `<DashboardKpiStrip>` replaces a custom-designed 4-card grid (gradient backgrounds, hover states, "vs last month" badges) that doesn't map 1:1 to phone tiles. Needs design alignment before refactor.
- **P1-5 Standardize filter affordance** — partial: 4/6 list pages (Properties, Contracts, Payments, Expenses) already on `FilterSheet`. ContactListPage and DocumentsPage carry legacy patterns; full migration is moderate (~100 LOC each) and should pair with the FilterSheet API extension to host sort.
- **P1-7 Service worker** — requires installing `vite-plugin-pwa` + Workbox config + `/offline.html` route + asset precache list; defer until a clean session that can run `yarn add` and validate the manifest+SW cooperate.
- **P1-17 iOS splash screens** — requires running `pwa-asset-generator` against the master logo to emit ~10 PNGs at exact `apple-touch-startup-image` dimensions; defer alongside P1-7.

P1: 19/21 shipped (P1-3 + P1-5 added in second pass — see commits `4a75bdcf` for P2-3 and `25ca288a` for P1-5; P1-3 in the most recent commit). Still deferred:
- **P1-7 Service worker** — needs `yarn add vite-plugin-pwa` + Workbox config + offline.html.
- **P1-17 iOS splash screens** — needs `pwa-asset-generator` against the master logo.

P2: 22/23 shipped. Still deferred:
- **P2-23 Stage Manager smoke test** — purely manual (iPad with Stage Manager at 600/700/800px window widths). Cannot meaningfully automate without a real iPad in the CI matrix.

P2-18 (flex-wrap min-w-0 audit) confirmed closed: re-checked RentTimeline.tsx:327/349/354 + Contacts filter row; flagged rows already use flex-wrap or have shrink-0 leaders + min-w-0 content. No concrete bugs.
