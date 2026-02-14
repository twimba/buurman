CREATE TABLE job_execution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(200) NOT NULL,
    job_group VARCHAR(200) NOT NULL,
    trigger_name VARCHAR(200),
    trigger_group VARCHAR(200),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    error_message TEXT,
    node_id VARCHAR(200),
    CONSTRAINT chk_exec_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_job_exec_job_name ON job_execution_history (job_name);
CREATE INDEX idx_job_exec_started_at ON job_execution_history (started_at DESC);
CREATE INDEX idx_job_exec_status ON job_execution_history (status);
