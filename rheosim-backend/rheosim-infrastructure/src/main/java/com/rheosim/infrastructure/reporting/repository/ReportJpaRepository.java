package com.rheosim.infrastructure.reporting.repository;

import com.rheosim.infrastructure.reporting.entity.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, UUID> {

    List<ReportJpaEntity> findByProjectId(UUID projectId);

    List<ReportJpaEntity> findByGeneratedBy(UUID userId);
}
