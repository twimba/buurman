-- Rent regulation reference tables, seed data, country request table, and region support
-- Combines: table creation, 20-country seed data (2022-2026), country requests, and properties.region_code
-- ============================================================
-- DDL: Rent regulation reference tables (global, no team_id)
-- ============================================================
CREATE TABLE rent_regulation_countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL UNIQUE,
    country_name VARCHAR(100) NOT NULL,
    has_regional_regulations BOOLEAN DEFAULT FALSE,
    summary TEXT,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE rent_regulation_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_code VARCHAR(20) NOT NULL,
    region_name VARCHAR(100) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, region_code),
    UNIQUE (country_id, identifier)
);

CREATE TABLE rent_regulation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    country_id UUID NOT NULL REFERENCES rent_regulation_countries (id),
    region_id UUID REFERENCES rent_regulation_regions (id),
    YEAR INT NOT NULL,
    property_category VARCHAR(50) DEFAULT 'ALL',
    sector VARCHAR(50),
    max_increase_percentage DECIMAL(5, 2),
    max_increase_type VARCHAR(30) NOT NULL,
    index_name VARCHAR(100),
    index_value DECIMAL(10, 4),
    effective_date DATE,
    notice_period_days INT,
    frequency VARCHAR(20) DEFAULT 'ANNUAL',
    additional_conditions TEXT,
    source_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    UNIQUE (country_id, identifier)
);

CREATE INDEX idx_rent_reg_rules_country_year ON rent_regulation_rules (country_id, YEAR);

CREATE INDEX idx_rent_reg_rules_region ON rent_regulation_rules (region_id);

-- ============================================================
-- DDL: Country regulation requests
-- ============================================================
CREATE TABLE rent_regulation_country_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    country_name VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_country_requests_team_id ON rent_regulation_country_requests (team_id);

CREATE UNIQUE INDEX idx_country_requests_unique ON rent_regulation_country_requests (team_id, lower(country_name), created_by);

-- ============================================================
-- DDL: Add region_code to properties
-- ============================================================
ALTER TABLE properties
ADD COLUMN region_code VARCHAR(20);

-- Seed rent regulation reference data for EU + UK + US + CA
-- Data covers 2022-2026 with real regulatory values
-- ============================================================
-- COUNTRIES
-- ============================================================
INSERT INTO
    rent_regulation_countries (
        identifier,
        country_code,
        country_name,
        has_regional_regulations,
        summary
    )
VALUES
    (
        'RRC01J00000000000000000001',
        'NL',
        'Netherlands',
        FALSE,
        'Regulated sector: max increase set by government (CBS huurverhoging). Free sector: CPI + surcharge, capped since 2024.'
    ),
    (
        'RRC01J00000000000000000002',
        'DE',
        'Germany',
        TRUE,
        'Mietspiegel-based rent control. Mietpreisbremse (rent brake) limits initial rents in tight markets. Regional variation.'
    ),
    (
        'RRC01J00000000000000000003',
        'PT',
        'Portugal',
        FALSE,
        'INE coefficient applied annually to all regulated leases. Effective January 1st each year.'
    ),
    (
        'RRC01J00000000000000000004',
        'FR',
        'France',
        FALSE,
        'IRL (Indice de Reference des Loyers) published quarterly by INSEE. Applied nationally.'
    ),
    (
        'RRC01J00000000000000000005',
        'ES',
        'Spain',
        FALSE,
        'CPI-linked until 2022, then temporary 2% cap (2023), 3% cap (2024), new reference index from 2025.'
    ),
    (
        'RRC01J00000000000000000006',
        'BE',
        'Belgium',
        TRUE,
        'Health index (Statbel) used for rent indexation. Varies by region (Flanders, Wallonia, Brussels).'
    ),
    (
        'RRC01J00000000000000000007',
        'IE',
        'Ireland',
        FALSE,
        'Rent Pressure Zones (RPZ) cap annual increases. 2% cap since 2021.'
    ),
    (
        'RRC01J00000000000000000008',
        'IT',
        'Italy',
        FALSE,
        'ISTAT FOI index (75% of index for regulated contracts). 100% for free-market contracts.'
    ),
    (
        'RRC01J00000000000000000009',
        'AT',
        'Austria',
        FALSE,
        'Richtwertmietzins (reference value rent) system. Adjusted by CPI. Category-based max rents.'
    ),
    (
        'RRC01J00000000000000000010',
        'GB',
        'United Kingdom',
        TRUE,
        'England/Wales: market rent with Section 13 procedure. Scotland: rent control areas possible. N. Ireland: market rent.'
    ),
    (
        'RRC01J00000000000000000011',
        'US',
        'United States',
        TRUE,
        'No federal rent control. State/city level regulations. Key states: CA, NY, OR have statewide controls.'
    ),
    (
        'RRC01J00000000000000000012',
        'CA',
        'Canada',
        TRUE,
        'Province-by-province rent control. ON, BC, QC have guideline increases. AB has no rent control.'
    ),
    (
        'RRC01J00000000000000000013',
        'DK',
        'Denmark',
        FALSE,
        'Cost-based rent regulation for pre-1992 buildings. Newer buildings: market rent with "das valgte lejedes" limits.'
    ),
    (
        'RRC01J00000000000000000014',
        'SE',
        'Sweden',
        FALSE,
        'Collective bargaining (bruksvardesystemet). Annual negotiations between tenant/landlord unions.'
    ),
    (
        'RRC01J00000000000000000015',
        'NO',
        'Norway',
        FALSE,
        'CPI-linked annual increases for existing leases. Market rent for new leases.'
    ),
    (
        'RRC01J00000000000000000016',
        'FI',
        'Finland',
        FALSE,
        'No statutory rent control. Increases per lease terms, typically CPI-linked.'
    ),
    (
        'RRC01J00000000000000000017',
        'PL',
        'Poland',
        FALSE,
        'Municipal housing: regulated rents. Private market: largely unregulated, contract terms apply.'
    ),
    (
        'RRC01J00000000000000000018',
        'CH',
        'Switzerland',
        FALSE,
        'Reference interest rate (hypothekarischer Referenzzinssatz) based system. CPI and cost-of-living adjustments.'
    ),
    (
        'RRC01J00000000000000000019',
        'CZ',
        'Czech Republic',
        FALSE,
        'Deregulated since 2012. Market rents with contract terms governing increases.'
    ),
    (
        'RRC01J00000000000000000020',
        'LU',
        'Luxembourg',
        FALSE,
        'Max rent = 5% of invested capital. Increases limited to cost-of-living index adjustments.'
    );

-- ============================================================
-- REGIONS (for countries with has_regional_regulations = true)
-- ============================================================
-- Germany regions
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
        'RRG01J00000000000000000001',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'BE',
        'Berlin',
        'Mietpreisbremse active. Mietspiegel updated regularly. Mietendeckel was struck down in 2021.'
    ),
    (
        'RRG01J00000000000000000002',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'BY',
        'Bavaria',
        'Mietpreisbremse in Munich and other designated areas. Kappungsgrenze 15% in 3 years in tight markets.'
    ),
    (
        'RRG01J00000000000000000003',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        'HH',
        'Hamburg',
        'Mietpreisbremse active. Kappungsgrenze 15% in 3 years.'
    );

-- Belgium regions
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
        'RRG01J00000000000000000004',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        'VLG',
        'Flanders',
        'Health index (gezondheidsindex) for annual indexation. Standard lease terms.'
    ),
    (
        'RRG01J00000000000000000005',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        'WAL',
        'Wallonia',
        'Health index for indexation. Slightly different lease regulations from Flanders.'
    ),
    (
        'RRG01J00000000000000000006',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'BE'
        ),
        'BRU',
        'Brussels',
        'Health index for indexation. Additional energy performance requirements affect rent caps.'
    );

