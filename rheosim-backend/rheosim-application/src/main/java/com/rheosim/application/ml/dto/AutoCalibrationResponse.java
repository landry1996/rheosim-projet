package com.rheosim.application.ml.dto;

import java.util.List;

public record AutoCalibrationResponse(
        String recommendedModel,
        double confidence,
        List<Double> initialParameters,
        List<AlternativeModel> alternatives,
        boolean mlAvailable
) {
    public record AlternativeModel(
            String modelType,
            double confidence,
            List<Double> initialParameters
    ) {}
}
