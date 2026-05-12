#include <gtest/gtest.h>
#include "rheosim/prony_series_law.h"
#include <cmath>

using namespace rheosim;

class PronySeriesTest : public ::testing::Test {
protected:
    PronySeriesLaw law{2}; // 2 branches
    Eigen::VectorXd params{5}; // G_inf, G_1, tau_1, G_2, tau_2

    void SetUp() override {
        params << 100.0, 500.0, 0.1, 300.0, 1.0;
    }
};

TEST_F(PronySeriesTest, RelaxationAtZero) {
    double g0 = law.compute_relaxation_modulus(0.0, params);
    // G(0) = G_inf + G_1 + G_2
    EXPECT_NEAR(g0, 100.0 + 500.0 + 300.0, 1e-10);
}

TEST_F(PronySeriesTest, RelaxationAtInfinity) {
    double g_inf = law.compute_relaxation_modulus(1e6, params);
    EXPECT_NEAR(g_inf, 100.0, 1e-6);
}

TEST_F(PronySeriesTest, RelaxationDecaysMonotonically) {
    double g1 = law.compute_relaxation_modulus(0.01, params);
    double g2 = law.compute_relaxation_modulus(0.1, params);
    double g3 = law.compute_relaxation_modulus(1.0, params);
    double g4 = law.compute_relaxation_modulus(10.0, params);
    EXPECT_GT(g1, g2);
    EXPECT_GT(g2, g3);
    EXPECT_GT(g3, g4);
}

TEST_F(PronySeriesTest, StorageModulusLowFreqLimit) {
    double gp = law.compute_storage_modulus(1e-8, params);
    // At omega→0: G' → G_inf
    EXPECT_NEAR(gp, 100.0, 1.0);
}

TEST_F(PronySeriesTest, StorageModulusHighFreqLimit) {
    double gp = law.compute_storage_modulus(1e8, params);
    // At omega→inf: G' → G_inf + G_1 + G_2
    EXPECT_NEAR(gp, 900.0, 1.0);
}

TEST_F(PronySeriesTest, LossModulusZeroAtLimits) {
    double gpp_low = law.compute_loss_modulus(1e-10, params);
    double gpp_high = law.compute_loss_modulus(1e10, params);
    EXPECT_LT(gpp_low, 0.01);
    EXPECT_LT(gpp_high, 0.01);
}

TEST_F(PronySeriesTest, ParameterCount) {
    EXPECT_EQ(law.parameter_count(), 5);
    auto names = law.parameter_names();
    EXPECT_EQ(names.size(), 5u);
    EXPECT_EQ(names[0], "G_inf");
    EXPECT_EQ(names[1], "G_1");
    EXPECT_EQ(names[2], "tau_1");
}

TEST_F(PronySeriesTest, ReducesToMaxwellWithOnebranchAndZeroGinf) {
    PronySeriesLaw single(1);
    Eigen::VectorXd p(3);
    p << 0.0, 5000.0, 2.0; // G_inf=0, G_1=5000, tau_1=2

    double g = single.compute_relaxation_modulus(2.0, p);
    double expected = 5000.0 * std::exp(-1.0);
    EXPECT_NEAR(g, expected, 1e-10);
}
