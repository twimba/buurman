# Buurman Mobile Experience — Master Plan

> Source material: four expert reports under `.mobile-review/reports/` plus 17 captured screenshots
> across iPhone 13 (390×844) and iPad portrait (768×1024) in `.mobile-review/screenshots/`.
> This document is the synthesis: prioritized, exhaustive, and ready to be sliced into tickets.

---

## 0. Executive Summary

**Verdict: mobile readiness ~2 / 5 — "responsive, not mobile."** Buurman reflows below 1024 px without
horizontal overflow and the existing card lists (Properties, Contacts, Contracts) survive at 390 px.
But every surface is a desktop screen squeezed: page chrome collides with content, primary navigation
is a 2-tap drawer round-trip, the dashboard buries actionable signal under analyst-style charts and
date-range controls, several modals fall back to centered desktop dialogs, raw `<table>` elements break
on mobile (e.g. Documents list right column is clipped in screenshot `09`), and the iPad portrait
viewport — 1/4 of likely real-world usage — gets the *iPhone* layout because the sidebar break is
pinned to `lg` (1024 px). The bones are sound (Tailwind v4, Recharts with `ResponsiveContainer`,
React 19, Vite 7, multi-locale support). The chrome, navigation, data density, and a few foundational
CSS variables are the work.

**Three highest-leverage moves**

1. **App-shell refactor**: introduce a sticky `<PageHeader>` (the hamburger and primary CTA live
   inside it, killing the floating-button overlap visible in screenshots `04`, `05`, `07`, `12`),
   move the sidebar break from `lg` to `md` so iPad portrait gets a 64 px icon rail, and add an
   iPhone-only bottom tab bar for the 4 daily destinations + More.
2. **Foundation CSS**: viewport meta with `viewport-fit=cover`, `--safe-*` tokens, `100vh → 100dvh`,
   16 px minimum input font-size, `touch-action: manipulation`, AA-compliant banner contrast. None
   of these are visible features but they cost ~1 day and unlock the rest.
3. **Three reusable primitives** that erase per-page boilerplate across 25–30 pages:
   `<PageHeader>`, `<ResponsiveTable + DataList>`, and `<Sheet>` (mobile bottom sheet / desktop dialog
   via Vaul). Each one replaces a recurring pattern; together they turn the migration of remaining
   screens into a mechanical pass rather than per-page design.

Total estimated work: **4–6 weeks** at one engineer + one designer, or **3–4 weeks** with a pair.

### Non-negotiable: desktop must not regress

Every change in this plan is gated on `<md` or `<lg` or implemented as a refactor with pixel parity
at `md+`. Five judgment calls would change desktop behavior if implemented naively; each has an
explicit adjustment in this doc:

