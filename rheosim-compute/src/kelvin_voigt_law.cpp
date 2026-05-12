#include "rheosim/kelvin_voigt_law.h"
#include <cmath>

namespace rheosim {

double KelvinVoigtLaw::compute_relaxation_modulus(double /*time*/, const Eigen::VectorXd& params) const {
    // KV element doesn't relax in pure form; returns spring constant
    return params(0);
}

double KelvinVoigtLaw::compute_creep_compliance(double time, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    return (1.0 / G) * (1.0 - std::exp(-time / tau));
}

double KelvinVoigtLaw::compute_storage_modulus(double /*omega*/, const Eigen::VectorXd& params) const {
    return params(0);
}

double KelvinVoigtLaw::compute_loss_modulus(double omega, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    return G * omega * tau;
}

double KelvinVoigtLaw::compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const {
    double gp = compute_storage_modulus(omega, params);
    double gpp = compute_loss_modulus(omega, params);
    return std::sqrt(gp * gp + gpp * gpp) / omega;
}

Eigen::VectorXd KelvinVoigtLaw::default_lower_bounds() const {
    Eigen::VectorXd lb(2);
    lb << 1e-3, 1e-10;
    return lb;
}

Eigen::VectorXd KelvinVoigtLaw::default_upper_bounds() const {
    Eigen::VectorXd ub(2);
    ub << 1e12, 1e6;
    return ub;
}

} // namespace rheosim
