package com.rheosim.application.project.dto;

import java.util.List;

public record MaterialModelResponse(
        String type,
        int numberOfBranches,
        double equilibriumModulus,
        List<BranchResponse> branches,
        double referenceTemperatureK
) {
    public record BranchResponse(double modulus, double relaxationTime) {}
}
