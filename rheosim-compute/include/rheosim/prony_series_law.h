#pragma once

#include "rheosim/constitutive_law.h"

namespace rheosim {

class PronySeriesLaw : public ConstitutiveLaw {
public:
    explicit PronySeriesLaw(int num_branches);

    // params = [G_inf, G_1, tau_1, G_2, tau_2, ...]
    double compute_relaxation_modulus(double time, const Eigen::VectorXd& params) const override;
    double compute_creep_compliance(double time, const Eigen::VectorXd& params) const override;
    double compute_storage_modulus(double omega, const Eigen::VectorXd& params) const override;
    double compute_loss_modulus(double omega, const Eigen::VectorXd& params) const override;
    double compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const override;

    int parameter_count() const override { return 1 + 2 * num_branches_; }
    std::vector<std::string> parameter_names() const override;
    Eigen::VectorXd default_lower_bounds() const override;
    Eigen::VectorXd default_upper_bounds() const override;

    int num_branches() const { return num_branches_; }

private:
    int num_branches_;
};

} // namespace rheosim
