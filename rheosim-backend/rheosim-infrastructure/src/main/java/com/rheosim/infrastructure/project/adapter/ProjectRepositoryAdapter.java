package com.rheosim.infrastructure.project.adapter;

import com.rheosim.domain.project.model.Project;
import com.rheosim.domain.project.model.ProjectStatus;
import com.rheosim.domain.project.port.ProjectRepository;
import com.rheosim.infrastructure.project.entity.ProjectJpaEntity;
import com.rheosim.infrastructure.project.repository.ProjectJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;

    public ProjectRepositoryAdapter(ProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = toJpaEntity(project);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Project> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerId(ownerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Project> findByOwnerIdAndStatus(UUID ownerId, ProjectStatus status) {
        return jpaRepository.findByOwnerIdAndStatus(ownerId, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndOwnerId(String name, UUID ownerId) {
        return jpaRepository.existsByNameAndOwnerId(name, ownerId);
    }

    private ProjectJpaEntity toJpaEntity(Project project) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(project.getId());
        entity.setName(project.getName());
        entity.setDescription(project.getDescription());
        entity.setOwnerId(project.getOwnerId());
        entity.setStatus(project.getStatus().name());
        entity.setCreatedAt(project.getCreatedAt());
        entity.setUpdatedAt(project.getUpdatedAt());
        return entity;
    }

    private Project toDomain(ProjectJpaEntity entity) {
        Project project = Project.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .ownerId(entity.getOwnerId())
                .status(ProjectStatus.valueOf(entity.getStatus()))
                .materialIds(new ArrayList<>())
                .build();

        project.setId(entity.getId());
        project.setCreatedAt(entity.getCreatedAt());
        project.setUpdatedAt(entity.getUpdatedAt());
        return project;
    }
}
