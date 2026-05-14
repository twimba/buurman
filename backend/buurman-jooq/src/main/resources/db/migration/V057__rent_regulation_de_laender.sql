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
