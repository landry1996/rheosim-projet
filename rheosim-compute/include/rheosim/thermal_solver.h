#pragma once

#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <vector>
#include <functional>
#include "rheosim/mesh.h"

namespace rheosim {

struct ThermalBoundaryCondition {
    enum Type { DIRICHLET, NEUMANN, ROBIN };
    Type type;
    std::vector<int> node_ids;
    double value;
    double convection_coeff;  // for Robin: h (W/m²·K)
    double ambient_temp;      // for Robin: T_inf
};

struct ThermalMaterialProps {
    double density;             // kg/m³
    double specific_heat;       // J/(kg·K)
    double conductivity;        // W/(m·K)
    double thermal_expansion;   // 1/K (alpha)
    double reference_temp;      // K
};

struct ThermalSolverSettings {
    double time_step = 0.1;
    int num_steps = 100;
    double initial_temperature = 293.15;  // 20°C in Kelvin
    bool use_iterative_solver = false;
};

struct ThermalResult {
    std::vector<Eigen::VectorXd> temperature_history;
    std::vector<double> time_points;
    double max_temperature;
    double min_temperature;
    bool success;
    std::string error_message;
};

using ThermalProgressCallback = std::function<void(int step, int total, double time,
                                                    double max_temp, double min_temp)>;

class ThermalSolver {
public:
    ThermalResult solve(
        const Mesh& mesh,
        const ThermalMaterialProps& material,
        const std::vector<ThermalBoundaryCondition>& bcs,
        const ThermalSolverSettings& settings,
        const Eigen::VectorXd& heat_source,
        ThermalProgressCallback progress = nullptr
    ) const;

private:
    Eigen::SparseMatrix<double> assemble_capacity_matrix(
        const Mesh& mesh, double rho_cp) const;

    Eigen::SparseMatrix<double> assemble_conductivity_matrix(
        const Mesh& mesh, double conductivity) const;

    void apply_robin_bc(
        Eigen::SparseMatrix<double>& K,
        Eigen::VectorXd& F,
        const std::vector<ThermalBoundaryCondition>& robin_bcs) const;

    void apply_thermal_dirichlet(
        Eigen::SparseMatrix<double>& K,
        Eigen::VectorXd& F,
        const std::vector<ThermalBoundaryCondition>& dirichlet_bcs) const;
};

} // namespace rheosim
