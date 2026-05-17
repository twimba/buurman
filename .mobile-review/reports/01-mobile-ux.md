# Mobile UX Review — Buurman

## Executive Verdict

**Mobile readiness: 2/5 — "responsive, not mobile."** The app reflows without horizontal scroll, components don't break catastrophically, and the hamburger drawer works. But almost every surface feels like a desktop screen squeezed into 390px rather than designed for a thumb. The three biggest issues, in priority order, are: (1) **page headers are a collision zone** — the title, subtitle, refresh, CSV/Import, and primary action buttons all share a single `flex-wrap` row that overlaps the page title and pushes critical action buttons under or past the hamburger on every list page (`04-properties`, `05-contacts`, `07-payments`, `08-expenses` all show this clearly); (2) **no bottom tab bar and a hamburger that hides the only nav** means every navigation move is a 2-tap, off-thumb interaction in the top-left corner — fatal for a daily-use management app; (3) **the dashboard is a desktop dashboard**, opening with a "Pending Extensions" alert card followed by date-range pickers, CSV/PDF exports, then 6+ KPI cards and full-width charts — none of which are above-the-fold-useful on a phone (`03-dashboard`, `16-ipad-dashboard`).

The good news: list pages already use card layouts for contacts, properties, and contracts; the Add Property form is single-column and reasonable; the Keycloak login is clean. The bones are fine. The chrome and the navigation are the problems.

## Findings by Area

### 1. Global Navigation

**Issues**

- Hamburger is anchored top-left (`fixed left-4 top-4`), the furthest pixel from a right-handed user's thumb. On a 390x844 device, that's a 760px reach.
- Hamburger sits on top of the page title rather than inside a header bar — see `04-properties` where the icon visually overlaps the "Properties" word, and `12-property-detail` where it overlaps "42185m²".
- The drawer (`14-mobile-drawer-open`) is a perfectly serviceable list but it's the *only* way to switch sections. Switching from Properties → Payments is 3 taps (open drawer, tap Payments, wait for route).
- iPad at 768px gets the *mobile* layout (`16-ipad-dashboard`, `17-ipad-properties`) even though there is ample horizontal room for a permanent sidebar. The hamburger on a 768px canvas with 600px of empty whitespace next to it is the clearest signal that the breakpoint is wrong.
- The "LOCAL ENVIRONMENT" status strip eats 16–20px at the top, pushing all chrome into a tight band.

**Recommendations**

1. **Add a bottom tab bar on iPhone for the 5 daily destinations**: Dashboard, Properties, Contacts, Payments, More. "More" opens a sheet containing Contracts, Expenses, Documents, Photos, Reports, Settings, Sign Out, team switcher. Rationale: 80% of nav traffic is to those 5; bottom-tab is the iOS/Android standard; restores thumb reachability. The hamburger stays for tablet/landscape edge cases or is removed entirely on phones.
2. **Move the hamburger into a proper top header bar** (not floating), aligned to the right (or eliminate it once the bottom tab bar exists). Header height ~56px, sticky, contains: page title (truncating, single line), and a right-side icon cluster for page-specific actions.
3. **Drop the `lg` breakpoint for the persistent sidebar to `md` (768px)** so iPad portrait gets the desktop sidebar treatment — the current behavior (`16-ipad-dashboard`, `17-ipad-properties`) wastes the iPad's real estate. Tailwind's `md:` is the right knob; `useIsMobile` (currently `max-width: 639px`) should be the canonical "phone" check.
4. **Respect safe-area-insets** (`env(safe-area-inset-top/bottom)`) for the new top bar and bottom tab bar so they don't collide with the notch or the home indicator.

### 2. Page Header & App Chrome

**Issues**

