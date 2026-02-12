ALTER TABLE phone_number_policy
    ADD COLUMN max_codes_per_hour INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN verification_code_expiry_minutes INTEGER NOT NULL DEFAULT 10;
