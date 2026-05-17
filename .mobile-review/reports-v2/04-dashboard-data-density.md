# Dashboard / Data-density Review v2 — Buurman

## Verdict + Above-the-Fold Composition Proposal

Verdict: the iPhone dashboard today is a half-finished migration. `MobileDashboardSummary` is doing the right thing in isolation, but `DashboardPage` then unconditionally renders the full desktop `PortfolioDashboard` underneath it. That double-render is the source of every single visible bug on `iphone-01-dashboard.png` except #1 (which is a separate header/hamburger issue) and #6 (a missing CSS variable). The user sees: (a) the phone hero, (b) two narrow KPI tiles, (c) the desktop "Portfolio Overview" header card with a desktop-only filter rail crammed into 358px, (d) the desktop `PortfolioSummaryCards` 6-card grid stacked vertically because of `grid-cols-1 sm:grid-cols-2`. The KPI rail and the Portfolio Summary cards are literally showing the same kind of information twice — once as the new mobile rail, once as the old desktop card grid wedged below.

Proposed above-the-fold (≤ 700px tall, no scroll) on phone, in order:

1. **Sticky page header**: 56 px `MobileMenuButton` + "Dashboard" title + ⋯ action menu. Solves bug #1 by giving the hamburger its own row instead of letting Sidebar's floating fallback `fixed left-4` overlay the hero.
2. **Alerts strip** (only if non-zero): "9 overdue · 3 extensions →". One tap = jump to /payments?status=OVERDUE. Already implemented, just needs to come *before* the hero.
3. **Hero KPI — Monthly Cash Flow** (not Income — see §2 below). 4xl tabular numeral, primary gradient, ~108 px tall. Replace today's `€30,541` (income) with `–€25,828` (cash flow), with cash flow colored success/error based on sign.
4. **KPI rail** — three tiles minimum, snap-x, each ≥ 11 rem wide. Three peeking + 50% of the fourth telegraphs swipe affordance.
5. **Day-of section** (below the fold): unpaid payments shortlist (already exists further down DashboardPage) — but it needs to surface ABOVE PortfolioDashboard on phone. Today it's last.

PortfolioDashboard (the desktop analytical view with 5 charts + perf table + a date range picker) does not belong on phone /dashboard. It should be moved entirely behind a "Portfolio analytics →" tap-through OR mounted only on /reports.

---

## The 5 Visible Bugs — Diagnoses + Fixes

### Bug #1 — "MONTHLY INCOME" label covered by the hamburger
**Where**: `frontend/app/src/components/Sidebar.tsx:297-316` renders a floating hamburger at `fixed left-4 z-50` whenever `!hasOwnMenuButton`. `DashboardPage` never mounts a `MobileMenuButton`, so the floating fallback fires. The hero card in `MobileDashboardSummary.tsx:86-105` has `p-5` (20 px) padding, so the floating 44×44 button drops directly onto the label "MONTHLY INCOME".

**Fix**: Add a phone-only sticky header to `DashboardPage.tsx` similar to how `PropertyListPage`/`ContactListPage` use `ListPageHeader` + `MobileMenuButton`. Insert before line 181 in `DashboardPage.tsx`:
```tsx
<div className="md:hidden flex items-center gap-3 -mt-6 -mx-4 px-4 py-3 bg-surface-base sticky top-0 z-10 border-b border-border-subtle">
  <MobileMenuButton />
  <h1 className="text-lg font-semibold">{t('dashboard.title')}</h1>
</div>
```
This sets `hasOwnMenuButton = true` in `MobileNavContext`, which suppresses the floating fallback (`Sidebar.tsx:297`).

### Bug #2 — KPI rail tiles truncate labels ("TOTAL PROPER…", "OCCUPIED")
**Where**: `MobileDashboardSummary.tsx:157` — `w-[60%] xs:w-[48%]`. At 390 px viewport minus `px-4` gutters = 358 px usable. 60% of that ≈ 215 px. After `p-4` (16 px each side) and the 16 px icon + 8 px gap, the label gets ~163 px. "TOTAL PROPERTIES" in `text-xs uppercase tracking-wider` is ~170-180 px wide. Hence "TOTAL PROPER…".