-- United Kingdom regions
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
        'RRG01J00000000000000000007',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        'ENG',
        'England',
        'Market rent. Section 13 for periodic tenancies. Renters Reform Bill pending.'
    ),
    (
        'RRG01J00000000000000000008',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        'SCT',
        'Scotland',
        'Rent cap introduced Sept 2022, modified to 3% from April 2023. Cost of Living Act provisions.'
    ),
    (
        'RRG01J00000000000000000009',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'GB'
        ),
        'WLS',
        'Wales',
        'Renting Homes (Wales) Act 2016. Market rent with contract-based increases.'
    );

-- United States regions
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
        'RRG01J00000000000000000010',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'CA',
        'California',
        'AB 1482 (Tenant Protection Act): CPI + 5%, max 10%. Local ordinances may be stricter.'
    ),
    (
        'RRG01J00000000000000000011',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'NY',
        'New York',
        'Rent Stabilization Board sets annual adjustments. Rent controlled and rent stabilized units.'
    ),
    (
        'RRG01J00000000000000000012',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'US'
        ),
        'OR',
        'Oregon',
        'SB 608: CPI + 7%, max 10%. First statewide rent control in US (2019).'
    );

-- Canada regions
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
        'RRG01J00000000000000000013',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'ON',
        'Ontario',
        'Annual guideline increase based on Ontario CPI. Exemptions for buildings occupied after Nov 2018.'
    ),
    (
        'RRG01J00000000000000000014',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'BC',
        'British Columbia',
        'Annual allowable increase = CPI (no additional percentage since 2024). Applies to most residential tenancies.'
    ),
    (
        'RRG01J00000000000000000015',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CA'
        ),
        'QC',
        'Quebec',
        'Tribunal administratif du logement (TAL) sets recommended increase percentages annually.'
    );

-- ============================================================
-- RULES: Netherlands (NL) — 2022-2026
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
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000001',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2022,
        'REGULATED',
        2.30,
        'FIXED_PERCENTAGE',
        'CBS huurverhoging',
        '2022-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Max increase for regulated sector (social housing)'
    ),
    (
        'RRL01J00000000000000000002',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2022,
        'FREE_SECTOR',
        3.30,
        'CPI_LINKED',
        'CPI + 1%',
        '2022-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Free sector: CPI + 1% (CPI was 2.3% in reference period)'
    ),
    (
        'RRL01J00000000000000000003',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2023,
        'REGULATED',
        3.10,
        'FIXED_PERCENTAGE',
        'CBS huurverhoging',
        '2023-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Max increase for regulated sector'
    ),
    (
        'RRL01J00000000000000000004',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2023,
        'FREE_SECTOR',
        4.10,
        'CPI_LINKED',
        'CPI + 1%',
        '2023-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Free sector: CPI-based cap introduced by Wet maximering huurprijsverhogingen'
    ),
    (
        'RRL01J00000000000000000005',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2024,
        'REGULATED',
        5.80,
        'FIXED_PERCENTAGE',
        'CBS huurverhoging',
        '2024-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Max increase for regulated sector (wage growth linked)'
    ),
    (
        'RRL01J00000000000000000006',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2024,
        'FREE_SECTOR',
        5.50,
        'CPI_LINKED',
        'CPI + 1%',
        '2024-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Free sector: capped at CPI + 1%, max 5.5%'
    ),
    (
        'RRL01J00000000000000000007',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2025,
        'REGULATED',
        4.10,
        'FIXED_PERCENTAGE',
        'CBS huurverhoging',
        '2025-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Max increase for regulated sector'
    ),
    (
        'RRL01J00000000000000000008',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2025,
        'FREE_SECTOR',
        4.10,
        'CPI_LINKED',
        'CPI + 1%',
        '2025-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Free sector: expected cap'
    ),
    (
        'RRL01J00000000000000000009',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2026,
        'REGULATED',
        4.00,
        'FIXED_PERCENTAGE',
        'CBS huurverhoging',
        '2026-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Projected max increase for regulated sector'
    ),
    (
        'RRL01J00000000000000000010',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NL'
        ),
        2026,
        'FREE_SECTOR',
        4.50,
        'CPI_LINKED',
        'CPI + 1%',
        '2026-07-01',
        'ANNUAL',
        'https://www.rijksoverheid.nl/onderwerpen/huurverhoging',
        'Projected free sector cap'
    );

-- ============================================================
-- RULES: Germany (DE) — National level 2022-2026
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000011',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2022,
        'ALL',
        20.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze',
        '2022-01-01',
        'ANNUAL',
        'Standard Kappungsgrenze: max 20% in 3 years. 15% in tight housing markets.'
    ),
    (
        'RRL01J00000000000000000012',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2023,
        'ALL',
        20.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze',
        '2023-01-01',
        'ANNUAL',
        'Unchanged from 2022. Mietpreisbremse extended to 2029.'
    ),
    (
        'RRL01J00000000000000000013',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2024,
        'ALL',
        20.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze',
        '2024-01-01',
        'ANNUAL',
        'Ongoing Mietpreisbremse. New construction exempt.'
    ),
    (
        'RRL01J00000000000000000014',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2025,
        'ALL',
        20.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze',
        '2025-01-01',
        'ANNUAL',
        'Mietpreisbremse extended. Discussion on further tightening.'
    ),
    (
        'RRL01J00000000000000000015',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DE'
        ),
        2026,
        'ALL',
        20.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation of current framework.'
    );

-- Germany regional rules (Berlin)
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000016',
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
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'DE'
                )
        ),
        2022,
        'ALL',
        15.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze (tight market)',
        '2022-01-01',
        'ANNUAL',
        'Berlin designated as tight housing market: 15% cap in 3 years.'
    ),
    (
        'RRL01J00000000000000000017',
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
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'DE'
                )
        ),
        2023,
        'ALL',
        15.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze (tight market)',
        '2023-01-01',
        'ANNUAL',
        'Mietpreisbremse active. Mietspiegel 2023 published.'
    ),
    (
        'RRL01J00000000000000000018',
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
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'DE'
                )
        ),
        2024,
        'ALL',
        15.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze (tight market)',
        '2024-01-01',
        'ANNUAL',
        'Continued tight market designation.'
    ),
    (
        'RRL01J00000000000000000019',
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
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'DE'
                )
        ),
        2025,
        'ALL',
        15.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze (tight market)',
        '2025-01-01',
        'ANNUAL',
        'Berlin tight market status ongoing.'
    ),
    (
        'RRL01J00000000000000000020',
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
                region_code = 'BE'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'DE'
                )
        ),
        2026,
        'ALL',
        15.00,
        'FIXED_PERCENTAGE',
        'Kappungsgrenze (tight market)',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation.'
    );

-- ============================================================
-- RULES: Portugal (PT) — 2022-2026
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
        index_value,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000021',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PT'
        ),
        2022,
        'ALL',
        0.43,
        'INDEX_LINKED',
        'INE coefficient',
        1.0043,
        '2022-01-01',
        'ANNUAL',
        'https://www.ine.pt',
        'Coeficiente de atualizacao de rendas 2022'
    ),
    (
        'RRL01J00000000000000000022',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PT'
        ),
        2023,
        'ALL',
        2.00,
        'FIXED_PERCENTAGE',
        'Emergency cap',
        NULL,
        '2023-01-01',
        'ANNUAL',
        'https://www.ine.pt',
        'Government capped at 2% despite higher INE coefficient (5.43%). Extraordinary measure.'
    ),
    (
        'RRL01J00000000000000000023',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PT'
        ),
        2024,
        'ALL',
        6.94,
        'INDEX_LINKED',
        'INE coefficient',
        1.0694,
        '2024-01-01',
        'ANNUAL',
        'https://www.ine.pt',
        'INE coefficient 1.0694. Government provided compensation to landlords for difference.'
    ),
    (
        'RRL01J00000000000000000024',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PT'
        ),
        2025,
        'ALL',
        6.01,
        'INDEX_LINKED',
        'INE coefficient',
        1.0601,
        '2025-01-01',
        'ANNUAL',
        'https://www.ine.pt',
        'INE coefficient for 2025'
    ),
    (
        'RRL01J00000000000000000025',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PT'
        ),
        2026,
        'ALL',
        3.50,
        'INDEX_LINKED',
        'INE coefficient',
        1.0350,
        '2026-01-01',
        'ANNUAL',
        'https://www.ine.pt',
        'Projected INE coefficient'
    );

