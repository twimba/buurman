-- Rent regulation data-quality pass (review date: 2026-06-20).
-- Addresses pre-existing gaps found during the 2026-06-20 freshness review:
--   1. Backfill missing source_url on current-year rows (DE national/Berlin, BE 2026, ES 2026, US-NY).
--   2. Add rules for region rows that had ZERO rules (DE-BY, DE-HH, US-MA, US-NJ state-level).
--   3. Add the missing 2026 rows for FR encadrement zones Est-Ensemble and Pays-Basque.
-- All URLs verified against official sources on 2026-06-20.
-- ============================================================
-- ============================================================
-- 1. source_url backfills
-- ============================================================
-- Germany national Kappungsgrenze (§558 BGB) — ALL/Kappungsgrenze rows lacked a source.
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.gesetze-im-internet.de/bgb/__558.html',
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
    AND property_category = 'ALL'
    AND source_url IS NULL;

-- Berlin tight-market Kappungsgrenze rows.
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.berlin.de/rbmskzl/aktuelles/pressemitteilungen/2025/pressemitteilung.1615170.php',
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
    AND region_id = (
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
                    country_code = 'DE'
            )
    )
    AND source_url IS NULL;

-- Belgium 2026 Flanders/Brussels rows (Wallonia already sourced in V053).
UPDATE rent_regulation_rules
SET
    source_url = 'https://statbel.fgov.be/nl/themas/consumptieprijsindex/gezondheidsindex',
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
    AND source_url IS NULL
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

-- Spain 2026 national IRAV row.
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.ine.es/jaxiT3/Tabla.htm?t=72975',
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
    AND property_category = 'ALL'
    AND source_url IS NULL;

-- US-NY 2026 rent-stabilized row (value remains a projection pending RGB final vote).
UPDATE rent_regulation_rules
SET
    source_url = 'https://rentguidelinesboard.cityofnewyork.us/',
    notes = 'PROJECTION pending RGB Order #58 final vote (late June 2026). Proposed range: 1-yr 0–2%, 2-yr 0–4% for leases commencing 2026-10-01.',
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
-- 2a. Germany — Bavaria (BY) tight-market Kappungsgrenze, 2022-2026 (RRL908-912)
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
    'RRL01J00000000000000000' || lpad((908 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'BY'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'DE'
            )
    ),
    ys.year,
    'ALL',
    15.00,
    'STATUTORY_CAP',
    'Kappungsgrenze (tight market)',
    make_date(ys.year, 1, 1),
    'TRIENNIAL',
    'KAPPUNGSGRENZE',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.verkuendung-bayern.de/baymbl/2025-558/',
    'Bayerische Mieterschutzverordnung: 15%/3y Kappungsgrenze + Mietpreisbremse in designated tight-market municipalities (272 per ordinance / 285 per press, up from 208). Current MiSchuV in force 2026-01-01 to 2029-12-31.'
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
-- 2b. Germany — Hamburg (HH) tight-market Kappungsgrenze, 2022-2026 (RRL913-917)
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
    'RRL01J00000000000000000' || lpad((913 + ys.idx)::TEXT, 3, '0'),
    (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'DE'
    ),
    (
        SELECT
            id
        FROM
            rent_regulation_regions
        WHERE
            region_code = 'HH'
            AND country_id = (
                SELECT
                    id
                FROM
                    rent_regulation_countries
                WHERE
                    country_code = 'DE'
            )
    ),
    ys.year,
    'ALL',
    15.00,
    'STATUTORY_CAP',
    'Kappungsgrenze (tight market)',
    make_date(ys.year, 1, 1),
    'TRIENNIAL',
    'KAPPUNGSGRENZE',
    'RESIDENTIAL',
    'EXISTING_LEASE',
    'https://www.hamburg.de/politik-und-verwaltung/behoerden/behoerde-fuer-stadtentwicklung-und-wohnen/aktuelles/pressemeldungen/senat-erlaesst-mietpreisbegrenzungsverordnung-bis-ende-2029-1120602',
    'Entire city designated tight market: 15%/3y Kappungsgrenzenverordnung (through 2028-08-31) + Mietpreisbegrenzungsverordnung/Mietpreisbremse (2026-01-01 to 2029-12-31).'
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
-- 2c. USA — Massachusetts (MA): no statewide rent control, 2022-2026 (RRL918-922)
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
        frequency,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((918 + ys.idx)::TEXT, 3, '0'),
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
            region_code = 'MA'
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
    'MARKET_RENT',
    make_date(ys.year, 1, 1),
    'ANNUAL',
    'RESIDENTIAL',
    'https://malegislature.gov/Laws/GeneralLaws/PartI/TitleVII/Chapter40P',
    'No statewide rent control. G.L. c.40P (1994 ballot Q9) prohibits municipal rent control absent home-rule authorization. As of 2026-06 no statewide change enacted; local-option efforts pending.'
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
-- 2d. USA — New Jersey (NJ) state-level: market; rent control is municipal, 2022-2026 (RRL923-927)
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
        frequency,
        property_type,
        source_url,
        notes
    )
SELECT
    'RRL01J00000000000000000' || lpad((923 + ys.idx)::TEXT, 3, '0'),
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
            region_code = 'NJ'
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
    'MARKET_RENT',
    make_date(ys.year, 1, 1),
    'ANNUAL',
    'RESIDENTIAL',
    'https://www.nj.gov/dca/',
    'No statewide rent cap. Rent control is municipal (home rule) — ~100+ municipalities maintain local ordinances (see DCA annual list). State-level row = market rent.'
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
-- 3. France — missing 2026 rows for encadrement zones Est-Ensemble & Pays-Basque
-- ============================================================
-- Est-Ensemble 2026 (RRL928) — experiment runs through its 2026-11 expiry; renewal pending.
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
VALUES
    (
        'RRL01J00000000000000000928',
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
        2026,
        'ALL',
        'CEILING_RENT',
        'Loyer de référence + 20%',
        '2026-01-01',
        'ANNUAL',
        'CEILING_RENT',
        'RESIDENTIAL',
        'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000043548908',
        'Est Ensemble encadrement: 9 communes. In force through the 5-year experiment expiry (2026-11); renewal pending.'
    );

-- Pays-Basque 2026 (RRL929) — 5-year experiment from 2024-11-25, in force through 2026 and beyond.
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
VALUES
    (
        'RRL01J00000000000000000929',
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
        2026,
        'ALL',
        'CEILING_RENT',
        'Loyer de référence + 20%',
        '2026-11-25',
        'ANNUAL',
        'CEILING_RENT',
        'RESIDENTIAL',
        'https://www.legifrance.gouv.fr/jorf/id/JORFTEXT000048249642',
        'Pays Basque encadrement: 24 communes (Bayonne, Biarritz, Anglet, Saint-Jean-de-Luz, Hendaye, etc.).'
    );
