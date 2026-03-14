# Buurman Design System

> Version 1.0 — 2026-03-14
> Authored by design review panel (Maya Chen, Jonas Eriksson, Sarah Park)
> Engineering review by Alex Rivera

## Brand Identity

### Personality

Buurman ("neighbor" in Dutch) serves small landlords managing a handful of properties. The interface should feel:

- **Grounded and Trustworthy**: Real estate is people's largest asset. The UI must feel stable and reliable without being sterile.
- **Warm Professional**: Competent but approachable — like a capable advisor, not a corporate ERP.
- **Distinctly European**: Influenced by Dutch design tradition — clean, functional, warm neutrals, considered restraint.

### Differentiators

| Competitor | Their Style | Buurman Difference |
|---|---|---|
| Buildium | Corporate blue, dense enterprise UI | Warmer, more spacious, less overwhelming |
| AppFolio | Clinical green, white-heavy, generic | Richer surfaces, deeper palette, personality |
| Stessa | Minimal green/white, investor-focused | More warmth, property-management focus |
| TenantCloud | Bright blue, cluttered | Clean hierarchy, better information density |

### Signature Elements

1. **Ocean Blue + Warm Amber** color combination — professional and trustworthy in the property management space
2. **Warm neutrals** (stone tones) instead of cold grays — subconsciously conveys "home"
3. **Restrained color use** — primary blue in ~5-10% of visual field (nav, buttons, links, key indicators)
4. **Pill highlight** on active sidebar items — rounded background fill with primary-tinted color and semibold weight, modern and clean
5. **Tabular numerals** everywhere financial data appears (`font-variant-numeric: tabular-nums`)

---

## Color Palette

### Primary: Ocean ("Maritime")

Ocean blue conveys trust, stability, and professionalism — the ideal foundation for a property management platform. Deep and authoritative without being corporate-cold, paired with warm stone neutrals.

| Token | Hex | Usage |
|---|---|---|
| `primary-50` | `#f0f9ff` | Background highlights, selected row bg |
| `primary-100` | `#e0f2fe` | Hover backgrounds, light badges |
| `primary-200` | `#bae6fd` | Progress bars, light accents |
| `primary-300` | `#7dd3fc` | Active states (light mode) |
| `primary-400` | `#38bdf8` | Icons on light backgrounds |
| `primary-500` | `#0284c7` | **DEFAULT** — primary buttons, links, active nav |
| `primary-600` | `#0369a1` | Button hover state |
| `primary-700` | `#075985` | Button active/pressed |
| `primary-800` | `#0c4a6e` | Dark mode text accents |
| `primary-900` | `#082f49` | Sidebar active bg (dark) |
| `primary-950` | `#041c2c` | Deepest dark mode accents |

### Accent: Warm Amber ("Hearth")

Amber/gold conveys warmth, home, and value. Complementary to ocean blue. Used for CTAs that need contrast, notifications, and financial highlights.

| Token | Hex | Usage |
|---|---|---|
| `accent-50` | `#fffbeb` | Light badge bg |
| `accent-100` | `#fef3c7` | Notification backgrounds |
| `accent-200` | `#fde68a` | Highlights |
| `accent-300` | `#fcd34d` | Active accent elements |
| `accent-400` | `#fbbf24` | Badges, highlights |
| `accent-500` | `#f59e0b` | **DEFAULT** — financial indicators |
| `accent-600` | `#d97706` | Accent hover |
| `accent-700` | `#b45309` | Dark text on accent |
| `accent-800` | `#92400e` | Strong emphasis |
| `accent-900` | `#78350f` | Deepest accent |

### Neutral: Warm Stone

Pure grays feel cold. These neutrals have a subtle warm undertone (toward brown/taupe) that feels grounded — appropriate for property management.

| Token | Hex | Usage |
|---|---|---|
| `neutral-0` | `#ffffff` | Card surfaces (light) |
| `neutral-25` | `#fafaf9` | Page background (light) |
| `neutral-50` | `#f5f5f4` | Recessed surfaces, input bg |
| `neutral-100` | `#e7e5e4` | Borders (light), dividers |
| `neutral-200` | `#d6d3d1` | Disabled bg, heavy dividers |
| `neutral-300` | `#a8a29e` | Placeholder text, muted icons |
| `neutral-400` | `#78716c` | Secondary icons, disabled text |
| `neutral-500` | `#57534e` | Secondary body text |
| `neutral-600` | `#44403c` | Body text (light mode) |
| `neutral-700` | `#292524` | Headings (light mode) |
| `neutral-800` | `#1c1917` | High-emphasis text |
| `neutral-900` | `#171412` | App background (dark) |
| `neutral-950` | `#0c0a09` | Deepest dark surface |

### Semantic Colors

Each semantic color has light and dark mode variants for text, background tint, and border.

#### Success
| Context | Light | Dark |
|---|---|---|
| Text | `#059669` | `#34d399` |
| Background | `#ecfdf5` | `#064e3b` at 20% opacity |
| Border | `#a7f3d0` | `#065f46` |

#### Warning
| Context | Light | Dark |
|---|---|---|
| Text | `#d97706` | `#fbbf24` |
| Background | `#fffbeb` | `#78350f` at 20% opacity |
| Border | `#fde68a` | `#92400e` |

