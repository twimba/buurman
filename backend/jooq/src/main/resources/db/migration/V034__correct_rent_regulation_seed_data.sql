-- Corrects errors in V030 seed data after verification against official government sources.
-- Only UPDATE statements — no INSERTs or DELETEs.
-- ============================================================
-- NETHERLANDS (NL)
-- ============================================================
-- 2025 REGULATED: 4.10% -> 5.00% (government set 5.0% for 2025)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    notes = 'Government set max increase at 5.0% for regulated sector (social housing) 2025'
WHERE
    identifier = 'RRL01J00000000000000000007';

-- 2026 REGULATED: 4.00% -> 4.10% (government set 4.1% for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.10,
    notes = 'Government set max increase at 4.1% for regulated sector 2026'
WHERE
    identifier = 'RRL01J00000000000000000009';

-- 2026 FREE_SECTOR: 4.50% -> 4.40% (cap is 4.4% for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.40,
    notes = 'Free sector cap 4.4% for 2026'
WHERE
    identifier = 'RRL01J00000000000000000010';

-- All FREE_SECTOR rules: effective_date should be January 1 (not July 1)
-- Free sector increases can happen from January 1
UPDATE rent_regulation_rules
SET
    effective_date = concat(YEAR, '-01-01')::DATE
WHERE
    identifier IN (
        'RRL01J00000000000000000002',
        'RRL01J00000000000000000004',
        'RRL01J00000000000000000006',
        'RRL01J00000000000000000008',
        'RRL01J00000000000000000010'
    );

-- ============================================================
-- PORTUGAL (PT)
-- ============================================================
-- 2025: 6.01% -> 2.16% (actual INE coefficient for 2025)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.16,
    index_value = 1.0216,
    notes = 'INE coefficient 1.0216 for 2025 (actual published value)'
WHERE
    identifier = 'RRL01J00000000000000000024';

-- 2026: 3.50% -> 2.24% (actual INE coefficient for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.24,
    index_value = 1.0224,
    notes = 'INE coefficient 1.0224 for 2026 (actual published value)'
WHERE
    identifier = 'RRL01J00000000000000000025';

-- ============================================================
-- FRANCE (FR)
-- ============================================================
-- 2025: max_increase_percentage 2.47% -> 1.40%, index_value 145.68 -> 145.47 (Q4 2024 IRL)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.40,
    index_value = 145.47,
    index_name = 'IRL Q4 2024',
    notes = 'IRL Q4 2024 reference value 145.47. Year-on-year variation 1.40%.'
WHERE
    identifier = 'RRL01J00000000000000000029';

-- ============================================================
-- SPAIN (ES)
-- ============================================================
-- 2022: effective_date '2022-01-01' -> '2022-03-31' (Royal Decree-Law enacted March 2022)
UPDATE rent_regulation_rules
SET
    effective_date = '2022-03-31',
    notes = 'Government imposed 2% cap as emergency measure due to inflation (Royal Decree-Law, enacted March 2022).'
WHERE
    identifier = 'RRL01J00000000000000000031';

-- ============================================================
-- GERMANY (DE) — National rules
-- ============================================================
-- All national DE rules (2022-2026): frequency 'ANNUAL' -> 'TRIENNIAL'
-- Kappungsgrenze is 20% cap over 3 years, not an annual cap
UPDATE rent_regulation_rules
SET
    frequency = 'TRIENNIAL',
    notes = CASE identifier
        WHEN 'RRL01J00000000000000000011' THEN 'Kappungsgrenze: max 20% over 3 years. 15% in tight housing markets (angespannte Wohnungsmarkte).'
        WHEN 'RRL01J00000000000000000012' THEN 'Kappungsgrenze unchanged. Mietpreisbremse extended to 2029. 20% cap applies over rolling 3-year period.'
        WHEN 'RRL01J00000000000000000013' THEN 'Ongoing Mietpreisbremse. New construction exempt. 20% cap over 3 years.'
        WHEN 'RRL01J00000000000000000014' THEN 'Mietpreisbremse extended. 20% cap over 3 years. Discussion on further tightening.'
        WHEN 'RRL01J00000000000000000015' THEN 'Expected continuation. 20% Kappungsgrenze over 3 years.'
    END
WHERE
    identifier IN (
        'RRL01J00000000000000000011',
        'RRL01J00000000000000000012',
        'RRL01J00000000000000000013',
        'RRL01J00000000000000000014',
        'RRL01J00000000000000000015'
    );

-- ============================================================
-- BELGIUM (BE) — Flanders (VLG)
-- ============================================================
-- 2022: 3.20% -> 3.45%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.45,
    notes = 'Health index (gezondheidsindex) for Flanders 2022: 3.45%.'
WHERE
    identifier = 'RRL01J00000000000000000036';

-- 2023: 5.40% -> 9.10% (health index spike)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.10,
    notes = 'Health index spike for Flanders 2023: 9.10% due to energy crisis inflation.'
WHERE
    identifier = 'RRL01J00000000000000000037';

-- 2024: 3.80% -> 3.72%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.72,
    notes = 'Health index for Flanders 2024: 3.72%.'
WHERE
    identifier = 'RRL01J00000000000000000038';

-- 2025: 2.50% -> 3.22%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.22,
    notes = 'Health index for Flanders 2025: 3.22%.'
WHERE
    identifier = 'RRL01J00000000000000000039';

-- 2026: 2.20% -> 2.42%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.42,
    notes = 'Health index for Flanders 2026: 2.42%.'
WHERE
    identifier = 'RRL01J00000000000000000040';

-- ============================================================
-- BELGIUM (BE) — Brussels (BRU)
-- ============================================================
-- 2022: 3.20% -> 3.45%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.45,
    notes = 'Health index for Brussels 2022: 3.45%.'
WHERE
    identifier = 'RRL01J00000000000000000041';

-- 2023: 2.00% -> 9.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.10,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'Gezondheidsindex',
    notes = 'Health index for Brussels 2023: 9.10%. Energy-inefficient properties (EPC E, F, G) had additional caps.'
WHERE
    identifier = 'RRL01J00000000000000000042';

-- 2024: 3.80% -> 3.72%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.72,
    notes = 'Health index for Brussels 2024: 3.72%.'
WHERE
    identifier = 'RRL01J00000000000000000043';

-- 2025: 2.50% -> 3.22%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.22,
    notes = 'Health index for Brussels 2025: 3.22%.'
WHERE
    identifier = 'RRL01J00000000000000000044';

-- 2026: 2.20% -> 2.42%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.42,
    notes = 'Health index for Brussels 2026: 2.42%.'
WHERE
    identifier = 'RRL01J00000000000000000045';

-- ============================================================
-- AUSTRIA (AT)
-- ============================================================
-- 2024: 5.00% -> 0.00%, suspended by government decree
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.00,
    notes = 'CPI increase suspended for 2024 by government decree. Richtwert and Kategoriemietzins frozen.'
WHERE
    identifier = 'RRL01J00000000000000000058';

-- 2025: 4.50% -> 2.90%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.90,
    notes = 'VPI-based Richtwert adjustment 2.90% for 2025.'
WHERE
    identifier = 'RRL01J00000000000000000059';

-- 2026: 3.00% -> 1.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.00,
    notes = 'VPI-based Richtwert adjustment 1.00% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000060';

-- ============================================================
-- ITALY (IT) — FOI values corrected
-- ============================================================
-- 2022: index_value -> 2.85, max_increase_percentage (75% of FOI) -> 2.14%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.14,
    index_value = 2.85,
    notes = '75% of ISTAT FOI index variation 2.85% = 2.14%. Contratti a canone concordato.'
WHERE
    identifier = 'RRL01J00000000000000000051';

-- 2023: index_value -> 8.48, max_increase_percentage -> 6.36%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 6.36,
    index_value = 8.48,
    notes = '75% of ISTAT FOI index variation 8.48% = 6.36%.'
WHERE
    identifier = 'RRL01J00000000000000000052';

-- 2024: index_value -> 0.45, max_increase_percentage -> 0.34%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.34,
    index_value = 0.45,
    notes = '75% of ISTAT FOI index variation 0.45% = 0.34%.'
WHERE
    identifier = 'RRL01J00000000000000000053';

-- 2025: index_value -> 0.83, max_increase_percentage -> 0.62%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.62,
    index_value = 0.83,
    notes = '75% of ISTAT FOI index variation 0.83% = 0.62%.'
WHERE
    identifier = 'RRL01J00000000000000000054';

-- 2026: index_value -> 0.83, max_increase_percentage -> 0.62%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.62,
    index_value = 0.83,
    notes = '75% of ISTAT FOI index variation 0.83% = 0.62%. Projected.'
WHERE
    identifier = 'RRL01J00000000000000000055';

-- ============================================================
-- UNITED KINGDOM — Scotland (SCT)
-- ============================================================
-- 2024: Add note that 3% cap expired March 31, 2024
UPDATE rent_regulation_rules
SET
    notes = '3% rent cap expired March 31, 2024. After expiry, market rules apply with Rent Adjudication via First-tier Tribunal.'
WHERE
    identifier = 'RRL01J00000000000000000063';

-- ============================================================
-- UNITED STATES — New York (NY)
-- ============================================================
-- 2025: 2.50% -> 3.00% (RGB Order #57)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.00,
    index_name = 'RGB Order #57 (1-year)',
    notes = 'RGB Order #57: 3.00% for 1-year leases. Oct 2025 - Sep 2026 cycle.'
WHERE
    identifier = 'RRL01J00000000000000000079';

-- NY region summary: fix board name
UPDATE rent_regulation_regions
SET
    summary = 'Rent Guidelines Board (RGB). Order-based annual increases for rent-stabilized apartments.'
WHERE
    region_code = 'NY'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    );

-- ============================================================
-- UNITED STATES — Oregon (OR)
-- ============================================================
-- 2026: 10.00% -> 9.50%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.50,
    notes = 'CPI + 7% = 9.50% for 2026 (under 10% max cap).'
WHERE
    identifier = 'RRL01J00000000000000000085';

-- ============================================================
-- UNITED STATES — California (CA)
-- ============================================================
-- 2025: 8.40% -> 8.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.00,
    notes = 'AB 1482: 5% + 3.0% CPI = 8.00% effective cap for Aug 2025 cycle.'
WHERE
    identifier = 'RRL01J00000000000000000074';

-- ============================================================
-- CANADA — Ontario (ON)
-- ============================================================
-- 2026: 2.50% -> 2.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.10,
    notes = 'Ontario guideline increase 2.10% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000090';

-- ============================================================
-- CANADA — British Columbia (BC)
-- ============================================================
-- 2026: 2.50% -> 2.30%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.30,
    notes = 'BC allowable rent increase 2.30% for 2026 (CPI-based).'
WHERE
    identifier = 'RRL01J00000000000000000095';

-- ============================================================
-- CANADA — Quebec (QC)
-- ============================================================
-- 2024: 3.90% -> 4.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.00,
    notes = 'TAL recommended increase 4.00% for 2024. Highest in years due to inflation.'
WHERE
    identifier = 'RRL01J00000000000000000098';

-- 2025: 3.20% -> 5.90% (historic high)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.90,
    notes = 'TAL recommended increase 5.90% for 2025. Historic high reflecting municipal tax and insurance cost increases.'
WHERE
    identifier = 'RRL01J00000000000000000099';

-- 2026: 2.80% -> 3.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.10,
    notes = 'TAL recommended increase 3.10% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000100';

-- ============================================================
-- DENMARK (DK) — Country summary typo fix
-- ============================================================
UPDATE rent_regulation_countries
SET
    summary = 'Cost-based rent regulation for pre-1992 buildings. Newer buildings: market rent with "det lejedes vaerdi" limits.'
WHERE
    country_code = 'DK';

-- ============================================================
-- SWEDEN (SE)
-- ============================================================
-- 2022: 1.30% -> 1.70%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.70,
    notes = 'Bruksvardesystemet: collectively negotiated. ~1.7% average outcome for 2022.'
WHERE
    identifier = 'RRL01J00000000000000000106';

-- 2024: 4.60% -> 5.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    notes = 'Collectively negotiated increase 5.00% for 2024.'
WHERE
    identifier = 'RRL01J00000000000000000108';

-- 2025: 3.20% -> 4.80%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.80,
    notes = 'Collectively negotiated increase 4.80% for 2025.'
WHERE
    identifier = 'RRL01J00000000000000000109';

-- ============================================================
-- NORWAY (NO)
-- ============================================================
-- 2022: 3.50% -> 5.76%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.76,
    notes = 'CPI-linked increase 5.76% for 2022 (KPI annual average).'
WHERE
    identifier = 'RRL01J00000000000000000111';

-- 2024: 4.80% -> 3.30%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.30,
    notes = 'CPI-linked increase 3.30% for 2024.'
WHERE
    identifier = 'RRL01J00000000000000000113';

-- ============================================================
-- SWITZERLAND (CH)
-- ============================================================
-- 2025: index_value 1.75 -> 1.25, notes update
UPDATE rent_regulation_rules
SET
    index_value = 1.2500,
    notes = 'Reference interest rate dropped from 1.75% to 1.50% (March 2025), then to 1.25% (September 2025). Tenants may request rent reductions.'
WHERE
    identifier = 'RRL01J00000000000000000129';

-- 2026: index_value 1.50 -> 1.25
UPDATE rent_regulation_rules
SET
    index_value = 1.2500,
    notes = 'Reference interest rate stable at 1.25%. No rent increase entitlement for landlords when rate unchanged.'
WHERE
    identifier = 'RRL01J00000000000000000130';
