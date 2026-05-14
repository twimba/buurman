-- BUUR-93 — Consolidated rent regulation overhaul (collapsed V053..V065).
-- Combines:
--   * V053 — confirmed 2026 values from official sources (May 2026 review)
--   * V054 — expert-panel validation corrections to 2022-2025 historical data
--             + missing frameworks (BE-Wallonia, GB-Wales, GB-NI, LU 5%-of-capital,
--               PL Art. 8a, CZ §2249, FI ARA, DE Mietpreisbremse §556d)
--   * V055 — schema additive: 13 typed dimensional columns
--   * V056 — backfill existing rows from compound property_category
--   * V057 — DE Länder rollout (NW, HE, BW, BB, NI, RP, SH)
--   * V058 — CA provinces (MB, NB, NS, PE, NL, AB, SK, YT, NT, NU)
--   * V059 — US sub-state (MN-STPAUL, ME-PORTLAND, NJ + 3 cities, DC, 3 MD counties, MA)
--   * V060 — FR encadrement des loyers (9 zones) + DPE 2026 coefficient reform
--   * V061 — CH cantons (GE LDTR, VD form, BS WRFG, ZH/BE/BL market)
--   * V062 — ES Catalonia regional + pre/post Ley 12/2023 historical contract-date split
--   * V063 — source_url backfill for historical Nordics/IT/AT/CH/PL/CZ/LU rows
--   * V064 — CHECK constraints on dimensional columns
--   * V065 — drop unused sector column; mark property_category deprecated
-- ============================================================
-- V053__rent_regulation_2026_updates
-- ============================================================
-- Update 2026 rent regulation values with confirmed data (review: May 2026)
-- Replaces projected/expected 2026 values from V029 with values now confirmed by official sources.
-- Also captures regulatory framework changes effective in 2026 (Ireland RPZ replacement,
-- Austrian Mietpreisbremse cap, UK Renters' Rights Act, Scotland Housing Act, NL middle-rent tier).
-- ============================================================
-- Netherlands (NL) — Ministry of Housing announced 2025-12-15
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.10,
    notes = 'Confirmed by Ministry of Housing 2025-12-15. Wage-growth linked.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'NL'
    )
    AND YEAR = 2026
    AND property_category = 'REGULATED';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.40,
    notes = 'Confirmed by Ministry of Housing 2025-12-15. CPI + 1%.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'NL'
    )
    AND YEAR = 2026
    AND property_category = 'FREE_SECTOR';

-- New middle-rent (middenhuur) segment, distinct cap from 2026
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000141',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2026,
        'MIDDLE_RENT',
        6.10,
        'FIXED_PERCENTAGE',
        'CBS middenhuur',
        '2026-01-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/actueel/nieuws/2025/12/15/maximale-huurverhoging-2026-in-sociale-sector-41-middenhuur-61-en-vrije-sector-44',
        'Mid-segment rent (middenhuur) cap. Confirmed 2025-12-15.'
    );

-- ============================================================
-- Germany (DE) — Mietpreisbremse extended to 2029
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Kappungsgrenze 20%/3y unchanged. Mietpreisbremse extended to 2029 (Bundestag KW26/2025). Bavaria coverage expanded to 285 municipalities (from 208).',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    )
    AND region_id IS NULL
    AND YEAR = 2026;

-- ============================================================
-- Portugal (PT) — INE coefficient confirmed (Aviso 23174/2025/2)
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.24,
    index_value = 1.0224,
    notes = 'Aviso n.º 23174/2025/2 (INE, 2025-09-19). Coefficient 1.0224.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PT'
    )
    AND YEAR = 2026;

-- ============================================================
-- France (FR) — IRL T1 2026 published by INSEE
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.78,
    index_name = 'IRL T1 2026',
    index_value = 146.60,
    notes = 'IRL T1 2026 = 146.60, +0.78% YoY (INSEE, published 2026-04). T2 2026 expected 2026-07-10.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    )
    AND YEAR = 2026;

-- ============================================================
-- Spain (ES) — IRAV index continues; cap lifted 2026-04-29
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'IRAV index (monthly, INE). Extraordinary 3% cap lifted 2026-04-29. May 2026 IRAV ≈ 2.47% (mandatory for post-2023-05-26 primary-residence contracts).',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'ES'
    )
    AND YEAR = 2026;

-- ============================================================
-- Belgium (BE) — Gezondheidsindex rebased to 2025=100; ~3.2% forecast 2026
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.20,
    notes = 'Gezondheidsindex rebased 2025=100 from 2026-01. April 2026 = 102.77. ~3.2% avg forecast (Federal Planning Bureau).',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'BE'
    )
    AND YEAR = 2026
    AND region_id IN (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code IN ('VLG', 'BRU')
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'BE'
            )
    );

-- ============================================================
-- Ireland (IE) — Residential Tenancies (Misc. Provisions) Act 2026
-- RPZ system replaced by nationwide CPI-or-2% cap from 2026-03-01
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_category = 'ALL',
    max_increase_percentage = 2.00,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'lesser of CPI or 2%',
    effective_date = '2026-03-01',
    additional_conditions = 'New-build apartments / student accommodation with commencement notice on or after 2025-06-10 are capped at CPI only (no 2% cap).',
    source_url = 'https://www.gov.ie/en/department-of-housing-local-government-and-heritage/publications/government-reforms-to-the-rental-sector-starting-1-march-2026/',
    notes = 'RPZ system dismantled 2026-02-28. Residential Tenancies (Miscellaneous Provisions) Act 2026 applies nationwide cap: lesser of CPI or 2%. Inflation measure changed from HICP to CPI.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IE'
    )
    AND YEAR = 2026;

-- ============================================================
-- Italy (IT) — ISTAT FOI March 2026 confirmed
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.13,
    notes = 'ISTAT FOI March 2026 = +1.5% YoY. 75% applied to regulated/concordato contracts = 1.13%.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    )
    AND YEAR = 2026;

-- ============================================================
-- Austria (AT) — 5. Mietrechtliches Inflationslinderungsgesetz
-- Statutory cap of 1% for Richtwert / Kategoriemiete in 2026
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.00,
    index_name = 'Mietpreisbremse (statutory cap)',
    notes = '5. Mietrechtliches Inflationslinderungsgesetz: statutory cap of 1% for Richtwert/Kategoriemiete 2026 (2% cap planned for 2027). Effective 2026-04-01; rent increases earliest from 2026-05-01.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'AT'
    )
    AND YEAR = 2026;

-- ============================================================
-- United Kingdom — Renters' Rights Act 2025 in force from 2026-05-01
-- ============================================================
-- England: Act in force; market rent retained with tribunal challenge mechanism
UPDATE rent_regulation_rules
SET
    additional_conditions = 'Max 1 rent increase per year. Section 13 procedure retained. Tenant may challenge at First-tier Tribunal. Fixed-term ASTs replaced by periodic tenancies. Section 21 abolished.',
    source_url = 'https://www.gov.uk/government/publications/renters-rights-act-2025',
    notes = 'Renters'' Rights Act 2025 received Royal Assent 2025-10-27; main provisions in force 2026-05-01. Market-based rent retained; tribunal oversight added.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'GB'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'ENG'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'GB'
            )
    );

-- Scotland: Housing (Scotland) Act 2025 enacted; rent control zones not yet designated
UPDATE rent_regulation_rules
SET
    additional_conditions = 'Future rent control framework: CPI + 1%, capped at 6%/year, applied only inside designated rent control zones. Exemptions: mid-market rent, build-to-rent, student accommodation.',
    source_url = 'https://www.legislation.gov.uk/asp/2025/housing-scotland-act',
    notes = 'Housing (Scotland) Act 2025 received Royal Assent November 2025. Rent control zones expected from 2027; no statutory cap nationally in 2026.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'GB'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'SCT'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'GB'
            )
    );

-- ============================================================
-- Sweden (SE) — 2026 collective bargaining outcome
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.40,
    source_url = 'https://www.hyresgastforeningen.se/om-oss/vad-vi-gor/hyresforhandling/hyresforhandling_2026/',
    notes = 'National average 3.4% (over 1M households finalized). Stockholm Fastighetsägarna arbitration: 3.6%. Lowest negotiated outcome in 4 years.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'SE'
    )
    AND YEAR = 2026;

-- ============================================================
-- Norway (NO) — KPI YoY confirmed by SSB
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.60,
    source_url = 'https://www.ssb.no/en/priser-og-prisindekser/konsumpriser/statistikk/konsumprisindeksen',
    notes = 'KPI YoY March 2025 → March 2026 = +3.6% (SSB). Max once per 12 months per husleieloven.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'NO'
    )
    AND YEAR = 2026;

-- ============================================================
-- Switzerland (CH) — Reference rate held at 1.25%
-- ============================================================
UPDATE rent_regulation_rules
SET
    index_value = 1.2500,
    source_url = 'https://www.bwo.admin.ch/de/referenzzinssatz',
    notes = 'Referenzzinssatz held at 1.25% (BWO, March 2026). Next publication June 2026. Banks expect 1.25% through end of 2026.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CH'
    )
    AND YEAR = 2026;

-- ============================================================
-- United States — Oregon (OR) — SB 608 cap confirmed
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.50,
    additional_conditions = 'Manufactured-dwelling parks with >30 spaces: 6.0% cap.',
    source_url = 'https://apps.oregon.gov/oregon-newsroom/OR/DAS/Posts/Post/Correction-2026-Rent-Stabilization-Percentages',
    notes = 'SB 608 (CPI + 7%, capped at 10%): 2026 cap = 9.5% per DAS correction notice.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'OR'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    );

-- ============================================================
-- Canada — Ontario (ON), British Columbia (BC), Quebec (QC) — confirmed guidelines
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.10,
    source_url = 'https://www.ontario.ca/page/residential-rent-increases',
    notes = 'Ontario CPI guideline 2.1% (RTB, announced 2025). Lowest in 4 years. Exemptions: buildings first occupied after 2018-11-15.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'ON'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'CA'
            )
    );

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.30,
    source_url = 'https://news.gov.bc.ca/releases/2025HMA0067-000786',
    notes = 'BC allowable rent increase 2.3% (12-month BC CPI average). Announced autumn 2025.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'BC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'CA'
            )
    );

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.10,
    additional_conditions = 'Service-included units (e.g. seniors residences): 6.7%. New simplified calculation method in effect.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    notes = 'TAL recommended 3.1% basic increase. Applies to leases renewing 2026-04-02 through 2027-04-01.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    )
    AND YEAR = 2026
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'QC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'CA'
            )
    );

-- ============================================================
-- Update last_reviewed_at on all countries reviewed in May 2026
-- ============================================================
UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 00:00:00',
    updated_at = now()
WHERE
    country_code IN (
        'NL',
        'DE',
        'PT',
        'FR',
        'ES',
        'BE',
        'IE',
        'IT',
        'AT',
        'LU',
        'GB',
        'SE',
        'NO',
        'CH',
        'DK',
        'FI',
        'PL',
        'CZ',
        'US',
        'CA'
    );

-- ============================================================
-- V054__rent_regulation_expert_validation
-- ============================================================
-- Expert validation corrections (review panels: NL/DE/BE, PT/ES/FR, IT/AT/CH/LU, IE/GB,
-- Nordics, US/CA, PL/CZ). Applies critical historical data corrections, fills missing
-- regulatory frameworks (BE-Wallonia, GB-Wales, GB-NI, LU 5%-of-capital, PL Art. 8a,
-- CZ §2249, FI ARA, DE Mietpreisbremse §556d), and corrects materially wrong values.
--
-- Out of scope (tracked separately): comprehensive German Länder rollout (NRW/HE/BW/BB/NI/RP/SH),
-- US states (MA/MN/ME/NJ/DC/MD), Canadian provinces (MB/NB/NS/PE/NL), Swiss cantons (GE/VD),
-- France encadrement des loyers cities (~69 zones).
-- ============================================================
-- Netherlands (NL) — fix 2025 regulated value, free-sector effective dates, backfill middenhuur
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    notes = 'Social housing cap: 5.00% for rents ≥ €350 (€25 absolute uplift for rents < €350). Lower-rent freeze proposal withdrawn 2025-06-03.',
    source_url = 'https://www.huurcommissie.nl/actueel/nieuws/2025/03/13/huurverhoging-per-1-juli-2025',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000007';

UPDATE rent_regulation_rules
SET
    effective_date = '2023-01-01',
    notes = 'Wet maximering huurprijsverhogingen: free-sector cap applied calendar-year basis (anniversary indexing in practice).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000004';

UPDATE rent_regulation_rules
SET
    effective_date = '2024-01-01',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000006';

UPDATE rent_regulation_rules
SET
    effective_date = '2025-01-01',
    notes = 'CPI 3.1% + 1% = 4.1%. Wet maximering extended through 2027-05-01.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000008';

UPDATE rent_regulation_rules
SET
    effective_date = '2026-01-01',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000010';

-- Backfill historical middenhuur tier (Wet betaalbare huur in force 2024-07-01)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000142',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2024,
        'MIDDLE_RENT',
        5.50,
        'CPI_LINKED',
        'CPI + 1% (transitional)',
        '2024-07-01',
        'ANNUAL',
        'https://www.huurcommissie.nl/onderwerpen/wet-betaalbare-huur',
        'Transitional: Wet betaalbare huur entered into force 2024-07-01. First-year cap aligned with free sector.'
    ),
    (
        'RRL01J00000000000000000143',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2025,
        'MIDDLE_RENT',
        7.70,
        'FIXED_PERCENTAGE',
        'CAO loonontwikkeling +1pp',
        '2025-01-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/actueel/nieuws/2024/12/17/maximale-huurverhoging-vanaf-1-januari-2025-41-procent-voor-vrije-sector-en-77-procent-voor-middenhuur',
        'Middenhuur tier (Wet betaalbare huur): 7.70% confirmed 2024-12-17.'
    );

