package com.rheosim.infrastructure.simulation.adapter;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;
import com.rheosim.domain.simulation.model.SimulationType;
import com.rheosim.domain.simulation.port.SimulationJobRepository;
import com.rheosim.infrastructure.simulation.entity.SimulationJobJpaEntity;
import com.rheosim.infrastructure.simulation.repository.SimulationJobJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SimulationJobRepositoryAdapter implements SimulationJobRepository {

    private final SimulationJobJpaRepository jpaRepository;

    public SimulationJobRepositoryAdapter(SimulationJobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SimulationJob save(SimulationJob job) {
        SimulationJobJpaEntity entity = toJpaEntity(job);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<SimulationJob> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SimulationJob> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SimulationJob> findByStatus(JobStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SimulationJob> findByProjectIdAndStatus(UUID projectId, JobStatus status) {
        return jpaRepository.findByProjectIdAndStatus(projectId, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(JobStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private SimulationJobJpaEntity toJpaEntity(SimulationJob job) {
        SimulationJobJpaEntity entity = new SimulationJobJpaEntity();
        entity.setId(job.getId());
        entity.setProjectId(job.getProjectId());
        entity.setDatasetId(job.getDatasetId());
        entity.setMaterialId(job.getMaterialId());
        entity.setSubmittedBy(job.getSubmittedBy());
        entity.setSimulationType(job.getSimulationType().name());
        entity.setModelType(job.getModelType().name());
        entity.setNumberOfBranches(job.getNumberOfBranches());
        entity.setStatus(job.getStatus().name());
        entity.setProgress(job.getProgress());
        entity.setResultData(job.getResultData());
        entity.setErrorMessage(job.getErrorMessage());
        entity.setStartedAt(job.getStartedAt());
        entity.setCompletedAt(job.getCompletedAt());
        entity.setCreatedAt(job.getCreatedAt());
        entity.setUpdatedAt(job.getUpdatedAt());
        return entity;
    }

    private SimulationJob toDomain(SimulationJobJpaEntity entity) {
        SimulationJob job = SimulationJob.builder()
                .projectId(entity.getProjectId())
                .datasetId(entity.getDatasetId())
                .materialId(entity.getMaterialId())
                .submittedBy(entity.getSubmittedBy())
                .simulationType(SimulationType.valueOf(entity.getSimulationType()))
                .modelType(ConstitutiveModelType.valueOf(entity.getModelType()))
                .numberOfBranches(entity.getNumberOfBranches())
                .build();

        job.setId(entity.getId());
        job.setStatus(JobStatus.valueOf(entity.getStatus()));
        job.setProgress(entity.getProgress());
        job.setResultData(entity.getResultData());
        job.setErrorMessage(entity.getErrorMessage());
        job.setStartedAt(entity.getStartedAt());
        job.setCompletedAt(entity.getCompletedAt());
        job.setCreatedAt(entity.getCreatedAt());
        job.setUpdatedAt(entity.getUpdatedAt());
        return job;
    }
}
