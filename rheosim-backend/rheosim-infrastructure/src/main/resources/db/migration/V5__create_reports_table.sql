-- V5: Reports table
-- RheoSim Enterprise

CREATE TABLE rheosim.reports (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES rheosim.simulation_jobs(id),
    generated_by    UUID NOT NULL REFERENCES rheosim.users(id),
    title           VARCHAR(200) NOT NULL,
    format          VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    file_path       VARCHAR(500),
    file_size       BIGINT DEFAULT 0,
    metadata        JSONB,
    error_message   VARCHAR(2000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_project_id ON rheosim.reports(project_id);
CREATE INDEX idx_reports_generated_by ON rheosim.reports(generated_by);
CREATE INDEX idx_reports_job_id ON rheosim.reports(job_id);
CREATE INDEX idx_reports_status ON rheosim.reports(status);
