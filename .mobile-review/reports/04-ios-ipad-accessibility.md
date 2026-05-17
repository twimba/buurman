# iOS / iPadOS Native-Feel & Mobile Accessibility — Buurman

Scope: how Buurman should behave when running in Mobile Safari on iPhone / iPad. The audit is grounded in the React 19 + Vite 7 + Tailwind v4 codebase (`frontend/app`, `frontend/packages/ui`) and the 17 mobile/iPad screenshots in `.mobile-review/screenshots/`.

Headline issues observed in screenshots:

- The hamburger button (top-left, `Sidebar.tsx:203`) overlaps the page heading (`Properties`, `Payments`, `Add New Property`) instead of sitting in a sticky top bar.
- The "LOCAL ENVIRONMENT" amber banner is full-bleed and does not account for the iPhone status bar / notch.
- The on-page action buttons (`+ Add Property`, `Register Payment`, `Schedule Payment`) wrap awkwardly because the page has no sticky header — they share vertical space with the title.
- Modals (Radix `Dialog.Content` in `ModalWrapper.tsx`) render as centered desktop dialogs even on a 390 px viewport; they should be bottom sheets.
- Viewport meta is `width=device-width, initial-scale=1.0` — missing `viewport-fit=cover`. Safe-area insets therefore won't be applied.

---

## Part A: Native-Feel

### 1. Viewport Meta

Current (`frontend/app/index.html:9`):

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
```

Replace with:

```html
<meta
  name="viewport"
  content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content"
/>
<meta name="theme-color" content="#fafaf9" media="(prefers-color-scheme: light)" />
<meta name="theme-color" content="#0c0a09" media="(prefers-color-scheme: dark)" />
<meta name="color-scheme" content="light dark" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<meta name="apple-mobile-web-app-title" content="Buurman" />
<meta name="format-detection" content="telephone=no" />
```

Notes:

- DO NOT add `user-scalable=no` or `maximum-scale=1` — that breaks WCAG 1.4.4 (Resize Text) and Dynamic Type. Modern iOS already prevents zoom-on-double-tap when needed.
- `viewport-fit=cover` is required for `env(safe-area-inset-*)` to return non-zero values.
- `interactive-widget=resizes-content` (Chrome/Android) helps with keyboard handling. iOS uses `visualViewport` API regardless.
- `theme-color` syncs Safari's chrome with the surface tokens already defined in `theme.css` (`--surface-page` light `#fafaf9`, dark `#0c0a09`).

### 2. Safe Areas (notch + home bar)

Add a global root padding model and expose two CSS variables that components can compose against. Put this near the top of `frontend/packages/ui/src/styles/theme.css`:

```css
@theme {
  --safe-top: env(safe-area-inset-top, 0px);
  --safe-bottom: env(safe-area-inset-bottom, 0px);
  --safe-left: env(safe-area-inset-left, 0px);
  --safe-right: env(safe-area-inset-right, 0px);
}

html, body { background: var(--surface-page); }

/* Page surface fills under the notch so the amber banner extends into it */
body {
  padding-left: var(--safe-left);
  padding-right: var(--safe-right);
}
```

EnvironmentBanner: it must include the top inset so its colored band reaches the very top of the display but its text stays below the status bar.

```css
.env-banner {
  padding-top: calc(0.5rem + var(--safe-top));
  padding-left: calc(1rem + var(--safe-left));
  padding-right: calc(1rem + var(--safe-right));
  /* the rendered height (including --safe-top) is what feeds --env-banner-height */
}
```

Update the ResizeObserver that writes `--env-banner-height` to use `getBoundingClientRect().height` — it already will be inclusive of the inset padding.

Sidebar drawer (`Sidebar.tsx:212`) needs bottom + left inset:

```jsx
<aside
  style={{
    top: 'var(--env-banner-height, 0px)',
    height: 'calc(100dvh - var(--env-banner-height, 0px))',
    paddingBottom: 'var(--safe-bottom)',
    paddingLeft: 'var(--safe-left)',
  }}
>
```

