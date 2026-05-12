#pragma once

#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <vector>
#include <string>

namespace rheosim {

struct Mesh {
    Eigen::MatrixXd nodes;        // (num_nodes x 3)
    Eigen::MatrixXi elements;     // (num_elements x 4) for tetrahedra
    int num_nodes;
    int num_elements;

    static Mesh create_beam(double length, double width, double height,
                            int nx, int ny, int nz);
};

struct BoundaryCondition {
    enum Type { DIRICHLET, NEUMANN };
    Type type;
    std::vector<int> node_ids;
    int dof;  // 0=x, 1=y, 2=z
    double value;
};

struct MaterialProps {
    double young_modulus;
    double poisson_ratio;
    double density;
    // Viscoelastic Prony series
    double equilibrium_modulus;
    std::vector<std::pair<double, double>> prony_branches; // (G_i, tau_i)
};

} // namespace rheosim