**Fix**: replace fluid percentage with content-based min-width:
```tsx
className="snap-start flex-shrink-0 min-w-[11rem] max-w-[14rem] bg-surface-card …"
```
11 rem = 176 px which comfortably holds "TOTAL PROPERTIES". This also keeps three tiles visible at iPhone 13 width (16 + 176×3 + 12×2 gaps = ~552 px — first 358 px shows tile 1 + 75% of tile 2, signaling scrollability). The current 60% rule fits exactly two and a half tiles which looks like a layout bug, not a horizontal scroller. Also drop `snap-mandatory` → `snap-proximity` so users can free-scroll without being yanked into snap points.

### Bug #3 — Period-chip row (6M/1Y/…/All) overflows the right edge
**Where**: `PortfolioDashboard.tsx:230-245`. The container is `flex items-center gap-3 flex-wrap`, but the inner chip group at line 231 is `flex gap-1` with NO wrap, plus 8 chips × ~44 px = ~352 px before the date inputs even render. At 358 px usable it juuust overshoots, hence "10Y" gets clipped and "All" disappears off-screen.

**Fix**: this whole block has no place on phone /dashboard (see §3). If it must stay, wrap the chip group in `flex-wrap` AND collapse to a single `<select>` below `md`:
```tsx
<div className="hidden md:flex gap-1">{/* chips */}</div>
<select className="md:hidden …">{/* same options */}</select>
```

### Bug #4 — Date inputs overflow the card's right edge
**Where**: `PortfolioDashboard.tsx:246-268`. Native `<input type="date">` on iOS renders at a fixed ~140-160 px width regardless of CSS. Two of them + `–` separator + 16 px gaps = ~330 px. Combined with the period chips above, the parent `flex-wrap` does wrap, but the date row itself doesn't shrink — it just overflows because the dates can't be smaller than their intrinsic content width.

**Fix**: same as #3 — hide on phone. If kept, swap the side-by-side range to a single tap-to-open sheet/bottom-sheet date picker.

### Bug #5 — "Portfolio Value / Total Equity" full-width cards are duplicates
**Where**: `PortfolioDashboard.tsx:330-333` renders `<PortfolioSummaryCards>` unconditionally. That component (`PortfolioSummaryCards.tsx:163`) uses `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`, so on phone it stacks all 6 cards full-width: Portfolio Value, Total Equity, Monthly Cash Flow, Wtd Cap Rate, Wtd Cash-on-Cash, Occupancy.

These ARE duplicates of the rail tiles (Properties/Occupied/Vacant/Maintenance show the same domain) and of the hero (Monthly Income vs. Monthly Cash Flow). The screen reads like: *here's a hero, here's a rail, oh here are six more cards saying mostly the same thing.*

**Fix**: hide `PortfolioSummaryCards` on phone:
```tsx
// PortfolioDashboard.tsx around line 330
<div className="hidden md:block">
  <PortfolioSummaryCards summary={dashboard.summary} currency={dashboard.currency} />
</div>
```
Or — cleaner — hide *all of* `PortfolioDashboard` on phone in `DashboardPage.tsx:206`:
```tsx
<div className="hidden md:block">
  <PortfolioDashboard />
</div>
```
…and add a "View portfolio analytics →" link in `MobileDashboardSummary` that routes to /reports (or a dedicated /dashboard/analytics route).

### Bug #6 — Total Equity value clipped by bottom tab bar
**Where**: `Layout.tsx:107` uses `pb-[calc(var(--bottomnav-h)+var(--safe-bottom,0px))]`. Grep the repo for `--bottomnav-h` and there are **zero** definitions — only two consumers (`Layout.tsx`, `MobileFormStepper.tsx:188`). With the variable undefined, `calc(var(--bottomnav-h) + 0px)` is invalid CSS and resolves to the property's initial value (0). So Layout reserves zero space, and `BottomTabBar.tsx:37` (`fixed inset-x-0 bottom-0`) covers ~64 px of content.

