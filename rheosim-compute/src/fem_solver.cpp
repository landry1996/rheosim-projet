#include "rheosim/fem_solver.h"
#include "rheosim/tetrahedron_p1.h"
#include <Eigen/SparseLU>
#include <Eigen/IterativeLinearSolvers>
#include <vector>

#ifdef RHEOSIM_USE_OPENMP
#include <omp.h>
#endif

namespace rheosim {

FEMResult FEMSolver::solve(
    const Mesh& mesh,
    const MaterialProps& material,
    const std::vector<BoundaryCondition>& bcs,
    const SolverSettings& settings,
    ProgressCallback progress
) const {
    FEMResult result;
    result.success = false;
    int total_dofs = mesh.num_nodes * 3;

    // Separate BCs
    std::vector<BoundaryCondition> dirichlet_bcs, neumann_bcs;
    for (const auto& bc : bcs) {
        if (bc.type == BoundaryCondition::DIRICHLET)
            dirichlet_bcs.push_back(bc);
        else
            neumann_bcs.push_back(bc);
    }

    // Assemble global stiffness
    Eigen::SparseMatrix<double> K = assemble_global_stiffness(
        mesh, material.young_modulus, material.poisson_ratio);

    // Force vector
    Eigen::VectorXd F = assemble_force_vector(mesh, neumann_bcs, total_dofs);

    // Apply Dirichlet BCs
    apply_dirichlet(K, F, dirichlet_bcs);

    // Time stepping (for viscoelastic: multiple steps; for elastic: single step)
    int num_steps = (material.prony_branches.empty()) ? 1 : settings.num_steps;
    double dt = settings.time_step;

    for (int step = 0; step < num_steps; ++step) {
        double current_time = (step + 1) * dt;

        // For viscoelastic: reassemble with effective modulus
        if (!material.prony_branches.empty() && step > 0) {
            auto D_eff = TetrahedronP1::viscoelastic_d_matrix(
                material.equilibrium_modulus, material.poisson_ratio,
                material.prony_branches, dt);
            // Simplified: use effective E from D_eff diagonal
            double E_eff = D_eff(0, 0) * (1.0 + material.poisson_ratio) *
                           (1.0 - 2.0 * material.poisson_ratio) /
                           (1.0 - material.poisson_ratio);
            K = assemble_global_stiffness(mesh, E_eff, material.poisson_ratio);
            apply_dirichlet(K, F, dirichlet_bcs);
        }

        // Solve
        Eigen::VectorXd u = solve_linear_system(K, F, settings.use_iterative_solver);

        // Compute stresses
        Eigen::VectorXd stress = compute_stress_field(
            mesh, u, material.young_modulus, material.poisson_ratio);

        double max_disp = u.cwiseAbs().maxCoeff();
        double max_stress_val = stress.cwiseAbs().maxCoeff();

        result.displacement_history.push_back(u);
        result.stress_history.push_back(stress);
        result.time_points.push_back(current_time);
        result.max_displacement = max_disp;
        result.max_stress = max_stress_val;

        if (progress) {
            progress(step + 1, num_steps, current_time, max_disp, max_stress_val);
        }
    }

    result.success = true;
    return result;
}

Eigen::SparseMatrix<double> FEMSolver::assemble_global_stiffness(
    const Mesh& mesh, double E, double nu) const {

    int total_dofs = mesh.num_nodes * 3;
    std::vector<Eigen::Triplet<double>> triplets;
    triplets.reserve(mesh.num_elements * 144); // 12*12 per element

    #ifdef RHEOSIM_USE_OPENMP
    #pragma omp parallel
    {
        std::vector<Eigen::Triplet<double>> local_triplets;
        local_triplets.reserve(mesh.num_elements * 144 / omp_get_num_threads());

        #pragma omp for nowait
        for (int e = 0; e < mesh.num_elements; ++e) {
            Eigen::Matrix<double, 4, 3> coords;
            for (int i = 0; i < 4; ++i) {
                coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
            }

            auto Ke = TetrahedronP1::stiffness_matrix(coords, E, nu);

            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 4; ++j) {
                    int gi = mesh.elements(e, i) * 3;
                    int gj = mesh.elements(e, j) * 3;
                    for (int di = 0; di < 3; ++di) {
                        for (int dj = 0; dj < 3; ++dj) {
                            double val = Ke(3 * i + di, 3 * j + dj);
                            if (std::abs(val) > 1e-20) {
                                local_triplets.emplace_back(gi + di, gj + dj, val);
                            }
                        }
                    }
                }
            }
        }