Sticky CTA bars / FABs (the blue `Add Property`, `Register Payment` actions) should reserve `calc(var(--safe-bottom) + 12px)` so they sit clear of the home indicator.

### 3. Sticky Headers

Today every page renders the title + actions inline at the top of `<main>`. On scroll they disappear, which on iPhone forces a scroll-to-top to find primary actions. Introduce a `<PageHeader sticky>` that pins below the env banner.

```jsx
// packages/ui/src/components/PageHeader.tsx
export function PageHeader({ title, subtitle, icon: Icon, actions }) {
  return (
    <header
      className="sticky z-30 -mx-4 md:-mx-6 lg:-mx-8 px-4 md:px-6 lg:px-8 py-3
                 bg-surface-page/85 backdrop-blur-md
                 border-b border-border-subtle
                 supports-[backdrop-filter]:bg-surface-page/70"
      style={{ top: 'var(--env-banner-height, 0px)' }}
    >
      <div className="flex items-center gap-3 min-h-11">
        {Icon && <Icon className="h-5 w-5 text-primary-600 shrink-0" />}
        <div className="min-w-0 flex-1">
          <h1 className="text-base md:text-2xl font-semibold truncate">{title}</h1>
          {subtitle && (
            <p className="hidden md:block text-sm text-text-secondary truncate">
              {subtitle}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">{actions}</div>
      </div>
    </header>
  );
}
```

Z-index plan (top → bottom):

| Layer | z-index | Notes |
|---|---|---|
| Toast/Notification | 70 | Always above sheets |
| Modal/Sheet overlay + content | 50 | Radix Dialog default range |
| Env banner | 45 | Must be above sticky page header |
| Mobile menu button | 45 | Lives inside banner area |
| Sticky `<PageHeader>` | 30 | Below banner; uses `top: var(--env-banner-height)` |
| Sidebar `<aside>` (drawer) | 40 | Above sticky header when open |
| Drawer scrim | 35 | Below drawer, above sticky header |

The hamburger currently has `z-50` and floats free over the title (screenshots 04, 05, 07, 15). Move it inside the sticky `PageHeader` as the leading action on `lg:hidden`, dropping the free-floating button entirely.

### 4. Modal → Bottom Sheet

`ModalWrapper.tsx` is a centered Radix Dialog. On a 390×844 viewport with the iOS keyboard open, that means a tiny window in the middle of the screen. Replace with a responsive variant: dialog on `md:` and up, bottom sheet on phones.

Recommended library: **Vaul** (`vaul`, by Emil Kowalski). It's purpose-built for this, ships with drag handles, snap points, focus trap (built on Radix), and is ~5 KB. Alternative: Radix Dialog + custom slide animation + `@use-gesture/react`.

Proposed shared API:

```tsx
// packages/ui/src/components/Sheet.tsx
import { Drawer } from 'vaul';

export function Sheet({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  snapPoints,           // e.g., [0.4, 0.9]
  dismissible = true,
}: SheetProps) {
  const isDesktop = useMediaQuery('(min-width: 768px)');
  if (isDesktop) return <ModalWrapper {...mapToModalProps(arguments[0])} />;

  return (
    <Drawer.Root open={open} onOpenChange={(o) => !o && dismissible && onClose()}
                 snapPoints={snapPoints} dismissible={dismissible}>
      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 bg-surface-overlay z-50" />
        <Drawer.Content
          className="fixed inset-x-0 bottom-0 z-50 max-h-[92dvh]
                     rounded-t-2xl bg-surface-card border-t border-border-default
                     flex flex-col outline-none"
          style={{ paddingBottom: 'var(--safe-bottom)' }}
        >
          <div aria-hidden className="mx-auto my-2 h-1.5 w-10 rounded-full bg-border-strong" />
          <Drawer.Title className="px-6 pt-2 text-lg font-semibold">{title}</Drawer.Title>
          {description && <Drawer.Description className="px-6 text-sm text-text-secondary">{description}</Drawer.Description>}
          <div className="flex-1 overflow-y-auto overscroll-contain px-6 py-4">{children}</div>
          {footer && <div className="border-t border-border-default px-6 py-3">{footer}</div>}
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
}
```

