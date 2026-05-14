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
