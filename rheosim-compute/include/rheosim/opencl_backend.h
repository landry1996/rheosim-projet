#pragma once

#include "rheosim/gpu_backend.h"

namespace rheosim {
namespace gpu {

class OpenClBackend : public GpuBackend {
public:
    OpenClBackend();
    ~OpenClBackend() override;

    bool initialize() override;
    void shutdown() override;
    DeviceInfo get_device_info() const override;

    void assemble_stiffness(
        const std::vector<double>& nodes,
        const std::vector<int>& elements,
        const std::vector<double>& material_props,
        Eigen::SparseMatrix<double>& global_stiffness) override;

    void solve_system(
        const Eigen::SparseMatrix<double>& A,
        const Eigen::VectorXd& b,
        Eigen::VectorXd& x) override;

    void compute_jacobian_batch(
        const std::vector<double>& parameters,
        const std::vector<double>& data_points,
        std::vector<double>& jacobian) override;

private:
    struct OpenClContext;
    std::unique_ptr<OpenClContext> ctx_;
    bool initialized_ = false;
};

} // namespace gpu
} // namespace rheosim
