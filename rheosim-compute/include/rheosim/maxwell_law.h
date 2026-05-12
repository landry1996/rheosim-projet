#pragma once

#include "rheosim/constitutive_law.h"

namespace rheosim {

class MaxwellLaw : public ConstitutiveLaw {
public:
    // params = [G (Pa), tau (s)]
    double compute_relaxation_modulus(double time, const Eigen::VectorXd& params) const override;
    double compute_creep_compliance(double time, const Eigen::VectorXd& params) const override;
    double compute_storage_modulus(double omega, const Eigen::VectorXd& params) const override;
    double compute_loss_modulus(double omega, const Eigen::VectorXd& params) const override;
    double compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const override;

    int parameter_count() const override { return 2; }
    std::vector<std::string> parameter_names() const override { return {"G", "tau"}; }
    Eigen::VectorXd default_lower_bounds() const override;
    Eigen::VectorXd default_upper_bounds() const override;
};

} // namespace rheosim
