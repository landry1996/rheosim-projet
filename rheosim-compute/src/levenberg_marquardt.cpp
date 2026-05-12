#include "rheosim/levenberg_marquardt.h"
#include <cmath>
#include <algorithm>

namespace rheosim {

IdentificationResult LevenbergMarquardt::identify(
    const ConstitutiveLaw& law,
    FitTarget target,
    const Eigen::VectorXd& x_data,
    const Eigen::VectorXd& y_data,
    const Eigen::VectorXd& initial_guess,
    const Eigen::VectorXd& lower_bounds,
    const Eigen::VectorXd& upper_bounds,
    const IdentificationOptions& options
) const {
    IdentificationResult result;
    result.converged = false;
    result.iterations = 0;

    Eigen::VectorXd params = clamp_params(initial_guess, lower_bounds, upper_bounds);
    double lambda = options.lambda_init;

    Eigen::VectorXd residuals = compute_residuals(law, target, x_data, y_data, params);
    double cost = residuals.squaredNorm();

    for (int iter = 0; iter < options.max_iterations; ++iter) {
        result.iterations = iter + 1;

        Eigen::MatrixXd J = compute_jacobian(law, target, x_data, params, options.jacobian_step);
        Eigen::MatrixXd JtJ = J.transpose() * J;
        Eigen::VectorXd Jtr = J.transpose() * residuals;

        // Damped normal equations: (J^T*J + lambda*diag(J^T*J)) * dp = -J^T*r
        Eigen::MatrixXd A = JtJ + lambda * JtJ.diagonal().asDiagonal().toDenseMatrix();
        Eigen::VectorXd dp = A.ldlt().solve(-Jtr);

        Eigen::VectorXd new_params = clamp_params(params + dp, lower_bounds, upper_bounds);
        Eigen::VectorXd new_residuals = compute_residuals(law, target, x_data, y_data, new_params);
        double new_cost = new_residuals.squaredNorm();

        if (new_cost < cost) {
            params = new_params;
            residuals = new_residuals;

            double relative_change = std::abs(cost - new_cost) / (cost + 1e-30);
            cost = new_cost;
            lambda /= options.lambda_factor;

            if (relative_change < options.tolerance) {
                result.converged = true;
                break;
            }
        } else {
            lambda *= options.lambda_factor;
        }
    }

    result.parameters = params;
    result.r_squared = compute_r_squared(y_data, residuals);

    // Compute fitted curve
    result.fitted_curve.resize(x_data.size());
    for (int i = 0; i < x_data.size(); ++i) {
        result.fitted_curve(i) = law.evaluate(target, x_data(i), params);
    }

    return result;
}

Eigen::VectorXd LevenbergMarquardt::compute_residuals(
    const ConstitutiveLaw& law, FitTarget target,
    const Eigen::VectorXd& x, const Eigen::VectorXd& y,
    const Eigen::VectorXd& params) const {

    Eigen::VectorXd r(x.size());
    for (int i = 0; i < x.size(); ++i) {
        r(i) = y(i) - law.evaluate(target, x(i), params);
    }
    return r;
}

Eigen::MatrixXd LevenbergMarquardt::compute_jacobian(
    const ConstitutiveLaw& law, FitTarget target,
    const Eigen::VectorXd& x, const Eigen::VectorXd& params,
    double step) const {

    int n = static_cast<int>(x.size());
    int p = static_cast<int>(params.size());
    Eigen::MatrixXd J(n, p);

    for (int j = 0; j < p; ++j) {
        double h = std::max(step * std::abs(params(j)), step);
        Eigen::VectorXd p_plus = params;
        Eigen::VectorXd p_minus = params;
        p_plus(j) += h;
        p_minus(j) -= h;

        for (int i = 0; i < n; ++i) {
            double f_plus = law.evaluate(target, x(i), p_plus);
            double f_minus = law.evaluate(target, x(i), p_minus);
            // Jacobian of residual: d(y - f)/dp = -df/dp
            J(i, j) = -(f_plus - f_minus) / (2.0 * h);
        }
    }
    return J;
}

Eigen::VectorXd LevenbergMarquardt::clamp_params(
    const Eigen::VectorXd& params,
    const Eigen::VectorXd& lower,
    const Eigen::VectorXd& upper) const {

    Eigen::VectorXd clamped = params;
    for (int i = 0; i < params.size(); ++i) {
        clamped(i) = std::clamp(params(i), lower(i), upper(i));
    }
    return clamped;
}

double LevenbergMarquardt::compute_r_squared(
    const Eigen::VectorXd& y_data,
    const Eigen::VectorXd& residuals) const {

    double ss_res = residuals.squaredNorm();
    double y_mean = y_data.mean();
    double ss_tot = (y_data.array() - y_mean).square().sum();
    if (ss_tot < 1e-30) return 0.0;
    return 1.0 - ss_res / ss_tot;
}

} // namespace rheosim
