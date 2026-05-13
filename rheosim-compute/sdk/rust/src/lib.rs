//! RheoSim Plugin SDK for Rust
//!
//! Build WASM plugins for the RheoSim constitutive model marketplace.
//!
//! # Example
//! ```rust
//! use rheosim_plugin_sdk::*;
//!
//! #[rheosim_plugin]
//! struct MyMaxwellModel;
//!
//! impl RheosimPlugin for MyMaxwellModel {
//!     fn metadata() -> PluginMetadata { ... }
//!     fn relaxation_modulus(t: f64, params: &[f64]) -> f64 { ... }
//!     ...
//! }
//! ```

#![no_std]

extern crate alloc;
use alloc::vec::Vec;
use alloc::string::String;

/// Plugin metadata returned by `get_metadata()`.
#[repr(C)]
pub struct PluginMetadata {
    pub name: &'static str,
    pub version: &'static str,
    pub author: &'static str,
    pub description: &'static str,
    pub api_version: i32,
    pub num_parameters: i32,
    pub parameter_names: &'static [&'static str],
}

/// Trait that all RheoSim plugins must implement.
pub trait RheosimPlugin {
    fn metadata() -> PluginMetadata;
    fn init() -> i32 { 0 }
    fn shutdown() {}
    fn relaxation_modulus(t: f64, params: &[f64]) -> f64;
    fn storage_modulus(omega: f64, params: &[f64]) -> f64;
    fn loss_modulus(omega: f64, params: &[f64]) -> f64;
    fn creep_compliance(t: f64, params: &[f64]) -> f64;
}

/// Macro to generate WASM exports from a plugin implementation.
///
/// Usage:
/// ```
/// rheosim_export!(MyPlugin);
/// ```
#[macro_export]
macro_rules! rheosim_export {
    ($plugin:ty) => {
        #[no_mangle]
        pub extern "C" fn rheosim_plugin_init() -> i32 {
            <$plugin as $crate::RheosimPlugin>::init()
        }

        #[no_mangle]
        pub extern "C" fn rheosim_plugin_relaxation_modulus(
            t: f64, params_ptr: *const f64, n: i32
        ) -> f64 {
            let params = unsafe { core::slice::from_raw_parts(params_ptr, n as usize) };
            <$plugin as $crate::RheosimPlugin>::relaxation_modulus(t, params)
        }

        #[no_mangle]
        pub extern "C" fn rheosim_plugin_storage_modulus(
            omega: f64, params_ptr: *const f64, n: i32
        ) -> f64 {
            let params = unsafe { core::slice::from_raw_parts(params_ptr, n as usize) };
            <$plugin as $crate::RheosimPlugin>::storage_modulus(omega, params)
        }

        #[no_mangle]
        pub extern "C" fn rheosim_plugin_loss_modulus(
            omega: f64, params_ptr: *const f64, n: i32
        ) -> f64 {
            let params = unsafe { core::slice::from_raw_parts(params_ptr, n as usize) };
            <$plugin as $crate::RheosimPlugin>::loss_modulus(omega, params)
        }

        #[no_mangle]
        pub extern "C" fn rheosim_plugin_creep_compliance(
            t: f64, params_ptr: *const f64, n: i32
        ) -> f64 {
            let params = unsafe { core::slice::from_raw_parts(params_ptr, n as usize) };
            <$plugin as $crate::RheosimPlugin>::creep_compliance(t, params)
        }

        #[no_mangle]
        pub extern "C" fn rheosim_plugin_shutdown() {
            <$plugin as $crate::RheosimPlugin>::shutdown()
        }
    };
}

/// Helper: compute Prony series relaxation modulus
/// G(t) = G_inf + sum_i G_i * exp(-t / tau_i)
pub fn prony_relaxation(t: f64, params: &[f64]) -> f64 {
    let g_inf = params[0];
    let n_terms = (params.len() - 1) / 2;
    let mut result = g_inf;
    for i in 0..n_terms {
        let g_i = params[1 + 2 * i];
        let tau_i = params[2 + 2 * i];
        if tau_i > 0.0 {
            result += g_i * libm::exp(-t / tau_i);
        }
    }
    result
}

/// Helper: compute Prony series storage modulus
/// G'(omega) = G_inf + sum_i G_i * (omega*tau_i)^2 / (1 + (omega*tau_i)^2)
pub fn prony_storage(omega: f64, params: &[f64]) -> f64 {
    let g_inf = params[0];
    let n_terms = (params.len() - 1) / 2;
    let mut result = g_inf;
    for i in 0..n_terms {
        let g_i = params[1 + 2 * i];
        let tau_i = params[2 + 2 * i];
        let wt = omega * tau_i;
        let wt2 = wt * wt;
        result += g_i * wt2 / (1.0 + wt2);
    }
    result
}

/// Helper: compute Prony series loss modulus
/// G''(omega) = sum_i G_i * (omega*tau_i) / (1 + (omega*tau_i)^2)
pub fn prony_loss(omega: f64, params: &[f64]) -> f64 {
    let n_terms = (params.len() - 1) / 2;
    let mut result = 0.0;
    for i in 0..n_terms {
        let g_i = params[1 + 2 * i];
        let tau_i = params[2 + 2 * i];
        let wt = omega * tau_i;
        let wt2 = wt * wt;
        result += g_i * wt / (1.0 + wt2);
    }
    result
}
