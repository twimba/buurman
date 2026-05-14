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
