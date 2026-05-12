package com.rheosim.infrastructure.reporting.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rheosim.domain.reporting.model.ReportFormat;
import com.rheosim.domain.reporting.port.ReportGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonReportGeneratorAdapter implements ReportGeneratorPort {

    private final Path reportsDir;
    private final ObjectMapper objectMapper;

    public JsonReportGeneratorAdapter(@Value("${rheosim.storage.path:./data/uploads}") String storagePath,
                                      ObjectMapper objectMapper) {
        this.reportsDir = Path.of(storagePath, "reports");
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create reports directory", e);
        }
    }

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.JSON;
    }

    @Override
    public GenerationResult generate(ReportRequest request) {
        Path outputPath = reportsDir.resolve(request.reportId() + ".json");

        Map<String, Object> report = new LinkedHashMap<>();

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", request.title());
        metadata.put("project", request.projectName());
        metadata.put("model", request.modelType());
        metadata.put("branches", request.numberOfBranches());
        metadata.put("generatedAt", request.metadata().date().toString());
        metadata.put("generator", "RheoSim Enterprise v0.1.0");
        report.put("metadata", metadata);

        // Dublin Core
        Map<String, Object> dublinCore = new LinkedHashMap<>();
        dublinCore.put("dc:title", request.metadata().title());
        dublinCore.put("dc:creator", request.metadata().creator());
        dublinCore.put("dc:subject", request.metadata().subject());
        dublinCore.put("dc:description", request.metadata().description());
        dublinCore.put("dc:publisher", request.metadata().publisher());
        dublinCore.put("dc:date", request.metadata().date().toString());
        dublinCore.put("dc:type", request.metadata().type());
        dublinCore.put("dc:format", request.metadata().format());
        dublinCore.put("dc:identifier", request.metadata().identifier());
        dublinCore.put("dc:language", request.metadata().language());
        dublinCore.put("dc:rights", request.metadata().rights());
        report.put("dublinCore", dublinCore);

        // Results
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("converged", request.converged());
        results.put("rSquared", request.rSquared());
        results.put("residualNorm", request.residualNorm());
        results.put("iterations", request.iterations());
        report.put("results", results);

        // Parameters
        if (request.identifiedParameters() != null) {
            Map<String, Double> params = new LinkedHashMap<>();
            for (int i = 0; i < request.identifiedParameters().length; i++) {
                String name = (request.parameterNames() != null && i < request.parameterNames().length)
                        ? request.parameterNames()[i] : "p" + (i + 1);
                params.put(name, request.identifiedParameters()[i]);
            }
            report.put("parameters", params);
        }

        // Fitted curve
        if (request.fittedCurve() != null && !request.fittedCurve().isEmpty()) {
            report.put("fittedCurve", request.fittedCurve());
        }

        // Metrics
        if (request.metrics() != null && !request.metrics().isEmpty()) {
            report.put("metrics", request.metrics());
        }

        try {
            objectMapper.writeValue(outputPath.toFile(), report);
            long fileSize = Files.size(outputPath);
            return new GenerationResult(outputPath.toString(), fileSize);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate JSON report", e);
        }
    }
}
