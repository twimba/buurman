# Responsive Engineering Review v2 — Buurman

## Verdict

The foundation is solid — Tailwind v4 `@theme`, well-defined design tokens (`--safe-*`, `--header-h`, `--bottomnav-h`, `--size-touch`, `--kbd-inset`), an `xs` breakpoint, a rail-mode sidebar, a phone-only bottom tab bar, and a shared `ListPageHeader`. But the dashboard is the one page that never got the `ListPageHeader` treatment, and that single omission is the proximate cause of three of the five visible bugs. The rest of the codebase has consistent breakpoint hygiene with two systemic gaps: (1) **zero use of container queries** — every responsive decision is viewport-bound — and (2) **discrete `text-2xl md:text-3xl` jumps everywhere** instead of fluid `clamp()`. iPad rail-mode also under-uses horizontal space because grids still jump 1→2 cols only at `md` then 2→3/4 at `lg`. Effort to ship a tight P0 fix bundle: ~2 hours. P1 (container queries on cards, fluid type) is a half-day refactor.

---

## The 5 Visible Bugs — Diagnoses + Fixes

### 1. Floating hamburger overlaps the Monthly Income hero card

**Root cause.** `DashboardPage.tsx` does **not** render `<ListPageHeader>` and does **not** render `<MobileMenuButton>` either. Every list page does (`PropertyListPage.tsx:162`, `ContactListPage.tsx:402`, `PaymentsPage`, `ExpensesPage`, etc.). With no `MobileMenuButton` mounted, `hasOwnMenuButton` stays `false`, so `Sidebar.tsx:297-317` renders the floating hamburger at `fixed left-4 top: calc(env-banner + 1rem)`. The Monthly Income hero `<section>` (`MobileDashboardSummary.tsx:86-105`) starts at the top of `<main>` with only `-mt-2` and `p-5` padding, so the icon button (~44×44 at left:16) lands on top of the white "MONTHLY INCOME" label.

**Fix.** Give the dashboard a `ListPageHeader` with `mobileLeading={<MobileMenuButton />}` (matches every other top-level page). At minimum add:

```tsx
// DashboardPage.tsx ~ line 181, before MobileDashboardSummary
<ListPageHeader
  title={t('sidebar.dashboard')}
  icon={LayoutDashboard}
  mobileLeading={<MobileMenuButton />}
/>
```

This (a) registers `hasOwnMenuButton`, killing the floating button; (b) moves the menu trigger inline at the top of the document flow so it can never overlap content; (c) gives the page a consistent title row with the rest of the app. (`frontend/app/src/components/DashboardPage.tsx:180-188`)

### 2. KPI rail tiles truncate labels ("TOTAL PROPER…")

**Root cause.** `RailTile` is at `MobileDashboardSummary.tsx:154-170`: `className="snap-start flex-shrink-0 w-[60%] xs:w-[48%]"` with the label inside a `<span className="truncate">` and the icon eating 16 px + 8 px gap. At 375 px viewport: container width = 375 − 32 (page px) = 343; 60% = ~206 px; minus 32 px (`p-4`), 24 px (icon + gap) → label gets ~150 px, which truncates "TOTAL PROPERTIES" to "TOTAL PROPER…".

**Fix.** Three options, in priority:

