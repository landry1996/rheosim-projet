package com.rheosim.domain.collaboration.port;

import com.rheosim.domain.collaboration.model.ProjectCollaborator;
import com.rheosim.domain.collaboration.model.ProjectRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectCollaboratorRepository {
    ProjectCollaborator save(ProjectCollaborator collaborator);
    List<ProjectCollaborator> findByProjectId(UUID projectId);
    List<ProjectCollaborator> findByUserId(UUID userId);
    Optional<ProjectCollaborator> findByProjectIdAndUserId(UUID projectId, UUID userId);
    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);
    boolean hasAccess(UUID projectId, UUID userId, ProjectRole minimumRole);
}
