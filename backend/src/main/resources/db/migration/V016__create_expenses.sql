-- Create expenses table for property expense tracking
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    property_id UUID NOT NULL REFERENCES properties(id),

    -- Expense details
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    expense_date DATE NOT NULL,

    -- Description
    description TEXT NOT NULL,
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT unique_expense_identifier UNIQUE(team_id, identifier),
    CONSTRAINT positive_expense_amount CHECK (amount > 0),
    CONSTRAINT valid_category CHECK (category IN (
        'MAINTENANCE', 'REPAIR', 'UTILITY', 'TAX', 'INSURANCE',
        'LEGAL', 'MARKETING', 'CLEANING', 'LANDSCAPING',
        'PROPERTY_MANAGEMENT', 'OTHER'
    ))
);

-- Create indexes for performance
CREATE INDEX idx_expenses_team_id ON expenses(team_id);
CREATE INDEX idx_expenses_property_id ON expenses(property_id);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX idx_expenses_deleted_at ON expenses(deleted_at);

-- Index for financial reporting
CREATE INDEX idx_expenses_reporting ON expenses(team_id, expense_date, category)
WHERE deleted_at IS NULL;

-- Comments for documentation
COMMENT ON TABLE expenses IS 'Property expenses and costs';
COMMENT ON COLUMN expenses.identifier IS 'ULID identifier for external use';
COMMENT ON COLUMN expenses.category IS 'Expense category (MAINTENANCE, REPAIR, UTILITY, etc.)';
COMMENT ON COLUMN expenses.expense_date IS 'Date the expense occurred';
