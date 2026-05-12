-- V1: Identity & Access Management tables
-- RheoSim Enterprise

CREATE SCHEMA IF NOT EXISTS rheosim;

-- Roles table
CREATE TABLE rheosim.roles (
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Users table
CREATE TABLE rheosim.users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(50) NOT NULL,
    last_name     VARCHAR(50) NOT NULL,
    organization  VARCHAR(100),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    locked        BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- User-Role junction table
CREATE TABLE rheosim.user_roles (
    user_id UUID NOT NULL REFERENCES rheosim.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES rheosim.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens table
CREATE TABLE rheosim.refresh_tokens (
    id         UUID PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    UUID NOT NULL REFERENCES rheosim.users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_email ON rheosim.users(email);
CREATE INDEX idx_refresh_tokens_user_id ON rheosim.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON rheosim.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON rheosim.refresh_tokens(expires_at);

-- Audit log table
CREATE TABLE rheosim.audit_logs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(100),
    entity_id  UUID,
    details    JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON rheosim.audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON rheosim.audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON rheosim.audit_logs(created_at);

-- Seed default roles
INSERT INTO rheosim.roles (id, name, description) VALUES
    (gen_random_uuid(), 'ADMIN', 'Platform administrator with full access'),
    (gen_random_uuid(), 'RESEARCHER', 'Academic researcher with advanced model access'),
    (gen_random_uuid(), 'ENGINEER', 'R&D engineer with standard simulation access'),
    (gen_random_uuid(), 'STUDENT', 'Student with educational mode access');
