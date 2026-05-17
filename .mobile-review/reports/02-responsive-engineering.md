# Responsive Engineering Plan — Buurman Mobile

## Architecture Direction

The current frontend is desktop-first with a single break at `lg` (1024px) where the sidebar appears. Below that, the app collapses to an off-canvas drawer triggered by a floating hamburger that overlaps page titles, page headers spill horizontally because their action toolbars are not designed for ~360px viewports, and every list view renders a raw `<table>` with no card fallback. The plan below pushes the app to a true mobile-first layout: add an `xs` breakpoint for very small phones, move the rail sidebar break to `md` (768px) so iPad gets a persistent icon rail, extract a `<PageHeader>` and `<ResponsiveTable>/<DataList>` pair to remove per-page boilerplate, add a `variant="sheet"` to `ModalWrapper` for full-screen mobile sheets, and gate the dashboard's chart-heavy composition behind a `lg:` check so phones see KPIs + alerts above the fold. The hamburger moves into the sticky page header to free up the title row, and an optional bottom tab bar on iPhone replaces drawer round-trips for primary navigation.

---

## 1. Breakpoint Strategy

Tailwind v4 reads breakpoints from `@theme` in `frontend/packages/ui/src/styles/theme.css`. Add an `xs` token and keep the rest at defaults:

```css
@theme {
  --breakpoint-xs: 22.5rem;   /* 360px — very small phones, iPhone SE */
  /* sm 640, md 768, lg 1024, xl 1280, 2xl 1536 — defaults retained */
}
```

| BP    | Width  | Devices                       | Design intent                                              |
|-------|--------|-------------------------------|------------------------------------------------------------|
| —     | <360   | iPhone SE 1st gen             | Single column, no decorative icons, abbreviated labels     |
| `xs`  | ≥360   | iPhone 12/13/14, Pixel 5      | Default mobile target. Single column lists as cards.       |
| `sm`  | ≥640   | Large phones landscape, phablets | 2-col card grids allowed, inline filter chips visible.  |
| `md`  | ≥768   | iPad portrait                 | **Sidebar rail appears (icon-only)**. 2-col forms.         |
| `lg`  | ≥1024  | iPad landscape, small laptops | Full sidebar expanded by default. 3-col card grids. Tables. |
| `xl`  | ≥1280  | Desktop                       | Multi-pane detail views, side-by-side dashboard charts.    |
| `2xl` | ≥1536  | Wide desktop                  | Max-width 1536 content. No layout change.                  |

**Sidebar break decision**: move from `lg:` to `md:` for the *rail* (icon-only) variant, keep `lg:` for the *expanded* variant. iPad portrait (768×1024) currently gets the mobile drawer, which wastes the 200px we could give to a rail. Drawer fallback remains for `<md`.

---

## 2. App Shell Refactor

`frontend/app/src/components/Layout.tsx` currently fixes `height: calc(100vh - env-banner)` on the outer flex and uses `lg:ml-20`/`lg:ml-64` margins. Refactor to mobile-first with safe-area variables, a sticky page header slot rendered via portal-or-context, and conditional bottom nav.

Add to `theme.css`:

```css
@theme {
  --safe-top: env(safe-area-inset-top);
  --safe-bottom: env(safe-area-inset-bottom);
  --header-h: 3.5rem;       /* 56px sticky mobile header */
  --bottomnav-h: 3.75rem;   /* 60px */
}
```

Proposed `Layout.tsx` skeleton:

```tsx
export const Layout = () => {
  const [collapsed, setCollapsed] = useSidebarCollapsed();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
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
          collapsed ? 'md:ml-16 lg:ml-20' : 'md:ml-16 lg:ml-64'
        )}
      >
        <AuthenticatedBroadcastBanner />
        <MobileTopBar
          className="md:hidden sticky top-0 z-30"
          onMenu={() => setMobileNavOpen(true)}
        />
        <main
          id="main-content"
          className="flex-1 overflow-x-hidden pb-[calc(var(--bottomnav-h)+var(--safe-bottom))] md:pb-0"
        >
          <div className="mx-auto w-full max-w-screen-2xl px-4 py-4 md:px-6 md:py-6 lg:px-8 lg:py-8">
            <Outlet />
          </div>
        </main>
        <BottomTabBar className="md:hidden" />
      </div>
      {showWizard && <OnboardingWizard ... />}
    </div>
  );
};
```

