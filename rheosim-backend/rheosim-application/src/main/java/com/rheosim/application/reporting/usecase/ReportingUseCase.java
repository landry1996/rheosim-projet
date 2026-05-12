package com.rheosim.application.reporting.usecase;

import com.rheosim.application.reporting.dto.GenerateReportRequest;
import com.rheosim.application.reporting.dto.ReportResponse;
import com.rheosim.domain.project.model.Project;
import com.rheosim.domain.project.port.ProjectRepository;
import com.rheosim.domain.reporting.model.DublinCoreMetadata;
import com.rheosim.domain.reporting.model.Report;
import com.rheosim.domain.reporting.model.ReportFormat;
import com.rheosim.domain.reporting.port.ReportGeneratorPort;
import com.rheosim.domain.reporting.port.ReportGeneratorPort.GenerationResult;
import com.rheosim.domain.reporting.port.ReportGeneratorPort.ReportRequest;
import com.rheosim.domain.reporting.port.ReportRepository;
import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;
import com.rheosim.domain.simulation.port.SimulationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReportingUseCase {

    private final ReportRepository reportRepository;
    private final SimulationJobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final List<ReportGeneratorPort> generators;

    public ReportingUseCase(ReportRepository reportRepository,
                            SimulationJobRepository jobRepository,
                            ProjectRepository projectRepository,
                            List<ReportGeneratorPort> generators) {
        this.reportRepository = reportRepository;
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.generators = generators;
    }

    public ReportResponse generateReport(GenerateReportRequest request, UUID userId) {
        SimulationJob job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Simulation job not found: " + request.jobId()));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Cannot generate report for non-completed job, status: " + job.getStatus());
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));

        ReportFormat format = ReportFormat.valueOf(request.format().toUpperCase());

        String title = request.title() != null ? request.title()
                : "RheoSim Report - " + project.getName() + " - " + job.getModelType().name();

        DublinCoreMetadata metadata = DublinCoreMetadata.builder()
                .title(title)
                .creator("RheoSim Enterprise")
                .subject("Viscoelastic parameter identification")
                .description("Simulation results for project: " + project.getName())
                .identifier(job.getId().toString())
                .format(format == ReportFormat.PDF ? "application/pdf" : "text/" + format.name().toLowerCase())
                .build();

        Report report = Report.builder()
                .projectId(request.projectId())
                .jobId(request.jobId())
                .generatedBy(userId)
                .title(title)
                .format(format)
                .metadata(metadata)
                .build();

        report = reportRepository.save(report);

        ReportGeneratorPort generator = generators.stream()
                .filter(g -> g.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No generator for format: " + format));

        try {
            ReportRequest reportRequest = new ReportRequest(
                    report.getId(),
                    title,
                    metadata,
                    project.getName(),
                    job.getModelType().name(),
                    job.getNumberOfBranches(),
                    new double[0],
                    new String[0],
                    0, 0, 0, false,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyMap()
            );

            GenerationResult genResult = generator.generate(reportRequest);
            report.markReady(genResult.filePath(), genResult.fileSize());

        } catch (Exception e) {
            report.markFailed(e.getMessage());
        }

        Report saved = reportRepository.save(report);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listReportsByProject(UUID projectId) {
        return reportRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteReport(UUID reportId, UUID userId) {
        reportRepository.deleteById(reportId);
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getProjectId(),
                report.getJobId(),
                report.getGeneratedBy(),
                report.getTitle(),
                report.getFormat().name(),
                report.getStatus().name(),
                report.getFilePath(),
                report.getFileSize(),
                report.getErrorMessage(),
                report.getCreatedAt()
        );
    }
}
