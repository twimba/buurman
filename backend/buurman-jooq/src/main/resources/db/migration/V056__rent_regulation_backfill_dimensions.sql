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