Key changes:
- `min-h-dvh` (dynamic viewport) instead of fixed `100vh` — fixes iOS Safari URL-bar jump
- Bottom padding reserved when bottom tab bar is mounted
- `MobileTopBar` is a thin sticky element that the page-level `<PageHeader>` overlays/extends; alternatively just slot the page header itself sticky
- `md:ml-16` keeps a 64px rail visible on iPad portrait

---

## 3. PageHeader Component

New file: `frontend/packages/ui/src/components/PageHeader.tsx`.

```tsx
interface PageHeaderAction {
  label: string;
  icon?: LucideIcon;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'ghost';
  disabled?: boolean;
  showOn?: 'mobile' | 'desktop' | 'both'; // default 'both'
}

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  icon?: LucideIcon;
  actions?: PageHeaderAction[];       // shown inline on md+
  overflowActions?: PageHeaderAction[]; // always in "..." menu
  primaryAction?: PageHeaderAction;   // becomes single visible CTA on mobile
  backHref?: string;
  sticky?: boolean;                    // default true on mobile
}
```

Class composition:

```tsx
<header
  className={clsx(
    'flex items-center gap-2 mb-4 md:mb-6',
    sticky && 'sticky top-0 z-20 -mx-4 px-4 md:mx-0 md:px-0',
    sticky && 'bg-surface-page/85 backdrop-blur-md',
    'min-h-[3.5rem] md:min-h-[4.5rem]',
    'pt-[var(--safe-top)] md:pt-0',
  )}
>
  {/* Mobile-only menu button (replaces floating one) */}
  <button className="md:hidden p-2 -ml-2 min-h-touch min-w-touch">
    <Menu />
  </button>
  {backHref && <BackButton href={backHref} />}

  <div className="flex-1 min-w-0">
    <div className="flex items-center gap-2">
      {Icon && <Icon className="h-6 w-6 md:h-8 md:w-8 text-primary-500 hidden xs:block" />}
      <h1 className="text-xl xs:text-2xl md:text-3xl font-bold truncate">{title}</h1>
    </div>
    {subtitle && (
      <p className="hidden md:block text-text-secondary text-sm md:text-base">
        {subtitle}
      </p>
    )}
  </div>

  <div className="flex items-center gap-2">
    {/* Desktop: all actions inline */}
    <div className="hidden md:flex items-center gap-2">
      {actions?.map(a => <Action {...a} />)}
    </div>
    {/* Mobile: primary CTA + overflow */}
    {primaryAction && <Action {...primaryAction} compact className="md:hidden" />}
    {(overflowActions?.length || (actions?.length && /*on mobile*/ true)) && (
      <OverflowMenu items={[...(actions ?? []), ...(overflowActions ?? [])]}
                    className="md:hidden" />
    )}
  </div>
</header>
```

**Before/after — `PropertyListPage.tsx` lines 129–160** becomes:

```tsx
<PageHeader
  title={t('list.title')}
  subtitle={t('list.subtitle')}
  icon={Home}
  primaryAction={{
    label: t('list.addButton'),
    icon: Plus,
    onClick: () => navigate('/properties/new'),
    disabled: !canEditData,
    variant: 'primary',
  }}
  actions={[
    { label: t('common:refresh'), icon: RefreshCw, onClick: () => refetch() },
  ]}
  overflowActions={[
    { label: 'Export CSV', icon: FileText, onClick: exportPropertiesCsv },
    { label: 'Export XLSX', icon: FileSpreadsheet, onClick: exportPropertiesXlsx },
    { label: 'Google Sheet', icon: Sheet, onClick: exportPropertiesGoogleSheet },
  ]}
/>
```

This removes ~30 lines per page across ~25 list pages.

---

## 4. Responsive Table → DataList