- (a) Drop the `uppercase tracking-wider` on rail labels — both eat horizontal space and reduce density. Switch to `text-[11px] font-medium` non-uppercase. Frees ~20 % width.
- (b) Bump the rail tile width to `w-[64%] xs:w-[52%]` — gives ~16 px more, enough for "TOTAL PROPERTIES" without truncation.
- (c) Better: drop the icon at this size (it's redundant with the label) or move it next to the value, not the label. `MobileDashboardSummary.tsx:159-162`.

The combination of (a)+(b) clears the truncation without redesign.

### 3. Portfolio Overview range chips overflow the card right edge

**Root cause.** `PortfolioDashboard.tsx:230-245`. The chips live in `<div className="flex gap-1">` inside an outer `<div className="flex items-center gap-3 flex-wrap">`. `flex gap-1` has **no `flex-wrap`**, so the 8 chips (6M, 1Y, 2Y, 3Y, 4Y, 5Y, 10Y, All) form one rigid row. Each chip is `px-3 py-1.5 text-xs` ≈ 36 px; 8 × 36 + 7 × 4 gaps = ~316 px. Card content width on iPhone is ~311 px, so the row clips the right edge.

**Fix.** Add `flex-wrap` to the chip group, or convert to a horizontal-scroll snap rail (consistent with `MobileDashboardSummary`'s KPI rail):

```tsx
// PortfolioDashboard.tsx:231
<div className="flex gap-1 flex-wrap">
```

Better long-term: this is a segmented control with 8 options on a 360-px screen. Either (a) drop "2Y/3Y/4Y/5Y" on `<md` and surface them in a `<select>`, or (b) use `overflow-x-auto snap-x` with `-mx-4 px-4` to bleed to the card edge.

### 4. Date range inputs overflow the card right edge

**Root cause.** Same row (`PortfolioDashboard.tsx:246-268`). Two `<input type="date">` with `px-3 py-1.5 text-xs` are intrinsically ~120-160 px wide each in Safari iOS (the native UA width includes the calendar pictogram). 2 × ~140 + en-dash + gaps ≈ 290 px. Even on its own row this barely fits, but it's still on the same `flex-wrap` parent at `gap-3` with no `min-w-0` so it just overflows.

**Fix.** Wrap the two inputs in their own flex container with `w-full` on `<md` and let them share width 50/50:

```tsx
<div className="flex gap-2 items-center w-full sm:w-auto">
  <input type="date" className="flex-1 min-w-0 sm:flex-none ..." />
  <span>–</span>
  <input type="date" className="flex-1 min-w-0 sm:flex-none ..." />
</div>
```

Also remove `text-xs` on the inputs themselves — iOS auto-zooms on `<input>` with `font-size < 16px`. This is a separate latent bug.

### 5. Total Equity card value clipped by bottom tab bar

**Root cause.** `Layout.tsx:107`:

```tsx
pb-[calc(var(--bottomnav-h)+var(--safe-bottom,0px))] md:pb-0
```

That reserves `60px + safe-bottom`. But `BottomTabBar.tsx` has **no fixed height** — it's content-driven: `flex flex-col items-center` icon (20 px) + gap (2 px) + 11 px text + `py-2` (16 px) + `min-h-touch` (44 px floor) + `paddingBottom: safe-bottom`. The actual rendered height is **`max(44 + 16, 20+2+11+16) = ~60 px PLUS safe-bottom`**, which Layout already adds. But there's a second issue: the `BottomTabBar` has a top border (`border-t border-border-default`) and the `--bottomnav-h: 3.75rem` (60 px) variable is exact, so on devices with bigger system fonts (`text-size-adjust` allows up to 1 rem at user scale 200 %) the bar grows to ~70 px and content under it is clipped. The screenshot shows this happening at default zoom — the bar reads ~68 px tall on iPhone 14.

**Fix.** Either (a) measure the bar at mount with a `ResizeObserver` and write `--bottomnav-h` to `:root`, or (b) hard-pin the bar to 60 px with `style={{ height: 'calc(var(--bottomnav-h) + var(--safe-bottom, 0px))' }}` and `flex-shrink-0` inner content. Option (b) is the simpler fix:

```tsx
// BottomTabBar.tsx:34-39
className="md:hidden fixed inset-x-0 bottom-0 z-40 ..."
style={{
  height: 'calc(var(--bottomnav-h) + var(--safe-bottom, 0px))',
  paddingBottom: 'var(--safe-bottom, 0px)',
}}
```

Additionally bump Layout's reservation to add a safety pad:

```tsx
pb-[calc(var(--bottomnav-h)+var(--safe-bottom,0px)+0.75rem)]
```

The extra 12 px clears any rounding error and breathes between the last card and the bar.

---

## Findings by Area

### 1. Container queries

**Current state: zero usage.** A `grep` for `@container | container-type` across `frontend/app/src` and `frontend/packages/ui/src` returns only Swagger-UI vendor CSS. Every responsive break in the app is `md:`/`lg:` viewport-based.

**Where this hurts.**

- `PortfolioDashboard.tsx:336` — `grid grid-cols-1 lg:grid-cols-2 gap-6` for the cash-flow + allocation row. At rail iPad (`md`-`lg`, ~768-1023), the dashboard main column is ~700 px because the rail eats 64 px and gutters eat ~48 px. That's plenty of width for 2 columns, but the grid stays 1-col until full `lg` (1024) — the row's *parent* width should drive the break, not the viewport.
- `PortfolioSummaryCards.tsx:163` — `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3` for 6 KPI cards. The desktop screenshot shows it correctly at 3-up, but the same component dropped into a 600 px property-detail right rail would still go 3-up despite cramping.
- `MobileDashboardSummary` KPI rail tiles use `w-[60%] xs:w-[48%]` — a hand-rolled container query in viewport disguise.

**Recommendation.** Add `container-type: inline-size` to `<Card>` in `packages/ui` and convert the most-reused dashboard composites (`PortfolioSummaryCards`, `MobileDashboardSummary` rail, the property-detail tab grid) to `@container (min-width: 32rem)` rules. Tailwind v4 has first-class container query support via `@container/name:` — wire `name: card` on `<Card>` and rules become `@card-lg:grid-cols-3`. This single change makes the cards usable inside any drawer, modal, or sidebar without bespoke breakpoint plumbing.

### 2. Fluid typography

Every large display number is `text-2xl md:text-3xl` — `DashboardPage.tsx:220, 238, 260, 278`, `ListPageHeader.tsx:91` (title), `MobileDashboardSummary.tsx:99` (`text-4xl` static), `:163` (`text-2xl` static). These are discrete 24 px ↔ 30 px ↔ 36 px jumps. At a 600-700 px tablet width the title is still 24 px because `md` (768) hasn't kicked in — the type feels small relative to the larger canvas.

**Recommendation.** Replace the title and hero KPI sizes with `clamp()`:

```css
/* in theme.css */
--text-display: clamp(1.5rem, 1.1rem + 1.6vw, 2.25rem);    /* 24 → 36px */
--text-hero: clamp(2rem, 1.4rem + 2.4vw, 3rem);            /* 32 → 48px */
```

Then `ListPageHeader` h1 → `style={{ fontSize: 'var(--text-display)' }}` (or expose as a Tailwind utility). Eliminates the awkward "small title on big iPad" feel and removes one breakpoint axis from JSX. Same treatment for chart axis ticks and the `text-4xl` hero KPI.

### 3. Bottom-padding / safe-area sweep

Only **one** place in the app reserves space for the bottom tab bar: `Layout.tsx:107`. That works for the standard `<Outlet />` content, but:

- `MobileFormStepper.tsx:188` correctly composes `var(--kbd-inset) + var(--safe-bottom) + var(--bottomnav-h)` for its sticky footer.
- `SelectionBar.tsx:59` correctly anchors at `calc(var(--bottomnav-h) + var(--safe-bottom))`.
- `CurrencyDropdown.tsx:286` uses `pb-[env(safe-area-inset-bottom)]` — inconsistent with the rest of the codebase (raw `env()` not the `--safe-bottom` indirection). Token drift.

The biggest gap is that **no page-level scrollable content (charts, long forms, drawers)** reserves extra padding on its own, relying entirely on Layout's `pb-`. That's fine for the outlet but it's fragile: any component that escapes `<main>`'s scroll container (modal, sheet) silently loses the reservation. Bug 5 is a symptom of this.

### 4. Breakpoint consistency

Generally good. The conventions I observed:

- List pages use `grid gap-6 md:grid-cols-2 lg:grid-cols-3` (Properties, Contacts).
- KPI strips use `grid-cols-1 md:grid-cols-2 lg:grid-cols-4`.
- Dashboard charts use `grid-cols-1 lg:grid-cols-2`.
- Headers use `flex-col sm:flex-row`.

**Inconsistencies found:**

- `PortfolioSummaryCards` (`lg:grid-cols-3`) vs `DashboardPage` KPI rail (`lg:grid-cols-4`) — same conceptual block, different col counts.
- `PaymentsPage.tsx:155` uses `lg:grid-cols-3` for 3 stat cards, fine, but `lg:grid-cols-3` again at `:250` for filters — that's a different layout type using the same break. Either re-justify or document.
- `ContactListPage.tsx:562` uses `sm:grid-cols-2 lg:grid-cols-4` for inline filter chips while the property page uses `flex flex-wrap gap-2` for the same UX (`PropertyListPage.tsx`).
- `xs:` is used in only **two** places (`MobileDashboardSummary.tsx:157`, `ListPageHeader.tsx:89`). Either commit to it or remove. The `--breakpoint-xs: 22.5rem` is defined.

### 5. iPad real-estate (rail mode)

At `md`→`lg` (768-1023) the sidebar is a 64-px rail. Main content width = viewport − 64 − 48 (px-6 ×2) ≈ 656-911 px. Yet:

- Dashboard KPI cards stay at `md:grid-cols-2` (DashboardPage:209), so only 2 cards across the full width. Desktop shows them comfortably at 3-up (PortfolioSummaryCards shows 3-up at lg).
- `PortfolioDashboard.tsx:336/362` chart pairs stay 1-col until lg.

**Recommendation.** Push the chart pair grids to `md:grid-cols-2` not `lg:grid-cols-2`. KPI rail to `md:grid-cols-3 lg:grid-cols-4`. The ipad-01 screenshot proves there's plenty of room — the rail-mode dashboard currently feels too breezy.

### 6. Grid layouts

- `PortfolioSummaryCards` renders **6 cards** in `lg:grid-cols-3`. 6 ÷ 3 = 2 rows, no orphan. Good.
- `DashboardPage` KPI rail renders **4 cards** in `lg:grid-cols-4`. 4 ÷ 4 = 1 row, no orphan. But at `md:grid-cols-2` the bottom row is also full — good.
- `PropertyListPage.tsx:107` `md:grid-cols-2 lg:grid-cols-3` for a list whose count is data-dependent — likely orphan when count = 4 (1 lone card on row 2 at lg). Acceptable trade-off.
- The `legend rows` at `DashboardPage.tsx:365` use `grid-cols-2 sm:grid-cols-3 lg:grid-cols-5` for **5** statuses. At sm (3 cols) the last 2 wrap leaving a 2-wide row of orphans below 3. Mild.

### 7. Brittle flex-wrap / hard-coded widths

`flex-wrap` is used in 15+ places; most are benign (badge groups). Concerns:

- `PortfolioDashboard.tsx:219, 230` — these are exactly the bug-3/bug-4 site. The outer `flex-wrap` is correct but the **inner** chip group at `:231` is `flex gap-1` with no wrap.
- `RentTimeline.tsx:327, 349, 354` — multiple nested `flex-wrap` siblings without `min-w-0` on inner text columns; likely to cascade into overflow when a tenant name is long.
- Hard-coded percentages: `MobileDashboardSummary.tsx:157` `w-[60%] xs:w-[48%]`. These should be `min-w-[15rem] max-w-[18rem]` (rem-based, intrinsic) so they don't break when the parent isn't full-bleed.
- Hard-coded pixel min-heights: `PortfolioDashboard.tsx:176` `min-h-[200px]`. Should be `min-h-[12rem]` to scale with user font preferences.

### 8. Mobile/desktop duplication

`grep` count: **26 files** use `md:hidden` or `hidden md:*`, and there are **64 occurrences** of mobile/desktop branching classes. The pattern: render two visual variants and toggle by viewport.

The worst offender is `DashboardPage.tsx`: it renders `MobileDashboardSummary` (md:hidden) AND a parallel 4-card grid (hidden md:grid). Both fetch the same `stats` data and render the same KPIs — pure duplication. ~150 LOC of forked markup.

**Recommendation.** Refactor `MobileDashboardSummary` and the desktop KPI grid into a single `<DashboardKpiStrip>` driven by container queries: hero KPI when narrow, equal-width grid when wide. ~50 % LOC reduction and easier to keep visually in sync.

Other duplications are cheaper to keep (the Sidebar's mobile-drawer vs desktop-rail vs collapsed-rail mode is irreducibly different IA), but the dashboard one is a clear win.

### 9. Token drift

182 raw color literals across `frontend/app/src/components/**`. Most live in chart files (`EquityCompositionChart.tsx`, `PortfolioCashFlowChart.tsx`, etc.) where Recharts requires string colors — partially defensible. But:

- `EquityCompositionChart.tsx:56-122` uses `#14161f / #2a2e3f / #eef0f6 / #8b90a8 / #6b7194` — these are **not in `theme.css`** at all. Off-palette greys that drift from the warm-stone neutral scale.
- `PortfolioCashFlowChart.tsx:19-20` uses `#059669 / #dc2626` — these DO match `--color-success / --color-error` tokens, but hardcoded. Should be `getComputedStyle(document.documentElement).getPropertyValue('--color-success')`.
- `CurrencyDropdown.tsx:286` `pb-[env(safe-area-inset-bottom)]` instead of `.pb-safe` (theme.css:230).
- Mixed semantic-vs-numeric color use: `DashboardPage.tsx:330` uses `bg-amber-500` (raw Tailwind) right next to `bg-success` (semantic token). Pick one.

Recommendation: add a `chartColors` hook that reads CSS variables at mount and returns a typed palette. Eliminates the dark-mode mismatch where charts don't theme-swap.

---

## Priority Punch List

### P0 — ship today (~2 hrs total)

1. **Dashboard hamburger overlap.** Add `<ListPageHeader title icon mobileLeading={<MobileMenuButton />} />` to `DashboardPage.tsx` (above `MobileDashboardSummary`). Fixes bug 1.
2. **PortfolioDashboard chips overflow.** Add `flex-wrap` to the chip row at `PortfolioDashboard.tsx:231`. Fixes bug 3.
3. **Date inputs overflow + iOS zoom.** Wrap dates in their own row at `<sm`, bump `text-xs` → `text-base` on `<input type="date">` to stop iOS auto-zoom. `PortfolioDashboard.tsx:246-268`. Fixes bug 4.
4. **KPI rail truncation.** Drop `uppercase tracking-wider` on rail tile labels; bump tile to `w-[64%] xs:w-[52%]`. `MobileDashboardSummary.tsx:157, 159-162`. Fixes bug 2.
5. **Bottom tab bar clipping.** Pin tab bar height to `calc(var(--bottomnav-h) + var(--safe-bottom))` in `BottomTabBar.tsx:34-39`. Bump Layout reservation by 12 px. Fixes bug 5.

### P1 — next sprint

6. Single dashboard KPI component with container queries (kill `MobileDashboardSummary` vs desktop grid fork in `DashboardPage.tsx:209-295`).
7. Fluid typography tokens (`--text-display`, `--text-hero`) in `theme.css`; wire into `ListPageHeader` h1 and hero KPIs.
8. Push dashboard chart pairs to `md:grid-cols-2` (currently `lg:`). Reclaims iPad rail-mode real estate.
9. Chart color hook reading from CSS variables; remove `#14161f`-style off-palette literals in dashboard charts.

### P2 — opportunistic

10. Add `container-type: inline-size` to `<Card>` and rewrite the 2 most-reused composites to `@container` rules.
11. Audit all `flex` rows that hold buttons + inputs (RentTimeline, ContactListPage filters) for missing `min-w-0` on text children.
12. Standardise on `.pb-safe` over raw `env(safe-area-inset-bottom)` — codemod the one drift site in `CurrencyDropdown.tsx:286`.
13. Decide whether `xs:` is part of the system or noise (used in 2 places only). If keep, document; if drop, codemod.