-- ============================================================
-- RULES: France (FR) — 2022-2026
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
        index_value,
        effective_date,
        frequency,
        source_url,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000026',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2022,
        'ALL',
        3.49,
        'INDEX_LINKED',
        'IRL Q2 2022',
        135.84,
        '2022-07-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/serie/001515333',
        'IRL (Indice de Reference des Loyers) Q2 2022. Bouclier loyer cap applied.'
    ),
    (
        'RRL01J00000000000000000027',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2023,
        'ALL',
        3.50,
        'FIXED_PERCENTAGE',
        'IRL capped',
        NULL,
        '2023-01-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/serie/001515333',
        'Bouclier loyer: government capped IRL at 3.5% despite higher inflation.'
    ),
    (
        'RRL01J00000000000000000028',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2024,
        'ALL',
        3.26,
        'INDEX_LINKED',
        'IRL Q1 2024',
        143.46,
        '2024-04-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/serie/001515333',
        'IRL-based increase. Bouclier loyer expired.'
    ),
    (
        'RRL01J00000000000000000029',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2025,
        'ALL',
        2.47,
        'INDEX_LINKED',
        'IRL Q1 2025',
        145.68,
        '2025-04-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/serie/001515333',
        'IRL reference value'
    ),
    (
        'RRL01J00000000000000000030',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FR'
        ),
        2026,
        'ALL',
        2.00,
        'INDEX_LINKED',
        'IRL Q1 2026',
        148.00,
        '2026-04-01',
        'QUARTERLY',
        'https://www.insee.fr/fr/statistiques/serie/001515333',
        'Projected IRL value'
    );

-- ============================================================
-- RULES: Spain (ES) — 2022-2026
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000031',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2022,
        'ALL',
        2.00,
        'FIXED_PERCENTAGE',
        'Emergency cap',
        '2022-01-01',
        'ANNUAL',
        'Government imposed 2% cap as emergency measure due to inflation (replacing CPI).'
    ),
    (
        'RRL01J00000000000000000032',
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
        'FIXED_PERCENTAGE',
        'Emergency cap extended',
        '2023-01-01',
        'ANNUAL',
        'Extended 2% emergency cap for 2023.'
    ),
    (
        'RRL01J00000000000000000033',
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
        'FIXED_PERCENTAGE',
        'Transitional cap',
        '2024-01-01',
        'ANNUAL',
        'Cap raised to 3% as transitional measure. Ley de Vivienda enacted May 2023.'
    ),
    (
        'RRL01J00000000000000000034',
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
        'IRAV (new reference index)',
        '2025-01-01',
        'ANNUAL',
        'New IRAV reference index replaces CPI. Expected to be below CPI. INE publishes.'
    ),
    (
        'RRL01J00000000000000000035',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'ES'
        ),
        2026,
        'ALL',
        NULL,
        'INDEX_LINKED',
        'IRAV',
        '2026-01-01',
        'ANNUAL',
        'IRAV index continuation. Zonas tensionadas may have stricter limits.'
    );

-- ============================================================
-- RULES: Belgium (BE) — Regional rules 2022-2026
-- ============================================================
-- Belgium - Flanders
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000036',
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
                region_code = 'VLG'
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
        3.20,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2022-01-01',
        'ANNUAL',
        'Health index-based indexation for Flanders.'
    ),
    (
        'RRL01J00000000000000000037',
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
                region_code = 'VLG'
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
        5.40,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2023-01-01',
        'ANNUAL',
        'Significant increase due to energy crisis inflation.'
    ),
    (
        'RRL01J00000000000000000038',
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
                region_code = 'VLG'
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
        3.80,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2024-01-01',
        'ANNUAL',
        'Moderating inflation reflected in health index.'
    ),
    (
        'RRL01J00000000000000000039',
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
                region_code = 'VLG'
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
        2.50,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2025-01-01',
        'ANNUAL',
        'Expected health index increase.'
    ),
    (
        'RRL01J00000000000000000040',
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
                region_code = 'VLG'
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
        2.20,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2026-01-01',
        'ANNUAL',
        'Projected health index.'
    );

-- Belgium - Brussels
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000041',
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
                region_code = 'BRU'
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
        3.20,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2022-01-01',
        'ANNUAL',
        'Health index indexation for Brussels region.'
    ),
    (
        'RRL01J00000000000000000042',
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
                region_code = 'BRU'
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
        2.00,
        'FIXED_PERCENTAGE',
        'Capped (energy crisis)',
        '2023-10-14',
        'ANNUAL',
        'Brussels capped indexation for energy-inefficient properties (EPC E, F, G).'
    ),
    (
        'RRL01J00000000000000000043',
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
                region_code = 'BRU'
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
        3.80,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2024-01-01',
        'ANNUAL',
        'Post-crisis indexation normalization.'
    ),
    (
        'RRL01J00000000000000000044',
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
                region_code = 'BRU'
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
        2.50,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2025-01-01',
        'ANNUAL',
        'Expected health index.'
    ),
    (
        'RRL01J00000000000000000045',
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
                region_code = 'BRU'
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
        2.20,
        'INDEX_LINKED',
        'Gezondheidsindex',
        '2026-01-01',
        'ANNUAL',
        'Projected.'
    );

-- ============================================================
-- RULES: Ireland (IE) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000046',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IE'
        ),
        2022,
        'RPZ',
        2.00,
        'FIXED_PERCENTAGE',
        '2022-01-01',
        'ANNUAL',
        'Rent Pressure Zones: max 2% per annum or CPI, whichever is lower.'
    ),
    (
        'RRL01J00000000000000000047',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IE'
        ),
        2023,
        'RPZ',
        2.00,
        'FIXED_PERCENTAGE',
        '2023-01-01',
        'ANNUAL',
        'RPZ 2% cap continued.'
    ),
    (
        'RRL01J00000000000000000048',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IE'
        ),
        2024,
        'RPZ',
        2.00,
        'FIXED_PERCENTAGE',
        '2024-01-01',
        'ANNUAL',
        'RPZ 2% cap. Extended to end of 2024.'
    ),
    (
        'RRL01J00000000000000000049',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IE'
        ),
        2025,
        'RPZ',
        2.00,
        'FIXED_PERCENTAGE',
        '2025-01-01',
        'ANNUAL',
        'RPZ framework continued. Review pending.'
    ),
    (
        'RRL01J00000000000000000050',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IE'
        ),
        2026,
        'RPZ',
        2.00,
        'FIXED_PERCENTAGE',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation of 2% RPZ cap.'
    );

