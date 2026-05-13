#include "rheosim/opencl_backend.h"
#include <spdlog/spdlog.h>
#include <fstream>
#include <sstream>

#ifdef RHEOSIM_USE_OPENCL
#include <CL/cl.h>
#endif

namespace rheosim {
namespace gpu {

struct OpenClBackend::OpenClContext {
#ifdef RHEOSIM_USE_OPENCL
    cl_platform_id platform = nullptr;
    cl_device_id device = nullptr;
    cl_context context = nullptr;
    cl_command_queue queue = nullptr;
    cl_program program = nullptr;
    cl_kernel kernel_assemble = nullptr;
    cl_kernel kernel_solve = nullptr;
    cl_kernel kernel_jacobian = nullptr;
#endif
    std::string device_name;
    size_t global_mem_size = 0;
    int compute_units = 0;
};

OpenClBackend::OpenClBackend() : ctx_(std::make_unique<OpenClContext>()) {}
OpenClBackend::~OpenClBackend() { shutdown(); }

bool OpenClBackend::initialize() {
#ifdef RHEOSIM_USE_OPENCL
    cl_uint num_platforms;
    cl_int err = clGetPlatformIDs(0, nullptr, &num_platforms);
    if (err != CL_SUCCESS || num_platforms == 0) {
        spdlog::debug("No OpenCL platforms found");
        return false;
    }

    std::vector<cl_platform_id> platforms(num_platforms);
    clGetPlatformIDs(num_platforms, platforms.data(), nullptr);
    ctx_->platform = platforms[0];

    cl_uint num_devices;
    err = clGetDeviceIDs(ctx_->platform, CL_DEVICE_TYPE_GPU, 0, nullptr, &num_devices);
    if (err != CL_SUCCESS || num_devices == 0) {
        spdlog::debug("No OpenCL GPU devices found");
        return false;
    }

    std::vector<cl_device_id> devices(num_devices);
    clGetDeviceIDs(ctx_->platform, CL_DEVICE_TYPE_GPU, num_devices, devices.data(), nullptr);
    ctx_->device = devices[0];

    // Get device info
    char name[256];
    clGetDeviceInfo(ctx_->device, CL_DEVICE_NAME, sizeof(name), name, nullptr);
    ctx_->device_name = name;

    cl_ulong mem_size;
    clGetDeviceInfo(ctx_->device, CL_DEVICE_GLOBAL_MEM_SIZE, sizeof(mem_size), &mem_size, nullptr);
    ctx_->global_mem_size = mem_size;

    cl_uint cu;
    clGetDeviceInfo(ctx_->device, CL_DEVICE_MAX_COMPUTE_UNITS, sizeof(cu), &cu, nullptr);
    ctx_->compute_units = cu;

    // Create context and command queue
    ctx_->context = clCreateContext(nullptr, 1, &ctx_->device, nullptr, nullptr, &err);
    if (err != CL_SUCCESS) return false;

    ctx_->queue = clCreateCommandQueue(ctx_->context, ctx_->device, 0, &err);
    if (err != CL_SUCCESS) return false;

    // Load and build kernel source
    std::string kernel_path = "src/opencl/kernels.cl";
    std::ifstream file(kernel_path);
    if (!file.is_open()) {
        spdlog::warn("OpenCL kernel file not found: {}", kernel_path);
        return false;
    }

    std::stringstream ss;
    ss << file.rdbuf();
    std::string source = ss.str();
    const char* src_ptr = source.c_str();
    size_t src_len = source.size();

    ctx_->program = clCreateProgramWithSource(ctx_->context, 1, &src_ptr, &src_len, &err);
    if (err != CL_SUCCESS) return false;

    err = clBuildProgram(ctx_->program, 1, &ctx_->device, "-cl-std=CL2.0", nullptr, nullptr);
    if (err != CL_SUCCESS) {
        char log[4096];
        clGetProgramBuildInfo(ctx_->program, ctx_->device, CL_PROGRAM_BUILD_LOG, sizeof(log), log, nullptr);
        spdlog::error("OpenCL build error: {}", log);
        return false;
    }

    ctx_->kernel_assemble = clCreateKernel(ctx_->program, "cl_assemble_stiffness", &err);
    ctx_->kernel_solve = clCreateKernel(ctx_->program, "cl_solve_jacobi", &err);
    ctx_->kernel_jacobian = clCreateKernel(ctx_->program, "cl_compute_jacobian", &err);

    initialized_ = true;
    spdlog::info("OpenCL initialized: {} ({} CUs, {} MB)",
                 ctx_->device_name, ctx_->compute_units, ctx_->global_mem_size / (1024 * 1024));
    return true;
#else
    spdlog::debug("OpenCL support not compiled");
    return false;
#endif
}

void OpenClBackend::shutdown() {
#ifdef RHEOSIM_USE_OPENCL
    if (!initialized_) return;

    if (ctx_->kernel_assemble) clReleaseKernel(ctx_->kernel_assemble);
    if (ctx_->kernel_solve) clReleaseKernel(ctx_->kernel_solve);
    if (ctx_->kernel_jacobian) clReleaseKernel(ctx_->kernel_jacobian);
    if (ctx_->program) clReleaseProgram(ctx_->program);
    if (ctx_->queue) clReleaseCommandQueue(ctx_->queue);
    if (ctx_->context) clReleaseContext(ctx_->context);

    initialized_ = false;
#endif
}

DeviceInfo OpenClBackend::get_device_info() const {
    DeviceInfo info;
    info.type = DeviceType::OPENCL;
    info.name = ctx_->device_name;
    info.memory_bytes = ctx_->global_mem_size;
    info.compute_units = ctx_->compute_units;
    info.available = initialized_;
    return info;
}

void OpenClBackend::assemble_stiffness(
    const std::vector<double>& nodes,
    const std::vector<int>& elements,
    const std::vector<double>& material_props,
    Eigen::SparseMatrix<double>& global_stiffness)
{
#ifdef RHEOSIM_USE_OPENCL
    if (!initialized_) return;

    int num_nodes = nodes.size() / 3;
    int num_elements = elements.size() / 4;
    int nnz = num_elements * 144;

    cl_int err;
    cl_mem d_nodes = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    nodes.size() * sizeof(double), (void*)nodes.data(), &err);
    cl_mem d_elements = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                       elements.size() * sizeof(int), (void*)elements.data(), &err);
    cl_mem d_material = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                       material_props.size() * sizeof(double), (void*)material_props.data(), &err);
    cl_mem d_rows = clCreateBuffer(ctx_->context, CL_MEM_WRITE_ONLY, nnz * sizeof(int), nullptr, &err);
    cl_mem d_cols = clCreateBuffer(ctx_->context, CL_MEM_WRITE_ONLY, nnz * sizeof(int), nullptr, &err);
    cl_mem d_vals = clCreateBuffer(ctx_->context, CL_MEM_WRITE_ONLY, nnz * sizeof(double), nullptr, &err);

    clSetKernelArg(ctx_->kernel_assemble, 0, sizeof(cl_mem), &d_nodes);
    clSetKernelArg(ctx_->kernel_assemble, 1, sizeof(cl_mem), &d_elements);
    clSetKernelArg(ctx_->kernel_assemble, 2, sizeof(cl_mem), &d_material);
    clSetKernelArg(ctx_->kernel_assemble, 3, sizeof(int), &num_elements);
    clSetKernelArg(ctx_->kernel_assemble, 4, sizeof(cl_mem), &d_rows);
    clSetKernelArg(ctx_->kernel_assemble, 5, sizeof(cl_mem), &d_cols);
    clSetKernelArg(ctx_->kernel_assemble, 6, sizeof(cl_mem), &d_vals);

    size_t global_size = ((num_elements + 255) / 256) * 256;
    clEnqueueNDRangeKernel(ctx_->queue, ctx_->kernel_assemble, 1, nullptr, &global_size, nullptr, 0, nullptr, nullptr);
    clFinish(ctx_->queue);

    std::vector<int> h_rows(nnz), h_cols(nnz);
    std::vector<double> h_vals(nnz);
    clEnqueueReadBuffer(ctx_->queue, d_rows, CL_TRUE, 0, nnz * sizeof(int), h_rows.data(), 0, nullptr, nullptr);
    clEnqueueReadBuffer(ctx_->queue, d_cols, CL_TRUE, 0, nnz * sizeof(int), h_cols.data(), 0, nullptr, nullptr);
    clEnqueueReadBuffer(ctx_->queue, d_vals, CL_TRUE, 0, nnz * sizeof(double), h_vals.data(), 0, nullptr, nullptr);

    // Build Eigen sparse matrix from COO
    int ndof = num_nodes * 3;
    std::vector<Eigen::Triplet<double>> triplets;
    triplets.reserve(nnz);
    for (int i = 0; i < nnz; i++) {
        if (h_vals[i] != 0.0) {
            triplets.emplace_back(h_rows[i], h_cols[i], h_vals[i]);
        }
    }
    global_stiffness.resize(ndof, ndof);
    global_stiffness.setFromTriplets(triplets.begin(), triplets.end());

    clReleaseMemObject(d_nodes);
    clReleaseMemObject(d_elements);
    clReleaseMemObject(d_material);
    clReleaseMemObject(d_rows);
    clReleaseMemObject(d_cols);
    clReleaseMemObject(d_vals);
