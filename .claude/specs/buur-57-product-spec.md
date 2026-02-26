# BUUR-57: Property Financials Module - Product & UX Specification

## 1. User Personas & Jobs-to-be-Done

### Primary Persona: Independent Landlord (1-20 properties)
- **Who**: Part-time landlord, often with a day job. Manages their own portfolio, not a professional property manager.
- **Pain points today**:
  - Tracks mortgage payments, insurance, and property taxes in spreadsheets (or not at all)
  - Has no consolidated view of "how much does this property actually cost me per month?"
  - Cannot answer "am I making money on this property?" without pulling out a calculator
  - Tax time is stressful: scrambling to gather annual expense totals per property, categorized for Schedule E / tax filing
  - Does not know their current equity position across properties
  - Mortgage info is scattered across bank portals, documents, and memory
- **NOT an accountant**: Needs plain-language financial summaries, not double-entry bookkeeping. Terms like NOI and Cap Rate need tooltips.

### Jobs-to-be-Done
| Job | Current Workaround | Target Experience |
|-----|--------------------|-------------------|
| "How much does property X cost me per month?" | Mental math or spreadsheet | Single glance at property dashboard card |
| "Am I cash-flow positive on this property?" | Compare bank statements | Monthly Cash Flow widget (income - all costs) |
| "What's my equity in this property?" | Check bank mortgage portal | Equity bar showing paid vs remaining |
| "How's my portfolio doing overall?" | Sum up spreadsheet rows | Portfolio financials page with aggregates |
| "Prepare for tax filing" | Painful manual export | One-click annual tax summary per property |
| "What are my total annual costs?" | Dig through receipts | Auto-calculated from property financial profile |

---

## 2. Information Architecture

### Navigation Placement Decision

**Recommendation: Enhance existing structure, NOT add new top-level nav items.**

Rationale: Small landlords (1-20 properties) think property-first. Financial data belongs in the context of each property, not as a standalone section. Adding another top-level nav item increases cognitive load. The existing Reports page handles portfolio-level aggregation.

### Proposed Structure

```
Sidebar (no changes to top-level items)
  Dashboard .............. + add "Portfolio Financial Summary" section
  Properties
    Property Detail Page
      [Info] [Financials] [Photos] [Documents] [Contracts] [Expenses] [Audit] [Dashboard]
                 ^^^^^^^
                 NEW TAB - replaces mortgage/insurance data from property edit form
  Expenses ............... unchanged (but MORTGAGE_PAYMENT category deprecated over time)
  Reports ................ + add "Cash Flow" and "Equity" report sections
```

**Key changes:**
1. **New "Financials" tab** on Property Detail Page -- the primary home for all property-level financial data
2. **Existing "Dashboard" tab** on Property Detail -- enhanced with financial widgets (cash flow card, equity bar)
3. **Main Dashboard page** -- add a compact "Portfolio Financial Health" card
4. **Reports page** -- add cash flow and equity reports

### Why NOT a top-level "Financials" nav item?
- The data is inherently property-scoped (each property has its own mortgage, insurance, tax)
- Portfolio aggregation belongs in the existing Reports section
- Avoids the "where do I go?" confusion of having financial data in two places
- Follows Stessa's pattern: property-level dashboard + portfolio-level reports

---

## 3. Property Financial Profile (the new "Financials" tab)

### 3.1 Tab Layout

The "Financials" tab on the Property Detail Page is divided into clearly labeled sections using cards. Each section is independently collapsible (like the existing `CollapsibleSection` component pattern used in `PropertyFinancialForm`).

