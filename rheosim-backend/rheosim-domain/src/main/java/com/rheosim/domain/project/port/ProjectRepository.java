package com.rheosim.domain.project.port;

import com.rheosim.domain.project.model.Project;
import com.rheosim.domain.project.model.ProjectStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    List<Project> findByOwnerId(UUID ownerId);

    List<Project> findByOwnerIdAndStatus(UUID ownerId, ProjectStatus status);

    void deleteById(UUID id);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
