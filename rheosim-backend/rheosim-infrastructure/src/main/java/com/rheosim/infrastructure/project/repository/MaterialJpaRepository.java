package com.rheosim.infrastructure.project.repository;

import com.rheosim.infrastructure.project.entity.MaterialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialJpaRepository extends JpaRepository<MaterialJpaEntity, UUID> {

    List<MaterialJpaEntity> findByProjectId(UUID projectId);

    List<MaterialJpaEntity> findByFamily(String family);
}
