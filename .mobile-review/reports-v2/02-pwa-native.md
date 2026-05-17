# PWA / Native-Feel Review — Buurman

## Verdict

The web-app foundation is solid: viewport meta is properly configured, safe-area variables are threaded through layout/banner/sheet/tab-bar, `100dvh` has correctly replaced `100vh` in the persistent chrome (Layout, Sidebar), `useKeyboardInset` publishes `--kbd-inset` from `visualViewport`, the Sheet primitive is built on Radix Dialog (free focus trap + scroll-lock + ARIA), and the AAA-band of mobile fundamentals — 16 px input font, tap-highlight off, `touch-action: manipulation`, 44 px hit targets — are in place. What it does NOT yet feel native at: (1) the bottom sheet looks like a sheet but doesn't behave like one — there's no drag-to-dismiss, no snap points, no inertia; (2) there is no service worker, so the manifest's `display: standalone` produces an Add-to-Home-Screen experience that breaks the moment connectivity blips; (3) page transitions are hard route swaps; (4) `min-h-screen` (= `100vh`) is still littered across ~35 page files, so every page below the Layout chrome will still pick up the address-bar/notch jump on iOS; (5) the BottomTabBar fights the MobileFormStepperFooter visually because both render simultaneously on form pages; (6) `maximum-scale=5` and `interactive-widget=resizes-content` together is mostly fine, but `maximum-scale` is hostile to accessibility and not needed since the 16 px rule already prevents zoom-on-focus; (7) no haptics, no install-prompt handling, no offline page, no splash screens, no `purpose: "any"` icon. Below is the detailed teardown.

## Findings by Area

### 1. Sheet / Modal motion

`frontend/packages/ui/src/components/Sheet.tsx` is Radix-Dialog-based with two motion variants:

- Bottom-sheet variant: `animate-slide-up` (a `translateY(100%) → 0`, 250 ms, `ease-out` keyframe defined in `frontend/app/src/index.css:75-86`).
- Desktop variant: Tailwind/Radix `fade-in-0 zoom-in-95`.

**Gaps that read as non-native:**

