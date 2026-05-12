#pragma once

#include <Eigen/Dense>
#include "rheosim/constitutive_law.h"

namespace rheosim {

struct IdentificationResult {
    bool converged;
    Eigen::VectorXd parameters;
    double r_squared;
    int iterations;
    Eigen::VectorXd fitted_curve;
};

struct IdentificationOptions {
    int max_iterations = 1000;
    double tolerance = 1e-12;
    double lambda_init = 1e-3;
    double lambda_factor = 10.0;
    double jacobian_step = 1e-8;
};

class LevenbergMarquardt {
public:
    IdentificationResult identify(
        const ConstitutiveLaw& law,
        FitTarget target,
        const Eigen::VectorXd& x_data,
        const Eigen::VectorXd& y_data,
        const Eigen::VectorXd& initial_guess,
        const Eigen::VectorXd& lower_bounds,
        const Eigen::VectorXd& upper_bounds,
        const IdentificationOptions& options = {}
    ) const;

private:
    Eigen::VectorXd compute_residuals(
        const ConstitutiveLaw& law, FitTarget target,
        const Eigen::VectorXd& x, const Eigen::VectorXd& y,
        const Eigen::VectorXd& params) const;

    Eigen::MatrixXd compute_jacobian(
        const ConstitutiveLaw& law, FitTarget target,
        const Eigen::VectorXd& x, const Eigen::VectorXd& params,
        double step) const;

    Eigen::VectorXd clamp_params(
        const Eigen::VectorXd& params,
        const Eigen::VectorXd& lower,
        const Eigen::VectorXd& upper) const;

    double compute_r_squared(
        const Eigen::VectorXd& y_data,
        const Eigen::VectorXd& residuals) const;
};

} // namespace rheosim