-- ============================================================
-- RULES: Italy (IT) — 2022-2026
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000051',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2022,
        'REGULATED',
        1.50,
        'INDEX_LINKED',
        'ISTAT FOI (75%)',
        '2022-01-01',
        'ANNUAL',
        'Contratti a canone concordato: 75% of ISTAT FOI index variation.'
    ),
    (
        'RRL01J00000000000000000052',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2023,
        'REGULATED',
        5.63,
        'INDEX_LINKED',
        'ISTAT FOI (75%)',
        '2023-01-01',
        'ANNUAL',
        '75% of 7.5% ISTAT FOI annual variation.'
    ),
    (
        'RRL01J00000000000000000053',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2024,
        'REGULATED',
        4.13,
        'INDEX_LINKED',
        'ISTAT FOI (75%)',
        '2024-01-01',
        'ANNUAL',
        '75% of 5.5% ISTAT FOI variation.'
    ),
    (
        'RRL01J00000000000000000054',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2025,
        'REGULATED',
        1.50,
        'INDEX_LINKED',
        'ISTAT FOI (75%)',
        '2025-01-01',
        'ANNUAL',
        '75% of estimated 2% ISTAT FOI variation.'
    ),
    (
        'RRL01J00000000000000000055',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'IT'
        ),
        2026,
        'REGULATED',
        1.13,
        'INDEX_LINKED',
        'ISTAT FOI (75%)',
        '2026-01-01',
        'ANNUAL',
        'Projected 75% of 1.5% ISTAT FOI.'
    );

-- ============================================================
-- RULES: Austria (AT) — 2022-2026
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000056',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2022,
        'REGULATED',
        5.85,
        'INDEX_LINKED',
        'Richtwertmietzins (VPI)',
        '2022-04-01',
        'ANNUAL',
        'Richtwert adjusted by VPI (consumer price index). Applied April 1.'
    ),
    (
        'RRL01J00000000000000000057',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2023,
        'REGULATED',
        8.60,
        'INDEX_LINKED',
        'Richtwertmietzins (VPI)',
        '2023-04-01',
        'ANNUAL',
        'High CPI reflected in Richtwert adjustment. Government deferred to June.'
    ),
    (
        'RRL01J00000000000000000058',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2024,
        'REGULATED',
        5.00,
        'FIXED_PERCENTAGE',
        'Government cap',
        '2024-01-01',
        'ANNUAL',
        'Government capped Richtwert and Kategoriemietzins increases at 5%.'
    ),
    (
        'RRL01J00000000000000000059',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2025,
        'REGULATED',
        4.50,
        'INDEX_LINKED',
        'Richtwertmietzins (VPI)',
        '2025-04-01',
        'ANNUAL',
        'Expected VPI-based adjustment.'
    ),
    (
        'RRL01J00000000000000000060',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'AT'
        ),
        2026,
        'REGULATED',
        3.00,
        'INDEX_LINKED',
        'Richtwertmietzins (VPI)',
        '2026-04-01',
        'ANNUAL',
        'Projected VPI adjustment.'
    );

-- ============================================================
-- RULES: United Kingdom (GB) — Regional rules 2022-2026
-- ============================================================
-- Scotland
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000061',
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
                region_code = 'SCT'
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
        NULL,
        'MARKET_RENT',
        '2022-01-01',
        'ANNUAL',
        'Market rent until Sept 2022. Rent freeze introduced Sept 6, 2022 (Cost of Living Act).'
    ),
    (
        'RRL01J00000000000000000062',
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
                region_code = 'SCT'
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
        3.00,
        'FIXED_PERCENTAGE',
        '2023-04-01',
        'ANNUAL',
        'Rent cap of 3% from April 2023 (replacing freeze). Cost of Living Act.'
    ),
    (
        'RRL01J00000000000000000063',
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
                region_code = 'SCT'
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
        3.00,
        'FIXED_PERCENTAGE',
        '2024-04-01',
        'ANNUAL',
        'Rent cap continued at 3% for in-tenancy increases.'
    ),
    (
        'RRL01J00000000000000000064',
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
                region_code = 'SCT'
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
        NULL,
        'MARKET_RENT',
        '2025-04-01',
        'ANNUAL',
        'Emergency provisions expired. Housing (Scotland) Bill pending.'
    ),
    (
        'RRL01J00000000000000000065',
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
                region_code = 'SCT'
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
        NULL,
        'MARKET_RENT',
        '2026-01-01',
        'ANNUAL',
        'New framework expected under Housing Bill.'
    );

-- England
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000066',
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
                region_code = 'ENG'
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
        '2022-01-01',
        'ANNUAL',
        'Market rent. Section 13 procedure for periodic tenancies. No statutory cap.'
    ),
    (
        'RRL01J00000000000000000067',
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
                region_code = 'ENG'
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
        'Market rent. Renters Reform Bill introduced.'
    ),
    (
        'RRL01J00000000000000000068',
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
                region_code = 'ENG'
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
        'Market rent. Renters Reform Bill progressing.'
    ),
    (
        'RRL01J00000000000000000069',
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
                region_code = 'ENG'
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
        'Renters Rights Bill expected to pass. Section 21 abolition planned.'
    ),
    (
        'RRL01J00000000000000000070',
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
                region_code = 'ENG'
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
        'New framework under Renters Rights Bill.'
    );

-- ============================================================
-- RULES: United States (US) — Regional rules 2022-2026
-- ============================================================
-- California
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000071',
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
                region_code = 'CA'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2022,
        'ALL',
        10.00,
        'CPI_LINKED',
        'CPI + 5% (max 10%)',
        '2022-08-01',
        'ANNUAL',
        'AB 1482: lower of 5% + local CPI or 10%. Aug 1 - Jul 31 cycle.'
    ),
    (
        'RRL01J00000000000000000072',
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
                region_code = 'CA'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2023,
        'ALL',
        10.00,
        'CPI_LINKED',
        'CPI + 5% (max 10%)',
        '2023-08-01',
        'ANNUAL',
        'CPI-based: effectively ~8.8% for Aug 2023 cycle.'
    ),
    (
        'RRL01J00000000000000000073',
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
                region_code = 'CA'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2024,
        'ALL',
        8.80,
        'CPI_LINKED',
        'CPI + 5% (max 10%)',
        '2024-08-01',
        'ANNUAL',
        'Effective cap around 8.8% (5% + 3.8% CPI).'
    ),
    (
        'RRL01J00000000000000000074',
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
                region_code = 'CA'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2025,
        'ALL',
        8.40,
        'CPI_LINKED',
        'CPI + 5% (max 10%)',
        '2025-08-01',
        'ANNUAL',
        'Effective cap: 5% + ~3.4% CPI.'
    ),
    (
        'RRL01J00000000000000000075',
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
                region_code = 'CA'
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
        7.50,
        'CPI_LINKED',
        'CPI + 5% (max 10%)',
        '2026-08-01',
        'ANNUAL',
        'Projected. AB 1482 sunset 2030.'
    );

-- New York
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000076',
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
                region_code = 'NY'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2022,
        'RENT_STABILIZED',
        3.25,
        'FIXED_PERCENTAGE',
        'RGB Order #54 (1-year)',
        '2022-10-01',
        'ANNUAL',
        'RGB: 3.25% for 1-year leases, 5% for 2-year. Oct 2022 - Sep 2023 cycle.'
    ),
    (
        'RRL01J00000000000000000077',
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
                region_code = 'NY'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2023,
        'RENT_STABILIZED',
        3.00,
        'FIXED_PERCENTAGE',
        'RGB Order #55 (1-year)',
        '2023-10-01',
        'ANNUAL',
        'RGB: 3% for 1-year leases, 2.75% for 2-year.'
    ),
    (
        'RRL01J00000000000000000078',
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
                region_code = 'NY'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2024,
        'RENT_STABILIZED',
        2.75,
        'FIXED_PERCENTAGE',
        'RGB Order #56 (1-year)',
        '2024-10-01',
        'ANNUAL',
        'RGB: 2.75% for 1-year leases, 5.25% for 2-year.'
    ),
    (
        'RRL01J00000000000000000079',
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
                region_code = 'NY'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2025,
        'RENT_STABILIZED',
        2.50,
        'FIXED_PERCENTAGE',
        'RGB projected',
        '2025-10-01',
        'ANNUAL',
        'Projected RGB increase.'
    ),
    (
        'RRL01J00000000000000000080',
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
                region_code = 'NY'
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
        'RENT_STABILIZED',
        2.50,
        'FIXED_PERCENTAGE',
        'RGB projected',
        '2026-10-01',
        'ANNUAL',
        'Projected.'
    );