Two new components in `frontend/packages/ui/src/components/`:

**`ResponsiveTable.tsx`** — wrapper that:
- Above `md`: renders `<table>` with overflow-x-auto + sticky header
- Below `md`: renders children mapped via `mobileRow` render prop into cards

```tsx
interface ResponsiveTableProps<T> {
  rows: T[];
  columns: Column<T>[];          // { key, header, cell, align, hideBelow? }
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
  mobileRow: (row: T) => ReactNode; // card layout
  emptyState?: ReactNode;
  stickyHeader?: boolean;
}

export function ResponsiveTable<T>({ rows, columns, mobileRow, ... }: Props<T>) {
  return (
    <>
      {/* Mobile cards */}
      <div className="md:hidden space-y-3">
        {rows.map(r => (
          <button
            key={rowKey(r)}
            onClick={() => onRowClick?.(r)}
            className="block w-full text-left bg-surface-card rounded-lg
                       border border-border-default p-4 min-h-touch
                       hover:border-primary-300 transition-colors"
          >
            {mobileRow(r)}
          </button>
        ))}
      </div>
      {/* Desktop table */}
      <div className="hidden md:block overflow-x-auto rounded-lg border border-border-default">
        <table className="min-w-full">{/* ... */}</table>
      </div>
    </>
  );
}
```

**`DataList.tsx`** — a simpler primitive used by `mobileRow` for consistent card row layout (label/value pairs, status pills, click target).

**Per-page migration list** (raw `<table>` usages — grepped):

| File | Notes |
|------|-------|
| `pages/PaymentsPage.tsx` | Hot path — 5 columns, currency-heavy. Card: title=payment ID, value=amount, badges=status + overdue |
| `pages/ExpensesPage.tsx` | Same pattern as payments |
| `pages/TransactionHistoryPage.tsx` | Append-only log, consider grouped-by-day on mobile |
| `pages/DocumentsPage.tsx` | Already half card-like, easy port |
| `pages/AuditLogPage.tsx` | Wide table — high value as mobile cards |
| `pages/PaymentDetailPage.tsx` | Inline table within page — convert |
| `pages/FinancialReportsPage.tsx` | Multiple tables. Consider tabs + collapse |
| `pages/admin/AdminNotificationsPage.tsx` | Admin only — lower priority |
| `components/dashboard/PropertyPerformanceTable.tsx` | Used in dashboard; on mobile, replace with top-3 card snippet + "View all" link |
| `components/dashboard/UpcomingRenewalsPanel.tsx` | Same as above |
| `components/contacts/ContactFinancialsTab.tsx` | Inside tab — render `<DataList>` directly |
| `components/contacts/ContactContractsTable.tsx` | Direct port |
| `components/contacts/ContactAddressList.tsx` | Small table, just stack rows |
| `components/contacts/ImportWizard.tsx` | Preview grid — keep table with horizontal scroll only |
| `components/contracts/ContractPaymentsTab.tsx` | Direct port |
| `components/properties/PropertyExpensesTab.tsx` | Direct port |
| `components/properties/PropertyContractsTab.tsx` | Direct port |
| `components/properties/DocumentList.tsx` | Direct port |
| `components/properties/financials/PropertyFinancialsTab.tsx` | Multiple inner tables |
| `components/rentIncreases/{ReviewStep,PropertyAdjustmentStep,ConfirmationStep}.tsx` | Wizard — wide tables; mobile-friendly card list of properties with inline editable rate |
| `components/rentRegulations/RuleHistoryTable.tsx` | Direct port |
| `components/settings/{PaymentHistorySection,UserPreferencesSection}.tsx` | Direct port |
| `components/common/BulkDataGrid.tsx` | Power-user grid — keep horizontal scroll only, don't card-ify |

Estimate: ~22 files, ~1 day each at high quality (cell layout decisions), reducible by sharing 2–3 card templates.

---

## 5. Filter Sheet Pattern

Today, `PropertyListPage` renders a full filter card inline (lines 162+). On mobile it eats half the first screen before any data. Introduce a chip + bottom-sheet pattern.

