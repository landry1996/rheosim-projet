package com.rheosim.infrastructure.simulation.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Constitutive Law Implementations — Scientific Validation")
class ConstitutiveLawTest {

    private final MaxwellLaw maxwell = new MaxwellLaw();
    private final KelvinVoigtLaw kelvinVoigt = new KelvinVoigtLaw();
    private final PronySeriesLaw prony = new PronySeriesLaw();

    // Maxwell: G = 1000 Pa, tau = 1 s
    private final double[] maxwellParams = {1000.0, 1.0};

    @Test
    @DisplayName("Maxwell: G(0) = G")
    void maxwell_relaxationAtZero_shouldEqualG() {
        double gt = maxwell.computeRelaxationModulus(0.0, maxwellParams);
        assertThat(gt).isCloseTo(1000.0, within(1e-10));
    }

    @Test
    @DisplayName("Maxwell: G(t→∞) → 0")
    void maxwell_relaxationAtInfinity_shouldApproachZero() {
        double gt = maxwell.computeRelaxationModulus(100.0, maxwellParams);
        assertThat(gt).isCloseTo(0.0, within(1e-30));
    }

    @Test
    @DisplayName("Maxwell: G(tau) = G * exp(-1)")
    void maxwell_relaxationAtTau_shouldEqualGExpMinus1() {
        double gt = maxwell.computeRelaxationModulus(1.0, maxwellParams);
        assertThat(gt).isCloseTo(1000.0 * Math.exp(-1), within(1e-10));
    }

    @Test
    @DisplayName("Maxwell: G'(omega) = G * (omega*tau)^2 / (1 + (omega*tau)^2)")
    void maxwell_storageModulus_formulaCheck() {
        double omega = 10.0;
        double wt2 = (omega * 1.0) * (omega * 1.0); // 100
        double expected = 1000.0 * wt2 / (1.0 + wt2); // 1000 * 100/101
        double actual = maxwell.computeStorageModulus(omega, maxwellParams);
        assertThat(actual).isCloseTo(expected, within(1e-10));
    }

    @Test
    @DisplayName("Maxwell: G''(omega) peaks at omega = 1/tau")
    void maxwell_lossModulusMaximum_shouldBeAtOneOverTau() {
        double gAtPeak = maxwell.computeLossModulus(1.0, maxwellParams);
        double gBelow = maxwell.computeLossModulus(0.5, maxwellParams);
        double gAbove = maxwell.computeLossModulus(2.0, maxwellParams);

        assertThat(gAtPeak).isGreaterThan(gBelow);
        assertThat(gAtPeak).isGreaterThan(gAbove);
    }

    @Test
    @DisplayName("Kelvin-Voigt: J(0) = 0")
    void kelvinVoigt_creepAtZero_shouldBeZero() {
        double[] params = {1000.0, 100.0}; // G=1000, eta=100
        double jt = kelvinVoigt.computeCreepCompliance(0.0, params);
        assertThat(jt).isCloseTo(0.0, within(1e-10));
    }

    @Test
    @DisplayName("Kelvin-Voigt: J(t→∞) = 1/G")
    void kelvinVoigt_creepAtInfinity_shouldEqual1OverG() {
        double[] params = {1000.0, 100.0};
        double jt = kelvinVoigt.computeCreepCompliance(1000.0, params);
        assertThat(jt).isCloseTo(1.0 / 1000.0, within(1e-6));
    }

    @Test
    @DisplayName("Prony: G(0) = G_inf + sum(G_i)")
    void prony_relaxationAtZero_shouldEqualSum() {
        // G_inf=100, G_1=500 tau_1=0.1, G_2=300 tau_2=10
        double[] params = {100.0, 500.0, 0.1, 300.0, 10.0};
        double gt = prony.computeRelaxationModulus(0.0, params);
        assertThat(gt).isCloseTo(900.0, within(1e-10));
    }

    @Test
    @DisplayName("Prony: G(t→∞) = G_inf")
    void prony_relaxationAtInfinity_shouldEqualGInf() {
        double[] params = {100.0, 500.0, 0.1, 300.0, 10.0};
        double gt = prony.computeRelaxationModulus(1e6, params);
        assertThat(gt).isCloseTo(100.0, within(1e-6));
    }

    @Test
    @DisplayName("Prony: G'(0) = G_inf (low frequency limit)")
    void prony_storageModulusLowFreq_shouldApproachGInf() {
        double[] params = {100.0, 500.0, 0.1, 300.0, 10.0};
        double gPrime = prony.computeStorageModulus(1e-10, params);
        assertThat(gPrime).isCloseTo(100.0, within(1.0));
    }

    @Test
    @DisplayName("Prony: G'(∞) = G_inf + sum(G_i) (high frequency limit)")
    void prony_storageModulusHighFreq_shouldApproachTotal() {
        double[] params = {100.0, 500.0, 0.1, 300.0, 10.0};
        double gPrime = prony.computeStorageModulus(1e10, params);
        assertThat(gPrime).isCloseTo(900.0, within(1.0));
    }
}
