#include "rheosim/gpu_backend.h"
#include "rheosim/fem_solver.h"
#include "rheosim/mesh.h"
#include <chrono>
#include <iostream>
#include <iomanip>
#include <random>
#include <vector>

using namespace rheosim;
using namespace std::chrono;

struct BenchmarkResult {
    std::string name;
    int num_elements;
    double cpu_time_ms;
    double gpu_time_ms;
    double speedup;
};

std::vector<double> generate_cube_mesh_nodes(int n_per_axis) {
    std::vector<double> nodes;
    double step = 1.0 / n_per_axis;
    for (int i = 0; i <= n_per_axis; i++) {
        for (int j = 0; j <= n_per_axis; j++) {
            for (int k = 0; k <= n_per_axis; k++) {
                nodes.push_back(i * step);
                nodes.push_back(j * step);
                nodes.push_back(k * step);
            }
        }
    }
    return nodes;
}

std::vector<int> generate_tet_elements(int n_per_axis) {
    std::vector<int> elements;
    int n1 = n_per_axis + 1;
    for (int i = 0; i < n_per_axis; i++) {
        for (int j = 0; j < n_per_axis; j++) {
            for (int k = 0; k < n_per_axis; k++) {
                int base = i * n1 * n1 + j * n1 + k;
                int v0 = base;
                int v1 = base + 1;
                int v2 = base + n1;
                int v3 = base + n1 * n1;
                int v4 = base + n1 + 1;
                int v5 = base + n1 * n1 + 1;
                int v6 = base + n1 * n1 + n1;
                int v7 = base + n1 * n1 + n1 + 1;

                // 5 tetrahedra per cube
                elements.insert(elements.end(), {v0, v1, v2, v3});
                elements.insert(elements.end(), {v1, v2, v3, v5});
                elements.insert(elements.end(), {v2, v3, v5, v6});
                elements.insert(elements.end(), {v1, v2, v4, v5});
                elements.insert(elements.end(), {v2, v5, v6, v7});
            }
        }
    }
    return elements;
}

BenchmarkResult run_assembly_benchmark(int target_elements) {
    // Compute n_per_axis to get approximately target_elements (5 tets per cube)
    int n = 1;
    while (n * n * n * 5 < target_elements) n++;

    auto nodes = generate_cube_mesh_nodes(n);
    auto elements = generate_tet_elements(n);
    int actual_elements = elements.size() / 4;

    std::vector<double> material_props(actual_elements * 2);
    for (int i = 0; i < actual_elements; i++) {
        material_props[i * 2] = 200e9;     // E = 200 GPa (steel)
        material_props[i * 2 + 1] = 0.3;   // nu = 0.3
    }

    BenchmarkResult result;
    result.name = "Assembly";
    result.num_elements = actual_elements;

    // CPU benchmark (placeholder — uses existing OpenMP assembly)
    auto start_cpu = high_resolution_clock::now();
    Eigen::SparseMatrix<double> K_cpu;
    // Simulate CPU assembly time
    auto end_cpu = high_resolution_clock::now();
    result.cpu_time_ms = duration_cast<microseconds>(end_cpu - start_cpu).count() / 1000.0;

    // GPU benchmark
    auto& dm = gpu::DeviceManager::instance();
    auto backend = dm.select_device(actual_elements, gpu::UserTier::ENTERPRISE);

    if (backend) {
        auto start_gpu = high_resolution_clock::now();
        Eigen::SparseMatrix<double> K_gpu;
        backend->assemble_stiffness(nodes, elements, material_props, K_gpu);
        auto end_gpu = high_resolution_clock::now();
        result.gpu_time_ms = duration_cast<microseconds>(end_gpu - start_gpu).count() / 1000.0;
    } else {
        result.gpu_time_ms = -1.0;
    }

    result.speedup = (result.gpu_time_ms > 0) ? result.cpu_time_ms / result.gpu_time_ms : 0.0;
    return result;
}

void print_results(const std::vector<BenchmarkResult>& results) {
    std::cout << "\n=== RheoSim GPU Benchmark Results ===" << std::endl;
    std::cout << std::setw(15) << "Test"
              << std::setw(12) << "Elements"
              << std::setw(15) << "CPU (ms)"
              << std::setw(15) << "GPU (ms)"
              << std::setw(12) << "Speedup"
              << std::endl;
    std::cout << std::string(69, '-') << std::endl;

    for (const auto& r : results) {
        std::cout << std::setw(15) << r.name
                  << std::setw(12) << r.num_elements
                  << std::setw(15) << std::fixed << std::setprecision(2) << r.cpu_time_ms;
        if (r.gpu_time_ms > 0) {
            std::cout << std::setw(15) << r.gpu_time_ms
                      << std::setw(10) << r.speedup << "x";
        } else {
            std::cout << std::setw(15) << "N/A (no GPU)"
                      << std::setw(12) << "-";
        }
        std::cout << std::endl;
    }

    // Print device info
    auto devices = gpu::DeviceManager::instance().list_devices();
    std::cout << "\n=== Detected Devices ===" << std::endl;
    for (const auto& dev : devices) {
        std::string type_str;
        switch (dev.type) {
            case gpu::DeviceType::CUDA: type_str = "CUDA"; break;
            case gpu::DeviceType::OPENCL: type_str = "OpenCL"; break;
            case gpu::DeviceType::CPU: type_str = "CPU"; break;
        }
        std::cout << "  [" << type_str << "] " << dev.name;
        if (dev.memory_bytes > 0) {
            std::cout << " (" << dev.memory_bytes / (1024 * 1024) << " MB)";
        }
        std::cout << (dev.available ? " [OK]" : " [UNAVAILABLE]") << std::endl;
    }
}

int main(int argc, char* argv[]) {
    std::cout << "RheoSim GPU Acceleration Benchmark" << std::endl;
    std::cout << "===================================" << std::endl;

    std::vector<int> element_counts = {100000, 500000, 1000000};

    if (argc > 1) {
        element_counts.clear();
        for (int i = 1; i < argc; i++) {
            element_counts.push_back(std::atoi(argv[i]));
        }
    }

    std::vector<BenchmarkResult> results;
    for (int count : element_counts) {
        std::cout << "Benchmarking " << count << " elements..." << std::flush;
        results.push_back(run_assembly_benchmark(count));
        std::cout << " done." << std::endl;
    }

    print_results(results);
    return 0;
}
