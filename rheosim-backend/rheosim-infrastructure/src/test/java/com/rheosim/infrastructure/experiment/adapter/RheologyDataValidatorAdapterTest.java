package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.model.ExperimentType;
import com.rheosim.domain.experiment.model.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RheologyDataValidatorAdapter")
class RheologyDataValidatorAdapterTest {

    private final RheologyDataValidatorAdapter validator = new RheologyDataValidatorAdapter();

    @Test
    @DisplayName("Valid creep data produces no errors")
    void validate_validCreepData_shouldProduceNoErrors() {
        List<DataColumn> columns = List.of(
                new DataColumn("time (s)", "s", DataColumn.PhysicalQuantity.TIME),
                new DataColumn("strain", "", DataColumn.PhysicalQuantity.STRAIN)
        );
        List<double[]> rows = List.of(
                new double[]{0.1, 0.001},
                new double[]{0.2, 0.002},
                new double[]{0.3, 0.003},
                new double[]{0.4, 0.004}
        );

        List<ValidationError> errors = validator.validate(ExperimentType.CREEP, columns, rows);

        List<ValidationError> criticalErrors = errors.stream()
                .filter(e -> e.severity() == ValidationError.Severity.ERROR)
                .toList();
        assertThat(criticalErrors).isEmpty();
    }

    @Test
    @DisplayName("Creep without TIME column produces error")
    void validate_creepWithoutTime_shouldProduceError() {
        List<DataColumn> columns = List.of(
                new DataColumn("strain", "", DataColumn.PhysicalQuantity.STRAIN)
        );
        List<double[]> rows = List.of(
                new double[]{0.001}, new double[]{0.002}, new double[]{0.003}
        );

        List<ValidationError> errors = validator.validate(ExperimentType.CREEP, columns, rows);

        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.message().contains("TIME"));
    }

    @Test
    @DisplayName("Empty rows produce error")
    void validate_emptyRows_shouldProduceError() {
        List<DataColumn> columns = List.of(
                new DataColumn("time", "s", DataColumn.PhysicalQuantity.TIME)
        );

        List<ValidationError> errors = validator.validate(ExperimentType.CREEP, columns, List.of());

        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.message().contains("No data rows"));
    }

    @Test
    @DisplayName("Negative values on positive-only quantities produce warnings")
    void validate_negativeValues_shouldProduceWarnings() {
        List<DataColumn> columns = List.of(
                new DataColumn("time (s)", "s", DataColumn.PhysicalQuantity.TIME),
                new DataColumn("viscosity (Pa.s)", "Pa.s", DataColumn.PhysicalQuantity.VISCOSITY)
        );
        List<double[]> rows = List.of(
                new double[]{1.0, 100.0},
                new double[]{2.0, -50.0},
                new double[]{3.0, 200.0}
        );

        List<ValidationError> errors = validator.validate(ExperimentType.FLOW_CURVE, columns, rows);

        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.WARNING && e.message().contains("Negative"));
    }

    @Test
    @DisplayName("Too few data points produce error")
    void validate_tooFewPoints_shouldProduceError() {
        List<DataColumn> columns = List.of(
                new DataColumn("time", "s", DataColumn.PhysicalQuantity.TIME),
                new DataColumn("strain", "", DataColumn.PhysicalQuantity.STRAIN)
        );
        List<double[]> rows = List.of(
                new double[]{1.0, 0.01},
                new double[]{2.0, 0.02}
        );

        List<ValidationError> errors = validator.validate(ExperimentType.CREEP, columns, rows);

        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.message().contains("Minimum"));
    }

    @Test
    @DisplayName("Oscillatory data without FREQUENCY column produces error")
    void validate_oscillatoryWithoutFrequency_shouldProduceError() {
        List<DataColumn> columns = List.of(
                new DataColumn("G'", "Pa", DataColumn.PhysicalQuantity.STORAGE_MODULUS),
                new DataColumn("G''", "Pa", DataColumn.PhysicalQuantity.LOSS_MODULUS)
        );
        List<double[]> rows = List.of(
                new double[]{1000, 500}, new double[]{1100, 600}, new double[]{1200, 700}
        );

        List<ValidationError> errors = validator.validate(ExperimentType.FREQUENCY_SWEEP, columns, rows);

        assertThat(errors).anyMatch(e ->
                e.severity() == ValidationError.Severity.ERROR && e.message().contains("FREQUENCY"));
    }
}
