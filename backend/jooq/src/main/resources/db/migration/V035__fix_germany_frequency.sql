-- V034 set Germany's frequency to TRIENNIAL which doesn't exist in the RentFrequency enum.
-- Revert to ANNUAL (increases happen annually, capped at 20% over 3 years).
-- Document the 3-year rolling cap in additional_conditions instead.
UPDATE rent_regulation_rules
SET
    frequency = 'ANNUAL',
    additional_conditions = 'Kappungsgrenze: total increases capped at 20% over any rolling 3-year period (15% in tight housing markets).'
WHERE
    identifier IN (
        'RRL01J00000000000000000011',
        'RRL01J00000000000000000012',
        'RRL01J00000000000000000013',
        'RRL01J00000000000000000014',
        'RRL01J00000000000000000015'
    );
