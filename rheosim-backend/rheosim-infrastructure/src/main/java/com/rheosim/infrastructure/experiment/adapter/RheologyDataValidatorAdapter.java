package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.model.ExperimentType;
import com.rheosim.domain.experiment.model.ValidationError;
import com.rheosim.domain.experiment.port.DataValidatorPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class RheologyDataValidatorAdapter implements DataValidatorPort {

    private static final int MIN_DATA_POINTS = 3;
    private static final int MAX_DATA_POINTS = 100_000;

    @Override
    public List<ValidationError> validate(ExperimentType experimentType,
                                          List<DataColumn> columns,
                                          List<double[]> rows) {
        List<ValidationError> errors = new ArrayList<>();

        if (columns.isEmpty()) {
            errors.add(new ValidationError(0, "", "No columns detected", ValidationError.Severity.ERROR));
            return errors;
        }

        if (rows.isEmpty()) {
            errors.add(new ValidationError(0, "", "No data rows found", ValidationError.Severity.ERROR));
            return errors;
        }

        if (rows.size() < MIN_DATA_POINTS) {
            errors.add(new ValidationError(0, "",
                    "Minimum " + MIN_DATA_POINTS + " data points required, got " + rows.size(),
                    ValidationError.Severity.ERROR));
        }

        if (rows.size() > MAX_DATA_POINTS) {
            errors.add(new ValidationError(0, "",
                    "Maximum " + MAX_DATA_POINTS + " data points allowed, got " + rows.size(),
                    ValidationError.Severity.ERROR));
        }

        validateRequiredColumns(experimentType, columns, errors);
        validateDataValues(columns, rows, errors);

        return errors;
    }

    private void validateRequiredColumns(ExperimentType experimentType,
                                         List<DataColumn> columns,
                                         List<ValidationError> errors) {
        Set<DataColumn.PhysicalQuantity> presentQuantities = Set.copyOf(
                columns.stream().map(DataColumn::quantity).toList());

        switch (experimentType) {
            case CREEP -> {
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.TIME)) {
                    errors.add(new ValidationError(0, "", "Creep experiment requires a TIME column",
                            ValidationError.Severity.ERROR));
                }
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.STRAIN)
                        && !presentQuantities.contains(DataColumn.PhysicalQuantity.COMPLIANCE)) {
                    errors.add(new ValidationError(0, "",
                            "Creep experiment requires STRAIN or COMPLIANCE column",
                            ValidationError.Severity.ERROR));
                }
            }
            case RELAXATION -> {
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.TIME)) {
                    errors.add(new ValidationError(0, "", "Relaxation experiment requires a TIME column",
                            ValidationError.Severity.ERROR));
                }
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.SHEAR_STRESS)
                        && !presentQuantities.contains(DataColumn.PhysicalQuantity.STORAGE_MODULUS)) {
                    errors.add(new ValidationError(0, "",
                            "Relaxation experiment requires SHEAR_STRESS or STORAGE_MODULUS column",
                            ValidationError.Severity.ERROR));
                }
            }
            case DYNAMIC_OSCILLATORY, FREQUENCY_SWEEP -> {
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.FREQUENCY)) {
                    errors.add(new ValidationError(0, "", "Oscillatory experiment requires a FREQUENCY column",
                            ValidationError.Severity.ERROR));
                }
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.STORAGE_MODULUS)
                        && !presentQuantities.contains(DataColumn.PhysicalQuantity.LOSS_MODULUS)) {
                    errors.add(new ValidationError(0, "",
                            "Oscillatory experiment requires STORAGE_MODULUS or LOSS_MODULUS column",
                            ValidationError.Severity.WARNING));
                }
            }
            case FLOW_CURVE -> {
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.SHEAR_RATE)) {
                    errors.add(new ValidationError(0, "", "Flow curve requires a SHEAR_RATE column",
                            ValidationError.Severity.ERROR));
                }
                if (!presentQuantities.contains(DataColumn.PhysicalQuantity.VISCOSITY)
                        && !presentQuantities.contains(DataColumn.PhysicalQuantity.SHEAR_STRESS)) {
                    errors.add(new ValidationError(0, "",
                            "Flow curve requires VISCOSITY or SHEAR_STRESS column",
                            ValidationError.Severity.ERROR));
                }
            }
            default -> {
                if (columns.size() < 2) {
                    errors.add(new ValidationError(0, "",
                            "At least 2 columns required for data analysis",
                            ValidationError.Severity.WARNING));
                }
            }
        }
    }

    private void validateDataValues(List<DataColumn> columns, List<double[]> rows,
                                    List<ValidationError> errors) {
        int maxErrorsPerColumn = 5;

        for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
            DataColumn column = columns.get(colIdx);
            int nanCount = 0;
            int negativeCount = 0;
            int colErrors = 0;

            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                double value = rows.get(rowIdx)[colIdx];

                if (Double.isNaN(value)) {
                    nanCount++;
                    continue;
                }

                if (mustBePositive(column.quantity()) && value < 0 && colErrors < maxErrorsPerColumn) {
                    negativeCount++;
                    if (negativeCount <= 3) {
                        errors.add(new ValidationError(rowIdx + 1, column.name(),
                                "Negative value (" + value + ") for quantity that should be positive",
                                ValidationError.Severity.WARNING));
                    }
                    colErrors++;
                }

                if (Double.isInfinite(value) && colErrors < maxErrorsPerColumn) {
                    errors.add(new ValidationError(rowIdx + 1, column.name(),
                            "Infinite value detected",
                            ValidationError.Severity.ERROR));
                    colErrors++;
                }
            }

            double nanPercentage = (double) nanCount / rows.size() * 100;
            if (nanPercentage > 50) {
                errors.add(new ValidationError(0, column.name(),
                        String.format("%.0f%% missing values in column", nanPercentage),
                        ValidationError.Severity.ERROR));
            } else if (nanPercentage > 10) {
                errors.add(new ValidationError(0, column.name(),
                        String.format("%.0f%% missing values in column", nanPercentage),
                        ValidationError.Severity.WARNING));
            }
        }
    }

    private boolean mustBePositive(DataColumn.PhysicalQuantity quantity) {
        return switch (quantity) {
            case TIME, FREQUENCY, VISCOSITY, STORAGE_MODULUS, LOSS_MODULUS,
                    COMPLEX_VISCOSITY, TEMPERATURE, COMPLIANCE -> true;
            default -> false;
        };
    }
}
