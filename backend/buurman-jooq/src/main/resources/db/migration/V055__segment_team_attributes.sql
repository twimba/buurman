-- Add team-level segment condition attributes:
-- team_name, team_admin_email, team_owner_email,
-- team_currency, team_default_country, team_timezone
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
        'is_user',
        'team_name',
        'team_admin_email',
        'team_owner_email',
        'team_currency',
        'team_default_country',
        'team_timezone'
    )
);
