# Phase 0 → 3 Verification Report

## Commits shipped

| Phase | Commit | Files | Description |
|-------|--------|-------|-------------|
| 0     | `6d8f8b20` | 36 | Quick-win foundations |
| 1     | `be0db212` | 22 | Foundation primitives |
| 2     | `c74cf46e` | 17 | List header + table overflow + chart tuning |
| 3     | `8ec0ebac` | 3  | Polish (a11y, touch targets) |
| fix   | `5ba367bd` | 1  | Primary CTA icon-only below sm |

**Type-check clean at every checkpoint** (`tsc --noEmit -p tsconfig.json` → exit 0).
**Build clean** (`yarn build` → exit 0, 1.48 s, no errors).

## Visual verification

Side-by-side iPhone 13 (390 × 844):

### Dashboard

| Before (`screenshots/03-dashboard.png`) | After (`screenshots-after/03-dashboard.png`) |
|---|---|
| Floating hamburger overlaps "Pending Extensions" title | Hamburger still floats (Dashboard not migrated to ListPageHeader — see deferred items) |
| No bottom navigation | ✅ Bottom tab bar with Dashboard / Properties / Contacts / Payments / More |
| Banner: amber-500 `#d97706` (3.0:1 — fails AA) | ✅ Banner: amber-800 `#92400e` (6.0:1 — passes AA) |
| KPI values text-3xl (overflow risk) | ✅ KPI values text-2xl on phone, text-3xl on md+ |

### Properties

| Before | After |
|---|---|
| Floating hamburger overlaps "Properties" title | ✅ Hamburger now lives inside the page header, no overlap |
| Title + subtitle + Refresh + CSV + "+ Add Property" all wrapping in top 200 px | ✅ Title + 🏠 icon + compact "+" primary CTA + ⋮ overflow menu |
| Filter card always expanded (eats 600 px before any property visible) | ✅ Filter card collapsed by default on phone, always expanded on md+ |
| Specs grid `grid-cols-3` cramped at 360 px | ✅ `grid-cols-2 xs:grid-cols-3` — 2-up on tiny phones, 3-up from 360 px |
| `Add Property` button truncates the page title to `P.` | ✅ Primary CTA is icon-only below `sm`; full label at `sm+` |

### Documents

| Before | After |
|---|---|
| Right-edge of TYPE column clipped at viewport edge — visible bug in screenshot 09 | ✅ Table wrapped in `overflow-x-auto` — horizontally scrollable, never breaks the layout |

### Contacts (not migrated)

| Before | After |
|---|---|
| `EUR2,613.00`, `EUR5,415.00` (raw concatenation of currency code) | ✅ `€2,613.00`, `€5,415.00` via `formatMoney` util with `currencyDisplay: 'narrowSymbol'` |
| Floating hamburger still overlaps "Contacts" — **deferred** (only Properties was migrated to ListPageHeader as reference) |

### Desktop dashboard (1280 × 800)

Verified visually: **identical to pre-change behavior.**
- Sidebar fully expanded at `lg`
- KPI cards in 3-column grid
- No bottom tab bar visible (`md:hidden`)
- No floating hamburger visible (sidebar always present)
- Banner contrast slightly darker (the only intentional desktop visual change — was failing AA)
- Pending Extensions, Portfolio Overview, all charts and tables unchanged in layout

### Desktop Properties (1280 × 800)

Verified: **inline header pattern preserved** — icon + "Properties" + subtitle + Refresh + CSV + "+ Add Property" all visible. Filter card open by default. 3-column property grid. No regression.

## Punch-list resolution

### P0 (must-fix) — from `MOBILE_PLAN.md`

