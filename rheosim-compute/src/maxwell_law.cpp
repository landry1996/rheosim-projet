#include "rheosim/maxwell_law.h"
#include <cmath>

namespace rheosim {

double MaxwellLaw::compute_relaxation_modulus(double time, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    return G * std::exp(-time / tau);
}

double MaxwellLaw::compute_creep_compliance(double time, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    return (1.0 / G) + time / (G * tau);
}

double MaxwellLaw::compute_storage_modulus(double omega, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    double wt = omega * tau;
    return G * wt * wt / (1.0 + wt * wt);
}

double MaxwellLaw::compute_loss_modulus(double omega, const Eigen::VectorXd& params) const {
    double G = params(0);
    double tau = params(1);
    double wt = omega * tau;
    return G * wt / (1.0 + wt * wt);
}

double MaxwellLaw::compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const {
    double gp = compute_storage_modulus(omega, params);
    double gpp = compute_loss_modulus(omega, params);
    return std::sqrt(gp * gp + gpp * gpp) / omega;
}

Eigen::VectorXd MaxwellLaw::default_lower_bounds() const {
    Eigen::VectorXd lb(2);
    lb << 1e-3, 1e-10;
    return lb;
}

Eigen::VectorXd MaxwellLaw::default_upper_bounds() const {
    Eigen::VectorXd ub(2);
    ub << 1e12, 1e6;
    return ub;
}

} // namespace rheosim
