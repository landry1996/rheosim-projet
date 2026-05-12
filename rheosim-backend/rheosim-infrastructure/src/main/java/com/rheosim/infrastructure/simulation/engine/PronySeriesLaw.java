package com.rheosim.infrastructure.simulation.engine;

import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import org.springframework.stereotype.Component;

@Component
public class PronySeriesLaw implements ConstitutiveLaw {

    @Override
    public ConstitutiveModelType getModelType() {
        return ConstitutiveModelType.PRONY;
    }

    @Override
    public double computeRelaxationModulus(double time, double[] parameters) {
        // G(t) = G_inf + sum_i( G_i * exp(-t / tau_i) )
        // parameters layout: [G_inf, G_1, tau_1, G_2, tau_2, ..., G_n, tau_n]
        double gInf = parameters[0];
        double result = gInf;
        int n = (parameters.length - 1) / 2;
        for (int i = 0; i < n; i++) {
            double gi = parameters[1 + 2 * i];
            double taui = parameters[2 + 2 * i];
            result += gi * Math.exp(-time / taui);
        }
        return result;
    }

    @Override
    public double computeCreepCompliance(double time, double[] parameters) {
        // Approximate via Laplace transform inversion
        // J(t) ≈ 1/G(0) + sum_i (1/G_i) * (1 - exp(-t * G_i / eta_i))
        // Simplified: J(t) ≈ 1/G(t) for estimation purposes
        double gt = computeRelaxationModulus(time, parameters);
        return (gt > 0) ? 1.0 / gt : Double.MAX_VALUE;
    }

    @Override
    public double computeStorageModulus(double omega, double[] parameters) {
        // G'(omega) = G_inf + sum_i( G_i * (omega*tau_i)^2 / (1 + (omega*tau_i)^2) )
        double gInf = parameters[0];
        double result = gInf;
        int n = (parameters.length - 1) / 2;
        for (int i = 0; i < n; i++) {
            double gi = parameters[1 + 2 * i];
            double taui = parameters[2 + 2 * i];
            double wt = omega * taui;
            double wt2 = wt * wt;
            result += gi * wt2 / (1.0 + wt2);
        }
        return result;
    }

    @Override
    public double computeLossModulus(double omega, double[] parameters) {
        // G''(omega) = sum_i( G_i * omega*tau_i / (1 + (omega*tau_i)^2) )
        double result = 0.0;
        int n = (parameters.length - 1) / 2;
        for (int i = 0; i < n; i++) {
            double gi = parameters[1 + 2 * i];
            double taui = parameters[2 + 2 * i];
            double wt = omega * taui;
            double wt2 = wt * wt;
            result += gi * wt / (1.0 + wt2);
        }
        return result;
    }

    @Override
    public double computeComplexViscosity(double omega, double[] parameters) {
        double gPrime = computeStorageModulus(omega, parameters);
        double gDoublePrime = computeLossModulus(omega, parameters);
        return Math.sqrt(gPrime * gPrime + gDoublePrime * gDoublePrime) / omega;
    }

    @Override
    public String[] getParameterNames() {
        return new String[]{"G_inf (Pa)", "G_1 (Pa)", "tau_1 (s)"};
    }

    @Override
    public double[] getDefaultParameters() {
        return new double[]{100.0, 1000.0, 1.0};
    }

    @Override
    public double[] getLowerBounds() {
        return new double[]{0.0, 1e-3, 1e-10};
    }

    @Override
    public double[] getUpperBounds() {
        return new double[]{1e12, 1e12, 1e6};
    }

    @Override
    public int getParameterCount(int numberOfBranches) {
        // G_inf + N * (G_i, tau_i)
        return 1 + 2 * numberOfBranches;
    }
}
