package com.rheosim.infrastructure.reporting.adapter;

import com.rheosim.domain.reporting.model.DublinCoreMetadata;
import com.rheosim.domain.reporting.model.ReportFormat;
import com.rheosim.domain.reporting.port.ReportGeneratorPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;

@Component
public class PdfReportGeneratorAdapter implements ReportGeneratorPort {

    private final Path reportsDir;

    public PdfReportGeneratorAdapter(@Value("${rheosim.storage.path:./data/uploads}") String storagePath) {
        this.reportsDir = Path.of(storagePath, "reports");
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create reports directory", e);
        }
    }

    @Override
    public boolean supports(ReportFormat format) {
        return format == ReportFormat.PDF;
    }

    @Override
    public GenerationResult generate(ReportRequest request) {
        Path outputPath = reportsDir.resolve(request.reportId() + ".pdf");

        try (PDDocument document = new PDDocument()) {
            setDocumentMetadata(document, request.metadata());

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 750;
                float margin = 50;

                // Title
                content.beginText();
                content.setFont(fontBold, 18);
                content.newLineAtOffset(margin, y);
                content.showText("RheoSim Enterprise - Simulation Report");
                content.endText();
                y -= 30;

                // Subtitle
                content.beginText();
                content.setFont(fontRegular, 12);
                content.newLineAtOffset(margin, y);
                content.showText(request.title());
                content.endText();
                y -= 20;

                // Separator line
                content.moveTo(margin, y);
                content.lineTo(545, y);
                content.stroke();
                y -= 30;

                // Project info
                y = writeLine(content, fontBold, fontRegular, margin, y, "Project:", request.projectName());
                y = writeLine(content, fontBold, fontRegular, margin, y, "Model:", request.modelType());
                y = writeLine(content, fontBold, fontRegular, margin, y, "Branches:", String.valueOf(request.numberOfBranches()));
                y -= 10;

                // Results section
                content.beginText();
                content.setFont(fontBold, 14);
                content.newLineAtOffset(margin, y);
                content.showText("Identification Results");
                content.endText();
                y -= 25;

                y = writeLine(content, fontBold, fontRegular, margin, y, "Converged:", request.converged() ? "Yes" : "No");
                y = writeLine(content, fontBold, fontRegular, margin, y, "R²:", String.format("%.6f", request.rSquared()));
                y = writeLine(content, fontBold, fontRegular, margin, y, "Residual Norm:", String.format("%.4e", request.residualNorm()));
                y = writeLine(content, fontBold, fontRegular, margin, y, "Iterations:", String.valueOf(request.iterations()));
                y -= 10;

                // Parameters section
                if (request.identifiedParameters() != null && request.identifiedParameters().length > 0) {
                    content.beginText();
                    content.setFont(fontBold, 14);
                    content.newLineAtOffset(margin, y);
                    content.showText("Identified Parameters");
                    content.endText();
                    y -= 25;

                    for (int i = 0; i < request.identifiedParameters().length; i++) {
                        String name = (request.parameterNames() != null && i < request.parameterNames().length)
                                ? request.parameterNames()[i] : "p" + (i + 1);
                        y = writeLine(content, fontBold, fontRegular, margin, y,
                                name + ":", String.format("%.6e", request.identifiedParameters()[i]));

                        if (y < 100) {
                            // New page if running out of space
                            break;
                        }
                    }
                }

                // Metrics
                if (request.metrics() != null && !request.metrics().isEmpty()) {
                    y -= 10;
                    content.beginText();
                    content.setFont(fontBold, 14);
                    content.newLineAtOffset(margin, y);
                    content.showText("Metrics");
                    content.endText();
                    y -= 25;

                    for (var entry : request.metrics().entrySet()) {
                        y = writeLine(content, fontBold, fontRegular, margin, y,
                                entry.getKey() + ":", String.format("%.6e", entry.getValue()));
                        if (y < 80) break;
                    }
                }

                // Footer
                content.beginText();
                content.setFont(fontRegular, 8);
                content.newLineAtOffset(margin, 30);
                content.showText("Generated by RheoSim Enterprise | " + request.metadata().date().toString());
                content.endText();
            }

            document.save(outputPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        }

        try {
            long fileSize = Files.size(outputPath);
            return new GenerationResult(outputPath.toString(), fileSize);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read generated PDF size", e);
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font bold, PDType1Font regular,
                            float x, float y, String label, String value) throws IOException {
        content.beginText();
        content.setFont(bold, 10);
        content.newLineAtOffset(x, y);
        content.showText(label + " ");
        content.setFont(regular, 10);
        content.showText(value);
        content.endText();
        return y - 18;
    }

    private void setDocumentMetadata(PDDocument document, DublinCoreMetadata metadata) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(metadata.title());
        info.setAuthor(metadata.creator());
        info.setSubject(metadata.subject());
        info.setProducer("RheoSim Enterprise v0.1.0");
        info.setCreator("RheoSim Enterprise");
        Calendar cal = GregorianCalendar.getInstance();
        info.setCreationDate(cal);
        info.setModificationDate(cal);
    }
}