#endif
}

void OpenClBackend::solve_system(
    const Eigen::SparseMatrix<double>& A,
    const Eigen::VectorXd& b,
    Eigen::VectorXd& x)
{
#ifdef RHEOSIM_USE_OPENCL
    if (!initialized_) return;

    // Convert Eigen sparse to CSR for OpenCL Jacobi solver
    int n = A.rows();
    int nnz = A.nonZeros();

    const int* outer = A.outerIndexPtr();
    const int* inner = A.innerIndexPtr();
    const double* values = A.valuePtr();

    cl_int err;
    cl_mem d_row_ptr = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                      (n + 1) * sizeof(int), (void*)outer, &err);
    cl_mem d_col_ind = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                      nnz * sizeof(int), (void*)inner, &err);
    cl_mem d_vals = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                   nnz * sizeof(double), (void*)values, &err);
    cl_mem d_b = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                n * sizeof(double), (void*)b.data(), &err);
    cl_mem d_x = clCreateBuffer(ctx_->context, CL_MEM_READ_WRITE, n * sizeof(double), nullptr, &err);
    cl_mem d_x_new = clCreateBuffer(ctx_->context, CL_MEM_READ_WRITE, n * sizeof(double), nullptr, &err);

    // Initialize x to zero
    std::vector<double> zeros(n, 0.0);
    clEnqueueWriteBuffer(ctx_->queue, d_x, CL_TRUE, 0, n * sizeof(double), zeros.data(), 0, nullptr, nullptr);

    clSetKernelArg(ctx_->kernel_solve, 0, sizeof(cl_mem), &d_vals);
    clSetKernelArg(ctx_->kernel_solve, 1, sizeof(cl_mem), &d_row_ptr);
    clSetKernelArg(ctx_->kernel_solve, 2, sizeof(cl_mem), &d_col_ind);
    clSetKernelArg(ctx_->kernel_solve, 3, sizeof(cl_mem), &d_b);
    clSetKernelArg(ctx_->kernel_solve, 6, sizeof(int), &n);

    size_t global_size = ((n + 255) / 256) * 256;
    int max_iter = 10000;

    for (int iter = 0; iter < max_iter; iter++) {
        clSetKernelArg(ctx_->kernel_solve, 4, sizeof(cl_mem), &d_x);
        clSetKernelArg(ctx_->kernel_solve, 5, sizeof(cl_mem), &d_x_new);
        clEnqueueNDRangeKernel(ctx_->queue, ctx_->kernel_solve, 1, nullptr, &global_size, nullptr, 0, nullptr, nullptr);
        std::swap(d_x, d_x_new);
    }
    clFinish(ctx_->queue);

    x.resize(n);
    clEnqueueReadBuffer(ctx_->queue, d_x, CL_TRUE, 0, n * sizeof(double), x.data(), 0, nullptr, nullptr);

    clReleaseMemObject(d_row_ptr);
    clReleaseMemObject(d_col_ind);
    clReleaseMemObject(d_vals);
    clReleaseMemObject(d_b);
    clReleaseMemObject(d_x);
    clReleaseMemObject(d_x_new);
