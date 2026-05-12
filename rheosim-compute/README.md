# RheoSim Compute Engine (C++)

High-performance compute engine for viscoelastic simulation — parameter identification (Levenberg-Marquardt) and FEM 3D (linear tetrahedra).

## Build

```bash
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
```

## Run

```bash
./rheosim_compute_standalone   # Demo mode
./rheosim_tests                # Unit tests
```

## Dependencies

- Eigen 3.4+ (auto-fetched via CMake if not found)
- GoogleTest 1.14 (auto-fetched)
- spdlog 1.12 (auto-fetched)
- gRPC + Protobuf (optional, for server mode)
- OpenMP (optional, for parallelism)

## Architecture

```
include/rheosim/
├── constitutive_law.h       # Abstract interface
├── maxwell_law.h            # G(t) = G·exp(-t/τ)
├── kelvin_voigt_law.h       # J(t) = (1/G)·(1-exp(-t/τ))
├── prony_series_law.h       # G(t) = G∞ + ΣGᵢ·exp(-t/τᵢ)
├── levenberg_marquardt.h    # LM optimizer with bounded params
├── mesh.h                   # Mesh data structures
├── tetrahedron_p1.h         # P1 element (shape functions, B, D, Ke)
└── fem_solver.h             # Global assembly + solve
```

## Tests

28 unit tests covering:
- Maxwell law (7 tests): relaxation, storage/loss moduli, creep
- Kelvin-Voigt (6 tests): creep compliance, moduli
- Prony series (8 tests): limits, monotonicity, reduction to Maxwell
- Levenberg-Marquardt (4 tests): convergence on synthetic data
- FEM (5 tests): element volume, symmetry, beam deflection
