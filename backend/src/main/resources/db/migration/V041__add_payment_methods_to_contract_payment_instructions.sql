-- Add iDEAL/Wero and Zelle to contract payment instructions custom method constraint
ALTER TABLE contract_payment_instructions
DROP CONSTRAINT chk_cpi_custom_method;

ALTER TABLE contract_payment_instructions
ADD CONSTRAINT chk_cpi_custom_method CHECK (
    custom_payment_method IS NULL
    OR custom_payment_method IN (
        'BANK_TRANSFER',
        'PAYPAL',
        'CASH',
        'CHECK',
        'DIRECT_DEBIT',
        'IDEAL_WERO',
        'ZELLE',
        'OTHER'
    )
);
