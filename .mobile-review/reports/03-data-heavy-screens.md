# Data-Heavy Screens Mobile Strategy — Buurman

## Overall Approach

Buurman's mobile audience is small landlords doing triage on the go: "anything overdue?", "who hasn't paid?", "any pending extensions?". Dashboards built for a 1440px-wide analyst (six KPIs, four charts, a 30×8 table) collapse into a 7-screen-tall scroll on a phone, burying the only three numbers that matter. The strategy across every data-heavy screen is the same: **decide-on-mobile, drill-on-desktop**. Mobile gets a tight, action-oriented vertical column with abbreviated numbers, opinionated defaults, and progressive disclosure (modals, bottom sheets, "view all"). Charts collapse to single-series sparklines or ranked top-N lists; tables become card lists keyed by the most diagnostic field; filter panels move to a bottom sheet; raw KPI grids become a horizontal-scroll "KPI rail" so the screen below it can host primary content. Everything below `md` is rebuilt — not just restyled — to respect that a 390px viewport has roughly 1/16th the area of a 1440px one.

---

## 1. Dashboard Mobile Redesign

### Mobile composition (ordered, above-the-fold first)

For a 390×844 viewport, "above the fold" is realistically ~720px of dashboard content after the header. Order:

```
┌──────────────────────────────────┐
│ ☰  Dashboard          🔔3   ⋯    │  56px sticky header
├──────────────────────────────────┤
│ HERO ALERT STRIP                 │  72px — only if there are alerts
│ ⚠ 9 overdue · 2 extensions →    │  tappable, scrolls list into view
├──────────────────────────────────┤
│ ┌──────────────────────────────┐ │
│ │ Monthly Cash Flow            │ │  192px — single "north star" card
│ │  −€25,828        ▼ −12% MoM  │ │  with sparkline, colored by sign
│ │  ▁▂▃▂▁▂▂▃▄▅▄▅                │ │
│ └──────────────────────────────┘ │
├──────────────────────────────────┤
│ KPI RAIL (horizontal scroll)     │  140px — 5 compact KPI tiles
│ [Value][Equity][Occ][CapRate][▶] │  swipe through; pagination dots
├──────────────────────────────────┤
│ Pending actions          See all │  48px section header
│ ┌──────────────────────────────┐ │
│ │ Extension #2 · CON…WHZ9      │ │  Card 1 of 2 — tap = detail
│ │ €502 → €527  until 17/02/31  │ │  swipe-left reveals Activate/Decline
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
       ↓  user has decided to scroll = wants depth
[ Unpaid payments (top 5 + view all) ]
[ Property performance (top 5 NOI / bottom 5 CF) ]
[ Charts module — opens fullscreen on tap ]
[ Property status donut + counts ]
```

The dashboard's job above the fold is **triage**, not analysis. A landlord opening the app at a tram stop needs to know "is anything on fire?" inside 3 seconds. Charts and the 30-row table are not that.

### Per-section recommendation

- **Pending Extensions card**: Keep at top *only when count > 0*. On mobile, render as a compact card with sticky horizontal row layout: `Ext #2 · €502→€527 · 17/02/31` with `Activate` / `Decline` as a swipe-action (iOS-style) plus an explicit "•••" trailing button for accessibility. Currently the card stacks `EUR 502.74 → EUR 527.35 until 17/02/2031` vertically — fix by abbreviating to `€502→€527·17/02/31`. If count > 3, show first 2 + "View all (n)".

- **Portfolio Overview header (6M/1Y/.../All + date pickers + CSV/PDF)**: This entire block is desktop-only chrome. On mobile, move the time range into a single dropdown (`Last 12 months ▼`) that opens a bottom sheet with the 8 presets + custom range. CSV/PDF buttons move into a "•••" overflow menu in the page header. The "30 of 30 properties have financial data" line becomes an info icon tooltip.

- **6 KPI cards (Value, Equity, Cash Flow, Cap Rate, CoC, Occupancy)**: Promote **Monthly Cash Flow** to a full-width hero card with sparkline + MoM delta — it's the only KPI a small landlord checks daily. The remaining 5 go into a horizontal-scrolling **KPI rail** (snap points, pagination dots). Don't stack 6 vertically — that's 1,100px of nothing-actionable. Don't 2×3 grid them either — at 390px wide minus padding, two cards per row leaves ~165px each, and `€9,671,330` doesn't fit.

