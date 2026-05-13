#pragma once

#include <Eigen/Dense>

namespace rheosim {

class TetrahedronP1 {
public:
    // Compute element stiffness matrix (12x12) for a linear tetrahedron
    static Eigen::Matrix<double, 12, 12> stiffness_matrix(
        const Eigen::Matrix<double, 4, 3>& node_coords,
        double young_modulus,
        double poisson_ratio);

    // Compute element volume
    static double volume(const Eigen::Matrix<double, 4, 3>& node_coords);

    // Alias used by thermal solver
    static double compute_volume(const Eigen::Matrix<double, 4, 3>& node_coords) {
        return volume(node_coords);
    }

    // Compute shape function gradients (4x3 matrix, each row is grad(N_i))
    static Eigen::Matrix<double, 4, 3> shape_function_gradients(
        const Eigen::Matrix<double, 4, 3>& node_coords);

    // Compute B matrix (strain-displacement, 6x12)
    static Eigen::Matrix<double, 6, 12> b_matrix(
        const Eigen::Matrix<double, 4, 3>& node_coords);

    // Compute elasticity matrix D (6x6) for isotropic material
    static Eigen::Matrix<double, 6, 6> elasticity_matrix(
        double young_modulus, double poisson_ratio);

    // Compute viscoelastic effective D for time integration
    static Eigen::Matrix<double, 6, 6> viscoelastic_d_matrix(
        double equilibrium_modulus,
        double poisson_ratio,
        const std::vector<std::pair<double, double>>& prony_branches,
        double dt);
};

} // namespace rheosim