        #pragma omp critical
        triplets.insert(triplets.end(), local_triplets.begin(), local_triplets.end());
    }
    #else
    for (int e = 0; e < mesh.num_elements; ++e) {
        Eigen::Matrix<double, 4, 3> coords;
        for (int i = 0; i < 4; ++i) {
            coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
        }

        auto Ke = TetrahedronP1::stiffness_matrix(coords, E, nu);

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                int gi = mesh.elements(e, i) * 3;
                int gj = mesh.elements(e, j) * 3;
                for (int di = 0; di < 3; ++di) {
                    for (int dj = 0; dj < 3; ++dj) {
                        double val = Ke(3 * i + di, 3 * j + dj);
                        if (std::abs(val) > 1e-20) {
                            triplets.emplace_back(gi + di, gj + dj, val);
                        }
                    }
                }
            }
        }
    }
    #endif

    Eigen::SparseMatrix<double> K(total_dofs, total_dofs);
    K.setFromTriplets(triplets.begin(), triplets.end());
    return K;
}

Eigen::VectorXd FEMSolver::assemble_force_vector(
    const Mesh& /*mesh*/,
    const std::vector<BoundaryCondition>& neumann_bcs,
    int total_dofs) const {

    Eigen::VectorXd F = Eigen::VectorXd::Zero(total_dofs);
    for (const auto& bc : neumann_bcs) {
        for (int node_id : bc.node_ids) {
            F(node_id * 3 + bc.dof) += bc.value;
        }
    }
    return F;
}

void FEMSolver::apply_dirichlet(
    Eigen::SparseMatrix<double>& K,
    Eigen::VectorXd& F,
    const std::vector<BoundaryCondition>& dirichlet_bcs) const {

    double penalty = 1e30;
    for (const auto& bc : dirichlet_bcs) {
        for (int node_id : bc.node_ids) {
            int dof = node_id * 3 + bc.dof;
            K.coeffRef(dof, dof) += penalty;
            F(dof) = penalty * bc.value;
        }
    }
}

Eigen::VectorXd FEMSolver::solve_linear_system(
    const Eigen::SparseMatrix<double>& K,
    const Eigen::VectorXd& F,
    bool use_iterative) const {

    if (use_iterative) {
        Eigen::BiCGSTAB<Eigen::SparseMatrix<double>> solver;
        solver.setTolerance(1e-10);
        solver.compute(K);
        return solver.solve(F);
    } else {
        Eigen::SparseLU<Eigen::SparseMatrix<double>> solver;
        solver.compute(K);
        return solver.solve(F);
    }
}

Eigen::VectorXd FEMSolver::compute_stress_field(
    const Mesh& mesh,
    const Eigen::VectorXd& displacement,
    double E, double nu) const {

    // Von Mises stress per element
    Eigen::VectorXd stress(mesh.num_elements);
    auto D = TetrahedronP1::elasticity_matrix(E, nu);

    for (int e = 0; e < mesh.num_elements; ++e) {
        Eigen::Matrix<double, 4, 3> coords;
        for (int i = 0; i < 4; ++i) {
            coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
        }

        auto B = TetrahedronP1::b_matrix(coords);

        // Element displacement vector
        Eigen::Matrix<double, 12, 1> ue;
        for (int i = 0; i < 4; ++i) {
            int node = mesh.elements(e, i);
            ue.segment<3>(3 * i) = displacement.segment<3>(node * 3);
        }

        // Stress = D * B * u
        Eigen::Matrix<double, 6, 1> sigma = D * B * ue;

        // Von Mises
        double s11 = sigma(0), s22 = sigma(1), s33 = sigma(2);
        double s12 = sigma(3), s23 = sigma(4), s13 = sigma(5);
        double vm = std::sqrt(0.5 * ((s11 - s22) * (s11 - s22) +
                                      (s22 - s33) * (s22 - s33) +
                                      (s33 - s11) * (s33 - s11) +
                                      6.0 * (s12 * s12 + s23 * s23 + s13 * s13)));
        stress(e) = vm;
    }
    return stress;
}

} // namespace rheosim
