package com.rheosim.infrastructure.reporting.adapter;

import com.rheosim.application.reporting.dto.GenerateReportRequest;
import com.rheosim.application.reporting.dto.ReportResponse;
import com.rheosim.application.reporting.usecase.ReportingUseCase;
import com.rheosim.domain.identity.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@Tag(name = "Reports", description = "Report generation and download")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportingUseCase reportingUseCase;

    public ReportController(ReportingUseCase reportingUseCase) {
        this.reportingUseCase = reportingUseCase;
    }

    @PostMapping
    @Operation(summary = "Generate a new report (PDF, CSV, or JSON)")
    public ResponseEntity<ReportResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReportResponse response = reportingUseCase.generateReport(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report metadata")
    public ResponseEntity<ReportResponse> getReport(@PathVariable UUID reportId) {
        ReportResponse response = reportingUseCase.getReport(reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List all reports for a project")
    public ResponseEntity<List<ReportResponse>> listReportsByProject(@PathVariable UUID projectId) {
        List<ReportResponse> reports = reportingUseCase.listReportsByProject(projectId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{reportId}/download")
    @Operation(summary = "Download the generated report file")
    public ResponseEntity<Resource> downloadReport(@PathVariable UUID reportId) {
        ReportResponse report = reportingUseCase.getReport(reportId);

        if (!"READY".equals(report.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Path filePath = Path.of(report.filePath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = switch (report.format()) {
            case "PDF" -> "application/pdf";
            case "CSV" -> "text/csv";
            case "JSON" -> "application/json";
            default -> "application/octet-stream";
        };

        String fileName = "rheosim-report-" + reportId + "." + report.format().toLowerCase();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(report.fileSize()))
                .body(resource);
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<Void> deleteReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal User currentUser
    ) {
        reportingUseCase.deleteReport(reportId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
