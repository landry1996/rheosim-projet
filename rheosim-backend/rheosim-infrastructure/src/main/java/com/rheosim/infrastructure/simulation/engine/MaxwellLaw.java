package com.rheosim.infrastructure.simulation.engine;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import org.springframework.stereotype.Component;

@Component
public class MaxwellLaw implements ConstitutiveLaw {

    @Override
    public ConstitutiveModelType getModelType() {
        return ConstitutiveModelType.MAXWELL;
    }

    @Override
    public double computeRelaxationModulus(double time, double[] parameters) {
        // G(t) = G * exp(-t / tau)
        double g = parameters[0];
        double tau = parameters[1];
        return g * Math.exp(-time / tau);
    }

    @Override
    public double computeCreepCompliance(double time, double[] parameters) {
        // J(t) = (1/G) * (1 + t/tau)
        double g = parameters[0];
        double tau = parameters[1];
        return (1.0 / g) * (1.0 + time / tau);
    }

    @Override
    public double computeStorageModulus(double omega, double[] parameters) {
        // G'(omega) = G * (omega*tau)^2 / (1 + (omega*tau)^2)
        double g = parameters[0];
        double tau = parameters[1];
        double wt = omega * tau;
        double wt2 = wt * wt;
        return g * wt2 / (1.0 + wt2);
    }

    @Override
    public double computeLossModulus(double omega, double[] parameters) {
        // G''(omega) = G * omega*tau / (1 + (omega*tau)^2)
        double g = parameters[0];
        double tau = parameters[1];
        double wt = omega * tau;
        double wt2 = wt * wt;
        return g * wt / (1.0 + wt2);
    }

    @Override
    public double computeComplexViscosity(double omega, double[] parameters) {
        double gPrime = computeStorageModulus(omega, parameters);
        double gDoublePrime = computeLossModulus(omega, parameters);
        return Math.sqrt(gPrime * gPrime + gDoublePrime * gDoublePrime) / omega;
    }

    @Override
    public String[] getParameterNames() {
        return new String[]{"G (Pa)", "tau (s)"};
    }

    @Override
    public double[] getDefaultParameters() {
        return new double[]{1000.0, 1.0};
    }

    @Override
    public double[] getLowerBounds() {
        return new double[]{1e-3, 1e-10};
    }

    @Override
    public double[] getUpperBounds() {
        return new double[]{1e12, 1e6};
    }

    @Override
    public int getParameterCount(int numberOfBranches) {
        return 2;
    }
}