- **4 chart panels**: Off the dashboard on mobile. Replace with a single **"Insights"** card that shows the Cash Flow sparkline (large), and a row of four mini-spark tiles `[Allocation] [Comparison] [Equity] [Occupancy]` — tap any to open a fullscreen chart modal (auto-rotate to landscape orientation hint). Do NOT render Recharts components stacked on a 390px column — axis labels overlap, legends wrap into 3 rows, tooltips are unusable without hover.

- **Property Performance table (30×8)**: Replace with two ranked card lists: **"Top 5 by NOI"** and **"Bottom 5 by Cash Flow"** (red-highlighted, action-implying). "View all 30 ▶" opens a dedicated route `/dashboard/properties-performance` with the table in a horizontally-scrollable wrapper + sticky first column. On `md` (tablet), keep the table but add `overflow-x-auto` and sticky first column.

- **Portfolio Occupancy chart**: Replace with a single line: `Occupancy 88.5%   ▁▂▃▄▅▄▅▅▆▇▇▇` and a `▼ -1.2% vs last month` delta. Full chart on tap.

- **Property Status (donut)**: Donut is fine on mobile because it's small and self-contained. Render at 120px diameter with legend below in a 2-column grid: `● Occupied 25  ● Vacant 3  ● Maintenance 2`.

- **Unpaid Payments (25 items, €33,779)**: This is THE list a landlord wants to see. Show top 5 most-overdue with action affordance, then "View all 25 →". See §6 for card spec.

- **Total Properties / Occupied / Occupancy Rate / Monthly Income**: These are redundant with the KPI rail — kill them on mobile.

---

## 2. KPI Card Pattern

The hero card and the rail tiles are two distinct components. **Don't use 2-up grid below md** — currency values like `€9,671,330` are 11 characters and force micro-typography that defeats the purpose.

### Hero card (full-width, ~180px)

```tsx
<HeroKpiCard
  label="Monthly Cash Flow"
  value={-25828}
  format="currency-abbrev"   // → "−€25,828"
  delta={{ pct: -12.4, vs: "last month" }}
  sparkline={cashFlowSeries}
  tone={value < 0 ? "negative" : "positive"}
/>
```

ASCII mock:

```
┌────────────────────────────────────────────┐
│ Monthly Cash Flow                       ⓘ  │   12px label, slate-500
│                                            │
│  −€25,828                                  │   36px display, red-600
│  ▼ −12.4% vs last month                    │   13px delta chip
│                                            │
│  ▁▁▂▂▃▂▂▃▄▅▄▅▄                             │   40px sparkline, full width
└────────────────────────────────────────────┘
```

### Rail tile (compact, ~150×140 with snap)

```
┌────────────────┐ ┌────────────────┐ ┌─────────
│ Portfolio Value│ │ Total Equity   │ │ Occupanc
│                │ │                │ │
│ €9.67M         │ │ €6.74M         │ │ 88.5%
│ +2.1% YoY      │ │ ▲ +€124K      │ │ ▼ −1.2pp
│   ▁▂▃▄▅       │ │   ▂▃▄▅▆       │ │   ▆▆▅▅
└────────────────┘ └────────────────┘ └─────────
```

Container: `flex overflow-x-auto snap-x snap-mandatory gap-3 -mx-4 px-4 scroll-pl-4`. Pagination dots below: `● ○ ○ ○ ○`.

### Currency formatting rules (mobile only, < md)

- `≥ 1,000,000` → `€9.67M` (2 decimals)
- `100,000 – 999,999` → `€124K` (no decimals)
- `< 100,000` → `€33,779` (no decimals, locale-grouped)
- Negative → leading minus + `text-red-600`, never parentheses (less legible)
- **Never** `EUR2,646.00` — this is the current bug. Use `Intl.NumberFormat(locale, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })`. The `EUR` prefix happens because of a missing locale or `currencyDisplay: 'code'` — switch to `'narrowSymbol'`.