#### Error
| Context | Light | Dark |
|---|---|---|
| Text | `#dc2626` | `#f87171` |
| Background | `#fef2f2` | `#7f1d1d` at 20% opacity |
| Border | `#fecaca` | `#991b1b` |

#### Info
| Context | Light | Dark |
|---|---|---|
| Text | `#0284c7` | `#38bdf8` |
| Background | `#f0f9ff` | `#0c4a6e` at 20% opacity |
| Border | `#bae6fd` | `#075985` |

### Surface Colors

| Token | Light | Dark |
|---|---|---|
| `surface-page` | `#fafaf9` | `#0c0a09` |
| `surface-card` | `#ffffff` | `#1c1917` |
| `surface-raised` | `#ffffff` | `#292524` |
| `surface-overlay` | `rgba(12, 10, 9, 0.4)` | `rgba(0, 0, 0, 0.6)` |
| `surface-inset` | `#f5f5f4` | `#171412` |

### Text Hierarchy

| Token | Light | Dark |
|---|---|---|
| `text-primary` | `#1c1917` | `#e7e5e4` |
| `text-secondary` | `#57534e` | `#a8a29e` |
| `text-muted` | `#78716c` | `#78716c` |
| `text-disabled` | `#a8a29e` | `#57534e` |
| `text-inverse` | `#fafaf9` | `#1c1917` |
| `text-link` | `#0284c7` | `#38bdf8` |

### Backoffice Variant: Indigo ("Command")

The backoffice uses indigo to visually distinguish it from the ocean blue user-facing app. Same warm neutrals for content, indigo replaces ocean blue for interactive elements. Backoffice is permanently light-only (no dark mode).

