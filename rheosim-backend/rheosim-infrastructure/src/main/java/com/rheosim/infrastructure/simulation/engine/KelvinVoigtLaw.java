package com.rheosim.infrastructure.simulation.engine;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import org.springframework.stereotype.Component;

@Component
public class KelvinVoigtLaw implements ConstitutiveLaw {

    @Override
    public ConstitutiveModelType getModelType() {
        return ConstitutiveModelType.KELVIN_VOIGT;
    }

    @Override
    public double computeRelaxationModulus(double time, double[] parameters) {
        // Kelvin-Voigt: G(t) = G + eta * delta(t) — not well-defined for continuous relaxation
        // Approximation: G(t) = G (constant, since the element doesn't relax)
        double g = parameters[0];
        return g;
    }

    @Override
    public double computeCreepCompliance(double time, double[] parameters) {
        // J(t) = (1/G) * (1 - exp(-t / tau)) where tau = eta/G
        double g = parameters[0];
        double eta = parameters[1];
        double tau = eta / g;
        return (1.0 / g) * (1.0 - Math.exp(-time / tau));
    }

    @Override
    public double computeStorageModulus(double omega, double[] parameters) {
        // G'(omega) = G
        return parameters[0];
    }

    @Override
    public double computeLossModulus(double omega, double[] parameters) {
        // G''(omega) = eta * omega
        double eta = parameters[1];
        return eta * omega;
    }

    @Override
    public double computeComplexViscosity(double omega, double[] parameters) {
        double gPrime = computeStorageModulus(omega, parameters);
        double gDoublePrime = computeLossModulus(omega, parameters);
        return Math.sqrt(gPrime * gPrime + gDoublePrime * gDoublePrime) / omega;
    }

    @Override
    public String[] getParameterNames() {
        return new String[]{"G (Pa)", "eta (Pa.s)"};
    }

    @Override
    public double[] getDefaultParameters() {
        return new double[]{1000.0, 100.0};
    }

    @Override
    public double[] getLowerBounds() {
        return new double[]{1e-3, 1e-6};
    }

    @Override
    public double[] getUpperBounds() {
        return new double[]{1e12, 1e12};
    }

    @Override
    public int getParameterCount(int numberOfBranches) {
        return 2;
    }
}