-- Oregon
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000081',
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
                region_code = 'OR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2022,
        'ALL',
        9.90,
        'CPI_LINKED',
        'CPI + 7%',
        '2022-01-01',
        'ANNUAL',
        'SB 608: 7% + 2.9% CPI = 9.9%.'
    ),
    (
        'RRL01J00000000000000000082',
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
                region_code = 'OR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2023,
        'ALL',
        14.60,
        'CPI_LINKED',
        'CPI + 7%',
        '2023-01-01',
        'ANNUAL',
        'SB 608: 7% + 7.6% CPI = 14.6%. Maximum 14.6%.'
    ),
    (
        'RRL01J00000000000000000083',
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
                region_code = 'OR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2024,
        'ALL',
        10.00,
        'CPI_LINKED',
        'CPI + 7% (max 10%)',
        '2024-01-01',
        'ANNUAL',
        'SB 611 amended: max cap of 10% introduced.'
    ),
    (
        'RRL01J00000000000000000084',
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
                region_code = 'OR'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'US'
                )
        ),
        2025,
        'ALL',
        10.00,
        'CPI_LINKED',
        'CPI + 7% (max 10%)',
        '2025-01-01',
        'ANNUAL',
        '10% max cap continuation.'
    ),
    (
        'RRL01J00000000000000000085',
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
                region_code = 'OR'
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
        10.00,
        'CPI_LINKED',
        'CPI + 7% (max 10%)',
        '2026-01-01',
        'ANNUAL',
        'Projected continuation.'
    );

-- ============================================================
-- RULES: Canada (CA) — Regional rules 2022-2026
-- ============================================================
-- Ontario
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000086',
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
                region_code = 'ON'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2022,
        'ALL',
        1.20,
        'CPI_LINKED',
        'Ontario CPI guideline',
        '2022-01-01',
        'ANNUAL',
        'Annual guideline increase. Exemption for units first occupied after Nov 15, 2018.'
    ),
    (
        'RRL01J00000000000000000087',
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
                region_code = 'ON'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2023,
        'ALL',
        2.50,
        'CPI_LINKED',
        'Ontario CPI guideline',
        '2023-01-01',
        'ANNUAL',
        'Guideline increase 2.5%.'
    ),
    (
        'RRL01J00000000000000000088',
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
                region_code = 'ON'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2024,
        'ALL',
        2.50,
        'CPI_LINKED',
        'Ontario CPI guideline',
        '2024-01-01',
        'ANNUAL',
        'Capped at 2.5% (actual CPI would have been higher).'
    ),
    (
        'RRL01J00000000000000000089',
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
                region_code = 'ON'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2025,
        'ALL',
        2.50,
        'CPI_LINKED',
        'Ontario CPI guideline',
        '2025-01-01',
        'ANNUAL',
        '2.5% guideline.'
    ),
    (
        'RRL01J00000000000000000090',
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
                region_code = 'ON'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2026,
        'ALL',
        2.50,
        'CPI_LINKED',
        'Ontario CPI guideline',
        '2026-01-01',
        'ANNUAL',
        'Projected guideline.'
    );

-- British Columbia
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000091',
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
                region_code = 'BC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2022,
        'ALL',
        1.50,
        'CPI_LINKED',
        'BC CPI',
        '2022-01-01',
        'ANNUAL',
        'Allowable rent increase = CPI (no extra %).'
    ),
    (
        'RRL01J00000000000000000092',
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
                region_code = 'BC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2023,
        'ALL',
        2.00,
        'CPI_LINKED',
        'BC CPI',
        '2023-01-01',
        'ANNUAL',
        'Max increase = CPI (2%).'
    ),
    (
        'RRL01J00000000000000000093',
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
                region_code = 'BC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2024,
        'ALL',
        3.50,
        'CPI_LINKED',
        'BC CPI',
        '2024-01-01',
        'ANNUAL',
        'CPI-based (3.5%).'
    ),
    (
        'RRL01J00000000000000000094',
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
                region_code = 'BC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2025,
        'ALL',
        3.00,
        'CPI_LINKED',
        'BC CPI',
        '2025-01-01',
        'ANNUAL',
        'CPI-based increase.'
    ),
    (
        'RRL01J00000000000000000095',
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
                region_code = 'BC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2026,
        'ALL',
        2.50,
        'CPI_LINKED',
        'BC CPI',
        '2026-01-01',
        'ANNUAL',
        'Projected.'
    );

-- Quebec
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000096',
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
                region_code = 'QC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2022,
        'ALL',
        1.28,
        'FIXED_PERCENTAGE',
        'TAL recommended',
        '2022-01-01',
        'ANNUAL',
        'Tribunal administratif du logement recommended increase.'
    ),
    (
        'RRL01J00000000000000000097',
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
                region_code = 'QC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2023,
        'ALL',
        2.30,
        'FIXED_PERCENTAGE',
        'TAL recommended',
        '2023-01-01',
        'ANNUAL',
        'TAL recommended increase, higher due to costs.'
    ),
    (
        'RRL01J00000000000000000098',
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
                region_code = 'QC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2024,
        'ALL',
        3.90,
        'FIXED_PERCENTAGE',
        'TAL recommended',
        '2024-01-01',
        'ANNUAL',
        'TAL recommended. Highest in years due to inflation.'
    ),
    (
        'RRL01J00000000000000000099',
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
                region_code = 'QC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2025,
        'ALL',
        3.20,
        'FIXED_PERCENTAGE',
        'TAL recommended',
        '2025-01-01',
        'ANNUAL',
        'TAL recommended increase.'
    ),
    (
        'RRL01J00000000000000000100',
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
                region_code = 'QC'
                AND country_id = (
                    SELECT
                        id
                    FROM
                        rent_regulation_countries
                    WHERE
                        country_code = 'CA'
                )
        ),
        2026,
        'ALL',
        2.80,
        'FIXED_PERCENTAGE',
        'TAL recommended',
        '2026-01-01',
        'ANNUAL',
        'Projected.'
    );

-- ============================================================
-- RULES: Denmark (DK) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000101',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2022,
        'PRE_1992',
        'NEGOTIATED',
        '2022-01-01',
        'ANNUAL',
        'Cost-based system (omkostningsbestemt leje). Increases follow documented cost increases.'
    ),
    (
        'RRL01J00000000000000000102',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2023,
        'PRE_1992',
        'NEGOTIATED',
        '2023-01-01',
        'ANNUAL',
        'Net price index (nettoprisindekset) for regulated leases.'
    ),
    (
        'RRL01J00000000000000000103',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2024,
        'PRE_1992',
        'NEGOTIATED',
        '2024-01-01',
        'ANNUAL',
        'Continued cost-based system.'
    ),
    (
        'RRL01J00000000000000000104',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2025,
        'PRE_1992',
        'NEGOTIATED',
        '2025-01-01',
        'ANNUAL',
        'Ongoing cost-based regulation.'
    ),
    (
        'RRL01J00000000000000000105',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'DK'
        ),
        2026,
        'PRE_1992',
        'NEGOTIATED',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation.'
    );

