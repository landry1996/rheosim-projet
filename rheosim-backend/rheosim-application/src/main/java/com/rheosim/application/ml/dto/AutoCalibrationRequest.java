package com.rheosim.application.ml.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AutoCalibrationRequest(
        @NotNull UUID datasetId,
        @NotEmpty List<Double> timePoints,
        @NotEmpty List<Double> values,
        String experimentType
) {
    public AutoCalibrationRequest {
        if (experimentType == null || experimentType.isBlank()) {
            experimentType = "relaxation";
        }
    }
}
