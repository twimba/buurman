-- Phase 4: Financial Reporting & Analytics
-- Generated Reports table for async report generation tracking

CREATE TABLE generated_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(26) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id),
    report_type VARCHAR(50) NOT NULL,
    format VARCHAR(10) NOT NULL,
    parameters JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    file_key VARCHAR(500),
    error TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by UUID REFERENCES users(id),
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(team_id, identifier),
    CHECK (progress >= 0 AND progress <= 100),
    CHECK (report_type IN ('INCOME_STATEMENT', 'EXPENSE_REPORT', 'PROPERTY_REPORT', 'TAX_SUMMARY', 'TRANSACTION_HISTORY')),
    CHECK (format IN ('PDF', 'EXCEL', 'CSV')),
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_reports_team ON generated_reports(team_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reports_status ON generated_reports(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reports_expires ON generated_reports(expires_at) WHERE status = 'COMPLETED' AND deleted_at IS NULL;
CREATE INDEX idx_reports_created_at ON generated_reports(team_id, created_at) WHERE deleted_at IS NULL;

COMMENT ON TABLE generated_reports IS 'Stores metadata for asynchronously generated financial reports';
COMMENT ON COLUMN generated_reports.identifier IS 'ULID identifier for user-facing report ID';
COMMENT ON COLUMN generated_reports.parameters IS 'JSON parameters used to generate the report (date range, filters, etc.)';
COMMENT ON COLUMN generated_reports.progress IS 'Report generation progress percentage (0-100)';
COMMENT ON COLUMN generated_reports.file_key IS 'S3 key where the generated report file is stored';
COMMENT ON COLUMN generated_reports.expires_at IS 'Timestamp when the report download link expires';
