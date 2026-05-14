-- Update 2026 rent regulation values with confirmed data (review: May 2026)
-- Replaces projected/expected 2026 values from V029 with values now confirmed by official sources.
-- Also captures regulatory framework changes effective in 2026 (Ireland RPZ replacement,
-- Austrian Mietpreisbremse cap, UK Renters' Rights Act, Scotland Housing Act, NL middle-rent tier).

-- ============================================================
-- Netherlands (NL) — Ministry of Housing announced 2025-12-15
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 4.10,
    notes = 'Confirmed by Ministry of Housing 2025-12-15. Wage-growth linked.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'NL')
  AND YEAR = 2026
  AND property_category = 'REGULATED';

UPDATE rent_regulation_rules
SET max_increase_percentage = 4.40,
    notes = 'Confirmed by Ministry of Housing 2025-12-15. CPI + 1%.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'NL')
  AND YEAR = 2026
  AND property_category = 'FREE_SECTOR';

-- New middle-rent (middenhuur) segment, distinct cap from 2026
INSERT INTO rent_regulation_rules (
    identifier, country_id, YEAR, property_category,
    max_increase_percentage, max_increase_type, index_name,
    effective_date, frequency, source_url, notes
) VALUES (
    'RRL01J00000000000000000141',
    (SELECT id FROM rent_regulation_countries WHERE country_code = 'NL'),
    2026, 'MIDDLE_RENT', 6.10, 'FIXED_PERCENTAGE', 'CBS middenhuur',
    '2026-01-01', 'ANNUAL',
    'https://www.rijksoverheid.nl/actueel/nieuws/2025/12/15/maximale-huurverhoging-2026-in-sociale-sector-41-middenhuur-61-en-vrije-sector-44',
    'Mid-segment rent (middenhuur) cap. Confirmed 2025-12-15.'
);

-- ============================================================
-- Germany (DE) — Mietpreisbremse extended to 2029
-- ============================================================
UPDATE rent_regulation_rules
SET notes = 'Kappungsgrenze 20%/3y unchanged. Mietpreisbremse extended to 2029 (Bundestag KW26/2025). Bavaria coverage expanded to 285 municipalities (from 208).',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'DE')
  AND region_id IS NULL
  AND YEAR = 2026;

-- ============================================================
-- Portugal (PT) — INE coefficient confirmed (Aviso 23174/2025/2)
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 2.24,
    index_value = 1.0224,
    notes = 'Aviso n.º 23174/2025/2 (INE, 2025-09-19). Coefficient 1.0224.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'PT')
  AND YEAR = 2026;

-- ============================================================
-- France (FR) — IRL T1 2026 published by INSEE
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 0.78,
    index_name = 'IRL T1 2026',
    index_value = 146.60,
    notes = 'IRL T1 2026 = 146.60, +0.78% YoY (INSEE, published 2026-04). T2 2026 expected 2026-07-10.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'FR')
  AND YEAR = 2026;

-- ============================================================
-- Spain (ES) — IRAV index continues; cap lifted 2026-04-29
-- ============================================================
UPDATE rent_regulation_rules
SET notes = 'IRAV index (monthly, INE). Extraordinary 3% cap lifted 2026-04-29. May 2026 IRAV ≈ 2.47% (mandatory for post-2023-05-26 primary-residence contracts).',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'ES')
  AND YEAR = 2026;

-- ============================================================
-- Belgium (BE) — Gezondheidsindex rebased to 2025=100; ~3.2% forecast 2026
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.20,
    notes = 'Gezondheidsindex rebased 2025=100 from 2026-01. April 2026 = 102.77. ~3.2% avg forecast (Federal Planning Bureau).',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'BE')
  AND YEAR = 2026
  AND region_id IN (
    SELECT id FROM rent_regulation_regions
    WHERE region_code IN ('VLG', 'BRU')
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'BE')
  );

-- ============================================================
-- Ireland (IE) — Residential Tenancies (Misc. Provisions) Act 2026
-- RPZ system replaced by nationwide CPI-or-2% cap from 2026-03-01
-- ============================================================
UPDATE rent_regulation_rules
SET property_category = 'ALL',
    max_increase_percentage = 2.00,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'lesser of CPI or 2%',
    effective_date = '2026-03-01',
    additional_conditions = 'New-build apartments / student accommodation with commencement notice on or after 2025-06-10 are capped at CPI only (no 2% cap).',
    source_url = 'https://www.gov.ie/en/department-of-housing-local-government-and-heritage/publications/government-reforms-to-the-rental-sector-starting-1-march-2026/',
    notes = 'RPZ system dismantled 2026-02-28. Residential Tenancies (Miscellaneous Provisions) Act 2026 applies nationwide cap: lesser of CPI or 2%. Inflation measure changed from HICP to CPI.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'IE')
  AND YEAR = 2026;

-- ============================================================
-- Italy (IT) — ISTAT FOI March 2026 confirmed
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 1.13,
    notes = 'ISTAT FOI March 2026 = +1.5% YoY. 75% applied to regulated/concordato contracts = 1.13%.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'IT')
  AND YEAR = 2026;