-- ============================================================
-- RULES: Sweden (SE) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_percentage,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000106',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'SE'
        ),
        2022,
        'ALL',
        1.30,
        'NEGOTIATED',
        '2022-01-01',
        'ANNUAL',
        'Bruksvardesystemet: collectively negotiated. ~1.3% average outcome.'
    ),
    (
        'RRL01J00000000000000000107',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'SE'
        ),
        2023,
        'ALL',
        4.10,
        'NEGOTIATED',
        '2023-01-01',
        'ANNUAL',
        'Higher negotiated increase due to inflation.'
    ),
    (
        'RRL01J00000000000000000108',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'SE'
        ),
        2024,
        'ALL',
        4.60,
        'NEGOTIATED',
        '2024-01-01',
        'ANNUAL',
        'Continued inflation-adjusted negotiations.'
    ),
    (
        'RRL01J00000000000000000109',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'SE'
        ),
        2025,
        'ALL',
        3.20,
        'NEGOTIATED',
        '2025-01-01',
        'ANNUAL',
        'Moderating negotiated increases.'
    ),
    (
        'RRL01J00000000000000000110',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'SE'
        ),
        2026,
        'ALL',
        2.50,
        'NEGOTIATED',
        '2026-01-01',
        'ANNUAL',
        'Projected negotiated outcome.'
    );

-- ============================================================
-- RULES: Norway (NO) — 2022-2026
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000111',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NO'
        ),
        2022,
        'ALL',
        3.50,
        'CPI_LINKED',
        'KPI (SSB)',
        '2022-01-01',
        'ANNUAL',
        'Annual CPI-linked increase for existing leases.'
    ),
    (
        'RRL01J00000000000000000112',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NO'
        ),
        2023,
        'ALL',
        5.80,
        'CPI_LINKED',
        'KPI (SSB)',
        '2023-01-01',
        'ANNUAL',
        'High CPI reflected.'
    ),
    (
        'RRL01J00000000000000000113',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NO'
        ),
        2024,
        'ALL',
        4.80,
        'CPI_LINKED',
        'KPI (SSB)',
        '2024-01-01',
        'ANNUAL',
        'CPI-linked increase.'
    ),
    (
        'RRL01J00000000000000000114',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NO'
        ),
        2025,
        'ALL',
        3.00,
        'CPI_LINKED',
        'KPI (SSB)',
        '2025-01-01',
        'ANNUAL',
        'Expected CPI-linked.'
    ),
    (
        'RRL01J00000000000000000115',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'NO'
        ),
        2026,
        'ALL',
        2.50,
        'CPI_LINKED',
        'KPI (SSB)',
        '2026-01-01',
        'ANNUAL',
        'Projected.'
    );

-- ============================================================
-- RULES: Finland (FI) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000116',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2022,
        'ALL',
        'NEGOTIATED',
        '2022-01-01',
        'ANNUAL',
        'No statutory cap. Increases per lease terms, typically CPI-linked.'
    ),
    (
        'RRL01J00000000000000000117',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2023,
        'ALL',
        'NEGOTIATED',
        '2023-01-01',
        'ANNUAL',
        'Market-based. Most leases specify CPI indexation.'
    ),
    (
        'RRL01J00000000000000000118',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2024,
        'ALL',
        'NEGOTIATED',
        '2024-01-01',
        'ANNUAL',
        'No change in regulatory framework.'
    ),
    (
        'RRL01J00000000000000000119',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2025,
        'ALL',
        'NEGOTIATED',
        '2025-01-01',
        'ANNUAL',
        'Continued market-based system.'
    ),
    (
        'RRL01J00000000000000000120',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'FI'
        ),
        2026,
        'ALL',
        'NEGOTIATED',
        '2026-01-01',
        'ANNUAL',
        'No changes expected.'
    );

-- ============================================================
-- RULES: Poland (PL) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000121',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2022,
        'MUNICIPAL',
        'NEGOTIATED',
        '2022-01-01',
        'ANNUAL',
        'Municipal housing: regulated rents set by local authorities. Private market largely unregulated.'
    ),
    (
        'RRL01J00000000000000000122',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2023,
        'MUNICIPAL',
        'NEGOTIATED',
        '2023-01-01',
        'ANNUAL',
        'Inflation up to 18.4% in Feb 2023. Private rents rose significantly.'
    ),
    (
        'RRL01J00000000000000000123',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2024,
        'MUNICIPAL',
        'NEGOTIATED',
        '2024-01-01',
        'ANNUAL',
        'Municipal housing increases follow local council decisions.'
    ),
    (
        'RRL01J00000000000000000124',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2025,
        'MUNICIPAL',
        'NEGOTIATED',
        '2025-01-01',
        'ANNUAL',
        'Continued framework.'
    ),
    (
        'RRL01J00000000000000000125',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'PL'
        ),
        2026,
        'MUNICIPAL',
        'NEGOTIATED',
        '2026-01-01',
        'ANNUAL',
        'No regulatory changes expected.'
    );

-- ============================================================
-- RULES: Switzerland (CH) — 2022-2026
-- ============================================================
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000126',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        2022,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        1.2500,
        '2022-01-01',
        'QUARTERLY',
        'Reference interest rate 1.25%. Plus CPI and maintenance cost pass-through.'
    ),
    (
        'RRL01J00000000000000000127',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        2023,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        1.5000,
        '2023-06-01',
        'QUARTERLY',
        'Rate raised to 1.5% (June 2023). Allows ~3% rent increase.'
    ),
    (
        'RRL01J00000000000000000128',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        2024,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        1.7500,
        '2024-03-01',
        'QUARTERLY',
        'Rate raised to 1.75% (March 2024). Further increase possible.'
    ),
    (
        'RRL01J00000000000000000129',
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
        1.7500,
        '2025-01-01',
        'QUARTERLY',
        'Rate stable at 1.75%.'
    ),
    (
        'RRL01J00000000000000000130',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CH'
        ),
        2026,
        'ALL',
        'INDEX_LINKED',
        'Hypothekarischer Referenzzinssatz',
        1.5000,
        '2026-01-01',
        'QUARTERLY',
        'Projected rate decrease.'
    );

-- ============================================================
-- RULES: Czech Republic (CZ) — 2022-2026
-- ============================================================
INSERT INTO
    rent_regulation_rules (
        identifier,
        country_id,
        YEAR,
        property_category,
        max_increase_type,
        effective_date,
        frequency,
        notes
    )
VALUES
    (
        'RRL01J00000000000000000131',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CZ'
        ),
        2022,
        'ALL',
        'MARKET_RENT',
        '2022-01-01',
        'ANNUAL',
        'Fully deregulated since 2012. Contract terms govern increases.'
    ),
    (
        'RRL01J00000000000000000132',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CZ'
        ),
        2023,
        'ALL',
        'MARKET_RENT',
        '2023-01-01',
        'ANNUAL',
        'Market rent. No statutory limits.'
    ),
    (
        'RRL01J00000000000000000133',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CZ'
        ),
        2024,
        'ALL',
        'MARKET_RENT',
        '2024-01-01',
        'ANNUAL',
        'Continued deregulated market.'
    ),
    (
        'RRL01J00000000000000000134',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CZ'
        ),
        2025,
        'ALL',
        'MARKET_RENT',
        '2025-01-01',
        'ANNUAL',
        'No regulatory changes.'
    ),
    (
        'RRL01J00000000000000000135',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'CZ'
        ),
        2026,
        'ALL',
        'MARKET_RENT',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation.'
    );

-- ============================================================
-- RULES: Luxembourg (LU) — 2022-2026
-- ============================================================
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
        notes
    )
