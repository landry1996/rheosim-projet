package com.rheosim.infrastructure.compute.grpc;

import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort;
import com.rheosim.infrastructure.simulation.engine.LevenbergMarquardtIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Routes compute requests to either the C++ gRPC engine (V2) or
 * the local Java engine (V1) based on availability and configuration.
 */
@Component
@Primary
public class ComputeEngineRoutingAdapter implements ParameterIdentificationPort {

    private static final Logger log = LoggerFactory.getLogger(ComputeEngineRoutingAdapter.class);

    private final ComputeEngineClient grpcClient;
    private final LevenbergMarquardtIdentifier javaIdentifier;

    public ComputeEngineRoutingAdapter(ComputeEngineClient grpcClient,
                                        LevenbergMarquardtIdentifier javaIdentifier) {
        this.grpcClient = grpcClient;
        this.javaIdentifier = javaIdentifier;
    }

    @Override
    public SimulationResult identify(
            ConstitutiveLaw law,
            double[] xData,
            double[] yData,
            double[] initialGuess,
            double[] lowerBounds,
            double[] upperBounds,
            FitTarget fitTarget
    ) {
        if (grpcClient.isAvailable()) {
            log.info("Routing to C++ compute engine (gRPC)");
            try {
                return grpcClient.identify(law, xData, yData, initialGuess, lowerBounds, upperBounds, fitTarget);
            } catch (Exception e) {
                log.warn("C++ engine failed, falling back to Java: {}", e.getMessage());
            }
        }

        log.debug("Using local Java compute engine");
        return javaIdentifier.identify(law, xData, yData, initialGuess, lowerBounds, upperBounds, fitTarget);
    }
}
