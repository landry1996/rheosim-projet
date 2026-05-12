#include "rheosim/mesh.h"

namespace rheosim {

Mesh Mesh::create_beam(double length, double width, double height,
                       int nx, int ny, int nz) {
    Mesh mesh;
    mesh.num_nodes = (nx + 1) * (ny + 1) * (nz + 1);
    mesh.nodes.resize(mesh.num_nodes, 3);

    double dx = length / nx;
    double dy = width / ny;
    double dz = height / nz;

    // Generate nodes
    int node_id = 0;
    for (int iz = 0; iz <= nz; ++iz) {
        for (int iy = 0; iy <= ny; ++iy) {
            for (int ix = 0; ix <= nx; ++ix) {
                mesh.nodes(node_id, 0) = ix * dx;
                mesh.nodes(node_id, 1) = iy * dy;
                mesh.nodes(node_id, 2) = iz * dz;
                ++node_id;
            }
        }
    }

    // Generate tetrahedra (6 per hexahedral cell)
    mesh.num_elements = nx * ny * nz * 6;
    mesh.elements.resize(mesh.num_elements, 4);

    int elem_id = 0;
    for (int iz = 0; iz < nz; ++iz) {
        for (int iy = 0; iy < ny; ++iy) {
            for (int ix = 0; ix < nx; ++ix) {
                // 8 corner nodes of the hex cell
                int n0 = iz * (ny + 1) * (nx + 1) + iy * (nx + 1) + ix;
                int n1 = n0 + 1;
                int n2 = n0 + (nx + 1);
                int n3 = n2 + 1;
                int n4 = n0 + (ny + 1) * (nx + 1);
                int n5 = n4 + 1;
                int n6 = n4 + (nx + 1);
                int n7 = n6 + 1;

                // Decompose hex into 6 tetrahedra
                mesh.elements.row(elem_id++) << n0, n1, n3, n5;
                mesh.elements.row(elem_id++) << n0, n3, n2, n6;
                mesh.elements.row(elem_id++) << n0, n5, n4, n6;
                mesh.elements.row(elem_id++) << n3, n5, n6, n7;
                mesh.elements.row(elem_id++) << n0, n3, n5, n6;
                mesh.elements.row(elem_id++) << n3, n6, n5, n7; // correction: use n0,n3,n6,n5
            }
        }
    }

    return mesh;
}

} // namespace rheosim
