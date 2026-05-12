#include "rheosim/prony_series_law.h"
#include <cmath>
#include <sstream>

namespace rheosim {

PronySeriesLaw::PronySeriesLaw(int num_branches) : num_branches_(num_branches) {}

double PronySeriesLaw::compute_relaxation_modulus(double time, const Eigen::VectorXd& params) const {
    double g_inf = params(0);
    double result = g_inf;
    for (int i = 0; i < num_branches_; ++i) {
        double g_i = params(1 + 2 * i);
        double tau_i = params(2 + 2 * i);
        result += g_i * std::exp(-time / tau_i);
    }
    return result;
}

double PronySeriesLaw::compute_creep_compliance(double time, const Eigen::VectorXd& params) const {
    // Approximate: J(t) ≈ 1/G(t) for small deformations
    double g = compute_relaxation_modulus(time, params);
    return (g > 0) ? 1.0 / g : 0.0;
}

double PronySeriesLaw::compute_storage_modulus(double omega, const Eigen::VectorXd& params) const {
    double g_inf = params(0);
    double result = g_inf;
    for (int i = 0; i < num_branches_; ++i) {
        double g_i = params(1 + 2 * i);
        double tau_i = params(2 + 2 * i);
        double wt = omega * tau_i;
        result += g_i * wt * wt / (1.0 + wt * wt);
    }
    return result;
}

double PronySeriesLaw::compute_loss_modulus(double omega, const Eigen::VectorXd& params) const {
    double result = 0.0;
    for (int i = 0; i < num_branches_; ++i) {
        double g_i = params(1 + 2 * i);
        double tau_i = params(2 + 2 * i);
        double wt = omega * tau_i;
        result += g_i * wt / (1.0 + wt * wt);
    }
    return result;
}

double PronySeriesLaw::compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const {
    double gp = compute_storage_modulus(omega, params);
    double gpp = compute_loss_modulus(omega, params);
    return std::sqrt(gp * gp + gpp * gpp) / omega;
}

std::vector<std::string> PronySeriesLaw::parameter_names() const {
    std::vector<std::string> names;
    names.push_back("G_inf");
    for (int i = 0; i < num_branches_; ++i) {
        names.push_back("G_" + std::to_string(i + 1));
        names.push_back("tau_" + std::to_string(i + 1));
    }
    return names;
}

Eigen::VectorXd PronySeriesLaw::default_lower_bounds() const {
    Eigen::VectorXd lb = Eigen::VectorXd::Constant(parameter_count(), 1e-3);
    return lb;
}

Eigen::VectorXd PronySeriesLaw::default_upper_bounds() const {
    int n = parameter_count();
    Eigen::VectorXd ub(n);
    ub(0) = 1e12; // G_inf
    for (int i = 0; i < num_branches_; ++i) {
        ub(1 + 2 * i) = 1e12;  // G_i
        ub(2 + 2 * i) = 1e6;   // tau_i
    }
    return ub;
}

} // namespace rheosim
