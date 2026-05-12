package com.rheosim.domain.reporting.port;

import com.rheosim.domain.reporting.model.DublinCoreMetadata;
import com.rheosim.domain.reporting.model.ReportFormat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReportGeneratorPort {

    boolean supports(ReportFormat format);

    GenerationResult generate(ReportRequest request);

    record ReportRequest(
            UUID reportId,
            String title,
            DublinCoreMetadata metadata,
            String projectName,
            String modelType,
            int numberOfBranches,
            double[] identifiedParameters,
            String[] parameterNames,
            double rSquared,
            double residualNorm,
            int iterations,
            boolean converged,
            List<double[]> fittedCurve,
            List<double[]> experimentalData,
            Map<String, Double> metrics
    ) {}

    record GenerationResult(
            String filePath,
            long fileSize
    ) {}
}
