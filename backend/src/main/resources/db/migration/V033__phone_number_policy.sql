CREATE TABLE phone_number_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    allowed_countries JSONB NOT NULL DEFAULT '[]'::JSONB,
    blocked_countries JSONB NOT NULL DEFAULT '[]'::JSONB,
    allowed_number_types JSONB NOT NULL DEFAULT '[
      "MOBILE",
      "FIXED_LINE",
      "FIXED_LINE_OR_MOBILE"
    ]'::JSONB,
    country_mode VARCHAR(20) NOT NULL DEFAULT 'BLOCK_LIST' CHECK (country_mode IN ('ALLOW_LIST', 'BLOCK_LIST')),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255)
);

-- Insert default policy row (singleton)
INSERT INTO
    phone_number_policy (id)
VALUES
    (gen_random_uuid());
