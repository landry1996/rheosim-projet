package com.rheosim.infrastructure.simulation.repository;

import com.rheosim.infrastructure.simulation.entity.SimulationJobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationJobJpaRepository extends JpaRepository<SimulationJobJpaEntity, UUID> {

    List<SimulationJobJpaEntity> findByProjectId(UUID projectId);

    List<SimulationJobJpaEntity> findByStatus(String status);

    List<SimulationJobJpaEntity> findByProjectIdAndStatus(UUID projectId, String status);

    long countByStatus(String status);
}
