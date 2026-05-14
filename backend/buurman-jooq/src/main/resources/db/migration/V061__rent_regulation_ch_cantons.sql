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
