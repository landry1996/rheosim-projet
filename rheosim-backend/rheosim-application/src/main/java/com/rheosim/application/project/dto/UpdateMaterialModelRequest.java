package com.rheosim.application.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateMaterialModelRequest(
        @NotNull String type,
        @Positive double equilibriumModulus,
        List<BranchRequest> branches,
        @Positive double referenceTemperatureK
) {
    public record BranchRequest(
            @Positive double modulus,
            @Positive double relaxationTime
    ) {}
}