Tap on any abbreviated number → opens a small popover with the full precision value (replaces hover-tooltip pattern).

---

## 3. Charts on Mobile

### Cross-cutting Recharts rules below `md`

- Drop `<CartesianGrid />` entirely
- Drop Y-axis labels; keep min/max as annotation badges in the card corners
- X-axis: max 4 ticks (`interval="preserveStartEnd"` + custom tickFormatter to `MMM`)
- Drop `<Legend />`; encode series via title (single series only on mobile)
- `<Tooltip />` → use Recharts' `trigger="click"` (built-in in 2.10+) instead of hover, with a custom `<MobileTooltip />` content component that's wider and dismissable
- Increase `<Line dot={{ r: 6 }} activeDot={{ r: 10 }}>` for touch targets
- Hide secondary series by default; surface a "Compare" toggle inside the fullscreen view

### Per-chart recommendation

- **Portfolio Cash Flow (line, 2 series)**: On mobile, render as **single-series sparkline** in the hero card. Income/Expense split is a fullscreen-modal feature. Color: green if last-month delta ≥ 0, red otherwise.

- **Portfolio Allocation (donut)**: Keep as donut at 140px. Legend below in 2-col grid showing only top 3 + "Others (4)". Tap legend row → fullscreen with all slices and percentages.

- **Property Comparison (horizontal bar, n=30)**: **Replace entirely** with a top-5 / bottom-5 card list — same data, dramatically more legible at this width. Horizontal bars at 390px wide have ~250px of bar area, which makes 30 bars 5px tall each. Unusable. Keep h-bar on desktop.

- **Equity Composition (horizontal bar)**: **Replace** with a stacked progress bar (single horizontal row, color-banded) plus a `% breakdown` list below. The "h-bar with 3 categories" only needs ~24px of vertical space when expressed as one stacked bar.

- **Portfolio Occupancy (line)**: Sparkline + headline number, as described in §1. Fullscreen modal has the full Recharts line.

### Expand chart → fullscreen modal

Yes — every chart's `⤢` button should open a fullscreen sheet. Best pattern:

