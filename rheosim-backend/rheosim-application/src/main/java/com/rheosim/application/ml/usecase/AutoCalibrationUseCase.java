package com.rheosim.application.ml.usecase;

import com.rheosim.application.ml.dto.AutoCalibrationRequest;
import com.rheosim.application.ml.dto.AutoCalibrationResponse;
import com.rheosim.domain.ml.model.MLPredictionResult;
import com.rheosim.domain.ml.port.MLPredictionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoCalibrationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutoCalibrationUseCase.class);

    private final MLPredictionPort mlPredictionPort;

    public AutoCalibrationUseCase(MLPredictionPort mlPredictionPort) {
        this.mlPredictionPort = mlPredictionPort;
    }

    public AutoCalibrationResponse predict(AutoCalibrationRequest request) {
        if (!mlPredictionPort.isAvailable()) {
            log.warn("ML Service is not available, returning empty prediction");
            return new AutoCalibrationResponse(
                    null, 0.0, List.of(), List.of(), false
            );
        }

        MLPredictionResult result = mlPredictionPort.predictModel(
                request.timePoints(),
                request.values(),
                request.experimentType()
        );

        List<AutoCalibrationResponse.AlternativeModel> alternatives = result.alternatives().stream()
                .map(alt -> new AutoCalibrationResponse.AlternativeModel(
                        alt.modelType(),
                        alt.confidence(),
                        alt.initialParameters()
                ))
                .toList();

        log.info("ML prediction: model={}, confidence={:.2f}",
                result.recommendedModel(), result.confidence());

        return new AutoCalibrationResponse(
                result.recommendedModel(),
                result.confidence(),
                result.initialParameters(),
                alternatives,
                true
        );
    }
}
