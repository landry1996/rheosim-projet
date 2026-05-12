package com.rheosim.infrastructure.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public Counter simulationStartedCounter(MeterRegistry registry) {
        return Counter.builder("rheosim.simulation.started.total")
                .description("Total number of simulations started")
                .register(registry);
    }

    @Bean
    public Counter simulationCompletedCounter(MeterRegistry registry) {
        return Counter.builder("rheosim.simulation.completed.total")
                .description("Total number of simulations completed successfully")
                .register(registry);
    }

    @Bean
    public Counter simulationFailedCounter(MeterRegistry registry) {
        return Counter.builder("rheosim.simulation.failed.total")
                .description("Total number of simulations that failed")
                .register(registry);
    }

    @Bean
    public Timer simulationDurationTimer(MeterRegistry registry) {
        return Timer.builder("rheosim.simulation.duration")
                .description("Time taken for simulation execution")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public Counter datasetUploadCounter(MeterRegistry registry) {
        return Counter.builder("rheosim.dataset.upload.total")
                .description("Total number of dataset uploads")
                .register(registry);
    }

    @Bean
    public Counter computeEngineGrpcCalls(MeterRegistry registry) {
        return Counter.builder("rheosim.compute.grpc.calls.total")
                .description("Total gRPC calls to C++ compute engine")
                .register(registry);
    }

    @Bean
    public Counter computeEngineFallbacks(MeterRegistry registry) {
        return Counter.builder("rheosim.compute.fallback.total")
                .description("Total fallbacks from C++ to Java engine")
                .register(registry);
    }
}
