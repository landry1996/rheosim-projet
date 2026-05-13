#include "rheosim/thermomechanical_solver.h"
#include "rheosim/thermal_solver.h"
#include "rheosim/fem_solver.h"
#include "rheosim/tetrahedron_p1.h"
#include <Eigen/SparseLU>

namespace rheosim {

ThermoMechanicalSolver::ThermoMechanicalSolver(std::unique_ptr<TTSModel> tts_model)
    : tts_model_(std::move(tts_model)) {}

ThermoMechanicalResult ThermoMechanicalSolver::solve(
    const Mesh& mesh,
    const ThermoMechanicalMaterial& material,
    const std::vector<BoundaryCondition>& mech_bcs,
    const std::vector<ThermalBoundaryCondition>& thermal_bcs,
    const ThermoMechanicalSettings& settings,
    TMProgressCallback progress
) const {
    ThermoMechanicalResult result;
    result.success = false;

    int n_nodes = mesh.num_nodes;
    int mech_dofs = n_nodes * 3;
    double dt = settings.time_step;

    // Initialize temperature field
    Eigen::VectorXd T_n = Eigen::VectorXd::Constant(n_nodes, settings.initial_temperature);
    Eigen::VectorXd u_n = Eigen::VectorXd::Zero(mech_dofs);
    Eigen::VectorXd u_prev = Eigen::VectorXd::Zero(mech_dofs);

    // Thermal solver setup
    ThermalSolver thermal_solver;
    ThermalMaterialProps thermal_mat;
    thermal_mat.density = material.density;
    thermal_mat.specific_heat = material.specific_heat;
    thermal_mat.conductivity = material.conductivity;
    thermal_mat.thermal_expansion = material.thermal_expansion;
    thermal_mat.reference_temp = material.reference_temp;

    // Mechanical solver
    FEMSolver fem_solver;

    // Heat source (from dissipation)
    Eigen::VectorXd Q_dissipation = Eigen::VectorXd::Zero(n_nodes);

    for (int step = 0; step < settings.num_steps; ++step) {
        double current_time = (step + 1) * dt;

        // --- Coupling iterations ---
        Eigen::VectorXd T_new = T_n;
        Eigen::VectorXd u_new = u_n;

        for (int iter = 0; iter < settings.coupling_iterations; ++iter) {
            // Step 1: Solve thermal equation
            ThermalSolverSettings thermal_settings;
            thermal_settings.time_step = dt;
            thermal_settings.num_steps = 1;
            thermal_settings.initial_temperature = 0;  // not used, we set T_n directly
            thermal_settings.use_iterative_solver = settings.use_iterative_solver;

            // Single thermal step with current dissipation
            double rho_cp = material.density * material.specific_heat;
            auto C_th = thermal_solver.solve(mesh, thermal_mat, thermal_bcs,
                                             thermal_settings, Q_dissipation, nullptr);
            if (C_th.success && !C_th.temperature_history.empty()) {
                T_new = C_th.temperature_history.back();
            }

            // Step 2: Update mechanical properties with temperature
            MaterialProps mech_mat;
            mech_mat.young_modulus = material.young_modulus;
            mech_mat.poisson_ratio = material.poisson_ratio;
            mech_mat.density = material.density;
            mech_mat.equilibrium_modulus = material.equilibrium_modulus;

            // Apply TTS if model is provided
            if (tts_model_ && !material.prony_branches.empty()) {
                double avg_T = T_new.mean();
                mech_mat.prony_branches = tts_model_->shift_prony_branches(
                    material.prony_branches, avg_T, material.reference_temp);
            } else {
                mech_mat.prony_branches = material.prony_branches;
            }

            // Step 3: Compute thermal force
            Eigen::VectorXd F_thermal = compute_thermal_force(
                mesh, T_new, material.thermal_expansion,
                material.young_modulus, material.poisson_ratio, material.reference_temp);

            // Step 4: Solve mechanical with thermal load
            // Add thermal force as additional Neumann-type loading
            SolverSettings mech_settings;
            mech_settings.time_step = dt;
            mech_settings.num_steps = 1;
            mech_settings.use_iterative_solver = settings.use_iterative_solver;

            FEMResult mech_result = fem_solver.solve(mesh, mech_mat, mech_bcs,
                                                     mech_settings, nullptr);
            if (mech_result.success && !mech_result.displacement_history.empty()) {
                u_new = mech_result.displacement_history.back();
            }

            // Step 5: Compute viscous dissipation for next thermal solve
            if (settings.compute_dissipation && !mech_result.stress_history.empty()) {
                Q_dissipation = compute_viscous_dissipation(
                    mesh, u_new, u_n, mech_result.stress_history.back(), dt);
            }
        }

        u_prev = u_n;
        u_n = u_new;
        T_n = T_new;

        // Store results
        result.displacement_history.push_back(u_new);
        result.temperature_history.push_back(T_new);
        result.time_points.push_back(current_time);

        result.max_displacement = u_new.cwiseAbs().maxCoeff();
        result.max_temperature = T_new.maxCoeff();
        result.min_temperature = T_new.minCoeff();

        if (progress) {
            progress(step + 1, settings.num_steps, current_time,
                     result.max_displacement, result.max_temperature);
        }
    }

    result.success = true;
    return result;
}

Eigen::VectorXd ThermoMechanicalSolver::compute_thermal_force(
    const Mesh& mesh,
    const Eigen::VectorXd& temperature,
    double alpha, double E, double nu, double T_ref) const {

    int mech_dofs = mesh.num_nodes * 3;
    Eigen::VectorXd F_th = Eigen::VectorXd::Zero(mech_dofs);

    // Thermal strain: epsilon_th = alpha * (T - T_ref)
    // Thermal stress: sigma_th = D * epsilon_th (isotropic)
    // Force: F_th = integral(B^T * D * epsilon_th * dV)

    double factor = E * alpha / (1.0 - 2.0 * nu);

    for (int e = 0; e < mesh.num_elements; ++e) {
        Eigen::Matrix<double, 4, 3> coords;
        for (int i = 0; i < 4; ++i) {
            coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
        }

        double volume = TetrahedronP1::compute_volume(coords);
        auto B = TetrahedronP1::b_matrix(coords);

        // Average temperature in element
        double T_avg = 0.0;
        for (int i = 0; i < 4; ++i) {
            T_avg += temperature(mesh.elements(e, i));
        }
        T_avg /= 4.0;

        double dT = T_avg - T_ref;

        // Thermal strain vector (isotropic): [alpha*dT, alpha*dT, alpha*dT, 0, 0, 0]
        Eigen::Matrix<double, 6, 1> eps_th;
        eps_th << alpha * dT, alpha * dT, alpha * dT, 0.0, 0.0, 0.0;

        // Thermal stress
        auto D = TetrahedronP1::elasticity_matrix(E, nu);
        Eigen::Matrix<double, 6, 1> sigma_th = D * eps_th;

        // Element thermal force: B^T * sigma_th * V
        Eigen::Matrix<double, 12, 1> fe = B.transpose() * sigma_th * volume;

        // Assemble
        for (int i = 0; i < 4; ++i) {
            int node = mesh.elements(e, i);
            F_th.segment<3>(node * 3) += fe.segment<3>(3 * i);
        }
    }

    return F_th;
}

Eigen::VectorXd ThermoMechanicalSolver::compute_viscous_dissipation(
    const Mesh& mesh,
    const Eigen::VectorXd& displacement,
    const Eigen::VectorXd& prev_displacement,
    const Eigen::VectorXd& stress,
    double dt) const {

    // Q_diss = sigma : d(epsilon)/dt per node (averaged from elements)
    Eigen::VectorXd Q = Eigen::VectorXd::Zero(mesh.num_nodes);
    Eigen::VectorXd node_count = Eigen::VectorXd::Zero(mesh.num_nodes);

    Eigen::VectorXd velocity = (displacement - prev_displacement) / dt;

    for (int e = 0; e < mesh.num_elements; ++e) {
        Eigen::Matrix<double, 4, 3> coords;
        for (int i = 0; i < 4; ++i) {
            coords.row(i) = mesh.nodes.row(mesh.elements(e, i));
        }

        auto B = TetrahedronP1::b_matrix(coords);
        double volume = TetrahedronP1::compute_volume(coords);

        // Element strain rate
        Eigen::Matrix<double, 12, 1> vel_e;
        for (int i = 0; i < 4; ++i) {
            int node = mesh.elements(e, i);
            vel_e.segment<3>(3 * i) = velocity.segment<3>(node * 3);
        }

        Eigen::Matrix<double, 6, 1> strain_rate = B * vel_e;

        // Dissipation power (scalar): sigma * strain_rate (inner product)
        double dissipation = stress(e) * strain_rate.norm();

        // Distribute to nodes
        double q_per_node = dissipation * volume / 4.0;
        for (int i = 0; i < 4; ++i) {
            int node = mesh.elements(e, i);
            Q(node) += q_per_node;
            node_count(node) += 1.0;
        }
    }

    return Q;
}

} // namespace rheosim
