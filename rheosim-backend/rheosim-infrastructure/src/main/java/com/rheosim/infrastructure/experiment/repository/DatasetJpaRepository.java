package com.rheosim.infrastructure.experiment.repository;

import com.rheosim.infrastructure.experiment.entity.DatasetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetJpaRepository extends JpaRepository<DatasetJpaEntity, UUID> {

    List<DatasetJpaEntity> findByProjectId(UUID projectId);

    List<DatasetJpaEntity> findByProjectIdAndStatus(UUID projectId, String status);
}
