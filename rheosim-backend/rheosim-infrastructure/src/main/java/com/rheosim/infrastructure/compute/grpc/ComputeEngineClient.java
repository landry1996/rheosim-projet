package com.rheosim.infrastructure.compute.grpc;

import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort.FitTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * gRPC client adapter for the C++ Compute Engine.
 * Delegates parameter identification to the high-performance C++ service
 * when available, falling back to the local Java implementation otherwise.
 */
@Component
public class ComputeEngineClient {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineClient.class);

    @Value("${rheosim.compute.grpc.host:localhost}")
    private String grpcHost;

    @Value("${rheosim.compute.grpc.port:50051}")
    private int grpcPort;

    @Value("${rheosim.compute.grpc.enabled:false}")
    private boolean grpcEnabled;

    public boolean isAvailable() {
        if (!grpcEnabled) return false;
        // TODO: Implement health check via gRPC HealthCheck RPC
        return false;
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
        if (!isAvailable()) {
            throw new IllegalStateException("C++ compute engine is not available");
        }

        log.info("Delegating identification to C++ engine at {}:{}", grpcHost, grpcPort);

        // TODO: Implement gRPC call when grpc-java dependency is added
        // ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
        //     .usePlaintext()
        //     .build();
        // ComputeEngineGrpc.ComputeEngineBlockingStub stub =
        //     ComputeEngineGrpc.newBlockingStub(channel);
        // IdentificationRequest request = IdentificationRequest.newBuilder()
        //     .setModelType(law.getClass().getSimpleName())
        //     .addAllTimePoints(...)
        //     .build();
        // IdentificationResponse response = stub.identify(request);
        // channel.shutdown();

        throw new UnsupportedOperationException("gRPC client not yet wired — awaiting protobuf codegen");
    }
}
