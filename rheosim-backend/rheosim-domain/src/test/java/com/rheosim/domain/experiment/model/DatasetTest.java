package com.rheosim.domain.experiment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dataset Entity")
class DatasetTest {

    @Test
    @DisplayName("Builder creates dataset with UPLOADED status")
    void builder_shouldCreateDatasetWithUploadedStatus() {
        Dataset dataset = Dataset.builder()
                .originalFileName("test.csv")
                .contentType("text/csv")
                .fileSize(1024)
                .projectId(UUID.randomUUID())
                .uploadedBy(UUID.randomUUID())
                .experimentType(ExperimentType.CREEP)
                .temperature(25.0)
                .temperatureUnit("°C")
                .build();

        assertThat(dataset.getId()).isNotNull();
        assertThat(dataset.getStatus()).isEqualTo(DatasetStatus.UPLOADED);
        assertThat(dataset.getOriginalFileName()).isEqualTo("test.csv");
        assertThat(dataset.getExperimentType()).isEqualTo(ExperimentType.CREEP);
    }

    @Test
    @DisplayName("markValidating transitions from UPLOADED to VALIDATING")
    void markValidating_shouldTransitionToValidating() {
        Dataset dataset = createTestDataset();

        dataset.markValidating();

        assertThat(dataset.getStatus()).isEqualTo(DatasetStatus.VALIDATING);
    }

    @Test
    @DisplayName("markValid sets status and row count")
    void markValid_shouldSetStatusAndRowCount() {
        Dataset dataset = createTestDataset();
        List<DataColumn> columns = List.of(
                new DataColumn("time (s)", "s", DataColumn.PhysicalQuantity.TIME),
                new DataColumn("strain", "", DataColumn.PhysicalQuantity.STRAIN)
        );

        dataset.markValid(100, columns);

        assertThat(dataset.getStatus()).isEqualTo(DatasetStatus.VALID);
        assertThat(dataset.getRowCount()).isEqualTo(100);
        assertThat(dataset.getColumns()).hasSize(2);
        assertThat(dataset.getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("markInvalid stores validation errors")
    void markInvalid_shouldStoreErrors() {
        Dataset dataset = createTestDataset();
        List<ValidationError> errors = List.of(
                new ValidationError(1, "time", "Negative value", ValidationError.Severity.ERROR),
                new ValidationError(5, "strain", "Missing data", ValidationError.Severity.WARNING)
        );

        dataset.markInvalid(errors);

        assertThat(dataset.getStatus()).isEqualTo(DatasetStatus.INVALID);
        assertThat(dataset.getValidationErrors()).hasSize(2);
    }

    @Test
    @DisplayName("markProcessing requires VALID status")
    void markProcessing_shouldRequireValidStatus() {
        Dataset dataset = createTestDataset();

        assertThatThrownBy(dataset::markProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VALID");
    }

    @Test
    @DisplayName("markProcessing succeeds when dataset is VALID")
    void markProcessing_shouldSucceedWhenValid() {
        Dataset dataset = createTestDataset();
        dataset.markValid(50, List.of());

        dataset.markProcessing();

        assertThat(dataset.getStatus()).isEqualTo(DatasetStatus.PROCESSING);
    }

    private Dataset createTestDataset() {
        return Dataset.builder()
                .originalFileName("experiment.csv")
                .contentType("text/csv")
                .fileSize(2048)
                .projectId(UUID.randomUUID())
                .uploadedBy(UUID.randomUUID())
                .experimentType(ExperimentType.RELAXATION)
                .temperature(30.0)
                .temperatureUnit("°C")
                .build();
    }
}
