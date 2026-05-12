package com.rheosim.application.simulation.usecase;

import com.rheosim.application.simulation.dto.JobResponse;
import com.rheosim.application.simulation.dto.SubmitJobRequest;
import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;
import com.rheosim.domain.simulation.port.SimulationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationUseCase")
class SimulationUseCaseTest {

    @Mock
    private SimulationJobRepository jobRepository;

    private SimulationUseCase simulationUseCase;

    @BeforeEach
    void setUp() {
        simulationUseCase = new SimulationUseCase(jobRepository);
    }

    @Test
    @DisplayName("submitJob creates a QUEUED job")
    void submitJob_shouldCreateQueuedJob() {
        SubmitJobRequest request = new SubmitJobRequest(
                UUID.randomUUID(), UUID.randomUUID(), null,
                "PARAMETER_IDENTIFICATION", "PRONY", 3
        );
        when(jobRepository.countByStatus(JobStatus.RUNNING)).thenReturn(0L);
        when(jobRepository.save(any(SimulationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = simulationUseCase.submitJob(request, UUID.randomUUID());

        assertThat(response.status()).isEqualTo("QUEUED");
        assertThat(response.modelType()).isEqualTo("PRONY");
        assertThat(response.numberOfBranches()).isEqualTo(3);
    }

    @Test
    @DisplayName("submitJob fails when max concurrent reached")
    void submitJob_shouldFailWhenMaxConcurrent() {
        SubmitJobRequest request = new SubmitJobRequest(
                UUID.randomUUID(), UUID.randomUUID(), null,
                "PARAMETER_IDENTIFICATION", "MAXWELL", 1
        );
        when(jobRepository.countByStatus(JobStatus.RUNNING)).thenReturn(4L);

        assertThatThrownBy(() -> simulationUseCase.submitJob(request, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum concurrent jobs");
    }

    @Test
    @DisplayName("cancelJob transitions to CANCELLED")
    void cancelJob_shouldTransitionToCancelled() {
        UUID jobId = UUID.randomUUID();
        SimulationJob job = SimulationJob.builder()
                .projectId(UUID.randomUUID())
                .datasetId(UUID.randomUUID())
                .submittedBy(UUID.randomUUID())
                .simulationType(com.rheosim.domain.simulation.model.SimulationType.PARAMETER_IDENTIFICATION)
                .modelType(com.rheosim.domain.project.model.ConstitutiveModelType.MAXWELL)
                .numberOfBranches(1)
                .build();
        job.setId(jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(SimulationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = simulationUseCase.cancelJob(jobId, UUID.randomUUID());

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("getJobResult throws if job not completed")
    void getJobResult_shouldThrowIfNotCompleted() {
        UUID jobId = UUID.randomUUID();
        SimulationJob job = SimulationJob.builder()
                .projectId(UUID.randomUUID())
                .datasetId(UUID.randomUUID())
                .submittedBy(UUID.randomUUID())
                .simulationType(com.rheosim.domain.simulation.model.SimulationType.PARAMETER_IDENTIFICATION)
                .modelType(com.rheosim.domain.project.model.ConstitutiveModelType.MAXWELL)
                .numberOfBranches(1)
                .build();
        job.setId(jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> simulationUseCase.getJobResult(jobId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not completed");
    }
}