VALUES
    (
        'RRL01J00000000000000000136',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2022,
        'ALL',
        'INDEX_LINKED',
        'Cost-of-living index (STATEC)',
        '2022-01-01',
        'ANNUAL',
        'Max rent = 5% of invested capital. Adjustments follow cost-of-living index.'
    ),
    (
        'RRL01J00000000000000000137',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2023,
        'ALL',
        'INDEX_LINKED',
        'Cost-of-living index (STATEC)',
        '2023-01-01',
        'ANNUAL',
        'Indexation temporarily suspended for some categories due to inflation.'
    ),
    (
        'RRL01J00000000000000000138',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2024,
        'ALL',
        'INDEX_LINKED',
        'Cost-of-living index (STATEC)',
        '2024-01-01',
        'ANNUAL',
        'Housing pact reform discussions ongoing.'
    ),
    (
        'RRL01J00000000000000000139',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2025,
        'ALL',
        'INDEX_LINKED',
        'Cost-of-living index (STATEC)',
        '2025-01-01',
        'ANNUAL',
        'Continued framework.'
    ),
    (
        'RRL01J00000000000000000140',
        (
            SELECT
                id
            FROM
                rent_regulation_countries
            WHERE
                country_code = 'LU'
        ),
        2026,
        'ALL',
        'INDEX_LINKED',
        'Cost-of-living index (STATEC)',
        '2026-01-01',
        'ANNUAL',
        'Expected continuation.'
    );

-- ============================================================
-- CORRECTIONS: Verified against official government sources
-- ============================================================
-- NETHERLANDS (NL)
-- ============================================================
-- 2025 REGULATED: 4.10% -> 5.00% (government set 5.0% for 2025)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    notes = 'Government set max increase at 5.0% for regulated sector (social housing) 2025'
WHERE
    identifier = 'RRL01J00000000000000000007';