New component: `frontend/packages/ui/src/components/FilterSheet.tsx`.

```tsx
interface FilterSheetProps {
  triggerLabel?: string;          // default "Filters"
  activeCount: number;            // shown as badge on trigger
  onClear: () => void;
  children: ReactNode;            // filter form content
}
```

API usage:

```tsx
<div className="flex items-center gap-2 mb-4">
  <SearchInput ... className="flex-1" />
  <FilterSheet activeCount={activeFilterCount} onClear={resetFilters}>
    <FilterGroup label="Category">
      <ChipGroup options={categoryOptions} value={category} onChange={setCategory} />
    </FilterGroup>
    <FilterGroup label="Status">
      <ChipGroup options={statusOptions} value={status} onChange={setStatus} />
    </FilterGroup>
  </FilterSheet>
</div>
{activeFilterCount > 0 && (
  <ActiveFilterChips filters={appliedFilters} onRemove={...} className="mb-3" />
)}
```

Implementation: on `<md` the sheet slides up from bottom (`fixed inset-x-0 bottom-0 rounded-t-2xl pb-[var(--safe-bottom)]`), on `≥md` it renders as a popover anchored to the trigger.

---

## 6. Modal → Sheet Variant

Extend `frontend/packages/ui/src/components/ModalWrapper.tsx` with:

```tsx
interface ModalWrapperProps {
  variant?: 'dialog' | 'sheet';   // default 'dialog'
  // ...
}
```

When `variant="sheet"` and viewport `<md`:
- Container: `fixed inset-0 flex flex-col bg-surface-card`
- Header: `sticky top-0 pt-[var(--safe-top)] bg-surface-card border-b z-10`
- Body: `flex-1 overflow-y-auto overscroll-contain`
- Footer (CTAs): `sticky bottom-0 pb-[var(--safe-bottom)] bg-surface-card border-t`
- Animation: slide-up enter (`translate-y-full → 0`)
- Above `md`: falls back to standard centered dialog

**Modals to convert** (greppable via `<ModalWrapper`):
- Property create/edit (`PropertyFormModal` / route-based form — see screenshot 15, currently a page already)
- Tenant create/edit
- Contract wizard (multi-step — sheet variant especially helpful)
- Payment registration (`Register Payment`, `Schedule Payment` in screenshot 07)
- Expense add/edit
- Document upload preview
- Photo viewer (`variant="fullscreen"` — separate)
- Onboarding wizard steps
- All "Confirm delete" small dialogs → keep as `dialog` (small action, full-screen is overkill)

Rule of thumb: any modal with > 1 input field → `sheet`. Confirmations → keep `dialog` (mobile-centered, max-w-sm).

---

## 7. Sidebar / Navigation Improvements

Issues from `Sidebar.tsx`:
- Drawer is `w-64` (256px) — on 360px phones that's 71% of screen, fine but consider `w-[min(18rem,85vw)]`
- No swipe-to-close
- No iPad rail variant — currently iPad portrait gets hamburger

Refactor:

```tsx
// Mobile drawer (<md)
className={clsx(
  'md:hidden fixed inset-y-0 left-0 z-40',
  'w-[min(18rem,85vw)]',
  'pt-[var(--safe-top)] pb-[var(--safe-bottom)]',
  mobileOpen ? 'translate-x-0' : '-translate-x-full',
  'transition-transform duration-300 ease-out',
)}

// iPad rail (md → lg)
className="hidden md:flex lg:hidden fixed inset-y-0 left-0 w-16 z-30 ...icon-only"

// Desktop expanded/collapsed (lg+)
className={clsx(
  'hidden lg:flex fixed inset-y-0 left-0 z-30',
  collapsed ? 'lg:w-20' : 'lg:w-64',
)}
```

Add swipe-to-close via a small hook `useSwipeClose(ref, onClose, { edge: 'left' })` — touchstart/touchmove on the drawer, threshold 60px + velocity check. Backdrop click already wired (line 427–432).

Add `aria-modal="true"`, focus trap, ESC handler.

---

