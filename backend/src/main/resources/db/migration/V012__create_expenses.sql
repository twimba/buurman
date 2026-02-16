CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    -- Expense details
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    expense_date DATE NOT NULL,
    -- Description
    description TEXT NOT NULL,
    notes TEXT,
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users (id),
    updated_by UUID NOT NULL REFERENCES users (id),
    deleted_at TIMESTAMP,
    -- Constraints
    CONSTRAINT uq_expenses_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_expenses_amount CHECK (amount > 0),
    CONSTRAINT chk_expenses_category CHECK (
        category IN (
            'MAINTENANCE',
            'REPAIR',
            'UTILITY',
            'TAX',
            'INSURANCE',
            'LEGAL',
            'MARKETING',
            'CLEANING',
            'LANDSCAPING',
            'PROPERTY_MANAGEMENT',
            'OTHER'
        )
    )
);

CREATE INDEX idx_expenses_team_id ON expenses (team_id);

CREATE INDEX idx_expenses_property_id ON expenses (property_id);

CREATE INDEX idx_expenses_category ON expenses (category);

CREATE INDEX idx_expenses_expense_date ON expenses (expense_date);

CREATE INDEX idx_expenses_deleted_at ON expenses (deleted_at);

CREATE INDEX idx_expenses_reporting ON expenses (team_id, expense_date, category)
WHERE
    deleted_at IS NULL;