#endif
}

void OpenClBackend::compute_jacobian_batch(
    const std::vector<double>& parameters,
    const std::vector<double>& data_points,
    std::vector<double>& jacobian)
{
#ifdef RHEOSIM_USE_OPENCL
    if (!initialized_) return;

    int n_params = parameters.size();
    int n_points = data_points.size();
    int n_prony_terms = (n_params - 1) / 2;
    double epsilon = 1e-8;

    jacobian.resize(n_points * n_params);

    cl_int err;
    cl_mem d_params = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                     n_params * sizeof(double), (void*)parameters.data(), &err);
    cl_mem d_points = clCreateBuffer(ctx_->context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                     n_points * sizeof(double), (void*)data_points.data(), &err);
    cl_mem d_jac = clCreateBuffer(ctx_->context, CL_MEM_WRITE_ONLY,
                                  n_points * n_params * sizeof(double), nullptr, &err);

    clSetKernelArg(ctx_->kernel_jacobian, 0, sizeof(cl_mem), &d_params);
    clSetKernelArg(ctx_->kernel_jacobian, 1, sizeof(int), &n_params);
    clSetKernelArg(ctx_->kernel_jacobian, 2, sizeof(cl_mem), &d_points);
    clSetKernelArg(ctx_->kernel_jacobian, 3, sizeof(int), &n_points);
    clSetKernelArg(ctx_->kernel_jacobian, 4, sizeof(int), &n_prony_terms);
    clSetKernelArg(ctx_->kernel_jacobian, 5, sizeof(cl_mem), &d_jac);
    clSetKernelArg(ctx_->kernel_jacobian, 6, sizeof(double), &epsilon);

    int total_work = n_points * n_params;
    size_t global_size = ((total_work + 255) / 256) * 256;
    clEnqueueNDRangeKernel(ctx_->queue, ctx_->kernel_jacobian, 1, nullptr, &global_size, nullptr, 0, nullptr, nullptr);
    clFinish(ctx_->queue);

    clEnqueueReadBuffer(ctx_->queue, d_jac, CL_TRUE, 0,
                        n_points * n_params * sizeof(double), jacobian.data(), 0, nullptr, nullptr);

    clReleaseMemObject(d_params);
    clReleaseMemObject(d_points);
    clReleaseMemObject(d_jac);
#endif
}

} // namespace gpu
} // namespace rheosim
