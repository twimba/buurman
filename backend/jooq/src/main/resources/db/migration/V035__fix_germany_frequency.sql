-- V034 correctly identified Germany's Kappungsgrenze as TRIENNIAL, but the
-- RentFrequency enum didn't have that value yet. Now that TRIENNIAL has been
-- added to the enum, restore the correct frequency and add additional_conditions.
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
