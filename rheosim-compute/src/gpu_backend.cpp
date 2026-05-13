#include "rheosim/gpu_backend.h"
#include "rheosim/cuda_backend.h"
#include "rheosim/opencl_backend.h"
#include <spdlog/spdlog.h>
#include <algorithm>

namespace rheosim {
namespace gpu {

DeviceManager& DeviceManager::instance() {
    static DeviceManager mgr;
    return mgr;
}

DeviceManager::DeviceManager() {
    // Probe CUDA
    {
        auto cuda = std::make_unique<CudaBackend>();
        if (cuda->initialize()) {
            available_devices_.push_back(cuda->get_device_info());
            cuda->shutdown();
            spdlog::info("CUDA device detected: {}", available_devices_.back().name);
        }
    }

    // Probe OpenCL
    {
        auto opencl = std::make_unique<OpenClBackend>();
        if (opencl->initialize()) {
            available_devices_.push_back(opencl->get_device_info());
            opencl->shutdown();
            spdlog::info("OpenCL device detected: {}", available_devices_.back().name);
        }
    }

    // Always add CPU fallback
    DeviceInfo cpu_info;
    cpu_info.type = DeviceType::CPU;
    cpu_info.name = "CPU (OpenMP)";
    cpu_info.memory_bytes = 0;
    cpu_info.compute_units = 0;
    cpu_info.available = true;
    available_devices_.push_back(cpu_info);
}

DeviceType DeviceManager::detect_gpu() {
    for (const auto& dev : available_devices_) {
        if (dev.type == DeviceType::CUDA && dev.available) {
            return DeviceType::CUDA;
        }
    }
    for (const auto& dev : available_devices_) {
        if (dev.type == DeviceType::OPENCL && dev.available) {
            return DeviceType::OPENCL;
        }
    }
    return DeviceType::CPU;
}

std::unique_ptr<GpuBackend> DeviceManager::select_device(size_t problem_size, UserTier tier) {
    // FREE tier: CPU only
    if (tier == UserTier::FREE) {
        spdlog::info("FREE tier: forcing CPU backend");
        return nullptr;
    }

    // Small problems: CPU is more efficient (avoid transfer overhead)
    if (problem_size < GPU_THRESHOLD) {
        spdlog::info("Problem size {} < threshold {}: using CPU", problem_size, GPU_THRESHOLD);
        return nullptr;
    }

    DeviceType target = (preferred_device_ != DeviceType::CPU)
        ? preferred_device_
        : detect_gpu();

    switch (target) {
        case DeviceType::CUDA: {
            auto backend = std::make_unique<CudaBackend>();
            if (backend->initialize()) {
                spdlog::info("Using CUDA backend for problem size {}", problem_size);
                return backend;
            }
            spdlog::warn("CUDA initialization failed, falling back");
            [[fallthrough]];
        }
        case DeviceType::OPENCL: {
            auto backend = std::make_unique<OpenClBackend>();
            if (backend->initialize()) {
                spdlog::info("Using OpenCL backend for problem size {}", problem_size);
                return backend;
            }
            spdlog::warn("OpenCL initialization failed, falling back to CPU");
            [[fallthrough]];
        }
        case DeviceType::CPU:
        default:
            return nullptr;
    }
}

std::vector<DeviceInfo> DeviceManager::list_devices() const {
    return available_devices_;
}

void DeviceManager::set_preferred_device(DeviceType type) {
    preferred_device_ = type;
}

DeviceType DeviceManager::get_preferred_device() const {
    return preferred_device_;
}

} // namespace gpu
} // namespace rheosim
