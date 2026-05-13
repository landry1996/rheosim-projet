#include "rheosim/thermal_solver.h"
#include "rheosim/tetrahedron_p1.h"
#include <Eigen/SparseLU>
#include <Eigen/IterativeLinearSolvers>

#ifdef RHEOSIM_USE_OPENMP
#include <omp.h>
#endif

namespace rheosim {

ThermalResult ThermalSolver::solve(
    const Mesh& mesh,
    const ThermalMaterialProps& material,
    const std::vector<ThermalBoundaryCondition>& bcs,
    const ThermalSolverSettings& settings,
    const Eigen::VectorXd& heat_source,
    ThermalProgressCallback progress
) const {
    ThermalResult result;
    result.success = false;
    int n_nodes = mesh.num_nodes;

    double rho_cp = material.density * material.specific_heat;

    // Assemble matrices
    Eigen::SparseMatrix<double> C = assemble_capacity_matrix(mesh, rho_cp);
    Eigen::SparseMatrix<double> K_th = assemble_conductivity_matrix(mesh, material.conductivity);

    // Separate BCs
    std::vector<ThermalBoundaryCondition> dirichlet_bcs, neumann_bcs, robin_bcs;
    for (const auto& bc : bcs) {
        switch (bc.type) {
            case ThermalBoundaryCondition::DIRICHLET: dirichlet_bcs.push_back(bc); break;
            case ThermalBoundaryCondition::NEUMANN: neumann_bcs.push_back(bc); break;
            case ThermalBoundaryCondition::ROBIN: robin_bcs.push_back(bc); break;
        }
    }

    // Initial temperature
    Eigen::VectorXd T_n = Eigen::VectorXd::Constant(n_nodes, settings.initial_temperature);

    // Apply initial Dirichlet BCs
    for (const auto& bc : dirichlet_bcs) {
        for (int node_id : bc.node_ids) {
            T_n(node_id) = bc.value;
        }
    }

    double dt = settings.time_step;

    // Implicit Euler: (C/dt + K_th) * T^{n+1} = C/dt * T^n + Q
    Eigen::SparseMatrix<double> A = C * (1.0 / dt) + K_th;

    for (int step = 0; step < settings.num_steps; ++step) {
        double current_time = (step + 1) * dt;

        // RHS: C/dt * T^n + heat_source
        Eigen::VectorXd rhs = (C * (1.0 / dt)) * T_n;

        // Add volumetric heat source
        if (heat_source.size() == n_nodes) {
            rhs += heat_source;
        }

        // Add Neumann flux
        for (const auto& bc : neumann_bcs) {
            for (int node_id : bc.node_ids) {
                rhs(node_id) += bc.value;
            }
        }

        // Apply Robin BCs (convection)
        Eigen::SparseMatrix<double> A_step = A;
        apply_robin_bc(A_step, rhs, robin_bcs);

        // Apply Dirichlet BCs
        apply_thermal_dirichlet(A_step, rhs, dirichlet_bcs);

        // Solve
        Eigen::VectorXd T_new;
        if (settings.use_iterative_solver) {
            Eigen::BiCGSTAB<Eigen::SparseMatrix<double>> solver;
            solver.setTolerance(1e-10);
            solver.compute(A_step);
            T_new = solver.solve(rhs);
        } else {
            Eigen::SparseLU<Eigen::SparseMatrix<double>> solver;
            solver.compute(A_step);
            T_new = solver.solve(rhs);
        }

        T_n = T_new;

        double max_T = T_new.maxCoeff();
        double min_T = T_new.minCoeff();

        result.temperature_history.push_back(T_new);
        result.time_points.push_back(current_time);
        result.max_temperature = max_T;
        result.min_temperature = min_T;

        if (progress) {
            progress(step + 1, settings.num_steps, current_time, max_T, min_T);
        }
    }

    result.success = true;
    return result;
}

Eigen::SparseMatrix<double> ThermalSolver::assemble_capacity_matrix(
    const Mesh& mesh, double rho_cp) const {

    int n = mesh.num_nodes;
    std::vector<Eigen::Triplet<double>> triplets;
    triplets.reserve(mesh.num_elements * 16);

    for (int e = 0; e < mesh.num_elements; ++e) {
        Eigen::Matrix<double, 4, 3> coords;
        for (int i = 0; i < 4; ++i) {
            coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
        }

        double volume = TetrahedronP1::compute_volume(coords);

        // Lumped mass: M_ii = rho_cp * V / 4
        double m_diag = rho_cp * volume / 4.0;
        for (int i = 0; i < 4; ++i) {
            int gi = mesh.elements(e, i);
            triplets.emplace_back(gi, gi, m_diag);
        }
    }

    Eigen::SparseMatrix<double> C(n, n);
    C.setFromTriplets(triplets.begin(), triplets.end());
    return C;
}

Eigen::SparseMatrix<double> ThermalSolver::assemble_conductivity_matrix(
    const Mesh& mesh, double conductivity) const {

    int n = mesh.num_nodes;
    std::vector<Eigen::Triplet<double>> triplets;
    triplets.reserve(mesh.num_elements * 16);

    #ifdef RHEOSIM_USE_OPENMP
    #pragma omp parallel
    {
        std::vector<Eigen::Triplet<double>> local_triplets;
        local_triplets.reserve(mesh.num_elements * 16 / omp_get_num_threads());

        #pragma omp for nowait
        for (int e = 0; e < mesh.num_elements; ++e) {
            Eigen::Matrix<double, 4, 3> coords;
            for (int i = 0; i < 4; ++i) {
                coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
            }

            double volume = TetrahedronP1::compute_volume(coords);
            auto gradN = TetrahedronP1::shape_function_gradients(coords);

            // K_e(i,j) = k * V * gradN_i . gradN_j
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 4; ++j) {
                    double val = conductivity * volume * gradN.row(i).dot(gradN.row(j));
                    if (std::abs(val) > 1e-20) {
                        int gi = mesh.elements(e, i);
                        int gj = mesh.elements(e, j);
                        local_triplets.emplace_back(gi, gj, val);
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

        double volume = TetrahedronP1::compute_volume(coords);
        auto gradN = TetrahedronP1::shape_function_gradients(coords);

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                double val = conductivity * volume * gradN.row(i).dot(gradN.row(j));
                if (std::abs(val) > 1e-20) {
                    int gi = mesh.elements(e, i);
                    int gj = mesh.elements(e, j);
                    triplets.emplace_back(gi, gj, val);
                }
            }
        }
    }
    #endif

    Eigen::SparseMatrix<double> K(n, n);
    K.setFromTriplets(triplets.begin(), triplets.end());
    return K;
}

void ThermalSolver::apply_robin_bc(
    Eigen::SparseMatrix<double>& K,
    Eigen::VectorXd& F,
    const std::vector<ThermalBoundaryCondition>& robin_bcs) const {

    for (const auto& bc : robin_bcs) {
        for (int node_id : bc.node_ids) {
            K.coeffRef(node_id, node_id) += bc.convection_coeff;
            F(node_id) += bc.convection_coeff * bc.ambient_temp;
        }
    }
}

void ThermalSolver::apply_thermal_dirichlet(
    Eigen::SparseMatrix<double>& K,
    Eigen::VectorXd& F,
    const std::vector<ThermalBoundaryCondition>& dirichlet_bcs) const {

    double penalty = 1e30;
    for (const auto& bc : dirichlet_bcs) {
        for (int node_id : bc.node_ids) {
            K.coeffRef(node_id, node_id) += penalty;
            F(node_id) = penalty * bc.value;
        }
    }
}

} // namespace rheosim
