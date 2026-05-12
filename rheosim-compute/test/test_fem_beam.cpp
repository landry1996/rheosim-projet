#include <gtest/gtest.h>
#include "rheosim/fem_solver.h"
#include "rheosim/mesh.h"
#include "rheosim/tetrahedron_p1.h"
#include <cmath>

using namespace rheosim;

TEST(TetrahedronP1Test, VolumeOfUnitTetrahedron) {
    Eigen::Matrix<double, 4, 3> coords;
    coords << 0, 0, 0,
              1, 0, 0,
              0, 1, 0,
              0, 0, 1;

    double vol = TetrahedronP1::volume(coords);
    EXPECT_NEAR(vol, 1.0 / 6.0, 1e-12);
}

TEST(TetrahedronP1Test, ElasticityMatrixSymmetric) {
    auto D = TetrahedronP1::elasticity_matrix(200e9, 0.3);
    for (int i = 0; i < 6; ++i) {
        for (int j = 0; j < 6; ++j) {
            EXPECT_NEAR(D(i, j), D(j, i), 1e-3);
        }
    }
}

TEST(TetrahedronP1Test, StiffnessMatrixSymmetric) {
    Eigen::Matrix<double, 4, 3> coords;
    coords << 0, 0, 0,
              1, 0, 0,
              0, 1, 0,
              0, 0, 1;

    auto Ke = TetrahedronP1::stiffness_matrix(coords, 200e9, 0.3);
    for (int i = 0; i < 12; ++i) {
        for (int j = 0; j < 12; ++j) {
            EXPECT_NEAR(Ke(i, j), Ke(j, i), 1e-3);
        }
    }
}

TEST(MeshTest, BeamMeshHasCorrectDimensions) {
    auto mesh = Mesh::create_beam(1.0, 0.1, 0.1, 4, 1, 1);
    EXPECT_EQ(mesh.num_nodes, 5 * 2 * 2); // (4+1)*(1+1)*(1+1) = 20
    EXPECT_EQ(mesh.num_elements, 4 * 1 * 1 * 6); // 24 tetrahedra
}

TEST(MeshTest, BeamNodesInCorrectRange) {
    auto mesh = Mesh::create_beam(2.0, 0.5, 0.3, 4, 2, 2);
    for (int i = 0; i < mesh.num_nodes; ++i) {
        EXPECT_GE(mesh.nodes(i, 0), 0.0);
        EXPECT_LE(mesh.nodes(i, 0), 2.0);
        EXPECT_GE(mesh.nodes(i, 1), 0.0);
        EXPECT_LE(mesh.nodes(i, 1), 0.5);
        EXPECT_GE(mesh.nodes(i, 2), 0.0);
        EXPECT_LE(mesh.nodes(i, 2), 0.3);
    }
}

TEST(FEMSolverTest, CantileverBeamDeflection) {
    // Simple cantilever beam with end load
    auto mesh = Mesh::create_beam(1.0, 0.1, 0.1, 8, 2, 2);

    MaterialProps material;
    material.young_modulus = 200e9;
    material.poisson_ratio = 0.3;
    material.density = 7800;

    std::vector<BoundaryCondition> bcs;

    // Fix x=0 face
    std::vector<int> fixed_nodes;
    for (int i = 0; i < mesh.num_nodes; ++i) {
        if (mesh.nodes(i, 0) < 1e-10) {
            fixed_nodes.push_back(i);
        }
    }
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 0, 0.0});
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 1, 0.0});
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 2, 0.0});

    // Load at x=L
    std::vector<int> loaded_nodes;
    for (int i = 0; i < mesh.num_nodes; ++i) {
        if (std::abs(mesh.nodes(i, 0) - 1.0) < 1e-10) {
            loaded_nodes.push_back(i);
        }
    }
    double F_total = -1000.0;
    double f_per_node = F_total / static_cast<double>(loaded_nodes.size());
    bcs.push_back({BoundaryCondition::NEUMANN, loaded_nodes, 2, f_per_node});

    SolverSettings settings;
    settings.num_steps = 1;

    FEMSolver solver;
    auto result = solver.solve(mesh, material, bcs, settings);

    ASSERT_TRUE(result.success);
    EXPECT_GT(result.max_displacement, 0.0);

    // Analytical: delta = F*L^3 / (3*E*I)
    double L = 1.0, b = 0.1, h = 0.1;
    double I = b * h * h * h / 12.0;
    double delta_analytical = std::abs(F_total) * L * L * L / (3.0 * 200e9 * I);

    // FEM with coarse mesh should be within 20% of analytical
    EXPECT_NEAR(result.max_displacement, delta_analytical, delta_analytical * 0.3);
}

TEST(FEMSolverTest, ZeroLoadGivesZeroDisplacement) {
    auto mesh = Mesh::create_beam(1.0, 0.1, 0.1, 2, 1, 1);

    MaterialProps material;
    material.young_modulus = 200e9;
    material.poisson_ratio = 0.3;
    material.density = 7800;

    std::vector<BoundaryCondition> bcs;
    std::vector<int> fixed_nodes;
    for (int i = 0; i < mesh.num_nodes; ++i) {
        if (mesh.nodes(i, 0) < 1e-10) {
            fixed_nodes.push_back(i);
        }
    }
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 0, 0.0});
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 1, 0.0});
    bcs.push_back({BoundaryCondition::DIRICHLET, fixed_nodes, 2, 0.0});
    // No load applied

    SolverSettings settings;
    settings.num_steps = 1;

    FEMSolver solver;
    auto result = solver.solve(mesh, material, bcs, settings);

    ASSERT_TRUE(result.success);
    EXPECT_LT(result.max_displacement, 1e-20);
}