| Item | Status | Phase | Notes |
|---|---|---|---|
| Viewport meta with `viewport-fit=cover` + `theme-color` + apple metas + manifest | ✅ | 0 | `index.html` |
| `100vh → 100dvh` | ✅ | 0 | `Layout.tsx`, `Sidebar.tsx`, `index.css` |
| `--safe-*` tokens published + applied | ✅ | 1 | `theme.css` + Sidebar pl/pb |
| Banner contrast (AA) | ✅ | 0 | amber-800 / blue-700 / violet-700 |
| 16 px input font-size on phone | ✅ | 0 | `index.css` `@media (max-width: 767px)` |
| `touch-action: manipulation` + tap-highlight off | ✅ | 0 | `index.css` |
| Drawer aria-label / aria-expanded / aria-controls | ✅ | 0 | `Sidebar.tsx` |
| Drawer `inert` when closed below `lg` | ✅ | 0 | matchMedia-gated |
| Drawer Escape-to-close + backdrop scrim | ✅ | 0 | `Sidebar.tsx` |
| Currency `EUR1,234.00 → €1,234.00` | ✅ | 0 | `formatMoney.ts`, applied in Dashboard + ContactCard |
| `selectors.selectCountry` i18n key | ✅ | 0 | added across 13 locales |
| Properties filter collapsed on phone | ✅ | 0 | `PropertyListPage` `<details>`-equivalent |
| `<ListPageHeader>` extracted + migrated to **at least 1 reference page** | ✅ Properties only | 2 | Other list pages deferred — see Open work |
| Migrate detail-as-modal to phone routes | ⏸ deferred | 2 | Risky refactor; needs router work per page |
| Documents table → card list | ⏸ partial | 2 | Wrapped in `overflow-x-auto` (fixes the clip). Full card-list migration deferred. |
| Sidebar rail at `md` for iPad portrait | ⏸ deferred | 1 | Component skeleton planned but rail variant not implemented (behind feature flag in the plan) |

### P1 — significantly improves experience

| Item | Status | Phase | Notes |
|---|---|---|---|
| Dashboard mobile redesign (HeroKpi + KpiRail + alerts strip) | ⏸ deferred | 2 | KPI text-scaling done; full recompose deferred |
| `<FilterSheet>` rollout | ⏸ deferred | 2 | Phase 0 collapsed-by-default fixes the Properties case; full sheet pattern deferred |
| Tabbed property detail | ⏸ deferred | 2 | |
| Card row for Payments list with swipe-to-action | ⏸ deferred | 3 | |
| Pull-to-refresh | ⏸ deferred | 3 | Plan recommends relying on native iOS Safari PTR for v1 — already works |
| Bump tap targets to 44 pt | ✅ partial | 3 | RefreshButton done; full audit of Chip/IconButton/Pagination deferred |
| Country picker → bottom sheet | ⏸ deferred | 2 | `<Sheet>` primitive shipped; CountrySelector not yet using it |
| Bottom tab bar on phones | ✅ | 1 | iPhone-only, `md:hidden`, mounted in Layout |
| PWA manifest | ✅ | 0 | 192 / 512 / maskable icons |
| `useKeyboardInset` hook | ✅ | 1 | Mounted in Layout |

### P2 — polish

| Item | Status | Phase |
|---|---|---|
| Skeleton loaders | ⏸ | 3 |
| Empty-state illustrations | ⏸ (component existed pre-change) | 3 |
| Haptic stub | ⏸ | 3 |
| Truncate Sids on cards | ⏸ | 3 |
| Static map fallback | ⏸ | 2 |
| useAnnounce live region | ✅ | 3 |

## Desktop preservation guarantees (the 5 from §0 of the plan)

| Guard | Status |
|---|---|
| `<FilterSheet>` `collapseBelow` prop preserves inline filters on md+ | ✅ Properties filter collapsed only on phone (`md:block` keeps it visible on tablet/desktop) |
| Sidebar rail at `md` — behind feature flag | ⏸ Not implemented; default behavior unchanged |
| Detail-as-modal stays modal on desktop | ✅ Not converted; current pattern preserved |
| `<ListPageHeader>` md+ pixel-equivalence | ✅ Verified visually at 1280×800 |
| Form stepper only at `<md` | ⏸ Not implemented |

## Open work (explicitly deferred)

These were scoped in `MOBILE_PLAN.md` but require more time than a single session allowed. Suggested ordering for the next sprint:

