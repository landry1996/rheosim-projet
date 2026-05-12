package com.rheosim.application.collaboration;

import com.rheosim.domain.collaboration.model.AuditEvent;
import com.rheosim.domain.collaboration.model.ProjectCollaborator;
import com.rheosim.domain.collaboration.model.ProjectRole;
import com.rheosim.domain.collaboration.port.AuditEventRepository;
import com.rheosim.domain.collaboration.port.ProjectCollaboratorRepository;

import java.util.List;
import java.util.UUID;

public class ProjectCollaborationUseCase {

    private final ProjectCollaboratorRepository collaboratorRepository;
    private final AuditEventRepository auditRepository;

    public ProjectCollaborationUseCase(ProjectCollaboratorRepository collaboratorRepository,
                                        AuditEventRepository auditRepository) {
        this.collaboratorRepository = collaboratorRepository;
        this.auditRepository = auditRepository;
    }

    public ProjectCollaborator addCollaborator(UUID projectId, UUID userId, ProjectRole role, UUID addedByUserId, UUID orgId) {
        if (collaboratorRepository.findByProjectIdAndUserId(projectId, userId).isPresent()) {
            throw new IllegalStateException("User is already a collaborator on this project");
        }

        ProjectCollaborator collaborator = ProjectCollaborator.create(projectId, userId, role);
        collaborator = collaboratorRepository.save(collaborator);

        auditRepository.save(AuditEvent.create(orgId, addedByUserId, "ADD_COLLABORATOR", "PROJECT", projectId, "Added collaborator with role " + role));

        return collaborator;
    }

    public void removeCollaborator(UUID projectId, UUID userId, UUID removedByUserId, UUID orgId) {
        collaboratorRepository.deleteByProjectIdAndUserId(projectId, userId);
        auditRepository.save(AuditEvent.create(orgId, removedByUserId, "REMOVE_COLLABORATOR", "PROJECT", projectId, "Removed collaborator"));
    }

    public List<ProjectCollaborator> getCollaborators(UUID projectId) {
        return collaboratorRepository.findByProjectId(projectId);
    }

    public boolean hasAccess(UUID projectId, UUID userId, ProjectRole minimumRole) {
        return collaboratorRepository.hasAccess(projectId, userId, minimumRole);
    }

    public void updateRole(UUID projectId, UUID userId, ProjectRole newRole, UUID updatedByUserId, UUID orgId) {
        ProjectCollaborator existing = collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));

        ProjectCollaborator updated = new ProjectCollaborator(existing.id(), projectId, userId, newRole, existing.addedAt());
        collaboratorRepository.save(updated);

        auditRepository.save(AuditEvent.create(orgId, updatedByUserId, "UPDATE_ROLE", "PROJECT", projectId, "Role changed to " + newRole));
    }
}
