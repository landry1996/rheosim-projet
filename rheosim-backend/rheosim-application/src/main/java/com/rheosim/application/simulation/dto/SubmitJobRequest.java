package com.rheosim.application.simulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitJobRequest(
        @NotNull UUID projectId,
        @NotNull UUID datasetId,
        UUID materialId,
        @NotBlank String simulationType,
        @NotBlank String modelType,
        @Min(1) @Max(20) int numberOfBranches
) {}
