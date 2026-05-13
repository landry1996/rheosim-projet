/**
 * OpenCL kernels for FEM stiffness assembly and solving.
 * Provides AMD/Intel GPU support as fallback when CUDA is unavailable.
 */

__kernel void cl_assemble_stiffness(
    __global const double* nodes,
    __global const int* elements,
    __global const double* material_props,
    int num_elements,
    __global int* coo_rows,
    __global int* coo_cols,
    __global double* coo_vals)
{
    int elem_idx = get_global_id(0);
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

    // Lame parameters
    double lambda = E * nu / ((1.0 + nu) * (1.0 - 2.0 * nu));
    double mu = E / (2.0 * (1.0 + nu));

    // Compute volume
    double x1 = elem_nodes[0], y1 = elem_nodes[1], z1 = elem_nodes[2];
    double x2 = elem_nodes[3], y2 = elem_nodes[4], z2 = elem_nodes[5];
    double x3 = elem_nodes[6], y3 = elem_nodes[7], z3 = elem_nodes[8];
    double x4 = elem_nodes[9], y4 = elem_nodes[10], z4 = elem_nodes[11];

    double det = (x2 - x1) * ((y3 - y1) * (z4 - z1) - (y4 - y1) * (z3 - z1))
               - (x3 - x1) * ((y2 - y1) * (z4 - z1) - (y4 - y1) * (z2 - z1))
               + (x4 - x1) * ((y2 - y1) * (z3 - z1) - (y3 - y1) * (z2 - z1));

    double volume = fabs(det) / 6.0;
    double inv_det = 1.0 / det;

    // Shape function derivatives
    double dN[4][3];
    dN[0][0] = inv_det * ((y3 - y4) * (z2 - z4) - (y2 - y4) * (z3 - z4));
    dN[0][1] = inv_det * ((x2 - x4) * (z3 - z4) - (x3 - x4) * (z2 - z4));
    dN[0][2] = inv_det * ((x3 - x4) * (y2 - y4) - (x2 - x4) * (y3 - y4));
    dN[1][0] = inv_det * ((y4 - y3) * (z1 - z3) - (y1 - y3) * (z4 - z3));
    dN[1][1] = inv_det * ((x1 - x3) * (z4 - z3) - (x4 - x3) * (z1 - z3));
    dN[1][2] = inv_det * ((x4 - x3) * (y1 - y3) - (x1 - x3) * (y4 - y3));
    dN[2][0] = inv_det * ((y1 - y4) * (z3 - z1) - (y3 - y1) * (z1 - z4));
    dN[2][1] = inv_det * ((x3 - x1) * (z1 - z4) - (x1 - x4) * (z3 - z1));
    dN[2][2] = inv_det * ((x1 - x4) * (y3 - y1) - (x3 - x1) * (y1 - y4));
    dN[3][0] = -(dN[0][0] + dN[1][0] + dN[2][0]);
    dN[3][1] = -(dN[0][1] + dN[1][1] + dN[2][1]);
    dN[3][2] = -(dN[0][2] + dN[1][2] + dN[2][2]);

    // B matrix (6x12) and compute Ke = V * B^T * D * B
    double Ke[12][12];
    for (int i = 0; i < 12; i++)
        for (int j = 0; j < 12; j++)
            Ke[i][j] = 0.0;

    // Direct computation of B^T * D * B
    for (int a = 0; a < 4; a++) {
        for (int b = 0; b < 4; b++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    double val = lambda * dN[a][i] * dN[b][j]
                               + mu * dN[a][j] * dN[b][i];
                    if (i == j) {
                        val += mu * (dN[a][0] * dN[b][0] + dN[a][1] * dN[b][1] + dN[a][2] * dN[b][2]);
                    }
                    Ke[a * 3 + i][b * 3 + j] = val * volume;
                }
            }
        }
    }

    // Write to COO format
    int node_ids[4] = {n0, n1, n2, n3};
    int entries_per_element = 144;
    int base = elem_idx * entries_per_element;
    int idx = 0;

    for (int i = 0; i < 12; i++) {
        int row_node = i / 3;
        int row_dof = i % 3;
        int global_row = node_ids[row_node] * 3 + row_dof;
        for (int j = 0; j < 12; j++) {
            int col_node = j / 3;
            int col_dof = j % 3;
            int global_col = node_ids[col_node] * 3 + col_dof;
            coo_rows[base + idx] = global_row;
            coo_cols[base + idx] = global_col;
            coo_vals[base + idx] = Ke[i][j];
            idx++;
        }
    }
}

__kernel void cl_solve_jacobi(
    __global const double* A_vals,
    __global const int* A_row_ptr,
    __global const int* A_col_ind,
    __global const double* b,
    __global double* x,
    __global double* x_new,
    int n)
{
    int row = get_global_id(0);
    if (row >= n) return;

    double sigma = 0.0;
    double diag = 1.0;
    int row_start = A_row_ptr[row];
    int row_end = A_row_ptr[row + 1];

    for (int j = row_start; j < row_end; j++) {
        int col = A_col_ind[j];
        if (col == row) {
            diag = A_vals[j];
        } else {
            sigma += A_vals[j] * x[col];
        }
    }

    x_new[row] = (b[row] - sigma) / diag;
}

__kernel void cl_compute_jacobian(
    __global const double* params,
    int n_params,
    __global const double* data_points,
    int n_points,
    int n_prony_terms,
    __global double* jacobian,
    double epsilon)
{
    int idx = get_global_id(0);
    int total_work = n_points * n_params;
    if (idx >= total_work) return;

    int point_idx = idx / n_params;
    int param_idx = idx % n_params;

    double t = data_points[point_idx];

    // Compute f(params)
    double G_inf = params[0];
    double f0 = G_inf;
    for (int i = 0; i < n_prony_terms; i++) {
        double G_i = params[1 + 2 * i];
        double tau_i = params[2 + 2 * i];
        f0 += G_i * exp(-t / tau_i);
    }

    // Compute f(params + eps*e_j)
    double h = epsilon * fmax(fabs(params[param_idx]), 1.0);
    double perturbed_val = params[param_idx] + h;

    double f1 = (param_idx == 0) ? perturbed_val : params[0];
    for (int i = 0; i < n_prony_terms; i++) {
        int g_idx = 1 + 2 * i;
        int tau_idx = 2 + 2 * i;
        double G_i = (param_idx == g_idx) ? perturbed_val : params[g_idx];
        double tau_i = (param_idx == tau_idx) ? perturbed_val : params[tau_idx];
        f1 += G_i * exp(-t / tau_i);
    }

    jacobian[point_idx * n_params + param_idx] = (f1 - f0) / h;
}
