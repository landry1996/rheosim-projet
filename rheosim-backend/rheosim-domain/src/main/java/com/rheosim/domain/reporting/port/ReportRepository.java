package com.rheosim.domain.reporting.port;

import com.rheosim.domain.reporting.model.Report;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

    Report save(Report report);

    Optional<Report> findById(UUID id);

    List<Report> findByProjectId(UUID projectId);

    List<Report> findByGeneratedBy(UUID userId);

    void deleteById(UUID id);
}
