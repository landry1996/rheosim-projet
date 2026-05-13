#pragma once

#include "rheosim/plugin_manager.h"
#include <memory>
#include <vector>
#include <string>

namespace rheosim {
namespace plugin {

class WasmRuntime {
public:
    WasmRuntime();
    ~WasmRuntime();

    bool initialize(const PluginSandboxConfig& config);
    void shutdown();

    std::unique_ptr<PluginInstance> instantiate(const std::vector<uint8_t>& wasm_bytes);

    bool validate_module(const std::vector<uint8_t>& wasm_bytes, std::string& error_msg);

    struct RuntimeStats {
        uint64_t fuel_consumed;
        size_t memory_used_bytes;
        double execution_time_ms;
    };
    RuntimeStats get_last_stats() const;

private:
    struct WasmEngine;
    std::unique_ptr<WasmEngine> engine_;
    PluginSandboxConfig config_;
    bool initialized_ = false;
};

} // namespace plugin
} // namespace rheosim
