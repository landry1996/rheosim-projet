#pragma once

#include <Eigen/Dense>
#include <string>
#include <vector>

namespace rheosim {

enum class FitTarget {
    RELAXATION_MODULUS,
    CREEP_COMPLIANCE,
    STORAGE_MODULUS,
    LOSS_MODULUS,
    COMPLEX_VISCOSITY
};

class ConstitutiveLaw {
public:
    virtual ~ConstitutiveLaw() = default;

    virtual double compute_relaxation_modulus(double time, const Eigen::VectorXd& params) const = 0;
    virtual double compute_creep_compliance(double time, const Eigen::VectorXd& params) const = 0;
    virtual double compute_storage_modulus(double omega, const Eigen::VectorXd& params) const = 0;
    virtual double compute_loss_modulus(double omega, const Eigen::VectorXd& params) const = 0;
    virtual double compute_complex_viscosity(double omega, const Eigen::VectorXd& params) const = 0;

    virtual int parameter_count() const = 0;
    virtual std::vector<std::string> parameter_names() const = 0;
    virtual Eigen::VectorXd default_lower_bounds() const = 0;
    virtual Eigen::VectorXd default_upper_bounds() const = 0;

    double evaluate(FitTarget target, double x, const Eigen::VectorXd& params) const {
        switch (target) {
            case FitTarget::RELAXATION_MODULUS: return compute_relaxation_modulus(x, params);
            case FitTarget::CREEP_COMPLIANCE: return compute_creep_compliance(x, params);
            case FitTarget::STORAGE_MODULUS: return compute_storage_modulus(x, params);
            case FitTarget::LOSS_MODULUS: return compute_loss_modulus(x, params);
            case FitTarget::COMPLEX_VISCOSITY: return compute_complex_viscosity(x, params);
        }
        return 0.0;
    }
};

} // namespace rheosim