**Fix**: define `--bottomnav-h` on the BottomTabBar element itself OR globally:
```tsx
// BottomTabBar.tsx — set the inline style so the variable is observable on :root
useLayoutEffect(() => {
  const el = ref.current;
  if (!el) return;
  document.documentElement.style.setProperty('--bottomnav-h', `${el.offsetHeight}px`);
  return () => document.documentElement.style.removeProperty('--bottomnav-h');
}, []);
```
Cheapest fix: hardcode in CSS (`:root { --bottomnav-h: 64px; }` inside a `@media (max-width: 767px)` block) since the bar height is stable.

---

## Findings by Area

### 1. KPI rail design
Three issues: (a) tile width is percentage-of-viewport instead of content-driven, (b) `snap-mandatory` is too aggressive for free-form lists, (c) only 3 tiles visible in screenshot but the code renders 4 — so the 4th ("Maintenance") is fully hidden off-screen with no scroll hint.

Rules I'd adopt:
- **Tile width**: `min-w-[11rem] max-w-[14rem]` content-driven. Labels in uppercase `text-xs tracking-wider` need ~170 px to never truncate.
- **Snap**: `snap-x snap-proximity` not `snap-mandatory`. Mandatory snap interferes with thumb-flick momentum on iOS.
- **Peek**: ensure tile-N+1 is 30-50% visible at the right edge. With 11 rem tiles, three full + ~25% of fourth at 390 px viewport — perfect affordance.
- **Scroll hint**: add a fade-right mask (`mask-image: linear-gradient(to right, black 90%, transparent)`) so the cut-off tile reads as "more to the right".
- **Rail order priority**: Occupied % > Vacant count > Total Properties > Maintenance. Vacancies are revenue at risk, so they outrank a static property count.

### 2. Hero KPI choice
The hero today is **Monthly Income** = `€30,541`. That's "expected revenue assuming 100% collection". The user's actual daily question is "did money show up?" — i.e. **Monthly Cash Flow** = income − expenses − debt service. On desktop, that number is `–€25,828` (negative). Showing income hides a bleeding portfolio.

Recommendation: hero shows **Monthly Cash Flow** with the sign-colored value (success/error) — the same logic already exists in `PortfolioSummaryCards.tsx:70-75` (`cashFlowColor`). Subtext stays "Income − expenses − debt service this month". Monthly Income demotes to a rail tile.

Alternatively (more product-y): make the hero a **two-line hero**: line 1 "Expected income €30,541", line 2 "Cash flow –€25,828" in smaller text. Hero is a single number though — pick one. Pick cash flow.

### 3. Portfolio Overview block on phone
This block (`PortfolioDashboard.tsx:217-327`) is a desktop analytics filter: 8 period chips + 2 date inputs + 3 export buttons. There is no plausible thumb-driven use case for date-ranging a portfolio while waiting for coffee. It's overflow city on phone and adds ~140 px of vertical noise before any data appears.

