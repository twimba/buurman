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