```
+-----------------------------------------------------------------------+
| FINANCIALS                                              [Edit] button  |
+-----------------------------------------------------------------------+
|                                                                        |
|  +--- Purchase & Acquisition ----+  +--- Current Valuation ---------+ |
|  | Purchase Price: EUR 250,000   |  | Market Value: EUR 310,000     | |
|  | Purchase Date: 2019-03-15     |  | As of: 2025-01-15             | |
|  | Closing Costs: EUR 12,500     |  | Appreciation: +24% (+EUR 60k) | |
|  | Total Invested: EUR 262,500   |  | Source: Manual / Appraisal    | |
|  +-------------------------------+  +---------------------------------+|
|                                                                        |
|  +--- Financing (Mortgage #1) ----------------------------------------+|
|  | Type: Fixed Rate  |  Lender: ING Bank                              ||
|  | Original Amount: EUR 200,000  |  Interest Rate: 3.5%               ||
|  | Start Date: 2019-04-01        |  End Date: 2049-04-01              ||
|  | Monthly Payment: EUR 898                                            ||
|  |   Principal: ~EUR 315  |  Interest: ~EUR 583 (estimated split)     ||
|  | Outstanding Balance: EUR 185,000 (manually entered or calculated)   ||
|  +--------------------------------------------------------------------+|
|  | [+ Add another loan/mortgage]                                       ||
|                                                                        |
|  +--- Recurring Costs (Annual) ----------------------------------------+
|  | Property Tax     EUR 1,800/yr  (EUR 150/mo)   Due: Jan, Jul       |
|  | Insurance        EUR 1,200/yr  (EUR 100/mo)   Due: Monthly        |
|  | HOA/VvE Fee      EUR 2,400/yr  (EUR 200/mo)   Due: Monthly        |
|  | Management Fee   EUR 0                                              |
|  | Maintenance Rsv  EUR 1,200/yr  (EUR 100/mo)   Budget/Reserve      |
|  |                                                                     |
|  | Total Annual Operating Costs: EUR 6,600 (EUR 550/mo)               |
|  +--------------------------------------------------------------------+
|                                                                        |
|  +--- Depreciation ---------------------------------------------------+
|  | Method: Straight Line  |  Useful Life: 27.5 years                  |
|  | Building Value: EUR 200,000  |  Annual Depreciation: EUR 7,273     |
|  +--------------------------------------------------------------------+
|                                                                        |
|  +--- Key Metrics (auto-calculated) ----------------------------------+
|  | Monthly Cash Flow:  EUR +352                                        |
|  |   Rent Income:      EUR 1,800                                       |
|  |   - Mortgage:       EUR 898                                         |
|  |   - Operating:      EUR 550                                         |
|  |                                                                     |
|  | NOI (Annual):       EUR 15,000    [?] tooltip                       |
|  | Cap Rate:           6.0%          [?] tooltip                       |
|  | Cash-on-Cash:       6.8%          [?] tooltip                       |
|  | Equity:             EUR 125,000   [?] tooltip                       |
|  +--------------------------------------------------------------------+
+-----------------------------------------------------------------------+
```

### 3.2 Section Details

#### Purchase & Acquisition
- **Fields**: Purchase price, purchase date, closing costs (new field), total invested (auto-calculated: purchase + closing)
- **Migration**: Purchase price & date already exist on property. Closing costs is new.
- **Currency**: Uses property's currency (already established pattern)

#### Current Valuation
- **Fields**: Current market value, valuation date, valuation source (Manual/Appraisal/Online Estimate)
- **Display**: Shows appreciation as both percentage and absolute value vs purchase price
- **Migration**: currentMarketValue & marketValueDate already exist on property

#### Financing
- **Supports multiple loans** per property (primary mortgage + HELOC, second mortgage, etc.)
- **Fields per loan**: Type (Fixed/Variable/Interest Only/Line of Credit), lender name, original amount, interest rate, start date, end date/term, monthly payment, fixed vs variable flag, outstanding balance, balance date
- **Outstanding balance**: Manually entered by user (auto-calculation from amortization schedule deferred to v2)
- **Display**: Principal vs interest split shown as estimate for fixed-rate mortgages (simple math from rate + balance)
- **Migration**: Existing property mortgage fields map to the first/primary loan record

#### Recurring Costs
- **Same fields as current**: Property tax, insurance, HOA, management fee, maintenance reserve
- **Enhanced display**: Shows both annual and monthly breakdown, due months
- **Kept on property model**: These are property-level attributes, not separate entities
- **Migration**: No schema change needed -- these already exist

#### Depreciation
- **Same fields as current**: Method, useful life, land value
- **Enhanced display**: Auto-calculate annual depreciation amount
- **Migration**: No schema change needed

#### Key Metrics (read-only, auto-calculated)
- **Monthly Cash Flow**: Rent income (from active contract) - mortgage payment - monthly operating costs
- **NOI**: Annual rent income - annual operating expenses (excludes mortgage -- standard real estate definition)
- **Cap Rate**: NOI / Current market value (or purchase price if no market value)
- **Cash-on-Cash Return**: Annual cash flow (after mortgage) / total cash invested
- **Equity**: Current market value - outstanding mortgage balance
- Each metric has a `[?]` tooltip explaining what it means (using existing `MetricHint` component)