-- 2026 REGULATED: 4.00% -> 4.10% (government set 4.1% for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.10,
    notes = 'Government set max increase at 4.1% for regulated sector 2026'
WHERE
    identifier = 'RRL01J00000000000000000009';

-- 2026 FREE_SECTOR: 4.50% -> 4.40% (cap is 4.4% for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.40,
    notes = 'Free sector cap 4.4% for 2026'
WHERE
    identifier = 'RRL01J00000000000000000010';

-- All FREE_SECTOR rules: effective_date should be January 1 (not July 1)
-- Free sector increases can happen from January 1
UPDATE rent_regulation_rules
SET
    effective_date = concat(YEAR, '-01-01')::DATE
WHERE
    identifier IN (
        'RRL01J00000000000000000002',
        'RRL01J00000000000000000004',
        'RRL01J00000000000000000006',
        'RRL01J00000000000000000008',
        'RRL01J00000000000000000010'
    );

-- ============================================================
-- PORTUGAL (PT)
-- ============================================================
-- 2025: 6.01% -> 2.16% (actual INE coefficient for 2025)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.16,
    index_value = 1.0216,
    notes = 'INE coefficient 1.0216 for 2025 (actual published value)'
WHERE
    identifier = 'RRL01J00000000000000000024';

-- 2026: 3.50% -> 2.24% (actual INE coefficient for 2026)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.24,
    index_value = 1.0224,
    notes = 'INE coefficient 1.0224 for 2026 (actual published value)'
WHERE
    identifier = 'RRL01J00000000000000000025';

-- ============================================================
-- FRANCE (FR)
-- ============================================================
-- 2025: max_increase_percentage 2.47% -> 1.40%, index_value 145.68 -> 145.47 (Q4 2024 IRL)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.40,
    index_value = 145.47,
    index_name = 'IRL Q4 2024',
    notes = 'IRL Q4 2024 reference value 145.47. Year-on-year variation 1.40%.'
WHERE
    identifier = 'RRL01J00000000000000000029';

-- ============================================================
-- SPAIN (ES)
-- ============================================================
-- 2022: effective_date '2022-01-01' -> '2022-03-31' (Royal Decree-Law enacted March 2022)
UPDATE rent_regulation_rules
SET
    effective_date = '2022-03-31',
    notes = 'Government imposed 2% cap as emergency measure due to inflation (Royal Decree-Law, enacted March 2022).'
WHERE
    identifier = 'RRL01J00000000000000000031';

-- ============================================================
-- GERMANY (DE) — National rules
-- ============================================================
-- All national DE rules (2022-2026): frequency 'ANNUAL' -> 'TRIENNIAL'
-- Kappungsgrenze is 20% cap over 3 years, not an annual cap
UPDATE rent_regulation_rules
SET
    frequency = 'TRIENNIAL',
    notes = CASE identifier
        WHEN 'RRL01J00000000000000000011' THEN 'Kappungsgrenze: max 20% over 3 years. 15% in tight housing markets (angespannte Wohnungsmarkte).'
        WHEN 'RRL01J00000000000000000012' THEN 'Kappungsgrenze unchanged. Mietpreisbremse extended to 2029. 20% cap applies over rolling 3-year period.'
        WHEN 'RRL01J00000000000000000013' THEN 'Ongoing Mietpreisbremse. New construction exempt. 20% cap over 3 years.'
        WHEN 'RRL01J00000000000000000014' THEN 'Mietpreisbremse extended. 20% cap over 3 years. Discussion on further tightening.'
        WHEN 'RRL01J00000000000000000015' THEN 'Expected continuation. 20% Kappungsgrenze over 3 years.'
    END
WHERE
    identifier IN (
        'RRL01J00000000000000000011',
        'RRL01J00000000000000000012',
        'RRL01J00000000000000000013',
        'RRL01J00000000000000000014',
        'RRL01J00000000000000000015'
    );

-- ============================================================
-- BELGIUM (BE) — Flanders (VLG)
-- ============================================================
-- 2022: 3.20% -> 3.45%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.45,
    notes = 'Health index (gezondheidsindex) for Flanders 2022: 3.45%.'
WHERE
    identifier = 'RRL01J00000000000000000036';

-- 2023: 5.40% -> 9.10% (health index spike)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.10,
    notes = 'Health index spike for Flanders 2023: 9.10% due to energy crisis inflation.'
WHERE
    identifier = 'RRL01J00000000000000000037';

-- 2024: 3.80% -> 3.72%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.72,
    notes = 'Health index for Flanders 2024: 3.72%.'
WHERE
    identifier = 'RRL01J00000000000000000038';

-- 2025: 2.50% -> 3.22%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.22,
    notes = 'Health index for Flanders 2025: 3.22%.'
WHERE
    identifier = 'RRL01J00000000000000000039';

-- 2026: 2.20% -> 2.42%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.42,
    notes = 'Health index for Flanders 2026: 2.42%.'
WHERE
    identifier = 'RRL01J00000000000000000040';

-- ============================================================
-- BELGIUM (BE) — Brussels (BRU)
-- ============================================================
-- 2022: 3.20% -> 3.45%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.45,
    notes = 'Health index for Brussels 2022: 3.45%.'
WHERE
    identifier = 'RRL01J00000000000000000041';

-- 2023: 2.00% -> 9.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.10,
    max_increase_type = 'INDEX_LINKED',
    index_name = 'Gezondheidsindex',
    notes = 'Health index for Brussels 2023: 9.10%. Energy-inefficient properties (EPC E, F, G) had additional caps.'
WHERE
    identifier = 'RRL01J00000000000000000042';

-- 2024: 3.80% -> 3.72%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.72,
    notes = 'Health index for Brussels 2024: 3.72%.'
WHERE
    identifier = 'RRL01J00000000000000000043';

-- 2025: 2.50% -> 3.22%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.22,
    notes = 'Health index for Brussels 2025: 3.22%.'
WHERE
    identifier = 'RRL01J00000000000000000044';

-- 2026: 2.20% -> 2.42%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.42,
    notes = 'Health index for Brussels 2026: 2.42%.'
WHERE
    identifier = 'RRL01J00000000000000000045';

-- ============================================================
-- AUSTRIA (AT)
-- ============================================================
-- 2024: 5.00% -> 0.00%, suspended by government decree
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.00,
    notes = 'CPI increase suspended for 2024 by government decree. Richtwert and Kategoriemietzins frozen.'
WHERE
    identifier = 'RRL01J00000000000000000058';

-- 2025: 4.50% -> 2.90%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.90,
    notes = 'VPI-based Richtwert adjustment 2.90% for 2025.'
WHERE
    identifier = 'RRL01J00000000000000000059';

-- 2026: 3.00% -> 1.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.00,
    notes = 'VPI-based Richtwert adjustment 1.00% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000060';

-- ============================================================
-- ITALY (IT) — FOI values corrected
-- ============================================================
-- 2022: index_value -> 2.85, max_increase_percentage (75% of FOI) -> 2.14%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.14,
    index_value = 2.85,
    notes = '75% of ISTAT FOI index variation 2.85% = 2.14%. Contratti a canone concordato.'
WHERE
    identifier = 'RRL01J00000000000000000051';

-- 2023: index_value -> 8.48, max_increase_percentage -> 6.36%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 6.36,
    index_value = 8.48,
    notes = '75% of ISTAT FOI index variation 8.48% = 6.36%.'
WHERE
    identifier = 'RRL01J00000000000000000052';

-- 2024: index_value -> 0.45, max_increase_percentage -> 0.34%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.34,
    index_value = 0.45,
    notes = '75% of ISTAT FOI index variation 0.45% = 0.34%.'
WHERE
    identifier = 'RRL01J00000000000000000053';

-- 2025: index_value -> 0.83, max_increase_percentage -> 0.62%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.62,
    index_value = 0.83,
    notes = '75% of ISTAT FOI index variation 0.83% = 0.62%.'
WHERE
    identifier = 'RRL01J00000000000000000054';

-- 2026: index_value -> 0.83, max_increase_percentage -> 0.62%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 0.62,
    index_value = 0.83,
    notes = '75% of ISTAT FOI index variation 0.83% = 0.62%. Projected.'
WHERE
    identifier = 'RRL01J00000000000000000055';

-- ============================================================
-- UNITED KINGDOM — Scotland (SCT)
-- ============================================================
-- 2024: Add note that 3% cap expired March 31, 2024
UPDATE rent_regulation_rules
SET
    notes = '3% rent cap expired March 31, 2024. After expiry, market rules apply with Rent Adjudication via First-tier Tribunal.'
WHERE
    identifier = 'RRL01J00000000000000000063';

-- ============================================================
-- UNITED STATES — New York (NY)
-- ============================================================
-- 2025: 2.50% -> 3.00% (RGB Order #57)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.00,
    index_name = 'RGB Order #57 (1-year)',
    notes = 'RGB Order #57: 3.00% for 1-year leases. Oct 2025 - Sep 2026 cycle.'
WHERE
    identifier = 'RRL01J00000000000000000079';

-- NY region summary: fix board name
UPDATE rent_regulation_regions
SET
    summary = 'Rent Guidelines Board (RGB). Order-based annual increases for rent-stabilized apartments.'
WHERE
    region_code = 'NY'
    AND country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'US'
    );

-- ============================================================
-- UNITED STATES — Oregon (OR)
-- ============================================================
-- 2026: 10.00% -> 9.50%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 9.50,
    notes = 'CPI + 7% = 9.50% for 2026 (under 10% max cap).'
WHERE
    identifier = 'RRL01J00000000000000000085';

-- ============================================================
-- UNITED STATES — California (CA)
-- ============================================================
-- 2025: 8.40% -> 8.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 8.00,
    notes = 'AB 1482: 5% + 3.0% CPI = 8.00% effective cap for Aug 2025 cycle.'
WHERE
    identifier = 'RRL01J00000000000000000074';

-- ============================================================
-- CANADA — Ontario (ON)
-- ============================================================
-- 2026: 2.50% -> 2.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.10,
    notes = 'Ontario guideline increase 2.10% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000090';

-- ============================================================
-- CANADA — British Columbia (BC)
-- ============================================================
-- 2026: 2.50% -> 2.30%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 2.30,
    notes = 'BC allowable rent increase 2.30% for 2026 (CPI-based).'
WHERE
    identifier = 'RRL01J00000000000000000095';

-- ============================================================
-- CANADA — Quebec (QC)
-- ============================================================
-- 2024: 3.90% -> 4.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.00,
    notes = 'TAL recommended increase 4.00% for 2024. Highest in years due to inflation.'
WHERE
    identifier = 'RRL01J00000000000000000098';

-- 2025: 3.20% -> 5.90% (historic high)
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.90,
    notes = 'TAL recommended increase 5.90% for 2025. Historic high reflecting municipal tax and insurance cost increases.'
WHERE
    identifier = 'RRL01J00000000000000000099';

-- 2026: 2.80% -> 3.10%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.10,
    notes = 'TAL recommended increase 3.10% for 2026.'
WHERE
    identifier = 'RRL01J00000000000000000100';

-- ============================================================
-- DENMARK (DK) — Country summary typo fix
-- ============================================================
UPDATE rent_regulation_countries
SET
    summary = 'Cost-based rent regulation for pre-1992 buildings. Newer buildings: market rent with "det lejedes vaerdi" limits.'
WHERE
    country_code = 'DK';

-- ============================================================
-- SWEDEN (SE)
-- ============================================================
-- 2022: 1.30% -> 1.70%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 1.70,
    notes = 'Bruksvardesystemet: collectively negotiated. ~1.7% average outcome for 2022.'
WHERE
    identifier = 'RRL01J00000000000000000106';

-- 2024: 4.60% -> 5.00%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.00,
    notes = 'Collectively negotiated increase 5.00% for 2024.'
WHERE
    identifier = 'RRL01J00000000000000000108';

-- 2025: 3.20% -> 4.80%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 4.80,
    notes = 'Collectively negotiated increase 4.80% for 2025.'
WHERE
    identifier = 'RRL01J00000000000000000109';

-- ============================================================
-- NORWAY (NO)
-- ============================================================
-- 2022: 3.50% -> 5.76%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 5.76,
    notes = 'CPI-linked increase 5.76% for 2022 (KPI annual average).'
WHERE
    identifier = 'RRL01J00000000000000000111';

-- 2024: 4.80% -> 3.30%
UPDATE rent_regulation_rules
SET
    max_increase_percentage = 3.30,
    notes = 'CPI-linked increase 3.30% for 2024.'
WHERE
    identifier = 'RRL01J00000000000000000113';

-- ============================================================
-- SWITZERLAND (CH)
-- ============================================================
-- 2025: index_value 1.75 -> 1.25, notes update
UPDATE rent_regulation_rules
SET
    index_value = 1.2500,
    notes = 'Reference interest rate dropped from 1.75% to 1.50% (March 2025), then to 1.25% (September 2025). Tenants may request rent reductions.'
WHERE
    identifier = 'RRL01J00000000000000000129';

-- 2026: index_value 1.50 -> 1.25
UPDATE rent_regulation_rules
SET
    index_value = 1.2500,
    notes = 'Reference interest rate stable at 1.25%. No rent increase entitlement for landlords when rate unchanged.'
WHERE
    identifier = 'RRL01J00000000000000000130';

-- ============================================================
-- Germany: additional_conditions for Kappungsgrenze
-- ============================================================
UPDATE rent_regulation_rules
SET
    additional_conditions = 'Kappungsgrenze: total increases capped at 20% over any rolling 3-year period (15% in tight housing markets).'
WHERE
    identifier IN (
        'RRL01J00000000000000000011',
        'RRL01J00000000000000000012',
        'RRL01J00000000000000000013',
        'RRL01J00000000000000000014',
        'RRL01J00000000000000000015'
    );
