-- V2: Project & Material tables
-- RheoSim Enterprise

-- Projects table
CREATE TABLE rheosim.projects (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id    UUID NOT NULL REFERENCES rheosim.users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_owner_id ON rheosim.projects(owner_id);
CREATE INDEX idx_projects_status ON rheosim.projects(status);
CREATE UNIQUE INDEX idx_projects_name_owner ON rheosim.projects(name, owner_id);

-- Materials table
CREATE TABLE rheosim.materials (
    id           UUID PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    grade        VARCHAR(50),
    family       VARCHAR(30) NOT NULL,
    supplier     VARCHAR(100),
    notes        VARCHAR(1000),
    project_id   UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    model_type   VARCHAR(30),
    model_data   JSONB,
    version      INT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_materials_project_id ON rheosim.materials(project_id);
CREATE INDEX idx_materials_family ON rheosim.materials(family);
CREATE INDEX idx_materials_model_type ON rheosim.materials(model_type);

-- Project-Material association (for quick lookup of material IDs per project)
CREATE TABLE rheosim.project_materials (
    project_id  UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES rheosim.materials(id) ON DELETE CASCADE,
    added_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, material_id)
);