## 8. Hamburger Repositioning

Current: `lg:hidden fixed left-4 z-50 top-(env-banner+1rem)` (Sidebar.tsx line 203–209) — floats above page content, overlapping titles (visible in screenshots 03, 04, 05, 06, 07, 12).

Move it into the page header itself. The `<PageHeader>` (section 3) renders a `md:hidden` menu button as its first child:

```tsx
<button
  onClick={openMobileNav}
  className="md:hidden -ml-2 p-2 min-h-touch min-w-touch
             rounded-md text-text-primary hover:bg-surface-inset"
  aria-label="Open menu"
>
  <Menu className="h-6 w-6" />
</button>
```

Sidebar no longer owns the trigger. Layout passes an `openMobileNav` callback via context (`MobileNavContext`) so `<PageHeader>` can call it without prop drilling. Detail pages without a `<PageHeader>` get a `<MobileTopBar>` from `Layout.tsx` instead.

Net effect: title row is no longer overlapped, hamburger has stable position, becomes part of the sticky header so it remains accessible while scrolling.

---

## 9. List Card Tuning

`frontend/app/src/components/properties/PropertyCard.tsx:77` uses `grid grid-cols-3 gap-2 mb-3` for specs (area/type/etc.). On 360px this is ~110px columns → cramped, label clipping visible in screenshots.

Change:

```tsx
<div className="grid grid-cols-2 xs:grid-cols-3 gap-2 mb-3">
```

Additional tweaks:
- Image: `aspect-[16/10] xs:aspect-[4/3] md:aspect-video` — wider on small phones to balance content density
- Padding: `p-3 md:p-4`
- Title: `text-base md:text-lg font-semibold` (currently `text-lg`/`text-xl` would overflow with ID line)
- ID line (`#PRO01...`): `text-xs text-text-muted truncate` — always truncate

`ContactCard` (screenshot 05) similar: `flex-wrap` the inline meta row (`email + phone`) to `grid grid-cols-1 sm:grid-cols-2 gap-1`.

---

## 10. Touch Targets

Add design tokens in `theme.css`:

```css
@theme {
  --size-touch: 2.75rem; /* 44px — Apple HIG */
}
```

Then in Tailwind v4, `min-h-touch` / `min-w-touch` arbitrary utilities won't work without configuration — instead use `min-h-[var(--size-touch)]` or add CSS layer:

```css
@layer utilities {
  .min-h-touch { min-height: var(--size-touch); }
  .min-w-touch { min-width: var(--size-touch); }
}
```

**Component files to audit/update in `packages/ui/src/components/`**:
- `Button.tsx` — already likely ok; ensure `size="sm"` still hits 44px on touch (use `py-1 px-3 min-h-touch` for the touch device variant via `@media (pointer: coarse)`)
- `IconButton.tsx` — frequent offender (used for refresh, close, overflow)
- `Chip.tsx` / filter chips — currently small; bump min-h on mobile
- `Checkbox.tsx`, `RadioButton.tsx`, `Switch.tsx` — invisible click target needs to be 44×44 even if visual is 20×20
- `Pagination.tsx` — page number buttons
- `Tabs.tsx` — tab triggers
- `DropdownMenu.tsx` — menu items
- `DatePicker.tsx` — day cells (consider 40px on phones since the grid is 7-wide)

Add Stylelint rule or a lint pass: any element with `onClick` or `<button>` must have `min-h-touch` (or descend from a base `<Button>` that has it). Track via ESLint custom rule on JSX—out of scope for v1, manual audit OK.

---

## 11. Charts on Mobile

Recharts `ResponsiveContainer` handles width but doesn't fix label collisions. Visible in screenshot 07 (`Last 6 Months` chart — fine), but multi-series charts on dashboard get worse.

Strategy:

```tsx
const isMobile = useIsMobile();
<LineChart>
  <XAxis interval={isMobile ? 'preserveStartEnd' : 0}
         tick={{ fontSize: isMobile ? 10 : 12 }}
         tickFormatter={isMobile ? shortDate : fullDate} />
  <YAxis hide={isMobile} />
  {!isMobile && <Legend />}
  {!isMobile && <CartesianGrid />}
  <Tooltip />
  <Line dataKey="primary" />
  {!isMobile && <Line dataKey="secondary" />}
</LineChart>
```

