# BUUR-57: Property Financial Domain Analysis

> Expert analysis by: Real Estate Finance Specialist, Licensed CPA (Rental Property Taxation), Property Investment Analyst
> Date: 2026-02-25
> Linear Issue: [BUUR-57](https://linear.app/buurman/issue/BUUR-57/extract-mortage-payment-from-expenses)

---

## Executive Summary

The current Buurman codebase stores all financial data as flat columns on the `properties` table (purchase price, mortgage fields, annual operating costs) and treats mortgage payments as a regular expense category. This analysis defines a proper financial domain model that:

1. Extracts mortgage/financing data into dedicated entities with full lifecycle tracking
2. Replaces flat annual cost columns with structured, time-series-capable records
3. Enables meaningful financial calculations (NOI, Cash Flow, Cap Rate, Equity, DSCR)
4. Aligns expense categories with IRS Schedule E for tax-time convenience
5. Supports multi-jurisdiction tax reporting without over-engineering

**Current state in codebase:**
- `Property.java`: ~20 financial columns (purchase price, mortgage fields, 5 annual operating cost pairs with due-month strings)
- `Expense.java`: `MORTGAGE_PAYMENT` is one of 13 `ExpenseCategory` enum values
- `V002__properties.sql`: All financial columns live on the properties table
- No valuation history, no insurance policies, no financing lifecycle events

---

## 1. Property Acquisition & Valuation

### Current State
The `properties` table stores `purchase_price`, `purchase_date`, and a single `current_market_value` with `market_value_date`. There is no history of valuations -- only the latest snapshot.

### Required Data Model

#### Acquisition Record (on Property or dedicated table)
These are **one-time facts** about the purchase. Recommend keeping on the property entity but adding missing fields:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `purchase_price` | BIGINT (cents) | Yes | Already exists |
| `purchase_date` | DATE | Yes | Already exists |
| `purchase_currency` | VARCHAR(3) | Yes | Already exists as `purchase_price_currency` |
| `closing_costs` | BIGINT (cents) | No | Title insurance, legal fees, recording fees, transfer tax |
| `initial_renovation_cost` | BIGINT (cents) | No | Pre-rental renovations (added to cost basis) |
| `acquisition_method` | ENUM | Yes | `PURCHASE`, `INHERITANCE`, `GIFT`, `FORECLOSURE`, `TAX_SALE`, `1031_EXCHANGE` |
| `cost_basis` | BIGINT (cents) | Calculated | `purchase_price + closing_costs + initial_renovation_cost - land_value` |

**Must-have:** purchase_price, purchase_date, closing_costs, cost_basis
**Nice-to-have:** acquisition_method, initial_renovation_cost

#### Valuation History (new table: `property_valuations`)
**Critical for net worth tracking over time.** Replace the single `current_market_value` column.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `valuation_date` | DATE | Yes | When the valuation was performed |
| `value` | BIGINT (cents) | Yes | Appraised/estimated value |
| `currency` | VARCHAR(3) | Yes | |
| `source` | ENUM | Yes | `PURCHASE_PRICE`, `APPRAISAL`, `TAX_ASSESSMENT`, `BROKER_OPINION`, `ONLINE_ESTIMATE`, `OWNER_ESTIMATE`, `REFINANCE_APPRAISAL` |
| `appraiser_name` | VARCHAR(255) | No | For formal appraisals |
| `notes` | TEXT | No | |
| Audit columns | | | Standard created_at/updated_at/created_by/updated_by/deleted_at |

**Net Worth Calculation:**
```
Property Net Worth = Latest Valuation Value - Outstanding Loan Balance
```

**Must-have:** Entire valuation history table (core requirement from BUUR-57)
**Nice-to-have:** appraiser_name, source granularity beyond basic types

---

## 2. Financing Types

### Current State
`MortgageType` enum: `FIXED_RATE`, `VARIABLE_RATE`, `INTEREST_ONLY`, `NONE`. One set of mortgage fields per property. No support for refinancing, multiple loans, or non-mortgage financing.

### Recommended: `property_financing` Table

A property can have **multiple financing records over time** (original mortgage, refinance, HELOC). Each is a separate row.

#### Financing Types to Support

| Type | Enum Value | Prevalence | Priority |
|------|-----------|------------|----------|
| Conventional Mortgage | `MORTGAGE` | Very common | Must-have |
| Cash/Outright Purchase | `CASH` | Common | Must-have |
| Seller Financing | `SELLER_FINANCING` | Occasional | Must-have |
| HELOC | `HELOC` | Common for existing owners | Must-have |
| Hard Money Loan | `HARD_MONEY` | Fix-and-flip investors | Nice-to-have |
| Leasing/Renting (ground lease) | `GROUND_LEASE` | Rare for small landlords | Nice-to-have |
| Partnership/Syndication | `PARTNERSHIP` | Rare for 1-20 properties | Nice-to-have |
| Commercial Loan | `COMMERCIAL_LOAN` | For commercial properties | Nice-to-have |
| Private Loan | `PRIVATE_LOAN` | Family/friend loans | Nice-to-have |

#### Data Fields per Financing Record

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `financing_type` | ENUM | Yes | See types above |
| `status` | ENUM | Yes | `ACTIVE`, `PAID_OFF`, `REFINANCED`, `DEFAULTED`, `SOLD` |
| `lender_name` | VARCHAR(255) | No | Bank/lender name |
| `loan_number` | VARCHAR(100) | No | Account/loan reference number |
| `original_amount` | BIGINT (cents) | Yes | Original loan amount |
| `currency` | VARCHAR(3) | Yes | |
| `current_balance` | BIGINT (cents) | No | Manually updated or calculated from payments |
| `interest_rate` | DECIMAL(6,4) | Yes | Annual rate (e.g., 4.5000%) |
| `rate_type` | ENUM | Yes | `FIXED`, `VARIABLE`, `ADJUSTABLE` |
| `rate_adjustment_period_months` | INTEGER | No | For ARM: how often rate adjusts |
| `rate_cap` | DECIMAL(6,4) | No | Maximum rate for variable/ARM |
| `rate_floor` | DECIMAL(6,4) | No | Minimum rate for variable/ARM |
| `term_months` | INTEGER | Yes | Total loan term |
| `amortization_months` | INTEGER | No | Amortization period (may differ from term for balloon) |
| `start_date` | DATE | Yes | Loan origination date |
| `end_date` | DATE | No | Maturity date (calculated or explicit) |
| `monthly_payment` | BIGINT (cents) | Yes | Regular monthly P&I payment |
| `escrow_amount` | BIGINT (cents) | No | Monthly escrow for taxes/insurance |
| `pmi_amount` | BIGINT (cents) | No | Monthly PMI (if LTV > 80%) |
| `pmi_removal_date` | DATE | No | When PMI was/will be removed |
| `down_payment` | BIGINT (cents) | No | Initial down payment amount |
| `ltv_at_origination` | DECIMAL(6,4) | No | Loan-to-value at origination |
| `is_primary` | BOOLEAN | Yes | Is this the primary/first mortgage? |
| `refinanced_from_id` | UUID | No | FK to previous financing record |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**Key decisions:**
- **Auto-calculate amortization vs record actuals?** Recommendation: **Record actuals only.** Auto-calculating amortization requires knowing the exact day-count convention, compounding method, and whether extra payments were made. Small landlords just need to log what they actually paid. The system can *optionally* generate an estimated amortization schedule for reference, but the source of truth should be actual payments.
- **Refinancing:** Model as a new `property_financing` row with `status=ACTIVE`, and the old one gets `status=REFINANCED`. Link via `refinanced_from_id`.
- **Multiple loans:** Fully supported -- a property can have a primary mortgage + HELOC simultaneously.

**Must-have fields:** financing_type, status, original_amount, interest_rate, rate_type, term_months, start_date, monthly_payment, is_primary
**Nice-to-have fields:** escrow_amount, pmi_amount, rate_adjustment fields, down_payment, ltv_at_origination

---

## 3. Payment Components (Financing Payments)

### Current State
Mortgage payments are stored as regular expenses with `category=MORTGAGE_PAYMENT`. No breakdown of principal vs. interest.

### Recommended: `financing_payments` Table

This **replaces** the `MORTGAGE_PAYMENT` expense category. Each payment records the actual components.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `financing_id` | UUID | FK | Links to `property_financing` |
| `payment_date` | DATE | Yes | |
| `total_amount` | BIGINT (cents) | Yes | Total payment made |
| `principal_amount` | BIGINT (cents) | No | Principal portion |
| `interest_amount` | BIGINT (cents) | No | Interest portion (tax-deductible) |
| `escrow_amount` | BIGINT (cents) | No | Escrow portion |
| `pmi_amount` | BIGINT (cents) | No | PMI portion |
| `extra_principal` | BIGINT (cents) | No | Additional principal payment |
| `currency` | VARCHAR(3) | Yes | |
| `payment_type` | ENUM | Yes | `REGULAR`, `EXTRA_PAYMENT`, `LUMP_SUM`, `FINAL_PAYMENT` |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**Validation rule:** `principal_amount + interest_amount + escrow_amount + pmi_amount + extra_principal` should equal `total_amount` when all components are provided. Allow partial breakdowns (just total_amount if landlord doesn't know the split).

**Key design decision:** The system should NOT require component breakdown. Many small landlords only know the total amount. Component breakdown is optional but encouraged because:
- `interest_amount` is tax-deductible (Schedule E Line 12)
- `principal_amount` builds equity (affects net worth calculation)
- `pmi_amount` may be deductible (changes by tax year)

**Extra payments and lump sums:** Captured via `payment_type` and `extra_principal` field. These reduce the outstanding balance faster and are common for small landlords trying to pay off properties.

**Refinancing events:** When a refinance occurs, the old financing record gets `status=REFINANCED` and a new record is created. Payments continue against the new record. Historical payments remain linked to the old record for tax/audit purposes.

**Must-have:** payment_date, total_amount, principal_amount, interest_amount, payment_type
**Nice-to-have:** escrow_amount, pmi_amount, extra_principal

---

## 4. Insurance Categories

### Current State
A single `annual_insurance` amount on the property. No breakdown by type, no policy tracking.

### Insurance Types Relevant to Rental Properties

| Type | Enum Value | Prevalence | Schedule E? |
|------|-----------|------------|-------------|
| Landlord/Dwelling Policy (DP-1/DP-3) | `LANDLORD` | Required | Yes (Line 9) |
| Liability Insurance | `LIABILITY` | Very common (often bundled) | Yes (Line 9) |
| Flood Insurance | `FLOOD` | Required in flood zones | Yes (Line 9) |
| Earthquake Insurance | `EARTHQUAKE` | Regional (CA, Pacific NW) | Yes (Line 9) |
| Umbrella/Excess Liability | `UMBRELLA` | Common for multi-property | Yes (Line 9) |
| Loss of Rent / Rental Income | `LOSS_OF_RENT` | Common (often bundled) | Yes (Line 9) |
| Rent Guarantee | `RENT_GUARANTEE` | Growing in popularity | Yes (Line 9) |
| Builder's Risk (during renovation) | `BUILDERS_RISK` | Occasional | Yes (Line 9) |
| Title Insurance | `TITLE` | One-time at purchase | No (capitalized) |
| Mortgage Insurance (PMI/MIP) | `MORTGAGE_INSURANCE` | If LTV > 80% | Separate treatment |

### Recommended: `property_insurance_policies` Table

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `insurance_type` | ENUM | Yes | See types above |
| `status` | ENUM | Yes | `ACTIVE`, `EXPIRED`, `CANCELLED`, `PENDING` |
| `provider_name` | VARCHAR(255) | No | Insurance company |
| `policy_number` | VARCHAR(100) | No | |
| `annual_premium` | BIGINT (cents) | Yes | Annual cost |
| `currency` | VARCHAR(3) | Yes | |
| `payment_frequency` | ENUM | Yes | `MONTHLY`, `QUARTERLY`, `SEMI_ANNUAL`, `ANNUAL` |
| `deductible` | BIGINT (cents) | No | |
| `coverage_amount` | BIGINT (cents) | No | Maximum coverage |
| `effective_date` | DATE | Yes | Policy start |
| `expiration_date` | DATE | No | Policy end/renewal date |
| `auto_renew` | BOOLEAN | No | Does it auto-renew? |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**Must-have types:** LANDLORD, LIABILITY, FLOOD (in applicable zones)
**Nice-to-have types:** UMBRELLA, EARTHQUAKE, RENT_GUARANTEE, LOSS_OF_RENT
**Must-have fields:** insurance_type, annual_premium, effective_date, payment_frequency
**Nice-to-have fields:** deductible, coverage_amount, policy_number, provider_name, auto_renew

---

## 5. Tax Categories

### Current State
A single `annual_property_tax` field with a due-month string. No breakdown by tax type or jurisdiction.

### Property Tax Types

| Type | Enum Value | Prevalence | Notes |
|------|-----------|------------|-------|
| Property Tax (ad valorem) | `PROPERTY_TAX` | Universal | Based on assessed value |
| School District Tax | `SCHOOL_TAX` | US: Common (some states separate) | Often part of property tax bill |
| Special Assessment | `SPECIAL_ASSESSMENT` | Occasional | Infrastructure, sidewalks, sewers |
| Mello-Roos / CFD | `MELLO_ROOS` | California-specific | Community Facilities District tax |
| Transfer Tax | `TRANSFER_TAX` | One-time at purchase | Varies by jurisdiction; capitalize to cost basis |
| Occupancy Tax | `OCCUPANCY_TAX` | Short-term rentals | If applicable |
| Waste/Refuse Tax | `WASTE_TAX` | Some jurisdictions | Garbage collection levy |

### European Context (Netherlands/Belgium/Germany -- Buurman's primary market)

| Country | Tax Type | Local Name | Notes |
|---------|----------|------------|-------|
| Netherlands | Property Tax | OZB (Onroerendezaakbelasting) | Ad valorem based on WOZ value |
| Netherlands | Water Board Tax | Waterschapsbelasting | Annual, varies by region |
| Netherlands | Waste Collection | Afvalstoffenheffing | Annual, per municipality |
| Netherlands | Income Tax on Property | Box 3 (Sparen en Beleggen) | Deemed yield on property value, NOT on rental income |
| Belgium | Cadastral Income Tax | Kadastraal inkomen (KI) | Based on indexed cadastral value |
| Belgium | Property Tax | Onroerende voorheffing | Regional, based on KI |
| Germany | Property Tax | Grundsteuer | Reformed 2025, based on property value |
| Germany | Income Tax on Rent | Einkommensteuer (Anlage V) | Actual income minus actual expenses |

### Payment Patterns

- **US:** Typically annual or semi-annual (some quarterly). Often escrowed with mortgage.
- **Netherlands:** Annual billing, due in installments (monthly/quarterly auto-debit common).
- **Germany:** Quarterly (Grundsteuer). Income tax annual.
- **Belgium:** Annual (onroerende voorheffing).

### Recommended: `property_taxes` Table

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `tax_type` | ENUM | Yes | See types above |
| `tax_year` | INTEGER | Yes | The fiscal/tax year |
| `annual_amount` | BIGINT (cents) | Yes | Total annual tax amount |
| `currency` | VARCHAR(3) | Yes | |
| `payment_frequency` | ENUM | Yes | `MONTHLY`, `QUARTERLY`, `SEMI_ANNUAL`, `ANNUAL` |
| `payment_method` | ENUM | No | `DIRECT`, `ESCROWED`, `AUTO_DEBIT` |
| `assessed_value` | BIGINT (cents) | No | Assessed/WOZ value used for calculation |
| `tax_rate` | DECIMAL(8,6) | No | Effective tax rate (mills or percentage) |
| `jurisdiction` | VARCHAR(255) | No | Taxing authority name |
| `due_dates` | VARCHAR(100) | No | Comma-separated due dates (e.g., "03-01,09-01") |
| `is_deductible` | BOOLEAN | Yes | Tax-deductible on income tax? |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**Design note:** Each tax type for each year is a separate row. This allows tracking changes over time (property tax reassessments, new special assessments, etc.).

**Must-have types:** PROPERTY_TAX (covers the primary ad valorem tax in any jurisdiction)
**Nice-to-have types:** SPECIAL_ASSESSMENT, SCHOOL_TAX, MELLO_ROOS, WATER_TAX, WASTE_TAX
**Must-have fields:** tax_type, tax_year, annual_amount, payment_frequency, is_deductible
**Nice-to-have fields:** assessed_value, tax_rate, jurisdiction, payment_method

---

## 6. Fee Categories

### Current State
`annual_hoa_fee`, `annual_management_fee`, `annual_maintenance_reserve` as flat columns. Cleaning and landscaping are expense categories.

### Fee Types for Rental Properties

| Type | Enum Value | Calculation | Schedule E Line | Priority |
|------|-----------|-------------|-----------------|----------|
| Property Management | `MANAGEMENT` | % of rent (8-12%) or flat fee | Line 11 | Must-have |
| HOA Regular Dues | `HOA` | Fixed monthly/quarterly/annual | Line 19 (Other) | Must-have |
| HOA Special Assessment | `HOA_SPECIAL_ASSESSMENT` | One-time or limited-term | Line 19 (Other) | Must-have |
| Cleaning (turnover) | `CLEANING` | Per-occurrence or recurring | Line 7 | Must-have |
| Landscaping/Gardening | `LANDSCAPING` | Monthly recurring | Line 7 | Must-have |
| Pest Control | `PEST_CONTROL` | Quarterly/monthly recurring | Line 7 | Nice-to-have |
| Snow Removal | `SNOW_REMOVAL` | Seasonal recurring | Line 7 | Nice-to-have |
| Pool Maintenance | `POOL_MAINTENANCE` | Monthly recurring | Line 7 | Nice-to-have |
| Security/Alarm Monitoring | `SECURITY` | Monthly recurring | Line 19 (Other) | Nice-to-have |
| Trash/Waste Collection | `WASTE_COLLECTION` | Monthly recurring | Line 17 or 19 | Nice-to-have |
| Bookkeeping/Accounting | `ACCOUNTING` | Monthly/annual | Line 10 | Nice-to-have |

### Design Decision: Fees vs. Expenses

**Key insight:** Many of these "fees" are really **recurring expenses**. The distinction matters:

- **Recurring fees** = predictable, contract-based costs that repeat on a schedule. Used for budgeting/forecasting and expected cash flow calculations.
- **Expenses** = actual payments that occurred (may or may not match the recurring fee amount due to one-offs, adjustments, etc.)

**Recommendation:** Model recurring fees as a **separate concept** from one-time expenses. A recurring fee defines the *expected* cost; actual expense records capture what was *actually paid*.

### Recommended: `property_recurring_fees` Table

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `fee_type` | ENUM | Yes | See types above |
| `amount` | BIGINT (cents) | Yes | Per-period amount |
| `currency` | VARCHAR(3) | Yes | |
| `frequency` | ENUM | Yes | `MONTHLY`, `QUARTERLY`, `SEMI_ANNUAL`, `ANNUAL`, `PER_OCCURRENCE` |
| `calculation_method` | ENUM | No | `FIXED`, `PERCENTAGE_OF_RENT`, `PERCENTAGE_OF_VALUE` |
| `percentage_rate` | DECIMAL(6,4) | No | If calculation_method is percentage |
| `vendor_name` | VARCHAR(255) | No | Service provider |
| `start_date` | DATE | Yes | When the fee starts |
| `end_date` | DATE | No | When the fee ends (NULL = ongoing) |
| `schedule_e_line` | VARCHAR(10) | No | Which Schedule E line this maps to |
| `is_tax_deductible` | BOOLEAN | Yes | Default true for operating fees |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**Management fee specifics:** When `fee_type=MANAGEMENT` and `calculation_method=PERCENTAGE_OF_RENT`, the system can auto-calculate the expected monthly fee based on the current lease rent. The `amount` field stores the calculated amount for the current period.

**Must-have types:** MANAGEMENT, HOA, CLEANING, LANDSCAPING
**Nice-to-have types:** PEST_CONTROL, SNOW_REMOVAL, POOL_MAINTENANCE, SECURITY
**Must-have fields:** fee_type, amount, frequency, start_date, is_tax_deductible
**Nice-to-have fields:** calculation_method, percentage_rate, vendor_name, schedule_e_line

---

## 7. Financial Calculations

### Formulas and Definitions

#### 1. Monthly Cash Flow (MUST-HAVE)
```
Monthly Cash Flow = Monthly Rental Income
                  - Monthly Operating Expenses
                  - Monthly Financing Payments (P&I + escrow + PMI)
```
Where Monthly Operating Expenses include: property taxes (prorated), insurance (prorated), management fees, HOA, maintenance, repairs, utilities (if landlord-paid), and other recurring fees.

**This is the #1 metric every small landlord cares about.** "Am I making money each month?"

#### 2. Net Operating Income - NOI (MUST-HAVE)
```
Annual NOI = Annual Gross Rental Income
           + Other Income (late fees, parking, laundry, pet fees)
           - Vacancy Loss (estimated or actual)
           - Operating Expenses (taxes, insurance, management, maintenance, repairs, HOA, utilities)
```
**Excludes:** Mortgage payments, depreciation, capital expenditures, income tax.

NOI is the standard measure of property profitability independent of financing. Essential for comparing properties and calculating Cap Rate.

#### 3. Cap Rate (MUST-HAVE)
```
Cap Rate = Annual NOI / Current Market Value * 100
```
Typical range: 4-10% depending on market and property type. Used to compare investment performance across properties and markets.

#### 4. Cash-on-Cash Return (MUST-HAVE)
```
Cash-on-Cash Return = Annual Pre-Tax Cash Flow / Total Cash Invested * 100
```
Where:
- `Annual Pre-Tax Cash Flow` = Annual NOI - Annual Debt Service
- `Total Cash Invested` = Down Payment + Closing Costs + Initial Renovation

This is the actual return on the cash the investor put in. A 10% CoC return means $10K annual cash flow on $100K invested.

#### 5. Debt Service Coverage Ratio - DSCR (NICE-TO-HAVE)
```
DSCR = Annual NOI / Annual Debt Service
```
Where Annual Debt Service = total annual mortgage/loan payments (P&I only, excluding escrow).

- DSCR >= 1.25: Healthy
- DSCR = 1.0: Break-even (income barely covers debt)
- DSCR < 1.0: Negative cash flow (feeding the property)

Primarily used when refinancing or seeking new loans. Lenders typically require 1.20-1.40.

#### 6. Property Equity / Net Worth (MUST-HAVE)
```
Property Equity = Current Market Value - Outstanding Loan Balance(s)
```
For the portfolio:
```
Portfolio Net Worth = SUM(Property Equity) for all properties
```

#### 7. Total Return on Investment (NICE-TO-HAVE)
```
Total ROI = (Annual Cash Flow + Annual Principal Paydown + Annual Appreciation) / Total Cash Invested * 100
```
Where:
- Annual Appreciation = Change in market value over the year
- Annual Principal Paydown = Sum of principal portions of all payments

This captures the "hidden" returns beyond just cash flow.

#### 8. Operating Expense Ratio (NICE-TO-HAVE)
```
OER = Annual Operating Expenses / Annual Gross Rental Income * 100
```
Healthy range: 35-45% for single-family, 45-55% for multi-family. Higher = less efficient.

#### 9. Gross Rent Multiplier (NICE-TO-HAVE)
```
GRM = Property Price / Annual Gross Rental Income
```
Quick screening tool. Lower GRM = potentially better value. Typical range: 5-15.

### Priority Summary for Small Landlords

| Metric | Priority | Reason |
|--------|----------|--------|
| Monthly Cash Flow | **MUST-HAVE** | The single most important number for any landlord |
| Property Equity/Net Worth | **MUST-HAVE** | Core BUUR-57 requirement; answers "what am I worth?" |
| NOI | **MUST-HAVE** | Foundation for Cap Rate and DSCR; standard metric |
| Cap Rate | **MUST-HAVE** | Essential for evaluating and comparing properties |
| Cash-on-Cash Return | **MUST-HAVE** | Shows actual return on invested capital |
| DSCR | Nice-to-have | Mainly needed when dealing with lenders |
| Total ROI | Nice-to-have | Comprehensive but requires good valuation data |
| OER | Nice-to-have | Useful for identifying cost-inefficient properties |
| GRM | Nice-to-have | Quick screening; less actionable for existing portfolio |

---

## 8. Tax Reporting Considerations

### US: Schedule E (Form 1040) -- Supplemental Income and Loss

Schedule E requires the following data per property (up to 3 per form, additional on supplementary pages):

#### Property Information
- Physical address
- Property type
- Fair rental days vs. personal use days
- Ownership percentage

#### Income
- Line 3: Rents received
- Line 4: Royalties received (N/A for most landlords)

#### Expense Categories (Lines 5-19)

| Line | Category | Buurman Data Source |
|------|----------|-------------------|
| 5 | Advertising | Expense: `MARKETING` |
| 6 | Auto and travel | Expense: New category needed or `OTHER` |
| 7 | Cleaning and maintenance | Expense: `CLEANING`, `MAINTENANCE`, `LANDSCAPING`; Recurring fees: `CLEANING`, `LANDSCAPING`, `PEST_CONTROL`, `SNOW_REMOVAL` |
| 8 | Commissions | Expense: `FEES` or new category |
| 9 | Insurance | Insurance policies: `annual_premium` sum for tax-deductible policies |
| 10 | Legal and professional fees | Expense: `LEGAL`; Recurring fees: `ACCOUNTING` |
| 11 | Management fees | Recurring fees: `MANAGEMENT` |
| 12 | Mortgage interest paid to banks | Financing payments: `interest_amount` sum |
| 13 | Other interest | Financing payments: interest from non-bank loans |
| 14 | Repairs | Expense: `REPAIR` |
| 15 | Supplies | Expense: New category or `OTHER` |
| 16 | Taxes | Property taxes: sum of `is_deductible=true` tax records |
| 17 | Utilities | Expense: `UTILITY` |
| 18 | Depreciation | Calculated from depreciation tracking |
| 19 | Other | HOA fees, misc recurring fees, uncategorized expenses |

#### Mapping Recommendation

The `ExpenseCategory` enum should be updated to align with Schedule E lines. Add a `scheduleELine` attribute to the enum:

```java
public enum ExpenseCategory {
    ADVERTISING(5),           // Line 5
    AUTO_AND_TRAVEL(6),       // Line 6
    CLEANING(7),              // Line 7
    MAINTENANCE(7),           // Line 7
    COMMISSIONS(8),           // Line 8
    LEGAL(10),                // Line 10
    SUPPLIES(15),             // Line 15
    UTILITY(17),              // Line 17
    REPAIR(14),               // Line 14
    LANDSCAPING(7),           // Line 7
    CAPITAL_IMPROVEMENT(null), // Not directly expensed; depreciated
    OTHER(19);                 // Line 19
}
```

**Note:** Categories that move to dedicated tables get removed from ExpenseCategory:
- `INSURANCE` -> `property_insurance_policies`
- `PROPERTY_TAX` -> `property_taxes`
- `TAX` -> `property_taxes`
- `MORTGAGE_PAYMENT` -> `financing_payments`
- `PROPERTY_MANAGEMENT` -> `property_recurring_fees` (type=MANAGEMENT)
- `FEES` -> Split between recurring fees and expenses

### Depreciation Tracking

#### Current State
`depreciation_method`, `depreciation_years`, `land_value` on the property. No actual depreciation schedule.

#### Required Model

Depreciation is critical for tax reporting. The system should track:

| Field | Purpose |
|-------|---------|
| `depreciable_basis` | Cost basis minus land value: `(purchase_price + closing_costs + improvements) - land_value` |
| `depreciation_method` | `STRAIGHT_LINE` (standard for residential rental), `DECLINING_BALANCE` |
| `useful_life_years` | 27.5 (US residential), 39 (US commercial), 30 (foreign residential), varies by country |
| `placed_in_service_date` | When property was first available for rent (NOT purchase date necessarily) |
| `prior_depreciation` | Depreciation taken before using Buurman (for existing properties) |

**Depreciation calculation (straight-line):**
```
Annual Depreciation = Depreciable Basis / Useful Life Years
Monthly Depreciation = Annual / 12
```
First year and last year use mid-month convention (US).

**Capital improvements** create separate depreciation schedules. Each improvement starts its own 27.5-year (or applicable) depreciation clock.

#### Recommended: `depreciation_schedules` Table

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | UUID | PK | |
| `identifier` | VARCHAR(29) | Yes | ULID |
| `team_id` | UUID | FK | Multi-tenant |
| `property_id` | UUID | FK | |
| `description` | VARCHAR(255) | Yes | "Building", "Kitchen renovation 2024", etc. |
| `asset_type` | ENUM | Yes | `BUILDING`, `IMPROVEMENT`, `APPLIANCE`, `FIXTURE` |
| `depreciable_basis` | BIGINT (cents) | Yes | Amount being depreciated |
| `currency` | VARCHAR(3) | Yes | |
| `depreciation_method` | ENUM | Yes | `STRAIGHT_LINE`, `DECLINING_BALANCE` |
| `useful_life_years` | DECIMAL(5,1) | Yes | 27.5, 39, 5, 7, 15, etc. |
| `placed_in_service_date` | DATE | Yes | |
| `prior_depreciation` | BIGINT (cents) | No | Already taken before system entry |
| `is_active` | BOOLEAN | Yes | Still being depreciated? |
| `notes` | TEXT | No | |
| Audit columns | | | Standard |

**IRS Depreciation Periods (common):**
- Residential rental building: 27.5 years
- Commercial building: 39 years
- Land improvements (fencing, paving): 15 years
- Appliances, carpet, furniture: 5 years
- Office equipment: 7 years
- Foreign residential rental: 30 years

**Must-have:** Building-level depreciation tracking with straight-line method
**Nice-to-have:** Per-improvement depreciation, multiple methods, appliance-level tracking

### European Tax Context

| Country | Depreciation | Rental Income Basis | Key Difference from US |
|---------|-------------|--------------------|-----------------------|
| Netherlands | 2% of WOZ value (building only) | Box 3: deemed yield on NET asset value, NOT actual income | No Schedule E equivalent; no expense deduction for Box 3 |
| Belgium | Generally no depreciation for individuals | Indexed cadastral income (not actual rent) | Actual rent irrelevant for most individual landlords |
| Germany | 2% (pre-1925) or 2.5% (post-1925) of building value | Actual rental income minus actual expenses (Anlage V) | Closest to US Schedule E approach |

**Implication:** The system should support configurable depreciation rates and methods per jurisdiction, but the core model is the same.

---

## 9. Multi-Currency / Multi-Jurisdiction

### Recommendation: Support but Don't Over-Engineer

**Currency:**
- The system already stores currency codes (VARCHAR(3)) per amount field. This is sufficient.
- All financial calculations should be performed in the property's primary currency.
- Cross-property portfolio calculations require currency conversion. Recommendation: store a `default_currency` at the team level and use it for portfolio aggregations.
- Do NOT build a real-time FX conversion engine. Allow manual exchange rate entry or use a simple daily rate table.

**Jurisdiction / Tax Structure:**
- The data model should be **jurisdiction-agnostic** by design. The tables defined above work for any country:
  - `property_taxes`: tax_type enum covers Dutch OZB, German Grundsteuer, US property tax
  - `property_insurance_policies`: insurance types are universal
  - `depreciation_schedules`: useful_life_years is configurable per jurisdiction
- Add a `country` and optionally `state_province` field to the property (already exists as `country`)
- Tax reporting templates (Schedule E, Anlage V, etc.) are a **presentation layer concern**, not a data model concern

**Practical approach for v1:**
1. Store all amounts with currency codes (already done)
2. Use property's country to hint at applicable tax rules (no enforcement)
3. Depreciation rates/methods are user-configurable (not hard-coded to US rules)
4. Schedule E mapping is a **reporting feature**, not embedded in the data model
5. No multi-currency conversion in v1; portfolio dashboards show amounts in their stored currency

**Must-have:** Currency codes on all monetary fields (already exists), configurable depreciation
**Nice-to-have:** Automatic Schedule E report generation, multi-currency portfolio aggregation
**Not needed for v1:** Real-time FX, jurisdiction-specific tax rule enforcement, automatic tax filing

---

## Appendix A: Migration Strategy from Current Model

### Fields to Remove from `properties` Table

The following columns should be migrated to dedicated tables and then dropped:

| Current Column | Migrates To |
|---------------|-------------|
| `mortgage_type` | `property_financing.financing_type` + `rate_type` |
| `mortgage_amount` | `property_financing.original_amount` |
| `mortgage_amount_currency` | `property_financing.currency` |
| `mortgage_interest_rate` | `property_financing.interest_rate` |
| `mortgage_start_date` | `property_financing.start_date` |
| `mortgage_end_date` | `property_financing.end_date` |
| `mortgage_payment_variable` | `property_financing.rate_type` (VARIABLE) |
| `monthly_mortgage_payment` | `property_financing.monthly_payment` |
| `monthly_mortgage_payment_currency` | `property_financing.currency` |
| `annual_property_tax` | `property_taxes` record |
| `annual_property_tax_currency` | `property_taxes.currency` |
| `annual_property_tax_due_month` | `property_taxes.due_dates` |
| `annual_insurance` | `property_insurance_policies` record |
| `annual_insurance_currency` | `property_insurance_policies.currency` |
| `annual_insurance_due_month` | `property_insurance_policies.payment_frequency` |
| `annual_hoa_fee` | `property_recurring_fees` (type=HOA) |
| `annual_hoa_fee_currency` | `property_recurring_fees.currency` |
| `annual_hoa_fee_due_month` | `property_recurring_fees.frequency` |
| `annual_management_fee` | `property_recurring_fees` (type=MANAGEMENT) |
| `annual_management_fee_currency` | `property_recurring_fees.currency` |
| `annual_management_fee_due_month` | `property_recurring_fees.frequency` |
| `annual_maintenance_reserve` | `property_recurring_fees` (type=MAINTENANCE_RESERVE) -- new type |
| `annual_maintenance_reserve_currency` | `property_recurring_fees.currency` |
| `annual_maintenance_reserve_due_month` | `property_recurring_fees.frequency` |
| `current_market_value` | `property_valuations` (latest record) |
| `current_market_value_currency` | `property_valuations.currency` |
| `market_value_date` | `property_valuations.valuation_date` |

### Fields to KEEP on `properties` Table

| Column | Reason |
|--------|--------|
| `purchase_price` | One-time acquisition fact, rarely changes |
| `purchase_price_currency` | Paired with purchase_price |
| `purchase_date` | One-time acquisition fact |
| `depreciation_method` | Default/primary depreciation method |
| `depreciation_years` | Default/primary useful life |
| `land_value` | One-time acquisition fact (needed for cost basis) |
| `land_value_currency` | Paired with land_value |

**Optionally add to properties:**
- `closing_costs` (BIGINT)
- `initial_renovation_cost` (BIGINT)
- `acquisition_method` (ENUM)

### ExpenseCategory Enum Changes

**Remove:**
- `MORTGAGE_PAYMENT` (-> `financing_payments` table)
- `INSURANCE` (-> `property_insurance_policies` table)
- `PROPERTY_TAX` (-> `property_taxes` table)
- `TAX` (-> `property_taxes` table)
- `PROPERTY_MANAGEMENT` (-> `property_recurring_fees` with type=MANAGEMENT)
- `FEES` (ambiguous; split into recurring fees or specific expense categories)

**Keep:**
- `MAINTENANCE`, `REPAIR`, `UTILITY`, `LEGAL`, `MARKETING`, `CLEANING`, `LANDSCAPING`, `OTHER`

**Add:**
- `ADVERTISING` (rename from MARKETING for Schedule E alignment)
- `AUTO_AND_TRAVEL` (Schedule E Line 6)
- `COMMISSIONS` (Schedule E Line 8)
- `SUPPLIES` (Schedule E Line 15)
- `CAPITAL_IMPROVEMENT` (not expensed; triggers depreciation schedule)

### Data Migration Plan

1. **Create new tables** (property_valuations, property_financing, financing_payments, property_insurance_policies, property_taxes, property_recurring_fees, depreciation_schedules)
2. **Migrate existing data** from properties table columns into new table rows
3. **Migrate MORTGAGE_PAYMENT expenses** into financing_payments (total_amount only, no component breakdown for historical data)
4. **Keep old columns temporarily** (nullable, deprecated) for rollback safety
5. **Drop old columns** in a subsequent migration after verification

---

## Appendix B: New Tables Summary

| Table | Purpose | Rows per Property |
|-------|---------|-------------------|
| `property_valuations` | Market value history | 1-N (one per appraisal/estimate) |
| `property_financing` | Loans, mortgages, HELOCs | 0-N (typically 1-2) |
| `financing_payments` | Individual loan payments | 0-N (typically 12/year per loan) |
| `property_insurance_policies` | Insurance coverage | 0-N (typically 1-3 policies) |
| `property_taxes` | Tax obligations by year | 1-N per year (typically 1-3 types) |
| `property_recurring_fees` | Recurring operational fees | 0-N (typically 2-5 active fees) |
| `depreciation_schedules` | Depreciation tracking | 1-N (building + improvements) |

**Total new tables: 7**

---

## Appendix C: Dashboard Metrics Data Sources

For the property dashboard (referenced in BUUR-57):

| Dashboard Widget | Data Sources |
|-----------------|-------------|
| **Monthly Cash Flow** | Rent payments + Expenses + Financing payments + Recurring fees (prorated) |
| **Equity Overview** | Latest valuation - SUM(financing current_balance where status=ACTIVE) |
| **Mortgage Progress** | property_financing: original_amount vs current_balance, % paid off |
| **Expense Breakdown** | Expenses by category (pie chart), month-over-month trend |
| **NOI** | Annual rent - Annual operating expenses (excludes financing) |
| **Cap Rate** | NOI / Latest valuation |
| **Insurance Coverage** | Active policies with renewal dates (alert on upcoming expirations) |
| **Tax Summary** | Current year property_taxes by type |
| **Upcoming Payments** | Financing payment due dates, tax due dates, insurance renewal dates |
