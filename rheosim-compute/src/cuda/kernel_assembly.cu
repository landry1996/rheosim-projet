/**
 * CUDA kernel for FEM stiffness matrix assembly.
 * One thread per finite element computes local stiffness and atomically
 * accumulates into global COO sparse matrix.
 */

#include <cuda_runtime.h>
#include <cusparse.h>
#include <device_launch_parameters.h>
#include <cstdio>

namespace rheosim {
namespace gpu {
namespace cuda {

struct ElementData {
    double nodes[4 * 3];    // 4 nodes x 3 coords (tetrahedron)
    double young_modulus;
    double poisson_ratio;
};

__device__ void compute_B_matrix(const double nodes[12], double B[6][12]) {
    // Shape function derivatives for linear tetrahedron (P1)
    double x1 = nodes[0], y1 = nodes[1], z1 = nodes[2];
    double x2 = nodes[3], y2 = nodes[4], z2 = nodes[5];
    double x3 = nodes[6], y3 = nodes[7], z3 = nodes[8];
    double x4 = nodes[9], y4 = nodes[10], z4 = nodes[11];

    // Jacobian determinant (6 * volume)
    double det_J = (x2 - x1) * ((y3 - y1) * (z4 - z1) - (y4 - y1) * (z3 - z1))
                 - (x3 - x1) * ((y2 - y1) * (z4 - z1) - (y4 - y1) * (z2 - z1))
                 + (x4 - x1) * ((y2 - y1) * (z3 - z1) - (y3 - y1) * (z2 - z1));

    double inv_det = 1.0 / det_J;

    // Derivatives of shape functions (constant for P1 tetrahedron)
    double dN1_dx = inv_det * ((y3 - y4) * (z2 - z4) - (y2 - y4) * (z3 - z4));
    double dN1_dy = inv_det * ((x2 - x4) * (z3 - z4) - (x3 - x4) * (z2 - z4));
    double dN1_dz = inv_det * ((x3 - x4) * (y2 - y4) - (x2 - x4) * (y3 - y4));

    double dN2_dx = inv_det * ((y4 - y3) * (z1 - z3) - (y1 - y3) * (z4 - z3));
    double dN2_dy = inv_det * ((x1 - x3) * (z4 - z3) - (x4 - x3) * (z1 - z3));
    double dN2_dz = inv_det * ((x4 - x3) * (y1 - y3) - (x1 - x3) * (y4 - y3));

    double dN3_dx = inv_det * ((y1 - y4) * (z3 - z1) - (y3 - y1) * (z1 - z4));
    double dN3_dy = inv_det * ((x3 - x1) * (z1 - z4) - (x1 - x4) * (z3 - z1));
    double dN3_dz = inv_det * ((x1 - x4) * (y3 - y1) - (x3 - x1) * (y1 - y4));

    double dN4_dx = -(dN1_dx + dN2_dx + dN3_dx);
    double dN4_dy = -(dN1_dy + dN2_dy + dN3_dy);
    double dN4_dz = -(dN1_dz + dN2_dz + dN3_dz);

    // Build strain-displacement matrix B (6x12)
    for (int i = 0; i < 6; i++)
        for (int j = 0; j < 12; j++)
            B[i][j] = 0.0;

    double dN[4][3] = {
        {dN1_dx, dN1_dy, dN1_dz},
        {dN2_dx, dN2_dy, dN2_dz},
        {dN3_dx, dN3_dy, dN3_dz},
        {dN4_dx, dN4_dy, dN4_dz}
    };

    for (int n = 0; n < 4; n++) {
        int col = n * 3;
        B[0][col]     = dN[n][0];
        B[1][col + 1] = dN[n][1];
        B[2][col + 2] = dN[n][2];
        B[3][col]     = dN[n][1]; B[3][col + 1] = dN[n][0];
        B[4][col + 1] = dN[n][2]; B[4][col + 2] = dN[n][1];
        B[5][col]     = dN[n][2]; B[5][col + 2] = dN[n][0];
    }
}

__device__ double compute_volume(const double nodes[12]) {
    double x1 = nodes[0], y1 = nodes[1], z1 = nodes[2];
    double x2 = nodes[3], y2 = nodes[4], z2 = nodes[5];
    double x3 = nodes[6], y3 = nodes[7], z3 = nodes[8];
    double x4 = nodes[9], y4 = nodes[10], z4 = nodes[11];

    double det = (x2 - x1) * ((y3 - y1) * (z4 - z1) - (y4 - y1) * (z3 - z1))
               - (x3 - x1) * ((y2 - y1) * (z4 - z1) - (y4 - y1) * (z2 - z1))
               + (x4 - x1) * ((y2 - y1) * (z3 - z1) - (y3 - y1) * (z2 - z1));

    return fabs(det) / 6.0;
}

__global__ void cu_assemble_stiffness_kernel(
    const double* __restrict__ nodes,
    const int* __restrict__ elements,
    const double* __restrict__ material_props,
    int num_elements,
    int* coo_rows,
    int* coo_cols,
    double* coo_vals,
    int entries_per_element)
{
    int elem_idx = blockIdx.x * blockDim.x + threadIdx.x;
    if (elem_idx >= num_elements) return;

    // Get element node indices
    int n0 = elements[elem_idx * 4 + 0];
    int n1 = elements[elem_idx * 4 + 1];
    int n2 = elements[elem_idx * 4 + 2];
    int n3 = elements[elem_idx * 4 + 3];

    // Gather node coordinates
    double elem_nodes[12];
    for (int i = 0; i < 3; i++) {
        elem_nodes[0 * 3 + i] = nodes[n0 * 3 + i];
        elem_nodes[1 * 3 + i] = nodes[n1 * 3 + i];
        elem_nodes[2 * 3 + i] = nodes[n2 * 3 + i];
        elem_nodes[3 * 3 + i] = nodes[n3 * 3 + i];
    }

    // Material properties
    double E = material_props[elem_idx * 2 + 0];
    double nu = material_props[elem_idx * 2 + 1];

    // Elasticity matrix D (isotropic, 3D)
    double lambda = E * nu / ((1.0 + nu) * (1.0 - 2.0 * nu));
    double mu = E / (2.0 * (1.0 + nu));

    double D[6][6];
    for (int i = 0; i < 6; i++)
        for (int j = 0; j < 6; j++)
            D[i][j] = 0.0;

    D[0][0] = D[1][1] = D[2][2] = lambda + 2.0 * mu;
    D[0][1] = D[0][2] = D[1][0] = D[1][2] = D[2][0] = D[2][1] = lambda;
    D[3][3] = D[4][4] = D[5][5] = mu;

    // B matrix
    double B[6][12];
    compute_B_matrix(elem_nodes, B);

    double volume = compute_volume(elem_nodes);

    // Compute local stiffness: Ke = V * B^T * D * B
    double Ke[12][12];
    for (int i = 0; i < 12; i++) {
        for (int j = 0; j < 12; j++) {
            double sum = 0.0;
            for (int k = 0; k < 6; k++) {
                double db = 0.0;
                for (int l = 0; l < 6; l++) {
                    db += D[k][l] * B[l][j];
                }
                sum += B[k][i] * db;
            }
            Ke[i][j] = sum * volume;
        }
    }

    // Write to COO format
    int dof_map[12];
    int node_ids[4] = {n0, n1, n2, n3};
    for (int n = 0; n < 4; n++) {
        dof_map[n * 3 + 0] = node_ids[n] * 3 + 0;
        dof_map[n * 3 + 1] = node_ids[n] * 3 + 1;
        dof_map[n * 3 + 2] = node_ids[n] * 3 + 2;
    }

    int base = elem_idx * entries_per_element;
    int idx = 0;
    for (int i = 0; i < 12; i++) {
        for (int j = 0; j < 12; j++) {
            coo_rows[base + idx] = dof_map[i];
            coo_cols[base + idx] = dof_map[j];
            coo_vals[base + idx] = Ke[i][j];
            idx++;
        }
    }
}

extern "C" {

void cu_assemble_stiffness(
    const double* h_nodes, int num_nodes,
    const int* h_elements, int num_elements,
    const double* h_material_props,
    int** h_coo_rows, int** h_coo_cols, double** h_coo_vals,
    int* nnz)
{
    const int entries_per_element = 144; // 12x12
    *nnz = num_elements * entries_per_element;

    // Device memory
    double* d_nodes;
    int* d_elements;
    double* d_material_props;
    int* d_coo_rows;
    int* d_coo_cols;
    double* d_coo_vals;

    cudaMalloc(&d_nodes, num_nodes * 3 * sizeof(double));
    cudaMalloc(&d_elements, num_elements * 4 * sizeof(int));
    cudaMalloc(&d_material_props, num_elements * 2 * sizeof(double));
    cudaMalloc(&d_coo_rows, (*nnz) * sizeof(int));
    cudaMalloc(&d_coo_cols, (*nnz) * sizeof(int));
    cudaMalloc(&d_coo_vals, (*nnz) * sizeof(double));

    // Transfer to device
    cudaMemcpy(d_nodes, h_nodes, num_nodes * 3 * sizeof(double), cudaMemcpyHostToDevice);
    cudaMemcpy(d_elements, h_elements, num_elements * 4 * sizeof(int), cudaMemcpyHostToDevice);
    cudaMemcpy(d_material_props, h_material_props, num_elements * 2 * sizeof(double), cudaMemcpyHostToDevice);

    // Launch kernel
    int block_size = 256;
    int grid_size = (num_elements + block_size - 1) / block_size;
    cu_assemble_stiffness_kernel<<<grid_size, block_size>>>(
        d_nodes, d_elements, d_material_props,
        num_elements, d_coo_rows, d_coo_cols, d_coo_vals,
        entries_per_element);

    cudaDeviceSynchronize();

    // Transfer back
    *h_coo_rows = new int[*nnz];
    *h_coo_cols = new int[*nnz];
    *h_coo_vals = new double[*nnz];

    cudaMemcpy(*h_coo_rows, d_coo_rows, (*nnz) * sizeof(int), cudaMemcpyDeviceToHost);
    cudaMemcpy(*h_coo_cols, d_coo_cols, (*nnz) * sizeof(int), cudaMemcpyDeviceToHost);
    cudaMemcpy(*h_coo_vals, d_coo_vals, (*nnz) * sizeof(double), cudaMemcpyDeviceToHost);

    // Cleanup
    cudaFree(d_nodes);
    cudaFree(d_elements);
    cudaFree(d_material_props);
    cudaFree(d_coo_rows);
    cudaFree(d_coo_cols);
    cudaFree(d_coo_vals);
}

} // extern "C"

} // namespace cuda
} // namespace gpu
} // namespace rheosim