For dashboard charts wrapped in cards: header includes an expand icon (`Maximize2`) that opens the chart in a sheet modal at full viewport — gives the user a way to see the full multi-series chart with legend on demand.

Wrap with a reusable `<ChartCard title expandable>` component in `app/src/components/dashboard/`.

---

## 12. Dashboard Mobile Composition

`DashboardPage.tsx` is currently a vertical stack of: alerts → portfolio overview controls → KPI cards → multiple charts → tables. On mobile (screenshot 03) the user lands on `Pending Extensions` (a relatively rare alert), then scrolls past time-range controls before seeing any KPI.

Reorder + collapse for `<lg`:

```tsx
<>
  <PageHeader title="Dashboard" />
  <KpiGrid />                                {/* always first on mobile */}
  <AlertsSection />                          {/* extensions, overdue payments */}
  <CollapsibleCard title="Portfolio Overview" defaultOpen={!isMobile}>
    <TimeRangeControls />
    <CashFlowChart compact={isMobile} />
  </CollapsibleCard>
  <CollapsibleCard title="Top Performers" defaultOpen={false}>
    <PropertyPerformanceTable mobileLimit={3} />
  </CollapsibleCard>
  <CollapsibleCard title="Upcoming Renewals" defaultOpen={false}>
    <UpcomingRenewalsPanel mobileLimit={3} />
  </CollapsibleCard>
</>
```

KPI grid:
```tsx
<div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3 md:gap-4">
```

The current iPad screenshot (16) shows 2-col KPIs which is fine — keep `md:grid-cols-2 lg:grid-cols-3`. Phone gets 2 per row but with smaller text:

```tsx
<KpiCard valueClass="text-xl xs:text-2xl md:text-3xl font-bold" />
```

Currently the KPI values are `text-3xl` which produces `€9,671,330` overflowing on 360px — drop to `text-2xl` until `md`.

---

## 13. Typography & Spacing

Audit pass — change globally where applicable (via codemod or `<PageHeader>` consolidation):

| Element       | Current               | Proposed                                |
|---------------|-----------------------|-----------------------------------------|
| Page title    | `text-3xl font-bold`  | `text-2xl md:text-3xl font-bold`        |
| Page icon     | `h-8 w-8`             | `h-6 w-6 md:h-8 md:w-8`                 |
| Subtitle      | `text-base`           | `text-sm md:text-base`                  |
| Card title    | `text-lg font-semibold` | `text-base md:text-lg font-semibold`  |
| KPI value     | `text-3xl`            | `text-2xl md:text-3xl`                  |
| Section gap   | `mb-6`                | `mb-4 md:mb-6`                          |
| Page padding  | `px-4 py-8`           | `px-4 py-4 md:px-6 md:py-6 lg:px-8 lg:py-8` |
| Card padding  | `p-6`                 | `p-4 md:p-6`                            |
| Form grid gap | `gap-4`               | `gap-3 md:gap-4`                        |

`PropertyListPage.tsx:128` has `px-4 py-8` — drop `py-8` to `py-4` on mobile.

---

## 14. Safe Areas

CSS variables already proposed in section 2 (`--safe-top`, `--safe-bottom`). Apply at:

- `Layout.tsx` — outer `<main>` padding-bottom when bottom nav present
- `Sidebar.tsx` (mobile drawer) — top/bottom padding
- `BottomTabBar.tsx` — `pb-[var(--safe-bottom)]`
- `ModalWrapper.tsx` sheet variant — sticky header `pt-[var(--safe-top)]`, sticky footer `pb-[var(--safe-bottom)]`
- `PageHeader.tsx` (sticky) — `pt-[var(--safe-top)]`
- Any fixed overlay (`Toast`, `Snackbar`) — top/bottom inset

Add a single utility class for convenience:

