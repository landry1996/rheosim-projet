/**
 * Example RheoSim Plugin: Custom Maxwell Model
 *
 * Demonstrates implementing a constitutive model plugin using the C SDK.
 * This implements a generalized Maxwell model with N parallel spring-dashpot pairs.
 *
 * Parameters: [G_inf, G_1, tau_1, G_2, tau_2, ..., G_N, tau_N]
 *
 * Build: clang --target=wasm32-wasi -O2 -I.. -o maxwell_plugin.wasm maxwell_plugin.c
 */

#include "../plugin_api.h"
#include <math.h>

static const char* PARAM_NAMES[] = {
    "G_inf", "G_1", "tau_1", "G_2", "tau_2", "G_3", "tau_3"
};

static RheosimPluginMetadata metadata = {
    .name = "Generalized Maxwell",
    .version = "1.0.0",
    .author = "RheoSim Community",
    .description = "Generalized Maxwell model with N spring-dashpot pairs",
    .api_version = RHEOSIM_PLUGIN_API_VERSION,
    .num_parameters = 7,
    .parameter_names = PARAM_NAMES
};

RHEOSIM_EXPORT int rheosim_plugin_init(void) {
    return 0;
}

RHEOSIM_EXPORT const RheosimPluginMetadata* rheosim_plugin_get_metadata(void) {
    return &metadata;
}

RHEOSIM_EXPORT double rheosim_plugin_relaxation_modulus(double t, const double* params, int n) {
    // G(t) = G_inf + sum_i G_i * exp(-t / tau_i)
    double G_inf = params[0];
    double result = G_inf;
    int n_terms = (n - 1) / 2;

    for (int i = 0; i < n_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        if (tau_i > 0.0) {
            result += G_i * exp(-t / tau_i);
        }
    }
    return result;
}

RHEOSIM_EXPORT double rheosim_plugin_storage_modulus(double omega, const double* params, int n) {
    // G'(omega) = G_inf + sum_i G_i * (omega*tau_i)^2 / (1 + (omega*tau_i)^2)
    double G_inf = params[0];
    double result = G_inf;
    int n_terms = (n - 1) / 2;

    for (int i = 0; i < n_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        double wt = omega * tau_i;
        double wt2 = wt * wt;
        result += G_i * wt2 / (1.0 + wt2);
    }
    return result;
}

RHEOSIM_EXPORT double rheosim_plugin_loss_modulus(double omega, const double* params, int n) {
    // G''(omega) = sum_i G_i * (omega*tau_i) / (1 + (omega*tau_i)^2)
    double result = 0.0;
    int n_terms = (n - 1) / 2;

    for (int i = 0; i < n_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        double wt = omega * tau_i;
        double wt2 = wt * wt;
        result += G_i * wt / (1.0 + wt2);
    }
    return result;
}

RHEOSIM_EXPORT double rheosim_plugin_creep_compliance(double t, const double* params, int n) {
    // J(t) = 1/G_inf * (1 - sum_i (G_i/G_total) * exp(-t/tau_i))
    double G_inf = params[0];
    if (G_inf <= 0.0) return 0.0;

    double G_total = G_inf;
    int n_terms = (n - 1) / 2;
    for (int i = 0; i < n_terms; i++) {
        G_total += params[1 + 2 * i];
    }

    double result = 1.0 / G_total;
    for (int i = 0; i < n_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        if (tau_i > 0.0) {
            double retardation_time = tau_i * G_total / G_inf;
            result += (G_i / (G_inf * G_total)) * (1.0 - exp(-t / retardation_time));
        }
    }
    return result;
}

RHEOSIM_EXPORT void rheosim_plugin_shutdown(void) {
    // No cleanup needed
}
