/**
 * CUDA solver kernels using cuSOLVER (Cholesky) and AmgX (iterative).
 */

#include <cuda_runtime.h>
#include <cusolverSp.h>
#include <cusparse.h>
#include <cstdio>

namespace rheosim {
namespace gpu {
namespace cuda {

struct CuSolverContext {
    cusolverSpHandle_t solver_handle;
    cusparseHandle_t sparse_handle;
    cusparseMatDescr_t descr;
};

extern "C" {

void* cu_solver_create() {
    auto* ctx = new CuSolverContext();
    cusolverSpCreate(&ctx->solver_handle);
    cusparseCreate(&ctx->sparse_handle);
    cusparseCreateMatDescr(&ctx->descr);
    cusparseSetMatType(ctx->descr, CUSPARSE_MATRIX_TYPE_GENERAL);
    cusparseSetMatIndexBase(ctx->descr, CUSPARSE_INDEX_BASE_ZERO);
    return ctx;
}

void cu_solver_destroy(void* context) {
    auto* ctx = static_cast<CuSolverContext*>(context);
    cusparseDestroyMatDescr(ctx->descr);
    cusparseDestroy(ctx->sparse_handle);
    cusolverSpDestroy(ctx->solver_handle);
    delete ctx;
}

int cu_solve_cholesky(
    void* context,
    int n, int nnz,
    const int* h_csr_row_ptr,
    const int* h_csr_col_ind,
    const double* h_csr_vals,
    const double* h_b,
    double* h_x)
{
    auto* ctx = static_cast<CuSolverContext*>(context);

    // Allocate device memory
    int* d_csr_row_ptr;
    int* d_csr_col_ind;
    double* d_csr_vals;
    double* d_b;
    double* d_x;

    cudaMalloc(&d_csr_row_ptr, (n + 1) * sizeof(int));
    cudaMalloc(&d_csr_col_ind, nnz * sizeof(int));
    cudaMalloc(&d_csr_vals, nnz * sizeof(double));
    cudaMalloc(&d_b, n * sizeof(double));
    cudaMalloc(&d_x, n * sizeof(double));

    // Copy to device
    cudaMemcpy(d_csr_row_ptr, h_csr_row_ptr, (n + 1) * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_csr_col_ind, h_csr_col_ind, nnz * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_csr_vals, h_csr_vals, nnz * sizeof(double), cudaMemcpyHostToDevice);
    cudaMemcpy(d_b, h_b, n * sizeof(double), cudaMemcpyHostToDevice);

    // Solve using Cholesky factorization (for SPD matrices)
    int singularity = -1;
    cusolverStatus_t status = cusolverSpDcsrlsvchol(
        ctx->solver_handle,
        n, nnz,
        ctx->descr,
        d_csr_vals, d_csr_row_ptr, d_csr_col_ind,
        d_b,
        1e-12,  // tolerance
        0,      // reorder (0 = no reorder)
        d_x,
        &singularity);

    if (status != CUSOLVER_STATUS_SUCCESS || singularity >= 0) {
        cudaFree(d_csr_row_ptr);
        cudaFree(d_csr_col_ind);
        cudaFree(d_csr_vals);
        cudaFree(d_b);
        cudaFree(d_x);
        return -1;
    }

    // Copy result back
    cudaMemcpy(h_x, d_x, n * sizeof(double), cudaMemcpyDeviceToHost);

    // Cleanup
    cudaFree(d_csr_row_ptr);
    cudaFree(d_csr_col_ind);
    cudaFree(d_csr_vals);
    cudaFree(d_b);
    cudaFree(d_x);

    return 0;
}

int cu_solve_cg(
    void* context,
    int n, int nnz,
    const int* h_csr_row_ptr,
    const int* h_csr_col_ind,
    const double* h_csr_vals,
    const double* h_b,
    double* h_x,
    int max_iter,
    double tol)
{
    auto* ctx = static_cast<CuSolverContext*>(context);

    int* d_csr_row_ptr;
    int* d_csr_col_ind;
    double* d_csr_vals;
    double* d_b;
    double* d_x;
    double* d_r;
    double* d_p;
    double* d_Ap;

    cudaMalloc(&d_csr_row_ptr, (n + 1) * sizeof(int));
    cudaMalloc(&d_csr_col_ind, nnz * sizeof(int));
    cudaMalloc(&d_csr_vals, nnz * sizeof(double));
    cudaMalloc(&d_b, n * sizeof(double));
    cudaMalloc(&d_x, n * sizeof(double));
    cudaMalloc(&d_r, n * sizeof(double));
    cudaMalloc(&d_p, n * sizeof(double));
    cudaMalloc(&d_Ap, n * sizeof(double));

    cudaMemcpy(d_csr_row_ptr, h_csr_row_ptr, (n + 1) * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_csr_col_ind, h_csr_col_ind, nnz * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_csr_vals, h_csr_vals, nnz * sizeof(double), cudaMemcpyHostToDevice);
    cudaMemcpy(d_b, h_b, n * sizeof(double), cudaMemcpyHostToDevice);
    cudaMemset(d_x, 0, n * sizeof(double));

    // CG iterations using cuSPARSE for SpMV
    // (Simplified — production would use cuSPARSE SpMV API)
    int converged = 0;
    // Placeholder: actual CG loop with cuBLAS/cuSPARSE calls
    // For large systems (>500k DOF), AmgX would be preferred

    cudaMemcpy(h_x, d_x, n * sizeof(double), cudaMemcpyDeviceToHost);

    cudaFree(d_csr_row_ptr);
    cudaFree(d_csr_col_ind);
    cudaFree(d_csr_vals);
    cudaFree(d_b);
    cudaFree(d_x);
    cudaFree(d_r);
    cudaFree(d_p);
    cudaFree(d_Ap);

    return converged ? 0 : -1;
}

} // extern "C"

} // namespace cuda
} // namespace gpu
} // namespace rheosim
