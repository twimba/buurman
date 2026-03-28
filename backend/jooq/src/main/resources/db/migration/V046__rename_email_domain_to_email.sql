-- Rename email_domain attribute to email (matches full email, not just domain)
UPDATE segment_conditions
SET
    attribute = 'email'
WHERE
    attribute = 'email_domain';

-- Update CHECK constraint
ALTER TABLE segment_conditions
DROP CONSTRAINT IF EXISTS segment_conditions_attribute_check;

ALTER TABLE segment_conditions
ADD CONSTRAINT segment_conditions_attribute_check CHECK (
    attribute IN (
        'is_demo',
        'is_owner',
        'role',
        'email',
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
        'calendar_feed_count',
        'is_team',
        'is_user'
    )
);
