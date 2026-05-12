FROM ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y \
    cmake g++ make git libeigen3-dev \
    libprotobuf-dev protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

RUN mkdir build && cd build && \
    cmake .. -DCMAKE_BUILD_TYPE=Release -DUSE_OPENMP=ON && \
    make -j$(nproc)

FROM ubuntu:22.04
RUN apt-get update && apt-get install -y libgomp1 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN useradd -m rheosim
COPY --from=build /app/build/rheosim_compute_standalone .
COPY --from=build /app/build/rheosim_tests ./tests
USER rheosim

EXPOSE 50051
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD echo "OK" || exit 1

ENTRYPOINT ["./rheosim_compute_standalone"]
