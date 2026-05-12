package com.rheosim.domain.reporting.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Report Entity")
class ReportTest {

    @Test
    @DisplayName("Builder creates report with GENERATING status")
    void builder_shouldCreateReportWithGeneratingStatus() {
        Report report = Report.builder()
                .projectId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .generatedBy(UUID.randomUUID())
                .title("Test Report")
                .format(ReportFormat.PDF)
                .build();

        assertThat(report.getId()).isNotNull();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(report.getFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("markReady sets file path and size")
    void markReady_shouldSetFileInfo() {
        Report report = createTestReport();
        report.markReady("/data/reports/abc.pdf", 12345);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(report.getFilePath()).isEqualTo("/data/reports/abc.pdf");
        assertThat(report.getFileSize()).isEqualTo(12345);
    }

    @Test
    @DisplayName("markFailed sets error message")
    void markFailed_shouldSetErrorMessage() {
        Report report = createTestReport();
        report.markFailed("PDF generation timeout");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getErrorMessage()).isEqualTo("PDF generation timeout");
    }

    @Test
    @DisplayName("markExpired transitions to EXPIRED")
    void markExpired_shouldTransitionToExpired() {
        Report report = createTestReport();
        report.markReady("/path", 100);
        report.markExpired();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.EXPIRED);
    }

    private Report createTestReport() {
        return Report.builder()
                .projectId(UUID.randomUUID())
                .jobId(UUID.randomUUID())
                .generatedBy(UUID.randomUUID())
                .title("Simulation Report")
                .format(ReportFormat.PDF)
                .build();
    }
}