### 3.3 Edit Mode

The Financials tab shows data in **read-only view** by default (clean, formatted display). An "Edit" button opens an edit form (either inline or as a side panel, consistent with existing property edit patterns).

**Critical design decision**: Financial data editing uses the **same property edit page**, but with the financial sections pre-expanded. This reuses the existing `PropertyFinancialForm` component (enhanced) rather than building a separate editing interface.

---

## 4. Data Entry Flows

### 4.1 Initial Property Financial Setup

**When**: User creates a new property or first visits the Financials tab on an existing property with no financial data.

**Flow**:
1. User navigates to Property > Financials tab
2. If no financial data exists, show a friendly empty state:
   ```
   +--------------------------------------------------+
   |  [icon: piggy bank]                               |
   |  No financial data yet                            |
   |  Add purchase details, financing, and costs to    |
   |  see your property's financial health at a glance |
   |                                                   |
   |  [Set Up Financials]  (primary button)            |
   |  [Skip for now]       (text link)                 |
   +--------------------------------------------------+
   ```
3. "Set Up Financials" opens the property edit page scrolled to the financial sections
4. Progressive disclosure: only Purchase & Valuation section starts expanded

### 4.2 Adding a Mortgage/Loan

**Flow**:
1. In the Financials tab (view mode), click "[+ Add Mortgage]" or edit existing
2. Opens a modal or inline form with loan fields
3. **Smart defaults**: If mortgage type is "Fixed Rate" and user enters amount + rate + term, auto-calculate monthly payment
4. Save creates a `property_loan` record linked to the property

### 4.3 Updating Outstanding Balance

**Flow** (v1 -- manual):
1. User receives monthly mortgage statement
2. Navigates to property Financials tab, clicks "Update Balance" on the loan
3. Enters new outstanding balance + date
4. System stores this as a balance snapshot (history preserved for equity tracking over time)

**Future (v2)**: Optional bank connection (Plaid/similar) for automatic balance updates.

### 4.4 Insurance, Tax, HOA Entries

**No change from current flow**: These remain fields on the property edit form (already implemented in `PropertyFinancialForm`). The Financials tab reads and displays them in a cleaner, read-only format.

### 4.5 Recurring Cost Updates

When annual amounts change (e.g., property tax reassessment):
1. User edits the property, updates the annual amount
2. The change takes effect immediately for future calculations
3. Historical calculations use the amounts that were in effect at that time (deferred to v2 -- v1 uses current amounts for all periods)

---

## 5. Dashboard Widgets

### 5.1 Property Dashboard Tab (Enhanced)

The existing `PropertyDashboardTab` component gets new financial cards. Priority order:

#### Priority 1 (MVP)
1. **Monthly Cash Flow Card** -- The single most important widget
   ```
   +----------------------------------+
   | Monthly Cash Flow     [?]        |
   | EUR +352              this month |
   |                                  |
   | Income    EUR 1,800   [=======]  |
   | Mortgage  EUR  -898   [====]     |
   | Operating EUR  -550   [===]      |
   +----------------------------------+
   ```
   - Green if positive, red if negative
   - Horizontal stacked bar showing income vs costs

2. **Equity Overview Card**
   ```
   +----------------------------------+
   | Equity             [?]           |
   | EUR 125,000                      |
   |                                  |
   | [========|||||||||||||]  62.5%   |
   |  Paid     Remaining              |
   | EUR 125k  EUR 75k               |
   +----------------------------------+
   ```
   - Simple progress bar: equity / market value
   - If no mortgage: shows "Fully owned" with 100% bar

3. **Annual P&L Summary Card**
   ```
   +----------------------------------+
   | Annual Summary (2026)  [?]       |
   | Net Income: EUR +4,224           |
   |                                  |
   | Gross Rent    EUR 21,600         |
   | Operating     EUR -6,600         |
   | Mortgage      EUR -10,776        |
   +----------------------------------+
   ```

#### Priority 2 (Post-MVP)
4. **Expense Breakdown** (pie chart by category) -- already partially exists in Reports
5. **Cash Flow Trend** (12-month sparkline) -- reuse income trend chart pattern
6. **Key Metrics Grid** (NOI, Cap Rate, Cash-on-Cash in a 3-column grid)

### 5.2 Main Dashboard Page