-- ============================================================
-- Austria (AT) — 5. Mietrechtliches Inflationslinderungsgesetz
-- Statutory cap of 1% for Richtwert / Kategoriemiete in 2026
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 1.00,
    index_name = 'Mietpreisbremse (statutory cap)',
    notes = '5. Mietrechtliches Inflationslinderungsgesetz: statutory cap of 1% for Richtwert/Kategoriemiete 2026 (2% cap planned for 2027). Effective 2026-04-01; rent increases earliest from 2026-05-01.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'AT')
  AND YEAR = 2026;

-- ============================================================
-- United Kingdom — Renters' Rights Act 2025 in force from 2026-05-01
-- ============================================================
-- England: Act in force; market rent retained with tribunal challenge mechanism
UPDATE rent_regulation_rules
SET additional_conditions = 'Max 1 rent increase per year. Section 13 procedure retained. Tenant may challenge at First-tier Tribunal. Fixed-term ASTs replaced by periodic tenancies. Section 21 abolished.',
    source_url = 'https://www.gov.uk/government/publications/renters-rights-act-2025',
    notes = 'Renters'' Rights Act 2025 received Royal Assent 2025-10-27; main provisions in force 2026-05-01. Market-based rent retained; tribunal oversight added.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'GB')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'ENG'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'GB')
  );

-- Scotland: Housing (Scotland) Act 2025 enacted; rent control zones not yet designated
UPDATE rent_regulation_rules
SET additional_conditions = 'Future rent control framework: CPI + 1%, capped at 6%/year, applied only inside designated rent control zones. Exemptions: mid-market rent, build-to-rent, student accommodation.',
    source_url = 'https://www.legislation.gov.uk/asp/2025/housing-scotland-act',
    notes = 'Housing (Scotland) Act 2025 received Royal Assent November 2025. Rent control zones expected from 2027; no statutory cap nationally in 2026.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'GB')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'SCT'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'GB')
  );

-- ============================================================
-- Sweden (SE) — 2026 collective bargaining outcome
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.40,
    source_url = 'https://www.hyresgastforeningen.se/om-oss/vad-vi-gor/hyresforhandling/hyresforhandling_2026/',
    notes = 'National average 3.4% (over 1M households finalized). Stockholm Fastighetsägarna arbitration: 3.6%. Lowest negotiated outcome in 4 years.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'SE')
  AND YEAR = 2026;

-- ============================================================
-- Norway (NO) — KPI YoY confirmed by SSB
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 3.60,
    source_url = 'https://www.ssb.no/en/priser-og-prisindekser/konsumpriser/statistikk/konsumprisindeksen',
    notes = 'KPI YoY March 2025 → March 2026 = +3.6% (SSB). Max once per 12 months per husleieloven.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'NO')
  AND YEAR = 2026;

-- ============================================================
-- Switzerland (CH) — Reference rate held at 1.25%
-- ============================================================
UPDATE rent_regulation_rules
SET index_value = 1.2500,
    source_url = 'https://www.bwo.admin.ch/de/referenzzinssatz',
    notes = 'Referenzzinssatz held at 1.25% (BWO, March 2026). Next publication June 2026. Banks expect 1.25% through end of 2026.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CH')
  AND YEAR = 2026;

-- ============================================================
-- United States — Oregon (OR) — SB 608 cap confirmed
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 9.50,
    additional_conditions = 'Manufactured-dwelling parks with >30 spaces: 6.0% cap.',
    source_url = 'https://apps.oregon.gov/oregon-newsroom/OR/DAS/Posts/Post/Correction-2026-Rent-Stabilization-Percentages',
    notes = 'SB 608 (CPI + 7%, capped at 10%): 2026 cap = 9.5% per DAS correction notice.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'US')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'OR'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'US')
  );

-- ============================================================
-- Canada — Ontario (ON), British Columbia (BC), Quebec (QC) — confirmed guidelines
-- ============================================================
UPDATE rent_regulation_rules
SET max_increase_percentage = 2.10,
    source_url = 'https://www.ontario.ca/page/residential-rent-increases',
    notes = 'Ontario CPI guideline 2.1% (RTB, announced 2025). Lowest in 4 years. Exemptions: buildings first occupied after 2018-11-15.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'ON'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  );

UPDATE rent_regulation_rules
SET max_increase_percentage = 2.30,
    source_url = 'https://news.gov.bc.ca/releases/2025HMA0067-000786',
    notes = 'BC allowable rent increase 2.3% (12-month BC CPI average). Announced autumn 2025.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'BC'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  );

UPDATE rent_regulation_rules
SET max_increase_percentage = 3.10,
    additional_conditions = 'Service-included units (e.g. seniors residences): 6.7%. New simplified calculation method in effect.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    notes = 'TAL recommended 3.1% basic increase. Applies to leases renewing 2026-04-02 through 2027-04-01.',
    updated_at = now()
WHERE country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  AND YEAR = 2026
  AND region_id = (
    SELECT id FROM rent_regulation_regions
    WHERE region_code = 'QC'
      AND country_id = (SELECT id FROM rent_regulation_countries WHERE country_code = 'CA')
  );

-- ============================================================
-- Update last_reviewed_at on all countries reviewed in May 2026
-- ============================================================
UPDATE rent_regulation_countries
SET last_reviewed_at = '2026-05-14 00:00:00',
    updated_at = now()
WHERE country_code IN (
    'NL','DE','PT','FR','ES','BE','IE','IT','AT','LU',
    'GB','SE','NO','CH','DK','FI','PL','CZ',
    'US','CA'
);