```css
@layer utilities {
  .pt-safe { padding-top: env(safe-area-inset-top); }
  .pb-safe { padding-bottom: env(safe-area-inset-bottom); }
  .min-h-screen-safe { min-height: 100dvh; }
}
```

---

## 15. Viewport / Meta

`frontend/app/index.html:9` is currently:
```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```

Change to:
```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, maximum-scale=5" />
<meta name="theme-color" content="#ffffff" media="(prefers-color-scheme: light)" />
<meta name="theme-color" content="#0a0a0a" media="(prefers-color-scheme: dark)" />
<meta name="mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
```

Notes:
- **Do NOT** disable user scaling (`maximum-scale=1, user-scalable=no`) — accessibility regression. `maximum-scale=5` allows pinch-zoom up to 500% while preventing iOS's auto-zoom on focusing inputs with font-size <16px.
- Separately, ensure all `<input>` fields use `text-base` (16px) or larger to avoid iOS auto-zoom-on-focus. Audit `Input.tsx` and `Select.tsx` in `packages/ui`.
- `viewport-fit=cover` enables `env(safe-area-inset-*)` to return real values on notched devices.

---

## 16. Bottom Tab Bar — Recommendation: **Yes, iPhone-only (`<md`)**

Trade-off: iPhone users tap "primary nav" multiple times per session. Drawer adds ~600ms friction (tap menu → wait animation → tap target → wait close). Bottom tab is one tap. Cost: 60px of vertical space and a second navigation surface to keep in sync.

Recommendation: **yes** on phones (`<md`), **no** on iPad (rail sidebar fills that need).

Design — 5 tabs (the 4 most-used routes plus More):

```tsx
const tabs = [
  { to: '/dashboard',  label: 'Home',     icon: LayoutDashboard },
  { to: '/properties', label: 'Props',    icon: Home },
  { to: '/contacts',   label: 'Contacts', icon: Users },
  { to: '/payments',   label: 'Money',    icon: DollarSign },
  { onClick: openDrawer, label: 'More',   icon: Menu },
];

<nav
  className={clsx(
    'md:hidden fixed inset-x-0 bottom-0 z-30',
    'bg-surface-card/95 backdrop-blur-xl border-t border-border-default',
    'pb-[var(--safe-bottom)]',
    'flex items-stretch justify-around',
  )}
  role="navigation"
  aria-label="Primary"
>
  {tabs.map(t => (
    <NavLink
      key={t.label}
      to={t.to ?? '#'}
      onClick={t.onClick}
      className={({ isActive }) => clsx(
        'flex-1 flex flex-col items-center justify-center gap-0.5',
        'min-h-touch py-2 text-xs',
        isActive
          ? 'text-primary-600 dark:text-primary-300'
          : 'text-text-secondary',
      )}
    >
      <t.icon className="h-5 w-5" />
      <span className="leading-none">{t.label}</span>
    </NavLink>
  ))}
</nav>
```

Active state: solid color icon + label. Hairline indicator on top (`before:absolute before:top-0 before:h-0.5 before:bg-primary-500 before:w-8`) when active. Hamburger button in `<PageHeader>` is retained on `<md` but used less often since More tab also opens it.

Sync rules:
- Tabs reflect feature flags (e.g., Reports tab only if enabled — but with 5 fixed slots, prefer keeping these stable and putting Reports in More)
- Persist last visited route per tab so switching tabs resumes scroll

---

## Implementation Sequence (Phased)

### Phase 1 — Foundation (1–2 weeks)
- Add `xs` breakpoint, `--safe-top/bottom/header-h`, `min-h-touch` tokens in `theme.css`
- Fix `index.html` viewport meta + theme-color
- Refactor `Layout.tsx` to mobile-first shell with sticky header slot and `MobileNavContext`
- Refactor `Sidebar.tsx`: move trigger out, add iPad rail variant (`md:flex lg:hidden`), swipe-to-close, focus trap, drawer width to `min(18rem,85vw)`
- Build `<PageHeader>` in `packages/ui` and migrate 3 reference pages (Dashboard, Properties, Contacts) to validate API
- Audit and bump `<input>` font-size to 16px+
- Bottom Tab Bar (gated behind a feature flag `MOBILE_BOTTOM_NAV` for staged rollout)

