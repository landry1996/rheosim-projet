package com.rheosim.infrastructure.project.repository;

import com.rheosim.infrastructure.project.entity.ProjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {

    List<ProjectJpaEntity> findByOwnerId(UUID ownerId);

    List<ProjectJpaEntity> findByOwnerIdAndStatus(UUID ownerId, String status);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
