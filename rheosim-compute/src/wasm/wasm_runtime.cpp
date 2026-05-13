#include "rheosim/wasm_runtime.h"
#include <spdlog/spdlog.h>
#include <chrono>

#ifdef RHEOSIM_USE_WASMTIME
#include <wasmtime.h>
#endif

namespace rheosim {
namespace plugin {

struct WasmRuntime::WasmEngine {
#ifdef RHEOSIM_USE_WASMTIME
    wasm_engine_t* engine = nullptr;
    wasmtime_store_t* store = nullptr;
    wasmtime_linker_t* linker = nullptr;
#endif
    RuntimeStats last_stats{};
};

class WasmPluginInstance : public PluginInstance {
public:
    WasmPluginInstance(
#ifdef RHEOSIM_USE_WASMTIME
        wasmtime_instance_t instance,
        wasmtime_store_t* store,
#endif
        PluginMetadata metadata)
        :
#ifdef RHEOSIM_USE_WASMTIME
          instance_(instance), store_(store),
#endif
          metadata_(std::move(metadata)) {}

    double call_relaxation_modulus(double t, const std::vector<double>& params) override {
#ifdef RHEOSIM_USE_WASMTIME
        return call_function("rheosim_plugin_relaxation_modulus", t, params);
#else
        return 0.0;
#endif
    }

    double call_storage_modulus(double omega, const std::vector<double>& params) override {
#ifdef RHEOSIM_USE_WASMTIME
        return call_function("rheosim_plugin_storage_modulus", omega, params);
#else
        return 0.0;
#endif
    }

    double call_loss_modulus(double omega, const std::vector<double>& params) override {
#ifdef RHEOSIM_USE_WASMTIME
        return call_function("rheosim_plugin_loss_modulus", omega, params);
#else
        return 0.0;
#endif
    }

    double call_creep_compliance(double t, const std::vector<double>& params) override {
#ifdef RHEOSIM_USE_WASMTIME
        return call_function("rheosim_plugin_creep_compliance", t, params);
#else
        return 0.0;
#endif
    }

    PluginMetadata get_metadata() const override {
        return metadata_;
    }

private:
#ifdef RHEOSIM_USE_WASMTIME
    wasmtime_instance_t instance_;
    wasmtime_store_t* store_;

