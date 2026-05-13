#pragma once

#include <cmath>
#include <vector>
#include <utility>

namespace rheosim {

class TTSModel {
public:
    virtual ~TTSModel() = default;
    virtual double shift_factor(double T, double T_ref) const = 0;

    std::vector<std::pair<double, double>> shift_prony_branches(
        const std::vector<std::pair<double, double>>& branches,
        double T,
        double T_ref
    ) const {
        double aT = shift_factor(T, T_ref);
        std::vector<std::pair<double, double>> shifted;
        shifted.reserve(branches.size());
        for (const auto& [G_i, tau_i] : branches) {
            shifted.emplace_back(G_i, tau_i * aT);
        }
        return shifted;
    }
};

class WLFModel : public TTSModel {
public:
    WLFModel(double C1, double C2) : c1_(C1), c2_(C2) {}

    double shift_factor(double T, double T_ref) const override {
        double dT = T - T_ref;
        double log_aT = -c1_ * dT / (c2_ + dT);
        return std::pow(10.0, log_aT);
    }

private:
    double c1_;
    double c2_;
};

class ArrheniusModel : public TTSModel {
public:
    ArrheniusModel(double activation_energy, double gas_constant = 8.314)
        : ea_(activation_energy), R_(gas_constant) {}

    double shift_factor(double T, double T_ref) const override {
        double log_aT = (ea_ / R_) * (1.0 / T - 1.0 / T_ref);
        return std::pow(10.0, log_aT / std::log(10.0));
    }

private:
    double ea_;
    double R_;
};

} // namespace rheosim
