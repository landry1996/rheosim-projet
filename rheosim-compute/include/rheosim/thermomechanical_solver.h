#pragma once

#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <vector>
#include <functional>
#include <memory>
#include "rheosim/mesh.h"
#include "rheosim/thermal_solver.h"
#include "rheosim/tts_model.h"

namespace rheosim {

struct ThermoMechanicalMaterial {
    // Mechanical
    double young_modulus;
    double poisson_ratio;
    double density;
    double equilibrium_modulus;
    std::vector<std::pair<double, double>> prony_branches;

    // Thermal
    double specific_heat;
    double conductivity;
    double thermal_expansion;
    double reference_temp;
};

struct ThermoMechanicalSettings {
    double time_step = 0.1;
    int num_steps = 100;
    double initial_temperature = 293.15;
    int coupling_iterations = 1;  // 1 = staggered (weak), >1 = iterative (strong)
    double coupling_tolerance = 1e-6;
    bool use_iterative_solver = false;
    bool compute_dissipation = true;
};

struct ThermoMechanicalResult {
    std::vector<Eigen::VectorXd> displacement_history;
    std::vector<Eigen::VectorXd> temperature_history;
    std::vector<Eigen::VectorXd> stress_history;
    std::vector<double> time_points;
    double max_displacement;
    double max_stress;
    double max_temperature;
    double min_temperature;
    bool success;
    std::string error_message;
};

using TMProgressCallback = std::function<void(int step, int total, double time,
                                               double max_disp, double max_temp)>;

class ThermoMechanicalSolver {
public:
    ThermoMechanicalSolver(std::unique_ptr<TTSModel> tts_model = nullptr);

    ThermoMechanicalResult solve(
        const Mesh& mesh,
        const ThermoMechanicalMaterial& material,
        const std::vector<BoundaryCondition>& mech_bcs,
        const std::vector<ThermalBoundaryCondition>& thermal_bcs,
        const ThermoMechanicalSettings& settings,
        TMProgressCallback progress = nullptr
    ) const;

private:
    std::unique_ptr<TTSModel> tts_model_;

    Eigen::VectorXd compute_thermal_force(
        const Mesh& mesh,
        const Eigen::VectorXd& temperature,
        double alpha, double E, double nu, double T_ref) const;

    Eigen::VectorXd compute_viscous_dissipation(
        const Mesh& mesh,
        const Eigen::VectorXd& displacement,
        const Eigen::VectorXd& prev_displacement,
        const Eigen::VectorXd& stress,
        double dt) const;
};

} // namespace rheosim
