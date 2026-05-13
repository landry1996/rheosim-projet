package com.rheosim.domain.ml.model;

import java.util.List;

public record MLPredictionResult(
        String recommendedModel,
        double confidence,
        List<Double> initialParameters,
        List<ModelAlternative> alternatives
) {
    public record ModelAlternative(
            String modelType,
            double confidence,
            List<Double> initialParameters
    ) {}
}
