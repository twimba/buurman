-- Add regex operator to segment conditions
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
        'ends_with',
        'regex'
    )
);
