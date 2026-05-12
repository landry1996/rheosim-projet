#include <gtest/gtest.h>
#include "rheosim/maxwell_law.h"
#include <cmath>

using namespace rheosim;

class MaxwellLawTest : public ::testing::Test {
protected:
    MaxwellLaw law;
    Eigen::VectorXd params{2};

    void SetUp() override {
        params << 5000.0, 2.0; // G=5000 Pa, tau=2 s
    }
};

TEST_F(MaxwellLawTest, RelaxationAtZero) {
    EXPECT_NEAR(law.compute_relaxation_modulus(0.0, params), 5000.0, 1e-10);
}

TEST_F(MaxwellLawTest, RelaxationDecaysExponentially) {
    double g_tau = law.compute_relaxation_modulus(2.0, params);
    EXPECT_NEAR(g_tau, 5000.0 * std::exp(-1.0), 1e-6);
}

TEST_F(MaxwellLawTest, RelaxationApproachesZero) {
    double g_inf = law.compute_relaxation_modulus(100.0, params);
    EXPECT_LT(g_inf, 1e-10);
}

TEST_F(MaxwellLawTest, StorageModulusAtHighFrequency) {
    double gp = law.compute_storage_modulus(1000.0, params);
    EXPECT_NEAR(gp, 5000.0, 5.0); // approaches G at high omega
}

TEST_F(MaxwellLawTest, LossModulusPeakAtInverseTau) {
    double omega_peak = 1.0 / 2.0; // 1/tau
    double gpp_peak = law.compute_loss_modulus(omega_peak, params);
    // G'' max = G/2 at omega = 1/tau
    EXPECT_NEAR(gpp_peak, 2500.0, 1e-6);
}

TEST_F(MaxwellLawTest, LossModulusSymmetryAroundPeak) {
    double omega_peak = 1.0 / 2.0;
    double factor = 10.0;
    double gpp_low = law.compute_loss_modulus(omega_peak / factor, params);
    double gpp_high = law.compute_loss_modulus(omega_peak * factor, params);
    // Should be approximately equal (log-symmetric)
    EXPECT_NEAR(gpp_low, gpp_high, 5.0);
}

TEST_F(MaxwellLawTest, CreepComplianceLinear) {
    double j1 = law.compute_creep_compliance(1.0, params);
    double j2 = law.compute_creep_compliance(2.0, params);
    // J(t) = 1/G + t/(G*tau) — linear in time
    double expected1 = 1.0 / 5000.0 + 1.0 / (5000.0 * 2.0);
    double expected2 = 1.0 / 5000.0 + 2.0 / (5000.0 * 2.0);
    EXPECT_NEAR(j1, expected1, 1e-10);
    EXPECT_NEAR(j2, expected2, 1e-10);
}