1. Slides up from bottom (`translate-y` transition)
2. Detects portrait orientation and shows a "Rotate device for better view" banner (don't force orientation lock — annoying)
3. Has a sticky bottom action bar with `Series ▾`, `Range ▾`, `Share`, `Close`
4. Pinch-to-zoom on the chart area (use `<TransformWrapper>` from `react-zoom-pan-pinch` over the Recharts SVG, or Recharts' built-in `<Brush>`)

---

## 4. Tables → Adaptive Lists

### Per-page treatment

- **Property Performance (30×8)**:
  - `< md`: Ranked card list. Two pinned sections: "Top 5 by NOI" (green chips), "Needs attention (5)" (red chips for negative CF). "View all 30 ▶" navigates to a dedicated route.
  - `md` (tablet 768): Keep table but wrap in `<div class="overflow-x-auto">` with `<table class="min-w-[900px]">` and use CSS `position: sticky; left: 0; background: white; z-index: 1` on the first cell of every row. Add a fade-gradient on the right edge to signal scroll.
  - `lg`+: Current behavior.

  Card spec:
  ```
  ┌────────────────────────────────────────┐
  │ Finca El Olivar 122          [Top 3]  │
  │ NOI €4,210/mo · Cap 6.2% · Occ 100%   │
  │ ▲ +€340 vs last month            ⋯    │
  └────────────────────────────────────────┘
  ```

- **Payments table** (not in screenshot but exists): On `< md`, card list grouped by status (Overdue → Pending → Paid). Each card: tenant + property + amount + due date + status pill + swipe-left for "Mark Paid". Group headers are sticky. Add a segmented control at top: `[All · Overdue · Pending · Paid]`. The hero KPI cards (Pending €91,624 / Overdue €11,480) stay — they're already mobile-friendly.

- **Expenses table**: Same card pattern. The current `Total Expenses €1,660,449` card + `Top Categories` mini-list (already on screenshot) is the right pattern — extend it. Below those, the expense rows themselves become cards: `[icon] Category · Property · €amount · date`. Group by month with sticky headers.

- **Contracts list (already cards)**: Mostly good. Issues from screenshot: `#CON01KRTERBEZ336M1A39Z8180FSZ` is a 26-char Sid that takes a full row and adds zero scan value — hide on mobile, show only on detail page. Status pill (Terminated/Active) should move to the top-right corner of the card (currently good). The bottom row (Start/End/Rent) is currently 3 columns at the screen edge — at 390px this works, but on iPhone SE (375px) it gets cramped. Use `flex-wrap` with `flex-1 min-w-[100px]`.

---

## 5. Filter UX Refactor

The Properties screenshot shows the core problem: a filter panel with two grouped tag sets (6 categories + 9 statuses) wrapping into 6+ rows. That's ~280px of vertical real estate before a single property appears.

**Recommended pattern below `md`**:

1. Collapse the entire filter panel into a single sticky bar:
   ```
   ┌──────────────────────────────────┐
   │ 🔍 Search…              ⇅  ⛕(2) │   ← chip count = active filters
   └──────────────────────────────────┘
   ```
2. The `⛕` icon (or a "Filters" text button) opens a **bottom sheet** (Radix `<Dialog>` with `bottom-0 rounded-t-2xl`):
   ```
   ┌──────────────────────────────────┐
   │  ─                               │   drag handle
   │  Filters              Clear all  │   sticky header
   ├──────────────────────────────────┤
   │  Category                        │
   │  [ All ] [ Residential ] [ … ]   │
   │                                  │
   │  Status                          │
   │  [ All ] [ Vacant ] [ Occupied ] │
   │                                  │
   │  Sort                            │
   │  ○ Date Created  ○ Name  ○ Rent  │
   ├──────────────────────────────────┤
   │  [ Apply 12 filters ]            │   sticky footer
   └──────────────────────────────────┘
   ```
3. Active filters render as removable chips above the list: `[Residential ×] [Vacant ×] [+2]`. Tapping `+2` reopens the sheet.
4. Sort moves into the same sheet — eliminates the separate `⇅ Date Created ▼` dropdown that's wasting header space on contacts/contracts.

Apply this same pattern to **Contacts, Contracts, Payments, Expenses, Documents, Photos**.

---

## 6. List & Card Patterns

### Unpaid Payments row (dashboard + payments page)

```
┌──────────────────────────────────────────┐
│ Marlene Drees                            │
│ Finca El Olivar 122 · due 14 days ago    │
│ €2,613                       3 overdue 🔴│
│ ─────────────────────────────────────────│
│           [ Mark Paid ]   [ Remind ]     │   44px buttons
└──────────────────────────────────────────┘
```

- Whole card is tappable → opens payment detail. Buttons are explicit (not "swipe-only" — discoverability) but **also** swipeable left to reveal "Mark Paid" as a primary green action and "Remind" as secondary (iOS Mail style).
- Currency: `€2,613` not `EUR2,613.00` — fix the formatter.
- Property+tenant currently a single inline button at <44px tap height — split into card-wide tap (default) + action buttons row with `min-h-[44px]`.

### Property card (already cards on listing)

```
┌──────────────────────────────────────────┐
│ [thumb]  Finca El Olivar 122             │
│   80×80  San Sebastián · Residential     │
│          ● Occupied · €1,513/mo          │
│                                       ⋯  │
└──────────────────────────────────────────┘
```

### Contact card (refining current)

Current screenshot is decent but dense. Trim: hide email (truncate < md), phone becomes a `tel:` chip icon, "Key Holder" stays. The red overdue strip (`€2,613.00 · 3 payments overdue`) is the most valuable signal — promote to top-right corner badge.

---

## 7. Photos & Documents

### Photos

Current 2-col grid is fine. Improvements:

- `IntersectionObserver`-driven lazy load: don't render images outside the viewport + 1 screen buffer
- Use `loading="lazy"` + `decoding="async"` on `<img>` as fallback
- Aspect-aware layout: switch from fixed-ratio thumbs to a CSS columns masonry (`columns-2 gap-2`) so portrait/landscape shots don't waste space
- Tap → open a swipe-lightbox (use `yet-another-react-lightbox` or `swiper`), full-bleed, pinch-zoom, swipe-down to dismiss
- Show file metadata (KB, date) only on long-press → info sheet, not on card. Currently `122.3 KB · 17/05/2026` clutters every tile.

### Documents

Current row is too tall (~140px per document) because Type and Property wrap. Card pattern:

```
┌──────────────────────────────────────────┐
│ 📄 Invoice — Annual property tax 2026    │
│    EXPENSE · Finca El Olivar 122         │
│    1.2 MB · 17/05/2026             ⬇ ⋯  │
└──────────────────────────────────────────┘
```

- File-type icon (color-coded: PDF red, image purple, sheet green) in the leading 40px column
- Filename wraps to max 2 lines with `line-clamp-2`
- "Vendor invoice" badge moves to a small chip on the right
- Trailing column: download icon (44×44 tap target) + overflow `⋯` for share/delete/move
- Selection checkbox: only visible in "Select" mode (see §9) — saves 40px per row

---

## 8. Number Formatting & Data Density

### Rules below `md`

| Range | Format | Example |
|---|---|---|
| ≥ 10M | `€X.XM` (1 decimal) | `€9.7M` |
| 1M–9.99M | `€X.XXM` | `€1.66M` |
| 100K–999K | `€XXXK` | `€124K` |
| < 100K | full with separators | `€33,779` |
| Percentages | 1 decimal | `88.5%` |
| Percentage deltas | "pp" suffix for points | `−1.2pp` |
| Cap rate / yield | 1 decimal + `%` | `6.2%` |

Always render full value in an accessible tooltip / popover for screen readers via `<abbr title="€9,671,330">€9.7M</abbr>`.

### Negatives

- `−€25,828` not `(€25,828)` — minus is more scannable on mobile and parses correctly with screen readers
- Color: `text-red-600` for value, plus a `▼` glyph in delta chips. Don't rely on color alone (accessibility).

### Tooltip alternative on mobile (no hover)

- Long-press (500ms) → popover with full precision + context
- Or inline `ⓘ` icon next to ambiguous values that opens a small bottom sheet
- For chart tooltips: tap a data point → static tooltip pins to point until tap-elsewhere dismisses. Use Recharts' `trigger="click"` on `<Tooltip>`.

---

## 9. Bulk Actions & Selection

Mobile multi-select pattern:

1. Default state: rows have no checkbox (saves space)
2. "Select" button in page header (or long-press any row) enters **selection mode**:
   - Top bar transforms: `[× Cancel] 3 selected  [Actions ▾]`
   - Every row gets a leading 32px checkbox
   - Bottom action bar slides up with primary actions: `Delete · Move · Download · Share`
3. Long-press to enter is the discoverability problem — always provide the explicit "Select" button too
4. Exit on `Cancel`, `Back gesture`, or "Done"

Apply to: **Photos, Documents, Payments (bulk mark paid / remind), Expenses (bulk categorize)**.

ASCII:

```
Normal:                    Selection mode:
┌────────────────────┐     ┌────────────────────┐
│ ⓘ Properties     ⋯ │     │ × 3 selected    ⋮  │
├────────────────────┤     ├────────────────────┤
│ ┌────────────────┐ │     │ ☑ ┌──────────────┐ │
│ │ Finca El …     │ │     │   │ Finca El …   │ │
│ └────────────────┘ │     │   └──────────────┘ │
│ ┌────────────────┐ │     │ ☐ ┌──────────────┐ │
│ │ Chemin du …    │ │     │   │ Chemin du …  │ │
│ └────────────────┘ │     │   └──────────────┘ │
└────────────────────┘     ├────────────────────┤
                           │ Move │ Del │ Share│  ← sticky action bar
                           └────────────────────┘
```

---

## 10. Loading / Empty / Error States

Data-heavy screens need explicit treatment of these — currently mostly missing or generic spinners:

- **Loading**: Skeleton screens that match the final layout shape (sparkline → animated gradient bar; KPI card → gray block with label-and-value placeholders; cards → ghost cards). React Query `isPending` → render `<DashboardSkeleton />`. Avoid centered spinners on a blank screen — they make the app feel broken.

- **Empty**: Each list gets a contextual empty state with an illustration + primary CTA. Examples:
  - Properties (0): "No properties yet. Add your first to start tracking rent." + `[+ Add property]`
  - Unpaid (0): "All caught up — no overdue payments. 🎉" (use illustration not emoji in product)
  - Documents (0): "Drag a file or tap to upload"
  - **Pending Extensions (0)**: Hide the entire card. Don't render an empty "0 extensions awaiting action" state — that's just noise.

- **Error**: Inline error card with retry button at the section level (not full-screen). Network errors get a small banner that auto-retries with exponential backoff via React Query's defaults. Permission errors get a specific "You don't have access to this view" message.

- **Stale-while-revalidate**: Show cached data with a subtle "Updated 2 min ago · Refresh" footer per section. Don't blank-out the screen on refetch.

---

## Priority Punch List

### P0 — Ship first (1–2 sprints)

1. **Fix currency formatter** — `EUR2,613.00` → `€2,613`. One-line `Intl.NumberFormat` fix, used on every screen. (1d)
2. **Dashboard mobile layout** — hero Cash Flow card + horizontal KPI rail + ranked Top/Bottom 5 + Unpaid top 5. Hide charts on `< md`. (5d)
3. **Property Performance table responsive wrapper** — `overflow-x-auto` + sticky first column + fade gradient. Avoids broken layouts on iPad portrait too. (1d)
4. **Filter bottom sheet** — single component reused across Properties, Contacts, Contracts, Payments, Expenses, Documents, Photos. (4d)
5. **Unpaid payments card with 44px actions + swipe Mark Paid** on dashboard. (2d)
6. **Recharts mobile defaults** — drop grid, drop legend, simplify ticks, tap-to-tooltip. Single config object. (2d)

### P1 — Next (2–4 sprints)

7. **Fullscreen chart modal** with rotation hint + pinch zoom. (3d)
8. **Documents/Photos card refactor** with file-type icons, line-clamp, action menu. (3d)
9. **Selection mode** pattern + bottom action bar (Photos, Documents first). (3d)
10. **Number abbreviation system** (€9.7M, €124K) with `<abbr>` for full value. (1d)
11. **KPI sparklines** — add `recharts` `<Line>` mini-component to each KPI. (2d)
12. **Skeleton loading states** for all major screens. (3d)

### P2 — Polish

13. **Pull-to-refresh** on list screens (`use-pull-to-refresh` or custom).
14. **Empty state illustrations** + CTAs across all empty lists.
15. **Stale-while-revalidate footers** ("Updated 2 min ago").
16. **Sort moves into filter sheet** — kill standalone `Date Created ▾` dropdown.
17. **Search auto-focus** on filter sheet open + recent-searches dropdown.
18. **Long-press tooltips** for abbreviated values + ambiguous KPIs.
19. **Bulk Mark Paid** in selection mode (Payments).
20. **Tablet tweaks** — iPad portrait gets the table back with sticky first column; iPad landscape gets near-desktop layout.

---

## Files most likely to change

- `frontend/app/src/components/DashboardPage.tsx` — recompose for mobile breakpoint
- `frontend/app/src/components/dashboard/PortfolioSummaryCards.tsx` — split into HeroKpiCard + KpiRail
- `frontend/app/src/components/dashboard/PropertyPerformanceTable.tsx` — add wrapper, sticky column, mobile card-list variant
- `frontend/app/src/components/dashboard/PortfolioCashFlowChart.tsx` and siblings — mobile Recharts defaults, fullscreen modal
- `frontend/app/src/components/dashboard/PortfolioDashboardFullscreen.tsx` — reuse / extend for per-chart modals
- `frontend/app/src/utils/` — new `formatCurrency.ts`, `formatNumber.ts` with abbreviation rules
- New: `frontend/packages/ui/src/FilterSheet.tsx`, `KpiRail.tsx`, `HeroKpiCard.tsx`, `SwipeAction.tsx`, `SelectionBar.tsx`
