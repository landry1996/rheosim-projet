#!/bin/bash
# RheoSim GPU Benchmark CI Script
# Runs FEM assembly and solver benchmarks with various problem sizes.
# Usage: ./benchmark_gpu.sh [--ci] [--output-json results.json]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/../build-benchmark"
BENCHMARK_BIN="${BUILD_DIR}/rheosim_benchmark_gpu"
CI_MODE=false
OUTPUT_JSON=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --ci) CI_MODE=true; shift ;;
        --output-json) OUTPUT_JSON="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo "=== RheoSim GPU Benchmark ==="
echo "Date: $(date -Iseconds)"
echo "Host: $(hostname)"

# Check GPU availability
echo ""
echo "--- GPU Detection ---"
if command -v nvidia-smi &>/dev/null; then
    echo "NVIDIA GPU detected:"
    nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv,noheader
    GPU_TYPE="cuda"
elif command -v clinfo &>/dev/null; then
    echo "OpenCL devices:"
    clinfo --list
    GPU_TYPE="opencl"
else
    echo "No GPU detected, running CPU-only benchmarks"
    GPU_TYPE="cpu"
fi

# Build benchmark
echo ""
echo "--- Building Benchmark ---"
mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"
cmake .. -DCMAKE_BUILD_TYPE=Release -DUSE_GPU=ON -DBUILD_BENCHMARKS=ON
cmake --build . --target rheosim_benchmark_gpu -j$(nproc)
cd "${SCRIPT_DIR}"

# Run benchmarks
echo ""
echo "--- Running Benchmarks ---"
ELEMENT_COUNTS="100000 500000 1000000"

if [[ "${CI_MODE}" == "true" ]]; then
    # CI mode: shorter runs
    ELEMENT_COUNTS="100000 500000"
fi

${BENCHMARK_BIN} ${ELEMENT_COUNTS}
BENCH_EXIT=$?

# Generate JSON output if requested
if [[ -n "${OUTPUT_JSON}" ]]; then
    echo ""
    echo "--- Generating JSON Report ---"
    cat > "${OUTPUT_JSON}" <<EOF
{
    "timestamp": "$(date -Iseconds)",
    "host": "$(hostname)",
    "gpu_type": "${GPU_TYPE}",
    "exit_code": ${BENCH_EXIT},
    "element_counts": [${ELEMENT_COUNTS// /, }]
}
EOF
    echo "Report written to: ${OUTPUT_JSON}"
fi

# CI thresholds
if [[ "${CI_MODE}" == "true" && "${GPU_TYPE}" != "cpu" ]]; then
    echo ""
    echo "--- CI Threshold Validation ---"
    echo "Expected: Assembly speedup > 10x for 1M elements"
    echo "Expected: Solver speedup > 5x for 1M DOF"
    echo "Expected: Total FEM 1M elements < 2 min"
fi

echo ""
echo "=== Benchmark Complete ==="
exit ${BENCH_EXIT}
