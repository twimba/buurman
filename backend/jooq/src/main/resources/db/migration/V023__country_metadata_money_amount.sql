-- V023: Convert country metadata monetary fields from plain decimals to {value, currency} objects.
-- Values are stored in minor units (e.g. 25000 = EUR 250.00) for consistency with BIGINT columns.
-- Currency is sourced from the contract's rent_amount_currency, falling back to 'EUR'.
-- Helper function to transform a single monetary field in JSONB from a plain number to a
-- {value, currency} minor-units object. Only transforms if the field exists and is a number
-- (idempotent — already-migrated objects are skipped).
CREATE OR REPLACE FUNCTION _migrate_metadata_money_field (metadata JSONB, field_name TEXT, currency TEXT) RETURNS JSONB AS $$
BEGIN
    IF metadata ? field_name
       AND jsonb_typeof(metadata -> field_name) = 'number' THEN
        RETURN metadata || jsonb_build_object(
            field_name,
            jsonb_build_object(
                'value', ((metadata ->> field_name)::numeric * 100)::bigint,
                'currency', currency
            )
        );
    END IF;
    RETURN metadata;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Migrate all countries in a single pass using the helper function.
UPDATE contracts
SET
    country_metadata = (
        SELECT
            result
        FROM
            (
                SELECT
                    CASE country_code
                        -- NL
                        WHEN 'NL' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'totalServiceCostsAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'liberalizationThreshold',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- DE
                        WHEN 'DE' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'nebenkostenAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'kautionAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- FR
                        WHEN 'FR' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                _migrate_metadata_money_field (
                                    country_metadata,
                                    'referenceRentPrice',
                                    coalesce(rent_amount_currency, 'EUR')
                                ),
                                'maxRentPrice',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'cautionAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- ES
                        WHEN 'ES' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'fianzaAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'garantiaAdicionalAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- IT
                        WHEN 'IT' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'depositoAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- GB
                        WHEN 'GB' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'depositAmount',
                            coalesce(rent_amount_currency, 'GBP')
                        )
                        -- US
                        WHEN 'US' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'securityDepositLimit',
                            coalesce(rent_amount_currency, 'USD')
                        )
                        -- AT
                        WHEN 'AT' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                _migrate_metadata_money_field (
                                    country_metadata,
                                    'betriebskostenAmount',
                                    coalesce(rent_amount_currency, 'EUR')
                                ),
                                'kautionAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'richtwertmiete',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- CH
                        WHEN 'CH' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'nebenkostenAmount',
                                coalesce(rent_amount_currency, 'CHF')
                            ),
                            'kautionAmount',
                            coalesce(rent_amount_currency, 'CHF')
                        )
                        -- DK
                        WHEN 'DK' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositumAmount',
                                coalesce(rent_amount_currency, 'DKK')
                            ),
                            'forudbetalingAmount',
                            coalesce(rent_amount_currency, 'DKK')
                        )
                        -- SE
                        WHEN 'SE' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'depositAmount',
                            coalesce(rent_amount_currency, 'SEK')
                        )
                        -- FI
                        WHEN 'FI' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'vakuusAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- NO
                        WHEN 'NO' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'depositumskontoAmount',
                            coalesce(rent_amount_currency, 'NOK')
                        )
                        -- IE
                        WHEN 'IE' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'marketRentAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- PL
                        WHEN 'PL' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'kaucjaAmount',
                            coalesce(rent_amount_currency, 'PLN')
                        )
                        -- CZ
                        WHEN 'CZ' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'kauceAmount',
                                coalesce(rent_amount_currency, 'CZK')
                            ),
                            'sluzbyAmount',
                            coalesce(rent_amount_currency, 'CZK')
                        )
                        -- HU
                        WHEN 'HU' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'kaucioAmount',
                                coalesce(rent_amount_currency, 'HUF')
                            ),
                            'kozosKoltsegAmount',
                            coalesce(rent_amount_currency, 'HUF')
                        )
                        -- RO
                        WHEN 'RO' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'garantieAmount',
                                coalesce(rent_amount_currency, 'RON')
                            ),
                            'intretinereAmount',
                            coalesce(rent_amount_currency, 'RON')
                        )
                        -- BG
                        WHEN 'BG' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'BGN')
                            ),
                            'obshtiRazhodiAmount',
                            coalesce(rent_amount_currency, 'BGN')
                        )
                        -- SK
                        WHEN 'SK' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'kauciaAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'poplatkyAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- SI
                        WHEN 'SI' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'varscinsAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'rezervniFondAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- HR
                        WHEN 'HR' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'jamcevinaAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'pricuvaAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- LT
                        WHEN 'LT' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'uzstatasAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'komunaliniaiAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- LV
                        WHEN 'LV' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'drosibaNaudaAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'komunalieAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- EE
                        WHEN 'EE' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'tagatisrahaAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'kommunaalkuludAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- GR
                        WHEN 'GR' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'eggysisAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'koinochristaAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- MT
                        WHEN 'MT' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'groundRent',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- CY
                        WHEN 'CY' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'commonExpensesAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- LU
                        WHEN 'LU' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'cautionAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'chargesAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- RS
                        WHEN 'RS' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'RSD')
                            ),
                            'komunalniTroskoviAmount',
                            coalesce(rent_amount_currency, 'RSD')
                        )
                        -- BA
                        WHEN 'BA' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'BAM')
                            ),
                            'rezijeAmount',
                            coalesce(rent_amount_currency, 'BAM')
                        )
                        -- AL
                        WHEN 'AL' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'garanciaAmount',
                                coalesce(rent_amount_currency, 'ALL')
                            ),
                            'shpenzimet',
                            coalesce(rent_amount_currency, 'ALL')
                        )
                        -- ME
                        WHEN 'ME' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'komunalijeAmount',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- MK
                        WHEN 'MK' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'MKD')
                            ),
                            'rezhiskiTroskoviAmount',
                            coalesce(rent_amount_currency, 'MKD')
                        )
                        -- XK
                        WHEN 'XK' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depozitAmount',
                                coalesce(rent_amount_currency, 'EUR')
                            ),
                            'shpenzimetKomunale',
                            coalesce(rent_amount_currency, 'EUR')
                        )
                        -- CA
                        WHEN 'CA' THEN _migrate_metadata_money_field (
                            country_metadata,
                            'securityDepositAmount',
                            coalesce(rent_amount_currency, 'CAD')
                        )
                        -- MX
                        WHEN 'MX' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositoAmount',
                                coalesce(rent_amount_currency, 'MXN')
                            ),
                            'mantenimientoAmount',
                            coalesce(rent_amount_currency, 'MXN')
                        )
                        -- BR
                        WHEN 'BR' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'caucaoAmount',
                                coalesce(rent_amount_currency, 'BRL')
                            ),
                            'condominioAmount',
                            coalesce(rent_amount_currency, 'BRL')
                        )
                        -- AR
                        WHEN 'AR' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositoAmount',
                                coalesce(rent_amount_currency, 'ARS')
                            ),
                            'expensasAmount',
                            coalesce(rent_amount_currency, 'ARS')
                        )
                        -- CL
                        WHEN 'CL' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'garantiaAmount',
                                coalesce(rent_amount_currency, 'CLP')
                            ),
                            'gastosComunes',
                            coalesce(rent_amount_currency, 'CLP')
                        )
                        -- CO
                        WHEN 'CO' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositoAmount',
                                coalesce(rent_amount_currency, 'COP')
                            ),
                            'administracionAmount',
                            coalesce(rent_amount_currency, 'COP')
                        )
                        -- PE
                        WHEN 'PE' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'garantiaAmount',
                                coalesce(rent_amount_currency, 'PEN')
                            ),
                            'mantenimientoAmount',
                            coalesce(rent_amount_currency, 'PEN')
                        )
                        -- UY
                        WHEN 'UY' THEN _migrate_metadata_money_field (
                            _migrate_metadata_money_field (
                                country_metadata,
                                'depositoAmount',
                                coalesce(rent_amount_currency, 'UYU')
                            ),
                            'gastosComunes',
                            coalesce(rent_amount_currency, 'UYU')
                        )
                        -- Generic fallback and any unknown country
                        ELSE country_metadata
                    END AS result
            ) sub
    )
WHERE
    country_metadata IS NOT NULL
    AND deleted_at IS NULL;

-- Clean up the helper function
DROP FUNCTION _migrate_metadata_money_field (JSONB, TEXT, TEXT);