1. **No drag-to-dismiss.** The visual handle at `Sheet.tsx:108-113` is decorative — it's an `<div aria-hidden>` with no pointer/touch listeners. iOS users will reach for it and nothing will happen. Either remove the handle (and accept "tap-X-or-backdrop only"), or wire it (and the whole `Dialog.Content`) to `@use-gesture/react` `useDrag` and translate Y on drag, dismiss past a threshold/velocity. `@use-gesture/react` is already in the bundle (used by `PinchZoomImage`), so the dep cost is zero.
2. **No snap points.** A native sheet typically supports a "half" detent (good for a multi-field form that doesn't need the full screen). Currently it's all-or-nothing at `max-h-[92dvh]`. Recommend a peek/full detent for long forms (especially the property step-form).
3. **`animate-slide-up` is `ease-out`, no inertia.** A more native curve would be a spring (`cubic-bezier(0.32, 0.72, 0, 1)` is the Apple sheet curve). Worth a `--ease-sheet` token.
4. **Closing animation is missing.** The bottom-sheet variant only sets `data-[state=open]:animate-slide-up`. There is no `data-[state=closed]:` rule for the slide-down — Radix sets `state="closed"` but the sheet just disappears because the close keyframe is unimplemented. The desktop variant correctly has both. Add `animate-slide-down` (mirror keyframe with reverse easing).
5. **Backdrop tap closes only if `preventClose` is false.** Fine, but the backdrop animation (`fade-in-0`/`fade-out-0`) is correct here. Good.
6. **`max-h-[92dvh]` is correct.** `dvh` accounts for the URL bar. Good. Body uses `overflow-y-auto overscroll-contain` (`Sheet.tsx:138`) which is correct — scroll won't bleed.

### 2. Keyboard-aware UI

The Sheet footer already rides `--kbd-inset` via `paddingBottom: 'calc(var(--safe-bottom) + var(--kbd-inset))'` at `Sheet.tsx:101`. `MobileFormStepperFooter` at `frontend/app/src/components/common/MobileFormStepper.tsx:188` does the same and also stacks above `--bottomnav-h`. Good.

**Gaps:**

1. **`BottomTabBar` does NOT respect `--kbd-inset`.** At `frontend/app/src/components/BottomTabBar.tsx:38` only `paddingBottom: 'var(--safe-bottom, 0px)'` is set. The container is `fixed bottom-0`. iOS Safari's `interactive-widget=resizes-content` (set in `index.html:12`) means the layout viewport shrinks when the keyboard opens, so the bar nominally moves with it — but in practice on iOS `visualViewport` is the source of truth and `position: fixed` containers ride the layout viewport, not the visual one. Net effect: when the keyboard is up on a form page (e.g. a Sheet rendered while standing on `/expenses`), the BottomTabBar will sit underneath the keyboard, not above it.
   - The simpler fix is to **hide the BottomTabBar when the keyboard is open** (`display: none` when `--kbd-inset > 0`). This also frees the form footer of the need to stack above `--bottomnav-h`.
   - Even better: hide `BottomTabBar` on routes that show their own form footer (Property/Contract/Expense create+edit pages) so the screenshot in `iphone-05-property-form.png` doesn't show TWO bottom UI strips fighting for the same 60 + safe-bottom px.
2. **`useKeyboardInset.ts:22`** computes `window.innerHeight - vv.height - vv.offsetTop`. With `interactive-widget=resizes-content`, on iOS Safari `window.innerHeight` already shrinks when the keyboard opens, so `inset` may compute to 0 even though the keyboard is up. Recommend swapping to a pure visual-viewport delta against a baseline captured at mount, or testing on a real iPhone before trusting the value. Today the math is fragile.
3. **No scrollIntoView for focused inputs inside a Sheet.** When a tall sheet has an input near the bottom and the keyboard opens, iOS will sometimes refuse to scroll the focused input into view because it's inside a scrollable child container (the Sheet body). Add a `focusin` listener inside the Sheet body that calls `target.scrollIntoView({ block: 'nearest', behavior: 'smooth' })` after a short delay. The dropdown selectors already do this for their option lists (`scrollIntoView({ block: 'nearest' })`) but not for inputs.
4. **`maximum-scale=5`** on the viewport meta is borderline-hostile (WCAG SC 1.4.4). The 16 px input rule already kills the iOS zoom-on-focus. Drop `maximum-scale=5`.

### 3. Pull-to-refresh

**There is no PTR implementation.** No matches for `PullToRefresh`, `usePullToRefresh`, or any equivalent. The only `onRefresh` hits are `onRefreshStatus` in `NotificationDetailModal` (button, not gesture).

Native browser PTR on the document body is **not reliable** here because `Layout.tsx:72` sets `overflow-hidden` on the outer flex container and `overflow-auto` on `<main>`. Native iOS Safari PTR only fires when the document body itself is the scroll container at scroll-top — which is never the case in this app. iPhone users will swipe down at the top of a list and get nothing (which is the current behaviour; verifiable by looking at `iphone-02-properties.png`).

Recommendation:

- Build a tiny `<PullToRefresh onRefresh={…}>` wrapper around the `<main>` scroll container (or each page's top scroller). `@use-gesture/react` is already a dep. The threshold/spinner can be a small CSS-only affair; on success call `queryClient.invalidateQueries({ queryKey })`. Wire it into the four list pages first (Properties, Contacts, Payments, Documents).
- Until then, ensure each list has a visible refresh button (`RefreshButton`) — looks like that exists; verify it's reachable on phone.

### 4. Service worker / offline / install

**There is no service worker.** No `vite-plugin-pwa`, no `workbox`, no `registerSW`, no `navigator.serviceWorker.register` call. The `manifest.webmanifest` declares `display: standalone` (`frontend/app/public/manifest.webmanifest:7`), so when a user does Add-to-Home-Screen they get a launcher with no chrome — and the moment they're offline they see Safari's "you are not connected" page in a chromeless shell. That's worse than browser-Safari.

Even without a full offline-first strategy, the bare minimum for a credible PWA install is:

1. A service worker that:
   - Pre-caches the app shell (`index.html`, the `vendor-*` chunks, the manifest, the icons, the fonts).
   - Serves a static `/offline.html` for navigation requests when the network is down.
   - Uses `NetworkFirst` for `/api/*` (with a 5 s timeout fallback to cache).
   - Uses `CacheFirst` for `/assets/*` and `/fonts/*`.
2. A `register-sw` call in `main.tsx` guarded by `import.meta.env.PROD`.
3. A `BeforeInstallPromptEvent` capture so we can show an in-app "Install Buurman" CTA on Chrome/Edge (iOS Safari has no programmatic install — it shows our manifest in Add-to-Home-Screen automatically).
4. An `appinstalled` event analytics ping (PostHog already in the stack).

`vite-plugin-pwa` with Workbox is the cheapest path; ~30 lines of config.

If web push is on the roadmap (rent-due reminders, document-shared notifications): a service worker is also a hard prerequisite, and on iOS 16.4+ web push works **only** for installed PWAs. Worth tying the SW work to the notifications backlog.

### 5. Page transitions

Routes are hard swaps via `react-router-dom@7` `<Routes>`/`<Route>` in `frontend/app/src/App.tsx:221-457`. Each `lazy()` boundary is wrapped in a single global `<Suspense fallback={<LoadingSpinner />}>` so navigations also flash a spinner while the chunk loads.

Native iOS gives users a left-edge-swipe-back gesture with a parallax slide. We can get close with:

- **Option A (low cost):** Wrap the `<Outlet />` content in a CSS view-transition. React-router v7 supports `unstable_viewTransition` on `<Link>`/`navigate()`. Add `viewTransitionName: 'page'` to the outlet wrapper and a `::view-transition-old(page)`/`::view-transition-new(page)` rule that slides X. Works on Safari 18+, Chrome ≥ 111.
- **Option B (medium cost):** Use Framer Motion's `AnimatePresence` keyed on `location.pathname` to fade/slide between routes. Adds ~25 kB.
- **Option C (high cost, highest reward):** Add a left-edge swipe-back gesture (intercept pointer events from the leftmost ~20 px, animate the page off, then `navigate(-1)`). Requires bookkeeping for the prev route's screenshot/snapshot. Probably skip unless we go full PWA.

Recommend Option A as the immediate win — purely CSS, zero JS cost, falls back to instant swap on browsers without view transitions.

Also: hoist `<Suspense>` to wrap each `Route` individually so the spinner flash on intra-app navigation is replaced with the previous page sitting still until the new chunk lands. Pair with `<Routes>`-level `key={location.pathname}` for the view transition handle.

### 6. Touch feedback

`Button.tsx` has `active:` variants for every visual style (primary, secondary, ghost, success, danger). Good. `touch-action: manipulation` + `-webkit-tap-highlight-color: transparent` are global. Good.

Gaps:

1. **No haptic API.** Buurman has destructive actions (delete property, end contract) — these would benefit from `if ('vibrate' in navigator) navigator.vibrate(10)` on submit. Wrap in a tiny `useHaptic()` hook with a `light` / `medium` / `heavy` API, even if all three are 10/20/30 ms `navigator.vibrate` calls. iOS Safari ignores `navigator.vibrate` — but the indirection means we can swap to a future iOS API or to a hidden Capacitor bridge without touching callers.
2. **No long-press affordance.** SwipeAction exists for list rows (`frontend/packages/ui/src/components/SwipeAction.tsx`) — verify it's actually mounted in list views. The screenshots don't show any swipe affordance hint.
3. **`active:` is not enough on touch.** It fires for 50–100 ms and is hard to see. Recommend pairing with a subtle `scale-95` press state on big tap targets (cards in `iphone-02-properties.png`). Even `active:scale-[0.98] transition-transform` looks dramatically more native.
4. **BottomTabBar tab presses have no `active:` style.** `BottomTabBar.tsx:43-50` only branches on `isActive` (current route). Add an `active:bg-surface-inset` and an `active:scale-95` to the inner `<span>`. NavLink supports neither out of the box — wrap in `tap-press` utility.

### 7. Address-bar / dvh

The persistent chrome correctly uses `100dvh`:
- `Layout.tsx:74`: `height: 'calc(100dvh - var(--env-banner-height, 0px))'`
- `Sidebar.tsx:344`: same calc
- `index.css:36-39`: `#root` uses `100vh` with `100dvh` override (good).

But **almost every page** still uses `min-h-screen` (Tailwind = `min-height: 100vh`). Greppable evidence at ~35 pages including `PropertyListPage`, `ContactListPage`, `PaymentsPage`, `ExpensesPage`, `ContractsPage`, `SettingsPage`, etc. Inside `<Layout>`, `<main>` is already a scroll container with bounded height (`100dvh - banner`), so `min-h-screen` on the inner page is mostly cosmetic background-color — but it causes the page to be at least one viewport tall, which on iOS Safari is the OLD-viewport tall (i.e. address-bar-visible), so the user can scroll an extra ~70 px even on a short page. Replace `min-h-screen` with `min-h-full` or `min-h-[100dvh]`.

Same for `WorkInProgress.tsx:15` which uses `min-h-[calc(100vh-4rem)]`, and `LoadingSpinner.tsx:21` (`min-h-screen` when `fullScreen`), and `createAuthProvider.tsx:131` (`minHeight: '100vh'`). Sweep with a codemod.

`DocumentPreviewModal.tsx:106,140` uses `min-h-screen` for the full-screen modal — also should be `100dvh`.

### 8. App icons / manifest

`frontend/app/public/manifest.webmanifest`:

```json
{
  "name": "Buurman",
  "short_name": "Buurman",
  "display": "standalone",
  "theme_color": "#fafaf9",
  "icons": [
    { "src": "/assets/logo/android-chrome-192x192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/assets/logo/android-chrome-512x512.png", "sizes": "512x512", "type": "image/png" },
    { "src": "/assets/logo/android-chrome-512x512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
```

Issues / gaps:

1. **No `purpose: "any"` declared.** Per spec, omitting `purpose` defaults to `"any"`, so this is technically OK, but Chrome's Lighthouse audit prefers explicit `"any maskable"` on the same icon. Either add `"purpose": "any"` to the first 512 entry, or merge: one entry with `"purpose": "any maskable"`.
2. **Same PNG used for `any` and `maskable`.** Maskable icons need their content within a 80 % safe zone (the OS may crop to a circle/squircle/teardrop). Confirm the 512×512 asset has the inner safe zone or generate a separate `-maskable.png` with extra padding. If it doesn't, you'll get a clipped logo on Android home screens.
3. **No iOS splash screens.** iOS Safari doesn't read the manifest's `icons` for the launch splash — it uses `apple-touch-startup-image` `<link>` tags, one per device class (iPhone 14 Pro, 15 Pro Max, iPad, etc.). Without them, an installed PWA on iOS flashes a white screen with the app name for ~1 s on cold start. Generate with `pwa-asset-generator` and add the ~10 `<link rel="apple-touch-startup-image" media="..." href="..." />` tags to `index.html`.
4. **Status bar style.** `apple-mobile-web-app-status-bar-style: black-translucent` is set — this causes the iOS status bar to overlap the top of the page. The EnvironmentBanner uses `paddingTop: env(safe-area-inset-top)` (good), but the Sidebar in mobile/drawer mode uses `top: 'var(--env-banner-height, 0px)'` — when there's no banner, the drawer's title row will sit under the iOS notch. Add a `pt-safe` (or `paddingTop: max(env(safe-area-inset-top), 0px)`) to the drawer.
5. **No `id` field.** Adding `"id": "/"` pins the manifest identity across `start_url` changes and is required for Play-Store-via-TWA later.
6. **No `categories`, `screenshots`, `shortcuts`.** Optional but `shortcuts` would let iOS/Android long-press on the icon to jump straight to e.g. "New Expense", "Properties". Cheap, very native-feel.
7. **No dark-mode `theme_color` in manifest.** The `<meta name="theme-color" media="...">` is duplicated for light/dark in `index.html`, but the manifest only has the light value. Doesn't matter on iOS, matters slightly on Android.
8. **`background_color` light only.** Same — Android splash will flash light even in dark mode.

### 9. Browser quirks

1. **iPad split-view / Stage Manager freeform.** With Stage Manager on iPadOS 16+, the app can be resized to arbitrary widths. The phone breakpoint is `(max-width: 767px)`; iPad in narrow Stage Manager (~700 px wide) will fall under the phone breakpoint and start showing BottomTabBar — but `Sidebar.tsx:338` still shows the rail (`md:translate-x-0`). At exactly `< md` this turns off; OK. But verify with a 600 px-wide Safari window that there's no double nav. Worth a real-device check.
2. **Hover / pointer media queries.** The code uses Tailwind's `hover:` directly. On touch devices `:hover` "sticks" after a tap. Recommend wrapping hover effects in `@media (hover: hover) and (pointer: fine)` — Tailwind 4 has the `hover:` variant configured for `@media (hover: hover)` by default since v4, so this is probably fine, but worth confirming `theme.css` or Tailwind config doesn't override.
3. **Safari overflow-clip vs Chrome.** `Layout.tsx:72` uses `overflow-hidden` on the flex container. Fine cross-browser. The `overscroll-contain` on Sheet body is supported in iOS 16+.
4. **Radix Dialog scroll-lock + iOS Safari.** Radix uses `body { overflow: hidden; position: fixed; top: -<scrollY>px }`. On iOS this is correct and survives the rubber-band. Good — but be aware that opening a Sheet while the keyboard is up will scroll the page to top on close. Not high-priority.
5. **`interactive-widget=resizes-content` is a Chrome/Edge directive.** Safari has its own resize-content behaviour and ignores this hint. The current `--kbd-inset` math compensates, but see §2.2 above for the iOS edge case.
6. **`format-detection: telephone=no`** is set. Good — prevents iOS from auto-styling phone numbers.

### 10. Scroll containment

1. **Sheet body**: `overflow-y-auto overscroll-contain` (`Sheet.tsx:138`). Correct.
2. **`<main>` in Layout**: `overflow-auto` only — no `overscroll-contain`. When the user reaches the top of a list and keeps pulling, on iOS the entire app rubber-bands. Add `overscroll-y-contain` to `<main>` to prevent the rubber-band bleeding out of the app.
3. **Mobile drawer sidebar**: scroll-locking is handled by Radix when the drawer is a `Dialog`, but the Sidebar in `Sidebar.tsx` is NOT a Radix Dialog — it's just an `<aside>` with a backdrop scrim. Body scroll is NOT locked. Open the drawer on phone, tap-and-drag inside the drawer scroll: the body behind also scrolls. Either convert the drawer to a `Radix Dialog`/`Sheet`, or add a manual `document.body.style.overflow = 'hidden'` while `mobileOpen`.
4. **`CurrencyDropdown` body uses `pb-[env(safe-area-inset-bottom)]`** — good attention to detail.
5. **`Layout.tsx:100`**: `<main className="flex-1 overflow-auto ...">` — when keyboard opens with focus inside `<main>`, the layout viewport shrinks (interactive-widget=resizes-content) and `<main>`'s `100dvh - banner` height shrinks accordingly. Good, but the inner `<div className="...pb-[calc(--bottomnav-h + --safe-bottom)] md:pb-0">` does NOT add `--kbd-inset` — so when keyboard is up, the form content can't scroll past where the keyboard now sits. Add `+ var(--kbd-inset, 0px)` to that `pb-` so the bottom padding grows with the keyboard.

## Priority Punch List

### P0 — ship this sprint
- **Hide `BottomTabBar` when `--kbd-inset > 0`** AND on routes that mount a `MobileFormStepperFooter` (Property/Contract/Expense create+edit). Today the form footer stacks above the tab bar, which is bewildering: `iphone-05-property-form.png` clearly shows two competing bottom strips.
- **Add closing animation to bottom-sheet variant** (`data-[state=closed]:animate-slide-down`). Right now the sheet pops out on close, which feels broken on a device that just slid it in.
- **Wire drag-to-dismiss on Sheet handle** via `@use-gesture/react` (already in bundle). Without this, the visual handle at `Sheet.tsx:108-113` is a lie.
- **Drop `maximum-scale=5`** in `index.html:12` — accessibility regression with no offsetting benefit (16 px input rule already prevents zoom-on-focus).
- **Replace `min-h-screen` → `min-h-[100dvh]`** across the ~35 page files. Mechanical sed/codemod. Eliminates the lingering iOS address-bar jump.
- **Body-scroll-lock on mobile drawer** in `Sidebar.tsx`. Trivial — set `document.body.style.overflow` in the `mobileOpen` effect.

### P1 — next sprint
- **Service worker via `vite-plugin-pwa`**: app-shell precache, `/offline.html` for navigation fallback, `NetworkFirst` for `/api/*`. Unlocks credible Add-to-Home-Screen, sets the stage for web push.
- **Pull-to-refresh primitive** on Properties / Contacts / Payments / Documents list scroll containers. Use `@use-gesture/react`.
- **View-transition page swap** (CSS-only, react-router v7 `unstable_viewTransition`). Slide-X on phone, fade on desktop. Falls back gracefully.
- **`scrollIntoView` on `focusin`** inside Sheet body so inputs always escape the keyboard.
- **Manifest**: add `"id"`, `"shortcuts"`, generate a true maskable icon (or change to `"any maskable"` with a verified safe zone).
- **iOS splash screens** via `pwa-asset-generator`. Adds ~10 `<link rel="apple-touch-startup-image">`.
- **Robust `useKeyboardInset`**: capture baseline `innerHeight` at mount, compute delta against current `visualViewport.height`, clamp ≥ 0. Today's math is fragile when `interactive-widget=resizes-content` actually fires on Chrome.
- **Add `overscroll-y-contain` to `<main>`** in `Layout.tsx`.

### P2 — when there's runway
- **Haptic API stub** (`useHaptic()` → `navigator.vibrate`). Ready for iOS-native bridge later.
- **Snap-points on Sheet** (`peek` and `full`) for long forms.
- **`active:scale-[0.98]`** + spring transitions on Buttons, BottomTabBar tabs, list cards. Big "feels native" delta for tiny effort.
- **`BeforeInstallPromptEvent` capture** + in-app install CTA (Android/desktop Chrome).
- **`appinstalled` PostHog event.**
- **Left-edge swipe-back gesture** on phone (Option C in §5). Skip unless a real native shell is on the table.
- **Stage Manager / iPad freeform** smoke test at 600 px / 700 px / 800 px breakpoints.
- **Dark-mode `theme_color` + `background_color`** in manifest for Android splash parity.

## Files referenced

- `frontend/app/index.html`
- `frontend/app/public/manifest.webmanifest`
- `frontend/app/src/main.tsx`
- `frontend/app/src/App.tsx`
- `frontend/app/src/index.css`
- `frontend/app/src/components/Layout.tsx`
- `frontend/app/src/components/Sidebar.tsx`
- `frontend/app/src/components/BottomTabBar.tsx`
- `frontend/app/src/components/common/MobileFormStepper.tsx`
- `frontend/app/src/components/documents/DocumentPreviewModal.tsx`
- `frontend/app/src/components/documents/PinchZoomImage.tsx`
- `frontend/app/src/hooks/useKeyboardInset.ts`
- `frontend/packages/ui/src/components/Sheet.tsx`
- `frontend/packages/ui/src/components/Button.tsx`
- `frontend/packages/ui/src/components/SelectionBar.tsx`
- `frontend/packages/ui/src/components/Toast.tsx`
- `frontend/packages/ui/src/components/SwipeAction.tsx`
- `frontend/packages/ui/src/styles/theme.css`