Migration path: keep `ModalWrapper` (desktop is fine), introduce `<Sheet>` for new flows, then progressively switch existing modals (Property edit, Payment register, Schedule Payment, Filters drawer) to `Sheet` which auto-routes by breakpoint.

### 5. Momentum Scrolling / Overscroll

Tailwind v4 already exposes `overscroll-*`. Apply at the right boundaries:

```css
html, body { overscroll-behavior-y: none; }   /* no bounce on the root */
main { overscroll-behavior-y: contain; }
.sheet-body, .drawer-body, .modal-body { overscroll-behavior: contain; }
```

`-webkit-overflow-scrolling: touch` is no-op in modern WebKit; omit. But `overflow-y: auto` is needed on the Sheet body so iOS uses native momentum (Drawer.Content has it via the inner div above).

### 6. iOS Safari Quirks

**100vh bug.** Layout uses `100vh` (`Layout.tsx:43`, `Sidebar.tsx:222`). On iPhone Safari the URL bar collapse causes overflow. Switch to dynamic viewport units (Tailwind v4 supports `h-dvh`, `min-h-dvh`, `h-svh`, `h-lvh`):

```diff
- height: 'calc(100vh - var(--env-banner-height, 0px))',
+ height: 'calc(100dvh - var(--env-banner-height, 0px))',
```

Same change in `Sidebar.tsx`. Use `dvh` for app shell, `svh` only if you want to lock to smallest viewport during keyboard show.

**Input zoom on focus < 16 px.** Audit forms in `15-property-new-form.png` — inputs look 16 px but the `Search by address` input in `04-properties.png` may be 14 px. Add a global guard:

```css
@media (max-width: 767px) {
  input:not([type='checkbox']):not([type='radio']),
  select,
  textarea {
    font-size: 16px;
  }
}
```

