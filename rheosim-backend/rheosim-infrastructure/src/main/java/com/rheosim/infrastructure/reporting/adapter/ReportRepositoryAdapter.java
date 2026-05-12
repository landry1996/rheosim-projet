package com.rheosim.infrastructure.reporting.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rheosim.domain.reporting.model.DublinCoreMetadata;
import com.rheosim.domain.reporting.model.Report;
import com.rheosim.domain.reporting.model.ReportFormat;
import com.rheosim.domain.reporting.model.ReportStatus;
import com.rheosim.domain.reporting.port.ReportRepository;
import com.rheosim.infrastructure.reporting.entity.ReportJpaEntity;
import com.rheosim.infrastructure.reporting.repository.ReportJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ReportRepositoryAdapter(ReportJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Report save(Report report) {
        ReportJpaEntity entity = toJpaEntity(report);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Report> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Report> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Report> findByGeneratedBy(UUID userId) {
        return jpaRepository.findByGeneratedBy(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ReportJpaEntity toJpaEntity(Report report) {
        ReportJpaEntity entity = new ReportJpaEntity();
        entity.setId(report.getId());
        entity.setProjectId(report.getProjectId());
        entity.setJobId(report.getJobId());
        entity.setGeneratedBy(report.getGeneratedBy());
        entity.setTitle(report.getTitle());
        entity.setFormat(report.getFormat().name());
        entity.setStatus(report.getStatus().name());
        entity.setFilePath(report.getFilePath());
        entity.setFileSize(report.getFileSize());
        entity.setErrorMessage(report.getErrorMessage());
        entity.setCreatedAt(report.getCreatedAt());
        entity.setUpdatedAt(report.getUpdatedAt());

        if (report.getMetadata() != null) {
            try {
                entity.setMetadata(objectMapper.writeValueAsString(report.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize metadata", e);
            }
        }

        return entity;
    }

    private Report toDomain(ReportJpaEntity entity) {
        Report report = Report.builder()
                .projectId(entity.getProjectId())
                .jobId(entity.getJobId())
                .generatedBy(entity.getGeneratedBy())
                .title(entity.getTitle())
                .format(ReportFormat.valueOf(entity.getFormat()))
                .build();

        report.setId(entity.getId());
        report.setStatus(ReportStatus.valueOf(entity.getStatus()));
        report.setFilePath(entity.getFilePath());
        report.setFileSize(entity.getFileSize());
        report.setErrorMessage(entity.getErrorMessage());
        report.setCreatedAt(entity.getCreatedAt());
        report.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            try {
                DublinCoreMetadata metadata = objectMapper.readValue(entity.getMetadata(), DublinCoreMetadata.class);
                report.setMetadata(metadata);
            } catch (JsonProcessingException e) {
                // Continue without metadata if JSON is corrupted
            }
        }

        return report;
    }
}
