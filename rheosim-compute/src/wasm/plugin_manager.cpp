#include "rheosim/plugin_manager.h"
#include "rheosim/wasm_runtime.h"
#include <spdlog/spdlog.h>
#include <mutex>
#include <algorithm>

namespace rheosim {
namespace plugin {

struct PluginManager::Impl {
    WasmRuntime runtime;
    std::unordered_map<std::string, std::unique_ptr<PluginInstance>> plugins;
    PluginSandboxConfig config;
    std::mutex mutex;
    int next_id = 1;
};

PluginManager& PluginManager::instance() {
    static PluginManager mgr;
    return mgr;
}

PluginManager::PluginManager() : impl_(std::make_unique<Impl>()) {
    impl_->runtime.initialize(impl_->config);
}

PluginManager::~PluginManager() = default;

std::string PluginManager::load_plugin(const std::vector<uint8_t>& wasm_bytes, const std::string& plugin_id) {
    std::lock_guard<std::mutex> lock(impl_->mutex);

    // Validate module first
    std::string error;
    if (!impl_->runtime.validate_module(wasm_bytes, error)) {
        spdlog::error("Plugin validation failed: {}", error);
        return "";
    }

    auto instance = impl_->runtime.instantiate(wasm_bytes);
    if (!instance) {
        spdlog::error("Plugin instantiation failed");
        return "";
    }

    std::string id = plugin_id.empty()
        ? "plugin_" + std::to_string(impl_->next_id++)
        : plugin_id;

    auto metadata = instance->get_metadata();
    spdlog::info("Loaded plugin: {} v{} by {} (id={})",
                 metadata.name, metadata.version, metadata.author, id);

    impl_->plugins[id] = std::move(instance);
    return id;
}

void PluginManager::unload_plugin(const std::string& id) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    auto it = impl_->plugins.find(id);
    if (it != impl_->plugins.end()) {
        spdlog::info("Unloading plugin: {}", id);
        impl_->plugins.erase(it);
    }
}

std::vector<PluginMetadata> PluginManager::list_plugins() const {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    std::vector<PluginMetadata> result;
    for (const auto& [id, instance] : impl_->plugins) {
        auto meta = instance->get_metadata();
        meta.id = id;
        result.push_back(meta);
    }
    return result;
}

PluginInstance* PluginManager::get_plugin(const std::string& id) const {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    auto it = impl_->plugins.find(id);
    return (it != impl_->plugins.end()) ? it->second.get() : nullptr;
}

void PluginManager::set_sandbox_config(const PluginSandboxConfig& config) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    impl_->config = config;
    impl_->runtime.shutdown();
    impl_->runtime.initialize(config);
}

PluginSandboxConfig PluginManager::get_sandbox_config() const {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    return impl_->config;
}

bool PluginManager::reload_plugin(const std::string& id, const std::vector<uint8_t>& new_wasm_bytes) {
    std::lock_guard<std::mutex> lock(impl_->mutex);

    std::string error;
    if (!impl_->runtime.validate_module(new_wasm_bytes, error)) {
        spdlog::error("Plugin reload validation failed: {}", error);
        return false;
    }

    auto new_instance = impl_->runtime.instantiate(new_wasm_bytes);
    if (!new_instance) {
        spdlog::error("Plugin reload instantiation failed");
        return false;
    }

    impl_->plugins[id] = std::move(new_instance);
    spdlog::info("Plugin reloaded: {}", id);
    return true;
}

} // namespace plugin
} // namespace rheosim
