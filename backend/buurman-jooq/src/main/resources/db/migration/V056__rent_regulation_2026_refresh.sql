-- Rent regulation freshness review (review date: 2026-06-20).
-- Builds on V029 (seed) and V053 (May-2026 overhaul). Applies only changes that are
-- backed by an official/authoritative source as of 2026-06-20:
--   * Confirmed 2026 corrections (DK NPI, US-CA AB1482 ceiling, NL note, FR DPE, IT canone libero)
--   * Two legislated 2027 values (AT 5. MILG 2% cap; NS cap legislated through 2027-12-31)
-- Items that are not yet published / still draft as of 2026-06-20 are intentionally NOT
-- written here (DE Mietrecht II 3.5% index cap, LU bail reform, BE >3.2% revision, US-NY RGB
-- Order #58, OR 6/9.5% mapping, NB cap status) — see the review report / backlog.
-- ============================================================
-- ============================================================
-- Denmark (DK) — 2026 PRE_1992 NPI value corrected
-- Nettoprisindeks Oct-2024 119.9 → Oct-2025 122.6 = +2.25% (DST Nyt nr. 312, 2025-11-10).
-- This Oct-to-Oct change drives NPI-indexed regulated rents from 2026-01-01.
-- Prior stored value (2.10%) was a stale estimate.
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.25,
    index_value = 122.6,
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
    notes = 'NPI Oct-2024 119.9 → Oct-2025 122.6 = +2.25% (DST Nyt nr. 312, 2025-11-10). NPI rebased to 2025=100 from 2026-01.',
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
    AND YEAR = 2026
    AND property_category = 'PRE_1992';

-- ============================================================
-- Netherlands (NL) — 2026 REGULATED note correction (value 4.10% unchanged and correct)
-- Basis is average inflation (3.6%) + 0.5%, NOT wage-growth as previously noted.
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Confirmed by Ministry of Housing 2025-12-15. Basis = avg inflation 3.6% + 0.5% (not wage growth). Applies to 2026-07-01.',
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

-- ============================================================
-- United States — California (US-CA) — 2026 AB 1482 data-quality fix
-- Prior 7.50% was an unsourced projection. AB 1482 is REGIONAL: 5% + local CPI, hard cap 10%.
-- There is no single statewide percentage; store the statutory ceiling (10%) which is always
-- correct, and document the regional formula. (Current AB 1482 year statewide baseline ≈ 6.3%;
-- 2026-08-01 period figures are region-specific and only just emerging.) Sunsets 2030-01-01.
-- ============================================================
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 10.00,
    max_increase_type = 'CPI_LINKED',
    index_name = '5% + regional CPI (cap 10%)',
    source_url = 'https://www.hcd.ca.gov/AB-1482',
    notes = 'AB 1482 (Tenant Protection Act): effective cap = 5% + regional CPI, hard ceiling 10%. Regional — no single statewide figure; 10% stored as the statutory ceiling. Sunsets 2030-01-01.',
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
            region_code = 'CA'
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
-- France (FR) — 2026 DPE F/G freeze: note the 2026-01-01 DPE recalculation
-- Electricity conversion coefficient lowered 2.3 → 1.9 from 2026-01-01, moving ~850,000
-- electrically-heated dwellings out of "passoire" (F/G) status (they may exit the freeze
-- with a new valid DPE). Freeze itself (0% IRL revision for F/G) is unchanged.
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Since 2022-08-24, DPE F/G dwellings barred from IRL revision (gel des loyers); G also barred from new leases since 2025-01-01. DPE electricity coefficient lowered 2.3→1.9 from 2026-01-01 — ~850k electrically-heated dwellings may exit F/G and resume indexation with a new DPE.',
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
    AND YEAR = 2026
    AND property_category = 'DPE_F_OR_G';

-- ============================================================
-- Italy (IT) — 2026 canone libero (FREE_MARKET) note correction
-- "1.50%" reflected only Jan/Mar-2026 FOI; canone libero may index up to 100% of ISTAT FOI,
-- which is monthly and rose to ~3.0% by May 2026. The stored percentage is a point-in-time
-- snapshot; clarify that the applicable figure tracks the monthly FOI at each anniversary.
-- ============================================================
UPDATE rent_regulation_rules
SET
    notes = 'Canone libero (4+4): up to 100% of ISTAT FOI when contractually stipulated. FOI is monthly — e.g. Mar-2026 +1.5%, May-2026 +3.0%. Stored % is a snapshot; apply the FOI of the month preceding each anniversary.',
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
    AND YEAR = 2026
    AND property_category = 'FREE_MARKET';

-- ============================================================
-- 2027 — only values already fixed by law as of 2026-06-20
-- ============================================================
-- Austria (AT) 2027 — 2% statutory cap already legislated in 5. Mietrechtliches
-- Inflationslinderungsgesetz (effective 2027-04-01). From 2028, inflation above 3% counts
-- only at half for the excess (permanent damping).
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
        'RRL01J00000000000000000906',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2027,
        'REGULATED',
        2.00,
        'FIXED_PERCENTAGE',
        'Mietpreisbremse (statutory cap)',
        '2027-04-01',
        'ANNUAL',
        'https://www.ris.bka.gv.at/Dokumente/BgblAuth/BGBLA_2025_I_12/BGBLA_2025_I_12.pdf',
        '5. MILG / MieWeG: statutory 2% cap for Richtwert/Kategoriemiete in 2027 (already enacted). From 2028: inflation above 3% counts at half for the excess.'
    );

-- Canada — Nova Scotia (CA-NS) 2027 — 5% cap legislated through 2027-12-31.
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
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000907',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        (
            SELECT
                id
            FROM
                rent_regulation_regions
            WHERE
                region_code = 'NS'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2027,
        'ALL',
        5.00,
        'FIXED_PERCENTAGE',
        '2027-01-01',
        'ANNUAL',
        'https://novascotia.ca/news/release/?id=20231006001',
        'Nova Scotia rent cap of 5%/yr legislated through 2027-12-31.'
    );

-- ============================================================
-- Mark all 20 countries reviewed on 2026-06-20
-- ============================================================
UPDATE rent_regulation_countries
SET
    last_reviewed_at = '2026-06-20 00:00:00',
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
