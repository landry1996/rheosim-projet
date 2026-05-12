-- V6: Collaboration & Multi-tenant tables

CREATE TABLE rheosim.organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    owner_user_id UUID NOT NULL REFERENCES rheosim.users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON rheosim.organizations(slug);
CREATE INDEX idx_organizations_owner ON rheosim.organizations(owner_user_id);

CREATE TABLE rheosim.organization_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES rheosim.organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES rheosim.users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, user_id)
);

CREATE INDEX idx_org_members_org ON rheosim.organization_members(organization_id);
CREATE INDEX idx_org_members_user ON rheosim.organization_members(user_id);

CREATE TABLE rheosim.invitations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES rheosim.organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by_user_id UUID NOT NULL REFERENCES rheosim.users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_invitations_email ON rheosim.invitations(email);
CREATE INDEX idx_invitations_org ON rheosim.invitations(organization_id);

CREATE TABLE rheosim.project_collaborators (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES rheosim.projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES rheosim.users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);

CREATE INDEX idx_project_collabs_project ON rheosim.project_collaborators(project_id);
CREATE INDEX idx_project_collabs_user ON rheosim.project_collaborators(user_id);

CREATE TABLE rheosim.audit_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES rheosim.organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES rheosim.users(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    details TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_org ON rheosim.audit_events(organization_id);
CREATE INDEX idx_audit_user ON rheosim.audit_events(user_id);
CREATE INDEX idx_audit_occurred ON rheosim.audit_events(occurred_at);

-- Add organization_id to projects for multi-tenant filtering
ALTER TABLE rheosim.projects ADD COLUMN organization_id UUID REFERENCES rheosim.organizations(id);
CREATE INDEX idx_projects_org ON rheosim.projects(organization_id);
