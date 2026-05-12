#include "rheosim/tetrahedron_p1.h"
#include <cmath>

namespace rheosim {

double TetrahedronP1::volume(const Eigen::Matrix<double, 4, 3>& coords) {
    Eigen::Vector3d a = coords.row(1) - coords.row(0);
    Eigen::Vector3d b = coords.row(2) - coords.row(0);
    Eigen::Vector3d c = coords.row(3) - coords.row(0);
    return std::abs(a.dot(b.cross(c))) / 6.0;
}

Eigen::Matrix<double, 6, 12> TetrahedronP1::b_matrix(
    const Eigen::Matrix<double, 4, 3>& coords) {

    // Compute shape function gradients for P1 tetrahedron
    // dN/dx, dN/dy, dN/dz for each of 4 nodes
    Eigen::Matrix3d J;
    J.row(0) = coords.row(1) - coords.row(0);
    J.row(1) = coords.row(2) - coords.row(0);
    J.row(2) = coords.row(3) - coords.row(0);

    Eigen::Matrix3d Jinv = J.inverse();

    // Gradients of shape functions in physical coordinates
    // N1 = 1 - xi - eta - zeta, N2 = xi, N3 = eta, N4 = zeta
    Eigen::Matrix<double, 4, 3> dN;
    dN.row(0) = -Jinv.col(0) - Jinv.col(1) - Jinv.col(2);
    dN.row(1) = Jinv.col(0);
    dN.row(2) = Jinv.col(1);
    dN.row(3) = Jinv.col(2);

    // Assemble B matrix (6x12): strain = B * u
    // Voigt: [eps_xx, eps_yy, eps_zz, gamma_xy, gamma_yz, gamma_xz]
    Eigen::Matrix<double, 6, 12> B = Eigen::Matrix<double, 6, 12>::Zero();
    for (int i = 0; i < 4; ++i) {
        int col = 3 * i;
        B(0, col)     = dN(i, 0);  // d/dx
        B(1, col + 1) = dN(i, 1);  // d/dy
        B(2, col + 2) = dN(i, 2);  // d/dz
        B(3, col)     = dN(i, 1);  // d/dy
        B(3, col + 1) = dN(i, 0);  // d/dx
        B(4, col + 1) = dN(i, 2);  // d/dz
        B(4, col + 2) = dN(i, 1);  // d/dy
        B(5, col)     = dN(i, 2);  // d/dz
        B(5, col + 2) = dN(i, 0);  // d/dx
    }
    return B;
}

Eigen::Matrix<double, 6, 6> TetrahedronP1::elasticity_matrix(
    double E, double nu) {

    double factor = E / ((1.0 + nu) * (1.0 - 2.0 * nu));
    Eigen::Matrix<double, 6, 6> D = Eigen::Matrix<double, 6, 6>::Zero();

    D(0, 0) = D(1, 1) = D(2, 2) = factor * (1.0 - nu);
    D(0, 1) = D(0, 2) = D(1, 0) = D(1, 2) = D(2, 0) = D(2, 1) = factor * nu;
    D(3, 3) = D(4, 4) = D(5, 5) = factor * (1.0 - 2.0 * nu) / 2.0;

    return D;
}

Eigen::Matrix<double, 6, 6> TetrahedronP1::viscoelastic_d_matrix(
    double G_inf,
    double poisson_ratio,
    const std::vector<std::pair<double, double>>& prony_branches,
    double dt) {

    // Effective modulus for implicit Euler time integration
    // G_eff = G_inf + sum(G_i * (1 - exp(-dt/tau_i)) * tau_i / dt)
    double G_eff = G_inf;
    for (const auto& [G_i, tau_i] : prony_branches) {
        G_eff += G_i * (1.0 - std::exp(-dt / tau_i)) * tau_i / dt;
    }

    // Convert shear modulus to Young's modulus: E = 2*G*(1+nu)
    double E_eff = 2.0 * G_eff * (1.0 + poisson_ratio);
    return elasticity_matrix(E_eff, poisson_ratio);
}

Eigen::Matrix<double, 12, 12> TetrahedronP1::stiffness_matrix(
    const Eigen::Matrix<double, 4, 3>& coords,
    double E, double nu) {

    double V = volume(coords);
    auto B = b_matrix(coords);
    auto D = elasticity_matrix(E, nu);

    // Ke = V * B^T * D * B (constant strain element)
    return V * B.transpose() * D * B;
}

} // namespace rheosim
