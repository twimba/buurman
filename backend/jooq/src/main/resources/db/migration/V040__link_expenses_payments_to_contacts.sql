-- V040: Add optional contact_id to expenses and payments tables
-- Allows explicitly linking expenses and payments to contacts
ALTER TABLE expenses
ADD COLUMN contact_id UUID REFERENCES contacts (id);

CREATE INDEX idx_expenses_contact_id ON expenses (contact_id)
WHERE
    contact_id IS NOT NULL;

ALTER TABLE payments
ADD COLUMN contact_id UUID REFERENCES contacts (id);

CREATE INDEX idx_payments_contact_id ON payments (contact_id)
WHERE
    contact_id IS NOT NULL;

-- Allow querying expenses/payments by contact across a team
CREATE INDEX idx_expenses_team_contact ON expenses (team_id, contact_id)
WHERE
    contact_id IS NOT NULL
    AND deleted_at IS NULL;

CREATE INDEX idx_payments_team_contact ON payments (team_id, contact_id)
WHERE
    contact_id IS NOT NULL
    AND deleted_at IS NULL;
