CREATE TABLE generated_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(29) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams (id),
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL,
    parameters JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    file_key VARCHAR(500),
    error TEXT,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES users (id),
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_reports_team_identifier UNIQUE (team_id, identifier),
    CONSTRAINT chk_reports_progress CHECK (
        progress >= 0
        AND progress <= 100
    ),
    CONSTRAINT chk_reports_type CHECK (
        report_type IN (
            'INCOME_STATEMENT',
            'EXPENSE_REPORT',
            'PROPERTY_REPORT',
            'TAX_SUMMARY',
            'TRANSACTION_HISTORY'
        )
    ),
    CONSTRAINT chk_reports_format CHECK (format IN ('PDF', 'EXCEL', 'CSV')),
    CONSTRAINT chk_reports_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX idx_reports_team ON generated_reports (team_id)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_reports_status ON generated_reports (status)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_reports_expires ON generated_reports (expires_at)
WHERE
    status = 'COMPLETED'
    AND deleted_at IS NULL;

CREATE INDEX idx_reports_created_at ON generated_reports (team_id, created_at)
WHERE
    deleted_at IS NULL;
