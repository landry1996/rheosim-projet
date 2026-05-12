#pragma once

#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <vector>
#include <functional>
#include "rheosim/mesh.h"

namespace rheosim {

struct FEMResult {
    std::vector<Eigen::VectorXd> displacement_history;  // per time step
    std::vector<Eigen::VectorXd> stress_history;
    std::vector<double> time_points;
    double max_displacement;
    double max_stress;
    bool success;
    std::string error_message;
};

struct SolverSettings {
    double time_step = 0.1;
    int num_steps = 10;
    double convergence_tolerance = 1e-8;
    int max_iterations = 100;
    bool use_iterative_solver = false;
};

using ProgressCallback = std::function<void(int step, int total, double time,
                                            double max_disp, double max_stress)>;

class FEMSolver {
public:
    FEMResult solve(
        const Mesh& mesh,
        const MaterialProps& material,
        const std::vector<BoundaryCondition>& bcs,
        const SolverSettings& settings,
        ProgressCallback progress = nullptr
    ) const;

private:
    Eigen::SparseMatrix<double> assemble_global_stiffness(
        const Mesh& mesh, double young_modulus, double poisson_ratio) const;

    Eigen::VectorXd assemble_force_vector(
        const Mesh& mesh,
        const std::vector<BoundaryCondition>& neumann_bcs,
        int total_dofs) const;

    void apply_dirichlet(
        Eigen::SparseMatrix<double>& K,
        Eigen::VectorXd& F,
        const std::vector<BoundaryCondition>& dirichlet_bcs) const;

    Eigen::VectorXd solve_linear_system(
        const Eigen::SparseMatrix<double>& K,
        const Eigen::VectorXd& F,
        bool use_iterative) const;

    Eigen::VectorXd compute_stress_field(
        const Mesh& mesh,
        const Eigen::VectorXd& displacement,
        double young_modulus,
        double poisson_ratio) const;
};

} // namespace rheosim
