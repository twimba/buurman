# design-sync notes — @buurman/ui

## Source shape & build
- Package shape, **synth-entry** mode: no `dist/` and no `build` script (`package.json` `main`→`src/index.ts`). The converter bundles `src/index.ts` directly via esbuild.
- Run the converter from `frontend/packages/ui` with `--node-modules ../../node_modules` (Yarn 4 node-modules linker hoists react/react-dom/posthog-js/keycloak-js/react-router-dom there as peer deps from the consuming apps; the package itself doesn't list them).
- Install: `cd frontend && yarn install --immutable` (Yarn 4, `nodeLinker: node-modules`).
- `ToastProvider` excluded via `componentSrcMap` (it's a context provider, nothing visual to render). `useToast`, `cn`, `createAnalytics`, `createAuthProvider` are camelCase — not discovered as components (bundled, no cards).

## CSS / Tailwind — CRITICAL, do not regress
- Components are styled with **Tailwind v4 utilities** keyed to custom `@theme` tokens (`bg-primary-500`, `text-text-secondary`, `bg-surface-card`) plus standard palette colors (`bg-green-50`). Nothing ships compiled, so `cfg.cssEntry` points at a **generated** stylesheet.
- `.design-sync/compile-css.mjs` runs the Tailwind v4 postcss plugin over `.design-sync/tw-input.css`, scanning `src/`, → `.design-sync/.cache/ui-compiled.css`. **This must be regenerated before every `package-build.mjs` run** (the `.cache/` is gitignored).
- **Layer order matters.** `tw-input.css` imports `tailwindcss` FIRST, then `theme.css`. theme.css contains a custom `@layer utilities {}` block (`.min-h-touch`, fluid type). If theme.css is imported before tailwindcss, that block registers the `utilities` layer ahead of `base`, so **preflight overrides component utilities** (`text-white`, `rounded-md`, `text-sm` silently fail while `bg-*` still works). Symptom: black text / square corners / 16px text on buttons. Keep tailwindcss import first.
- Primary theme is the **app** palette (Ocean/blue, `theme.css`). Backoffice (`theme-backoffice.css`, Indigo, `.theme-backoffice` scope, light-only) is a future variant sync — not shipped.

## Fonts
- Satoshi (variable woff2 in `src/fonts/`) shipped via `cfg.extraFonts` → `.design-sync/satoshi.css` (@font-face). `--font-sans` in theme.css points at Satoshi.

## Preview authoring conventions
- Import components from `'@buurman/ui'`. Icons are lucide-react, passed as **ReactNode** (`icon={<Home className="h-6 w-6" />}`), not component refs.
- Use realistic property-management content: Dutch addresses, € amounts, tenants/leases/properties.
- Use DS tokens in wrapper classNames (`text-text-primary`, `text-text-secondary`, `bg-surface-card`, `bg-surface-page`).
- StatusBadge color backgrounds are intentionally faint pastels (`bg-*-50`) — that's the real design, grade good.

## react-router (PageHeader) — do not regress
- `PageHeader` calls `useNavigate()`; react-router's context is inlined inside `_ds_bundle.js`. A preview's own react-router is a different instance → context mismatch → blank cell.
- Fix in place: `cfg.extraEntries: ["react-router-dom"]` merges react-router onto `window.BuurmanUI` from the SAME bundle pass. The `PageHeader.tsx` preview imports `MemoryRouter` from `'@buurman/ui'` (NOT from `'react-router-dom'`) and wraps the header — context then matches. PageHeader is the only component using `useNavigate`.
- This is why `window.BuurmanUI` lists extra (react-router) exports — harmless; component discovery is from `index.ts`, not the global.

## Per-component prop gotchas (from wave authoring)
- **StatusBadge**: API is `label` + `color` (gray/red/orange/amber/yellow/green/emerald/teal/cyan/blue/indigo/violet/purple/rose), NOT `variant`/children. `bg-*-50` pastel fills are the real design.
- **Input/Select/Textarea**: native element wrappers — pass native attrs (`defaultValue`, `disabled`, `placeholder`). Input/Select have `size` + `error`; Textarea uses `rows` (no `size`). Select needs `<option>` children.
- **FormField**: label+control+error wrapper; `error` overrides `hint`; `readOnly` only mutes the label (pass `disabled` on the inner control to disable).
- **DataTable `ColumnDef`** (read from `src/components/`, NOT the generated `.d.ts`): `id`, `header`, `cell(row,index)`, `accessor(row)`, `align`, `sortable`. Sort arrow needs `sort` state + `onSortChange`.
- **ResponsiveTable**: requires a `mobileRow(row)` render prop even for desktop capture. Capture is desktop-width → shows the `md+` `<table>` branch.
- **FilterBar `FilterDef`**: union of `toggle`/`select`/`search`; `values` is `Record<string,string>`.
- Sub-types (ColumnDef/FilterDef/DataListItem/ResponsiveTableColumn/SortState) are re-exported from the `@buurman/ui` barrel but NOT present in the generated per-component `.d.ts`.

## Static-only limitations (graded good; gesture/hover states can't render statically)
- **SwipeAction / PullToRefresh**: touch-gesture wrappers — cards show wrapped content at rest; swipe/pull reveal is interaction-only.
- **SidebarTooltip**: tooltip bubble is hover-only (`group-hover:opacity-100`); preview shows the rail + a static repro of the bubble.
- **FilterSheet**: no `open` prop — sheet is internal-state-gated. Preview uses `collapseBelow="md"` + a ≥768px override viewport (820x520) so the inline filter form renders.
- **Overlay fixed-position trick**: `ConfirmDialog` and `SelectionBar` use raw `position:fixed` (not Radix portals); their previews wrap in a `transform:translateZ(0)` containing-block div sized to the override viewport so the fixed overlay is captured in-frame. `ModalWrapper`/`Sheet` are Radix portals — `open={true}` (+ `forceSheet` for Sheet's bottom-sheet visual) just works.
- **LazyImage**: IntersectionObserver-gated — uses a large `rootMargin` so the `<img>` mounts immediately in headless; real remote images load under networkidle.
- **EnvironmentBanner**: `position:fixed; top:0`; `production` renders null (not previewed) — dev/local/staging shown.

## Re-sync risks
- `.design-sync/.cache/ui-compiled.css` is generated and gitignored — re-sync MUST re-run `compile-css.mjs` before the build, or `cfg.cssEntry` is stale/missing.
- The Tailwind-import-order fix lives in `tw-input.css` (committed). If anyone "tidies" it to put theme.css first, all component utilities silently break — see CSS section.
- Synth-entry `.d.ts` props come from parsing TS source (no shipped types). Generally clean here, but complex inline prop types may need `cfg.dtsPropsFor`.
