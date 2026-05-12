package com.rheosim.domain.simulation.model;

import java.util.List;
import java.util.Map;

public record SimulationResult(
        double[] identifiedParameters,
        String[] parameterNames,
        double residualNorm,
        double rSquared,
        int iterations,
        boolean converged,
        List<double[]> fittedCurve,
        Map<String, Double> metrics
) {}