    double call_function(const char* name, double arg, const std::vector<double>& params) {
        wasmtime_context_t* context = wasmtime_store_context(store_);

        // Write params to WASM memory
        wasmtime_extern_t memory_extern;
        bool found = wasmtime_instance_export_get(context, &instance_, "memory", 6, &memory_extern);
        if (!found) return 0.0;

        wasmtime_memory_t memory = memory_extern.of.memory;
        uint8_t* data = wasmtime_memory_data(context, &memory);
        size_t mem_size = wasmtime_memory_data_size(context, &memory);

        // Write params at offset 1024
        int32_t params_offset = 1024;
        if (params_offset + params.size() * sizeof(double) > mem_size) return 0.0;
        memcpy(data + params_offset, params.data(), params.size() * sizeof(double));

        // Get function
        wasmtime_extern_t func_extern;
        found = wasmtime_instance_export_get(context, &instance_, name, strlen(name), &func_extern);
        if (!found) return 0.0;

        wasmtime_func_t func = func_extern.of.func;

        // Call: (f64, i32_ptr, i32_len) -> f64
        wasmtime_val_t args[3];
        args[0].kind = WASMTIME_F64;
        args[0].of.f64 = arg;
        args[1].kind = WASMTIME_I32;
        args[1].of.i32 = params_offset;
        args[2].kind = WASMTIME_I32;
        args[2].of.i32 = (int32_t)params.size();

        wasmtime_val_t results[1];
        wasm_trap_t* trap = nullptr;
        wasmtime_error_t* error = wasmtime_func_call(context, &func, args, 3, results, 1, &trap);

        if (error || trap) {
            if (error) wasmtime_error_delete(error);
            if (trap) wasm_trap_delete(trap);
            return 0.0;
        }

        return results[0].of.f64;
    }
#endif
    PluginMetadata metadata_;
};

WasmRuntime::WasmRuntime() : engine_(std::make_unique<WasmEngine>()) {}
WasmRuntime::~WasmRuntime() { shutdown(); }

bool WasmRuntime::initialize(const PluginSandboxConfig& config) {
    config_ = config;
#ifdef RHEOSIM_USE_WASMTIME
    wasm_config_t* wasm_config = wasm_config_new();
    wasmtime_config_consume_fuel_set(wasm_config, true);
    wasmtime_config_epoch_interruption_set(wasm_config, true);

    engine_->engine = wasm_engine_new_with_config(wasm_config);
    if (!engine_->engine) {
        spdlog::error("Failed to create Wasmtime engine");
        return false;
    }

    engine_->store = wasmtime_store_new(engine_->engine, nullptr, nullptr);
    wasmtime_context_t* context = wasmtime_store_context(engine_->store);
    wasmtime_context_set_fuel(context, config.fuel_limit);

    engine_->linker = wasmtime_linker_new(engine_->engine);

    initialized_ = true;
    spdlog::info("Wasmtime runtime initialized (fuel={}, mem={}MB, timeout={}s)",
                 config.fuel_limit, config.memory_limit_mb, config.timeout_seconds);
#else
    spdlog::warn("Wasmtime not available, plugin system disabled");
    initialized_ = false;
#endif
    return initialized_;
}

void WasmRuntime::shutdown() {
#ifdef RHEOSIM_USE_WASMTIME
    if (engine_->linker) { wasmtime_linker_delete(engine_->linker); engine_->linker = nullptr; }
    if (engine_->store) { wasmtime_store_delete(engine_->store); engine_->store = nullptr; }
    if (engine_->engine) { wasm_engine_delete(engine_->engine); engine_->engine = nullptr; }
#endif
    initialized_ = false;
}

std::unique_ptr<PluginInstance> WasmRuntime::instantiate(const std::vector<uint8_t>& wasm_bytes) {
#ifdef RHEOSIM_USE_WASMTIME
    if (!initialized_) return nullptr;

    wasmtime_context_t* context = wasmtime_store_context(engine_->store);

    // Compile module
    wasmtime_module_t* module = nullptr;
    wasmtime_error_t* error = wasmtime_module_new(
        engine_->engine, wasm_bytes.data(), wasm_bytes.size(), &module);

    if (error) {
        wasmtime_error_delete(error);
        spdlog::error("Failed to compile WASM module");
        return nullptr;
    }

    // Set fuel for this instantiation
    wasmtime_context_set_fuel(context, config_.fuel_limit);

    // Instantiate
    wasmtime_instance_t instance;
    wasm_trap_t* trap = nullptr;
    error = wasmtime_linker_instantiate(engine_->linker, context, module, &instance, &trap);

    wasmtime_module_delete(module);

    if (error || trap) {
        if (error) wasmtime_error_delete(error);
        if (trap) wasm_trap_delete(trap);
        return nullptr;
    }

    // Call init function
    wasmtime_extern_t init_extern;
    if (wasmtime_instance_export_get(context, &instance, "rheosim_plugin_init", 20, &init_extern)) {
        wasmtime_func_t init_func = init_extern.of.func;
        wasmtime_val_t init_result[1];
        error = wasmtime_func_call(context, &init_func, nullptr, 0, init_result, 1, &trap);
        if (error || trap || init_result[0].of.i32 != 0) {
            spdlog::error("Plugin init function failed");
            return nullptr;
        }
    }

    PluginMetadata metadata;
    metadata.name = "wasm_plugin";
    metadata.version = "1.0.0";
    metadata.author = "community";

    return std::make_unique<WasmPluginInstance>(instance, engine_->store, metadata);
#else
    return nullptr;
#endif
}

bool WasmRuntime::validate_module(const std::vector<uint8_t>& wasm_bytes, std::string& error_msg) {
    if (wasm_bytes.size() < 8) {
        error_msg = "Module too small";
        return false;
    }

    // Check WASM magic number
    if (wasm_bytes[0] != 0x00 || wasm_bytes[1] != 0x61 ||
        wasm_bytes[2] != 0x73 || wasm_bytes[3] != 0x6d) {
        error_msg = "Invalid WASM magic number";
        return false;
    }

    // Check memory limit
    if (wasm_bytes.size() > config_.memory_limit_mb * 1024 * 1024) {
        error_msg = "Module exceeds memory limit";
        return false;
    }

#ifdef RHEOSIM_USE_WASMTIME
    wasmtime_module_t* module = nullptr;
    wasmtime_error_t* err = wasmtime_module_validate(
        engine_->engine, wasm_bytes.data(), wasm_bytes.size());
    if (err) {
        wasm_message_t msg;
        wasmtime_error_message(err, &msg);
        error_msg = std::string(msg.data, msg.size);
        wasm_byte_vec_delete(&msg);
        wasmtime_error_delete(err);
        return false;
    }
#endif

    return true;
}

WasmRuntime::RuntimeStats WasmRuntime::get_last_stats() const {
    return engine_->last_stats;
}

} // namespace plugin
} // namespace rheosim