- The inline `flex justify-between items-center` + `flex-wrap` pattern (used in `PropertyListPage.tsx` 129–160 and repeated across every list page) puts a 2-line title + subtitle on the left and a wrapping action cluster on the right. On 390px this collapses the actions *under* themselves (`04-properties`: refresh on row 1, CSV on row 2, Add Property on row 3, all stacked beside a wrapped title), creating a visually broken jigsaw.
- The page-level icon next to the title (house, contacts, contract) consumes 32–40px next to a long title — on `09-documents` "Document Library" wraps to two lines pushing the refresh button to a third row.
- "Add Property", "Add Contract", "Add Expense", "Register Payment", "Schedule Payment" buttons all use a 2-line `+ Add Foo` layout. On `07-payments` the two stacked buttons consume ~25% of the visible viewport before any content.

**Recommendations**

Define a **single mobile page-header component** with three slots and these rules:

- Slot A (title) — 1 line, truncates, no icon on mobile (icons in section labels are decorative noise at this width).
- Slot B (overflow menu, right) — a single `⋯` button opens an action sheet for: Refresh, Export CSV, Export PDF, Import, anything secondary.
- Slot C (primary action) — moves to a **floating action button (FAB)** in the bottom-right above the tab bar for create flows (Add Property, Add Contract, Add Expense, Register Payment). One CTA per page. The current "Schedule Payment" + "Register Payment" pair on `07-payments` becomes one FAB that opens a small menu.
- Subtitle ("Manage your rental properties and units") — **remove from mobile**. It is value-free chrome that pushes content below the fold on every page.

Per-page specifics:

- `04-properties`: title only + FAB (+) for Add Property; CSV/Refresh in overflow.
- `05-contacts`: title only + FAB; CSV/Import/Refresh in overflow.
- `06-contracts`: title only + FAB; Refresh/CSV in overflow.
- `07-payments`: title only + FAB. FAB opens a small action menu: "Register Payment" (primary) / "Schedule Payment" (secondary).
- `08-expenses`: title only + FAB; Refresh/CSV in overflow.
- `09-documents`, `10-photos`: title only + FAB (upload); Refresh in overflow.

### 3. Dashboard Composition

**Issues**

- `03-dashboard` opens with "Pending Extensions" — a workflow inbox embedded into the dashboard hero. Two cards with a tiny `Contract #CON01KRTERBEY9MD30S03Z3SDWHZ9` SID line wrapping awkwardly, plus inline Activate/Decline buttons. This is a useful concept but should live in a notification surface, not the top of the dashboard.
- Below that: Portfolio Overview card with **date-range pickers and CSV/PDF buttons** before any number has been shown. The user has to scroll past the export tools to get to "Portfolio Value €9,671,330" on `03-dashboard`.
- Charts (`Portfolio Cash Flow` visible on `16-ipad-dashboard`) at 390px are useless — axis labels collide, hover is unavailable, the data is illegible.

**Recommendations**

Build a **mobile-first dashboard** with a strict above-the-fold contract: in the first viewport the user must see (a) one greeting line and (b) 2–3 large KPI tiles. Everything else is below.

Proposed phone composition, top to bottom:

1. **Hero KPIs** — 2-up grid: "Portfolio Value €9.67M" + "Occupancy 88.5%". Tap to drill in.
2. **Action Required** — collapsible row with badge counts: "9 Overdue Payments • 2 Pending Extensions • 3 Contracts Expiring". Tap = navigate to filtered list.
3. **This Month** — 1-up wide card: Cash Flow with delta, tap to expand.
4. **Recent Activity** — last 5 events (payments registered, contracts signed, expenses added).
5. **Reports** — single tile/link "See full reports →" that routes to a Reports tab where the date-range pickers, CSV/PDF exports, and all charts live. This is where iPad-dense view also belongs.

Critically: **move date-range picker, CSV, PDF, and charts off the dashboard**. They belong in `/reports`.

### 4. List Screens (Properties, Contacts, Contracts, Payments, Expenses, Documents, Photos)

