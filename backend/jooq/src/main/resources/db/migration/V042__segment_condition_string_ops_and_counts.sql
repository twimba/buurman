-- Expand operators: add string operators (contains, not_contains, starts_with, ends_with)
ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_operator_check;

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
        'lte',
        'contains',
        'not_contains',
        'starts_with',
        'ends_with'
    )
);

-- Expand attributes: add entity count attributes
ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_attribute_check;

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
        'team_age_days',
        'contract_count',
        'contact_count',
        'photo_count',
        'document_count',
        'expense_count',
        'payment_count',
        'calendar_feed_count'
    )
);