-- ============================================================
-- Germany (DE) — fix Kappungsgrenze frequency (triennial, not annual); add Mietpreisbremse §556d
-- ============================================================
UPDATE rent_regulation_rules
SET
    frequency = 'TRIENNIAL',
    max_increase_type = 'STATUTORY_CAP',
    index_name = 'Mietspiegel',
    notes = 'Kappungsgrenze §558 BGB: rent uplift to comparable local rent (Mietspiegel) capped at 20% over rolling 36 months. Tight-market designated areas: 15% / 36 months.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    )
    AND identifier IN (
        'RRL01J00000000000000000011',
        'RRL01J00000000000000000012',
        'RRL01J00000000000000000013',
        'RRL01J00000000000000000014',
        'RRL01J00000000000000000015',
        'RRL01J00000000000000000016',
        'RRL01J00000000000000000017',
        'RRL01J00000000000000000018',
        'RRL01J00000000000000000019',
        'RRL01J00000000000000000020'
    );

-- Mietpreisbremse §556d BGB (initial rent on re-letting, capped at Mietspiegel + 10%)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000150',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2022,
        'NEW_LEASE',
        10.00,
        'CAP_OVER_INDEX',
        'Mietspiegel + 10%',
        '2022-01-01',
        'ON_RELET',
        'https://www.gesetze-im-internet.de/bgb/__556d.html',
        'Mietpreisbremse §556d BGB: initial rent in designated tight markets capped at Mietspiegel + 10%.'
    ),
    (
        'RRL01J00000000000000000151',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2023,
        'NEW_LEASE',
        10.00,
        'CAP_OVER_INDEX',
        'Mietspiegel + 10%',
        '2023-01-01',
        'ON_RELET',
        'https://www.gesetze-im-internet.de/bgb/__556d.html',
        'Mietpreisbremse Verordnungen reissued in multiple Länder.'
    ),
    (
        'RRL01J00000000000000000152',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2024,
        'NEW_LEASE',
        10.00,
        'CAP_OVER_INDEX',
        'Mietspiegel + 10%',
        '2024-01-01',
        'ON_RELET',
        'https://www.gesetze-im-internet.de/bgb/__556d.html',
        'Mietpreisbremse continues. New-build exemption (§556f BGB) and modernisation exemption apply.'
    ),
    (
        'RRL01J00000000000000000153',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2025,
        'NEW_LEASE',
        10.00,
        'CAP_OVER_INDEX',
        'Mietspiegel + 10%',
        '2025-01-01',
        'ON_RELET',
        'https://www.gesetze-im-internet.de/bgb/__556d.html',
        'Mietpreisbremse Verordnungen extended in NRW/HE/BW/SH/RP/NI/BB.'
    ),
    (
        'RRL01J00000000000000000154',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2026,
        'NEW_LEASE',
        10.00,
        'CAP_OVER_INDEX',
        'Mietspiegel + 10%',
        '2026-01-01',
        'ON_RELET',
        'https://www.bundestag.de/dokumente/textarchiv/2025/kw26-de-mietpreisbremse-1084786',
        'Mietpreisbremse §556d BGB extended to 2029-12-31 by Bundestag KW26/2025.'
    );

-- Modernisierungsumlage §559 BGB (modernisation cost pass-through)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000160',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2026,
        'MODERNISATION',
        8.00,
        'COST_PASS_THROUGH',
        '§559 BGB',
        '2026-01-01',
        'PER_MODERNISATION',
        'Absolute Kappungsgrenze: €3/m²/6y (non-tight markets); €2/m²/6y (tight markets).',
        'https://www.gesetze-im-internet.de/bgb/__559.html',
        'Modernisierungsumlage: 8% p.a. of modernisation cost may be added to rent.'
    );

-- ============================================================
-- Portugal (PT) — fix 2025 coefficient (was 6.01%, actual 2.16%) and 2024 misleading note
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.16,
    index_value = 1.0216,
    notes = 'Aviso n.º 19772/2024 (INE, 2024-09-11). Coefficient 1.0216.',
    source_url = 'https://www.portaldahabitacao.pt/coeficientes-de-atualizacao-de-rendas',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PT'
    )
    AND YEAR = 2025
    AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET
    notes = 'INE coefficient 1.0694 (+6.94%). No cap applied. State provided IRS deduction to landlords whose 2023 updates stayed below CPI.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PT'
    )
    AND YEAR = 2024
    AND property_category = 'ALL';

-- ============================================================
-- France (FR) — fix 2024/2025 IRL values; add DPE F/G rent freeze and DOM/Corse IRL variants
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.50,
    index_name = 'IRL T1 2024',
    index_value = 143.46,
    notes = 'IRL T1 2024 = 143.46, +3.50% YoY (INSEE). Bouclier loyer expired Q1 2024.',
    source_url = 'https://www.insee.fr/fr/statistiques/serie/001515333',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    )
    AND YEAR = 2024
    AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.40,
    index_name = 'IRL T1 2025',
    index_value = 145.47,
    notes = 'IRL T1 2025 = 145.47, +1.40% YoY (INSEE).',
    source_url = 'https://www.insee.fr/fr/statistiques/8558868',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    )
    AND YEAR = 2025
    AND property_category = 'ALL';

-- DPE F/G rent freeze (loi Climat & Résilience, effective 2022-08-24)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000200',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2026,
        'DPE_F_OR_G',
        0.00,
        'FIXED_PERCENTAGE',
        'Gel des loyers (loi Climat)',
        '2026-01-01',
        'ANNUAL',
        'https://www.ecologie.gouv.fr/politiques-publiques/location-gel-loyers-passoires-energetiques',
        'Since 2022-08-24, dwellings rated DPE F/G are barred from IRL revision (gel des loyers). G also barred from new leases since 2025-01-01.'
    ),
    (
        'RRL01J00000000000000000201',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2026,
        'DOM',
        0.78,
        'INDEX_LINKED',
        'IRL T1 2026 (DOM)',
        '2026-04-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/8974207',
        'Article 73 Constitution: IRL DOM T1 2026 = 143.78.'
    ),
    (
        'RRL01J00000000000000000202',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2026,
        'CORSE',
        0.78,
        'INDEX_LINKED',
        'IRL T1 2026 (Corse)',
        '2026-04-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/8974207',
        'Corsica IRL T1 2026 = 142.38.'
    );

UPDATE rent_regulation_rules
SET
    index_value = 143.78
WHERE
    identifier = 'RRL01J00000000000000000201';

UPDATE rent_regulation_rules
SET
    index_value = 142.38
WHERE
    identifier = 'RRL01J00000000000000000202';

-- ============================================================
-- Spain (ES) — fix 2022 mechanism, V053 note, add post-2023-05-26 category & zonas tensionadas
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_type = 'INDEX_LINKED',
    index_name = 'IGC capped at 2%',
    notes = 'RDL 6/2022 (2022-03-29): annual revision = min(IGC variation, 2%). IGC published monthly by INE.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'ES'
    )
    AND YEAR = 2022
    AND property_category = 'ALL';

UPDATE rent_regulation_rules
SET
    notes = 'IRAV index (monthly, INE) mandatory for primary-residence contracts signed on/after 2023-05-26. May 2026 IRAV ≈ 2.47% (March IRAV applied). Pre-2023-05-26 contracts may still use IPC unless updated. RDL 8/2026 obligatory 2-year extension ended 2026-04-29.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'ES'
    )
    AND YEAR = 2026
    AND property_category = 'ALL';

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000210',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2026,
        'CONTRACT_POST_2023_05_26',
        2.47,
        'INDEX_LINKED',
        'IRAV (Mar 2026)',
        '2026-05-01',
        'MONTHLY',
        'https://www.ine.es/jaxiT3/Tabla.htm?t=72975',
        'Mandatory IRAV (art. 18 LAU mod. Ley 12/2023) for primary-residence contracts signed on/after 2023-05-26.'
    ),
    (
        'RRL01J00000000000000000211',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2026,
        'ZONA_TENSIONADA_GRAN_TENEDOR',
        NULL,
        'INDEX_LINKED',
        'SERPAVI / Índice de Referencia',
        '2026-01-01',
        'ANNUAL',
        'https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-5214',
        'Large landlords (5+ properties in declared zona tensionada) must cap new-contract rents at SERPAVI reference. 271 Catalan municipalities declared 2024. Non-large landlords: new rent ≤ previous rent + IRAV.'
    ),
    (
        'RRL01J00000000000000000212',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2026,
        'SEASONAL',
        NULL,
        'FREE_MARKET',
        NULL,
        '2026-01-01',
        'ANNUAL',
        'https://www.boe.es/buscar/act.php?id=BOE-A-1994-26003',
        'Alquileres de temporada / habitación: outside LAU vivienda habitual; not subject to IRAV. Catalonia attempted regulation via Decret-Llei 6/2024.'
    );

-- ============================================================
-- Belgium (BE) — fix fabricated historical YoY values; add Wallonia rules; EPC-tiered conditions
-- ============================================================
-- Flanders 2022-2025 corrections (gezondheidsindex annual averages per Statbel)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.30,
    additional_conditions = 'From 2022-10-01 to 2023-09-30: EPC A/B/C = full index; EPC D = 50% of index; EPC E/F/no-EPC = 0% (freeze).',
    notes = 'Gezondheidsindex YoY annual average 2022 ≈ 9.3% (Statbel). EPC-tiered freeze active from 2022-10-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000036';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.30,
    additional_conditions = 'EPC-tiered freeze active until 2023-09-30 (VLG decree 2022-09-30).',
    notes = 'Gezondheidsindex YoY annual average 2023 ≈ 4.3% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000037';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.30,
    notes = 'Gezondheidsindex YoY 2024 = 3.28% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000038';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.60,
    notes = 'Gezondheidsindex YoY 2025 = 2.63% (Statbel). Rebased 2025=100 from 2026-01-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000039';

-- Brussels 2022-2025 corrections
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.30,
    notes = 'Gezondheidsindex YoY annual average 2022 ≈ 9.3% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000041';

UPDATE rent_regulation_rules
SET
    effective_date = '2022-10-14',
    additional_conditions = 'EPC A/B/C/D: full indexation. EPC E: 50% of index. EPC F/G: 0% (no indexation). In force 2022-10-14 → 2023-10-13.',
    notes = 'Brussels Ordinance 14-10-2022. EPC-tiered freeze. Single percentage shown is the cap for non-restricted properties.',
    source_url = 'https://be.brussels/en/housing/rental/lease-contracts/rental-price-indexation',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000042';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.30,
    notes = 'Gezondheidsindex YoY 2024 = 3.28% (Statbel).',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000043';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.60,
    notes = 'Gezondheidsindex YoY 2025 = 2.63% (Statbel). Rebased 2025=100 from 2026-01-01.',
    source_url = 'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000044';

-- Wallonia rules 2022-2026 (previously empty)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000170',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WAL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'BE'
                )
        ),
        2022,
        'ALL',
        9.30,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2022-01-01',
        'ANNUAL',
        'From 2022-11-01: EPC A/B/C = full index; EPC D = 75% of index; EPC E = 50%; EPC F/G/no-PEB = 0% (freeze).',
        'https://wallex.wallonie.be/',
        'Walloon Decree 19-10-2022. Gezondheidsindex YoY 2022 ≈ 9.3%.'
    ),
    (
        'RRL01J00000000000000000171',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WAL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'BE'
                )
        ),
        2023,
        'ALL',
        4.30,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2023-01-01',
        'ANNUAL',
        'EPC-tiered freeze active until 2023-10-31. After 2023-11-01: PEB restrictions lifted.',
        'https://wallex.wallonie.be/',
        'Gezondheidsindex YoY 2023 ≈ 4.3% (Statbel).'
    ),
    (
        'RRL01J00000000000000000172',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WAL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'BE'
                )
        ),
        2024,
        'ALL',
        3.30,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2024-01-01',
        'ANNUAL',
        NULL,
        'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
        'Gezondheidsindex YoY 2024 = 3.28% (Statbel).'
    ),
    (
        'RRL01J00000000000000000173',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WAL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'BE'
                )
        ),
        2025,
        'ALL',
        2.60,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2025-01-01',
        'ANNUAL',
        NULL,
        'https://statbel.fgov.be/en/themes/consumer-prices/health-index',
        'Gezondheidsindex YoY 2025 = 2.63% (Statbel).'
    ),
    (
        'RRL01J00000000000000000174',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WAL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'BE'
                )
        ),
        2026,
        'ALL',
        3.20,
        'INDEX_LINKED',
        'Gezondheidsindex (rebased 2025=100)',
        '2026-01-01',
        'ANNUAL',
        NULL,
        'https://www.plan.be/en/data/consumer-price-index-inflation-forecasts',
        'Federal Planning Bureau forecast 3.2% for 2026.'
    );

-- ============================================================
-- Ireland (IE) — fix 2022 effective date and mechanism (RPZ HICP-linked, not fixed 2%)
-- ============================================================
UPDATE rent_regulation_rules
SET
    effective_date = '2021-12-11',
    max_increase_type = 'INDEX_LINKED',
    index_name = 'lesser of HICP or 2%',
    notes = 'RPZ: lesser of HICP or 2% per annum (Residential Tenancies (Amendment) Act 2021, in force 11-Dec-2021). Pre-11-Dec-2021 cap was 4%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000046';

-- ============================================================
-- Italy (IT) — fix all 2022-2025 ISTAT FOI values; add canone libero (FREE_MARKET); cedolare secca
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.60,
    notes = '75% of ISTAT FOI Jan-2022 YoY ~4.8% = 3.60%. Applied at contract anniversary, on written request.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000051';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 7.95,
    notes = '75% of ISTAT FOI Jan-2023 YoY ~10.6% = 7.95%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000052';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.60,
    notes = '75% of ISTAT FOI Jan-2024 YoY ~0.8% = 0.60%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000053';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.05,
    notes = '75% of ISTAT FOI Jan-2025 YoY ~1.4% = 1.05%.',
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000054';

