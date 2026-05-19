package com.rheosim.infrastructure.compute.grpc;

import com.rheosim.compute.grpc.ComputeEngineGrpc;
import com.rheosim.compute.grpc.Empty;
import com.rheosim.compute.grpc.IdentificationRequest;
import com.rheosim.compute.grpc.IdentificationResponse;
import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort.FitTarget;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ComputeEngineClient {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineClient.class);

    @Value("${rheosim.compute.grpc.host:localhost}")
    private String grpcHost;

    @Value("${rheosim.compute.grpc.port:50051}")
    private int grpcPort;

    @Value("${rheosim.compute.grpc.enabled:false}")
    private boolean grpcEnabled;

    private ManagedChannel channel;
    private ComputeEngineGrpc.ComputeEngineBlockingStub stub;

    private void ensureConnected() {
        if (channel == null || channel.isShutdown()) {
            channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                    .usePlaintext()
                    .build();
            stub = ComputeEngineGrpc.newBlockingStub(channel);
        }
    }

    public boolean isAvailable() {
        if (!grpcEnabled) return false;
        try {
            ensureConnected();
            stub.withDeadlineAfter(2, TimeUnit.SECONDS)
                .healthCheck(Empty.getDefaultInstance());
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Compute engine not available: {}", e.getStatus());
            return false;
        }
    }

    public SimulationResult identify(
            ConstitutiveLaw law,
            double[] xData,
            double[] yData,
            double[] initialGuess,
            double[] lowerBounds,
            double[] upperBounds,
            FitTarget fitTarget
    ) {
        ensureConnected();

        IdentificationRequest request = IdentificationRequest.newBuilder()
                .setModelType(law.getClass().getSimpleName())
                .addAllTimePoints(toList(xData))
                .addAllMeasuredValues(toList(yData))
                .addAllInitialGuess(toList(initialGuess))
                .addAllLowerBounds(toList(lowerBounds))
                .addAllUpperBounds(toList(upperBounds))
                .setFitTarget(fitTarget.name())
                .setMaxIterations(500)
                .setTolerance(1e-8)
                .build();

        log.info("Delegating identification to C++ engine at {}:{}", grpcHost, grpcPort);

        IdentificationResponse response = stub
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .identify(request);

        if (!response.getErrorMessage().isEmpty()) {
            throw new RuntimeException("Compute engine error: " + response.getErrorMessage());
        }

        double[] params = response.getParametersList().stream()
                .mapToDouble(Double::doubleValue).toArray();
        String[] paramNames = response.getParameterNamesList().toArray(String[]::new);
        double[] fitted = response.getFittedCurveList().stream()
                .mapToDouble(Double::doubleValue).toArray();

        return new SimulationResult(
                params,
                paramNames,
                0.0,
                response.getRSquared(),
                response.getIterations(),
                response.getConverged(),
                List.of(fitted),
                Map.copyOf(response.getMetricsMap())
        );
    }

    private static List<Double> toList(double[] arr) {
        return Arrays.stream(arr).boxed().toList();
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
            }
        }
    }
}