Add a single **"Portfolio Financial Health"** card to the main dashboard (after the existing 4 stat cards):

```
+-----------------------------------------------------------------------+
| Portfolio Financial Health                                             |
|                                                                        |
| Total Monthly Cash Flow: EUR +1,850    Total Equity: EUR 412,000      |
| Properties: 3 cash-flow positive, 1 break-even                        |
|                                                                        |
| [View Financial Reports ->]                                            |
+-----------------------------------------------------------------------+
```

This is a compact summary that links to the Reports page for details. Avoid cluttering the main dashboard with too much financial data.

---

## 6. Portfolio-Level View

### 6.1 Enhancement to Existing Reports Page

The existing `FinancialReportsPage` already has income/expense charts and property comparison. Enhance with:

#### New Section: "Portfolio Net Worth"
```
+-----------------------------------------------------------------------+
| Portfolio Net Worth                                                    |
|                                                                        |
| Total Property Value:     EUR 920,000                                  |
| Total Outstanding Debt:   EUR 508,000                                  |
| Total Equity:             EUR 412,000                                  |
|                                                                        |
| [Property 1]  [========|||||]  EUR 125k equity                        |
| [Property 2]  [===========||]  EUR 187k equity                        |
| [Property 3]  [=====||||||||]  EUR  65k equity                        |
| [Property 4]  [==============] EUR  35k equity (no mortgage)          |
+-----------------------------------------------------------------------+
```

#### New Section: "Property Comparison Table" (Enhanced)
Enhance the existing property comparison with financial metrics:

| Property | Monthly Rent | Monthly Costs | Cash Flow | NOI | Cap Rate | Equity |
|----------|-------------|---------------|-----------|-----|----------|--------|
| Maple St | EUR 1,800 | EUR 1,448 | EUR +352 | EUR 15,000 | 6.0% | EUR 125k |
| Oak Ave | EUR 2,200 | EUR 1,650 | EUR +550 | EUR 19,200 | 5.4% | EUR 187k |

Sortable by any column. Highlight rows that are cash-flow negative in a subtle red/pink.

#### New Section: "Cash Flow Trend" (Portfolio-Level)
12-month chart showing total portfolio cash flow (income - all costs including mortgages). Line chart with income and total costs as separate lines, shaded area between = cash flow.

---

## 7. Reports

### 7.1 MVP Reports (v1)

1. **Monthly/Annual P&L per Property** (enhance existing financial overview)
   - Already exists but needs mortgage costs included
   - Add "NOI" and "Cash Flow After Debt Service" lines

2. **Tax Summary per Property** (enhance existing)
   - Already exists via `getTaxSummary`
   - Add depreciation line items
   - One-click export to CSV (already supported)

3. **Portfolio Cash Flow Report**
   - Monthly cash flow per property and total
   - Filterable by date range and property
   - Export to CSV/PDF

### 7.2 Future Reports (v2+)

4. **Equity Growth Over Time** -- requires historical balance snapshots
5. **Cash Flow Projection** -- requires forward-looking amortization calculation
6. **Rent vs Mortgage Coverage Ratio** -- per property
7. **Schedule E Helper** -- formatted for tax filing (country-specific, complex)

---

## 8. Migration Strategy

### 8.1 Data Migration: Mortgage Fields to Loan Records

**Current state**: Mortgage data stored as flat fields on the `properties` table (mortgage_type, mortgage_amount, mortgage_interest_rate, mortgage_start_date, mortgage_end_date, monthly_mortgage_payment, mortgage_payment_variable).

**Target state**: Separate `property_loans` table allowing multiple loans per property.

**Migration approach: Automatic + Non-destructive**

1. **Flyway migration** creates the `property_loans` table
2. **Data migration script** (in the same Flyway migration) copies existing property mortgage fields into a loan record for each property that has mortgage data
3. **Old columns are NOT dropped** -- kept as deprecated, read-only backup
4. **Backend** reads from new `property_loans` table; writes only to `property_loans`
5. **Frontend** reads from new loan response objects; the `PropertyFinancialForm` is updated to use the loan sub-form

### 8.2 Expense Category Migration: MORTGAGE_PAYMENT

**Current state**: Users may have created expenses with category `MORTGAGE_PAYMENT`. These are essentially manual records of mortgage payments.

**Migration approach: Soft deprecation**

