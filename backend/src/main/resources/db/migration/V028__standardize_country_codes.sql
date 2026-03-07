-- Standardize all country fields to use ISO 3166-1 alpha-2 codes instead of full names
-- and rename columns to "country_code" for clarity
-- team_preferences: default_country -> default_country_code
ALTER TABLE team_preferences
RENAME COLUMN default_country TO default_country_code;

ALTER TABLE team_preferences
ALTER COLUMN default_country_code
SET DEFAULT 'NL';

UPDATE team_preferences
SET
    default_country_code = CASE default_country_code
        WHEN 'Netherlands' THEN 'NL'
        WHEN 'Germany' THEN 'DE'
        WHEN 'France' THEN 'FR'
        WHEN 'Belgium' THEN 'BE'
        WHEN 'Portugal' THEN 'PT'
        WHEN 'Spain' THEN 'ES'
        WHEN 'Italy' THEN 'IT'
        WHEN 'United Kingdom' THEN 'GB'
        WHEN 'United States' THEN 'US'
        WHEN 'Austria' THEN 'AT'
        WHEN 'Switzerland' THEN 'CH'
        WHEN 'Denmark' THEN 'DK'
        WHEN 'Sweden' THEN 'SE'
        WHEN 'Finland' THEN 'FI'
        WHEN 'Norway' THEN 'NO'
        WHEN 'Ireland' THEN 'IE'
        WHEN 'Poland' THEN 'PL'
        WHEN 'Czech Republic' THEN 'CZ'
        WHEN 'Hungary' THEN 'HU'
        WHEN 'Romania' THEN 'RO'
        WHEN 'Bulgaria' THEN 'BG'
        WHEN 'Slovakia' THEN 'SK'
        WHEN 'Slovenia' THEN 'SI'
        WHEN 'Croatia' THEN 'HR'
        WHEN 'Lithuania' THEN 'LT'
        WHEN 'Latvia' THEN 'LV'
        WHEN 'Estonia' THEN 'EE'
        WHEN 'Greece' THEN 'GR'
        WHEN 'Malta' THEN 'MT'
        WHEN 'Cyprus' THEN 'CY'
        WHEN 'Luxembourg' THEN 'LU'
        WHEN 'Canada' THEN 'CA'
        WHEN 'Mexico' THEN 'MX'
        WHEN 'Brazil' THEN 'BR'
        WHEN 'Argentina' THEN 'AR'
        WHEN 'Chile' THEN 'CL'
        WHEN 'Colombia' THEN 'CO'
        WHEN 'Peru' THEN 'PE'
        WHEN 'Uruguay' THEN 'UY'
        ELSE default_country_code
    END
WHERE
    length(default_country_code) > 2;

-- properties: country -> country_code
ALTER TABLE properties
RENAME COLUMN country TO country_code;

ALTER TABLE properties
ALTER COLUMN country_code
SET DEFAULT 'NL';

UPDATE properties
SET
    country_code = CASE country_code
        WHEN 'Netherlands' THEN 'NL'
        WHEN 'Germany' THEN 'DE'
        WHEN 'France' THEN 'FR'
        WHEN 'Belgium' THEN 'BE'
        WHEN 'Portugal' THEN 'PT'
        WHEN 'Spain' THEN 'ES'
        WHEN 'Italy' THEN 'IT'
        WHEN 'United Kingdom' THEN 'GB'
        WHEN 'United States' THEN 'US'
        WHEN 'Austria' THEN 'AT'
        WHEN 'Switzerland' THEN 'CH'
        WHEN 'Denmark' THEN 'DK'
        WHEN 'Sweden' THEN 'SE'
        WHEN 'Finland' THEN 'FI'
        WHEN 'Norway' THEN 'NO'
        WHEN 'Ireland' THEN 'IE'
        WHEN 'Poland' THEN 'PL'
        WHEN 'Czech Republic' THEN 'CZ'
        WHEN 'Hungary' THEN 'HU'
        WHEN 'Romania' THEN 'RO'
        WHEN 'Bulgaria' THEN 'BG'
        WHEN 'Slovakia' THEN 'SK'
        WHEN 'Slovenia' THEN 'SI'
        WHEN 'Croatia' THEN 'HR'
        WHEN 'Lithuania' THEN 'LT'
        WHEN 'Latvia' THEN 'LV'
        WHEN 'Estonia' THEN 'EE'
        WHEN 'Greece' THEN 'GR'
        WHEN 'Malta' THEN 'MT'
        WHEN 'Cyprus' THEN 'CY'
        WHEN 'Luxembourg' THEN 'LU'
        WHEN 'Canada' THEN 'CA'
        WHEN 'Mexico' THEN 'MX'
        WHEN 'Brazil' THEN 'BR'
        WHEN 'Argentina' THEN 'AR'
        WHEN 'Chile' THEN 'CL'
        WHEN 'Colombia' THEN 'CO'
        WHEN 'Peru' THEN 'PE'
        WHEN 'Uruguay' THEN 'UY'
        ELSE country_code
    END
WHERE
    length(country_code) > 2;

-- tenant_addresses: country -> country_code
ALTER TABLE tenant_addresses
RENAME COLUMN country TO country_code;

ALTER TABLE tenant_addresses
ALTER COLUMN country_code
SET DEFAULT 'NL';

UPDATE tenant_addresses
SET
    country_code = CASE country_code
        WHEN 'Netherlands' THEN 'NL'
        WHEN 'Germany' THEN 'DE'
        WHEN 'France' THEN 'FR'
        WHEN 'Belgium' THEN 'BE'
        WHEN 'Portugal' THEN 'PT'
        WHEN 'Spain' THEN 'ES'
        WHEN 'Italy' THEN 'IT'
        WHEN 'United Kingdom' THEN 'GB'
        WHEN 'United States' THEN 'US'
        WHEN 'Austria' THEN 'AT'
        WHEN 'Switzerland' THEN 'CH'
        WHEN 'Denmark' THEN 'DK'
        WHEN 'Sweden' THEN 'SE'
        WHEN 'Finland' THEN 'FI'
        WHEN 'Norway' THEN 'NO'
        WHEN 'Ireland' THEN 'IE'
        WHEN 'Poland' THEN 'PL'
        WHEN 'Czech Republic' THEN 'CZ'
        WHEN 'Hungary' THEN 'HU'
        WHEN 'Romania' THEN 'RO'
        WHEN 'Bulgaria' THEN 'BG'
        WHEN 'Slovakia' THEN 'SK'
        WHEN 'Slovenia' THEN 'SI'
        WHEN 'Croatia' THEN 'HR'
        WHEN 'Lithuania' THEN 'LT'
        WHEN 'Latvia' THEN 'LV'
        WHEN 'Estonia' THEN 'EE'
        WHEN 'Greece' THEN 'GR'
        WHEN 'Malta' THEN 'MT'
        WHEN 'Cyprus' THEN 'CY'
        WHEN 'Luxembourg' THEN 'LU'
        WHEN 'Canada' THEN 'CA'
        WHEN 'Mexico' THEN 'MX'
        WHEN 'Brazil' THEN 'BR'
        WHEN 'Argentina' THEN 'AR'
        WHEN 'Chile' THEN 'CL'
        WHEN 'Colombia' THEN 'CO'
        WHEN 'Peru' THEN 'PE'
        WHEN 'Uruguay' THEN 'UY'
        ELSE country_code
    END
WHERE
    length(country_code) > 2;
