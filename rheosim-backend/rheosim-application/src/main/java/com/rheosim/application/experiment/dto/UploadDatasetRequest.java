package com.rheosim.application.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UploadDatasetRequest(
        @NotNull UUID projectId,
        @NotBlank String experimentType,
        Double temperature,
        String temperatureUnit,
        String notes
) {}