UPDATE rent_regulation_rules
SET
    additional_conditions = 'Cedolare secca regime (D.Lgs. 23/2011 art. 3 c.11) forbids any ISTAT adjustment for the contract''s duration.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    );

-- Canone libero (free market 4+4) — 100% FOI permitted if contract specifies
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000250',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2026,
        'FREE_MARKET',
        1.50,
        'INDEX_LINKED',
        'ISTAT FOI (100%)',
        '2026-01-01',
        'ANNUAL',
        'https://www.confedilizia.it/locazioni/indice-istat/',
        'Canone libero (4+4 contracts): up to 100% of FOI permitted when contractually stipulated.'
    );

-- ============================================================
-- Austria (AT) — fix 2024 (suspended) and 2025 (5.00% capped, not 4.50%); add free-market category
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Richtwert valorisation 1-Apr-2023 = +8.6% (statutory; no deferment).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000057';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    effective_date = '2024-04-01',
    notes = '3. MILG: Richtwert- und Kategorievalorisierung für 2024 ausgesetzt (no statutory adjustment).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000058';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    notes = '1-Apr-2025 valorisation capped at 5% by 3./4. MILG (uncapped VPI Ø 2024 / Ø 2023 ≈ 7.8%).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000059';

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000240',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2026,
        'FREE_MARKET',
        'MARKET_RENT',
        '2026-01-01',
        'ANNUAL',
        'https://www.ris.bka.gv.at/GeltendeFassung.wxe?Abfrage=Bundesnormen&Gesetzesnummer=10002531',
        'Vollausnahme MRG §1 Abs.2: post-1953 new builds, detached single-family homes, holiday lets — freier Mietzins, only contractual indexation. ~50% of AT private rental stock.'
    );

-- ============================================================
-- Switzerland (CH) — fix 2024 effective date; split 2025 into two cuts (March and September)
-- ============================================================
UPDATE rent_regulation_rules
SET
    effective_date = '2023-12-01',
    notes = 'Reference rate raised to 1.75% on 1-Dec-2023; held throughout 2024.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000128';

UPDATE rent_regulation_rules
SET
    index_value = 1.5000,
    effective_date = '2025-03-03',
    notes = 'Reference rate cut to 1.50% on 3-Mar-2025.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000129';

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        index_value,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000270',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        2025,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        1.2500,
        '2025-09-02',
        'QUARTERLY',
        'https://www.bwo.admin.ch/de/referenzzinssatz',
        'Reference rate cut to 1.25% on 2-Sep-2025 (lowest since 2008).'
    );

-- ============================================================
-- Luxembourg (LU) — restructure: residential leases are CAPITAL_BASED (5% formula), no STATEC auto-index
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_category = 'RESIDENTIAL',
    max_increase_type = 'CAPITAL_BASED',
    index_name = '5% of revalued invested capital',
    additional_conditions = 'Rent ceiling = 5% p.a. of capital invested (revalued by STATEC construction-cost index, depreciated 2%/yr, energy-class reduction). No automatic CPI clause permitted (Loi 21-09-2006 Art. 3). Biennial revision allowed (Art. 5).',
    source_url = 'https://logement.public.lu/fr/proprietaire/logement-location/faq-bail-a-loyer.html',
    notes = 'Bail à usage d''habitation: no STATEC auto-indexation; 5%-of-capital ceiling retained after Loi du 23 juillet 2024 (proposed reduction to 3.5% dropped).',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'LU'
    );

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000241',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2026,
        'COMMERCIAL',
        'INDEX_LINKED',
        'Indice des prix à la consommation (STATEC)',
        '2026-01-01',
        'ANNUAL',
        'https://statistiques.public.lu/fr/themes/economie-finances/prix.html',
        'Bail commercial: STATEC CPI auto-indexation permitted unless excluded by contract.'
    );

-- ============================================================
-- Sweden (SE) — correct 2022-2025 negotiated outcomes per Hyresgästföreningen
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.70,
    source_url = 'https://www.hyresgastforeningen.se/',
    notes = 'Hyresgästföreningen: rikssnitt 1.7% (collective bargaining outcome).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000106';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.20,
    notes = 'Bruksvärdessystemet: rikssnitt 4.2% (higher reflecting 2022 CPI peak).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000107';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.10,
    notes = 'Bruksvärdessystemet: rikssnitt 5.1% (Stockholm arbitrator spread 5.0–6.3%).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000108';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.80,
    notes = 'Bruksvärdessystemet: rikssnitt 4.8% (Stockholm arbitrator higher).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000109';

-- ============================================================
-- Norway (NO) — correct 2022 / 2024 / 2025 KPI values per SSB
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.80,
    additional_conditions = '§4-2 KPI-justering: max én gang per 12 mnd, 1 mnd skriftlig varsel. §4-3 markedsleie: tidligst etter 2 år 6 mnd, 6 mnd varsel.',
    notes = 'KPI YoY 2022 ≈ 5.8% (SSB annual avg).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000111';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.10,
    notes = 'KPI YoY 2024 ≈ 3.1% (SSB annual avg).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000113';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.50,
    notes = 'KPI YoY 2025 ≈ 3.5% (SSB published rate for 2025 anniversaries).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000114';

-- ============================================================
-- Denmark (DK) — backfill missing percentages; record 2022 statutory 4% cap
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.10,
    max_increase_type = 'COST_BASED',
    index_name = 'NPI (Nettoprisindeks)',
    index_value = 9.10,
    additional_conditions = 'Statutory 4% cap applied 2022-2024 (Lov nr. 197/2022) on NPI-linked rent increases.',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI årsgennemsnit +9.1%, but capped at 4% by Lov nr. 197/2022.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000101';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.00,
    max_increase_type = 'FIXED_PERCENTAGE',
    index_name = 'NPI (capped at 4%)',
    source_url = 'https://www.retsinformation.dk/eli/lta/2022/197',
    notes = 'NPI 2023 ≈ 3.3%, capped at 4% by Lov nr. 197/2022.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000102';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.40,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'Statutory 4% cap expired 2024-12-31; NPI ≈ 1.4%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000103';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.40,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI ≈ 1.4% (cost-based system; landlord pass-through documented cost increases).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000104';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.10,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'NPI (Nettoprisindeks)',
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI April-2026 YoY ≈ 2.1%. Pre-1992 cost-based system per Lejeloven §§ 19-22 (Boligreguleringsloven repealed 2022-07-01).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000105';

-- Add POST_1991 and SMÅHUSE categories for 2026
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000260',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2026,
        'POST_1991',
        'MARKET',
        '2026-01-01',
        'ANNUAL',
        'https://www.dst.dk/',
        'Post-1991 buildings: fri leje per Lejeloven §53 stk. 3-5. Contractual indexation typically NPI-linked.'
    ),
    (
        'RRL01J00000000000000000261',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2026,
        'SMÅHUSE',
        'COMPARATIVE',
        '2026-01-01',
        'ANNUAL',
        'https://www.dst.dk/',
        'Småhuse (≤6 units pre-1992): det lejedes værdi rule. Comparable-rent benchmark.'
    );

-- ============================================================
-- Finland (FI) — backfill missing index_name; add ARA social housing category
-- ============================================================
UPDATE rent_regulation_rules
SET
    index_name = 'elinkustannusindeksi (Tilastokeskus)',
    additional_conditions = 'No statutory cap. AHVL 481/1995 § 27: min 2-month written notice for rent change.',
    notice_period_days = 60,
    source_url = 'https://stat.fi/vuokran-tarkistaminen-elinkustannusindeksilla',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FI'
    );

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000230',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2026,
        'ARA_SOCIAL',
        'COST_BASED',
        'omakustannusvuokra',
        '2026-01-01',
        'ANNUAL',
        'https://ohjeet.ara.fi/fi/vuokranmaaritys-hyresbestamning/v4/vuokrantarkistus-elinkustannusindeksilla',
        'ARA-funded social housing: cost-recovery rent (omakustannusperiaate) per ARAVA-laki and AKVL.'
    );

-- ============================================================
-- Poland (PL) — fix property categorisation; add Art. 8a 3%-of-reconstruction-value rule
-- ============================================================
-- Add PRIVATE_REGULATED, INSTITUTIONAL, OCCASIONAL categories for 2026
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000220',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2026,
        'PRIVATE_REGULATED',
        3.00,
        'THRESHOLD_PERCENTAGE',
        'wskaźnik przeliczeniowy kosztu odtworzenia 1 m²',
        '2026-01-01',
        90,
        'SEMIANNUAL',
        'Increases above 3% of reconstruction value challengeable by tenant within 2 months. Min 6 months between increases. Landlord must justify on demand within 14 days.',
        'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
        'Ustawa o ochronie praw lokatorów art. 8a, art. 9. Wskaźnik set per voivodeship every 6 months by wojewoda.'
    ),
    (
        'RRL01J00000000000000000221',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2026,
        'INSTITUTIONAL',
        NULL,
        'MARKET_RENT',
        NULL,
        '2026-01-01',
        NULL,
        'ANNUAL',
        NULL,
        'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
        'Najem instytucjonalny (art. 19f-19j): exempt from art. 8a cap; contractual indexation only.'
    ),
    (
        'RRL01J00000000000000000222',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2026,
        'OCCASIONAL',
        NULL,
        'MARKET_RENT',
        NULL,
        '2026-01-01',
        NULL,
        'ANNUAL',
        NULL,
        'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
        'Najem okazjonalny (art. 19a-19e): exempt from art. 8a cap; notarised eviction declaration required.'
    );

UPDATE rent_regulation_rules
SET
    notes = 'Mieszkania komunalne / TBS / SIM: rent set by uchwała gminy; subject to wskaźnik-based ceiling (art. 7 ust. 5).'
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PL'
    )
    AND property_category = 'MUNICIPAL';

-- ============================================================
-- Czech Republic (CZ) — add §2249 NOZ 20%-over-3-years statutory ceiling
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 20.00,
    max_increase_type = 'STATUTORY_CEILING',
    frequency = 'TRIENNIAL',
    notice_period_days = 90,
    additional_conditions = '§2249 NOZ: unilateral landlord proposal capped at 20% over rolling 3 years, up to local market level. Tenant may refuse; court sets price. §2254: deposit ≤ 3× monthly rent. Contractual inflation indexation (ČSÚ CPI) permitted.',
    source_url = 'https://www.zakonyprolidi.cz/cs/2012-89#p2249',
    notes = 'Deregulated 2012. §2249 NOZ caps unilateral increases at 20% / 3 years.',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CZ'
    );

-- ============================================================
-- United Kingdom — Scotland 2024/2025 formula-based cap; complete Wales rules; add Northern Ireland
-- ============================================================
-- Scotland 2024: Rent Adjudication Regulations 2024 (tapered formula)
UPDATE rent_regulation_rules
SET
    effective_date = '2024-04-01',
    max_increase_percentage = 12.00,
    max_increase_type = 'FORMULA_BASED',
    notes = 'Cost of Living rent cap ended 1-Apr-2024. Rent Adjudication (Temporary Modifications) (Scotland) Regulations 2024: market-gap formula = 6% + 0.33% per pp over 6%, capped at 12%.',
    source_url = 'https://www.gov.scot/publications/cost-of-living-rent-and-eviction/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000063';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 12.00,
    max_increase_type = 'FORMULA_BASED',
    notes = 'Tapered rent adjudication formula extended to 31-Mar-2025 then status quo restored. Housing (Scotland) Bill in progress.',
    source_url = 'https://spice-spotlight.scot/2025/04/07/rent-adjudication-a-return-to-the-status-quo/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000064';

-- England 2026: enrich Renters' Rights Act conditions
UPDATE rent_regulation_rules
SET
    additional_conditions = 'Max 1 rent increase per year. Section 13 notice procedure retained (2 months notice). Tenant may challenge at First-tier Tribunal; tribunal cannot exceed landlord''s proposed rent. Periodic assured tenancies replace fixed-term ASTs. Section 21 abolished. Max 1 month rent in advance after agreement signed. Code-registered PBSA exempt from APT regime; private student HMOs in scope.',
    source_url = 'https://www.legislation.gov.uk/ukpga/2025/26/contents',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000070';

-- Wales rules 2022-2026 (previously empty)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000180',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WLS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2022,
        'ALL',
        'MARKET_RENT',
        '2022-12-01',
        'ANNUAL',
        'Renting Homes (Wales) Act 2016 in force 1-Dec-2022. Standard occupation contracts; rent variation requires 2 months written notice. No statutory cap.',
        'https://www.gov.wales/renting-homes',
        'Welsh framework switched from ASTs to standard occupation contracts on 1-Dec-2022.'
    ),
    (
        'RRL01J00000000000000000181',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WLS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2023,
        'ALL',
        'MARKET_RENT',
        '2023-01-01',
        'ANNUAL',
        'Rent variation requires 2 months written notice. No statutory cap.',
        'https://www.gov.wales/renting-homes',
        'Standard market-based system continues.'
    ),
    (
        'RRL01J00000000000000000182',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WLS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2024,
        'ALL',
        'MARKET_RENT',
        '2024-01-01',
        'ANNUAL',
        'Rent variation requires 2 months written notice. No statutory cap.',
        'https://www.gov.wales/written-statement-publication-white-paper-adequate-housing-and-fair-rents',
        'October 2024 White Paper: Welsh Government rejected rent controls, citing Scotland evidence.'
    ),
    (
        'RRL01J00000000000000000183',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WLS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2025,
        'ALL',
        'MARKET_RENT',
        '2025-01-01',
        'ANNUAL',
        'Rent variation requires 2 months written notice. No statutory cap.',
        'https://www.gov.wales/renting-homes',
        'No statutory rent cap. Renters'' Rights Act 2025 extends limited discrimination protections to Wales.'
    ),
    (
        'RRL01J00000000000000000184',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'WLS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2026,
        'ALL',
        'MARKET_RENT',
        '2026-01-01',
        'ANNUAL',
        'Rent variation requires 2 months written notice. No statutory cap.',
        'https://www.gov.wales/renting-homes',
        'Market-based; no rent control planned.'
    );