1. `MORTGAGE_PAYMENT` category is **hidden from the expense creation dropdown** (no new expenses of this type)
2. **Existing MORTGAGE_PAYMENT expenses are preserved** -- they still appear in expense lists and reports
3. A **migration banner** appears on the property Financials tab:
   ```
   We found 12 mortgage payment expenses for this property totaling EUR 10,776.
   These are now tracked separately in the Financing section above.
   [Dismiss] [View these expenses]
   ```
4. No automatic deletion or conversion of existing expense records
5. Reports are updated to exclude MORTGAGE_PAYMENT expenses from operating expenses (they're now counted via the loan record's monthly payment instead)

### 8.3 User Experience During Transition

- **Zero disruption**: All existing data remains accessible
- **Gradual adoption**: Users with existing mortgage fields see their data automatically in the new Financials tab
- **No forced action**: Users don't need to do anything -- the migration is silent
- **Clear messaging**: If conflicts exist (mortgage expenses AND loan records), surface them with a dismissible info banner

---

## 9. MVP vs Future Prioritization

### MVP (v1) -- Target scope

| Feature | Effort | Impact | Include? |
|---------|--------|--------|----------|
| Financials tab on Property Detail | Medium | High | YES |
| Property Loans table (multiple loans) | Medium | High | YES |
| Auto-migrate existing mortgage fields | Low | High | YES |
| Key Metrics (NOI, Cap Rate, Cash-on-Cash) | Low | High | YES |
| Monthly Cash Flow widget on Property Dashboard | Low | High | YES |
| Equity Overview widget | Low | High | YES |
| Closing costs field | Low | Medium | YES |
| Outstanding balance (manual entry) | Low | High | YES |
| Depreciate MORTGAGE_PAYMENT expense category | Low | Medium | YES |
| Portfolio Financial Health card on main dashboard | Low | Medium | YES |
| Portfolio Net Worth section in Reports | Medium | Medium | YES |
| Enhanced property comparison with metrics | Low | Medium | YES |

### Post-MVP (v2)

| Feature | Effort | Why Later? |
|---------|--------|------------|
| Amortization schedule calculator | Medium | Complexity; manual balance works for v1 |
| Equity growth over time chart | Medium | Requires historical balance snapshots first |
| Cash flow projection/forecasting | High | Requires amortization + vacancy modeling |
| Bank connection for balance sync | High | Third-party integration (Plaid/similar) |
| Historical cost tracking (costs that changed over time) | Medium | v1 uses current amounts |
| Valuation history (multiple appraisals) | Low-Medium | Nice-to-have, manual updates sufficient |
| Lender document attachments | Low | Documents system already exists, just link |
| Insurance policy details (provider, policy number, coverage) | Medium | Separate insurance entity overkill for v1 |
| Loan refinancing workflow | Medium | Edge case, can be handled as new loan + close old |
| Schedule E formatted export | High | Country-specific tax formatting |
| Mobile-optimized financial dashboard | Medium | Desktop-first for financial data |

### Explicitly NOT in Scope (avoid scope creep)
- Automated bank transaction import
- Accounting / double-entry bookkeeping
- Tenant portal financial views
- Property valuation APIs (Zillow/similar)
- Multi-currency consolidation for portfolio view (use team default currency)
- Budgeting / budget vs actual

---

## 10. Edge Cases

### 10.1 Cash Purchase (No Mortgage)
- Mortgage section shows "No financing on this property" with option to add
- Cash flow calculation excludes mortgage cost (all rent minus operating = cash flow)
- Equity = full market value (or purchase price if no market value)
- Cash-on-Cash = NOI / Total cash invested (purchase + closing)

### 10.2 Multiple Loans on One Property
- `property_loans` table supports multiple loans per property
- UI shows each loan as a separate card within the Financing section
- Cash flow calculation sums all loan payments
- Equity calculation sums all outstanding balances

### 10.3 Refinancing
- User adds new loan record, marks old loan as "closed" (with close date)
- Closed loans show in a collapsed "Loan History" section
- Outstanding balance of closed loan goes to zero
- No automatic transfer -- user enters new loan terms manually

### 10.4 Property Sold (Historical Data)
- Property status set to a terminal state (e.g., "SOLD" -- may need new status)
- All financial data preserved and viewable
- Property excluded from portfolio-level "active" calculations
- Gain/Loss calculation: Sale price - (Purchase price + closing costs + improvements)
- Sale tracking deferred to v2 (not MVP)

