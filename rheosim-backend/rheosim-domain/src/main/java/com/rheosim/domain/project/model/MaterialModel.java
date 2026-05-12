package com.rheosim.domain.project.model;

import java.util.List;

public record MaterialModel(
        ConstitutiveModelType type,
        int numberOfBranches,
        double equilibriumModulus,
        List<PronyBranch> branches,
        double referenceTemperatureK
) {
    public record PronyBranch(double modulus, double relaxationTime) {
        public PronyBranch {
            if (modulus < 0) throw new IllegalArgumentException("Modulus must be non-negative");
            if (relaxationTime <= 0) throw new IllegalArgumentException("Relaxation time must be positive");
        }
    }

    public MaterialModel {
        if (type == null) throw new IllegalArgumentException("Model type is required");
        if (equilibriumModulus < 0) throw new IllegalArgumentException("Equilibrium modulus must be non-negative");
        if (referenceTemperatureK <= 0) throw new IllegalArgumentException("Reference temperature must be positive (Kelvin)");
    }
}
