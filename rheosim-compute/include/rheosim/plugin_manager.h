#pragma once

#include <memory>
#include <string>
#include <vector>
#include <unordered_map>
#include <functional>

namespace rheosim {
namespace plugin {

struct PluginMetadata {
    std::string id;
    std::string name;
    std::string version;
    std::string author;
    std::string description;
    int num_parameters;
    std::vector<std::string> parameter_names;
};

class PluginInstance {
public:
    virtual ~PluginInstance() = default;

    virtual double call_relaxation_modulus(double t, const std::vector<double>& params) = 0;
    virtual double call_storage_modulus(double omega, const std::vector<double>& params) = 0;
    virtual double call_loss_modulus(double omega, const std::vector<double>& params) = 0;
    virtual double call_creep_compliance(double t, const std::vector<double>& params) = 0;
    virtual PluginMetadata get_metadata() const = 0;
};

struct PluginSandboxConfig {
    uint64_t fuel_limit = 1000000000;   // CPU instruction budget
    size_t memory_limit_mb = 64;         // Max memory in MB
    uint64_t timeout_seconds = 5;        // Wall-clock timeout
    bool allow_wasi = false;             // No filesystem/network access
};

class PluginManager {
public:
    static PluginManager& instance();

    std::string load_plugin(const std::vector<uint8_t>& wasm_bytes, const std::string& plugin_id = "");
    void unload_plugin(const std::string& id);
    std::vector<PluginMetadata> list_plugins() const;
    PluginInstance* get_plugin(const std::string& id) const;

    void set_sandbox_config(const PluginSandboxConfig& config);
    PluginSandboxConfig get_sandbox_config() const;

    // Hot-reload: replace plugin without restart
    bool reload_plugin(const std::string& id, const std::vector<uint8_t>& new_wasm_bytes);

private:
    PluginManager();
    ~PluginManager();

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace plugin
} // namespace rheosim
