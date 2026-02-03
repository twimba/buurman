# Implementation Plan

## Task 1: Team Selector opens to the right (expanding upward)
- Modify `TeamSwitcher.tsx`: change dropdown from `top-full left-0 right-0 mt-2` to `left-full bottom-0 ml-2` so it opens to the right of the trigger and aligns to the bottom edge, expanding upward

## Task 2: User Preferences - enforce date format & timezone across UI

### 2a: Create `useFormattedDate` hook + `formatAppDate` utility
- New file `frontend/src/utils/dateFormatting.ts`
- Reads user preferences from React Query cache (`useUserPreferences`)
- Maps `DD/MM/YYYY` -> `dd/MM/yyyy`, `MM/DD/YYYY` -> `MM/dd/yyyy`, `YYYY-MM-DD` -> `yyyy-MM-dd` (date-fns tokens)
- Uses `date-fns-tz` for timezone conversion: `utcToZonedTime(date, userTimezone)` then `format()`
- Export a `useFormatDate()` hook that returns a `formatDate(dateStr)` function

### 2b: Replace all hardcoded date formatting across the UI
Files to update (replace `format(new Date(x), 'MMM d, yyyy')` with the hook):
- PaymentsPage, PaymentDetailPage, DashboardPage
- ExpensesPage, ExpenseDetailPage
- ContractCard, ContractDetailPage
- AuditLogPage, TransactionHistoryPage
- DocumentsPage, PhotosPage
- TeamSettingsSection, SubscriptionSection, PaymentHistorySection
- PropertyDetailPage, TenantDetailPage
- InvitationPage (toLocaleDateString)

## Task 3: Team Preferences - default currency & country

### 3a: Add `defaultCountry` to TeamPreferencesSection UI
- Already exists in backend `TeamSettings.RegionalSettings`
- Add country selector to `TeamPreferencesSection.tsx` using existing `countries` utility
- Wire up save to include `defaultCountry` in the update request
- Allow empty string = no default (initial state)

### 3b: Use team default currency in forms
- Modify `PaymentForm.tsx`: if no existing payment currency, use team default
- Modify `ExpenseForm.tsx`: same
- Modify `ContractForm.tsx`: same
- Use `useTeamSettings` hook to fetch defaults; fallback to 'EUR' only if no team setting

### 3c: Use team default country in forms
- Modify `PropertyForm.tsx`: default country from team settings (if creating new)
- Modify `AddressForm.tsx`: same
- Allow "Select a country" option to remain (no forced default if team has none)

## Task 4: Dark/Light Mode

### 4a: Configure Tailwind for dark mode
- Add `darkMode: 'class'` to `tailwind.config.js`

### 4b: Create ThemeProvider context
- New file `frontend/src/context/ThemeContext.tsx`
- Reads user preference (from `useUserPreferences`)
- Applies `dark` class to `document.documentElement`
- Handles 'system' mode via `matchMedia('(prefers-color-scheme: dark)')`
- Wrap app in `<ThemeProvider>`

### 4c: Add dark mode CSS variables
- Update `index.css` with dark mode color overrides

### 4d: Add `dark:` variants to all major components
- Sidebar, Layout, all pages, all forms, all modals, cards, tables, etc.
- Systematic approach: bg-white -> dark:bg-gray-800, text-gray-900 -> dark:text-gray-100, border-gray-200 -> dark:border-gray-700, etc.
