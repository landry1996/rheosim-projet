#include <gtest/gtest.h>
#include "rheosim/levenberg_marquardt.h"
#include "rheosim/maxwell_law.h"
#include "rheosim/prony_series_law.h"
#include <cmath>

using namespace rheosim;

TEST(LevenbergMarquardtTest, RecoversMaxwellFromRelaxation) {
    MaxwellLaw law;
    double true_G = 5000.0, true_tau = 2.0;
    Eigen::VectorXd true_params(2);
    true_params << true_G, true_tau;

    int n = 50;
    Eigen::VectorXd x(n), y(n);
    for (int i = 0; i < n; ++i) {
        x(i) = 0.01 + i * 0.2;
        y(i) = law.compute_relaxation_modulus(x(i), true_params);
    }

    Eigen::VectorXd guess(2);
    guess << 1000.0, 1.0;

    LevenbergMarquardt lm;
    auto result = lm.identify(law, FitTarget::RELAXATION_MODULUS,
                              x, y, guess,
                              law.default_lower_bounds(),
                              law.default_upper_bounds());

    EXPECT_TRUE(result.converged);
    EXPECT_GT(result.r_squared, 0.999);
    EXPECT_NEAR(result.parameters(0), true_G, true_G * 0.01);
    EXPECT_NEAR(result.parameters(1), true_tau, true_tau * 0.01);
}

TEST(LevenbergMarquardtTest, RecoversMaxwellFromStorageModulus) {
    MaxwellLaw law;
    double true_G = 2000.0, true_tau = 0.5;
    Eigen::VectorXd true_params(2);
    true_params << true_G, true_tau;

    int n = 30;
    Eigen::VectorXd omega(n), gp(n);
    for (int i = 0; i < n; ++i) {
        omega(i) = std::pow(10.0, -2.0 + i * 0.2);
        gp(i) = law.compute_storage_modulus(omega(i), true_params);
    }

    Eigen::VectorXd guess(2);
    guess << 500.0, 0.1;

    LevenbergMarquardt lm;
    auto result = lm.identify(law, FitTarget::STORAGE_MODULUS,
                              omega, gp, guess,
                              law.default_lower_bounds(),
                              law.default_upper_bounds());

    EXPECT_TRUE(result.converged);
    EXPECT_GT(result.r_squared, 0.99);
    EXPECT_NEAR(result.parameters(0), true_G, true_G * 0.05);
    EXPECT_NEAR(result.parameters(1), true_tau, true_tau * 0.05);
}

TEST(LevenbergMarquardtTest, RecoversPronyFromRelaxation) {
    PronySeriesLaw law(2);
    Eigen::VectorXd true_params(5);
    true_params << 100.0, 500.0, 0.1, 300.0, 1.0;

    int n = 60;
    Eigen::VectorXd x(n), y(n);
    for (int i = 0; i < n; ++i) {
        x(i) = 0.001 + i * 0.1;
        y(i) = law.compute_relaxation_modulus(x(i), true_params);
    }

    Eigen::VectorXd guess(5);
    guess << 50.0, 200.0, 0.05, 100.0, 0.5;

    LevenbergMarquardt lm;
    IdentificationOptions opts;
    opts.max_iterations = 2000;

    auto result = lm.identify(law, FitTarget::RELAXATION_MODULUS,
                              x, y, guess,
                              law.default_lower_bounds(),
                              law.default_upper_bounds(), opts);

    EXPECT_TRUE(result.converged);
    EXPECT_GT(result.r_squared, 0.99);
}

TEST(LevenbergMarquardtTest, ReturnsBadR2ForImpossibleFit) {
    MaxwellLaw law;
    Eigen::VectorXd x(5), y(5);
    x << 1, 2, 3, 4, 5;
    y << 100, -50, 200, -100, 300; // Oscillating, not exponential

    Eigen::VectorXd guess(2);
    guess << 1000.0, 1.0;

    LevenbergMarquardt lm;
    auto result = lm.identify(law, FitTarget::RELAXATION_MODULUS,
                              x, y, guess,
                              law.default_lower_bounds(),
                              law.default_upper_bounds());

    EXPECT_LT(result.r_squared, 0.5);
}
