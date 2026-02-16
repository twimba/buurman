-- Add phone number to users table (E.164 format, e.g. +31612345678)
ALTER TABLE users
ADD COLUMN phone VARCHAR(20);

-- CHECK constraint for E.164 format (+ followed by 1-15 digits)
ALTER TABLE users
ADD CONSTRAINT chk_users_phone_e164 CHECK (
    phone IS NULL
    OR phone ~ '^\+[1-9]\d{1,14}$'
);

CREATE INDEX idx_users_phone ON users (phone)
WHERE
    phone IS NOT NULL;