**Decision**: hide the entire `PortfolioDashboard` on phone /dashboard. Move analytics to /reports (which already exists per `desktop-01`'s sidebar). On phone /dashboard, replace it with a single CTA card: "Portfolio analytics — Cash flow, allocation, performance →" routing to /reports. Reports route can render `PortfolioDashboard` full-width with the period selector as a sheet.

### 4. Charts on phone — keep / move / kill
Charts already drop axis/grid/legend on phone via `useIsMobile` gating. That's good engineering but the right product call is: **kill all five charts on /dashboard for phone**. Sparkline-sized line/bar at 358×140 px tells a vibe, not a story. If users want analytics, they tap into /reports.

Exception: a single hero **sparkline** under the cash-flow hero (last 6 months, no axis, no legend, 56 px tall) — that's atmospheric, not analytical. Already idiomatic on Robinhood, Sequence, Apple Wallet.

### 5. Top performers / underperformers
`PropertyPerformanceTable.tsx:150-174` does render ranked card lists on phone — good. But they are buried below the cash-flow chart and the equity composition chart inside `PortfolioDashboard`. On phone, after we remove PortfolioDashboard, they need to surface somewhere — they're the only piece of `PortfolioDashboard` that's actually useful at-a-glance.

Move the Top 5 / Bottom 5 ranked cards directly into `MobileDashboardSummary` (or the phone /dashboard), as a two-tab section ("Top performers / Needs attention") under the alerts band. Two collapsed cards by default, expand-to-5 link. They're the answer to "which property is killing me this month?" which is the actual hero question.

### 6. iPad composition
Looking at `ipad-01-dashboard.png` at 768×1024: the 64 px rail is rendered, the content uses the full ~704 px, but the layout is a **stretched phone**:
- PortfolioOverview header card spans full width — same overflow as phone, just less obvious because there's more room.
- `PortfolioSummaryCards`: `sm:grid-cols-2 lg:grid-cols-3` → at 768 (sm but not lg) it's 2 columns. Each card occupies ~340 px and looks lonely.
- Cash flow chart sized correctly because it's a single chart in `grid-cols-1 lg:grid-cols-2` (so single-column under lg).

iPad is the worst victim of the breakpoint choice. The dashboard wants `md:grid-cols-3` for KPI cards so iPad sees a balanced 3-up. And `md:grid-cols-2` for chart pairs so iPad sees side-by-side too (cash flow ranges naturally to 350 px). Currently `lg:` is gating the multi-column layouts (1024+), so iPad portrait gets the phone-grade single-column-with-two-fat-cards layout.

Concrete iPad fixes:
- `PortfolioSummaryCards.tsx:163`: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3` → `grid-cols-1 sm:grid-cols-2 md:grid-cols-3`.
- `PortfolioDashboard.tsx:336,362`: `grid-cols-1 lg:grid-cols-2` → `grid-cols-1 md:grid-cols-2`.
- `DashboardPage.tsx:209`: `md:grid-cols-2 lg:grid-cols-4` → keep as-is; works.

### 7. Payments + Expenses pages
`iphone-04-payments.png`: Pending €91,624 card, then a same-size Overdue €11,480 card, then Last-6-Months mini-chart, then Filters, then list. Three full-width KPI-ish cards before any data row = ~520 px of "summary" before the user can see a single transaction. **Two of these three cards are above-the-fold blockers.**

Recommendation: combine Pending + Overdue into one row (split 50/50), tile-style — Overdue keeps the red treatment. The chart is decorative for daily-check usage; demote to a collapsible "View trend" disclosure. Goal: first payment row visible at fold (~700 px), not below it.

`iphone-06-expenses.png`: Total Expenses €1,660,449 + Top Categories list + Last-6-Months chart, then Filters, then list. Same problem. Top Categories is genuinely useful (it answers "where did money go?"). Suggest:
- Hero = Total Expenses (single tile, full width).
- Top Categories collapsed to a 3-bullet inline list under the hero, not its own card.
- Drop the chart on phone (it's a sparkline that doesn't communicate).

Total Expenses €1,660,449 — also: that's "all time" or "this period"? Sublabel says "across 10 categories" — useless. Should be "€1.66M this year" with period clear.

### 8. Data abbreviation
`formatMoneyCompact` (`formatMoney.ts:59-84`) does:
| Range | Format |
|---|---|
| ≥10M | `€9.7M` |
| 1M–10M | `€1.66M` (2 decimals) |
| 100K–1M | `€124K` (no decimals) |
| <100K | full `€33,779` |

Issue: at 100K-999K range, dropping all decimals loses precision (`€124K` could be €124,001-€124,999). For a KPI hero on a property dashboard where the user is comparing month-to-month, `€124.5K` is preferable. Suggest:
- 1K–10K: full `€1,234`
- 10K–1M: `€124.5K` (one decimal)
- 1M–10M: `€1.66M`
- ≥10M: `€9.7M`

The hero in `MobileDashboardSummary` uses compact for income, which currently formats `30541` as `€30,541` (because it's under 100K and falls through to full). That's correct — heroes should be precise when they fit. But the rail tiles use raw numbers (`83.3%`, `30`) so no abbreviation issue there.

Add a long-press / tap tooltip on the hero that shows the full precise value, currency, and as-of date. That hedges the precision loss for power users.

### 9. Information scent
"Do I have anything to do today?" The dashboard today fails this — first paint is `€30,541` (a number with no temporal urgency) and a hamburger overlapping the label. The alerts strip lives in `MobileDashboardSummary` but only renders below the hero, and only in the conditional code path (`hasAlerts`). Reorder to alerts-first.

Better information scent (in render order):
1. Alerts strip ("9 overdue · 3 extensions →") — only if non-zero, but PROMINENT, red/warning treatment.
2. Hero: Cash flow (this month, color-coded).
3. Unpaid payments shortlist (top 3) — these are the actionable rows. Already on the page but at line ~397, far below PortfolioDashboard. Hoist to position #3 on phone.
4. KPI rail.
5. Top performers / underperformers cards.
6. "View portfolio analytics →" link.

The unpaid payments block (`DashboardPage.tsx:397-528`) is genuinely the meat of "today's tasks" — Mark Paid lives there, the overdue badges are there. It needs to be ABOVE PortfolioDashboard on phone.

---

## Priority Punch List

### P0 — broken or duplicated content (ship this week)
- **P0.1** Define `--bottomnav-h: 64px` (or measure dynamically) so `Layout.tsx:107` actually reserves bottom-tab-bar space. Bug #6.
- **P0.2** Add `MobileMenuButton` + sticky header in `DashboardPage.tsx`. Eliminates floating-hamburger overlap. Bug #1.
- **P0.3** Hide `PortfolioDashboard` below `md`. Single line change: wrap line 206 in `hidden md:block`. Eliminates bugs #3, #4, #5 in one move.
- **P0.4** Replace rail tile width `w-[60%] xs:w-[48%]` with `min-w-[11rem] max-w-[14rem]`. Bug #2.

### P1 — composition (ship within sprint)
- **P1.1** Swap hero KPI: Monthly Income → Monthly Cash Flow (sign-colored), demote income to rail tile.
- **P1.2** Hoist unpaid-payments block above PortfolioDashboard in `DashboardPage.tsx` (or render it inside `MobileDashboardSummary` directly).
- **P1.3** Surface Top 5 / Bottom 5 performers in phone /dashboard outside `PortfolioDashboard` (currently inaccessible on phone once P0.3 lands).
- **P1.4** Add a "Portfolio analytics →" CTA card on phone routing to /reports.
- **P1.5** Payments page: collapse Pending + Overdue into a two-up row. Demote 6-month chart to a disclosure.
- **P1.6** Expenses page: collapse hero + Top Categories into one card. Drop chart on phone.

### P2 — polish (next sprint)
- **P2.1** iPad breakpoint fixes: switch `lg:grid-cols-3` → `md:grid-cols-3` in `PortfolioSummaryCards.tsx:163` and similar in `PortfolioDashboard.tsx:336,362`.
- **P2.2** Replace snap-mandatory with snap-proximity on the KPI rail.
- **P2.3** Add fade-right mask on the rail to telegraph scrollability.
- **P2.4** Tweak `formatMoneyCompact` thresholds — one-decimal in the 10K-1M range.
- **P2.5** Long-press tooltip on hero KPI showing full precision + as-of timestamp.
- **P2.6** Period chips/date range on /reports become a bottom-sheet picker on phone, not an inline button row.
