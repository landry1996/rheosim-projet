-- V4: Simulation Jobs table
-- RheoSim Enterprise

CREATE TABLE rheosim.simulation_jobs (
    id                  UUID PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    dataset_id          UUID NOT NULL REFERENCES rheosim.datasets(id),
    material_id         UUID REFERENCES rheosim.materials(id),
    submitted_by        UUID NOT NULL REFERENCES rheosim.users(id),
    simulation_type     VARCHAR(30) NOT NULL,
    model_type          VARCHAR(30) NOT NULL,
    number_of_branches  INT DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress            DOUBLE PRECISION DEFAULT 0.0,
    result_data         JSONB,
    error_message       VARCHAR(2000),
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_simulation_jobs_project_id ON rheosim.simulation_jobs(project_id);
CREATE INDEX idx_simulation_jobs_status ON rheosim.simulation_jobs(status);
CREATE INDEX idx_simulation_jobs_submitted_by ON rheosim.simulation_jobs(submitted_by);
CREATE INDEX idx_simulation_jobs_dataset_id ON rheosim.simulation_jobs(dataset_id);