### 10.5 Variable Rate Mortgage
- Monthly payment field left empty or marked as "variable"
- Cash flow calculation prompts user: "Enter current monthly payment for accurate cash flow"
- Or: user enters current payment amount which they update when it changes

### 10.6 Vacant Property (No Active Contract)
- Income = 0
- Cash flow = negative (just costs)
- NOI = negative
- Dashboard clearly shows "No active lease -- estimated vacancy cost: EUR X/mo"

### 10.7 Incomplete Financial Data
- Metrics gracefully handle missing data
- If no purchase price: Cap Rate shows "N/A -- add purchase price"
- If no mortgage: Equity = market value, cash flow excludes mortgage
- If no market value: use purchase price for equity/cap rate calculations
- Never show misleading numbers from partial data

### 10.8 Multi-Currency Properties
- Each property has its own currency (already supported)
- Loan currency matches property currency (enforced)
- Portfolio aggregation converts to team default currency (simple conversion, not real-time forex)
- v1: only aggregate properties with same currency; show "mixed currencies" warning for others

---

## Appendix A: Competitive Analysis Summary

| Feature | Stessa | Baselane | Landlord Studio | Buurman (Proposed) |
|---------|--------|----------|-----------------|-------------------|
| Property-level dashboard | Yes | Yes | Yes | Yes (Financials tab) |
| Portfolio dashboard | Yes | Yes | Yes | Yes (Reports page) |
| NOI / Cap Rate / CoC | Yes | Limited | Yes | Yes |
| Equity tracking | Yes (bank sync) | Yes | Yes | Yes (manual) |
| Multiple loans | Yes | Yes | Limited | Yes |
| Amortization schedule | Yes | No | No | No (v2) |
| Bank connection | Yes | Yes (core) | No | No (v2) |
| Tax reports | Yes | Yes (Schedule E) | Yes | Yes (basic) |
| Mobile app | Yes | Yes | Yes (core) | No |
| Free tier | Yes | Yes | Yes | Yes |

**Buurman differentiation**: Simplicity-first approach. Unlike Stessa (which is finance-heavy) or Baselane (which is banking-heavy), Buurman integrates financial data naturally into the property management workflow. The landlord sees financial health in context alongside tenants, contracts, and maintenance -- not in a separate financial app.

---

## Appendix B: Technical Notes for Implementation

### New Database Entities
1. **`property_loans`** -- Multiple loans per property (replaces flat mortgage fields)
2. **`loan_balance_snapshots`** -- Historical outstanding balance entries per loan

### Modified Entities
1. **`properties`** -- Add `closing_costs` column; deprecate mortgage columns (keep for data integrity)

### New API Endpoints
- `GET /properties/{id}/loans` -- List loans for a property
- `POST /properties/{id}/loans` -- Create a loan
- `PUT /properties/{id}/loans/{loanId}` -- Update a loan
- `DELETE /properties/{id}/loans/{loanId}` -- Soft delete a loan
- `POST /properties/{id}/loans/{loanId}/balance` -- Record balance snapshot
- `GET /properties/{id}/financials` -- Computed financial summary (metrics, cash flow)
- `GET /reports/portfolio-financials` -- Portfolio-level financial aggregation

### Frontend New Components
- `PropertyFinancialsTab` -- New tab component for property detail
- `LoanCard` -- Display/edit a single loan
- `LoanForm` -- Modal form for creating/editing loans
- `CashFlowWidget` -- Dashboard widget
- `EquityWidget` -- Dashboard widget
- `FinancialMetricsGrid` -- NOI, Cap Rate, Cash-on-Cash display
- `PortfolioNetWorthSection` -- Reports page section

### Key Calculations (Backend)
```
NOI = Annual Gross Rent - Annual Operating Expenses
    (operating = tax + insurance + HOA + management + maintenance)
    (excludes mortgage, excludes depreciation)

Cap Rate = NOI / Property Value
    (use market value if available, else purchase price)

Cash-on-Cash = Annual Cash Flow After Debt / Total Cash Invested
    Annual Cash Flow = Annual Gross Rent - Annual Operating - Annual Mortgage Payments
    Total Cash Invested = Purchase Price + Closing Costs - Mortgage Amount

Monthly Cash Flow = Monthly Rent - Monthly Mortgage - Monthly Operating

Equity = Market Value - Sum(Outstanding Loan Balances)
    (if no market value, use purchase price)
```
