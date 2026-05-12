package com.rheosim.application.project.usecase;

import com.rheosim.application.project.dto.CreateProjectRequest;
import com.rheosim.application.project.dto.ProjectResponse;
import com.rheosim.domain.project.model.Project;
import com.rheosim.domain.project.model.ProjectStatus;
import com.rheosim.domain.project.port.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectUseCase {

    private final ProjectRepository projectRepository;

    public ProjectUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        if (projectRepository.existsByNameAndOwnerId(request.name(), ownerId)) {
            throw new IllegalArgumentException("A project with this name already exists");
        }

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(ownerId)
                .status(ProjectStatus.ACTIVE)
                .build();

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied to project: " + projectId);
        }

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID ownerId) {
        return projectRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UUID ownerId, String name, String description) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied to project: " + projectId);
        }

        project.updateDetails(name, description);
        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public void archiveProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied to project: " + projectId);
        }

        project.archive();
        projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied to project: " + projectId);
        }

        projectRepository.deleteById(projectId);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus().name(),
                project.getOwnerId(),
                project.getMaterialIds(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
