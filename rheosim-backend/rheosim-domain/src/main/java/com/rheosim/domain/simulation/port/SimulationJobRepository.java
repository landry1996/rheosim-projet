package com.rheosim.domain.simulation.port;

import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationJobRepository {

    SimulationJob save(SimulationJob job);

    Optional<SimulationJob> findById(UUID id);

    List<SimulationJob> findByProjectId(UUID projectId);

    List<SimulationJob> findByStatus(JobStatus status);

    List<SimulationJob> findByProjectIdAndStatus(UUID projectId, JobStatus status);

    long countByStatus(JobStatus status);

    void deleteById(UUID id);
}
