#pragma once

#include <memory>
#include <string>
#include <vector>
#include <Eigen/Sparse>

namespace rheosim {
namespace gpu {

enum class DeviceType {
    CPU,
    CUDA,
    OPENCL
};

enum class UserTier {
    FREE,
    PRO,
    ENTERPRISE
};

struct DeviceInfo {
    DeviceType type;
    std::string name;
    size_t memory_bytes;
    int compute_units;
    bool available;
};

class GpuBackend {
public:
    virtual ~GpuBackend() = default;

    virtual bool initialize() = 0;
    virtual void shutdown() = 0;
    virtual DeviceInfo get_device_info() const = 0;

    virtual void assemble_stiffness(
        const std::vector<double>& nodes,
        const std::vector<int>& elements,
        const std::vector<double>& material_props,
        Eigen::SparseMatrix<double>& global_stiffness) = 0;

    virtual void solve_system(
        const Eigen::SparseMatrix<double>& A,
        const Eigen::VectorXd& b,
        Eigen::VectorXd& x) = 0;

    virtual void compute_jacobian_batch(
        const std::vector<double>& parameters,
        const std::vector<double>& data_points,
        std::vector<double>& jacobian) = 0;
};

class DeviceManager {
public:
    static DeviceManager& instance();

    DeviceType detect_gpu();
    std::unique_ptr<GpuBackend> select_device(size_t problem_size, UserTier tier);
    std::vector<DeviceInfo> list_devices() const;

    void set_preferred_device(DeviceType type);
    DeviceType get_preferred_device() const;

private:
    DeviceManager();
    DeviceType preferred_device_ = DeviceType::CPU;
    std::vector<DeviceInfo> available_devices_;

    static constexpr size_t GPU_THRESHOLD = 10000;
};

} // namespace gpu
} // namespace rheosim
