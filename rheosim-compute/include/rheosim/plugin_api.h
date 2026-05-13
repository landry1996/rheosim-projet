#pragma once

/**
 * RheoSim Plugin C ABI Interface
 *
 * All community constitutive model plugins must implement these functions.
 * Plugins are compiled to WebAssembly and run in a sandboxed environment.
 */

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define RHEOSIM_PLUGIN_API_VERSION 1

typedef struct {
    const char* name;
    const char* version;
    const char* author;
    const char* description;
    int api_version;
    int num_parameters;
    const char** parameter_names;
} RheosimPluginMetadata;

/**
 * Called once when the plugin is loaded. Return 0 on success.
 */
int rheosim_plugin_init(void);

/**
 * Returns plugin metadata. Must remain valid for plugin lifetime.
 */
const RheosimPluginMetadata* rheosim_plugin_get_metadata(void);

/**
 * Compute relaxation modulus G(t) at time t.
 * @param t      Time in seconds
 * @param params Array of model parameters
 * @param n      Number of parameters
 * @return       Relaxation modulus value
 */
double rheosim_plugin_relaxation_modulus(double t, const double* params, int n);

/**
 * Compute storage modulus G'(omega) at angular frequency omega.
 * @param omega  Angular frequency in rad/s
 * @param params Array of model parameters
 * @param n      Number of parameters
 * @return       Storage modulus value
 */
double rheosim_plugin_storage_modulus(double omega, const double* params, int n);

/**
 * Compute loss modulus G''(omega) at angular frequency omega.
 * @param omega  Angular frequency in rad/s
 * @param params Array of model parameters
 * @param n      Number of parameters
 * @return       Loss modulus value
 */
double rheosim_plugin_loss_modulus(double omega, const double* params, int n);

/**
 * Compute creep compliance J(t) at time t.
 * @param t      Time in seconds
 * @param params Array of model parameters
 * @param n      Number of parameters
 * @return       Creep compliance value
 */
double rheosim_plugin_creep_compliance(double t, const double* params, int n);

/**
 * Called when the plugin is unloaded. Cleanup resources.
 */
void rheosim_plugin_shutdown(void);

#ifdef __cplusplus
}
#endif
