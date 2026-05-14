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
