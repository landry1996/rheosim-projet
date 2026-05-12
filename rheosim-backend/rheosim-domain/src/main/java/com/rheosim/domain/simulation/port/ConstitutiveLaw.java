package com.rheosim.domain.simulation.port;

import com.rheosim.domain.project.model.ConstitutiveModelType;

public interface ConstitutiveLaw {

    ConstitutiveModelType getModelType();

    double computeRelaxationModulus(double time, double[] parameters);

    double computeCreepCompliance(double time, double[] parameters);

    double computeStorageModulus(double omega, double[] parameters);

    double computeLossModulus(double omega, double[] parameters);

    double computeComplexViscosity(double omega, double[] parameters);

    String[] getParameterNames();

    double[] getDefaultParameters();

    double[] getLowerBounds();

    double[] getUpperBounds();

    int getParameterCount(int numberOfBranches);
}
