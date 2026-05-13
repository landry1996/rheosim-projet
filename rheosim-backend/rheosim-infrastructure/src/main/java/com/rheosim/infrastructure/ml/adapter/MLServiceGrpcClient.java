package com.rheosim.infrastructure.ml.adapter;

import com.rheosim.domain.ml.model.MLPredictionResult;
import com.rheosim.domain.ml.port.MLPredictionPort;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MLServiceGrpcClient implements MLPredictionPort {

    private static final Logger log = LoggerFactory.getLogger(MLServiceGrpcClient.class);

    @Value("${rheosim.ml.grpc.host:localhost}")
    private String mlServiceHost;

    @Value("${rheosim.ml.grpc.port:50052}")
    private int mlServicePort;

    @Value("${rheosim.ml.grpc.timeout-ms:5000}")
    private long timeoutMs;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(mlServiceHost, mlServicePort)
                .usePlaintext()
                .build();
        log.info("ML Service gRPC client initialized: {}:{}", mlServiceHost, mlServicePort);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
            }
        }
    }

    @Override
    public MLPredictionResult predictModel(List<Double> timePoints, List<Double> values, String experimentType) {
        try {
            var stub = MLServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

            var request = MlService.PredictionRequest.newBuilder()
                    .addAllTimePoints(timePoints)
                    .addAllValues(values)
                    .setExperimentType(experimentType)
                    .build();

            var response = stub.predictModel(request);

            List<MLPredictionResult.ModelAlternative> alternatives = new ArrayList<>();
            for (var alt : response.getAlternativesList()) {
                alternatives.add(new MLPredictionResult.ModelAlternative(
                        alt.getModelType(),
                        alt.getConfidence(),
                        alt.getInitialParametersList()
                ));
            }

            return new MLPredictionResult(
                    response.getRecommendedModel(),
                    response.getConfidence(),
                    response.getInitialParametersList(),
                    alternatives
            );
        } catch (StatusRuntimeException e) {
            log.error("ML Service gRPC call failed: {}", e.getStatus());
            throw new RuntimeException("ML Service unavailable: " + e.getStatus(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            var stub = MLServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(2000, TimeUnit.MILLISECONDS);

            var response = stub.healthCheck(MlService.Empty.newBuilder().build());
            return "healthy".equals(response.getStatus()) && response.getModelsLoaded();
        } catch (Exception e) {
            log.debug("ML Service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
