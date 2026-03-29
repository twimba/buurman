-- Expand segment condition attributes and operators
-- Drop existing unnamed CHECK constraints and recreate with new values.
-- PostgreSQL auto-names inline CHECKs as <table>_check, <table>_check1, etc.
ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_check;

ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_check1;

-- Also try the column-named variants in case PostgreSQL used that pattern
ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_attribute_check;

ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_operator_check;

ALTER TABLE segment_conditions
ADD CONSTRAINT segment_conditions_attribute_check CHECK (
    attribute IN (
        'is_demo',
        'is_owner',
        'role',
        'email_domain',
        'email_verified',
        'property_count',
        'member_count',
        'team_age_days'
    )
);

ALTER TABLE segment_conditions
ADD CONSTRAINT segment_conditions_operator_check CHECK (
    operator IN (
        'eq',
        'neq',
        'in',
        'not_in',
        'gt',
        'gte',
        'lt',
        'lte'
    )
);
