-- Add iDEAL/Wero and Zelle payment methods
ALTER TABLE payment_instructions
DROP CONSTRAINT chk_pi_payment_method;

ALTER TABLE payment_instructions
ADD CONSTRAINT chk_pi_payment_method CHECK (
    payment_method IN (
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
