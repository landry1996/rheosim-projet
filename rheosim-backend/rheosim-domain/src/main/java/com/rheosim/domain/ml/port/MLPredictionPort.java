package com.rheosim.domain.ml.port;

import com.rheosim.domain.ml.model.MLPredictionResult;

import java.util.List;

public interface MLPredictionPort {

    MLPredictionResult predictModel(List<Double> timePoints, List<Double> values, String experimentType);

    boolean isAvailable();
}
