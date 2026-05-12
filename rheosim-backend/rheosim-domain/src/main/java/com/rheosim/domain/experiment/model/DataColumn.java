package com.rheosim.domain.experiment.model;

public record DataColumn(
        String name,
        String unit,
        PhysicalQuantity quantity
) {
    public enum PhysicalQuantity {
        TIME,
        FREQUENCY,
        SHEAR_RATE,
        SHEAR_STRESS,
        VISCOSITY,
        STORAGE_MODULUS,
        LOSS_MODULUS,
        COMPLEX_VISCOSITY,
        STRAIN,
        TEMPERATURE,
        COMPLIANCE,
        OTHER
    }
}
