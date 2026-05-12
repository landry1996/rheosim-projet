package com.rheosim.application.simulation.usecase;

import com.rheosim.application.simulation.dto.JobResponse;
import com.rheosim.application.simulation.dto.JobResultResponse;
import com.rheosim.application.simulation.dto.SubmitJobRequest;
import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;
import com.rheosim.domain.simulation.model.SimulationType;
import com.rheosim.domain.simulation.port.SimulationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SimulationUseCase {

    private final SimulationJobRepository jobRepository;

    public SimulationUseCase(SimulationJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobResponse submitJob(SubmitJobRequest request, UUID submittedBy) {
        long runningCount = jobRepository.countByStatus(JobStatus.RUNNING);
        if (runningCount >= 4) {
            throw new IllegalStateException("Maximum concurrent jobs reached (4). Please wait for a job to complete.");
        }

        SimulationJob job = SimulationJob.builder()
                .projectId(request.projectId())
                .datasetId(request.datasetId())
                .materialId(request.materialId())
                .submittedBy(submittedBy)
                .simulationType(SimulationType.valueOf(request.simulationType().toUpperCase()))
                .modelType(ConstitutiveModelType.valueOf(request.modelType().toUpperCase()))
                .numberOfBranches(request.numberOfBranches())
                .build();

        SimulationJob saved = jobRepository.save(job);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        SimulationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobsByProject(UUID projectId) {
        return jobRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listQueuedJobs() {
        return jobRepository.findByStatus(JobStatus.QUEUED).stream()
                .map(this::toResponse)
                .toList();
    }

    public JobResponse cancelJob(UUID jobId, UUID userId) {
        SimulationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.cancel();
        SimulationJob saved = jobRepository.save(job);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobResultResponse getJobResult(UUID jobId) {
        SimulationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Job is not completed yet, status: " + job.getStatus());
        }

        return new JobResultResponse(
                job.getId(),
                null, null, 0, 0, 0, false, null, null
        );
    }

    private JobResponse toResponse(SimulationJob job) {
        return new JobResponse(
                job.getId(),
                job.getProjectId(),
                job.getDatasetId(),
                job.getMaterialId(),
                job.getSubmittedBy(),
                job.getSimulationType().name(),
                job.getModelType().name(),
                job.getNumberOfBranches(),
                job.getStatus().name(),
                job.getProgress(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
