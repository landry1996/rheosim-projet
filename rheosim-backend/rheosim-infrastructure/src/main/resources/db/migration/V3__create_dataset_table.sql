-- V3: Datasets table for experimental data
-- RheoSim Enterprise

CREATE TABLE rheosim.datasets (
    id                  UUID PRIMARY KEY,
    file_name           VARCHAR(500) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    content_type        VARCHAR(50) NOT NULL,
    project_id          UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    uploaded_by         UUID NOT NULL REFERENCES rheosim.users(id),
    experiment_type     VARCHAR(30) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    columns_metadata    JSONB,
    row_count           INT DEFAULT 0,
    temperature         DOUBLE PRECISION DEFAULT 25.0,
    temperature_unit    VARCHAR(10) DEFAULT '°C',
    notes               VARCHAR(1000),
    validation_errors   JSONB,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_datasets_project_id ON rheosim.datasets(project_id);
CREATE INDEX idx_datasets_status ON rheosim.datasets(status);
CREATE INDEX idx_datasets_experiment_type ON rheosim.datasets(experiment_type);
CREATE INDEX idx_datasets_uploaded_by ON rheosim.datasets(uploaded_by);