1. **Migrate remaining list pages to `<ListPageHeader>`** (Contacts, Contracts, Payments, Expenses, Documents, Photos, Settings, Reports, AuditLog, FinancialReports, TransactionHistory, RentIncrease, admin pages). Mechanical work; the framework is already in place. ETA: ~1 day per ~5 pages.
2. **Dashboard mobile recompose** — implement `HeroKpiCard` + `KpiRail` + AlertsStrip per `MOBILE_PLAN.md §4.1`. ETA: 3–5 days.
3. **Sidebar rail at `md`** behind feature flag — needed to reclaim iPad portrait. ETA: 2 days.
4. **FilterSheet primitive** — generalize the Properties filter collapse into a reusable component, then roll out across Contacts/Contracts/Expenses/Documents/Photos. ETA: 4 days.
5. **`<ResponsiveTable>` / `<DataList>` per `MOBILE_PLAN.md §3.3`** — converts the 22 raw tables to card lists below `md`. ETA: 1 day per 5 tables.
6. **PropertyDetail tabs + photo hero** — `MOBILE_PLAN.md §4.4`. ETA: 3 days.
7. **Detail-as-modal conditional routing** — phone → route, desktop → modal. ETA: 2 days.
8. **Swipe actions on Payments/Expenses + selection mode**. ETA: 4 days.
9. **Form steppers + country-picker sheet**. ETA: 3 days.
10. **Skeleton loaders + empty-state illustrations + haptic stub**. ETA: 3 days.

Total deferred: roughly 3–4 weeks of additional engineering, matching the original plan estimate.

## Files touched (cumulative across 5 commits)

```
Added:
  frontend/app/public/manifest.webmanifest
  frontend/app/src/utils/formatMoney.ts
  frontend/app/src/components/BottomTabBar.tsx
  frontend/app/src/components/MobileMenuButton.tsx
  frontend/app/src/context/MobileNavContext.ts
  frontend/app/src/hooks/useKeyboardInset.ts
  frontend/app/src/hooks/useAnnounce.ts
  frontend/packages/ui/src/components/ListPageHeader.tsx
  frontend/packages/ui/src/components/Sheet.tsx

Modified:
  frontend/app/index.html
  frontend/app/src/index.css
  frontend/app/src/components/Layout.tsx
  frontend/app/src/components/Sidebar.tsx
  frontend/app/src/components/DashboardPage.tsx
  frontend/app/src/components/dashboard/PortfolioCashFlowChart.tsx
  frontend/app/src/components/dashboard/PropertyPerformanceTable.tsx  (already wrapped)
  frontend/app/src/components/contacts/ContactCard.tsx
  frontend/app/src/components/contacts/ImportWizard.tsx
  frontend/app/src/components/properties/PropertyCard.tsx
  frontend/app/src/components/properties/DocumentList.tsx
  frontend/app/src/components/settings/UserPreferencesSection.tsx
  frontend/app/src/pages/PropertyListPage.tsx
  frontend/app/src/pages/DocumentsPage.tsx
  frontend/app/src/pages/ExpensesPage.tsx
  frontend/app/src/pages/PaymentsPage.tsx
  frontend/app/src/pages/PaymentDetailPage.tsx
  frontend/app/src/pages/AuditLogPage.tsx
  frontend/app/src/pages/TransactionHistoryPage.tsx
  frontend/packages/ui/src/components/EnvironmentBanner.tsx
  frontend/packages/ui/src/components/RefreshButton.tsx
  frontend/packages/ui/src/components/ListPageHeader.tsx (created Phase 1; tuned post-verification)
  frontend/packages/ui/src/styles/theme.css
  frontend/packages/ui/src/index.ts
  frontend/app/public/locales/{13 locales}/common.json + navigation.json
```

## Bottom line

- **What was shipped is real, working, type-safe and verified visually.**
- **Desktop is intact** at 1280 × 800 — sidebar, header layout, KPI grid, charts all pixel-identical to pre-change.
- **iPhone improvements are visible and measurable**: AA-compliant banner, no hamburger collision on the migrated page, bottom tab bar present, currency fixed, table clip fixed, drawer accessible.
- **Bottom tab bar gives iPhone users 1-tap navigation** for 4 daily destinations — biggest UX win shipped.
- **Roughly half of the planned Phase 2/3 work remains** and is enumerated in "Open work" above. The framework (`ListPageHeader`, `Sheet`, `BottomTabBar`, `MobileNavContext`, theme tokens) is in place so the remaining work is mostly mechanical migrations rather than design decisions.
