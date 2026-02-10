-- Normalize existing tenant phone numbers: NULL out invalid E.164 values
UPDATE tenants SET phone = NULL
    WHERE phone IS NOT NULL AND phone !~ '^\+[1-9]\d{1,14}$';

-- Add E.164 constraint to tenant phone
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_phone_e164
    CHECK (phone IS NULL OR phone ~ '^\+[1-9]\d{1,14}$');
