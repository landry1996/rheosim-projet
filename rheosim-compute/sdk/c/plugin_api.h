#pragma once

/**
 * RheoSim Plugin SDK — C Interface
 *
 * Include this header in your plugin to get the correct ABI.
 * Compile with: clang --target=wasm32-wasi -O2 -o plugin.wasm plugin.c
 */

#include <stdint.h>

#define RHEOSIM_EXPORT __attribute__((visibility("default"))) __attribute__((used))

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

/* Required exports — implement all of these */

RHEOSIM_EXPORT int rheosim_plugin_init(void);
RHEOSIM_EXPORT const RheosimPluginMetadata* rheosim_plugin_get_metadata(void);
RHEOSIM_EXPORT double rheosim_plugin_relaxation_modulus(double t, const double* params, int n);
RHEOSIM_EXPORT double rheosim_plugin_storage_modulus(double omega, const double* params, int n);
RHEOSIM_EXPORT double rheosim_plugin_loss_modulus(double omega, const double* params, int n);
RHEOSIM_EXPORT double rheosim_plugin_creep_compliance(double t, const double* params, int n);
RHEOSIM_EXPORT void rheosim_plugin_shutdown(void);
