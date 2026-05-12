#include <gtest/gtest.h>
#include "rheosim/kelvin_voigt_law.h"
#include <cmath>

using namespace rheosim;

class KelvinVoigtTest : public ::testing::Test {
protected:
    KelvinVoigtLaw law;
    Eigen::VectorXd params{2};

    void SetUp() override {
        params << 1000.0, 5.0; // G=1000 Pa, tau=5 s
    }
};

TEST_F(KelvinVoigtTest, CreepComplianceAtZero) {
    double j0 = law.compute_creep_compliance(0.0, params);
    EXPECT_NEAR(j0, 0.0, 1e-10);
}

TEST_F(KelvinVoigtTest, CreepComplianceApproachesEquilibrium) {
    double j_inf = law.compute_creep_compliance(1000.0, params);
    EXPECT_NEAR(j_inf, 1.0 / 1000.0, 1e-8);
}

TEST_F(KelvinVoigtTest, CreepAtTau) {
    double j_tau = law.compute_creep_compliance(5.0, params);
    double expected = (1.0 / 1000.0) * (1.0 - std::exp(-1.0));
    EXPECT_NEAR(j_tau, expected, 1e-10);
}

TEST_F(KelvinVoigtTest, CreepIsMonotonicallyIncreasing) {
    double j1 = law.compute_creep_compliance(1.0, params);
    double j2 = law.compute_creep_compliance(2.0, params);
    double j3 = law.compute_creep_compliance(5.0, params);
    EXPECT_LT(j1, j2);
    EXPECT_LT(j2, j3);
}

TEST_F(KelvinVoigtTest, StorageModulusIsConstant) {
    double gp1 = law.compute_storage_modulus(0.1, params);
    double gp2 = law.compute_storage_modulus(10.0, params);
    EXPECT_NEAR(gp1, 1000.0, 1e-10);
    EXPECT_NEAR(gp2, 1000.0, 1e-10);
}

TEST_F(KelvinVoigtTest, LossModulusLinearInFrequency) {
    double gpp1 = law.compute_loss_modulus(1.0, params);
    double gpp2 = law.compute_loss_modulus(2.0, params);
    // G''(w) = G * w * tau → linear in omega
    EXPECT_NEAR(gpp2 / gpp1, 2.0, 1e-10);
}
