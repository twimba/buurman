-- BUUR-93 §11 — backfill source_url on historical 2022-2025 rows where V029 left it NULL.
-- Only updates rows still missing source_url to avoid clobbering URLs added by V053/V054.
-- Denmark — DST nettoprisindeks for all historical rows missing URL
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.dst.dk/da/Statistik/emner/oekonomi/prisindeks/nettoprisindeks',
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
    AND source_url IS NULL;

-- Sweden — Hyresgästföreningen
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.hyresgastforeningen.se/om-oss/vad-vi-gor/hyresforhandling/',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'SE'
    )
    AND source_url IS NULL;

-- Norway — SSB konsumprisindeksen
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.ssb.no/priser-og-prisindekser/konsumpriser/statistikk/konsumprisindeksen',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'NO'
    )
    AND source_url IS NULL;

-- Finland — Tilastokeskus
UPDATE rent_regulation_rules
SET
    source_url = 'https://stat.fi/vuokran-tarkistaminen-elinkustannusindeksilla',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'FI'
    )
    AND source_url IS NULL;

-- Italy — Confedilizia ISTAT FOI tables
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.confedilizia.it/locazioni/indice-istat/',
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
    AND source_url IS NULL;

-- Austria — Statistik Austria Richtwerte
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.statistik.at/statistiken/bevoelkerung-und-soziales/wohnen/richtwerte-und-kategoriebetraege',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'AT'
    )
    AND source_url IS NULL;

-- Switzerland — BWO Referenzzinssatz
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.bwo.admin.ch/de/referenzzinssatz',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CH'
    )
    AND source_url IS NULL;

-- Poland — Ustawa o ochronie praw lokatorów
UPDATE rent_regulation_rules
SET
    source_url = 'https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20010710733',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'PL'
    )
    AND source_url IS NULL;

-- Czech Republic — Občanský zákoník
UPDATE rent_regulation_rules
SET
    source_url = 'https://www.zakonyprolidi.cz/cs/2012-89',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'CZ'
    )
    AND source_url IS NULL;

-- Luxembourg — Logement.lu FAQ
UPDATE rent_regulation_rules
SET
    source_url = 'https://logement.public.lu/fr/proprietaire/logement-location/faq-bail-a-loyer.html',
    updated_at = now()
WHERE
    country_id = (
        SELECT
            id
        FROM
            rent_regulation_countries
        WHERE
            country_code = 'LU'
    )
    AND source_url IS NULL;