### Phase 2 — Core screens (2–3 weeks)
- Migrate remaining ~22 pages to `<PageHeader>`
- Build `<ResponsiveTable>` + `<DataList>` and convert hot pages first: PaymentsPage, ExpensesPage, ContactFinancialsTab, PropertyContractsTab, DocumentsPage, AuditLogPage
- Build `<FilterSheet>` + `<ChipGroup>` and convert PropertyListPage, ContactListPage, ContractListPage filter blocks
- Add `variant="sheet"` to `ModalWrapper`; convert Property/Contract/Tenant/Payment/Expense form modals
- PropertyCard / ContactCard grid + typography tweaks
- Dashboard mobile composition (reorder + collapsible cards + KPI text scale)

### Phase 3 — Polish (1 week)
- Touch target audit pass across `packages/ui` (IconButton, Chip, Checkbox, Pagination, DatePicker)
- Chart mobile mode + expand-to-sheet
- Migrate remaining tables (admin/wizard tables)
- Visual QA on iPhone SE (320), iPhone 14 (390), iPad portrait (768), iPad landscape (1024), Pixel 7
- Add Playwright responsive snapshot tests at 4 widths
- Lighthouse mobile pass, target Performance >85, Accessibility >95

---

## Risks & Gotchas

- **Tailwind v4 arbitrary screens**: `min-h-touch` cannot be used as an arbitrary modifier; either define utilities in `@layer utilities` (as shown) or use `min-h-[var(--size-touch)]`. Decide once and document.
- **`100dvh` browser support**: iOS Safari 16+, Chrome Android 108+. Add fallback `min-h-screen` first, then `min-h-dvh` overriding. Test on iOS 15.
- **`viewport-fit=cover` + iOS bottom inset**: any element that was previously flush at the bottom (existing footers, toast container) will look "floating" until you add `pb-[var(--safe-bottom)]`. Sweep all `fixed bottom-0` usages.
- **Sticky header + iOS Safari backdrop-blur**: combine with `transform: translateZ(0)` or `will-change: transform` to avoid white flash on scroll. Limit to mobile to skip the perf cost on desktop.
- **Drawer swipe vs page scroll**: swipe-to-close must use `touchmove` with `passive: false` only when the gesture starts within 24px of the left edge — otherwise it competes with horizontal scroll inside tables.
- **Bottom tab bar + keyboard**: on iOS the keyboard pushes the visual viewport up, but `fixed` elements get hidden behind it. Use `visualViewport.height` to hide the tab bar while the keyboard is open: `if (visualViewport.height < window.innerHeight - 100) hide()`.
- **`ModalWrapper` sheet variant + body scroll lock**: existing modals likely already lock scroll; if they use `overflow:hidden` on `<body>`, that conflicts with the dynamic viewport. Switch to `overscroll-contain` on the sheet's scroll container and skip the body lock.
- **`<PageHeader>` migration is mechanical but high-blast-radius**: do it behind a per-page branch flag or merge in batches of 5 pages with visual QA each batch — easier to revert.
- **iPad rail + collapsed desktop state**: avoid two sources of truth for "collapsed" — let `md` always be rail-mode, `lg` honor the user's localStorage toggle. The toggle button should be hidden below `lg`.
- **Recharts `<Legend hide={...}>` and dynamic remounts**: toggling on resize re-creates the SVG; debounce the `useIsMobile` hook by 100ms to avoid layout thrash during browser-window resizes.
- **Translation keys**: `<PageHeader>` consolidates action labels; ensure `t('list.addButton')` calls still resolve and that long German/Dutch labels (`Eigenschaft hinzufügen`) don't break the mobile single-CTA layout. Use `truncate` + `aria-label` on compact buttons.
- **Form pages already exist as routes (not modals)**: screenshot 15 confirms `PropertyForm` is a route. Don't accidentally convert these to sheets; only convert routes that are conceptually modal (quick add, confirmations).