1. **`<FilterSheet>`** — at `md+` renders inline (preserves today's always-visible filter card).
   Only at `<md` does it collapse to a trigger + bottom sheet. See §3.4.
2. **Sidebar rail at `md`** — affects users with browser windows 768–1023 px (small laptops,
   half-screen). UX is objectively better but it is a behavior change. Ship behind a
   `MOBILE_SIDEBAR_RAIL` feature flag with metrics review before defaulting on. See §2.5.
3. **Contact / Contract detail routing** — phone navigates to a page; **desktop keeps the modal**
   (or evolves to a list-detail split pane at `xl+`). Conditional in the click handler. See §4.6.
4. **`<PageHeader>` consolidation** — at `md+` must render pixel-equivalent to the current header
   (icon + title + subtitle + inline actions). Refactor, not redesign. Visual-diff CI per batch
   of 5 pages catches regressions. See §3.1 and §9.3.
5. **Form stepper** — only applied at `<md`. Desktop keeps the single-scroll long-form layout.
   Shared form state. See §4.13.

Everything else — dashboard mobile redesign, HeroKpiCard, KpiRail, currency abbreviation,
Recharts simplifications, PropertyCard `grid-cols-3 → grid-cols-2 xs:grid-cols-3`, hit-44 utility,
swipe actions, selection mode, mobile detail tabs — is conditionally rendered at the breakpoint
and *cannot* reach desktop without an explicit code change.

---

## 1. Scope & Terminology

- **Phone**: viewport `< 768 px` (Tailwind `< md`). Primary target: iPhone 13/14 at 390 × 844.
  Edge target: iPhone SE at 375 × 667 (and Split View ≈ 320 × 1024 on iPad).
- **Tablet**: 768 px ≤ viewport `< 1024 px` (Tailwind `md → lg`). Primary target: iPad portrait
  at 768 × 1024.
- **Desktop**: `≥ 1024 px`. Untouched by this plan except by side effects.
- **Sidebar rail**: collapsed icon-only sidebar (~64 px). New variant for `md → lg`.
- **Sidebar drawer**: off-canvas sidebar (`< md`). Existing pattern, kept and polished.
- **Sticky page header**: a new `<PageHeader>` that pins under the env banner; carries the page
  title, primary CTA, overflow menu, and (on phone) the hamburger.
- **Bottom tab bar**: a phone-only fixed nav at the bottom edge with the 5 daily routes.
- **Sheet**: bottom-anchored slide-up modal on phone, normal centered dialog on tablet/desktop.

Throughout this doc, when we say "mobile" we mean phone unless explicitly tagged tablet.

---

## 2. Foundations (Phase 1)

These are the deepest, highest-leverage changes. **Land them first** — every subsequent ticket
depends on them.

### 2.1 Viewport meta + PWA basics — `frontend/app/index.html`

```html
<meta name="viewport"
      content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content, maximum-scale=5" />
<meta name="theme-color" content="#fafaf9" media="(prefers-color-scheme: light)" />
<meta name="theme-color" content="#0c0a09" media="(prefers-color-scheme: dark)" />
<meta name="color-scheme" content="light dark" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<meta name="apple-mobile-web-app-title" content="Buurman" />
<meta name="format-detection" content="telephone=no" />
<link rel="manifest" href="/manifest.webmanifest" />
```

- **Do not** disable scaling (`maximum-scale=1` / `user-scalable=no`) — WCAG 1.4.4 violation.
- `viewport-fit=cover` is the prerequisite for `env(safe-area-inset-*)` returning non-zero values.
- `<html lang>` must be set dynamically from i18next (`languageChanged` listener).
- Ship a minimal `manifest.webmanifest` so "Add to Home Screen" produces a real icon and
  `display: standalone`. Use existing `apple-touch-icon` plus add 192 px / 512 px / maskable icons.

### 2.2 Design tokens — `frontend/packages/ui/src/styles/theme.css`

Add to the `@theme` block:

```css
@theme {
  --breakpoint-xs: 22.5rem;        /* 360px — iPhone SE-class */

  --safe-top:    env(safe-area-inset-top,    0px);
  --safe-bottom: env(safe-area-inset-bottom, 0px);
  --safe-left:   env(safe-area-inset-left,   0px);
  --safe-right:  env(safe-area-inset-right,  0px);

  --header-h:     3.5rem;          /* 56px sticky page header on mobile */
  --bottomnav-h:  3.75rem;         /* 60px iPhone bottom tab bar */
  --size-touch:   2.75rem;         /* 44px Apple HIG */
  --kbd-inset:    0px;             /* set by visualViewport listener */
}

@layer utilities {
  .min-h-touch  { min-height: var(--size-touch); }
  .min-w-touch  { min-width:  var(--size-touch); }
  .pt-safe      { padding-top:    var(--safe-top); }
  .pb-safe      { padding-bottom: var(--safe-bottom); }
  .hit-44       { position: relative; }
  .hit-44::after {
    content: '';
    position: absolute;
    inset: 50% auto auto 50%;
    width:  max(100%, var(--size-touch));
    height: max(100%, var(--size-touch));
    transform: translate(-50%, -50%);
  }
}

button, [role='button'], a, summary, label {
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}

html, body { overscroll-behavior-y: none; }

@media (max-width: 767px) {
  input:not([type='checkbox']):not([type='radio']),
  select,
  textarea {
    font-size: 16px;   /* kill iOS focus-zoom */
  }
}
```

Notes:

- Tailwind v4 does **not** ship arbitrary `min-h-touch` modifiers without theme registration —
  use the `@layer utilities` rule above, or `min-h-[var(--size-touch)]` inline. Pick one and stick
  to it.
- Banner contrast bug: `LOCAL ENVIRONMENT` is currently white on `#d97706` ≈ 3.0:1. Use
  `bg-accent-700` (`#92400e` or equivalent) for ~5.9:1 — passes AA.

### 2.3 Breakpoint strategy

| Token | Width  | Devices                          | Design intent                                                       |
|-------|--------|----------------------------------|---------------------------------------------------------------------|
| –     | <360   | iPhone SE 1st gen                | Single column, no decorative icons, abbreviated labels              |
| `xs`  | ≥360   | iPhone 12/13/14, Pixel 5         | Default phone. Single-column lists as cards. 2-col KPI grids.       |
| `sm`  | ≥640   | Large phone landscape, phablets  | 2-col card grids, inline filter chips                               |
| `md`  | ≥768   | **iPad portrait**                | **Sidebar rail (64 px). 2-col forms. Inline page-header actions.**  |
| `lg`  | ≥1024  | iPad landscape, laptops          | Expanded sidebar. 3-col card grids. Tables.                         |
| `xl`  | ≥1280  | Desktop                          | Multi-pane detail views, side-by-side dashboard charts              |
| `2xl` | ≥1536  | Wide desktop                     | Max-width 1536 content, no other change                             |

The decision that matters most: **sidebar rail at `md`, expanded at `lg`, drawer below `md`.**
This single change reclaims the iPad portrait experience (screenshots `16`, `17` go from "phone in
a big box" to "small laptop").

### 2.4 App shell refactor — `frontend/app/src/components/Layout.tsx`

Replace fixed `100vh` with `100dvh`, mount the new `<MobileTopBar>` / `<PageHeader>` slot, reserve
bottom padding for the tab bar, expose `MobileNavContext` so any sticky header can open the drawer.

```tsx
export const Layout = () => {
  const [collapsed, setCollapsed] = useSidebarCollapsed();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <MobileNavContext.Provider value={{ open: () => setMobileNavOpen(true) }}>
      <div className="min-h-dvh bg-surface-page flex">
        <SkipLink />
        <Sidebar
          collapsed={collapsed}
          onToggleCollapse={() => setCollapsed(!collapsed)}
          mobileOpen={mobileNavOpen}
          onMobileClose={() => setMobileNavOpen(false)}
        />
        <div
          className={clsx(
            'flex-1 min-w-0 flex flex-col',
            'transition-[margin] duration-300',
            collapsed ? 'md:ml-16 lg:ml-20' : 'md:ml-16 lg:ml-64',
          )}
        >
          <AuthenticatedBroadcastBanner />
          <main
            id="main-content"
            className={clsx(
              'flex-1 overflow-x-hidden',
              'pb-[calc(var(--bottomnav-h)+var(--safe-bottom))] md:pb-0',
            )}
          >
            <div className="mx-auto w-full max-w-screen-2xl px-4 py-4 md:px-6 md:py-6 lg:px-8 lg:py-8">
              <Outlet />
            </div>
          </main>
          <BottomTabBar className="md:hidden" />
        </div>
      </div>
    </MobileNavContext.Provider>
  );
};
```

### 2.5 Sidebar refactor — `frontend/app/src/components/Sidebar.tsx`

> **Behind a feature flag.** Moving the sidebar rail to `md` affects users browsing at
> 768–1023 px (small laptops, half-screen windows on desktop). UX is objectively better — a 64 px
> icon rail beats hamburger + drawer round-trips — but it is a behavior change. Ship behind a
> `MOBILE_SIDEBAR_RAIL` flag (env var or PostHog), A/B for 1–2 weeks, watch nav-engagement and
> task-completion metrics, then default on. At `lg+` the experience is identical regardless.

Three variants, one component:

```tsx
// Mobile drawer (<md)
className={clsx(
  'md:hidden fixed inset-y-0 left-0 z-40',
  'w-[min(18rem,85vw)]',
  'pt-[var(--safe-top)] pb-[var(--safe-bottom)] pl-[var(--safe-left)]',
  mobileOpen ? 'translate-x-0' : '-translate-x-full',
  'transition-transform duration-300 ease-out',
)}

// iPad rail (md → lg) — icon-only, 64px wide
className="hidden md:flex lg:hidden fixed inset-y-0 left-0 w-16 z-30 ...icon-only-variant"

// Desktop (lg+) — expanded or collapsed by user preference
className={clsx(
  'hidden lg:flex fixed inset-y-0 left-0 z-30',
  collapsed ? 'lg:w-20' : 'lg:w-64',
)}
```

Drop the free-floating hamburger button at `Sidebar.tsx:203`. Replace with a trigger
inside `<PageHeader>` (see §3.1). Add accessibility:

```tsx
<aside
  id="primary-sidebar"
  aria-label={t('a11y.primaryNav')}
  inert={!mobileOpen ? '' : undefined}   /* removed from tab order when closed */
  style={{ height: 'calc(100dvh - var(--env-banner-height, 0px))' }}
>
```

Add focus trap when drawer opens (Radix Dialog or `focus-trap-react`), restore focus on close,
ESC handler, swipe-to-close (touch start within 24 px of left edge, threshold 60 px + velocity).

### 2.6 Env banner — extend into the notch

```css
.env-banner {
  background: theme(colors.accent.700);  /* fix contrast */
  color: white;
  padding-top:   calc(0.5rem + var(--safe-top));
  padding-left:  calc(1rem  + var(--safe-left));
  padding-right: calc(1rem  + var(--safe-right));
}
```

The ResizeObserver that feeds `--env-banner-height` continues to work because it reads
`getBoundingClientRect().height`, which already includes safe-area padding.

### 2.7 Keyboard inset hook — new `frontend/app/src/hooks/useKeyboardInset.ts`

```ts
export function useKeyboardInset() {
  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) return;
    const apply = () => {
      const inset = window.innerHeight - vv.height - vv.offsetTop;
      document.documentElement.style.setProperty('--kbd-inset', `${Math.max(0, inset)}px`);
    };
    vv.addEventListener('resize', apply);
    vv.addEventListener('scroll', apply);
    apply();
    return () => {
      vv.removeEventListener('resize', apply);
      vv.removeEventListener('scroll', apply);
    };
  }, []);
}
```

Mount once at the app root. Sticky sheet footers consume the variable:
`bottom: calc(var(--kbd-inset, 0px) + var(--safe-bottom))`.

---

## 3. Design System Primitives (Phase 1 / 2)

These six new components are the leverage. Build them in `frontend/packages/ui/src/components/`
and they replace per-page boilerplate everywhere.

### 3.1 `<PageHeader>`

Replaces ~30 lines per page across ~25 list/detail pages.

```tsx
interface PageHeaderAction {
  label: string;
  icon?: LucideIcon;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'ghost';
  disabled?: boolean;
  showOn?: 'mobile' | 'desktop' | 'both';   // default 'both'
}

interface PageHeaderProps {
  title: string;
  subtitle?: string;             // shown md+, hidden on mobile
  icon?: LucideIcon;             // hidden xs:block, never on phone if it eats title space
  actions?: PageHeaderAction[];        // inline md+
  overflowActions?: PageHeaderAction[];// always in ⋯ menu
  primaryAction?: PageHeaderAction;    // single visible CTA on mobile
  backHref?: string;             // detail pages
  sticky?: boolean;              // default true on mobile
}
```

Behavior:
- Mobile (`<md`): hamburger (leading) → title (truncating) → primary action button → overflow `⋯`.
  Subtitle hidden. Page icon hidden below `xs`. Title `text-xl xs:text-2xl`.
- Tablet+ (`md+`): hamburger gone, sidebar visible. Title `md:text-3xl`. All actions inline.
- Sticky on mobile only, `top: var(--env-banner-height)`, `pt-[var(--safe-top)]`, `backdrop-blur`.
- Z-index 30 (below drawer + drawer scrim, above page content).

**Per-page migration table** (the right column lists the primary CTA that becomes the mobile FAB
equivalent — single visible action via PageHeader's `primaryAction`):

| Page                         | Primary action            | Overflow                              | Notes                       |
|------------------------------|---------------------------|---------------------------------------|-----------------------------|
| `/dashboard`                 | —                         | Refresh, View reports                 | Title only                  |
| `/properties`                | Add Property              | Refresh, Export CSV / XLSX / Sheets   |                             |
| `/properties/:id`            | Edit                      | Archive, Share, Documents             | Back arrow + truncating addr|
| `/contacts`                  | Add Contact               | Refresh, Import, Export CSV           |                             |
| `/contacts/:id`              | Edit                      | Archive, Share                        | Convert from modal to page  |
| `/contracts`                 | Add Contract              | Refresh, Export CSV                   |                             |
| `/contracts/:id`             | Edit / Sign / Adjust Rent | Terminate, Renew, Documents           |                             |
| `/payments`                  | Register Payment          | Schedule Payment, Refresh, Export CSV |                             |
| `/payments/:id`              | Mark Paid                 | Send Reminder, View Contract          |                             |
| `/expenses`                  | Add Expense               | Refresh, Export CSV, Bulk Categorize  |                             |
| `/documents`                 | Upload                    | Refresh, Select                       |                             |
| `/photos`                    | Upload                    | Refresh, Select                       |                             |
| `/reports`                   | —                         | Time range, Export PDF                | New route — see §4.1        |
| `/settings`                  | —                         | Sign Out                              |                             |
| `/properties/new`            | Save                      | Save & Add Another                    | Sticky bottom CTA, not header |

### 3.2 `<Sheet>` (Vaul-based responsive modal)

Phone bottom sheet, tablet/desktop centered dialog. Single API.

```tsx
import { Drawer } from 'vaul';

interface SheetProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;             // sticky action bar
  snapPoints?: number[];          // e.g. [0.4, 0.9]
  dismissible?: boolean;
}

export function Sheet(props: SheetProps) {
  const isDesktop = useMediaQuery('(min-width: 768px)');
  if (isDesktop) return <ModalWrapper {...mapToModalProps(props)} />;

  return (
    <Drawer.Root open={props.open} onOpenChange={(o) => !o && props.dismissible !== false && props.onClose()}
                 snapPoints={props.snapPoints} dismissible={props.dismissible !== false}>
      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 bg-surface-overlay z-50" />
        <Drawer.Content
          className="fixed inset-x-0 bottom-0 z-50 max-h-[92dvh] rounded-t-2xl
                     bg-surface-card border-t border-border-default flex flex-col outline-none"
          style={{ paddingBottom: 'calc(var(--safe-bottom) + var(--kbd-inset, 0px))' }}
        >
          <div aria-hidden className="mx-auto my-2 h-1.5 w-10 rounded-full bg-border-strong" />
          <Drawer.Title className="px-6 pt-2 text-lg font-semibold">{props.title}</Drawer.Title>
          {props.description && (
            <Drawer.Description className="px-6 text-sm text-text-secondary">
              {props.description}
            </Drawer.Description>
          )}
          <div className="flex-1 overflow-y-auto overscroll-contain px-6 py-4">
            {props.children}
          </div>
          {props.footer && (
            <div className="border-t border-border-default px-6 py-3">
              {props.footer}
            </div>
          )}
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
}
```

**Modals that should migrate to `<Sheet>`**:
- Filter sheet (Properties, Contacts, Contracts, Payments, Expenses, Documents, Photos)
- Payment registration / scheduling
- Expense add/edit (small enough — `snapPoints={[0.5, 0.9]}`)
- Document upload preview
- Contract wizard steps
- Onboarding wizard steps
- Country picker (new — replace native `<select>` for ~250 countries with searchable sheet)
- Time-range presets on the new Reports route

**Stay as centered dialog (`<ModalWrapper>`)**: confirmation dialogs, tiny pickers (< 2 fields).
Rule: `>= 2 inputs OR > 1 action -> Sheet`.

### 3.3 `<ResponsiveTable>` + `<DataList>`

Phone: card list. Tablet+: `<table>` with horizontal scroll + sticky first column when needed.

```tsx
interface Column<T> {
  key: string;
  header: ReactNode;
  cell: (row: T) => ReactNode;
  align?: 'left' | 'right' | 'center';
  hideBelow?: 'sm' | 'md' | 'lg';   // progressive disclosure on tablet
  sticky?: boolean;                  // first column on tablet h-scroll
}

interface ResponsiveTableProps<T> {
  rows: T[];
  columns: Column<T>[];
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
  mobileRow: (row: T) => ReactNode;   // card body below md
  emptyState?: ReactNode;
  stickyHeader?: boolean;
}
```

Implementation skeleton — phone branch above `md`, table below:

```tsx
{/* Phone: cards */}
<div className="md:hidden space-y-3">
  {rows.map(r => (
    <button
      key={rowKey(r)}
      onClick={() => onRowClick?.(r)}
      className="block w-full text-left bg-surface-card rounded-lg border border-border-default
                 p-4 min-h-touch hover:border-primary-300 transition-colors"
    >
      {mobileRow(r)}
    </button>
  ))}
</div>

{/* Tablet/desktop: table with overflow + sticky first column */}
<div className="hidden md:block overflow-x-auto rounded-lg border border-border-default">
  <table className="min-w-full">
    {/* … sticky first cell when columns[0].sticky */}
  </table>
</div>
```

**Pages to migrate** (22 files identified; group by similarity to share `mobileRow` templates):

```
pages/PaymentsPage.tsx                       — high priority, hot path
pages/ExpensesPage.tsx
pages/TransactionHistoryPage.tsx             — group by day on phone
pages/DocumentsPage.tsx                      — see §5.9
pages/AuditLogPage.tsx
pages/PaymentDetailPage.tsx                  — inline table
pages/FinancialReportsPage.tsx               — multiple tables, tab + collapse
pages/admin/AdminNotificationsPage.tsx       — low priority
components/dashboard/PropertyPerformanceTable.tsx
components/dashboard/UpcomingRenewalsPanel.tsx
components/contacts/ContactFinancialsTab.tsx
components/contacts/ContactContractsTable.tsx
components/contacts/ContactAddressList.tsx
components/contacts/ImportWizard.tsx          — keep h-scroll table only
components/contracts/ContractPaymentsTab.tsx
components/properties/PropertyExpensesTab.tsx
components/properties/PropertyContractsTab.tsx
components/properties/DocumentList.tsx
components/properties/financials/PropertyFinancialsTab.tsx
components/rentIncreases/{ReviewStep,PropertyAdjustmentStep,ConfirmationStep}.tsx
components/rentRegulations/RuleHistoryTable.tsx
components/settings/{PaymentHistorySection,UserPreferencesSection}.tsx
components/common/BulkDataGrid.tsx           — power-user: h-scroll only, don't card-ify
```

### 3.4 `<FilterSheet>`

Replaces every always-expanded Filters card on a list page (Properties, Contracts, Expenses,
Documents…) **on phone only**. At `md+` the component renders the filter content **inline**,
preserving today's always-visible filter card so desktop and tablet users keep their current
zero-click filtering. At `<md` the filter content moves into a bottom sheet behind a
`[⛕ Filters (n)]` trigger button.

```tsx
interface FilterSheetProps {
  triggerLabel?: string;        // default "Filters"
  activeCount: number;          // shown as badge on phone trigger
  onClear: () => void;
  children: ReactNode;          // filter form content (same on both modes)
  /**
   * Where to switch from inline (always-visible) to triggered-sheet.
   * Default 'md' — inline at md+, sheet below md. Set 'lg' if a page has
   * particularly heavy filters and tablet should also collapse.
   */
  collapseBelow?: 'sm' | 'md' | 'lg';
}
```

Implementation sketch:

```tsx
const collapseBelow = props.collapseBelow ?? 'md';
return (
  <>
    {/* Inline mode — md+ (or whatever collapseBelow says) */}
    <div className={clsx('hidden', collapseBelowToInlineVisible[collapseBelow])}>
      <FilterCard onClear={props.onClear}>{props.children}</FilterCard>
    </div>
    {/* Trigger + sheet mode — below collapseBelow */}
    <div className={collapseBelowToTriggerVisible[collapseBelow]}>
      <FilterTrigger activeCount={props.activeCount} onClick={open} />
      <Sheet open={isOpen} onClose={close} title="Filters" footer={<ApplyButton />}>
        {props.children}
      </Sheet>
    </div>
  </>
);
```

Net effect on desktop: zero. Today's `PropertyListPage` filter card renders identically because the
inline branch wraps the exact same JSX it currently uses.

```tsx
<div className="flex items-center gap-2 mb-4">
  <SearchInput value={query} onChange={setQuery} className="flex-1" />
  <FilterSheet activeCount={activeFilterCount} onClear={resetFilters}>
    <FilterGroup label="Category">
      <ChipGroup options={CATEGORY_OPTIONS} value={category} onChange={setCategory} />
    </FilterGroup>
    <FilterGroup label="Status">
      <ChipGroup options={STATUS_OPTIONS} value={status} onChange={setStatus} />
    </FilterGroup>
    <FilterGroup label="Sort">
      <RadioGroup options={SORT_OPTIONS} value={sort} onChange={setSort} />
    </FilterGroup>
  </FilterSheet>
</div>

{activeFilterCount > 0 && (
  <ActiveFilterChips filters={appliedFilters} onRemove={removeFilter} className="mb-3" />
)}
```

The sheet has Apply (sticky bottom) and Clear All (top-right). Sort moves *into* the filter sheet —
kill the standalone `Date Created ▾` dropdown that's chewing the top row on `05-contacts`.

### 3.5 `<HeroKpiCard>` + `<KpiRail>`

Two-piece KPI primitive replacing the current 6-card grid. `HeroKpiCard` is full-width with
sparkline + delta; `KpiRail` is horizontal snap-scroll of compact tiles.

```tsx
<HeroKpiCard
  label="Monthly Cash Flow"
  value={-25828}
  format="currency-compact"            // → −€25,828
  delta={{ pct: -12.4, vs: 'last month' }}
  sparkline={cashFlowSeries}
  tone="negative"
/>

<KpiRail className="md:hidden">
  <KpiTile label="Portfolio Value" value={9671330} format="currency-abbrev" delta={2.1} unit="%" />
  <KpiTile label="Total Equity"    value={6740405} format="currency-abbrev" delta={124000} unit="€" />
  <KpiTile label="Occupancy"       value={88.5}    format="percent"          delta={-1.2} unit="pp" />
  <KpiTile label="Cap Rate"        value={-1.2}    format="percent"          delta={-0.3} unit="pp" />
  <KpiTile label="Cash-on-Cash"    value={-6.4}    format="percent"          delta={-1.1} unit="pp" />
</KpiRail>

{/* Desktop: keep the existing 6-up grid */}
<div className="hidden md:grid md:grid-cols-3 lg:grid-cols-6 gap-4">
  {/* … existing KpiCard rendering */}
</div>
```

Rail container: `flex overflow-x-auto snap-x snap-mandatory gap-3 -mx-4 px-4 scroll-pl-4`.
Pagination dots below.

### 3.6 `<ChartCard>` + fullscreen chart sheet

```tsx
<ChartCard title="Portfolio Cash Flow" expandable>
  <PortfolioCashFlowChart compact={isMobile} />
</ChartCard>
```

`expandable` adds a `⤢` button that opens the chart in a fullscreen `<Sheet>` with a "rotate device
for better view" hint banner (don't force orientation lock), pinch-zoom via `react-zoom-pan-pinch`
over the SVG, and a sticky bottom action bar with Series ▾ / Range ▾ / Share / Close.

### 3.7 `<SwipeAction>` + `<SelectionBar>`

`SwipeAction` wraps a row and exposes `left` / `right` action slots that reveal on swipe (use
`@use-gesture/react`). Used by Payments rows (Mark Paid / Remind), Expenses rows (Archive),
Documents rows (Delete). Always pair with explicit buttons — swipe is bonus, not the only way.

`SelectionBar` is the sticky bottom action bar shown in selection mode (Photos, Documents,
Payments, Expenses): `Move · Delete · Download · Share`. Toggled via a "Select" button in the
overflow menu and/or long-press a row. See §6.4.

---

## 4. Per-Screen Plans

### 4.1 Dashboard — `frontend/app/src/components/DashboardPage.tsx`

The biggest single redesign. Today (screenshot `03`) the user lands on a workflow inbox
("Pending Extensions") followed by date-range pickers and CSV/PDF exports, then 6 KPI cards stacked
vertically, then four charts, then a 30-row table. **Triage, not analysis** is the mobile job.

**Mobile composition** (in order, above-the-fold first):

```
┌──────────────────────────────────┐
│ ☰  Dashboard          🔔3   ⋯    │  Sticky PageHeader, 56px
├──────────────────────────────────┤
│ ⚠ Action required                │  Compact alert strip, hidden if count=0
│  9 overdue · 2 extensions    →  │  taps scroll lists into view / route
├──────────────────────────────────┤
│ HeroKpiCard: Monthly Cash Flow   │  Hero north-star number, with sparkline + delta
│  −€25,828   ▼ −12.4% MoM         │
│  ▁▂▃▂▁▂▂▃▄▅▄▅                    │
├──────────────────────────────────┤
│ KpiRail (horizontal snap)        │  140px, swipe through 5 KPIs
│ [Value][Equity][Occ][Cap][CoC]   │
├──────────────────────────────────┤
│ Pending actions          See all │
│ ┌──────────────────────────────┐ │  cards swipe-left to reveal Activate/Decline
│ │ Ext #2 · €502→€527 · 17/02/31│ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
  ↓ user has chosen to scroll → wants depth
[ Unpaid payments (top 5 + view all) ]
[ Top 5 properties by NOI / Bottom 5 by CF ]
[ Insights: tap mini-spark to open fullscreen chart ]
[ Property status donut + counts ]
```

**Per-section recommendations**:

- **Pending Extensions card**: keep top-of-page **only** if count > 0. Compact horizontal row:
  `Ext #2 · €502→€527 · 17/02/31`. Activate / Decline as `<SwipeAction>` + explicit `⋯` for a11y.
  Limit to 2 shown + "View all (n)".
- **Portfolio Overview chrome (6M/1Y/.../All + date pickers + CSV/PDF)**: **remove entirely from
  mobile**. Route to a new `/reports` destination that hosts all charts + date controls + exports.
- **6 KPI cards**: split as described in 3.5 — one hero card (Monthly Cash Flow) + horizontal rail
  for the other 5. Currency value `text-2xl md:text-3xl`. `€9,671,330` → `€9.7M` on mobile.
- **4 chart panels**: hidden below `md`. Replace with a single "Insights" card holding four mini
  sparklines that open fullscreen chart sheets on tap.
- **Property Performance table (30×8)**: replace with two ranked card lists — "Top 5 by NOI"
  (green chip), "Needs attention (5)" (red, negative CF first). "View all 30 →" navigates to a
  dedicated `/dashboard/property-performance` route with the table in a horizontal-scrollable
  wrapper + sticky first column. On `md` (iPad portrait) keep the table, sticky first column, fade
  gradient on right edge.
- **Portfolio Occupancy**: single line `Occupancy 88.5%  ▁▂▃▄▅▄▅▆▇▇  ▼ −1.2 pp`. Full chart on tap.
- **Property Status donut**: keep but shrink to 120 px diameter + 2-col legend below.
- **Unpaid Payments (currently 25 items)**: top 5 via card layout described in §6.1 + "View all 25 →".
- **Total / Occupied / Occupancy rate / Monthly Income tiny cards**: kill on mobile — redundant
  with the KPI rail.

**New `/reports` route** absorbs everything we strip from the dashboard:
date-range presets (`<Sheet>` for the picker), 4 full charts, CSV/PDF export buttons. Visible to
all users, default to "Last 12 months". This is also where a "View charts" CTA from the mobile
dashboard lands.

### 4.2 Properties list — `frontend/app/src/pages/PropertyListPage.tsx`

Current state (`04-properties`): hamburger overlaps title; Properties title + subtitle + refresh +
CSV + (wrapped) "Add Property" button all share the top 200 px. Filter card consumes the next 600 px
before a property shows.

**Plan**:
- Use new `<PageHeader title="Properties" primaryAction={Add Property} overflowActions={[Refresh, CSV, XLSX]}>`. Drop subtitle on mobile.
- Replace filter card with the sticky `[search] [⛕ Filters (n)]` row → `<FilterSheet>`.
- Active filter chips row above the list (`[Vacant ×] [Residential ×] [+2]`).
- "30 properties" count moves into the filter row (right-aligned, muted).
- PropertyCard tweaks (see §4.3 below).

### 4.3 Property card — `frontend/app/src/components/properties/PropertyCard.tsx`

- Specs grid `grid grid-cols-3 gap-2 mb-3` → `grid grid-cols-2 xs:grid-cols-3 gap-2 mb-3`.
- Image aspect: `aspect-[16/10] xs:aspect-[4/3] md:aspect-video`.
- Padding: `p-3 md:p-4` (currently `p-4`).
- Title `text-base md:text-lg font-semibold` (currently `text-lg`).
- ID line `#PRO01…` → `text-xs text-text-muted truncate`.
- Currency formatter consolidates with the global util (see §5).

### 4.4 Property detail — `frontend/app/src/pages/PropertyDetailPage.tsx`

Current state (`12-property-detail`): hamburger overlaps "Area / 42185 m²"; just a stacked
label-value list with icons; Google Maps embed shows "Oops! Something went wrong"; no hero, no
tabs, no clear primary actions.

**Plan**:

```
┌─────────────────────────────────┐
│ ← Estrada das Quintas 130    ⋯  │  Sticky PageHeader with back + overflow (Edit/Archive/Share)
│ [Vacant pill]   €1,513 / mo      │
├─────────────────────────────────┤
│ [Photo hero 16:9, swipeable]     │  reuses photo gallery component
├─────────────────────────────────┤
│ Overview · Financials · Tenants · Documents · Photos · Map │  horizontal scroll segmented control
├─────────────────────────────────┤
│ … current section content …    │
├─────────────────────────────────┤
│ [ context CTA — e.g. Add tenant ]│  per-tab primary action
└─────────────────────────────────┘
```

- Overview tab: address card, specs grid (2-up: Area, Type, Year, Beds/Baths), key dates, primary
  contact link.
- Map gets its own tab so the Google Maps failure doesn't break the page hero.
- Map error fallback: static address pill + "Open in Maps →" deep link to `maps:` / Google Maps URL.

### 4.5 Contacts list — `frontend/app/src/pages/ContactListPage.tsx`

Current state (`05-contacts`): rightmost "Filters" button clipped at edge (actively unusable);
sort dropdown eats a row.

**Plan**:
- `<PageHeader title="Contacts" primaryAction={Add Contact} overflowActions={[Refresh, Import, Export CSV]}>`.
- Sticky search + `<FilterSheet>` row (sort moves into the sheet).
- ContactCard: promote the red overdue strip (`€2,613 · 3 payments overdue`) to a corner badge or
  full-width top strip — it's the most actionable signal. Hide low-value email truncation; turn
  phone number into a `tel:` chip.

### 4.6 Contact detail — `frontend/app/src/pages/ContactDetailPage.tsx`

**Stop opening as a modal on mobile, but keep it on desktop.** Today (`13-contact-detail`)
clicking a contact card keeps the URL at `/contacts` and renders a modal that on a phone is
essentially the desktop dialog with the list bleeding through. The fix is conditional on the
viewport — desktop users who rely on the "peek at detail without leaving the list" pattern keep it.

```tsx
// frontend/app/src/pages/ContactListPage.tsx
const isPhone = useIsPhone(); // matches max-width: 767px

const handleContactClick = (contact: Contact) => {
  if (isPhone) {
    navigate(`/contacts/${contact.identifier}`);    // real route on phone
  } else {
    openContactModal(contact.identifier);            // existing modal on tablet+/desktop
  }
};
```

Routing setup:
- Add a *new* `/contacts/:id` route that renders `ContactDetailPage` in full-page mode (uses
  `<PageHeader backHref="/contacts">`, no surrounding modal chrome).
- Keep the existing modal handler intact for the `md+` path.
- (Optional desktop polish — Phase 3) At `xl+`, evolve the desktop pattern further into a true
  list-detail split pane (`/contacts` shows list + currently-selected detail pane side by side).
  Out of scope for the initial migration.

Apply the same conditional click handler to **ContractDetailPage** and any other "detail as modal
over list" pattern.

### 4.7 Contracts list — `frontend/app/src/pages/ContractsPage.tsx`

Current state (`06-contracts`): mostly fine. Two issues:
- The 26-char Sid (`#CON01KRTERBEZ336M1A39Z8180FSZ`) wastes a full row. Hide on mobile, show on
  detail.
- Status filter (`All / Active / Draft / Pending Signature / Expired / Terminated`) wraps to 3 rows;
  move into the `<FilterSheet>`.
- `<PageHeader title="Contracts" primaryAction={Add Contract} overflowActions={[Refresh, CSV]}>`.

### 4.8 Payments — `frontend/app/src/pages/PaymentsPage.tsx`

Current state (`07-payments`): "Schedule Payment" + "Register Payment" buttons stack with wrapped
labels, eating ~150 px above the fold. KPI cards in single column (good). Trend chart compact (good).

**Plan**:
- `<PageHeader title="Payments" primaryAction={Register Payment} overflowActions={[Schedule Payment, Refresh, Export CSV]}>`.
- Keep the two big stat cards (Pending €91,624 / Overdue €11,480) — they're already strong.
- Segmented control: `[All · Overdue · Pending · Paid]`.
- Payment rows: card layout, swipe-left to Mark Paid, swipe-right to Remind, plus explicit
  44 px buttons (see §6.1).
- Group by status with sticky group headers.

### 4.9 Expenses — `frontend/app/src/pages/ExpensesPage.tsx`

Current state (`08-expenses`): header is the worst offender; mini-trend chart same size as KPI
cards conveys less.

**Plan**:
- `<PageHeader title="Expenses" primaryAction={Add Expense} overflowActions={[Refresh, Export CSV, Bulk Categorize]}>`.
- Move the trend chart behind a "See trend →" link to `/reports`.
- Keep Total Expenses + Top Categories card row.
- Expense rows: card with `[icon] Category · Property · €amount · date`; group by month.

### 4.10 Documents — `frontend/app/src/pages/DocumentsPage.tsx`

Current state (`09-documents`): the **only** page still using a real table layout, and its right
column is clipped at 390 px. P0 fix.

**Plan**: convert to `<ResponsiveTable mobileRow={…}>` with this card layout:

```
┌──────────────────────────────────────────┐
│ 📄 Invoice — Annual property tax 2026    │   line-clamp-2 filename
│    EXPENSE · Finca El Olivar 122         │   property link
│    1.2 MB · 17/05/2026             ⬇ ⋯  │   download + overflow
└──────────────────────────────────────────┘
```

- File-type icon colored (PDF red, image purple, sheet green) leading 40 px.
- Selection checkbox only visible in Select mode.

### 4.11 Photos — `frontend/app/src/pages/PhotosPage.tsx`

Current state (`10-photos`): 2-col grid works. Minor cleanup:
- Hide file size on mobile (`122.3 KB` clutter); keep type tag + date.
- Lazy-load via IntersectionObserver, `loading="lazy"`, `decoding="async"`.
- Tap → swipe-lightbox (`yet-another-react-lightbox` or `swiper`), full-bleed, pinch-zoom,
  swipe-down to dismiss.
- Long-press → metadata sheet (size, EXIF if applicable).
- Selection mode for bulk delete / download / move.

### 4.12 Settings — `frontend/app/src/pages/SettingsPage.tsx`

Current state (`11-settings`): the cleanest screen of the bunch. Tabs `Profile & Security` /
`My Preferences` are horizontal pills; forms single-column. Tiny tweaks:
- Page title can stay; subtitle can stay (it's short and adds context).
- Move "Sign Out" out of the sidebar drawer footer into the settings page footer too (already a
  link in the drawer; mobile users may not find it there).

### 4.13 Forms — `Add Property` and friends — `frontend/app/src/pages/PropertyCreatePage.tsx`

Current state (`15-property-new-form`): the best of the bunch. Single column, labeled. Two issues:
- `selectors.selectCountry` shows a raw i18n key — fix the locale file.
- Form is long enough that scroll fatigue + a buried submit hurts on phone (but works fine for
  power users on desktop who scan top-to-bottom).

**Plan**:
- **Stepper at `<md` only.** Desktop and tablet keep the current single-scroll long-form layout —
  experienced users prefer to see everything at once and tab through. Phone gets a stepper for any
  form with 3+ logical sections (Add Property, Add Contract):
  Step 1 Address → Step 2 Specifications → Step 3 Financial → Step 4 Photos → Review.
  Progress dots top, "Back / Continue" sticky bottom. Both layouts share the same react-hook-form
  state — toggling viewport doesn't lose entered values.
- Sticky bottom action bar with Save + Save & Add Another **at `<md`**. Desktop keeps the existing
  inline submit at the form footer. Mobile bar respects `--kbd-inset` so it rides above the keyboard.
- Country picker: replace native `<select>` with a `<Sheet>` containing a search input + virtualized
  list of ~250 countries — **on phone only**. Desktop keeps its current `<select>` or combobox.
- All inputs `font-size: 16px` minimum (handled globally — see §2.2; benign on desktop).
- Phone field: `inputMode="tel"`; numeric/currency: `inputMode="decimal"`; email: `inputMode="email"`
  — these are touch-keyboard hints, no effect on desktop.

### 4.14 Login — `frontend/app/src/pages/LoginPage.tsx` + Keycloak

Current state (`01-login`, `02-keycloak-login`): clean, centered, big tap targets. Mostly fine.
Minor: the Keycloak language picker is small and far down (`02`) — bump touch target. The
"Toggle password visibility" eye icon is small — wrap in 44 px hit box.

---

## 5. Data Formatting & Visualization Rules

### 5.1 Currency

- **Fix the `EUR2,613.00` bug.** Today's `Intl.NumberFormat` is using `currencyDisplay: 'code'`
  (or missing the locale). Switch to `{ style: 'currency', currency: 'EUR', currencyDisplay: 'narrowSymbol', maximumFractionDigits: 0 }` on mobile (1 decimal for `< 1000`).
- Mobile abbreviation rules (`< md`):

  | Range          | Format                        | Example     |
  |----------------|-------------------------------|-------------|
  | ≥ 10 M         | `€X.XM` (1 decimal)           | `€9.7M`     |
  | 1 M – 9.99 M   | `€X.XXM`                      | `€1.66M`    |
  | 100 K – 999 K  | `€XXXK` (no decimals)         | `€124K`     |
  | < 100 K        | locale-grouped full           | `€33,779`   |

- Always render the full value in an accessible affordance:
  `<abbr title="€9,671,330">€9.7M</abbr>` or long-press popover with full precision.
- Centralize in `frontend/app/src/utils/formatCurrency.ts`. Single import everywhere.

### 5.2 Negatives & deltas

- `−€25,828`, never `(€25,828)`.
- Color `text-red-600`, paired with a `▼` glyph so we never rely on color alone.
- Percentage deltas: 1 decimal; use `pp` for percentage-point deltas, `%` for relative changes.

### 5.3 Tooltips on touch

- Hover doesn't exist on touch. For abbreviated values: long-press (500 ms) → popover.
- For chart points: tap → tooltip pins; tap elsewhere → dismiss. Use Recharts' `<Tooltip trigger="click">`.

### 5.4 Charts on mobile (Recharts)

Single shared config object below `md`:

```ts
const mobileChartProps = {
  margin: { top: 8, right: 8, bottom: 8, left: 8 },
  xAxis:  { interval: 'preserveStartEnd', tick: { fontSize: 10 }, tickFormatter: shortMonth },
  yAxis:  { hide: true },
  grid:   false,
  legend: false,
  tooltip: { trigger: 'click' },
  line:   { dot: { r: 6 }, activeDot: { r: 10 } },
  hideSecondarySeries: true,
};
```

Per-chart treatment:

| Chart                    | Mobile treatment                                                  |
|--------------------------|-------------------------------------------------------------------|
| Portfolio Cash Flow      | Single-series sparkline in HeroKpiCard; full chart in fullscreen sheet |
| Portfolio Allocation     | Donut at 140 px + top-3 legend + "Others (n)" + fullscreen on tap |
| Property Comparison      | **Replace** with Top-5 / Bottom-5 ranked card list                |
| Equity Composition       | Stacked progress bar + % breakdown list                          |
| Portfolio Occupancy      | Single line `88.5% ▁▂▃▄▅▄▅▆▇▇ ▼ −1.2 pp` + fullscreen on tap     |

### 5.5 Typography & spacing

Codemod-able table:

| Element        | Current               | Mobile-aware                              |
|----------------|-----------------------|-------------------------------------------|
| Page title     | `text-3xl font-bold`  | `text-2xl md:text-3xl font-bold`          |
| Page icon      | `h-8 w-8`             | `h-6 w-6 md:h-8 md:w-8`                   |
| Subtitle       | `text-base`           | `text-sm md:text-base` (hidden on phone)  |
| Card title     | `text-lg font-semibold` | `text-base md:text-lg font-semibold`    |
| KPI value      | `text-3xl`            | `text-2xl md:text-3xl`                    |
| Section gap    | `mb-6`                | `mb-4 md:mb-6`                            |
| Page padding   | `px-4 py-8`           | `px-4 py-4 md:px-6 md:py-6 lg:px-8 lg:py-8` |
| Card padding   | `p-6`                 | `p-4 md:p-6`                              |
| Form grid gap  | `gap-4`               | `gap-3 md:gap-4`                          |

---

## 6. Touch, Gesture, Microinteractions

### 6.1 Touch targets — minimum 44 × 44 (AAA, Apple HIG)

Audit table:

| Element                                | Current  | Action                  |
|----------------------------------------|----------|-------------------------|
| Hamburger menu button                  | 40 × 40  | Bump to 44              |
| Refresh icon button                    | 32 × 32  | Wrap in `hit-44`        |
| CSV / Import icon buttons              | 32 × 40  | Wrap in `hit-44`        |
| Range chips (`6M`, `1Y`, …)            | 32       | Bump to 40 + `hit-44`   |
| Filter chips (`Vacant`, `Residential`) | 36       | Bump to 40              |
| Activate / Decline buttons             | 32       | Bump to 44              |
| Expand-chart icon                      | 24       | Wrap in `hit-44`        |
| Password-visibility eye                | 24       | Wrap in `hit-44`        |
| Date-picker day cells                  | 36       | Tablet OK; on phone, 40 |
| Sidebar nav rows                       | ~44      | Keep                    |
| List card body                         | full     | Keep (card-level click) |

Components in `packages/ui` that need updating: `Button.tsx`, `IconButton.tsx`, `Chip.tsx`,
`Checkbox.tsx`, `RadioButton.tsx`, `Switch.tsx`, `Pagination.tsx`, `Tabs.tsx`, `DropdownMenu.tsx`,
`DatePicker.tsx`.

### 6.2 Swipe-to-action

- **Payments rows** (high value): swipe left → Mark Paid (green); swipe right → Send Reminder.
- **Expenses rows**: swipe left → Archive.
- **Documents rows**: swipe left → Delete (with confirmation sheet).
- Always paired with explicit buttons. Use `<SwipeAction>` (§3.7) wrapping each row.

### 6.3 Pull-to-refresh

- Keep iOS Safari's native PTR for v1 — costs nothing. `overscroll-behavior` on `<main>` set to
  `contain` only where we genuinely need to (sheets, drawers) so the document body can still PTR.
- Remove the standalone "Refresh" icon button from headers **only after** custom PTR is in place,
  or keep it as an accessible alternative for VoiceOver users.

### 6.4 Selection mode + bulk actions

- Default state: rows have no checkbox.
- Enter selection mode via: explicit "Select" overflow action **or** long-press a row.
- In selection mode: top bar shows `× Cancel · n selected · Actions ▾`, rows get a leading
  32 px checkbox, sticky `<SelectionBar>` (Move · Delete · Download · Share) slides up from bottom.
- Apply to: Photos, Documents, Payments (bulk Mark Paid / Remind), Expenses (bulk Categorize).

### 6.5 Haptics

iOS Safari does not expose `navigator.vibrate`. Keep a `frontend/app/src/utils/haptic.ts` stub
that no-ops today and centralizes future Capacitor wrap. Mark with TODOs: Mark Paid (`impactMedium`),
Swipe complete (`impactLight`), Form submit success (`notificationSuccess`).

### 6.6 Loading / empty / error states

- **Loading**: skeleton screens shaped like the final content (`<DashboardSkeleton>`,
  `<CardListSkeleton>`). React Query `isPending` → skeleton; never a centered spinner on a blank
  page.
- **Empty**: per-list illustration + primary CTA.
  - Properties (0): "No properties yet. Add your first to start tracking rent." + `[+ Add property]`
  - Unpaid (0): "All caught up — no overdue payments." (use the success illustration, not emoji)
  - Documents (0): "Tap to upload or drag a file"
  - **Pending Extensions (0): hide the entire card.** No "0 extensions" noise.
- **Error**: inline error card at the section level with retry. React Query handles network retry
  with exponential backoff via defaults. `<div role="alert">` for assertive announcements.
- **Stale-while-revalidate**: cached data shown with subtle "Updated 2 min ago · Refresh" footer.

---

## 7. Accessibility & i18n

(Some items recur in §2 and §3 — collected here for ticketing.)

### 7.1 ARIA & landmarks

- Hamburger button gets `aria-label={t('a11y.openMenu' | 'closeMenu')}`, `aria-expanded`,
  `aria-controls="primary-sidebar"`.
- `<aside id="primary-sidebar">` gets `aria-label={t('a11y.primaryNav')}` and `inert` when closed
  (so its links leave the tab order).
- All icon-only buttons: explicit `aria-label` (Refresh, CSV, Expand chart, Close).
- `<main id="main-content">` already present; add `<header role="banner">` wrapping env banner +
  sticky PageHeader.

### 7.2 Focus management

- Drawer: focus trap on open, restore focus on close. Use Radix Dialog under the hood (or wrap with
  `focus-trap-react`).
- `<Sheet>` (Vaul) already manages focus correctly.
- Verify `focus-ring` utility (`theme.css:203`) is applied everywhere `outline-none` is currently
  used.

### 7.3 Live regions

- Mount `<div id="a11y-live" role="status" aria-live="polite" className="sr-only" />` in Layout.
- New `useAnnounce()` hook reads/writes to it. Wire into React Query mutation success/error +
  refresh actions: `useAnnounce()('Refresh complete, 30 properties')`.
- Form-level errors: `<div role="alert">` at the form root.

### 7.4 Contrast

- LOCAL ENVIRONMENT banner: switch to `bg-accent-700 text-white` (≈ 5.9:1).
- `Action required: Review overdue payments` small body: tighten to `error-text-strong`.
- `--text-disabled` (`#a8a29e`) currently 2.6:1: reserve for *actually disabled* controls, never
  for body text. Audit usages.

### 7.5 Dynamic Type

- Confirm no hard-coded `px` font-size in shared UI.
- Allow line wrap on sidebar logo/title at very large text (currently truncated).
- Layout heights using `h-16` (rem) scale correctly — leave.

### 7.6 Reduced motion

- Global `prefers-reduced-motion: reduce` rule already in `theme.css:213`. Extend with a Radix data
  attribute neutralizer:
  ```css
  @media (prefers-reduced-motion: reduce) {
    [data-state='open'][data-side], [data-state='closed'][data-side] { transform: none !important; }
  }
  ```
- Vaul respects `--vaul-after-transition`; the global rule covers it.

### 7.7 Form labels & required indicator

- Every `<input>` paired with `<label htmlFor>` or `aria-labelledby`.
- Asterisk hidden from SR; visually-hidden "required" word:
  ```jsx
  <label htmlFor="street">
    {t('street')}<span aria-hidden="true"> *</span>
    <span className="sr-only"> {t('a11y.required')}</span>
  </label>
  ```
- Group sections in `<fieldset>` / `<legend>` (currently `<h2>` — acceptable but weaker).

### 7.8 i18n + RTL

- Wire i18next `languageChanged` to `document.documentElement.lang` and `dir`:
  ```ts
  i18n.on('languageChanged', (lng) => {
    document.documentElement.lang = lng;
    document.documentElement.dir = ['ar', 'he', 'fa', 'ur'].includes(lng) ? 'rtl' : 'ltr';
  });
  ```
- Fix `selectors.selectCountry` raw i18n key in property form.
- RTL pass is P2 — when Arabic ships, audit sidebar `inset-inline-start`, `safe-area-inset-{left,right}` symmetry, and `translate-x-*` variants.

---

## 8. iOS / iPadOS Specifics

(Most items already covered in §2; this section is a focused checklist.)

- `viewport-fit=cover` + `apple-mobile-web-app-status-bar-style=black-translucent` → env banner
  extends under the notch.
- `100vh` → `100dvh` (`Layout.tsx:43`, `Sidebar.tsx:222`) — Safari URL-bar bug fix.
- 16 px minimum input font-size to kill focus-zoom.
- `touch-action: manipulation` + `-webkit-tap-highlight-color: transparent` globally on
  interactive elements (kills the 300 ms tap delay, removes the gray highlight flash).
- `overscroll-behavior: contain` on sheet body / drawer body / modal body — keep document PTR.
- Date inputs: native `<input type="date">` is fine; ensure `min-height: 44 px`.
- `--kbd-inset` driven by `visualViewport` API; sheet footers consume it.
- Pointer-events vs touch-events: standardize on Pointer Events (Radix/Vaul already do).
  Add a `usePointer()` returning `'fine' | 'coarse'` for iPad-with-keyboard distinction:
  - `@media (hover: hover) and (pointer: fine)` → desktop hover affordances
  - `@media (hover: none)` → pressed/active states
- Stage Manager / Split View at 320 px: verify the dashboard collapses to 1-up KPIs.
- PWA manifest + 192 / 512 / maskable icons; iOS doesn't support web push but standalone install
  is worth it for daily users.

---

## 9. Acceptance Criteria & Verification

### 9.1 Manual QA matrix

| Device                    | Width  | Key flows to verify                                                |
|---------------------------|--------|--------------------------------------------------------------------|
| iPhone SE 1st gen         | 320    | Dashboard above-the-fold, Property card readable, no h-scroll      |
| iPhone 13                 | 390    | All P0 flows, no header collision, tab bar visible                 |
| iPhone 14 Plus            | 428    | Tab bar spacing, drawer width cap (`min(18rem, 85vw)`)             |
| Pixel 7                   | 412    | Chrome rendering, Vaul gestures                                    |
| iPad portrait             | 768    | **Sidebar rail visible**, KPI 2-up grid, header inline actions     |
| iPad landscape            | 1024   | Sidebar expanded by user toggle, behaves like desktop              |
| Split View / iPad half    | 375    | Behaves like phone — no rail, drawer + hamburger                   |

### 9.2 Automated checks

- Playwright responsive snapshot tests at 4 widths: 375, 390, 768, 1280. Snapshot every page after
  Phase 2.
- Lighthouse mobile pass: target Performance > 85, Accessibility > 95. Enforce in CI on PR
  preview.
- Axe-core a11y scan as part of CI test suite.
- Visual regression diff for `<PageHeader>` migrations (per-batch).

### 9.3 Definition of done per screen

A screen ships when:
1. No content clipped or overlapped at 320, 390, 768.
2. Primary action reachable in ≤ 1 tap from the header.
3. Skeleton + empty + error states implemented.
4. All interactive elements ≥ 44 × 44.
5. Axe scan reports no critical/serious issues.
6. VoiceOver swipe-through reaches every interactive element in logical order.
7. **Desktop pixel parity (1280 × 800 and 1440 × 900):** visual-diff snapshot vs. `main` shows
   no unintended changes. PR must include before/after screenshots at desktop widths for every
   page touched. Intentional desktop changes (e.g. the `<PageHeader>` consolidation producing the
   same layout via different markup) must be approved by design with a side-by-side comparison.
   Add a Playwright snapshot suite per page at 1280 × 800; bake into CI.
8. **Tablet pixel parity at 1024 × 768** (iPad landscape) — same rule. The new sidebar rail
   appears at 768–1023 px only; at exactly 1024 the desktop sidebar is shown.

---

## 10. Phased Roadmap

### Phase 0 — Quick wins (3 days, can land before refactor)

These are "fix today" items that don't depend on the new primitives:

- F0.1 Fix viewport meta (`index.html`).
- F0.2 `100vh → 100dvh` in `Layout.tsx`, `Sidebar.tsx`.
- F0.3 LOCAL ENVIRONMENT banner contrast (`bg-accent-700`).
- F0.4 Currency formatter bug: `EUR2,613.00` → `€2,613`.
- F0.5 `selectors.selectCountry` raw i18n key fix.
- F0.6 16 px global input font-size + `touch-action: manipulation`.
- F0.7 Drawer accessibility: `aria-label` / `aria-expanded` / `inert` when closed.
- F0.8 Properties page filter card: collapse `<details>` by default on `< md`.

### Phase 1 — Foundation (1.5 weeks)

Goal: ship the design-system primitives plus the new app shell so Phase 2 is a mechanical migration.

- F1.1 Add `xs` breakpoint, `--safe-*`, `--header-h`, `--bottomnav-h`, `--size-touch`, `--kbd-inset`
       tokens to `theme.css`.
- F1.2 `Layout.tsx` refactor (min-h-dvh, MobileNavContext, max-w container, bottom-nav padding).
- F1.3 `Sidebar.tsx` three-variant refactor: drawer (`< md`) + rail (`md → lg`) + expanded (`lg+`).
       Swipe-to-close. Focus trap. Env banner inset.
- F1.4 `<PageHeader>` component in `packages/ui` + migrate 3 reference pages (Dashboard,
       Properties, Payments) to validate API.
- F1.5 `<Sheet>` component (Vaul) — wraps `ModalWrapper` for `md+` fallback.
- F1.6 `useKeyboardInset` hook.
- F1.7 Bottom Tab Bar component, behind a `MOBILE_BOTTOM_NAV` feature flag.
- F1.8 PWA manifest + icons.

### Phase 2 — Core screens (2–3 weeks)

Goal: every primary screen looks designed-for-mobile.

- F2.1 Migrate remaining ~22 pages to `<PageHeader>`. Batches of 5 with visual QA each batch.
- F2.2 `<FilterSheet>` + `<ChipGroup>`; convert PropertyList, ContactList, ContractList, Expenses,
       Documents, Photos filter blocks.
- F2.3 `<ResponsiveTable>` + `<DataList>`; convert hot pages (Payments, Expenses, Documents,
       AuditLog, ContactFinancialsTab, PropertyContractsTab) — see §3.3 list.
- F2.4 **Dashboard mobile redesign**: `<HeroKpiCard>`, `<KpiRail>`, alerts strip, top/bottom-5
       cards, hidden charts below `md`, "Insights" mini-spark grid + fullscreen chart sheets.
- F2.5 New `/reports` route with all charts, time-range presets, CSV/PDF exports.
- F2.6 ContactDetail / ContractDetail: convert from modal-over-list to real routes on mobile.
- F2.7 PropertyDetail: sticky header + photo hero + horizontal tabs (Overview / Financials /
       Tenants / Documents / Photos / Map). Map error fallback (deep link to native Maps).
- F2.8 Forms: Add Property + Add Contract steppers with sticky bottom CTA and country picker sheet.
- F2.9 PropertyCard / ContactCard tweaks (grid-cols, typography, badge promotion).
- F2.10 Recharts mobile defaults + `<ChartCard expandable>` + fullscreen chart sheet.

### Phase 3 — Polish (1–1.5 weeks)

- F3.1 Touch-target audit pass across all `packages/ui` components.
- F3.2 `<SwipeAction>` rollout (Payments, Expenses, Documents).
- F3.3 Selection mode + `<SelectionBar>` (Photos, Documents, Payments, Expenses).
- F3.4 Skeleton loaders per major screen.
- F3.5 Custom empty states with illustrations + CTAs.
- F3.6 Live region + `useAnnounce` wired to React Query mutations.
- F3.7 Visual QA across the device matrix in §9.1.
- F3.8 Lighthouse / Axe in CI; fix regressions.

### Phase 4 — Optional (later)

- F4.1 Custom pull-to-refresh if telemetry shows users hit Refresh > 1×/session.
- F4.2 Service worker for offline read-only access to cached dashboard data.
- F4.3 RTL pass when Arabic locale lands.
- F4.4 Capacitor wrap for native haptics + push (if/when product wants App Store presence).

---

## 11. Risk Register & Gotchas

- **Tailwind v4 arbitrary utilities**: `min-h-touch` won't work as a modifier without `@theme`
  registration or a `@layer utilities` rule (we use the latter). Decide once, document, don't mix.
- **`100dvh` browser support**: iOS 16+, Chrome Android 108+. Layer with `min-h-screen` fallback
  first, then `min-h-dvh`. Test on iOS 15.
- **`viewport-fit=cover` side effects**: any `fixed bottom-0` element will look floating until you
  add `pb-[var(--safe-bottom)]`. Sweep all current `fixed bottom-0` usages before merging.
- **Sticky header + backdrop-blur on iOS**: combine with `transform: translateZ(0)` or
  `will-change: transform` to avoid a white flash on scroll. Limit to mobile for perf.
- **Drawer swipe vs horizontal table scroll**: only attach swipe-to-close listener when the touch
  starts within 24 px of the left edge, else it'll fight horizontal scrolling inside tables.
- **Bottom tab bar + keyboard**: on iOS the keyboard pushes the visual viewport up; `fixed`
  elements hide behind it. Use `visualViewport.height` to hide the tab bar while keyboard is open
  (`if (visualViewport.height < window.innerHeight - 100) hide()`).
- **Sheet body scroll lock**: don't apply `overflow:hidden` on `<body>` while a sheet is open — it
  conflicts with `100dvh`. Use `overscroll-contain` on the sheet's scroll container instead.
- **`<PageHeader>` migration is high-blast-radius**: do it behind per-page branch flags or merge in
  batches of 5 with visual QA each batch — easier to revert.
- **Recharts remount thrash on resize**: toggling `<Legend>` etc. via `useIsMobile` re-creates the
  SVG. Debounce `useIsMobile` by 100 ms.
- **Long translation strings**: German/Dutch action labels (`Eigenschaft hinzufügen`) might break a
  compact single-CTA layout. Use `truncate` + descriptive `aria-label` on compact buttons.
- **Form pages already exist as routes (not modals)**: don't accidentally convert them to sheets;
  only convert routes that are conceptually modal (quick add, confirmations).
- **Modal/Sheet z-index conflict**: consolidate the z-index table from §3 of the iOS report.
  Today the floating hamburger has `z-50` colliding with the Radix Dialog overlay.
- **Sidebar rail at `md`**: avoid two sources of truth for "collapsed". At `md` always rail; at
  `lg` honor user's localStorage toggle. Hide the toggle button below `lg`.

---

## 12. Files Most Affected (Quick Reference)

```
frontend/app/index.html                                       — viewport meta, manifest link
frontend/app/public/manifest.webmanifest                      — NEW
frontend/packages/ui/src/styles/theme.css                     — tokens, utilities, contrast fix

frontend/app/src/components/Layout.tsx                        — dvh, nav context, max-w, tab-bar pad
frontend/app/src/components/Sidebar.tsx                       — drawer + rail + desktop variants
frontend/app/src/components/EnvironmentBanner.tsx (or eq)     — notch padding + contrast
frontend/app/src/hooks/useIsMobile.ts                         — debounce
frontend/app/src/hooks/useKeyboardInset.ts                    — NEW
frontend/app/src/hooks/useAnnounce.ts                         — NEW

frontend/packages/ui/src/components/PageHeader.tsx            — NEW
frontend/packages/ui/src/components/Sheet.tsx                 — NEW (Vaul)
frontend/packages/ui/src/components/ResponsiveTable.tsx       — NEW
frontend/packages/ui/src/components/DataList.tsx              — NEW
frontend/packages/ui/src/components/FilterSheet.tsx           — NEW
frontend/packages/ui/src/components/HeroKpiCard.tsx           — NEW
frontend/packages/ui/src/components/KpiRail.tsx               — NEW
frontend/packages/ui/src/components/ChartCard.tsx             — NEW
frontend/packages/ui/src/components/SwipeAction.tsx           — NEW
frontend/packages/ui/src/components/SelectionBar.tsx          — NEW
frontend/packages/ui/src/components/BottomTabBar.tsx          — NEW (iPhone only)
frontend/packages/ui/src/components/ModalWrapper.tsx          — add `variant="sheet"` shim

frontend/app/src/utils/formatCurrency.ts                      — NEW (Intl + abbreviation rules)
frontend/app/src/utils/formatNumber.ts                        — NEW
frontend/app/src/utils/haptic.ts                              — NEW (stub for Capacitor wrap)

frontend/app/src/components/DashboardPage.tsx                 — recompose
frontend/app/src/components/dashboard/PortfolioSummaryCards.tsx — split: hero + rail
frontend/app/src/components/dashboard/PropertyPerformanceTable.tsx — sticky col, mobile card variant
frontend/app/src/components/dashboard/PortfolioCashFlowChart.tsx and siblings — mobile defaults
frontend/app/src/pages/{PropertyListPage,PropertyDetailPage,PropertyCreatePage}.tsx
frontend/app/src/pages/{ContactListPage,ContactDetailPage}.tsx — route-ify detail on mobile
frontend/app/src/pages/{ContractsPage,ContractDetailPage,ContractCreatePage}.tsx
frontend/app/src/pages/{PaymentsPage,PaymentDetailPage,PaymentCreatePage}.tsx
frontend/app/src/pages/{ExpensesPage,DocumentsPage,PhotosPage,SettingsPage}.tsx
frontend/app/src/pages/FinancialReportsPage.tsx              — becomes new /reports
                                                                charts + date controls + export
```

---

## 13. Open Questions

These need product / design input before Phase 2 execution:

1. **Bottom tab bar destinations**: confirm the 5 routes (Dashboard / Properties / Contacts /
   Payments / More) match the most-used routes in analytics. PostHog data should validate or
   suggest swaps (e.g., Contracts may rank above Contacts for some user segments).
2. **`/reports` route content**: do we want it gated by role? TEAM_VIEWER vs TEAM_EDITOR.
3. **Dashboard "Action Required" alert strip**: which alerts qualify? Suggested: overdue payments,
   pending contract extensions, contracts expiring < 30 days, document signatures pending.
4. **PWA install prompt**: do we want a non-intrusive nudge after N visits, or rely on browser?
5. **Detail-page modal removal**: confirm we kill the modal-as-detail anti-pattern on *all* breakpoints
   (current code path), or keep it for desktop list-detail split-view at `xl+`.
6. **Phone bottom tab vs hamburger only**: ship behind a feature flag for A/B?
7. **Currency abbreviation cutoff** (`< md` is the proposal — but on tablet the long Property
   Performance table shows full numbers fine; do we want consistency or context-aware
   abbreviation?).
8. **Country picker library**: build a custom searchable Sheet vs use an existing component
   (`react-select` is too heavy; `cmdk` is a strong option).
9. **Photo lightbox library**: `yet-another-react-lightbox` (smaller, simpler) vs `swiper`
   (richer, larger).

---

*End of plan.*
