# Mobile UX Review (Round 2) — Buurman

## Verdict

The mobile composition has matured a lot — the property/contacts/payments/expenses screens now feel like they were *designed* for a phone rather than retro-fitted (good headers, hero metric cards, decent typography, working bottom tab bar). The native-feel gap remaining is concentrated almost entirely on **the Dashboard**, which is the worst offender for first-impression and the page the user just sent. It still wears its desktop clothes: a fixed floating hamburger collides with the hero card, the Portfolio Overview control bar is a horizontal desktop flex row jammed into 390 px, and the page itself has no real mobile header. Outside of the dashboard, three secondary issues hold the rest of the app back from feeling fully native: the legacy `<table>` on DocumentsPage, raw 26-char Sid identifiers wrapping inside property cards, and a couple of "filter row" patterns that still wrap to two/three rows on phone (Contacts header). Estimate: ~80% there. About a day of focused work on the four files below closes the gap.

## Critical Bugs (the user's screenshot — `iphone-01-dashboard.png`)

### 1. Floating hamburger overlaps "MONTHLY INCOME" hero label
**Root cause.** `Sidebar.tsx:297-316` renders a `position: fixed; left:4; top: calc(env-banner + 1rem)` menu button that is suppressed *only* when `useMobileNav().hasOwnMenuButton === true`. That flag is set by `MobileMenuButton`, which is in turn rendered by `ListPageHeader`. `DashboardPage.tsx` does **not** use `ListPageHeader` (it jumps directly from `Layout`'s padding into `<MobileDashboardSummary />`), so the floating button stays mounted and sits on top of the gradient hero card.

**Fix.** Two options, in order of preference:
- (Preferred) Give the Dashboard a proper mobile page header. Wrap the phone tree in a `ListPageHeader title={t('dashboard.title')} icon={LayoutDashboard} />` (rendered `md:hidden` if the desktop layout shouldn't change). This also gives the page a title, which it currently lacks.
- (Quick) Mount a bare `<MobileMenuButton />` at the top of `DashboardPage.tsx`'s mobile tree so `setHasOwnMenuButton(true)` fires and the floating button hides — but then add `pt-14` to the mobile summary so the hero starts below the inline menu button.

### 2. KPI rail tiles truncate labels ("TOTAL PROPER…", "OCCUPI…")
**Root cause.** `MobileDashboardSummary.tsx:157` — `RailTile` is `w-[60%] xs:w-[48%]` (≈228 px at 390 px viewport), then inside the tile the label row is `flex items-center gap-2` with an icon (`h-4 w-4` + 8 px gap = ~24 px chrome) and a `<span class="truncate">`. The combined uppercase tracking-wider text "TOTAL PROPERTIES" at `text-xs` needs ~150 px; with 24 px of icon chrome it just barely fits, but the card has 16 px padding on each side (`p-4`) so the actual text-box is ~196 px — *still* not enough for "TOTAL PROPERTIES" at `tracking-wider`. Same story for "OCCUPIED" (would fit) and "ACTIVE PROPERTIES IN PORTFOLIO" on the sub-line.

**Fix.** Three small things:
- Drop `tracking-wider` on tile labels (it costs ~10% width on the most-truncated label).
- Either remove the inline icon from the tile label row, or move the icon to its own row above the label so the label gets the full tile width.
- Bump tile width from `60%` to `72%` so two tiles peek (the rail is a `snap-x`, you don't need to *fit* two tiles — peeking encourages discovery of the scroll). Or widen to a clean 220 px fixed.

### 3. Range chips (6M/1Y/…/10Y/All) overflow the card edge
**Root cause.** `PortfolioDashboard.tsx:230-244`. The chip row is `<div className="flex gap-1">` with 8 buttons (`PERIOD_OPTIONS` has values `6, 12, 24, 36, 48, 60, 120, all` = 8 chips). No mobile escape hatch — no horizontal scroll wrapper, no media query, no chip-row truncation. With `px-3 py-1.5 text-xs` each chip is ~44 px wide → 352 px just for chips + 7 × 4 px gaps = 380 px, leaving zero room for the parent Card's `p-6` (24 px each side).

**Fix.** Two options:
- (Preferred, native-feeling) Wrap the chip row in an overflow-x-auto snap container on phone:
  ```tsx
  <div className="-mx-2 px-2 overflow-x-auto md:overflow-visible">
    <div className="flex gap-1 md:flex-wrap whitespace-nowrap">…chips…</div>
  </div>
  ```
- (Reductive) Hide 2Y/3Y/4Y/10Y at `<md` and expose them via the existing custom-range date inputs (or behind a "More ranges" segmented overflow menu).

### 4. Date input "17/05/2026" clipped on the right edge
**Root cause.** Same parent block, `PortfolioDashboard.tsx:246-268`. The date-input row is *also* a desktop-flex (`flex gap-2 items-center` with two `<input type="date">` + an em dash) that lives next to the chip row inside the same `flex flex-wrap` container. After the chip row wraps to its own line, the date row tries to occupy the next line — but iOS Safari renders `<input type="date">` at its intrinsic width (~140 px each + the dash + 8 px gap = ~292 px), and inside a `p-6` card on 390 px viewport that's already over budget. The second input visually clips against the right card edge.

**Fix.**
- At `<md`, stack the date inputs vertically (`flex-col items-stretch` not row), make them `w-full`, and put the em-dash inside a `text-center` `my-1` row OR replace it with `to` text label.
- Better: collapse the entire custom-range UI into a single "Custom range…" trigger that opens a `<Sheet>` (you already have one) with two large native date pickers — this is the iOS-native pattern. The chips become the primary, the sheet handles the edge case.

### 5. "Total Equity" value clipped by the bottom tab bar
**Root cause.** `Layout.tsx:107` uses `pb-[calc(var(--bottomnav-h)+var(--safe-bottom,0px))]`. `--bottomnav-h` is defined in `packages/ui/src/styles/theme.css:213` as `3.75rem` (60 px) — so the math is OK *at the page bottom*. But the screenshot shows the cut-off mid-scroll. That isn't actually a layout bug — that's just where the viewport ends and the tab bar overlays content below. What it *is* is an information-hierarchy bug: the user reads "Total Equity" + a partial number and assumes content is broken.

**Fix.** Two compounding fixes:
- The Portfolio Overview block (chips + dates + export) is so tall on phone that by the time the user scrolls past it, they hit the bottom of the viewport mid-value of the next card. Shrink the controls block (see fixes 3 & 4) and the second card will sit fully visible.
- Visually communicate that more content is below: add a subtle bottom shadow / fade *above* the tab bar (`linear-gradient` mask on the main scroll container's last 24 px on phone) so users learn the bar is overlaying, not chopping. Cheap, native-feeling, fixes the perception.

## Findings by Area

### 1. App Chrome

- **Dashboard has no title** (`iphone-01`). Every other page has "Properties"/"Contacts"/"Payments"/"Expenses"/"Document Lib…" left-aligned with an icon. The Dashboard just dumps the hero KPI in directly, which makes the floating hamburger collision worse and breaks consistency. The page literally has no name — confusing on first visit.
- **Page title truncation on Documents** (`iphone-07`). "Document Library" is rendered as "Document Libr…" because the `ListPageHeader` title shrinks to fit alongside the kebab menu but doesn't have enough room with the title icon + back button. The label should drop the word "Library" on phone (`Documents` is enough) or shrink the icon.
- **Hamburger button top position** is set with `top: calc(var(--env-banner-height, 0px) + 1rem)` (`Sidebar.tsx:309`). Good — respects the orange "LOCAL ENVIRONMENT" strip. Confirmed visually it sits at the right vertical position.
- **Bottom tab bar.** Five tabs, even spacing, safe-area inset honored. Works. Active state is clear (primary color + bold). One minor: 11 px label leading-none is very tight — at iPhone Mini sizes "Properties" wraps awkwardly. Consider `text-[10px]` + `leading-tight` or letter-spacing tweak.
- **Status bar / env banner.** The orange `LOCAL ENVIRONMENT` ribbon is sticky-top, sets `--env-banner-height`, and the hamburger respects it. Solid.

### 2. Layout Overflow

- **Dashboard Portfolio Overview header** (`iphone-01`) — see critical bugs §3 & §4. The single worst overflow in the app.
- **Contacts page sort/filter row** (`iphone-03`) — the `Date Created` `<select>` is full-width and pushes the filter button (`F…`) off the right edge. The row goes: `search icon button` + `↑↓ sort icon` + `<select>` + `direction arrow` + `filter dropdown` — five controls on a 358 px wide content area. Filter chip is clipped.
- **Property identifier** (`iphone-02`) — `#PRO01KRVEH57A7SSF33CZ6M8E6ZY1` (28 chars) is rendered as `p-sm` text with no truncation, taking a full line below the street. On phone that's wasted vertical real estate and unrecognizable noise. Truncate or hide on `<md`.
- **Payments page Filters dropdown** (`iphone-04`) — sits floating right with no left-side label. Looks orphaned. Group it with a count badge ("X results · Filters (1)") in a single row.
- **Documents page** (`iphone-07`) — full `<table>` with `DOCUMENT | TYPE | …` headers crammed into 390 px. Document filename column wraps to 3-4 lines per row. Type pill ("EXPENS…") truncates. This is the only major page that didn't get migrated to the card-list-on-phone pattern.
- **Hero card** (`iphone-01`) — apart from the hamburger overlap, the hero itself is well-proportioned. `€30,541` at `text-4xl` is the right loudness for the primary KPI.

### 3. Information Hierarchy on Mobile

- **Dashboard** leads with Monthly Income (good) but immediately follows with a horizontal scroll rail (no scroll affordance — feels like content runs off the right). Then Portfolio Overview's broken controls eat ~30% of viewport height before showing any actual data. On phone, the user should see: hero → alerts → key KPIs → recent activity. Move "Pending Invitations" and "Pending Extensions" *above* PortfolioDashboard on `<md` — those are actionable; the portfolio analytics are not.
- **Property card** — image is full width and ~16:9, then name, then a 28-char identifier (low value), then city/postal, then unit count, then a tag chip row. The identifier should be `hidden md:block` or `truncate w-24 font-mono text-text-muted` and stop fighting the address for visual weight.
- **Contacts** — the card is dense but well-organized: avatar+name → type → key holder → contact info → overdue alert → contract count + relative time. The overdue strip ("€2,613.00 · 3 payments overdue") is the right info, rendered as a red callout — good. But the value `+41726172569` is rendered without a leading space or formatting, fighting the email next to it. Consider stacking phone/email on phone.
- **Payments** leads with two KPIs (Pending / Overdue) which is exactly right; Overdue is in danger-bg, well-flagged. Good.
- **Expenses** leads with Total + Top Categories + sparkline + filters. The information ladder is right.

### 4. Touch Feel & Native-ness

- **Bottom tab bar** is now native-feeling — taps respond, active state animates color, safe-area handled. Good.
- **The Dashboard's KPI rail** is missing the iOS overscroll bounce affordance: the rail doesn't visually indicate it's scrollable. Add `scrollbar-none` and a soft right-edge gradient mask (last 24 px fades) so users *see* there's more content sideways.
- **Range chips** ought to be a segmented control feel (touch one, slide to another). Right now they're 8 individual buttons that wrap — un-native.
- **Date inputs** rendering as native `<input type="date">` is correct on iOS (uses the wheel picker). Good — don't replace with a custom React picker on phone, that's the rare time native wins.
- **Page transitions** — react-router default transitions, no slide. On iOS the right-edge swipe-back gesture doesn't trigger a navigation transition because there's no `<NavigationContainer>`/`framer-motion AnimatePresence`. Lower priority but a real "feels like a website" tell.
- **Pull-to-refresh** absent. Not critical, but it's the #1 native iOS gesture users expect on data lists. The TanStack Query refetch is right there.
- **Long-press / context menu** absent on cards. Property card on iOS gets the OS share/copy sheet on long-press — overrides the app. Consider `touch-action: manipulation` and a proper hold-to-select pattern (which would tie into `SelectionBar`).

### 5. iPad (portrait, 768×1024)

`ipad-01-dashboard.png`: Rail mode is correctly active (icon-only 64 px column on the left). Hierarchy: Buurman logo top-left, vertical icon nav, then the content area uses the full remaining width. *But:* the Portfolio Overview chips still render as desktop flex (8 chips on one row — fits at 768 px just barely). KPI cards become a 2x4 grid — well-sized. The Portfolio Cash Flow chart is full width, readable. Looks essentially correct.

`ipad-02-properties.png`: Property cards render as a 2-column grid — proportions are good. Filter row at the top is fine on 768 px. One minor: the Filters card uses very wide horizontal chip rows ("All Categories Residential Commercial Industrial Agricultural Mixed-Use" on one line + "All Statuses Vacant Occupied Self-Occupied Maintenance Unavailable Under Renovation Fallow Listed" on the next — that second row barely fits). On 768 px portrait it works; landscape will be even more crowded if not collapsed. Consider expandable filter sheet on iPad like on phone.

**Wasted space?** No, density looks right. iPad isn't being treated as a stretched phone.

### 6. Forms (Add Property — `iphone-05`)

Very strong. The MobileFormStepper gives a clear "Step 1 of 3" + section label + progress bar. Fields are large, well-spaced, labels are inline, required asterisks present. The bottom action bar (`Cancel | Continue >`) sits above the tab bar and looks docked. The form has *room to breathe* — no over-cramming.

Two refinements:
- **Country `<select>`** uses the OS picker — good. But on iOS the value is rendered tiny ("Select a country" gray) and the chevron is too small. Use the `Select` primitive consistently.
- **Cancel button on phone** could be a left-arrow back icon to align with iOS conventions (`< Cancel` is rare; `←` is universal). Or use the existing header back arrow and remove the duplicate Cancel.

### 7. Consistency Pass

- **Page header pattern.** Properties, Contacts, Payments, Expenses, Documents all use `ListPageHeader`. Dashboard doesn't. Add it for parity.
- **Add (+) button.** Properties, Contacts, Expenses have a primary `+` action in the header. Dashboard has none (correct — no add action). Payments has a calendar-with-check icon as primary (`iphone-04`) — semantically unclear what it does. A second-row button reading "Record Payment" would be clearer than a glyph.
- **Filter affordance.** Inconsistent.
  - Properties: `Filters ⌄` dropdown chip (top of list)
  - Contacts: `F…` (truncated, almost off-screen)
  - Payments: floats right, no count or label context
  - Expenses: dedicated `Filters` chip with chevron, right-aligned
  - Documents: `All Types` `<select>` (not a chip)
  Adopt one pattern (FilterSheet trigger chip with `Filters (count) ⌄`) everywhere.
- **Spacing scale.** Some cards use `p-4`, some `p-5`, some `p-6` (Portfolio Overview card is `p-6` and that's part of why its inner flex overflows). Pick one mobile card padding (`p-4`) and one desktop padding (`p-6`).
- **Identifier rendering.** Property card shows full `#PRO01…` Sid. Contact card doesn't show identifier at all. Pick: show truncated `#PRO…ZY1` everywhere, or nowhere.
- **Iconography.** Generally consistent (Lucide). One inconsistency: dashboard uses `DollarSign` for currency, payments page uses `DollarSign`, but the bottom tab bar Payments uses `$` glyph styling that doesn't quite match. Minor.

## Priority Punch List

### P0 — fix before next demo (visible bugs in the screenshot)

| # | Page | Issue | Fix | Effort |
|---|------|-------|-----|--------|
| 1 | Dashboard | Floating hamburger overlaps hero KPI | Add `<ListPageHeader title="Dashboard" icon={LayoutDashboard} />` at top of `DashboardPage.tsx` (md:hidden wrap), so `hasOwnMenuButton` is set and the floating button hides. Add a small top spacer for hero. | S |
| 2 | Dashboard | Range chips overflow card edge | In `PortfolioDashboard.tsx:230-244`, wrap chip row in `overflow-x-auto -mx-2 px-2` with `whitespace-nowrap` on phone. | S |
| 3 | Dashboard | Date inputs clip right edge | In `PortfolioDashboard.tsx:246-268`, stack inputs vertically `<md` (`flex-col items-stretch w-full`). Better: replace with a single "Custom range…" Sheet trigger on phone. | S→M |
| 4 | Dashboard | KPI rail tile labels truncate | In `MobileDashboardSummary.tsx:159`, drop `tracking-wider` on tile labels and move the icon to its own row (icon above label, not beside). Bump tile width to `w-[72%]`. | S |
| 5 | Dashboard | Hero value cut by tab bar perception | Trim Portfolio Overview height (via #2/#3 fixes) and add a 24 px gradient mask above the tab bar so the cut-off reads as a hint, not a bug. | S |

### P1 — meaningful native-feel polish

| # | Page | Issue | Fix | Effort |
|---|------|-------|-----|--------|
| 6 | Documents | Page still uses desktop `<table>` on phone | Migrate `DocumentsPage.tsx:349-529` to the `ResponsiveTable` / card-list pattern used on Payments. | M |
| 7 | Contacts | Sort/filter row clips filter chip | Reflow the row: search + sort + filter on phone, hide the sort `<select>` behind the filter sheet on phone. | S |
| 8 | Properties | 28-char identifier wraps under address | In `PropertyCard.tsx:69-71`, render identifier as `font-mono text-xs text-text-muted` with `truncate` and hide on `<md` (or shorten to last 6 chars `…E6ZY1`). | S |
| 9 | Dashboard | No mobile-first information ladder | Move `PendingInvitationsPanel` + `PendingExtensionsPanel` above `PortfolioDashboard` on `<md`. Actionable content first; analytics second. | S |
| 10 | App-wide | KPI rail has no scroll affordance | Add right-edge gradient mask on the rail container; remove visible scrollbar. | S |
| 11 | App-wide | Filter affordance inconsistent | Standardize on `Filters (count) ⌄` chip pattern (already exists in `FilterSheet`) across Properties / Contacts / Payments / Expenses / Documents. | M |

### P2 — refinement / nice-to-have

| # | Page | Issue | Fix | Effort |
|---|------|-------|-----|--------|
| 12 | Dashboard | Range selector feels like buttons not a segmented control | Replace 8 chips with a styled segmented control + "Custom" option. | M |
| 13 | Lists | No pull-to-refresh | Wrap `Properties` / `Contacts` / `Payments` content in a phone-only PullToRefresh that calls `refetch()` on TanStack Query. | M |
| 14 | Cards | No long-press selection | Wire long-press on Property/Contact cards into existing `SelectionBar`. | M |
| 15 | Forms | Cancel button on stepper should be `←` on iOS | Adjust `MobileFormStepper` action bar: left button becomes a back arrow when there's no prior step, label moves to `aria-label`. | S |
| 16 | Documents | Page title "Document Library" truncates to "Document Libr…" | Shorten title to "Documents" on phone. | XS |
| 17 | Payments | Primary calendar-check action glyph is ambiguous | Replace with `+ Record` or `+` icon with explicit label. | XS |
| 18 | App-wide | No swipe-back transition | Wrap routes in `framer-motion AnimatePresence` + slide on phone routes. | L |
| 19 | iPad | Filter card chip rows too wide in landscape | Collapse to FilterSheet on iPad <lg. | M |
| 20 | Tab bar | "Properties" label tight at smaller widths | `text-[10px]` + `leading-tight` on tab labels. | XS |

---

**Files to touch for the P0 sweep** (estimated 2-4 hours total):
- `frontend/app/src/components/DashboardPage.tsx` (~180–207): add `ListPageHeader`, reorder panels.
- `frontend/app/src/components/dashboard/MobileDashboardSummary.tsx` (~154–170): RailTile label/icon row + width.
- `frontend/app/src/components/dashboard/PortfolioDashboard.tsx` (~218–268): chip-row overflow-x-auto + stacked dates `<md`.
- `frontend/app/src/components/Layout.tsx` (~107): no change needed (var is defined), but optionally add the gradient mask above the tab bar.