-- Northern Ireland: new region + rules
INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000016',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        'NIR',
        'Northern Ireland',
        'Private Tenancies Act (NI) 2022. Rent may rise once per 12 months with 3 months written notice. No statutory percentage cap.'
    );

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        notice_period_days,
        frequency,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000185',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NIR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2023,
        'ALL',
        'MARKET_RENT',
        '2023-04-01',
        90,
        'ANNUAL',
        'Section 7 in force 1-Apr-2023: rent may increase max once per 12 months; 3 months written notice required. 12-month protection from grant of tenancy.',
        'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
        'Private Tenancies Act (NI) 2022 effective 1-Apr-2023.'
    ),
    (
        'RRL01J00000000000000000186',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NIR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2024,
        'ALL',
        'MARKET_RENT',
        '2024-01-01',
        90,
        'ANNUAL',
        'Max one rent increase per 12 months; 3 months written notice.',
        'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
        'Continued operation of Private Tenancies Act (NI) 2022.'
    ),
    (
        'RRL01J00000000000000000187',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NIR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2025,
        'ALL',
        'MARKET_RENT',
        '2025-01-01',
        90,
        'ANNUAL',
        'Max one rent increase per 12 months; 3 months written notice.',
        'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
        'No change in 2025.'
    ),
    (
        'RRL01J00000000000000000188',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NIR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'GB'
                )
        ),
        2026,
        'ALL',
        'MARKET_RENT',
        '2026-01-01',
        90,
        'ANNUAL',
        'Max one rent increase per 12 months; 3 months written notice. Department reserve power to extend 12-month protection up to 2 years (not yet exercised).',
        'https://www.communities-ni.gov.uk/articles/sections-7-12-private-tenancies-act-northern-ireland-2022',
        'No statutory percentage cap. Renters'' Rights Act 2025 does not extend to NI (housing is devolved).'
    );

-- ============================================================
-- US — California historical regional CPI values (LA region as canonical, not statutory ceiling)
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.60,
    notes = 'LA region Aug-2022 cycle: 5% + 3.6% Apr-Apr CPI = 8.6%. Statewide ceiling 10%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000071';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.80,
    notes = 'LA/SF Aug-2023 cycle: 5% + 3.8% CPI = 8.8%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000072';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.90,
    notes = 'LA Aug-2024 cycle: 5% + 3.9% CPI = 8.9%. SF Aug-2024 ≈ 8.9%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000073';

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.00,
    notes = 'LA Aug-2025 cycle: 5% + 3.0% CPI = 8.0%. SF Aug-2025: 6.3%.',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000074';

-- US — New York 2025 actual RGB Order #57 (not the 2.50% projection)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.00,
    index_name = 'RGB Order #57 (1-year)',
    notes = 'Order #57: 3.00% 1-year, 4.50% 2-year. Adopted 2025-06-30.',
    source_url = 'https://rentguidelinesboard.cityofnewyork.us/2025-26-apartment-loft-order-57/',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000079';

UPDATE rent_regulation_rules
SET
    notes = 'Preliminary; RGB Order #58 vote scheduled 2026-06-22. Range under discussion: 0–2% (1-year), 0–4% (2-year).',
    updated_at = now()
WHERE
    identifier = 'RRL01J00000000000000000080';

-- Fix NY 2023 note (2-year was 2.75% for first 12 months + 3.20% for second)
UPDATE rent_regulation_rules
SET
    notes = 'Order #55: 3.00% 1-year; 2-year = 2.75% (yr 1) + 3.20% (yr 2).',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    )
    AND YEAR = 2023
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'NY'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    );

-- ============================================================
-- Canada — Quebec TAL: fix 2024 (4.00%) and 2025 (5.90%) basic values
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.00,
    additional_conditions = 'Electric-heat: 4.0%. Gas-heat: 4.5%. Oil-heat: 7.3%.',
    notes = 'TAL 2024 base (unheated): 4.0%.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    )
    AND YEAR = 2024
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'QC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'CA'
            )
    );

UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.90,
    additional_conditions = 'Electric-heat: 5.8%. Gas-heat: 7.3%. Largest increase in 3+ decades.',
    notes = 'TAL 2025 base (unheated): 5.9%.',
    source_url = 'https://www.tal.gouv.qc.ca/en/calculation-for-rent-increase',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    )
    AND YEAR = 2025
    AND region_id = (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'QC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'CA'
            )
    );

-- ============================================================
-- Bump last_reviewed_at on all countries reviewed in this expert validation pass
-- ============================================================
UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:00:00',
    updated_at = now()
WHERE
    country_code IN (
        'NL',
        'DE',
        'PT',
        'FR',
        'ES',
        'BE',
        'IE',
        'IT',
        'AT',
        'LU',
        'GB',
        'SE',
        'NO',
        'CH',
        'DK',
        'FI',
        'PL',
        'CZ',
        'US',
        'CA'
    );

-- ============================================================
-- V055__rent_regulation_dimensional_columns
-- ============================================================
-- BUUR-93 §3 — schema evolution for multi-dimensional rent-regulation regimes.
-- Adds typed dimensional columns so that the single overloaded `property_category` slot
-- can be replaced by an axis-per-column representation. NULL = wildcard "applies regardless
-- of this axis". Existing rows are populated in V056.
ALTER TABLE rent_regulation_rules
-- Regulatory regime (was overloaded into property_category): REGULATED, FREE_SECTOR,
-- MIDDLE_RENT, CEILING_RENT, KAPPUNGSGRENZE, MIETPREISBREMSE, ZONA_TENSIONADA,
-- COST_BASED, RICHTWERT, SMÅHUSE, FREE_MARKET, etc.
ADD COLUMN regime VARCHAR(40),
-- Building / occupancy type: RESIDENTIAL, COMMERCIAL, STUDENT, SOCIAL, MIXED, SEASONAL.
ADD COLUMN property_type VARCHAR(40),
-- Contract form: CANONE_LIBERO, CANONE_CONCORDATO, INSTITUTIONAL, OCCASIONAL,
-- STANDARD_AST, ASSURED_PERIODIC, etc.
ADD COLUMN contract_type VARCHAR(40),
-- Tax regime: CEDOLARE_SECCA, ORDINARY, IRS_DEDUCTION, etc.
ADD COLUMN tax_regime VARCHAR(40),
-- Phase of tenancy: EXISTING_LEASE, NEW_LEASE, RENEWAL, RELET, MODERNISATION.
ADD COLUMN tenancy_phase VARCHAR(20),
-- Build-year window: e.g. DK pre/post 1992, AT pre/post 1953.
ADD COLUMN build_year_min INT,
ADD COLUMN build_year_max INT,
-- EPC class window: e.g. BE EPC-tiered freezes, FR DPE F/G ban.
ADD COLUMN epc_class_min CHAR(1),
ADD COLUMN epc_class_max CHAR(1),
-- Contract signature date window: e.g. ES pre/post Ley 12/2023 (2023-05-26 threshold).
ADD COLUMN contract_signed_after DATE,
ADD COLUMN contract_signed_before DATE,
-- Landlord-size threshold: e.g. ES "gran tenedor" (5+ properties in zona tensionada).
ADD COLUMN landlord_min_properties INT,
-- Region-internal area code (e.g. FR encadrement zone, MD county sub-area).
ADD COLUMN area_code VARCHAR(40);

-- Index supporting the specificity-ranked rule-resolution query.
CREATE INDEX idx_rent_reg_rules_dimensions ON rent_regulation_rules (
    country_id,
    YEAR,
    regime,
    property_type,
    contract_type
);

-- ============================================================
-- V056__rent_regulation_backfill_dimensions
-- ============================================================
-- BUUR-93 §3 — backfill the new dimensional columns from compound `property_category`
-- values written by V029/V053/V054. After this migration every row has at least
-- `property_type` populated; specific dimensional fields are filled where the original
-- property_category encoded that axis.
-- ============================================================
-- NL regimes (REGULATED / FREE_SECTOR / MIDDLE_RENT) → regime + property_type
-- ============================================================
UPDATE rent_regulation_rules
SET
    regime = 'REGULATED',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'REGULATED';

UPDATE rent_regulation_rules
SET
    regime = 'FREE_SECTOR',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'FREE_SECTOR';

UPDATE rent_regulation_rules
SET
    regime = 'MIDDLE_RENT',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'MIDDLE_RENT';

-- ============================================================
-- DK build-era split (PRE_1992 / POST_1991 / SMÅHUSE)
-- ============================================================
UPDATE rent_regulation_rules
SET
    build_year_max = 1991,
    property_type = 'RESIDENTIAL',
    regime = 'COST_BASED'
WHERE
    property_category = 'PRE_1992';

UPDATE rent_regulation_rules
SET
    build_year_min = 1992,
    property_type = 'RESIDENTIAL',
    regime = 'FREE_MARKET'
WHERE
    property_category = 'POST_1991';

UPDATE rent_regulation_rules
SET
    build_year_max = 1991,
    property_type = 'RESIDENTIAL',
    regime = 'SMÅHUSE'
WHERE
    property_category = 'SMÅHUSE';

-- ============================================================
-- FI ARA social housing
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'SOCIAL',
    regime = 'COST_BASED'
WHERE
    property_category = 'ARA_SOCIAL';

-- ============================================================
-- FR DPE F/G freeze + DOM + Corse
-- ============================================================
UPDATE rent_regulation_rules
SET
    epc_class_min = 'F',
    epc_class_max = 'G',
    property_type = 'RESIDENTIAL',
    regime = 'EPC_FREEZE'
WHERE
    property_category = 'DPE_F_OR_G';

UPDATE rent_regulation_rules
SET
    area_code = 'DOM',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'DOM';

UPDATE rent_regulation_rules
SET
    area_code = 'CORSE',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'CORSE';

-- ============================================================
-- ES contract-date thresholds
-- ============================================================
UPDATE rent_regulation_rules
SET
    contract_signed_after = DATE '2023-05-26',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'CONTRACT_POST_2023_05_26';

UPDATE rent_regulation_rules
SET
    contract_signed_before = DATE '2023-05-26',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'CONTRACT_PRE_LEY_VIVIENDA';

-- ============================================================
-- ES zonas tensionadas (gran tenedor) + seasonal
-- ============================================================
UPDATE rent_regulation_rules
SET
    regime = 'ZONA_TENSIONADA',
    landlord_min_properties = 5,
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'ZONA_TENSIONADA_GRAN_TENEDOR';

UPDATE rent_regulation_rules
SET
    property_type = 'SEASONAL'
WHERE
    property_category = 'SEASONAL';

-- ============================================================
-- IT canone libero (FREE_MARKET) + cedolare secca conditional
-- ============================================================
UPDATE rent_regulation_rules
SET
    regime = 'FREE_MARKET',
    contract_type = 'CANONE_LIBERO',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'FREE_MARKET'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    );

-- Italian REGULATED rows (canone concordato) — set contract type explicitly
UPDATE rent_regulation_rules
SET
    contract_type = 'CANONE_CONCORDATO',
    property_type = 'RESIDENTIAL'
WHERE
    property_category = 'REGULATED'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    );

-- Cedolare secca tax-regime marker (additional_conditions text already captures the rule).
-- We flag the dimension so rule-resolution can match a non-cedolare lease against these rows.
UPDATE rent_regulation_rules
SET
    tax_regime = 'NON_CEDOLARE_SECCA'
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    )
    AND additional_conditions ILIKE '%cedolare secca%';

-- ============================================================
-- AT Vollausnahme (FREE_MARKET, post-1953)
-- ============================================================
UPDATE rent_regulation_rules
SET
    regime = 'FREE_MARKET',
    property_type = 'RESIDENTIAL',
    build_year_min = 1953
WHERE
    property_category = 'FREE_MARKET'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'AT'
    );

-- AT REGULATED rows are Richtwert (Altbau pre-1953)
UPDATE rent_regulation_rules
SET
    regime = 'RICHTWERT',
    property_type = 'RESIDENTIAL',
    build_year_max = 1953
WHERE
    property_category = 'REGULATED'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'AT'
    );

-- ============================================================
-- LU residential vs commercial
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    regime = 'CAPITAL_BASED'
WHERE
    property_category = 'RESIDENTIAL'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'LU'
    );

UPDATE rent_regulation_rules
SET
    property_type = 'COMMERCIAL'
WHERE
    property_category = 'COMMERCIAL';

-- ============================================================
-- DE Mietpreisbremse (NEW_LEASE) and Modernisierungsumlage tenancy phases
-- ============================================================
UPDATE rent_regulation_rules
SET
    tenancy_phase = 'NEW_LEASE',
    property_type = 'RESIDENTIAL',
    regime = 'MIETPREISBREMSE'
WHERE
    property_category = 'NEW_LEASE';

UPDATE rent_regulation_rules
SET
    tenancy_phase = 'MODERNISATION',
    property_type = 'RESIDENTIAL',
    regime = 'MODERNISATION_PASS_THROUGH'
WHERE
    property_category = 'MODERNISATION';

-- DE Kappungsgrenze (existing-lease cap, currently property_category = 'ALL' on DE rows)
UPDATE rent_regulation_rules
SET
    tenancy_phase = 'EXISTING_LEASE',
    regime = 'KAPPUNGSGRENZE',
    property_type = 'RESIDENTIAL'
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    )
    AND property_category = 'ALL';

-- ============================================================
-- IE RPZ → property_type = RESIDENTIAL
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    regime = 'RPZ'
WHERE
    property_category = 'RPZ';

-- ============================================================
-- US-NY RENT_STABILIZED
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    regime = 'RENT_STABILIZED'
WHERE
    property_category = 'RENT_STABILIZED';

-- ============================================================
-- PL contract-category split (MUNICIPAL / PRIVATE_REGULATED / INSTITUTIONAL / OCCASIONAL)
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'SOCIAL',
    regime = 'MUNICIPAL'
WHERE
    property_category = 'MUNICIPAL';

UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    regime = 'PRIVATE_REGULATED'
WHERE
    property_category = 'PRIVATE_REGULATED';

UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    contract_type = 'INSTITUTIONAL',
    regime = 'FREE_MARKET'
WHERE
    property_category = 'INSTITUTIONAL';

UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL',
    contract_type = 'OCCASIONAL',
    regime = 'FREE_MARKET'
WHERE
    property_category = 'OCCASIONAL';

-- ============================================================
-- Catch-all: property_category = 'ALL' → property_type = 'RESIDENTIAL'
-- (most countries use ALL as a generic residential cap)
-- ============================================================
UPDATE rent_regulation_rules
SET
    property_type = 'RESIDENTIAL'
WHERE
    property_type IS NULL
    AND property_category = 'ALL';

-- ============================================================
-- V057__rent_regulation_de_laender
-- ============================================================
-- BUUR-93 §5 — German Länder Mietpreisbremse rollout.
-- Adds 7 new regions: NW, HE, BW, BB, NI, RP, SH (Schleswig-Holstein has Kappungsgrenze
-- only, no Mietpreisbremse). Federal §556d BGB extended to 2029-12-31 by Bundestag KW26/2025.
-- Each region gets a 15%/3-year Kappungsgrenze rule per year 2022-2026.
-- ============================================================
-- New regions (RRG017-023)
-- ============================================================
INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000017',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'NW',
        'Nordrhein-Westfalen',
        'Mieterschutzverordnung NRW (MietSchVO NRW) of 28.01.2025 designates 57 tight-market municipalities (Köln, Düsseldorf, Bonn, Münster, Aachen) with Mietpreisbremse and 15%/3-year Kappungsgrenze. In force until 31.12.2029 (Kappungsgrenze 28.02.2030).'
    ),
    (
        'RRG01J00000000000000000018',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'HE',
        'Hessen',
        'Hessische Mieterschutzverordnung covers 49 tight-market municipalities (Frankfurt am Main, Wiesbaden, Darmstadt, Offenbach, Marburg) with Mietpreisbremse and 15%/3-year Kappungsgrenze. Current Verordnung valid until 25.11.2026; alignment with federal 2029 horizon planned.'
    ),
    (
        'RRG01J00000000000000000019',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'BW',
        'Baden-Württemberg',
        'Mietpreisbegrenzungsverordnung BW (GBl. 2025 Nr. 144) with Gebietskulisse of 130 tight-market municipalities effective 01.01.2026. Companion Kappungsgrenzenverordnung enforces 15%/3-year cap. Notable cities: Stuttgart, Karlsruhe, Heidelberg, Freiburg, Tübingen.'
    ),
    (
        'RRG01J00000000000000000020',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'BB',
        'Brandenburg',
        'New Mietpreisbegrenzungsverordnung and Kappungsgrenzenverordnung (Kabinettsbeschluss 25.11.2025) cover 36 Berliner-Umland municipalities (Potsdam-Umland: Teltow, Kleinmachnow, Stahnsdorf, Falkensee, Königs Wusterhausen, Oranienburg) from 01.01.2026 to 31.12.2029.'
    ),
    (
        'RRG01J00000000000000000021',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'NI',
        'Niedersachsen',
        'Niedersächsische Mieterschutzverordnung designates 57 tight-market municipalities (Hannover, Braunschweig, Göttingen, Oldenburg, Osnabrück) with Mietpreisbremse and 15%/3-year Kappungsgrenze; extended to 31.12.2029.'
    ),
    (
        'RRG01J00000000000000000022',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'RP',
        'Rheinland-Pfalz',
        'Landesverordnung 16.09.2025 designates 7 tight-market areas (Mainz, Ludwigshafen, Speyer, Worms, Landau in der Pfalz, Landkreis Alzey-Worms, Rhein-Pfalz-Kreis) with Mietpreisbremse and 15%/3-year Kappungsgrenze. In force 08.10.2025 → 31.12.2029.'
    ),
    (
        'RRG01J00000000000000000023',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'SH',
        'Schleswig-Holstein',
        'Kappungsgrenzenverordnung effective 01.05.2024 → 30.04.2029 reduces Kappungsgrenze to 15%/3 years in 62 tight-market municipalities (Kiel, Lübeck, Flensburg, Sylt, Ahrensburg). NO active Mietpreisbremse Verordnung.'
    );

-- ============================================================
-- Kappungsgrenze rules (15%/3y) for 7 regions × 5 years = 35 rules
-- Identifier range: RRL300-334
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
WITH
    de_country AS (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    ),
    region_data AS (
        SELECT
            'NW' AS code,
            'https://recht.nrw.de/lmi/owa/br_vbl_detail_text?anw_nr=6&vd_id=22119' AS url,
            'MietSchVO NRW 28.01.2025: 15%/3y in 57 designated municipalities.' AS note
        UNION ALL
        SELECT
            'HE',
            'https://wirtschaft.hessen.de/Mieterschutzverordnung-vom-18-November-2020-GVBl-S-802',
            'Hessische Mieterschutzverordnung: 15%/3y in 49 designated municipalities.'
        UNION ALL
        SELECT
            'BW',
            'https://mlw.baden-wuerttemberg.de/de/bauen-wohnen/wohnungsbau/mietpreisbremse',
            'Kappungsgrenzenverordnung BW: 15%/3y in 130 designated municipalities (effective 2026-01-01).'
        UNION ALL
        SELECT
            'BB',
            'https://mil.brandenburg.de/mil/de/presse/detail/~25-11-2025-kabinett-mietpreisbremse-und-kappungsgrenzenverordnung',
            'Brandenburg Kappungsgrenzenverordnung: 15%/3y in 36 Berliner-Umland municipalities (effective 2026-01-01).'
        UNION ALL
        SELECT
            'NI',
            'https://www.mw.niedersachsen.de/startseite/bauen_wohnen/soziales_wohnungswesen/niedersachsische_mieterschutzverordnung/niedersachsische-mieterschutzverordnung-217000.html',
            'Niedersächsische Mieterschutzverordnung: 15%/3y in 57 designated municipalities.'
        UNION ALL
        SELECT
            'RP',
            'https://fm.rlp.de/themen/bauen-und-wohnen/rheinland-pfaelzische-mieterschutzregelungen',
            'Landesverordnung 16.09.2025: 15%/3y in 7 designated areas (Mainz, Ludwigshafen, Speyer, Worms, Landau, +2 Landkreise).'
        UNION ALL
        SELECT
            'SH',
            'https://www.schleswig-holstein.de/DE/landesregierung/ministerien-behoerden/IV/Presse/PI/2024/240319_kappungsgrenzenverordnung',
            'Kappungsgrenzenverordnung 01.05.2024: 15%/3y in 62 designated municipalities. No Mietpreisbremse.'
    ),
    region_order AS (
        SELECT
            rd.code,
            rd.url,
            rd.note,
            CASE rd.code
                WHEN 'NW' THEN 0
                WHEN 'HE' THEN 1
                WHEN 'BW' THEN 2
                WHEN 'BB' THEN 3
                WHEN 'NI' THEN 4
                WHEN 'RP' THEN 5
                WHEN 'SH' THEN 6
            END AS region_idx
        FROM
            region_data rd
    ),
    year_series AS (
        SELECT
            2022 AS YEAR,
            0 AS year_idx
        UNION ALL
        SELECT
            2023,
            1
        UNION ALL
        SELECT
            2024,
            2
        UNION ALL
        SELECT
            2025,
            3
        UNION ALL
        SELECT
            2026,
            4
    )
SELECT
    'RRL01J00000000000000000' || lpad(
        (300 + ro.region_idx * 5 + ys.year_idx)::TEXT,
        3,
        '0'
    ) AS identifier,
    (
        SELECT
            id
        FROM
            de_country
    ) AS country_id,
    r.id AS region_id,
    ys.year AS YEAR,
    'ALL' AS property_category,
    15.00 AS max_increase_percentage,
    'STATUTORY_CAP' AS max_increase_type,
    'Kappungsgrenze (tight market)' AS index_name,
    make_date(ys.year, 1, 1) AS effective_date,
    'TRIENNIAL' AS frequency,
    'KAPPUNGSGRENZE' AS regime,
    'RESIDENTIAL' AS property_type,
    'EXISTING_LEASE' AS tenancy_phase,
    ro.url AS source_url,
    ro.note AS notes
FROM
    region_order ro
    JOIN rent_regulation_regions r ON r.region_code = ro.code
    AND r.country_id = (
        SELECT
            id
        FROM
            de_country
    )
    CROSS JOIN year_series ys;

UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:30:00',
    updated_at = now()
WHERE
    country_code = 'DE';

-- ============================================================
-- V058__rent_regulation_ca_provinces
-- ============================================================
-- BUUR-93 §6 — Canadian provinces/territories rollout (beyond ON/BC/QC already in V029).
-- 10 new regions, 50 yearly rules (5 per region × 2022-2026).
-- Capped jurisdictions: MB (1.8% in 2026), NB (3.0%), NS (5.0%, until 2027), PE (2.0%).
-- Market jurisdictions: NL, AB, SK, YT, NT, NU.
-- ============================================================
-- New regions (RRG024-033)
-- ============================================================
INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000024',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'MB',
        'Manitoba',
        'Residential Tenancies Branch sets annual guideline (RTA C.C.S.M. c. R119 Part 9). Calendar year; 3-month notice; once per 12 months. Exempt: buildings first occupied after 2001-04-09, personal-care homes, non-profit/co-op, high-rent threshold (2026: $1,615/mo).'
    ),
    (
        'RRG01J00000000000000000025',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'NB',
        'New Brunswick',
        'Residential Tenancies Tribunal / Service NB. Cap reintroduced 2023 (3% one-year), extended through fiscal 2025-2026 = 3.0%. Above-cap permitted up to 9% with renovation justification.'
    ),
    (
        'RRG01J00000000000000000026',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'NS',
        'Nova Scotia',
        'Residential Tenancies Program. Interim Residential Rental Increase Cap Act (SNS 2021 c 22): 5% cap extended through 2027-12-31. 4-month written notice. Cap applies to same tenant only.'
    ),
    (
        'RRG01J00000000000000000027',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'PE',
        'Prince Edward Island',
        'Island Regulatory and Appeals Commission (IRAC). Annual guideline; 3-month notice; once per 12 months. Above-guideline applications permitted (capital cost / tax increase).'
    ),
    (
        'RRG01J00000000000000000028',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'NL',
        'Newfoundland and Labrador',
        'Residential Tenancies Office (Service NL). No percentage cap. 12-month notice for fixed-term/yearly tenancies; 3 months for monthly; 8 weeks for weekly. Once per 12 months.'
    ),
    (
        'RRG01J00000000000000000029',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'AB',
        'Alberta',
        'Residential Tenancy Dispute Resolution Service. No cap. 3-month notice (periodic monthly), 90 days (fixed-term). Once per 12 months between increases.'
    ),
    (
        'RRG01J00000000000000000030',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'SK',
        'Saskatchewan',
        'Office of Residential Tenancies. No cap. For month-to-month: 6 months notice (no tenants association) or 12 months (with association). Fixed-term: cannot raise during term. Once per 12 months.'
    ),
    (
        'RRG01J00000000000000000031',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'YT',
        'Yukon',
        'Residential Tenancies Office. No percentage cap (rent control repealed). 3-month notice; once per 12 months.'
    ),
    (
        'RRG01J00000000000000000032',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'NT',
        'Northwest Territories',
        'Office of the Rental Officer. No percentage cap. 3-month notice; minimum 12 months between increases.'
    ),
    (
        'RRG01J00000000000000000033',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'NU',
        'Nunavut',
        'Office of the Rental Officer. No percentage cap. 3-month notice; minimum 12 months between increases.'
    );

-- ============================================================
-- Capped provinces — year-by-year rules
-- Manitoba (MB) — RRL500-504
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        additional_conditions,
        source_url,
        notes
    )
SELECT
    (
        'RRL01J00000000000000000' || lpad((500 + (rd.idx * 5) + ys.idx)::TEXT, 3, '0')
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    ),
    r.id,
    ys.year,
    'ALL',
    rd.cap_arr[ys.idx + 1],
    rd.rule_type,
    rd.index_name,
    make_date(ys.year, rd.effective_month, 1),
    rd.notice_days,
    'ANNUAL',
    rd.regime,
    'RESIDENTIAL',
    'EXISTING_LEASE',
    rd.conditions,
    rd.source_url,
    rd.notes
FROM
    (
        VALUES
            (
                0,
                'MB',
                ARRAY[0.0::NUMERIC, 0.0, 3.0, 1.7, 1.8],
                'FIXED_PERCENTAGE'::TEXT,
                'MB RTB guideline'::TEXT,
                1::INT,
                90::INT,
                'STATUTORY_CAP'::TEXT,
                'Buildings first occupied after 2001-04-09, personal-care homes, non-profit/co-op, and high-rent units (2026 threshold $1,615/mo) exempt.'::TEXT,
                'https://www.gov.mb.ca/cca/rtb/rentincreaseguideline/currentrentguideline.html'::TEXT,
                'Manitoba RTB annual guideline. 2026 = 1.8%.'::TEXT
            ),
            (
                1,
                'NB',
                ARRAY[NULL::NUMERIC, 3.8, 3.0, 3.0, 3.0],
                'FIXED_PERCENTAGE'::TEXT,
                'NB Service Tribunal cap'::TEXT,
                4::INT,
                60::INT,
                'STATUTORY_CAP'::TEXT,
                'Cap reintroduced 2023 (3.8% one-year), reduced to 3% for 2024-2026. Above-cap up to 9% with renovation justification.'::TEXT,
                'https://www2.gnb.ca/content/gnb/en/news/news_release.2025.06.0232.html'::TEXT,
                'NB 2022 was market (no cap); cap 2023-onwards.'::TEXT
            ),
            (
                2,
                'NS',
                ARRAY[2.0::NUMERIC, 2.0, 5.0, 5.0, 5.0],
                'FIXED_PERCENTAGE'::TEXT,
                'Interim Residential Rental Increase Cap Act'::TEXT,
                1::INT,
                120::INT,
                'STATUTORY_CAP'::TEXT,
                'Cap applies to same tenant only; new rentals exempt. Extended through 2027-12-31 by 2024 amendments (raised from 2% to 5% in 2024).'::TEXT,
                'https://news.novascotia.ca/en/2024/09/06/changes-rent-cap-residential-tenancies-act'::TEXT,
                'NS Interim Cap Act SNS 2021 c 22. 2024: cap raised to 5%.'::TEXT
            ),
            (
                3,
                'PE',
                ARRAY[1.0::NUMERIC, 0.0, 3.0, 3.0, 2.0],
                'FIXED_PERCENTAGE'::TEXT,
                'IRAC annual guideline'::TEXT,
                1::INT,
                90::INT,
                'STATUTORY_CAP'::TEXT,
                'Above-guideline applications permitted (capital cost / tax increase). 2023 cap was 0% for heated units.'::TEXT,
                'https://peirentaloffice.ca/allowable-rent-increases/'::TEXT,
                'PE IRAC guideline. 2026 = 2.0%.'::TEXT
            )
    ) AS rd (
        idx,
        code,
        cap_arr,
        rule_type,
        index_name,
        effective_month,
        notice_days,
        regime,
        conditions,
        source_url,
        notes
    )
    CROSS JOIN (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx)
    JOIN rent_regulation_regions r ON r.region_code = rd.code
    AND r.country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    );

