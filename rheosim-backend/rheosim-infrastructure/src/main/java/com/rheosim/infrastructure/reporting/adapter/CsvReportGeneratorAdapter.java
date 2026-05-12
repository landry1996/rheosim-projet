package com.rheosim.infrastructure.reporting.adapter;

import com.rheosim.domain.reporting.model.ReportFormat;
import com.rheosim.domain.reporting.port.ReportGeneratorPort;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class CsvReportGeneratorAdapter implements ReportGeneratorPort {

    private final Path reportsDir;

    public CsvReportGeneratorAdapter(@Value("${rheosim.storage.path:./data/uploads}") String storagePath) {
        this.reportsDir = Path.of(storagePath, "reports");
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create reports directory", e);
        }
    }

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.CSV;
    }

    @Override
    public GenerationResult generate(ReportRequest request) {
        Path outputPath = reportsDir.resolve(request.reportId() + ".csv");

        try (Writer writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("Section", "Key", "Value")
                     .build())) {

            // Metadata
            printer.printRecord("Metadata", "Title", request.title());
            printer.printRecord("Metadata", "Project", request.projectName());
            printer.printRecord("Metadata", "Model", request.modelType());
            printer.printRecord("Metadata", "Branches", request.numberOfBranches());
            printer.printRecord("Metadata", "Converged", request.converged());
            printer.printRecord("Metadata", "R²", request.rSquared());
            printer.printRecord("Metadata", "Residual Norm", request.residualNorm());
            printer.printRecord("Metadata", "Iterations", request.iterations());

            // Parameters
            if (request.identifiedParameters() != null) {
                for (int i = 0; i < request.identifiedParameters().length; i++) {
                    String name = (request.parameterNames() != null && i < request.parameterNames().length)
                            ? request.parameterNames()[i] : "p" + (i + 1);
                    printer.printRecord("Parameter", name, request.identifiedParameters()[i]);
                }
            }

            // Fitted curve
            if (request.fittedCurve() != null) {
                for (int i = 0; i < request.fittedCurve().size(); i++) {
                    double[] point = request.fittedCurve().get(i);
                    printer.printRecord("FittedCurve", "x=" + point[0], point[1]);
                }
            }

            // Metrics
            if (request.metrics() != null) {
                for (var entry : request.metrics().entrySet()) {
                    printer.printRecord("Metric", entry.getKey(), entry.getValue());
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate CSV report", e);
        }

        try {
            long fileSize = Files.size(outputPath);
            return new GenerationResult(outputPath.toString(), fileSize);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read generated CSV size", e);
        }
    }
}
