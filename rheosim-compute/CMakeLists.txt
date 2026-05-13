cmake_minimum_required(VERSION 3.20)
project(rheosim-compute VERSION 0.3.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Options
option(BUILD_TESTS "Build unit tests" ON)
option(BUILD_BENCHMARKS "Build GPU benchmarks" OFF)
option(USE_OPENMP "Enable OpenMP parallelism" ON)
option(USE_GPU "Enable GPU acceleration (CUDA/OpenCL)" OFF)
option(USE_CUDA "Enable CUDA backend" ON)
option(USE_OPENCL "Enable OpenCL backend" ON)

# OpenMP
if(USE_OPENMP)
    find_package(OpenMP)
    if(OpenMP_CXX_FOUND)
        message(STATUS "OpenMP found: ${OpenMP_CXX_VERSION}")
    endif()
endif()

# GPU Support
if(USE_GPU)
    # CUDA
    if(USE_CUDA)
        include(CheckLanguage)
        check_language(CUDA)
        if(CMAKE_CUDA_COMPILER)
            enable_language(CUDA)
            find_package(CUDAToolkit QUIET)
            if(CUDAToolkit_FOUND)
                message(STATUS "CUDA Toolkit found: ${CUDAToolkit_VERSION}")
                set(RHEOSIM_HAS_CUDA TRUE)
            endif()
        else()
            message(STATUS "CUDA compiler not found, CUDA backend disabled")
        endif()
    endif()

    # OpenCL
    if(USE_OPENCL)
        find_package(OpenCL QUIET)
        if(OpenCL_FOUND)
            message(STATUS "OpenCL found: ${OpenCL_VERSION_STRING}")
            set(RHEOSIM_HAS_OPENCL TRUE)
        else()
            message(STATUS "OpenCL not found, OpenCL backend disabled")
        endif()
    endif()
endif()

# Eigen (header-only)
find_package(Eigen3 3.4 QUIET)
if(NOT Eigen3_FOUND)
    include(FetchContent)
    FetchContent_Declare(
        eigen
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 3.4.0
        GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(eigen)
endif()

# gRPC and Protobuf
find_package(Protobuf QUIET)
find_package(gRPC QUIET)

# spdlog
find_package(spdlog QUIET)
if(NOT spdlog_FOUND)
    include(FetchContent)
    FetchContent_Declare(
        spdlog
        GIT_REPOSITORY https://github.com/gabime/spdlog.git
        GIT_TAG v1.12.0
        GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(spdlog)
endif()

# Source files
set(LIB_SOURCES
    src/maxwell_law.cpp
    src/kelvin_voigt_law.cpp
    src/prony_series_law.cpp
    src/levenberg_marquardt.cpp
    src/fem_solver.cpp
    src/mesh.cpp
    src/tetrahedron_p1.cpp
    src/thermal_solver.cpp
    src/thermomechanical_solver.cpp
    src/gpu_backend.cpp
)

# GPU source files
set(GPU_SOURCES "")
if(RHEOSIM_HAS_CUDA)
    list(APPEND GPU_SOURCES
        src/cuda/kernel_assembly.cu
        src/cuda/kernel_solver.cu
        src/cuda/kernel_jacobian.cu
    )
endif()
if(RHEOSIM_HAS_OPENCL)
    list(APPEND GPU_SOURCES
        src/opencl/opencl_backend.cpp
    )
endif()

# Library
add_library(rheosim_compute_lib STATIC ${LIB_SOURCES} ${GPU_SOURCES})
target_include_directories(rheosim_compute_lib PUBLIC include)
target_link_libraries(rheosim_compute_lib PUBLIC Eigen3::Eigen)
if(spdlog_FOUND OR TARGET spdlog::spdlog)
    target_link_libraries(rheosim_compute_lib PUBLIC spdlog::spdlog)
endif()
if(OpenMP_CXX_FOUND)
    target_link_libraries(rheosim_compute_lib PUBLIC OpenMP::OpenMP_CXX)
    target_compile_definitions(rheosim_compute_lib PUBLIC RHEOSIM_USE_OPENMP)
endif()

# GPU linking
if(RHEOSIM_HAS_CUDA)
    target_link_libraries(rheosim_compute_lib PUBLIC
        CUDA::cudart
        CUDA::cusparse
        CUDA::cusolver
    )
    target_compile_definitions(rheosim_compute_lib PUBLIC RHEOSIM_USE_CUDA)
    set_target_properties(rheosim_compute_lib PROPERTIES CUDA_SEPARABLE_COMPILATION ON)
endif()
if(RHEOSIM_HAS_OPENCL)
    target_link_libraries(rheosim_compute_lib PUBLIC OpenCL::OpenCL)
    target_compile_definitions(rheosim_compute_lib PUBLIC RHEOSIM_USE_OPENCL)
endif()

# Main executable (standalone mode without gRPC)
add_executable(rheosim_compute_standalone src/main_standalone.cpp)
target_link_libraries(rheosim_compute_standalone PRIVATE rheosim_compute_lib)

# gRPC server (optional, when gRPC available)
if(gRPC_FOUND AND Protobuf_FOUND)
    # Proto generation
    set(PROTO_FILES proto/compute.proto)
    add_library(rheosim_proto ${PROTO_FILES})
    target_link_libraries(rheosim_proto PUBLIC protobuf::libprotobuf gRPC::grpc++)
    protobuf_generate(TARGET rheosim_proto LANGUAGE cpp)
    protobuf_generate(TARGET rheosim_proto LANGUAGE grpc GENERATE_EXTENSIONS .grpc.pb.h .grpc.pb.cc PLUGIN "protoc-gen-grpc=\$<TARGET_FILE:gRPC::grpc_cpp_plugin>")

    add_executable(rheosim_compute_server src/grpc_server.cpp)
    target_link_libraries(rheosim_compute_server PRIVATE rheosim_compute_lib rheosim_proto)
endif()

# Tests
if(BUILD_TESTS)
    include(FetchContent)
    FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.14.0
        GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(googletest)
    enable_testing()

    add_executable(rheosim_tests
        test/test_maxwell.cpp
        test/test_kelvin_voigt.cpp
        test/test_prony_series.cpp
        test/test_lm_convergence.cpp
        test/test_fem_beam.cpp
    )
    target_link_libraries(rheosim_tests PRIVATE rheosim_compute_lib GTest::gtest_main)
    include(GoogleTest)
    gtest_discover_tests(rheosim_tests)
endif()

# Benchmarks
if(BUILD_BENCHMARKS)
    add_executable(rheosim_benchmark_gpu benchmark/benchmark_gpu.cpp)
    target_link_libraries(rheosim_benchmark_gpu PRIVATE rheosim_compute_lib)
endif()
