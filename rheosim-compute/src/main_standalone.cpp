#include "rheosim/maxwell_law.h"
#include "rheosim/prony_series_law.h"
#include "rheosim/levenberg_marquardt.h"
#include "rheosim/fem_solver.h"
#include "rheosim/mesh.h"
#include <iostream>
#include <iomanip>
#include <cmath>

using namespace rheosim;

void demo_identification() {
    std::cout << "=== Parameter Identification Demo ===" << std::endl;

    MaxwellLaw law;
    double true_G = 5000.0;
    double true_tau = 2.0;
    Eigen::VectorXd true_params(2);
    true_params << true_G, true_tau;

    // Generate synthetic data
    int n = 50;
    Eigen::VectorXd time(n), data(n);
    for (int i = 0; i < n; ++i) {
        time(i) = 0.01 + i * 0.2;
        data(i) = law.compute_relaxation_modulus(time(i), true_params);
    }

    // Initial guess
    Eigen::VectorXd guess(2);
    guess << 1000.0, 1.0;

    LevenbergMarquardt lm;
    auto result = lm.identify(law, FitTarget::RELAXATION_MODULUS,
                              time, data, guess,
                              law.default_lower_bounds(),
                              law.default_upper_bounds());

    std::cout << "Converged: " << (result.converged ? "YES" : "NO") << std::endl;
    std::cout << "R²: " << std::fixed << std::setprecision(8) << result.r_squared << std::endl;
    std::cout << "Iterations: " << result.iterations << std::endl;
    std::cout << "G = " << result.parameters(0) << " (true: " << true_G << ")" << std::endl;
    std::cout << "tau = " << result.parameters(1) << " (true: " << true_tau << ")" << std::endl;
    std::cout << std::endl;
}

void demo_fem() {
    std::cout << "=== FEM 3D Beam Demo ===" << std::endl;

    // Create a simple beam mesh (4x1x1 elements)
    auto mesh = Mesh::create_beam(1.0, 0.1, 0.1, 4, 1, 1);
    std::cout << "Mesh: " << mesh.num_nodes << " nodes, "
              << mesh.num_elements << " elements" << std::endl;

    // Material: steel-like
    MaterialProps material;
    material.young_modulus = 200e9;  // 200 GPa
    material.poisson_ratio = 0.3;
    material.density = 7800;

    // BCs: fixed at x=0, force at x=L
    std::vector<BoundaryCondition> bcs;

    // Find nodes at x=0 (fixed end)
    std::vector<int> fixed_nodes;
    for (int i = 0; i < mesh.num_nodes; ++i) {
        if (std::abs(mesh.nodes(i, 0)) < 1e-10) {
            fixed_nodes.push_back(i);
        }
    }
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 0, 0.0}); // ux=0
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 1, 0.0}); // uy=0
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 2, 0.0}); // uz=0

    // Find nodes at x=L (loaded end)
    std::vector<int> loaded_nodes;
    for (int i = 0; i < mesh.num_nodes; ++i) {
        if (std::abs(mesh.nodes(i, 0) - 1.0) < 1e-10) {
            loaded_nodes.push_back(i);
        }
    }
    // Apply downward force
    double total_force = -1000.0; // -1 kN in z direction
    double force_per_node = total_force / static_cast<double>(loaded_nodes.size());
    bcs.push_back({BoundaryCondition::NEUMANN, loaded_nodes, 2, force_per_node});

    SolverSettings settings;
    settings.num_steps = 1;
    settings.use_iterative_solver = false;

    FEMSolver solver;
    auto result = solver.solve(mesh, material, bcs, settings);

    if (result.success) {
        std::cout << "Solution converged!" << std::endl;
        std::cout << "Max displacement: " << std::scientific << result.max_displacement << " m" << std::endl;
        std::cout << "Max Von Mises stress: " << result.max_stress << " Pa" << std::endl;

        // Analytical solution for cantilever beam:
        // delta_max = F*L^3 / (3*E*I)
        double L = 1.0, b = 0.1, h = 0.1;
        double I = b * h * h * h / 12.0;
        double delta_analytical = std::abs(total_force) * L * L * L / (3.0 * material.young_modulus * I);
        std::cout << "Analytical max deflection: " << delta_analytical << " m" << std::endl;
    } else {
        std::cout << "Solver failed: " << result.error_message << std::endl;
    }
}

int main() {
    demo_identification();
    demo_fem();
    return 0;
}
