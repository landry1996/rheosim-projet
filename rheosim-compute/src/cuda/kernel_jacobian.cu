/**
 * CUDA kernel for batch Jacobian computation in Levenberg-Marquardt optimization.
 * Each thread computes one column of the Jacobian via finite differences.
 */

#include <cuda_runtime.h>
#include <device_launch_parameters.h>
#include <cmath>

namespace rheosim {
namespace gpu {
namespace cuda {

__device__ double prony_relaxation(double t, const double* params, int n_terms) {
    // G(t) = G_inf + sum_i G_i * exp(-t / tau_i)
    double G_inf = params[0];
    double result = G_inf;
    for (int i = 0; i < n_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        result += G_i * exp(-t / tau_i);
    }
    return result;
}

__global__ void cu_jacobian_kernel(
    const double* __restrict__ params,
    int n_params,
    const double* __restrict__ data_points,
    int n_points,
    int n_prony_terms,
    double* __restrict__ jacobian,
    double epsilon)
{
    int idx = blockIdx.x * blockDim.x + threadIdx.x;
    int total_work = n_points * n_params;
    if (idx >= total_work) return;

    int point_idx = idx / n_params;
    int param_idx = idx % n_params;

    double t = data_points[point_idx];

    // Forward difference: J[i][j] = (f(p + eps*e_j) - f(p)) / eps
    double f0 = prony_relaxation(t, params, n_prony_terms);

    // Perturb parameter
    double h = epsilon * fmax(fabs(params[param_idx]), 1.0);

    // Copy params to local (thread-local perturbation)
    extern __shared__ double s_params[];
    if (threadIdx.x == 0) {
        for (int i = 0; i < n_params; i++) {
            s_params[i] = params[i];
        }
    }
    __syncthreads();

    // Use register-based perturbation for this specific parameter
    double perturbed_val = params[param_idx] + h;
    double original_val = params[param_idx];

    // Compute perturbed function value
    double G_inf = (param_idx == 0) ? perturbed_val : params[0];
    double result = G_inf;
    for (int i = 0; i < n_prony_terms; i++) {
        int g_idx = 1 + 2 * i;
        int tau_idx = 2 + 2 * i;
        double G_i = (param_idx == g_idx) ? perturbed_val : params[g_idx];
        double tau_i = (param_idx == tau_idx) ? perturbed_val : params[tau_idx];
        result += G_i * exp(-t / tau_i);
    }

    double f1 = result;
    jacobian[point_idx * n_params + param_idx] = (f1 - f0) / h;
}

extern "C" {

void cu_compute_jacobian_batch(
    const double* h_params, int n_params,
    const double* h_data_points, int n_points,
    int n_prony_terms,
    double* h_jacobian)
{
    double* d_params;
    double* d_data_points;
    double* d_jacobian;

    cudaMalloc(&d_params, n_params * sizeof(double));
    cudaMalloc(&d_data_points, n_points * sizeof(double));
    cudaMalloc(&d_jacobian, n_points * n_params * sizeof(double));

    cudaMemcpy(d_params, h_params, n_params * sizeof(double), cudaMemcpyHostToDevice);
    cudaMemcpy(d_data_points, h_data_points, n_points * sizeof(double), cudaMemcpyHostToDevice);

    int total_work = n_points * n_params;
    int block_size = 256;
    int grid_size = (total_work + block_size - 1) / block_size;
    int shared_mem = n_params * sizeof(double);

    cu_jacobian_kernel<<<grid_size, block_size, shared_mem>>>(
        d_params, n_params,
        d_data_points, n_points,
        n_prony_terms,
        d_jacobian,
        1e-8);

    cudaDeviceSynchronize();

    cudaMemcpy(h_jacobian, d_jacobian, n_points * n_params * sizeof(double), cudaMemcpyDeviceToHost);

    cudaFree(d_params);
    cudaFree(d_data_points);
    cudaFree(d_jacobian);
}

} // extern "C"

} // namespace cuda
} // namespace gpu
} // namespace rheosim
