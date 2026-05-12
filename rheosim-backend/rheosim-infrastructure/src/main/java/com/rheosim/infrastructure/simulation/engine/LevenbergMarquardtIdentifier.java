package com.rheosim.infrastructure.simulation.engine;

import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort;
import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem.Evaluation;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;

@Component
public class LevenbergMarquardtIdentifier implements ParameterIdentificationPort {

    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_EVALUATIONS = 5000;
    private static final double COST_TOLERANCE = 1e-12;
    private static final double PARAM_TOLERANCE = 1e-12;

    @Override
    public SimulationResult identify(ConstitutiveLaw law,
                                     double[] xData,
                                     double[] yData,
                                     double[] initialGuess,
                                     double[] lowerBounds,
                                     double[] upperBounds,
                                     FitTarget target) {
        BiFunction<Double, double[], Double> modelFunction = getModelFunction(law, target);

        MultivariateJacobianFunction jacobianFunction = point -> {
            double[] params = point.toArray();
            int n = xData.length;
            int p = params.length;

            double[] values = new double[n];
            double[][] jacobian = new double[n][p];

            for (int i = 0; i < n; i++) {
                values[i] = modelFunction.apply(xData[i], params);

                // Numerical differentiation (central differences)
                for (int j = 0; j < p; j++) {
                    double h = Math.max(1e-8, Math.abs(params[j]) * 1e-6);
                    double[] paramsPlus = params.clone();
                    double[] paramsMinus = params.clone();
                    paramsPlus[j] += h;
                    paramsMinus[j] -= h;

                    // Enforce bounds
                    paramsPlus[j] = Math.min(paramsPlus[j], upperBounds[j]);
                    paramsMinus[j] = Math.max(paramsMinus[j], lowerBounds[j]);

                    double fPlus = modelFunction.apply(xData[i], paramsPlus);
                    double fMinus = modelFunction.apply(xData[i], paramsMinus);
                    jacobian[i][j] = (fPlus - fMinus) / (paramsPlus[j] - paramsMinus[j]);
                }
            }

            RealVector residuals = new ArrayRealVector(values);
            RealMatrix jMatrix = org.apache.commons.math3.linear.MatrixUtils.createRealMatrix(jacobian);
            return new Pair<>(residuals, jMatrix);
        };

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(initialGuess)
                .model(jacobianFunction)
                .target(yData)
                .maxIterations(MAX_ITERATIONS)
                .maxEvaluations(MAX_EVALUATIONS)
                .checker(createChecker())
                .build();

        try {
            LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);

            double[] identifiedParams = optimum.getPoint().toArray();

            // Enforce bounds on final result
            for (int i = 0; i < identifiedParams.length; i++) {
                identifiedParams[i] = Math.max(lowerBounds[i], Math.min(upperBounds[i], identifiedParams[i]));
            }

            double residualNorm = optimum.getResiduals().getNorm();
            double rSquared = computeRSquared(xData, yData, identifiedParams, modelFunction);

            List<double[]> fittedCurve = computeFittedCurve(xData, identifiedParams, modelFunction);

            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("residualNorm", residualNorm);
            metrics.put("rSquared", rSquared);
            metrics.put("iterations", (double) optimum.getIterations());
            metrics.put("evaluations", (double) optimum.getEvaluations());

            return new SimulationResult(
                    identifiedParams,
                    law.getParameterNames(),
                    residualNorm,
                    rSquared,
                    optimum.getIterations(),
                    true,
                    fittedCurve,
                    metrics
            );
        } catch (Exception e) {
            return new SimulationResult(
                    initialGuess,
                    law.getParameterNames(),
                    Double.MAX_VALUE,
                    0.0,
                    MAX_ITERATIONS,
                    false,
                    Collections.emptyList(),
                    Map.of("error", -1.0)
            );
        }
    }

    private BiFunction<Double, double[], Double> getModelFunction(ConstitutiveLaw law, FitTarget target) {
        return switch (target) {
            case RELAXATION_MODULUS -> law::computeRelaxationModulus;
            case CREEP_COMPLIANCE -> law::computeCreepCompliance;
            case STORAGE_MODULUS -> law::computeStorageModulus;
            case LOSS_MODULUS -> law::computeLossModulus;
            case COMPLEX_VISCOSITY -> law::computeComplexViscosity;
        };
    }

    private ConvergenceChecker<Evaluation> createChecker() {
        return (iteration, previous, current) -> {
            double prevCost = previous.getResiduals().getNorm();
            double currCost = current.getResiduals().getNorm();
            if (Math.abs(prevCost - currCost) < COST_TOLERANCE) return true;

            double[] prevParams = previous.getPoint().toArray();
            double[] currParams = current.getPoint().toArray();
            double paramDiff = 0;
            for (int i = 0; i < prevParams.length; i++) {
                paramDiff += Math.abs(prevParams[i] - currParams[i]);
            }
            return paramDiff < PARAM_TOLERANCE;
        };
    }

    private double computeRSquared(double[] xData, double[] yData, double[] params,
                                   BiFunction<Double, double[], Double> modelFunction) {
        double yMean = 0;
        for (double y : yData) yMean += y;
        yMean /= yData.length;

        double ssRes = 0;
        double ssTot = 0;
        for (int i = 0; i < yData.length; i++) {
            double predicted = modelFunction.apply(xData[i], params);
            ssRes += (yData[i] - predicted) * (yData[i] - predicted);
            ssTot += (yData[i] - yMean) * (yData[i] - yMean);
        }
        return (ssTot > 0) ? 1.0 - (ssRes / ssTot) : 0.0;
    }

    private List<double[]> computeFittedCurve(double[] xData, double[] params,
                                              BiFunction<Double, double[], Double> modelFunction) {
        List<double[]> curve = new ArrayList<>();
        for (double x : xData) {
            curve.add(new double[]{x, modelFunction.apply(x, params)});
        }
        return curve;
    }
}