**Properties** (`04-properties`) — Card layout is fine. Filters card (Search + Category chips + Status chips) currently consumes the entire first viewport before a single property is visible. See §8 for filter recommendations.

**Contacts** (`05-contacts`, `13-contact-detail`) — Card layout works. Two minor issues: (a) the email field truncates with ellipsis but the phone number wraps next to it — single-line per field on phone, label-less; (b) the "overdue" badge `€2,613.00 • 3 payments overdue` is the most important signal on the card but visually it's the *third* row. Promote to a right-side colored bar or move directly under the name.

**Contracts** (`06-contracts`) — Card layout fine. The SID `#CON01KRTERBEZ336M1A39Z8180FSZ` displayed in full on the card is noise — show last 6 chars on mobile, full on detail view.

**Payments** (`07-payments`) — Above-the-fold is currently the summary cards which is good. The list below (not in screenshot but present) breaks at mobile per the prompt. Recommended **card row** for each payment:

```
[Avatar/icon]  Tenant Name              €1,200.00
               Property short label     Due in 3 days
               [Status pill]            [⋯ overflow]
```

Where status pill is colored (red overdue / amber due-soon / green paid) and the overflow menu offers "Mark paid", "Send reminder", "View contract". The whole card is tappable to the payment detail.

**Expenses** (`08-expenses`) — Header is the worst offender: title, subtitle, refresh, CSV, and the 2-line "Add Expense" button all jam into the top 200px. Then Total Expenses → Top Categories → 6-month chart → Filters card → list. **The chart on this page actively harms mobile** because it's the same size as the KPI cards but conveys far less at 390px. Move the chart behind a tap (`See trend →`).

**Documents** (`09-documents`) — This is the only place where a **table layout was retained** ("DOCUMENT", "TYPE" column headers) and it shows: the second column is clipped, the right edge "EXPENS..." is cut off. Convert to card list (file icon + filename + sub-line "Vendor invoice • property-tax-invoice-template.pdf" + type pill + date).