| Token | Hex | Usage |
|---|---|---|
| `bo-primary-400` | `#818cf8` | Light accents |
| `bo-primary-500` | `#6366f1` | Buttons, links, active nav |
| `bo-primary-600` | `#4f46e5` | Button hover |
| `bo-primary-700` | `#4338ca` | Button active/pressed, active nav text |
| `bo-sidebar-bg` | `#f5f3ff` | Sidebar background (indigo-tinted — distinct from app's white) |
| `bo-sidebar-border` | `#e0e7ff` | Sidebar border (indigo-tinted) |
| `bo-sidebar-hover` | `#ede9fe` | Sidebar item hover |
| `bo-sidebar-active-bg` | `#eef2ff` | Active nav item fill |

The backoffice sidebar uses a visibly distinct indigo-tinted background (`#f5f3ff`) so users immediately recognize they are in the admin area — not the tenant-facing app. This distinction also carries through to the Keycloak login page (see Cross-Touchpoint Application).

---

## Typography

### Font Family

**Primary**: Satoshi (all uses). Neo-grotesque with character — distinctive without being distracting. Naturally uniform-width numerals make financial data highly scannable in dense layouts. Modern, confident identity that sets Buurman apart from generic SaaS interfaces.

- **Source**: Indian Type Foundry via [Fontshare](https://www.fontshare.com/fonts/satoshi) — free for commercial use (web, app, PDF embedding)
- **Loading**: Self-host font files in production; Fontshare CDN for development
- **Fallback stack**: `'Satoshi', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`
- **Emails**: Falls back to system sans-serif (standard practice — Gmail/Outlook ignore web fonts)
- **PDFs**: Embed Satoshi OTF/TTF files via iText7 font configuration

**Monospace**: System monospace stack. Reserve for code blocks and identifiers only. Financial figures use `font-variant-numeric: tabular-nums` on Satoshi (no font switch needed).

### Type Scale (Major Third — 1.250 ratio)

| Token | Size | Weight | Line Height | Letter Spacing | Usage |
|---|---|---|---|---|---|
| `display` | 36px / 2.25rem | 700 | 1.1 | -0.025em | Hero numbers, portfolio total value |
| `h1` | 28px / 1.75rem | 700 | 1.25 | -0.015em | Page titles |
| `h2` | 22px / 1.375rem | 600 | 1.25 | -0.015em | Section headers |
| `h3` | 18px / 1.125rem | 600 | 1.25 | 0 | Card titles, panel headers |
| `h4` | 16px / 1rem | 600 | 1.35 | 0 | Subsection titles |
| `body` | 14px / 0.875rem | 400 | 1.5 | 0 | Body text, descriptions, table cells |
| `caption` | 13px / 0.8125rem | 400 | 1.4 | 0 | Secondary info, table metadata |
| `small` | 12px / 0.75rem | 500 | 1.4 | 0 | Badges, timestamps |
| `overline` | 11px / 0.6875rem | 600 | 1.35 | 0.08em | Category labels, section labels (uppercase) |

**Note**: 14px body is intentional for data-dense dashboard UIs. Balances readability with density.

### Font Weights

| Weight | Value | Usage |
|---|---|---|
| Regular | 400 | Body text, descriptions, table cells |
| Medium | 500 | Labels, nav items, active states, input text |
| Semibold | 600 | Card titles, section headers, emphasis |
| Bold | 700 | Page headings, display numbers, KPIs |

Never use 300 (light) — compromises readability. Never use 800/900 — feels aggressive.

---

## Iconography

### Library: Lucide React

Keep Lucide. Consistent 2px stroke weight at 24px, comprehensive set, tree-shakeable.

### Icon Sizing Scale

| Token | Size | Usage |
|---|---|---|
| `icon-xs` | 14px / 0.875rem | Inline with small text, badge icons |
| `icon-sm` | 16px / 1rem | Inline with body text, table actions |
| `icon-md` | 20px / 1.25rem | **DEFAULT**: buttons, nav items, card headers |
| `icon-lg` | 24px / 1.5rem | Page header icons, standalone actions |
| `icon-xl` | 32px / 2rem | Empty state illustrations |
| `icon-2xl` | 48px / 3rem | Large empty states, onboarding |

Do not customize `strokeWidth` — keep Lucide defaults for visual consistency.

---

## Border Radius

| Token | Value | Usage |
|---|---|---|
| `radius-sm` | 4px / 0.25rem | Small badges, tags, inline chips |
| `radius-md` | 6px / 0.375rem | Inputs, selects, small buttons |
| `radius-lg` | 8px / 0.5rem | **DEFAULT**: buttons, cards, dropdowns |
| `radius-xl` | 12px / 0.75rem | Modal dialogs, large panels |
| `radius-2xl` | 16px / 1rem | Feature cards, hero sections |
| `radius-full` | 9999px | Avatars, dot indicators, pills |

### Specific Assignments

- **Cards**: `radius-lg` (8px) — professional, not playful
- **Buttons**: `radius-md` (6px) — crisp and clickable
- **Inputs**: `radius-md` (6px) — match buttons in forms
- **Badges/Status**: `radius-sm` (4px) — compact, information-dense
- **Avatars**: `radius-full` — always circular
- **Modals**: `radius-xl` (12px) — elevated surfaces get more roundness

---

## Shadow System

Warm-tinted shadows (matching stone neutrals). Subtle — heavy shadows feel dated.

### Light Mode

| Token | Value | Usage |
|---|---|---|
| `shadow-xs` | `0 1px 2px rgba(28,25,23,0.04)` | Buttons, badges |
| `shadow-sm` | `0 1px 3px rgba(28,25,23,0.06), 0 1px 2px rgba(28,25,23,0.04)` | Cards at rest, input focus |
| `shadow-md` | `0 4px 8px -2px rgba(28,25,23,0.06), 0 2px 4px -2px rgba(28,25,23,0.04)` | Card hover, dropdowns |
| `shadow-lg` | `0 12px 24px -4px rgba(28,25,23,0.08), 0 4px 8px -4px rgba(28,25,23,0.03)` | Modals, popovers |
| `shadow-xl` | `0 24px 48px -8px rgba(28,25,23,0.10), 0 8px 16px -4px rgba(28,25,23,0.04)` | Toast notifications, command palettes |
| `shadow-ring` | `0 0 0 3px rgba(20,184,146,0.15)` | Focus ring (primary) |
| `shadow-ring-error` | `0 0 0 3px rgba(220,38,38,0.15)` | Focus ring (error) |

### Dark Mode

| Token | Value |
|---|---|
| `shadow-xs` | `0 1px 2px rgba(0,0,0,0.2)` |
| `shadow-sm` | `0 1px 3px rgba(0,0,0,0.3), 0 1px 2px rgba(0,0,0,0.2)` |
| `shadow-md` | `0 4px 8px -2px rgba(0,0,0,0.4), 0 2px 4px -2px rgba(0,0,0,0.3)` |
| `shadow-lg` | `0 12px 24px -4px rgba(0,0,0,0.5), 0 4px 8px -4px rgba(0,0,0,0.3)` |
| `shadow-ring` | `0 0 0 3px rgba(45,212,172,0.2)` |

---

## Borders & Dividers

### When to Use What

| Technique | When | Example |
|---|---|---|
| Borders | Separating adjacent same-level elements | Table rows, sidebar sections |
| Spacing | Separating groups of related elements | Card groups on dashboard |
| Shadows | Indicating elevation | Cards on page, modal above content |
| Background change | Nesting or recessing content | Inset panels, code blocks |

### Border Tokens

| Token | Light | Dark |
|---|---|---|
| `border-default` | `neutral-100` (#e7e5e4) | `neutral-800` (#1c1917) |
| `border-subtle` | `neutral-50` (#f5f5f4) | `neutral-900` (#171412) |
| `border-strong` | `neutral-200` (#d6d3d1) | `neutral-700` (#292524) |

---

## Spacing System

### Base Unit: 4px

Use only these Tailwind spacing values. Avoid 5, 7, 9, 10, 14.

| Token | Value | Tailwind | Usage |
|---|---|---|---|
| `space-1` | 4px | `1` | Icon-to-label (compact), error message offset |
| `space-1.5` | 6px | `1.5` | Badge padding, label-to-input gap |
| `space-2` | 8px | `2` | Icon-to-text in buttons/nav, between badges, button groups |
| `space-3` | 12px | `3` | List item spacing, sidebar nav padding |
| `space-4` | 16px | `4` | Form field gaps, filter bar padding, mobile page gutter |
| `space-6` | 24px | `6` | **Card padding (standard)**, grid gap, header-to-content |
| `space-8` | 32px | `8` | Between page sections, desktop page gutter, form section separation |
| `space-12` | 48px | `12` | Major page-level separation |
| `space-16` | 64px | `16` | Empty state vertical padding |

### Page-Level Padding

Owned by the Layout component. Pages should NOT add their own padding.

| Breakpoint | Horizontal | Vertical |
|---|---|---|
| Mobile (< 768px) | `px-4` (16px) | `py-6` (24px) |
| Tablet (md) | `px-6` (24px) | `py-8` (32px) |
| Desktop (lg+) | `px-8` (32px) | `py-8` (32px) |

Tailwind: `px-4 py-6 md:px-6 md:py-8 lg:px-8`

### Standardized Spacing Rules

| Context | Value | Class |
|---|---|---|
| Between page sections | 32px | `space-y-8` |
| Within a card/section | 16-24px | `space-y-4` (compact) / `space-y-6` (comfortable) |
| Card header to body | 16px | `mb-4` |
| Card padding | 24px | `p-6` — no exceptions |
| Modal padding | 24px | `p-6` |
| Label to input | 6px | `mb-1.5` |
| Input to error message | 4px | `mt-1` |
| Form field group gap | 16px (h), 24px (v) | `gap-4` horizontal, `space-y-6` vertical |
| Button group gap | 8px | `gap-2` |
| Icon to label (buttons) | 8px | `gap-2` |
| Icon to label (compact) | 6px | `gap-1.5` |

---

## Layout System

### Content Width Strategy

- **Full-width**: List pages, dashboards, detail pages (data benefits from horizontal space)
- **Constrained**: Form pages (`max-w-3xl` / 768px), settings content (`max-w-3xl`)
- **Overall max**: `max-w-screen-2xl` (1536px) with `mx-auto` on the Layout content area. Prevents unreadable line lengths on ultra-wide monitors.

### Sidebar

| Property | App | Backoffice |
|---|---|---|
| Expanded width | `w-64` (256px) | `w-64` (256px) |
| Collapsed width | `w-20` (80px) | `w-20` (80px) |
| Mobile | Overlay with backdrop | N/A (desktop-only) |
| Transition | `transition-[width] duration-200 ease-out` | Same |

Never use `transition-all` on the sidebar — it transitions colors/backgrounds and causes jank.

### Page Templates

#### List Page
```
<div>
  <PageHeader ... />                           <!-- mb-6 -->
  <FilterCard className="p-6 mb-6" />          <!-- filters -->
  <p className="text-sm text-secondary mb-4">  <!-- result count -->
  <div className="grid gap-6 ...">             <!-- card grid -->
  <Pagination className="mt-6" />
</div>
```

#### Detail Page
```
<div>
  <PageHeader ... />                              <!-- mb-6, with back nav -->
  <div className="border-b border-border mb-6">   <!-- tabs -->
  <div className="space-y-6">                     <!-- tab content sections -->
    <div className="bg-surface rounded-xl border p-6"> <!-- section cards -->
  </div>
</div>
```

#### Form Page
```
<div className="max-w-3xl mx-auto">
  <BackButton + h1 className="mb-6" />
  <div className="bg-surface rounded-xl border p-6">
    <form className="space-y-8">                  <!-- sections -->
      <fieldset>
        <legend className="text-lg font-semibold mb-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField ... />
        </div>
      </fieldset>
      <div className="flex justify-end gap-2 pt-6 border-t">
    </form>
  </div>
</div>
```

#### Dashboard Page
```
<div className="space-y-8">
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"> <!-- metrics -->
  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">               <!-- charts -->
  <div className="bg-surface rounded-xl border p-6">                    <!-- table -->
</div>
```

---

## Grid System

### Dashboard Metric Cards
```
grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6
```

### Entity Card Grids (Properties, Tenants)
```
grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4
```

### Form Grids
```
grid grid-cols-1 md:grid-cols-2 gap-4
```
Never 3-column forms — cognitive load too high.

### Detail Page 2-Column
```
grid grid-cols-1 lg:grid-cols-2 gap-6
```

### Data Spec Grids (Key-Value Pairs)
```
grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4
```

---

## Responsive Design

### Breakpoints (Tailwind 4 Defaults)

| Breakpoint | Width | Target |
|---|---|---|
| (default) | < 640px | Mobile phones |
| `sm` | 640px | Large phones / small tablet |
| `md` | 768px | Tablet portrait |
| `lg` | 1024px | Tablet landscape / laptop |
| `xl` | 1280px | Desktop |
| `2xl` | 1536px | Wide desktop |

### Behavior at Each Breakpoint

| Breakpoint | Changes |
|---|---|
| Mobile | Single-column. Sidebar hidden (overlay). Page padding 16px. Modals become bottom sheets. |
| `sm` | Card grids 2-column. Metric cards 2-column. |
| `md` | Page padding 24px. Form grids 2-column. |
| `lg` | Sidebar visible (fixed). Card grids 3-column. Metric cards 4-column. Page padding 32px. |
| `xl` | Card grids 4-column. |
| `2xl` | Max-width constraint kicks in. |

### Table Responsive Behavior

- < 6 columns: horizontal scroll with `overflow-x-auto`
- Many columns: hide less important with `hidden md:table-cell`
- Very data-dense: card layout on mobile with `hidden sm:block` / `sm:hidden`

### Modal Responsive Behavior

- Desktop: centered dialog with max-width
- Mobile (< sm): full-width bottom sheet with `rounded-t-xl`, slide-up animation

### Touch Targets

Minimum 44x44px for all interactive elements.

| Element | Fix |
|---|---|
| Sidebar nav links | `py-3` (48px with text) |
| Filter buttons | `py-2.5` (min 44px) |
| Mobile menu button | `p-2.5` (44px) |
| Icon-only buttons | `min-h-[44px] min-w-[44px]` |

---

## Shared Component Library

All shared components live in `packages/ui/src/components/` and export via `@buurman/ui`.

### 1. StatusBadge

Generic badge with color variant system. Domain-specific wrappers compose it.

```typescript
type BadgeColorVariant =
  | 'gray' | 'red' | 'orange' | 'amber' | 'yellow' | 'green'
  | 'emerald' | 'teal' | 'cyan' | 'blue' | 'indigo' | 'violet' | 'purple' | 'rose';

interface StatusBadgeProps {
  label: string;
  color: BadgeColorVariant;
  size?: 'xs' | 'sm' | 'md';       // default 'sm'
  dot?: boolean;                     // leading dot indicator
  icon?: React.ReactNode;           // leading icon
  shape?: 'rounded' | 'pill';       // default 'rounded'
  className?: string;
}
```

Domain wrappers:
```typescript
// PaymentStatusBadge.tsx — entire file:
const STATUS_MAP: Record<PaymentStatus, { label: string; color: BadgeColorVariant }> = {
  PENDING:        { label: 'Pending',   color: 'amber' },
  PAID:           { label: 'Paid',      color: 'emerald' },
  OVERDUE:        { label: 'Overdue',   color: 'red' },
  // ...
};

export const PaymentStatusBadge = ({ status }: { status: PaymentStatus }) => {
  const { label, color } = STATUS_MAP[status];
  return <StatusBadge label={label} color={color} />;
};
```

### 2. FormField

Layout wrapper for label + input + error + hint. Does not own input state.

```typescript
interface FormFieldProps {
  label: string;
  htmlFor?: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactNode;
  labelPosition?: 'top' | 'left';   // default 'top'
  readOnly?: boolean;
  className?: string;
  colSpan?: 1 | 2;
}
```

Accessibility: `htmlFor` on label, `aria-describedby` for error/hint, `aria-required` on input.

### 3. EmptyState

Consistent empty data display.

```typescript
interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actions?: React.ReactNode;
  variant?: 'page' | 'section' | 'inline';  // default 'section'
  className?: string;
}
```

Icon uses `aria-hidden="true"`. Container uses `role="status"`.

### 4. ModalWrapper

Unified modal with focus trap, keyboard handling, and animation.

```typescript
interface ModalWrapperProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';  // default 'md'
  children: React.ReactNode;
  footer?: React.ReactNode;
  preventClose?: boolean;
  onSubmit?: () => void;                          // Cmd/Ctrl+Enter
  initialFocusRef?: React.RefObject<HTMLElement>;  // Radix handles default
  className?: string;
}
```

Built-in: focus trap, Escape to close, backdrop click, Cmd+Enter submit, enter/exit animations, scroll management, focus restoration on close.

Footer button order: Cancel (left) — Destructive (middle) — Primary (right).

### 5. DataTable

Type-safe sortable data table with built-in loading, empty state, and responsive behavior.

```typescript
interface ColumnDef<TRow> {
  id: string;
  header: string | (() => React.ReactNode);
  cell?: (row: TRow, rowIndex: number) => React.ReactNode;
  accessor?: (row: TRow) => string | number | Date | null;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  width?: string;
  pin?: 'left' | 'right';
  className?: string;
}

interface DataTableProps<TRow> {
  columns: ColumnDef<TRow>[];
  data: TRow[];
  rowKey: (row: TRow) => string;
  sort?: { columnId: string; direction: 'asc' | 'desc' };
  onSortChange?: (sort: SortState) => void;
  sortMode?: 'client' | 'server';
  selectionMode?: 'none' | 'single' | 'multi';
  selectedKeys?: Set<string>;
  onSelectionChange?: (keys: Set<string>) => void;
  rowActions?: RowAction<TRow>[];
  onRowClick?: (row: TRow) => void;
  loading?: boolean;
  loadingStyle?: 'skeleton' | 'spinner';
  emptyState?: React.ReactNode;
  pagination?: React.ReactNode;
  stickyHeader?: boolean;
  'aria-label'?: string;
  className?: string;
}
```

Note: DataTable is read-only display. BulkDataGrid (existing) is for editable spreadsheet entry. They are separate components.

### 6. MetricCard

Dashboard metric with trend indicator and optional click behavior.

```typescript
interface MetricCardProps {
  label: string;
  value: string;
  icon?: React.ReactNode;
  iconBg?: string;
  trend?: {
    direction: 'up' | 'down' | 'flat';
    label: string;
    sentiment?: 'positive' | 'negative' | 'neutral';
  };
  subtitle?: string;
  size?: 'sm' | 'md' | 'lg';
  alert?: boolean;                      // tints card background with semantic color
  alertSeverity?: 'warning' | 'critical'; // warning = amber bg, critical = red bg
  onClick?: () => void;
  href?: string;
  hint?: string;
  className?: string;
}
```

### 7. FilterBar

Composable filter bar with URL state sync.

```typescript
type FilterDef =
  | { type: 'toggle'; key: string; label: string; options: { value: string | undefined; label: string }[] }
  | { type: 'select'; key: string; label: string; options: { value: string; label: string }[] }
  | { type: 'search'; key: string; label: string; placeholder?: string; debounce?: number }
  | { type: 'dateRange'; key: string; label: string; presets?: string[] };

interface FilterBarProps {
  filters: FilterDef[];
  values: Record<string, string | undefined>;
  onChange: (values: Record<string, string | undefined>) => void;
  onReset?: () => void;
  className?: string;
}
```

Mobile (< md): collapses into "Filters" button with badge showing active count.

---

## Accessibility

### Focus Management

Standardize on `focus-visible` (not `:focus`) everywhere:
```css
outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 focus-visible:ring-offset-2
```

### Skip Navigation

Add to Layout, before sidebar:
```html
<a href="#main-content" class="sr-only focus:not-sr-only ...">Skip to main content</a>
```

### Landmark Regions

- `<aside aria-label="Sidebar navigation">` — sidebar
- `<nav aria-label="Main navigation">` — nav within sidebar
- `<main id="main-content">` — content area

### Heading Hierarchy

| Level | Usage | Style |
|---|---|---|
| `h1` | Page title (one per page) | `text-h1` |
| `h2` | Major sections | `text-h2` |
| `h3` | Card titles, subsections | `text-h3` |
| `h4` | Sub-subsections (rare) | `text-h4` |

### Color Contrast

WCAG AA minimum: 4.5:1 for normal text, 3:1 for large text.

**Known failures to fix:**
- Muted text `#9ca0b8` on `#f8f9fc` = ~3.1:1. Fix: darken to `#71778f`+
- Secondary text `#6b7194` on `#f8f9fc` = ~4.3:1. Fix: darken to `#5c6180`+

### Motion

Global reduced-motion rule:
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

### Screen Reader Patterns

- Toast container: `aria-live="polite"` with `role="status"`
- Loading states: `role="status"` with `<span class="sr-only">Loading...</span>`
- Form errors: `aria-live="assertive"` for error summaries

---

## Animation & Transitions

### Standardized Durations

| Token | Duration | Easing | Usage |
|---|---|---|---|
| `fast` | 100ms | `ease-out` | Hover color/bg changes |
| `normal` | 150ms | `ease-out` | Button press, focus ring |
| `moderate` | 200ms | `ease-out` | Dropdowns, chevron rotation |
| `slow` | 300ms | `ease-in-out` | Sidebar collapse, modal enter |

### Rules

- **Never use `transition-all`**. Always specify properties: `transition-colors`, `transition-shadow`, `transition-transform`, `transition-[width,margin]`
- **No page transitions**. Fast mounts are better for productivity tools.
- **Skeleton screens** for first-load. Inline spinners for mutations. Full-page spinner only for route-level Suspense.

### Component Animations

- **Modals**: Desktop: fade + scale (95% → 100%). Mobile bottom sheet: slide up.
- **Toasts**: Slide in from right (existing).
- **Dropdowns**: `opacity-0 scale-95` → `opacity-100 scale-100`, 200ms.

---

## Engineering Guidelines

### CSS Architecture (Tailwind v4)

**Kill `tailwind.config.js` — go all-in on `@theme`.**

Tailwind v4's `@theme` directive replaces the JS config. Define all tokens in `packages/ui/src/styles/theme.css` as the single source of truth. Delete both `app/tailwind.config.js` and `backoffice/tailwind.config.js`.

App and backoffice `index.css` simply import and source:
```css
@import '@buurman/ui/src/styles/theme.css';
@import 'tailwindcss';
@source "../../packages/ui/src";
```

### Dark Mode: Semantic CSS Custom Properties

Eliminate 90% of `dark:` prefixes. Define semantic tokens that auto-swap:

```css
:root {
  --surface-page: var(--color-neutral-25);
  --surface-card: var(--color-neutral-0);
  --text-primary: var(--color-neutral-800);
  --text-secondary: var(--color-neutral-500);
  --border-default: var(--color-neutral-100);
}
.dark {
  --surface-page: var(--color-neutral-950);
  --surface-card: var(--color-neutral-800);
  --text-primary: var(--color-neutral-100);
  --text-secondary: var(--color-neutral-300);
  --border-default: var(--color-neutral-800);
}
```

Components use `bg-surface-page`, `text-primary`, `border-default` — no `dark:` needed. Reserve `dark:` for occasional one-offs.

### Backoffice Color Variant

Single CSS file override. Create `packages/ui/src/styles/theme-backoffice.css`:
```css
.theme-backoffice {
  --color-primary-400: #818cf8;
  --color-primary-500: #6366f1;
  --color-primary-600: #4f46e5;
  --color-primary-700: #4338ca;
}
```

Backoffice root adds `className="theme-backoffice"`. Same components, different primary color.

### Required Dependencies

| Package | Purpose | Size |
|---|---|---|
| `@radix-ui/react-dialog` | ModalWrapper primitive (focus trap, a11y) | 4.2kB gzip |
| `tailwind-merge` | Resolve conflicting Tailwind classes | 3.5kB gzip |
| `clsx` | Conditional className composition | 0.3kB gzip |

Create a `cn()` utility in `packages/ui`:
```ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
export const cn = (...inputs: ClassValue[]) => twMerge(clsx(inputs));
```

### Focus Ring Utility

Define a reusable focus ring in `theme.css`:
```css
@utility focus-ring {
  outline: none;
  &:focus-visible {
    box-shadow: 0 0 0 2px var(--surface-card), 0 0 0 4px var(--color-primary-500);
  }
}
```

Every interactive element uses `focus-ring` instead of repeating 3+ classes.

### Component Architecture Rules

1. **Use `@radix-ui/react-dialog`** for ModalWrapper. Do not hand-roll focus trap.
2. **Build DataTable from scratch** — do not wrap TanStack Table (overkill for read-only display tables).
3. **React 19**: Use `ref` as a regular prop (drop `forwardRef`). Use `use()` for context.
4. **Always set `displayName`** on shared components.
5. **No `as` prop / polymorphism** — compose internally based on `onClick`/`href` props.
6. **FilterBar URL sync** via a `useFilterState` hook wrapping `useSearchParams`, not a `syncToUrl` prop.
7. **Skeleton loading**: CSS-only `@keyframes shimmer` animation, thin React wrapper.
8. **Modal animations**: CSS transitions via Radix `data-[state]` attributes. No Framer Motion (32kB).
9. **No Storybook** until 25+ components. Use a `/dev` route instead.

### Additional Shared Components Needed

Beyond the 7 specified in the component library section:

| Component | Rationale |
|---|---|
| `Card` | Eliminates repeated `bg-surface-card rounded-lg border p-6` across hundreds of elements |
| `Input` | Standardizes input styling, dark mode, focus ring — eliminates CSS overrides in index.css |
| `Select` | Same as Input |
| `Textarea` | Same as Input |
| `Skeleton` | CSS shimmer animation wrapper for loading states |
| `Toast` | Already exists in app — promote to shared (used by both app and backoffice) |

### API Corrections (from engineering review)

- **MetricCard `iconBg`**: Change from `string` to `iconBgVariant?: 'primary' | 'accent' | 'success' | 'warning' | 'error' | 'info'`. Raw CSS in props is unsafe.
- **FilterBar `syncToUrl`**: Remove. URL sync belongs in a `useFilterState` hook, not the component.
- **FilterBar `select` type**: Replace `render` callback with `options: { value: string; label: string }[]`. The component should render the select.
- **ModalWrapper `initialFocus`**: Simplify to `initialFocusRef?: React.RefObject<HTMLElement>`. Radix handles defaults.
- **ModalWrapper `mobileBehavior`**: Remove. Default to bottom sheet for sm/md, fullscreen for xl/full. Add later if needed.
- **DataTable `pagination`**: Keep pagination external (composed by the page), not passed as a slot.

### Preventing Regression: ESLint Rule

Ban hardcoded hex values in Tailwind classes:
```js
// eslint.config.js
{
  rules: {
    'no-restricted-syntax': ['error', {
      selector: 'Literal[value=/\\[#[0-9a-fA-F]{3,8}\\]/]',
      message: 'Use design tokens instead of hardcoded hex values in Tailwind classes.',
    }],
  },
}
```

---

## Cross-Touchpoint Application

The design system applies consistently across **all** user-facing surfaces — not just the React apps. Every touchpoint should feel like the same product.

### Email Templates (17 Thymeleaf templates)

**Location**: `backend/app/src/main/resources/templates/email/`

**Current state**: All 17 templates use inline styles with generic system fonts and hardcoded blue (#2563eb) CTAs.

**Target state**:

| Element | Token | Value |
|---|---|---|
| Font stack | Satoshi fallback | `'Satoshi', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif` |
| CTA button | `primary-500` | `#0284c7` (Ocean) |
| CTA hover | `primary-600` | `#0369a1` |
| Body text | `neutral-700` | `#292524` |
| Secondary text | `neutral-500` | `#57534e` |
| Card background | `neutral-0` | `#ffffff` |
| Page background | `neutral-25` | `#fafaf9` |
| Border | `neutral-100` | `#e7e5e4` (warm stone, not cold gray) |
| Success accent | `success` | `#059669` |
| Warning accent | `warning` | `#d97706` |
| Error accent | `error` | `#dc2626` |

**Implementation notes**:
- All styles must remain **inline** (email clients strip `<style>` tags)
- Satoshi won't render in Gmail/Outlook — the fallback stack handles this gracefully
- Use a shared Thymeleaf fragment (`_email-base.html`) for consistent header/footer/wrapper
- Logo should match sidebar logo styling (Ocean primary background, white "B")
- Max-width: 600px, centered
- Warm stone neutrals (not cold grays) — consistent with app

### Keycloak Login/Register Pages (2 themes)

**Location**: `keycloak/themes/buurman/` and `keycloak/themes/buurman-backoffice/`

**Current state**: Custom themes with gradient backgrounds and blue CTAs. CSS matches old app styling.

**Target state**:

| Theme | Primary | Card/Form bg | Page bg | Accent area |
|---|---|---|---|---|
| `buurman` | Ocean `#0284c7` | `#ffffff` | `#fafaf9` | Ocean-tinted sidebar or header stripe |
| `buurman-backoffice` | Indigo `#6366f1` | `#ffffff` | `#f5f3ff` (indigo-tinted) | Indigo-tinted, visually distinct from app login |

The backoffice login page must be **immediately recognizable** as a different entry point — use the indigo-tinted page background (`#f5f3ff`) and indigo CTA buttons so admins know they're logging into the right place.

**Apply to**:
- `resources/css/styles.css` in each theme — update all color references
- Font: Load Satoshi via `@font-face` (self-hosted, not CDN — Keycloak may not have internet access)
- Buttons, links, focus rings: use respective primary color (Ocean for app, Indigo for backoffice)
- Form inputs: warm stone borders (`#e7e5e4`), inset background (`#f5f5f4`)
- Error states: semantic `#dc2626`
- Logo: update `resources/img/logo.png` and `logo_square.png` to match new branding
- Backoffice login: indigo-tinted page background (`#f5f3ff`), distinct from app login (`#fafaf9`)

### Generated PDFs (iText7 via PdfRenderer)

**Location**: `backend/buurman-booklets/` (14 exporters + PdfRenderer)

**Current state**: HTML-to-PDF conversion via `HtmlConverter`. Styling from inline HTML.

**Target state**:

| Element | Value |
|---|---|
| Font | Satoshi (embed TTF/OTF files) |
| Headings | Ocean `#0284c7` for accent, `#292524` for text |
| Body text | `#292524` (neutral-700) |
| Secondary text | `#57534e` (neutral-500) |
| Table headers | Ocean `#0284c7` background, white text |
| Table borders | `#e7e5e4` (warm stone) |
| Status badges | Same semantic colors as app (success/warning/error/info) |
| Page header | Company logo + document title, subtle Ocean accent line |
| Page footer | Page numbers in `#78716c` (neutral-400), tabular numerals |
| Financial figures | `font-variant-numeric: tabular-nums`, right-aligned |

**Implementation notes**:
- Bundle Satoshi font files in `backend/app/src/main/resources/fonts/`
- Register font with iText7's `FontProvider` before conversion
- Create a shared HTML template/fragment for consistent PDF header, footer, and styling
- A4 page size (already configured)
- Use CSS `@page` rules for margins and page breaks
- Financial amounts: always right-aligned, tabular numerals, consistent decimal formatting

### Public Website (www.buurman.io)

**Target state**: Same design language as the app — visitors should recognize the product immediately upon logging in.

| Element | Value |
|---|---|
| Font | Satoshi (load via Fontshare CDN or self-host) |
| Primary color | Ocean `#0284c7` |
| Accent color | Warm Amber `#f59e0b` |
| Neutrals | Warm Stone palette (same as app) |
| CTA buttons | Ocean primary, same border-radius and shadow system |
| Dark sections | Use app dark mode surface tokens for contrast sections |
| Spacing | Same 4px base grid, same spacing scale |

**Implementation notes**:
- Technology TBD (static site, Next.js, etc.) — design tokens apply regardless
- Share the same CSS custom properties as the app (copy `theme.css` or publish as npm package)
- Hero/marketing sections can use the full color palette more liberally than the app dashboard
- Ensure logo, favicon, and OG images use Ocean primary

---

### Testing Strategy for `@buurman/ui`

- **Test**: ModalWrapper (focus trap, Escape, backdrop click, preventClose), DataTable (sorting, selection, empty state), FilterBar (URL serialization, debounce, reset)
- **Skip tests**: StatusBadge, EmptyState, MetricCard, FormField (pure presentational — visual regression catches more bugs)
- Target: ~70% coverage on interaction-heavy components

---

## Implementation Priority

### Phase 1: Token Foundation (1 PR, no visual changes)
1. Populate `theme.css` with full ocean blue + stone + amber palette via `@theme`
2. Add semantic CSS custom properties (surface, text, border) with dark mode swap
3. Delete both `tailwind.config.js` files
4. Add `cn()` utility, `tailwind-merge`, `clsx`

### Phase 2: Layout Fix (1 PR)
5. Add `max-w-screen-2xl mx-auto` to Layout content area
6. Remove per-page padding wrappers (Layout owns all page padding)
7. Swap Layout background to `bg-surface-page`
8. Fix `transition-all` on sidebar → `transition-[width,margin]`

### Phase 3: Sidebar + Shared Components (1 PR)
9. Swap all sidebar hex colors to semantic tokens
10. Fix Pagination inline styles

### Phase 4: Build New Shared Components (3-4 PRs)
11. Card, Input, Select, Textarea (highest reuse impact)
12. ModalWrapper (Radix), StatusBadge, FormField, EmptyState
13. DataTable, MetricCard, Skeleton
14. FilterBar + `useFilterState` hook

### Phase 5: Page-by-Page Migration (1 PR per domain)
15. Dashboard page (tests MetricCard, charts)
16. Properties (list + detail + forms)
17. Tenants, Contracts, Payments, Expenses
18. Documents, Photos, Settings
19. Backoffice pages (apply indigo variant)

### Phase 6: Email Templates (1 PR)
20. Create shared Thymeleaf fragment (`_email-base.html`) with consistent header/footer/wrapper
21. Update all 17 email templates to use Ocean primary, warm stone neutrals, Satoshi fallback stack
22. Update logo assets in email templates

### Phase 7: Keycloak Themes (1 PR)
23. Bundle Satoshi font files in Keycloak theme resources
24. Update `buurman` theme CSS — Ocean primary, warm stone neutrals, Satoshi font
25. Update `buurman-backoffice` theme CSS — Indigo primary, warm stone neutrals, Satoshi font
26. Update logo assets in both themes

### Phase 8: PDF Templates (1 PR)
27. Bundle Satoshi font files in backend resources
28. Register Satoshi with iText7 `FontProvider`
29. Create shared PDF HTML template with consistent header/footer/styling
30. Update all 14 booklet exporters to use design system colors and typography

### Phase 9: Polish
31. Accessibility pass (focus rings, skip nav, ARIA, contrast)
32. Add ESLint rule banning hardcoded hex
33. Design system document finalization
