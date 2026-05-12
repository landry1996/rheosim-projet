package com.rheosim.domain.simulation.model;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SimulationJob Entity")
class SimulationJobTest {

    @Test
    @DisplayName("Builder creates job with QUEUED status")
    void builder_shouldCreateJobWithQueuedStatus() {
        SimulationJob job = createTestJob();

        assertThat(job.getId()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(job.getProgress()).isEqualTo(0.0);
        assertThat(job.getModelType()).isEqualTo(ConstitutiveModelType.PRONY);
    }

    @Test
    @DisplayName("start transitions QUEUED to RUNNING")
    void start_shouldTransitionToRunning() {
        SimulationJob job = createTestJob();
        job.start();

        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("start fails if not QUEUED")
    void start_shouldFailIfNotQueued() {
        SimulationJob job = createTestJob();
        job.start();

        assertThatThrownBy(job::start)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateProgress clamps between 0 and 100")
    void updateProgress_shouldClampValues() {
        SimulationJob job = createTestJob();
        job.start();

        job.updateProgress(150.0);
        assertThat(job.getProgress()).isEqualTo(100.0);

        job.updateProgress(-10.0);
        assertThat(job.getProgress()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("complete sets status and result")
    void complete_shouldSetStatusAndResult() {
        SimulationJob job = createTestJob();
        job.start();
        job.complete("{\"r2\": 0.99}");

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getProgress()).isEqualTo(100.0);
        assertThat(job.getResultData()).contains("0.99");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("fail sets error message")
    void fail_shouldSetErrorMessage() {
        SimulationJob job = createTestJob();
        job.start();
        job.fail("Convergence failed");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Convergence failed");
    }

    @Test
    @DisplayName("cancel throws on finished job")
    void cancel_shouldThrowOnFinishedJob() {
        SimulationJob job = createTestJob();
        job.start();
        job.complete("{}");

        assertThatThrownBy(job::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel");
    }

    private SimulationJob createTestJob() {
        return SimulationJob.builder()
                .projectId(UUID.randomUUID())
                .datasetId(UUID.randomUUID())
                .submittedBy(UUID.randomUUID())
                .simulationType(SimulationType.PARAMETER_IDENTIFICATION)
                .modelType(ConstitutiveModelType.PRONY)
                .numberOfBranches(3)
                .build();
    }
}