-- ============================================================
-- Market provinces/territories — single MARKET_RENT row per year
-- NL: RRL520-524, AB: 525-529, SK: 530-534, YT: 535-539, NT: 540-544, NU: 545-549
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        additional_conditions,
        source_url,
        notes
    )
SELECT
    (
        'RRL01J00000000000000000' || lpad((520 + (rd.idx * 5) + ys.idx)::TEXT, 3, '0')
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    ),
    r.id,
    ys.year,
    'ALL',
    'MARKET_RENT',
    make_date(ys.year, 1, 1),
    rd.notice_days,
    'ANNUAL',
    'FREE_MARKET',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    rd.conditions,
    rd.source_url,
    rd.notes
FROM
    (
        VALUES
            (
                0,
                'NL',
                90::INT,
                'No cap; 12 months notice for fixed-term/yearly; 3 months for monthly; 8 weeks for weekly. Once per 12 months.'::TEXT,
                'https://www.gov.nl.ca/dgsnl/landlord/'::TEXT,
                'NL: market rent.'::TEXT
            ),
            (
                1,
                'AB',
                90::INT,
                'No cap; 3-month notice (periodic monthly), 90 days (fixed-term). Once per 12 months.'::TEXT,
                'https://www.alberta.ca/rent-increases'::TEXT,
                'AB: market rent.'::TEXT
            ),
            (
                2,
                'SK',
                180::INT,
                'No cap. Periodic month-to-month: 6 months notice (no tenants association) or 12 months (with association). Fixed-term not raisable during term.'::TEXT,
                'https://www.saskatchewan.ca/residents/housing/renting-leasing-and-tenant-rights'::TEXT,
                'SK: market rent.'::TEXT
            ),
            (
                3,
                'YT',
                90::INT,
                'No cap (rent control repealed). 3-month notice; once per 12 months.'::TEXT,
                'https://yukon.ca/en/housing-and-property/renting/learn-about-rent-rules-yukon'::TEXT,
                'YT: market rent.'::TEXT
            ),
            (
                4,
                'NT',
                90::INT,
                'No cap; 3-month notice; minimum 12 months between increases.'::TEXT,
                'https://www.justice.gov.nt.ca/en/rental-office/'::TEXT,
                'NT: market rent.'::TEXT
            ),
            (
                5,
                'NU',
                90::INT,
                'No cap; 3-month notice; minimum 12 months between increases.'::TEXT,
                'https://www.gov.nu.ca/justice/information/office-rental-officer'::TEXT,
                'NU: market rent.'::TEXT
            )
    ) AS rd (
        idx,
        code,
        notice_days,
        conditions,
        source_url,
        notes
    )
    CROSS JOIN (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx)
    JOIN rent_regulation_regions r ON r.region_code = rd.code
    AND r.country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CA'
    );

UPDATE rent_regulation_countries
SET
    has_regional_regulations = TRUE,
    last_reviewed_at = '2026-05-14 12:35:00',
    updated_at = now()
WHERE
    country_code = 'CA';

-- ============================================================
-- V059__rent_regulation_us_sub_state
-- ============================================================
-- BUUR-93 §7 — US sub-state rent regulation rollout.
-- 11 new regions: MA placeholder, MN-STPAUL, ME-PORTLAND, NJ + 3 cities, DC, 3 MD counties.
-- ============================================================
-- New regions (RRG034-03E)
-- ============================================================
INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000034',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'MA',
        'Massachusetts',
        'Chapter 40P (1994) bans local rent control statewide. 2026 ballot Initiative Petition 25-21 (lower of 5% or CPI) pending. Local home-rule petitions (Boston CPI+6%/max 10%, Cambridge, Somerville CPI+2%/max 5%) stalled at state legislature.'
    ),
    (
        'RRG01J00000000000000000035',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'MN-STPAUL',
        'Saint Paul, Minnesota',
        'Voter-approved Rent Stabilization Ordinance (2021). Flat 3% cap per 12 months. 2024 amendments permanently exempt buildings constructed after 2004 and permit vacancy decontrol.'
    ),
    (
        'RRG01J00000000000000000036',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'ME-PORTLAND',
        'Portland, Maine',
        'Citizen Initiative 1 (2020 voter referendum). Annual cap = 70% of Greater Boston CPI-U for prior 12 months, set Sept 1, effective Jan 1. Banking allowed up to 10%. 90-day notice; one increase per 12 months.'
    ),
    (
        'RRG01J00000000000000000037',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'NJ',
        'New Jersey',
        'No statewide cap. ~100 municipalities maintain local rent control ordinances administered by municipal boards; rules vary widely. Common pattern: lower of fixed % (3-6%) or local CPI.'
    ),
    (
        'RRG01J00000000000000000038',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'NJ-NEWARK',
        'Newark, New Jersey',
        'Rent Control Ordinance. Annual cap = lower of 4% or CPI. Applies to buildings with 3+ units. Hardship increases available.'
    ),
    (
        'RRG01J00000000000000000039',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'NJ-JERSEYCITY',
        'Jersey City, New Jersey',
        'Rent Control Ordinance. Annual cap = lower of 4% or CPI (measured 3 months pre-/post-lease). Applies to buildings with 5+ units; single-family and small properties exempt.'
    ),
    (
        'RRG01J00000000000000000040',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'NJ-HOBOKEN',
        'Hoboken, New Jersey',
        'Rent Control Ordinance administered by Rent Leveling and Stabilization Office. Annual cap = lower of 5% or CPI.'
    ),
    (
        'RRG01J00000000000000000041',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'DC',
        'District of Columbia',
        'Rental Housing Act of 1985. Standard tenants: CPI-W + 2%, max 10%. Elderly/disabled: lesser of CPI-W, Social Security COLA, or 5%. Vacancy: 10% (20% if vacant >10 days, capped). Rent control year = May 1 - April 30.'
    ),
    (
        'RRG01J00000000000000000042',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'MD-MONTGOMERY',
        'Montgomery County, Maryland',
        'HOME Act / Rent Stabilization Law effective 2024-07-23. Annual cap = lower of CPI-U + 3% or 6% hard cap. Year runs Jul 1 - Jun 30. Buildings <23 years old exempt. 90-day notice; banking up to 10%.'
    ),
    (
        'RRG01J00000000000000000043',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'MD-PRINCEGEORGES',
        'Prince George''s County, Maryland',
        'Permanent Rent Stabilization and Protection Act of 2024 (effective 2024-10-17). Standard: lower of CPI-U + 3% or 6%. Senior housing (62+): lower of CPI-U or 4.5%. Year runs Jul 1 - Jun 30.'
    ),
    (
        'RRG01J00000000000000000044',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'MD-TAKOMAPARK',
        'Takoma Park, Maryland',
        'Rent Stabilization Ordinance (Ch. 6.20). Annual cap = 100% Washington-Baltimore CPI-U (March-to-March). Effective July 1 each year.'
    );

