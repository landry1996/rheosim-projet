package com.rheosim.infrastructure.privacy.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rheosim.domain.privacy.port.DataExportPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DataExportService implements DataExportPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public ExportData collectUserData(UUID userId) {
        Object userProfile = jdbcTemplate.queryForMap(
                "SELECT id, email, first_name, last_name, created_at FROM users WHERE id = ?",
                userId);

        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
                "SELECT id, name, description, created_at, updated_at FROM projects WHERE user_id = ?",
                userId);

        List<Map<String, Object>> materials = jdbcTemplate.queryForList(
                "SELECT m.id, m.name, m.family, m.grade, m.created_at FROM materials m " +
                        "JOIN projects p ON m.project_id = p.id WHERE p.user_id = ?",
                userId);

        List<Map<String, Object>> simulations = jdbcTemplate.queryForList(
                "SELECT s.id, s.type, s.status, s.created_at, s.completed_at FROM simulations s " +
                        "JOIN projects p ON s.project_id = p.id WHERE p.user_id = ?",
                userId);

        List<Map<String, Object>> reports = jdbcTemplate.queryForList(
                "SELECT r.id, r.name, r.format, r.created_at FROM reports r " +
                        "JOIN projects p ON r.project_id = p.id WHERE p.user_id = ?",
                userId);

        List<Map<String, Object>> consents = jdbcTemplate.queryForList(
                "SELECT type, granted, granted_at, revoked_at FROM user_consents WHERE user_id = ?",
                userId);

        return new ExportData(userProfile, projects, materials, simulations, reports, consents);
    }

    @Override
    public byte[] generateExportZip(ExportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            addJsonEntry(zos, "user_profile.json", data.userProfile());
            addJsonEntry(zos, "projects.json", data.projects());
            addJsonEntry(zos, "materials.json", data.materials());
            addJsonEntry(zos, "simulations.json", data.simulations());
            addJsonEntry(zos, "reports.json", data.reports());
            addJsonEntry(zos, "consents.json", data.consents());

            Map<String, Object> metadata = Map.of(
                    "export_date", Instant.now().toString(),
                    "format_version", "1.0",
                    "gdpr_article", "Article 15 & 20 - Right of access & Data portability"
            );
            addJsonEntry(zos, "metadata.json", metadata);

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate export ZIP", e);
        }
    }

    @Override
    public String uploadExport(byte[] zipBytes, UUID userId) {
        // In production: upload to S3/MinIO with pre-signed URL (48h expiry)
        String filename = "export_" + userId + "_" + Instant.now().toEpochMilli() + ".zip";
        return "/api/v1/privacy/export/download/" + filename;
    }

    @Override
    public void deleteExpiredExports() {
        // Clean up exports older than 48 hours from object storage
    }

    private void addJsonEntry(ZipOutputStream zos, String name, Object data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        byte[] json = objectMapper.writeValueAsBytes(data);
        zos.write(json);
        zos.closeEntry();
    }
}
