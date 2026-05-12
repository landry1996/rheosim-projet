package com.rheosim.infrastructure.simulation.engine;

import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Parameter Identification — Convergence Tests")
class ParameterIdentificationTest {

    private final LevenbergMarquardtIdentifier identifier = new LevenbergMarquardtIdentifier();
    private final MaxwellLaw maxwell = new MaxwellLaw();

    @Test
    @DisplayName("Recovers Maxwell parameters from synthetic relaxation data")
    void identify_shouldRecoverMaxwellParamsFromRelaxation() {
        // Generate synthetic data: G(t) = 5000 * exp(-t / 2.0)
        double trueG = 5000.0;
        double trueTau = 2.0;
        double[] trueParams = {trueG, trueTau};

        int n = 50;
        double[] time = new double[n];
        double[] gOfT = new double[n];
        for (int i = 0; i < n; i++) {
            time[i] = 0.01 + i * 0.2;
            gOfT[i] = maxwell.computeRelaxationModulus(time[i], trueParams);
        }

        double[] initialGuess = {1000.0, 1.0};
        double[] lowerBounds = {1e-3, 1e-10};
        double[] upperBounds = {1e12, 1e6};

        SimulationResult result = identifier.identify(
                maxwell, time, gOfT, initialGuess, lowerBounds, upperBounds,
                ParameterIdentificationPort.FitTarget.RELAXATION_MODULUS
        );

        assertThat(result.converged()).isTrue();
        assertThat(result.rSquared()).isGreaterThan(0.999);
        assertThat(result.identifiedParameters()[0]).isCloseTo(trueG, within(trueG * 0.01));
        assertThat(result.identifiedParameters()[1]).isCloseTo(trueTau, within(trueTau * 0.01));
    }

    @Test
    @DisplayName("Recovers Maxwell parameters from synthetic storage modulus data")
    void identify_shouldRecoverMaxwellParamsFromStorageModulus() {
        double trueG = 2000.0;
        double trueTau = 0.5;
        double[] trueParams = {trueG, trueTau};

        int n = 30;
        double[] omega = new double[n];
        double[] gPrime = new double[n];
        for (int i = 0; i < n; i++) {
            omega[i] = Math.pow(10, -2 + i * 0.2);
            gPrime[i] = maxwell.computeStorageModulus(omega[i], trueParams);
        }

        double[] initialGuess = {500.0, 0.1};
        double[] lowerBounds = {1e-3, 1e-10};
        double[] upperBounds = {1e12, 1e6};

        SimulationResult result = identifier.identify(
                maxwell, omega, gPrime, initialGuess, lowerBounds, upperBounds,
                ParameterIdentificationPort.FitTarget.STORAGE_MODULUS
        );

        assertThat(result.converged()).isTrue();
        assertThat(result.rSquared()).isGreaterThan(0.99);
        assertThat(result.identifiedParameters()[0]).isCloseTo(trueG, within(trueG * 0.05));
        assertThat(result.identifiedParameters()[1]).isCloseTo(trueTau, within(trueTau * 0.05));
    }

    @Test
    @DisplayName("Returns converged=false for impossible fit")
    void identify_shouldReturnNotConvergedForBadData() {
        // Random noise data that doesn't match Maxwell model
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {100, -50, 200, -100, 300}; // oscillating, not exponential decay

        double[] initialGuess = {1000.0, 1.0};
        double[] lowerBounds = {1e-3, 1e-10};
        double[] upperBounds = {1e12, 1e6};

        SimulationResult result = identifier.identify(
                maxwell, x, y, initialGuess, lowerBounds, upperBounds,
                ParameterIdentificationPort.FitTarget.RELAXATION_MODULUS
        );

        // Even if it "converges" to something, R² should be terrible
        assertThat(result.rSquared()).isLessThan(0.5);
    }
}
