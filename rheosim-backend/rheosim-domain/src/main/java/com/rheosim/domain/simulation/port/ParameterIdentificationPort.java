package com.rheosim.domain.simulation.port;

import com.rheosim.domain.simulation.model.SimulationResult;

public interface ParameterIdentificationPort {

    SimulationResult identify(
            ConstitutiveLaw law,
            double[] xData,
            double[] yData,
            double[] initialGuess,
            double[] lowerBounds,
            double[] upperBounds,
            FitTarget target
    );

    enum FitTarget {
        RELAXATION_MODULUS,
        CREEP_COMPLIANCE,
        STORAGE_MODULUS,
        LOSS_MODULUS,
        COMPLEX_VISCOSITY
    }
}
