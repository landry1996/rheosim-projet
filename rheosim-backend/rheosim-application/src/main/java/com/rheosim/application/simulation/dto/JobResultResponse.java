package com.rheosim.application.simulation.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record JobResultResponse(
        UUID jobId,
        double[] identifiedParameters,
        String[] parameterNames,
        double residualNorm,
        double rSquared,
        int iterations,
        boolean converged,
        List<double[]> fittedCurve,
        Map<String, Double> metrics
) {}