**Photos** (`10-photos`) — Grid is genuinely good at 2-up. Small note: the size text "122.3 KB" wraps under the date in an odd `PROPERTY 122.3 KB` two-column layout. Hide file size on mobile (it's rarely actionable), keep the colored type tag and date.

### 5. Detail Screens

**Property Detail** (`12-property-detail`) — Currently just a stacked label-value pair list with icons. No section grouping, no hierarchy, no actions visible above the fold, no photos as hero. The Google Maps embed is broken (`Oops! Something went wrong`) but even if it loaded it shouldn't be on the detail page hero.

**Recommendation: tabbed detail layout** with a sticky property header:

```
┌─────────────────────────────────┐
│ ← Estrada das Quintas 130    ⋯  │  ← sticky bar with back + overflow
│ [Vacant pill]   €1,513 / mo      │
├─────────────────────────────────┤
│ [Photo hero 16:9, swipeable]     │
├─────────────────────────────────┤
│ Overview │ Financials │ Tenants │ Documents │ Photos │ Map │
└─────────────────────────────────┘
```

- Sticky header: back button, address, status pill, monthly rent. Overflow contains Edit, Archive, Share.
- Tabs scroll horizontally (segmented control style). Default tab "Overview" contains: address card, specs grid (2-up: Area, Type, Year, Beds/Baths), key dates, primary contact.
- Map gets its own tab so its load failure doesn't ruin the page.
- Bottom of every tab: contextual action — e.g., on Tenants tab, "Add tenant" button; on Documents, "Upload".

### 6. Forms

**Add Property** (`15-property-new-form`) — Single column, good. Sections (Address / Specifications / ...) work. A few specifics:

- The "selectors.selectCountry" placeholder is an unresolved i18n key — fix.
- The form is long. Adopt a **mobile stepper** for forms with 3+ logical sections: Step 1 Address → Step 2 Specs → Step 3 Financial → Step 4 Photos → Review. Progress dots at top, "Back / Continue" sticky at bottom. This avoids the "scroll-fatigue" problem and lets the user save partial state.
- Sticky bottom action bar with "Save" (and "Save & Add Another") — never bury the submit button at the end of a long scroll.
- Use the system phone/email/number keyboards consistently (`inputMode="numeric"`, `inputMode="email"`).
- Country picker should be a bottom sheet with search, not a native `<select>` — there are ~250 countries and the native picker is a usability cliff.

### 7. Modals & Sheets

The single biggest anti-pattern: **contact and contract detail open as desktop-style modals over the list** (`13-contact-detail` is literally just `05-contacts` again — that's the bug: on a phone the modal is so small or off-canvas that the list shows through). On mobile, a "detail" is a destination, not a modal.

**Recommendations**

1. On mobile, route to a real `/contacts/:id` page instead of opening a modal. This restores back-button behavior, deep linking, and proper share/recall semantics.
2. Where a modal is genuinely the right interaction (Edit dialog, confirmation, picker), use a **bottom sheet** that snaps to 50%/90%/full, with a drag handle and a header bar containing Cancel / Title / Save. Never a centered card overlay on mobile.
3. Action sheets (replacing dropdown menus / overflow menus) should slide from the bottom, list items full-width with 48px+ rows.

### 8. Filters / Sort / Search

**Issues**

- The Filters card on `04-properties` consumes the entire viewport before any property loads: search field, Category chips (6), Status chips (8). On `06-contracts` and `08-expenses` it's similar.
- On `05-contacts` the inline row "🔍 [↕ Date Created ▾] [↓] [▽ Filters]" is on a single row but the rightmost Filters button is clipped at the edge of the screen — actively unusable.

**Recommendations**

1. Replace the always-expanded Filters card with a **single sticky filter bar** below the header: `[🔍 search] [Filters · 2]` (showing active count). Sticky so it remains accessible while scrolling.
2. Tap the Filters pill → opens a **bottom sheet** containing the full filter UI (category chips, status chips, date ranges, sort). Sheet has Apply (sticky bottom) and Reset (top-right).
3. Surface active filters as a horizontally-scrolling chip row immediately below the sticky bar (`Vacant ×` `Residential ×`) so the user always sees what's narrowing the list. Tap a chip to remove that filter.
4. Search should be the first focus when the sheet opens — type-ahead the moment a user lands.
5. The free "30 properties" / "55 contacts" / "71 contracts" count line should live inside the sticky filter bar (right-aligned) — currently it's a fourth row of chrome eating space.

### 9. Touch Targets & Microinteractions

- The hamburger button is 40x40px — meets the 44pt Apple minimum but only just; bump to 44x44.
- The CSV / Refresh icon buttons in headers are visually 32x32 with no obvious tap target — bump touch surface to 44 with invisible padding.
- The chip filters (Residential / Commercial / etc.) on `04-properties` are around 32–36px tall. They feel like buttons but they're hard to hit consecutively. 40px minimum chip height.
- The Activate / Decline buttons on `03-dashboard` Pending Extensions are tightly stacked next to a wrapping content block — easy mis-tap. Stack them vertically or move them into a swipe action.
- **Add swipe-to-action** on Payments cards (left swipe reveals "Mark Paid"; right swipe reveals "Send Reminder") and Expenses cards (swipe to archive). This is the highest-value microinteraction for daily use.
- **Pull-to-refresh** is expected on every list screen. The standalone refresh icon button (top-right of every header) should be removed once pull-to-refresh is in.
- Add **haptic feedback** on primary actions (Mark Paid, Activate Extension, Save) — `navigator.vibrate(10)` is fine.
- Loading states: skeleton card rows that match the final card shape, not centered spinners.
- Empty states: each list page needs a custom empty state with an illustration + primary CTA (e.g., Contracts empty: "No contracts yet · Add Contract"). Currently most empties are blank.
- Errors: the Google Maps "Oops! Something went wrong" generic message on `12-property-detail` is a terrible mobile failure mode — replace with a static map fallback (just an address pill with "Open in Maps →").

### 10. iPad-Specific (768–1023px)

- `16-ipad-dashboard` and `17-ipad-properties` show iPad portrait stuck in mobile mode. This is the single highest-leverage fix: move the persistent sidebar breakpoint from `lg` (1024) to `md` (768).
- Once sidebar is present at iPad portrait: dashboard KPIs become 2-up or 3-up grid (already does, see `16-ipad-dashboard`), list cards go 2-column (already does for properties, see `17-ipad-properties`), forms go 2-column where logical (address fields paired: city + postal code on one row).
- The PageHeader at iPad width can keep the title+subtitle and a row of action buttons — *but* still use overflow for tertiary actions to keep the row clean.
- Detail screens at iPad portrait should use a 2-pane split: left rail of tabs / right pane of content, or list-detail (list on left, detail on right) on Properties / Contacts / Contracts.

## Priority Punch List

### P0 — must fix (blocks daily mobile use)

- **Add a bottom tab bar on iPhone** with 5 destinations (Dashboard, Properties, Contacts, Payments, More). All screens. **L**
- **Replace inline page headers with a single sticky `MobilePageHeader` component**; remove subtitles on phone; collapse secondary actions into a `⋯` overflow menu. All list pages (`04`, `05`, `06`, `07`, `08`, `09`, `10`). **M**
- **Promote primary create actions to a FAB** (Add Property, Add Contract, Add Expense, Register Payment, Upload). `04`, `05`, `06`, `07`, `08`, `09`, `10`. **M**
- **Lower sidebar breakpoint from `lg` to `md`** so iPad portrait gets the desktop sidebar. `16`, `17`. **S**
- **Route contact/contract details to real pages on mobile**, not modals. `13`. **M**
- **Convert the Documents table to a card list** — current right edge is clipped. `09`. **S**
- **Fix `selectors.selectCountry` i18n key** in Add Property form. `15`. **S**

### P1 — significantly improves experience

- **Dashboard rebuild** for phone: hero KPIs first, Action Required band, recent activity; defer charts & exports to a `/reports` route. `03`. **L**
- **Sticky filter pill + bottom sheet** for all list filters; replace always-open Filters cards. `04`, `06`, `08`. **M**
- **Tabbed property detail** (Overview/Financials/Tenants/Documents/Photos/Map) with sticky header and photo hero. `12`. **L**
- **Card row layout for Payments list** with status pill, due date, swipe-to-action. `07`. **M**
- **Pull-to-refresh** on every list page; remove the standalone Refresh icon buttons from headers. All list pages. **S**
- **Swipe actions**: Payments (Mark Paid, Remind), Expenses (Archive), Documents (Delete). **M**
- **Bottom sheet for country picker** in Add Property form. `15`. **S**
- **Form stepper** for Add Property and Add Contract; sticky save bar. `15`. **M**
- **Bump tap targets** to 44pt minimum (hamburger, chip filters, icon buttons). All screens. **S**

### P2 — polish

- **Custom empty states** with illustration + CTA per list page. **M**
- **Skeleton loaders** that match card shape. **S**
- **Haptic feedback** on primary actions and swipe completions. **S**
- **Truncate Sid identifiers** on cards (`#CON…180FSZ`) — show full on detail only. `06`. **S**
- **Hide low-value metadata on mobile** (file size on photos, "X months ago" timestamps where redundant). `05`, `10`. **S**
- **Static map fallback** for property detail when Google Maps fails. `12`. **S**
- **Respect safe-area-insets** for new top/bottom bars; especially for the home indicator on devices without a physical button. **S**
- **Promote overdue badge** on contact cards to first-row, right-aligned colored bar. `05`. **S**
- **Action sheet styling** for all dropdown menus (anchor from bottom, full-width rows). **M**
