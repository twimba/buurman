-- Add explicit currency columns to payment_receivals and contract_rent_periods
-- so they are self-contained and don't rely on transitive currency from parent entities.
-- payment_receivals: currency from payment → contract
ALTER TABLE payment_receivals
ADD COLUMN currency VARCHAR(3);

UPDATE payment_receivals pr
SET
    currency = coalesce(
        (
            SELECT
                p.currency
            FROM
                payments p
            WHERE
                p.id = pr.payment_id
        ),
        'EUR'
    );

ALTER TABLE payment_receivals
ALTER COLUMN currency
SET NOT NULL;

ALTER TABLE payment_receivals
ALTER COLUMN currency
SET DEFAULT 'EUR';

-- contract_rent_periods: currency from contract
ALTER TABLE contract_rent_periods
ADD COLUMN currency VARCHAR(3);

UPDATE contract_rent_periods crp
SET
    currency = coalesce(
        (
            SELECT
                c.currency
            FROM
                contracts c
            WHERE
                c.id = crp.contract_id
        ),
        'EUR'
    );

ALTER TABLE contract_rent_periods
ALTER COLUMN currency
SET NOT NULL;

ALTER TABLE contract_rent_periods
ALTER COLUMN currency
SET DEFAULT 'EUR';