**Keyboard / `position: fixed` jumpiness.** With `interactive-widget=resizes-content` plus the `visualViewport` API you can pin the sticky footer (e.g., sheet's action row) to the visible viewport:

```ts
useEffect(() => {
  const vv = window.visualViewport;
  if (!vv) return;
  const onResize = () => {
    document.documentElement.style.setProperty('--kbd-inset', `${window.innerHeight - vv.height - vv.offsetTop}px`);
  };
  vv.addEventListener('resize', onResize); vv.addEventListener('scroll', onResize);
  onResize();
  return () => { vv.removeEventListener('resize', onResize); vv.removeEventListener('scroll', onResize); };
}, []);
```

Then `bottom: calc(var(--kbd-inset, 0px) + var(--safe-bottom))` on the sheet footer.

**Tap delay / touch-action.** Set `touch-action: manipulation` on all interactive elements to kill the 300 ms double-tap delay and avoid accidental pinch on buttons. Add to global utilities and to `button`, `a`, `[role="button"]`.

```css
button, [role='button'], a, summary, label {
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}
```

**Text selection.** Disable selection on chrome (toolbars, nav items, segmented controls), keep selection in content (descriptions, titles, identifiers):

```css
.ui-chrome, nav, aside, [role='toolbar'] {
  -webkit-user-select: none; user-select: none;
}
.ui-chrome [data-selectable], .prose, [data-content] { user-select: text; }
```

**Date input.** Both screenshots 03 / 16 show native `<input type="date">` pickers, which are fine on iOS/iPadOS/Android. Keep them; ensure `min-height: 44px` on mobile and label association (see B-8).

**Address bar collapse.** Now solved by `dvh`. No JS hack needed.

### 7. Pull-to-Refresh

The pages most asked to refresh from a phone are Dashboard, Payments, Properties. Native browser pull-to-refresh on iOS already works on the document body — but only at scrollTop=0, which a sticky header doesn't break. Two options:

1. **Do nothing**: rely on Safari's native PTR. Cheapest. Recommended for v1.
2. **Custom PTR** on specific lists (`Payments`, `Properties`) with `react-pull-to-refresh-fizzlepop` or hand-rolled via `@use-gesture`. Only worth it if you also disable the native PTR (`overscroll-behavior-y: contain` on `<main>` would do that — which we want for sheets but might be confusing on lists).

Recommendation: keep native PTR for now. Add an explicit refresh icon button (already exists in Properties / Payments screenshots) — it's discoverable for VoiceOver users anyway.

### 8. PWA / Add to Home Screen

There is **no `manifest.webmanifest`** in `frontend/app/public/`. There is already an `apple-touch-icon`. Adding a minimal manifest costs little and unlocks "Add to Home Screen" with a real icon and standalone display.

```json
// frontend/app/public/manifest.webmanifest
{
  "name": "Buurman",
  "short_name": "Buurman",
  "start_url": "/",
  "scope": "/",
  "display": "standalone",
  "background_color": "#fafaf9",
  "theme_color": "#fafaf9",
  "icons": [
    { "src": "/assets/logo/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/assets/logo/icon-512.png", "sizes": "512x512", "type": "image/png" },
    { "src": "/assets/logo/icon-maskable.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
```

Link from `index.html`: `<link rel="manifest" href="/manifest.webmanifest" />`.

iOS PWA limitations to flag: no push, no background sync, no real install prompt (user has to use Share → Add to Home Screen), 7-day data eviction if app unused. Don't promise offline unless a service worker is added (out of scope here).

### 9. Haptics

Safari does not expose `navigator.vibrate` on iOS. Skip silently. Spots where haptics *would* help if iOS opens up (or in a future Capacitor wrap):

- Mark-paid action on a payment row → `impactMedium`
- Swipe-to-delete confirmation → `impactLight`
- Successful form submit in a sheet → `notificationSuccess`
- Pull-to-refresh release → `impactLight`

Keep them centralized in a `haptic.ts` utility that no-ops on iOS web; that way the future wrap is one swap.

### 10. iPad Specifics

iPad screenshots (`16-ipad-dashboard.png`, `17-ipad-properties.png`) at 768 px wide already hit `md:`. Layout holds. Split View at 320 px width is the danger zone — verify the dashboard KPI grid collapses to one column. Currently the grid is 2-up at 768; in Split View half (≈ 375 px) you'd want 1-up — keep `grid-cols-1 sm:grid-cols-2`.

**Stage Manager / freeform windows**: behaves like a desktop browser, so `md`/`lg` queries Just Work. But hover states from Magic Keyboard / trackpad pointers want desktop affordances:

```css
@media (hover: hover) and (pointer: fine) { /* show hover ring on rows */ }
@media (hover: none)  { /* show pressed/active state instead */ }
```

The `useIsMobile` hook at `useIsMobile.ts:5` uses `max-width: 639px`. For iPad-aware behavior add a sibling `usePointer` returning `'fine' | 'coarse'` and treat iPad with keyboard as fine pointer.

**Pointer events vs touch.** Use Pointer Events everywhere (already done by Radix/Vaul). Don't mix `onTouchStart` and `onClick` handlers.

### 11. Status Bar / Notch

With `viewport-fit=cover` + `apple-mobile-web-app-status-bar-style=black-translucent`, the env banner needs to extend into the inset area. Currently:

- Banner top edge is at `top: 0` of layout viewport.
- Without `viewport-fit=cover`, iOS already gives a white inset; the amber band sits below it, which is what screenshots show.
- With the new viewport meta, add `padding-top: var(--safe-top)` to the banner. Its measured `--env-banner-height` will increase by the inset, which Layout and Sidebar both consume — they remain correct without further changes.

---

## Part B: Mobile Accessibility

### 1. Touch Targets

WCAG 2.5.8 (AA) = 24×24 CSS px minimum, 2.5.5 (AAA) = 44×44. Apple HIG = 44×44. Targets observed in screenshots:

| Element | Current size (approx) | Verdict |
|---|---|---|
| Hamburger menu button (`Sidebar.tsx:203`, `p-2`) | 40×40 | Fails AAA, passes AA, fix to 44 |
| Refresh icon button (Properties, Payments) | 32×32 | Fails 2.5.5, fix |
| CSV icon button | 32×40 (compact) | Borderline |
| Sidebar nav row (`py-2.5`) | ~44 with line-height | OK |
| `Activate` / `Decline` chip buttons in Pending Extensions | ~32 tall | Fails — bump to 44 |
| Filter chips (`All Categories`, `Residential`…) | ~36 | Borderline, bump to 40 |
| Range chips (`6M`, `1Y`, `2Y`…) | ~32 | Fails — bump to 40 |
| Tap-to-expand chart icon | ~24 | Fails — wrap in 44 hit box |

Fix recipe — keep visual size, expand hit area:

```css
@utility hit-44 {
  position: relative;
  &::after {
    content: '';
    position: absolute;
    inset: 50% auto auto 50%;
    width: max(100%, 44px);
    height: max(100%, 44px);
    transform: translate(-50%, -50%);
  }
}
```

Then `<button class="p-1.5 hit-44">…</button>`.

### 2. Dynamic Type

`theme.css` defines no font-size tokens — Tailwind defaults apply (rem-based: `text-sm` = `0.875rem`). That's good. Audit checklist:

- Grep for hard-coded `px` font sizes in components — found none in shared UI; OK.
- Confirm `<html>` doesn't set `font-size: 16px` explicitly anywhere (it doesn't, Tailwind preflight leaves it as user-agent default).
- Header heights using `h-16` (`Sidebar.tsx:228`) are in rem already (4 rem) so they scale; but text inside with `truncate` may clip at 200%. Allow line wrap on the sidebar logo block at large text.

