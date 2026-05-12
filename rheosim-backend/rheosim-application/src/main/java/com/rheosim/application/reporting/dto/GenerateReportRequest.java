package com.rheosim.application.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateReportRequest(
        @NotNull UUID projectId,
        @NotNull UUID jobId,
        @NotBlank String format,
        String title
) {}