-- ============================================================
-- Rules: MN-STPAUL (flat 3% × 5 years), RRL400-404
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((400 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'MN-STPAUL'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'ALL',
    3.00,
    'FIXED_PERCENTAGE',
    make_date(ys.year, 1, 1),
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.stpaul.gov/departments/safety-inspections/rent-buy-sell-property/rent-stabilization',
    'St. Paul Rent Stabilization Ordinance: flat 3% per 12 months. 2024 amendments permanently exempt post-2004 construction; vacancy decontrol permitted.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- ============================================================
-- Rules: ME-PORTLAND (70% CPI), RRL405-409
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((405 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'ME-PORTLAND'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'ALL',
    ys.cap,
    'CPI_LINKED',
    '70% of Greater Boston CPI-U',
    make_date(ys.year, 1, 1),
    90,
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://portlandmaine.gov/1148/Rent-Control-Rental-Housing-Rights',
    'Portland Citizen Initiative 1 (2020). Banking up to 10%; one increase per 12 months.'
FROM
    (
        VALUES
            (2022, 0, 4.30::NUMERIC),
            (2023, 1, 7.00),
            (2024, 2, 2.00),
            (2025, 3, 2.50),
            (2026, 4, 2.20)
    ) AS ys (YEAR, idx, cap);

-- ============================================================
-- Rules: NJ-NEWARK, NJ-JERSEYCITY, NJ-HOBOKEN — 2026 only (boards set year-by-year)
-- RRL410-412
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000410',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NJ-NEWARK'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2026,
        'ALL',
        4.00,
        'CPI_LINKED',
        'Lower of 4% or NY-Newark-JC CPI-U',
        '2026-01-01',
        'ANNUAL',
        'RENT_STABILIZED',
        'RESIDENTIAL',
        'EXISTING_LEASE',
        'https://www.newarknj.gov/',
        'Newark Rent Control Ordinance: lower of 4% or CPI. Buildings with 3+ units.'
    ),
    (
        'RRL01J00000000000000000411',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NJ-JERSEYCITY'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2026,
        'ALL',
        4.00,
        'CPI_LINKED',
        'Lower of 4% or NY-Newark-JC CPI-U',
        '2026-01-01',
        'ANNUAL',
        'RENT_STABILIZED',
        'RESIDENTIAL',
        'EXISTING_LEASE',
        'https://www.jerseycitynj.gov/cityhall/housinganddevelopment/housingpreservation/landlordtenantrelations',
        'Jersey City Rent Control Ordinance: lower of 4% or CPI. Buildings with 5+ units; single-family exempt.'
    ),
    (
        'RRL01J00000000000000000412',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NJ-HOBOKEN'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2026,
        'ALL',
        5.00,
        'CPI_LINKED',
        'Lower of 5% or CPI',
        '2026-01-01',
        'ANNUAL',
        'RENT_STABILIZED',
        'RESIDENTIAL',
        'EXISTING_LEASE',
        'https://www.hobokennj.gov/departments/rent-leveling-and-stabilization-office',
        'Hoboken Rent Control Ordinance: lower of 5% or CPI.'
    );

-- ============================================================
-- Rules: DC standard 2022-2026 (RRL413-417), elderly/disabled (RRL418-422)
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((413 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'DC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'STANDARD',
    ys.cap,
    'CPI_LINKED',
    'DC CPI-W + 2% (max 10%)',
    make_date(ys.year, 5, 1),
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://rhc.dc.gov/page/rent-adjustments',
    'DC standard tenants: CPI-W + 2%, max 10%. Rent control year May 1 → Apr 30.'
FROM
    (
        VALUES
            (2022, 0, 6.20::NUMERIC),
            (2023, 1, 8.90),
            (2024, 2, 6.00),
            (2025, 3, 4.10),
            (2026, 4, 4.10)
    ) AS ys (YEAR, idx, cap);

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((418 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'DC'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'ELDERLY_DISABLED',
    ys.cap,
    'CPI_LINKED',
    'Lesser of CPI-W, SS COLA, or 5%',
    make_date(ys.year, 5, 1),
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://rhc.dc.gov/page/rent-adjustments',
    'DC elderly/disabled tenants: lesser of CPI-W, SS COLA, or 5%.'
FROM
    (
        VALUES
            (2022, 0, 4.20::NUMERIC),
            (2023, 1, 5.00),
            (2024, 2, 4.00),
            (2025, 3, 2.50),
            (2026, 4, 2.10)
    ) AS ys (YEAR, idx, cap);

-- ============================================================
-- Rules: MD-MONTGOMERY (RRL423-425), MD-PRINCEGEORGES standard (426-428) + senior (429-430), MD-TAKOMAPARK (431-432)
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((423 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'MD-MONTGOMERY'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'ALL',
    ys.cap,
    'CPI_LINKED',
    'CPI-U Wash-Arl-Alex + 3% (max 6%)',
    CASE
        WHEN ys.year = 2024 THEN DATE '2024-07-23'
        ELSE make_date(ys.year, 7, 1)
    END,
    90,
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.montgomerycountymd.gov/department-housing-community-affairs/rent-stabilization',
    'Montgomery County HOME Act effective 2024-07-23. Cap = lower of CPI-U + 3% or 6%. Buildings <23 years old exempt.'
FROM
    (
        VALUES
            (2024, 0, 6.00::NUMERIC),
            (2025, 1, 5.70),
            (2026, 2, 5.20)
    ) AS ys (YEAR, idx, cap);

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((426 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'MD-PRINCEGEORGES'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'STANDARD',
    ys.cap,
    'CPI_LINKED',
    'CPI-U + 3% (max 6%)',
    CASE
        WHEN ys.year = 2024 THEN DATE '2024-10-17'
        ELSE make_date(ys.year, 7, 1)
    END,
    90,
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.princegeorgescountymd.gov/departments-offices/housing-community-development/permanent-rent-stabilization-and-protection-act-2024',
    'Prince George''s County Permanent Rent Stabilization Act, effective 2024-10-17.'
FROM
    (
        VALUES
            (2024, 0, 6.00::NUMERIC),
            (2025, 1, 5.70),
            (2026, 2, 5.20)
    ) AS ys (YEAR, idx, cap);

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        notice_period_days,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((429 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'MD-PRINCEGEORGES'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'SENIOR',
    ys.cap,
    'CPI_LINKED',
    'Lower of CPI-U or 4.5%',
    make_date(ys.year, 7, 1),
    90,
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.princegeorgescountymd.gov/departments-offices/housing-community-development/permanent-rent-stabilization-and-protection-act-2024',
    'Prince George''s senior housing (62+): lower of CPI-U or 4.5%.'
FROM
    (
        VALUES
            (2025, 0, 2.70::NUMERIC),
            (2026, 1, 2.20)
    ) AS ys (YEAR, idx, cap);

INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        tenancy_phase,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((431 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'MD-TAKOMAPARK'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'US'
            )
    ),
    ys.year,
    'ALL',
    ys.cap,
    'CPI_LINKED',
    'Washington-Baltimore CPI-U (Mar-Mar)',
    make_date(ys.year, 7, 1),
    'ANNUAL',
    'RENT_STABILIZED',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://takomaparkmd.gov/1594/Rent-Stabilization-Rent-Increase-Allowan',
    'Takoma Park: 100% Wash-Balt CPI-U (Mar-Mar). 2026 value pending late-spring publication.'
FROM
    (
        VALUES
            (2025, 0, 2.40::NUMERIC),
            (2026, 1, NULL)
    ) AS ys (YEAR, idx, cap);

UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:40:00',
    updated_at = now()
WHERE
    country_code = 'US';

-- ============================================================
-- V060__rent_regulation_fr_encadrement_dpe
-- ============================================================
-- BUUR-93 §8 — French encadrement des loyers (city-level rent ceilings) + 2026 DPE reform.
-- Adds 9 zones with CEILING_RENT rules. Statutory authorisation expires 2026-11-23.
-- Also updates the FR DPE F/G rent freeze rule for the 2026-01-01 coefficient change (2.3 → 1.9).
-- ============================================================
-- New regions (RRG045-04D) - mark FR as having regional regulations
-- ============================================================
UPDATE rent_regulation_countries
SET
    has_regional_regulations = TRUE
WHERE
    country_code = 'FR';

INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000045',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-PARIS',
        'Paris (encadrement des loyers)',
        'Encadrement des loyers re-instated 2019-07-01 (Décret 2019-315). Annual arrêté préfectoral sets loyer de référence per arrondissement / type / époque. Cap = loyer de référence majoré (+20%). Authorisation expires 2026-11-23 unless renewed.'
    ),
    (
        'RRG01J00000000000000000046',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-LILLE-EPCI',
        'Lille MEL (encadrement)',
        'Encadrement des loyers since 2020-03-01 (Décret 2020-41). Covers Lille, Hellemmes, Lomme within MEL. Loyer de référence majoré (+20%).'
    ),
    (
        'RRG01J00000000000000000047',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-PLAINE-COMMUNE',
        'EPT Plaine Commune (encadrement)',
        '9 communes: Aubervilliers, La Courneuve, Épinay-sur-Seine, L''Île-Saint-Denis, Pierrefitte-sur-Seine, Saint-Denis, Saint-Ouen-sur-Seine, Stains, Villetaneuse. In force since 2021-06-01 (Décret 2020-1619).'
    ),
    (
        'RRG01J00000000000000000048',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-LYON',
        'Métropole de Lyon (encadrement)',
        'Lyon + Villeurbanne. In force since 2021-11-01 (Décret 2021-1143). Loyer de référence majoré (+20%).'
    ),
    (
        'RRG01J00000000000000000049',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-EST-ENSEMBLE',
        'EPT Est Ensemble (encadrement)',
        '9 communes: Bagnolet, Bobigny, Bondy, Le Pré-Saint-Gervais, Les Lilas, Montreuil, Noisy-le-Sec, Pantin, Romainville. In force since 2021-12-01 (Décret 2021-688).'
    ),
    (
        'RRG01J00000000000000000050',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-MONTPELLIER',
        'Montpellier (encadrement)',
        'In force since 2022-07-01 (Décret 2021-1144). Loyer de référence majoré (+20%).'
    ),
    (
        'RRG01J00000000000000000051',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-BORDEAUX',
        'Bordeaux (encadrement)',
        'In force since 2022-07-15 (Décret 2021-1145). Loyer de référence majoré (+20%).'
    ),
    (
        'RRG01J00000000000000000052',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-PAYS-BASQUE',
        'Communauté d''Agglomération Pays Basque (encadrement)',
        '24 communes incl. Bayonne, Biarritz, Anglet, Saint-Jean-de-Luz, Hendaye. In force since 2024-11-25 (Décret 2023-981 + arrêté préfectoral 21-10-2024).'
    ),
    (
        'RRG01J00000000000000000053',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        'FR-GRENOBLE-METRO',
        'Grenoble-Alpes Métropole (encadrement)',
        '21 communes (13 intégrale + 8 partielle). In force since 2025-01-20 (arrêté préfectoral de l''Isère 11-12-2024).'
    );

-- ============================================================
-- Rules: CEILING_RENT per zone × year from start until 2026-11-23 expiry
-- ============================================================
-- Paris (since 2019) - RRL600-604 (2022-2026)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((600 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-PARIS'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    CASE
        WHEN ys.year = 2026 THEN DATE '2026-07-01'
        ELSE make_date(ys.year, 7, 1)
    END,
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.prefectures-regions.gouv.fr/ile-de-france/',
    'Paris encadrement: loyer de référence majoré per arrondissement/type/époque. Annual arrêté préfectoral.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Lille MEL (since 2020) - RRL610-614
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((610 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-LILLE-EPCI'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 3, 1),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000041434865',
    'Lille MEL encadrement (Lille, Hellemmes, Lomme). Annual loyer de référence.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Plaine Commune (since 2021) - RRL620-624
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((620 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-PLAINE-COMMUNE'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 6, 1),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'http://www.referenceloyer.drihl.ile-de-france.developpement-durable.gouv.fr/plaine-commune/',
    'Plaine Commune encadrement: 9 communes.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Lyon (since 2021-11) - RRL630-634
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((630 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-LYON'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 11, 1),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000044015017',
    'Lyon + Villeurbanne encadrement.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Est Ensemble (since 2021-12) - RRL640-643 (only through Nov 2026 expiry)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((640 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-EST-ENSEMBLE'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 12, 1),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000043548908',
    'Est Ensemble encadrement: 9 communes.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3)
    ) AS ys (YEAR, idx);

-- Montpellier (since 2022-07) - RRL650-654
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((650 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-MONTPELLIER'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 7, 1),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000044010571',
    'Montpellier encadrement.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Bordeaux (since 2022-07-15) - RRL660-664
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((660 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-BORDEAUX'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    make_date(ys.year, 7, 15),
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000044010585',
    'Bordeaux encadrement.'
FROM
    (
        VALUES
            (2022, 0),
            (2023, 1),
            (2024, 2),
            (2025, 3),
            (2026, 4)
    ) AS ys (YEAR, idx);

-- Pays Basque (since 2024-11-25) - RRL670-671 (2024 partial, 2025 full)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((670 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-PAYS-BASQUE'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    CASE
        WHEN ys.year = 2024 THEN DATE '2024-11-25'
        ELSE make_date(ys.year, 11, 25)
    END,
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000048249642',
    'Pays Basque encadrement: 24 communes (Bayonne, Biarritz, Anglet, Saint-Jean-de-Luz, Hendaye, etc.).'
FROM
    (
        VALUES
            (2024, 0),
            (2025, 1)
    ) AS ys (YEAR, idx);

-- Grenoble (since 2025-01-20) - RRL680-681 (2025 partial, 2026)
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((680 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'FR-GRENOBLE-METRO'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'FR'
            )
    ),
    ys.year,
    'ALL',
    'CEILING_RENT',
    'Loyer de référence + 20%',
    CASE
        WHEN ys.year = 2025 THEN DATE '2025-01-20'
        ELSE make_date(ys.year, 1, 20)
    END,
    'ANNUAL',
    'CEILING_RENT',
    'RESIDENTIAL',
    'https://www.isere.gouv.fr/Actions-de-l-Etat/Amenagement-du-territoire-construction-logement-et-associations-de-proprietaires/Construction-logement/Logement/Encadrement-des-loyers/Application-de-l-encadrement-des-loyers-2026',
    'Grenoble-Alpes Métropole encadrement: 21 communes (13 intégrale + 8 partielle).'
FROM
    (
        VALUES
            (2025, 0),
            (2026, 1)
    ) AS ys (YEAR, idx);

-- ============================================================
-- FR DPE F/G rule — note 2026-01-01 coefficient change (2.3 → 1.9)
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Gel des loyers passoires énergétiques (loi Climat & Résilience art. 159, art. 17-1 loi 89-462). Effective 2022-08-24 for F/G dwellings; G also barred from new leases since 2025-01-01; F barred from new leases from 2028. Arrêté du 13 août 2025 (JORF 26-08-2025) modifies DPE coefficient électricité (2.3 → 1.9) from 2026-01-01: ~850,000 dwellings exit F/G classification.',
    source_url = 'https://www.ecologie.gouv.fr/presse/evolution-du-calcul-du-dpe-1er-janvier-2026-favoriser-lelectrification-du-chauffage',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FR'
    )
    AND property_category = 'DPE_F_OR_G';

UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:45:00',
    updated_at = now()
WHERE
    country_code = 'FR';

-- ============================================================
-- V061__rent_regulation_ch_cantons
-- ============================================================
-- BUUR-93 §9 — Swiss cantonal rent regulation specifics on top of federal Mietrecht.
-- GE and BS have meaningful cantonal restrictions (LDTR / WRFG + formule officielle).
-- VD has mandatory cantonal form. ZH/BE/BL: federal only.
UPDATE rent_regulation_countries
SET
    has_regional_regulations = TRUE
WHERE
    country_code = 'CH';

INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000054',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'GE',
        'Genève',
        'LDTR (Loi sur les démolitions, transformations et rénovations, RSG L 5 20): rent-control on renovated/new-build dwellings 3-5 years, simplified procedure with absolute pass-through caps. Formule officielle obligatoire on every new residential lease (CO art. 270 al. 2).'
    ),
    (
        'RRG01J00000000000000000055',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'VD',
        'Vaud',
        'Notification de loyer initial via cantonal official form mandatory on every new lease in all districts except Aigle (2026 exemption confirmed). 30-day contest right. No additional cantonal rent-cap beyond federal Mietrecht.'
    ),
    (
        'RRG01J00000000000000000056',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'ZH',
        'Zürich',
        'No cantonal rent control beyond federal hypothekarischer Referenzzinssatz. Housing-protection initiative pending (would allow municipalities to require permits + temporary rent regulation); not in force as of 2026-05.'
    ),
    (
        'RRG01J00000000000000000057',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'BE',
        'Bern',
        'No cantonal rent control beyond federal Mietrecht. City of Bern Art. 16b (municipal "affordable housing", in force since 2020-01-01) applies only to the municipality.'
    ),
    (
        'RRG01J00000000000000000058',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'BS',
        'Basel-Stadt',
        'Wohnraumfördergesetz (WRFG, 2013) + Verordnung über den Schutz von Wohnraum (WRSchV, 2022): permit required for demolition/conversion/renovation; simplified procedure caps pass-through at 50% of value-enhancing investment with absolute caps CHF 80/120/160 per month (2/3/4-room). Formule officielle obligatoire on new leases.'
    ),
    (
        'RRG01J00000000000000000059',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        'BL',
        'Basel-Landschaft',
        'No cantonal rent control beyond federal hypothekarischer Referenzzinssatz.'
    );

-- Cantonal rules: one 2026 row per canton
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000700',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'GE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'CANTONAL_RESTRICTION',
        'Hypothekarischer Referenzzinssatz + LDTR',
        '2026-01-01',
        'QUARTERLY',
        'LDTR',
        'RESIDENTIAL',
        'LDTR (RSG L 5 20): 3-5 year rent control on renovated/demolished/new-build dwellings. Simplified procedure: 50% of value-enhancing investment, abs. caps CHF 80/120/160 (2/3/4-room). Permit required. Formule officielle obligatoire (CO art. 270 al. 2). Since 2025-10-01 form must show reference mortgage rate, CPI used for previous rent, prior tenant rent + justifications. Non-compliance → rent null.',
        'https://www.ge.ch/legislation/rsg/f/rsg_l5_20.html',
        'Geneva LDTR + cantonal formule officielle obligation. Stricter than federal baseline.'
    ),
    (
        'RRL01J00000000000000000701',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'VD'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'CANTONAL_RESTRICTION',
        'Hypothekarischer Referenzzinssatz',
        '2026-01-01',
        'QUARTERLY',
        'CANTONAL_FORM',
        'RESIDENTIAL',
        'Notification de loyer initial via cantonal official form mandatory in all districts EXCEPT Aigle (2026 exemption confirmed). Must disclose previous tenant rent + right to contest within 30 days. No additional cantonal rent-cap.',
        'https://www.vd.ch/territoire-et-construction/logement/droit-du-bail',
        'Vaud cantonal notification form obligation.'
    ),
    (
        'RRL01J00000000000000000702',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'ZH'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        '2026-01-01',
        'QUARTERLY',
        'FEDERAL_ONLY',
        'RESIDENTIAL',
        NULL,
        'https://www.zh.ch/',
        'ZH: federal rules only. Housing-protection initiative pending.'
    ),
    (
        'RRL01J00000000000000000703',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        '2026-01-01',
        'QUARTERLY',
        'FEDERAL_ONLY',
        'RESIDENTIAL',
        'City of Bern Art. 16b (municipal, since 2020-01-01) is municipal-level only.',
        'https://www.be.ch/',
        'Bern canton: federal rules only.'
    ),
    (
        'RRL01J00000000000000000704',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'BS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'CANTONAL_RESTRICTION',
        'Hypothekarischer Referenzzinssatz + WRFG',
        '2026-01-01',
        'QUARTERLY',
        'WRFG',
        'RESIDENTIAL',
        'WRFG (2013) + WRSchV (2022): permit required for demolition/conversion/renovation; simplified procedure caps pass-through at 50% of value-enhancing investment with absolute caps CHF 80/120/160 (2/3/4-room). Formule officielle obligatoire on new leases.',
        'https://www.gesetzessammlung.bs.ch/app/de/texts_of_law/861.500',
        'Basel-Stadt WRFG + WRSchV cantonal restrictions.'
    ),
    (
        'RRL01J00000000000000000705',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'BL'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CH'
                )
        ),
        2026,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        '2026-01-01',
        'QUARTERLY',
        'FEDERAL_ONLY',
        'RESIDENTIAL',
        NULL,
        'https://www.baselland.ch/',
        'Basel-Landschaft: federal rules only.'
    );

UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:50:00',
    updated_at = now()
WHERE
    country_code = 'CH';

-- ============================================================
-- V062__rent_regulation_es_catalonia_historical
-- ============================================================
-- BUUR-93 §10 — ES Catalonia regional + pre/post Ley 12/2023 historical contract-date split.
UPDATE rent_regulation_countries
SET
    has_regional_regulations = TRUE
WHERE
    country_code = 'ES';

-- ============================================================
-- New region: Catalonia (CAT)
-- ============================================================
INSERT INTO
    rent_regulation_regions (
        identifier,
        country_id,
        region_code,
        region_name,
        summary
    )
VALUES
    (
        'RRG01J00000000000000000060',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        'CAT',
        'Catalunya',
        'Resolution TER/2408/2024 (BOE 2024-10-09): 271 declared zonas tensionadas municipalities (140 from March 2024 + 131 added July 2024). Covers 7,172,196 residents (90.7% of Catalan population). 3-year validity. IRPL (Índex de Referència de Preus del Lloguer) applies to large landlords + dwellings unrented in previous 5 years. Llei 11/2025 closes seasonal/room loophole effective 2026-01-01. Gran tenedor Catalan threshold: 5+ dwellings in zona tensionada (vs. state 10+).'
    );

-- ============================================================
-- Catalonia rules: 2024 initial declaration, 2025 expanded, 2026 with Llei 11/2025
-- RRL800-806
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        region_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        landlord_min_properties,
        additional_conditions,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000800',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'CAT'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'ES'
                )
        ),
        2024,
        'ALL',
        NULL,
        'INDEX_LINKED',
        'IRPL (Índex de Referència de Preus del Lloguer)',
        '2024-03-14',
        'MONTHLY',
        'ZONA_TENSIONADA',
        'RESIDENTIAL',
        5,
        'Initial 140 municipalities declared March 2024. Applies to gran tenedor (5+ in zona) + dwellings unrented prior 5 years.',
        'https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-5214',
        'Catalonia zonas tensionadas — initial declaration (140 munis).'
    ),
    (
        'RRL01J00000000000000000801',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'CAT'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'ES'
                )
        ),
        2025,
        'ALL',
        NULL,
        'INDEX_LINKED',
        'IRPL',
        '2025-01-01',
        'MONTHLY',
        'ZONA_TENSIONADA',
        'RESIDENTIAL',
        5,
        'Resolution TER/2408/2024 (Jul 2024) added 131 munis → 271 total. 3-year validity from BOE 2024-10-09. Decret-Llei 1/2025 + 2/2025 confirm gran tenedor Catalan threshold = 5+ dwellings in zona tensionada.',
        'https://www.boe.es/diario_boe/txt.php?id=BOE-A-2024-20576',
        'Catalonia 271 munis. Gran tenedor 5+ threshold in force from 2025.'
    ),
    (
        'RRL01J00000000000000000802',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'CAT'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'ES'
                )
        ),
        2026,
        'ALL',
        2.32,
        'INDEX_LINKED',
        'IRPL (annual update Feb 2026)',
        '2026-01-01',
        'MONTHLY',
        'ZONA_TENSIONADA',
        'RESIDENTIAL',
        5,
        'IRPL annual update Feb 2025 → Feb 2026: +2.32%. State INE reference (Nov 2025): +2.9%. Cap applies in 271 declared munis to gran tenedor (5+) and dwellings unrented prior 5 years. Pre-2023-05-26 contracts exempt.',
        'https://agenciahabitatge.gencat.cat/indexdelloguer/',
        'Catalonia 2026 IRPL +2.32%.'
    ),
    (
        'RRL01J00000000000000000803',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'CAT'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'ES'
                )
        ),
        2026,
        'SEASONAL',
        NULL,
        'INDEX_LINKED',
        'IRPL',
        '2026-01-01',
        'MONTHLY',
        'ZONA_TENSIONADA',
        'SEASONAL',
        NULL,
        'Llei 11/2025 (Parlament 2025-12-18, DOGC 2025-12-31, in force 2026-01-01): extends IRPL caps to seasonal (1-12 months) and room rentals. CGE ruled constitutional. PP filed Tribunal Constitucional challenge (pending).',
        'https://www.catalannews.com/politics/item/parliament-passes-landmark-law-to-close-rent-cap-loophole',
        'Catalonia seasonal/room rentals brought under IRPL cap by Llei 11/2025.'
    );

-- ============================================================
-- ES historical pre/post Ley 12/2023 contract-date split (RRL900-905)
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        index_name,
        effective_date,
        frequency,
        regime,
        property_type,
        contract_signed_before,
        contract_signed_after,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000900',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2023,
        'ALL',
        2.00,
        'INDEX_LINKED',
        'min(IGC, 2%)',
        '2023-01-01',
        'ANNUAL',
        'EMERGENCY_CAP',
        'RESIDENTIAL',
        '2023-05-26',
        NULL,
        'https://www.boe.es/buscar/doc.php?id=BOE-A-2022-22685',
        'Pre-Ley 12/2023 contracts in 2023: RDL 20/2022 extended emergency 2% cap through 2023-12-31, all LAU vivienda contracts regardless of landlord size.'
    ),
    (
        'RRL01J00000000000000000901',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2023,
        'ALL',
        2.00,
        'INDEX_LINKED',
        'min(IGC, 2%) (pre-IRAV)',
        '2023-05-26',
        'ANNUAL',
        'EMERGENCY_CAP',
        'RESIDENTIAL',
        NULL,
        '2023-05-26',
        'https://www.boe.es/buscar/act.php?id=BOE-A-2023-12203',
        'Post-Ley 12/2023 contracts in 2023: same 2% emergency cap (transitional, IRAV not yet published).'
    ),
    (
        'RRL01J00000000000000000902',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2024,
        'ALL',
        3.00,
        'INDEX_LINKED',
        'min(IGC, 3%)',
        '2024-01-01',
        'ANNUAL',
        'EMERGENCY_CAP',
        'RESIDENTIAL',
        '2023-05-26',
        NULL,
        'https://www.boe.es/buscar/act.php?id=BOE-A-2023-12203',
        'Pre-Ley contracts 2024: Ley 12/2023 DF 6ª set cap at 3% for 2024.'
    ),
    (
        'RRL01J00000000000000000903',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2024,
        'ALL',
        3.00,
        'INDEX_LINKED',
        'min(IGC, 3%) (pre-IRAV)',
        '2024-01-01',
        'ANNUAL',
        'EMERGENCY_CAP',
        'RESIDENTIAL',
        NULL,
        '2023-05-26',
        'https://www.boe.es/buscar/act.php?id=BOE-A-2023-12203',
        'Post-Ley contracts 2024: same 3% statutory cap; IRAV index still pending INE publication.'
    ),
    (
        'RRL01J00000000000000000904',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2025,
        'ALL',
        NULL,
        'INDEX_LINKED',
        'IPC general (contractual)',
        '2025-01-01',
        'ANNUAL',
        'CONTRACTUAL_INDEX',
        'RESIDENTIAL',
        '2023-05-26',
        NULL,
        'https://www.boe.es/buscar/act.php?id=BOE-A-1994-26003',
        'Pre-Ley contracts 2025: emergency caps lapsed 2024-12-31. Revert to contractual index per art. 18 LAU (default IPC general). IRAV only by pacto expreso.'
    ),
    (
        'RRL01J00000000000000000905',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2025,
        'ALL',
        2.32,
        'INDEX_LINKED',
        'IRAV (INE monthly)',
        '2025-01-01',
        'MONTHLY',
        'IRAV_CAP',
        'RESIDENTIAL',
        NULL,
        '2023-05-26',
        'https://www.ine.es/uc/oC7D0Ncd',
        'Post-Ley contracts 2025: IRAV mandatory ceiling. Monthly range 1.98%-2.32%; Dec 2025 = 2.32%.'
    );

UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-05-14 12:55:00',
    updated_at = now()
WHERE
    country_code = 'ES';

-- ============================================================
-- V063__rent_regulation_source_url_backfill
-- ============================================================
-- BUUR-93 §11 — backfill source_url on historical 2022-2025 rows where V029 left it NULL.
-- Only updates rows still missing source_url to avoid clobbering URLs added by V053/V054.
-- Denmark — DST nettoprisindeks for all historical rows missing URL
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DK'
    )
    AND source_url IS NULL;

-- Sweden — Hyresgästföreningen
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.hyresgastforeningen.se/om-oss/vad-vi-gor/hyresforhandling/',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'SE'
    )
    AND source_url IS NULL;

-- Norway — SSB konsumprisindeksen
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.ssb.no/priser-og-prisindekser/konsumpriser/statistikk/konsumprisindeksen',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'NO'
    )
    AND source_url IS NULL;

-- Finland — Tilastokeskus
UPDATE rent_regulation_rules
SET
    source_url = 'https://stat.fi/vuokran-tarkistaminen-elinkustannusindeksilla',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FI'
    )
    AND source_url IS NULL;

-- Italy — Confedilizia ISTAT FOI tables
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'IT'
    )
    AND source_url IS NULL;

-- Austria — Statistik Austria Richtwerte
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.statistik.at/statistiken/bevoelkerung-und-soziales/wohnen/richtwerte-und-kategoriebetraege',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'AT'
    )
    AND source_url IS NULL;

-- Switzerland — BWO Referenzzinssatz
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.bwo.admin.ch/de/referenzzinssatz',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CH'
    )
    AND source_url IS NULL;

-- Poland — Ustawa o ochronie praw lokatorów
UPDATE rent_regulation_rules
SET
    source_url = 'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PL'
    )
    AND source_url IS NULL;

-- Czech Republic — Občanský zákoník
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.zakonyprolidi.cz/cs/2012-89',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CZ'
    )
    AND source_url IS NULL;

