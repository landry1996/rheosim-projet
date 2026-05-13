package com.rheosim.infrastructure.ml.adapter;

import com.rheosim.application.ml.dto.AutoCalibrationRequest;
import com.rheosim.application.ml.dto.AutoCalibrationResponse;
import com.rheosim.application.ml.usecase.AutoCalibrationUseCase;
import com.rheosim.domain.ml.port.MLPredictionPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ml")
public class MLController {

    private final AutoCalibrationUseCase autoCalibrationUseCase;
    private final MLPredictionPort mlPredictionPort;

    public MLController(AutoCalibrationUseCase autoCalibrationUseCase, MLPredictionPort mlPredictionPort) {
        this.autoCalibrationUseCase = autoCalibrationUseCase;
        this.mlPredictionPort = mlPredictionPort;
    }

    @PostMapping("/predict")
    public ResponseEntity<AutoCalibrationResponse> predictModel(
            @Valid @RequestBody AutoCalibrationRequest request) {
        AutoCalibrationResponse response = autoCalibrationUseCase.predict(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<MLStatusResponse> getMLStatus() {
        return ResponseEntity.ok(new MLStatusResponse(mlPredictionPort.isAvailable()));
    }

    record MLStatusResponse(boolean available) {}
}