### 3. Contrast

Mental WCAG check from screenshots:

- **LOCAL ENVIRONMENT banner** — white text on amber `#d97706` ≈ 3.0:1 contrast. Fails AA (4.5:1) for normal text. The text is small caps ~11 px. Options: darken background to `#92400e` (4.6:1) or use `text-neutral-900` on the amber. Recommend `bg-accent-700 text-white` (≈ 5.9:1).
- **Primary `+ Add Property` button** — white on `#0284c7` ≈ 4.6:1. Passes AA.
- **Negative numbers in red** (`-€25,828`, `-1.2%`, `-6.4%`) on white — `#dc2626` ≈ 4.65:1. Passes AA at sizes shown (≥ 18 px).
- **Overdue Payments callout** — `text-error-text` on `error-bg` `#dc2626` on `#fef2f2` ≈ 4.85:1. Passes AA.
- **`Action required: Review overdue payments`** small body in red on light red — same colors at 12 px. AA requires 4.5 — borderline; tighten to `#b91c1c` or `error-text-strong`.
- **Disabled / muted text** `--text-disabled: #a8a29e` on white = 2.6:1. Fails. Reserve for genuinely disabled controls (where contrast rule doesn't strictly apply) and never for body text.

### 4. Focus Management

`focus-ring` utility is defined in `theme.css:203` — good. Verify it's applied to all interactive controls. Audit:

- `Sidebar.tsx` hamburger button (`button` on line 203): no `focus-ring` class — add it.
- Nav links use `NavLink` with `transition-all`; visible focus is inherited from browser default but is suppressed by `outline-none` patterns elsewhere — confirm with a tab through.
- Drawer focus trap: Radix Dialog (used by `ModalWrapper`) traps focus automatically. For the mobile drawer (`Sidebar.tsx`), currently it's an `<aside>` toggled by state — there is no focus trap. When `mobileOpen=true`, trap focus inside the drawer, restore on close. Easiest: switch the drawer to a `Drawer.Root`/`Dialog.Root` for mobile, or wire `focus-trap-react`.
- Skip link exists (`Layout.tsx:47`) — good.

### 5. Screen Reader

Icon-only buttons need `aria-label`. From `Sidebar.tsx`:

```diff
- <button onClick={() => setMobileOpen(!mobileOpen)} className="lg:hidden …">
+ <button
+   onClick={() => setMobileOpen(!mobileOpen)}
+   aria-label={mobileOpen ? t('a11y.closeMenu') : t('a11y.openMenu')}
+   aria-expanded={mobileOpen}
+   aria-controls="primary-sidebar"
+   className="lg:hidden …"
+ >
```

Add `id="primary-sidebar"` and `role="navigation"` (or use `<nav>`) on the `<aside>`. Other icon-only buttons to label:

- Refresh button (Properties, Payments, Dashboard) → `aria-label="Refresh"` plus visually-hidden status announce after refresh completes.
- CSV download button → `aria-label="Export as CSV"`.
- Expand chart icon (top-right of Portfolio Cash Flow) → `aria-label="Expand chart"`.
- Collapse sidebar chevron (`ChevronsLeft/Right`) — `title` is set but `aria-label` should be set too; `title` doesn't announce reliably in VoiceOver.
- Modal close button has `<span className="sr-only">Close</span>` — good.

Live regions for async (see B-9).

### 6. Reduced Motion

`theme.css:213` already has a global `prefers-reduced-motion: reduce` rule that flattens animations to 0.01 ms. Good. Verify Vaul/Radix snap animations respect it (Vaul honors `--vaul-after-transition` and Radix data attributes respect CSS; the global rule will neutralize both). Add to the rule:

```css
@media (prefers-reduced-motion: reduce) {
  [data-state='open'][data-side], [data-state='closed'][data-side] {
    transform: none !important;
  }
}
```

### 7. Landmarks

`Layout.tsx` has `<main id="main-content">` (good), Sidebar is an `<aside>`. Issues:

- When the mobile drawer is closed, the `<aside>` still exists in DOM with `translate-x-full`. Its links remain in the tab order. Add `inert` on the `<aside>` while closed (or `hidden`/`aria-hidden=true` and `tabindex=-1` on children — `inert` is the modern, simplest answer, supported in all current browsers).
- `<aside>` does not have a `role="navigation"` or labeled `<nav>` inside it directly. The inner `<nav>` (line 294) is unlabeled — add `aria-label={t('a11y.primaryNav')}`.
- The hamburger button is outside any landmark. Wrap the entire mobile top region (banner + hamburger + sticky page header) in a `<header role="banner">`.

```jsx
<aside
  id="primary-sidebar"
  aria-label={t('a11y.primaryNav')}
  inert={!mobileOpen && isMobile ? '' : undefined}
  …
>
```

### 8. Form Labels

`15-property-new-form.png` shows visible labels (Street*, City*, Postal Code*). Confirm each `<input>` has either `<label htmlFor={id}>` or `aria-labelledby`. The asterisk should be hidden from SR:

```jsx
<label htmlFor="street">
  {t('street')}<span aria-hidden="true"> *</span>
  <span className="sr-only"> {t('a11y.required')}</span>
</label>
<input id="street" required aria-required="true" />
```

Group related fields (`Address`, `Specifications`) in `<fieldset>` / `<legend>` — currently `<h2>` headings are used, which is acceptable but `fieldset` is stronger for forms.

### 9. Live Regions

Async updates currently silent. Add:

- A global `<div role="status" aria-live="polite" className="sr-only" id="a11y-live" />` mounted in `Layout`.
- A `useAnnounce()` hook that writes to it: `useAnnounce()('Refresh complete, 30 properties')`.
- Hook into React Query `onSuccess`/`onError` for the actions on Dashboard / Properties / Payments.
- Error boundaries / form-level errors: `<div role="alert">` for assertive announcement.
- Loading states longer than ~1 s: announce "Loading properties" once via `useAnnounce`.

### 10. i18n / RTL

`index.html` ships with `<html lang="en">` hard-coded. The app supports multiple locales — set `lang` dynamically:

```ts
// inside i18next init
i18n.on('languageChanged', (lng) => {
  document.documentElement.lang = lng;
  document.documentElement.dir = ['ar', 'he', 'fa', 'ur'].includes(lng) ? 'rtl' : 'ltr';
});
```

Buurman doesn't ship RTL languages today (English / Dutch). When/if Arabic is added, the sidebar's left-anchored drawer needs to mirror — use `inset-inline-start: 0` and `translate-x-*` → `[dir=rtl]:translate-x-full` variants, and `safe-area-inset-left/right` will need symmetric application via `padding-inline`.

---

## Priority Punch List

### P0 — Ship this sprint

1. **Viewport meta**: replace with `viewport-fit=cover` + `theme-color` + apple-web-app metas (`frontend/app/index.html`).
2. **`100vh` → `100dvh`** in `Layout.tsx:43` and `Sidebar.tsx:222` to fix iOS Safari URL-bar bug.
3. **Safe-area insets**: add `--safe-*` tokens to `theme.css`; apply `padding-top: var(--safe-top)` to EnvironmentBanner and `padding-bottom: var(--safe-bottom)` to Sidebar drawer footer + any sticky CTAs.
4. **Sticky `<PageHeader>` component**: integrates hamburger as leading action; pins below env banner. Migrate Dashboard, Properties, Payments, Expenses, Contacts, Contracts, Documents, Photos, Settings.
5. **Form input font-size ≥ 16 px on mobile** to disable focus zoom (global `@media` rule).
6. **`touch-action: manipulation` + `-webkit-tap-highlight-color: transparent`** globally on interactive elements.
7. **Mobile drawer accessibility**: add `aria-label`, `aria-expanded`, `aria-controls` on hamburger; add `inert` on `<aside>` when closed; trap focus when open.
8. **Banner contrast**: switch LOCAL ENVIRONMENT to `bg-accent-700 text-white` for AA.
9. **Touch targets**: bump Refresh, CSV, range chips, Activate/Decline, expand-chart to ≥ 44 px via `hit-44` utility.
10. **Dynamic `<html lang>` / `dir`** wired to i18n changes.

### P1 — Next sprint

11. **`<Sheet>` component** based on Vaul; migrate Property / Payment / Schedule / Filter modals.
12. **`overscroll-behavior: contain`** on Sheet body, drawer body, main scroll container.
13. **`visualViewport`-driven `--kbd-inset`** for sheet footers above the iOS keyboard.
14. **Live region + `useAnnounce` hook**; wire to React Query mutations + refresh actions.
15. **PWA manifest** + 192 / 512 / maskable icons.
16. **Z-index audit** consolidating into the table above (kill the duplicate `z-50` on hamburger and modal overlay).
17. **`@media (hover: hover)` split** so iPad with trackpad shows hover affordances, touch-only shows pressed states.
18. **Fix muted text contrast** — restrict `--text-disabled` to actually-disabled controls.

### P2 — Later

19. **`apple-mobile-web-app-status-bar-style=black-translucent`** behavior tested in standalone PWA mode.
20. **Custom pull-to-refresh** on Payments list once telemetry shows manual refresh usage.
21. **Haptics utility stub** for future Capacitor wrap.
22. **RTL pass** when Arabic locale is on the roadmap.
23. **Font-size fluid scaling** with `clamp()` for very large Dynamic Type (300%+).
24. **iPad Stage Manager Split View test pass** at 320 px width.
25. **Service worker** for offline read-only access to Dashboard cached data.

---

Files most affected by the P0 changes:

- `frontend/app/index.html`
- `frontend/packages/ui/src/styles/theme.css`
- `frontend/app/src/components/Layout.tsx`
- `frontend/app/src/components/Sidebar.tsx`
- `frontend/packages/ui/src/components/EnvironmentBanner.tsx` (and the height-measuring effect that feeds `--env-banner-height`)
- `frontend/packages/ui/src/components/ModalWrapper.tsx` (later, when Sheet ships)
- new: `frontend/packages/ui/src/components/PageHeader.tsx`
- new: `frontend/packages/ui/src/components/Sheet.tsx`
- new: `frontend/app/src/hooks/useAnnounce.ts`