-- Luxembourg — Logement.lu FAQ
UPDATE rent_regulation_rules
SET
    source_url = 'https://logement.public.lu/fr/proprietaire/logement-location/faq-bail-a-loyer.html',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'LU'
    )
    AND source_url IS NULL;

-- ============================================================
-- V064__rent_regulation_check_constraints
-- ============================================================
-- BUUR-93 §12 — CHECK constraints on dimensional columns added by V055.
-- Ensures invariants hold (build year ranges, EPC class ranges, contract date ordering).
ALTER TABLE rent_regulation_rules
ADD CONSTRAINT chk_rrr_build_year_range CHECK (
    build_year_min IS NULL
    OR build_year_max IS NULL
    OR build_year_min <= build_year_max
),
ADD CONSTRAINT chk_rrr_epc_class_range CHECK (
    epc_class_min IS NULL
    OR epc_class_min IN ('A', 'B', 'C', 'D', 'E', 'F', 'G')
),
ADD CONSTRAINT chk_rrr_epc_class_max CHECK (
    epc_class_max IS NULL
    OR epc_class_max IN ('A', 'B', 'C', 'D', 'E', 'F', 'G')
),
ADD CONSTRAINT chk_rrr_epc_class_order CHECK (
    epc_class_min IS NULL
    OR epc_class_max IS NULL
    OR epc_class_min <= epc_class_max
),
ADD CONSTRAINT chk_rrr_contract_date_order CHECK (
    contract_signed_after IS NULL
    OR contract_signed_before IS NULL
    OR contract_signed_after <= contract_signed_before
),
ADD CONSTRAINT chk_rrr_landlord_min_properties CHECK (
    landlord_min_properties IS NULL
    OR landlord_min_properties >= 1
),
ADD CONSTRAINT chk_rrr_build_year_min_sane CHECK (
    build_year_min IS NULL
    OR build_year_min BETWEEN 1800 AND 2100
),
ADD CONSTRAINT chk_rrr_build_year_max_sane CHECK (
    build_year_max IS NULL
    OR build_year_max BETWEEN 1800 AND 2100
);

-- ============================================================
-- V065__rent_regulation_drop_sector
-- ============================================================
-- BUUR-93 §2 — drop the unused `sector` column.
-- `sector` was added in V029 alongside `property_category` but never populated in any
-- migration or codepath. The dimensional columns added in V055 make it permanently unused.
--
-- `property_category` is kept as-is for backwards compatibility this release; it will be
-- dropped in a future migration once all consumers (frontend, JOOQ codegen, API) are off it.
ALTER TABLE rent_regulation_rules
DROP COLUMN sector;

-- Add a comment marking property_category as deprecated (PostgreSQL column comment).
COMMENT ON COLUMN rent_regulation_rules.property_category IS 'DEPRECATED (BUUR-93): use the typed dimensional columns instead (regime, property_type, contract_type, tax_regime, tenancy_phase, build_year_min/max, epc_class_min/max, contract_signed_after/before, landlord_min_properties, area_code). Retained for backwards compat; to be dropped after consumer migration.';
