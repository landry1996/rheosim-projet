# RheoSim Enterprise

Scientific platform for viscoelastic material simulation — parameter identification, constitutive law modeling, ML-powered auto-calibration, real-time collaboration, and automated report generation.

## Architecture

```
rheosim-projet/
├── rheosim-backend/          # Spring Boot 3.4.5, Java 21 (hexagonal architecture)
│   ├── rheosim-domain/       # Entities, value objects, domain events, ports
│   ├── rheosim-application/  # Use cases, DTOs, orchestration
│   ├── rheosim-infrastructure/ # JPA, Kafka, REST, gRPC, WebSocket, billing
│   └── rheosim-bootstrap/    # Application entry point, configuration
├── rheosim-frontend/         # Angular 21 standalone (signals, lazy routes, PWA)
├── rheosim-compute/          # C++ FEM engine (Eigen, OpenMP, gRPC)
├── rheosim-ml/               # Python ML service (FastAPI, gRPC, ONNX)
├── docker/                   # Docker Compose (dev)
├── deploy/edge/              # Helm chart for K3s edge/air-gapped
├── tests/load/               # k6 load testing (500 VUs)
└── .github/workflows/        # CI/CD pipeline
```

**Bounded Contexts:** Identity, Project, Experiment, Simulation, Reporting, ML, Marketplace, Billing, Collaboration

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.5, Java 21 |
| Frontend | Angular 21, Chart.js, Three.js, PWA |
| ML Service | Python 3.11, FastAPI, gRPC, XGBoost, PyTorch, ONNX Runtime |
| Compute Engine | C++17, Eigen 3.4, OpenMP, gRPC (FEM 3D + thermo-mechanical) |
| Database | PostgreSQL 15 + Flyway |
| Messaging | Apache Kafka (KRaft mode) |
| Cache | Redis 7 (auth required) |
| Security | Spring Security + JWT + RBAC + Rate Limiting + CSP |
| Billing | Stripe (SaaS tiers: FREE, PRO, ENTERPRISE) |
| Collaboration | WebSocket CRDT + awareness protocol |
| Reports | Apache PDFBox 3, Commons CSV |
| API Docs | OpenAPI 3 / Swagger UI |
| Tests | JUnit 5, Mockito, ArchUnit, JaCoCo, k6 |
| DevOps | Docker, Helm, K3s, GitHub Actions |

## Quick Start

### Prerequisites

- Java 21+ (runtime 25 supported)
- Node.js 22+
- Docker & Docker Compose

### 1. Start infrastructure

```bash
cd docker
cp .env.example .env
# Edit .env with strong passwords
docker compose up -d
```

Services: PostgreSQL (:5432), Kafka (:9092), Redis (:6379), ML (:8000/:50052), Kafka UI (:8090)

### 2. Run backend

```bash
cd rheosim-backend
mvn spring-boot:run -pl rheosim-bootstrap
```

API available at http://localhost:8080/api  
Swagger UI at http://localhost:8080/api/swagger-ui.html

### 3. Run frontend

```bash
cd rheosim-frontend
npm install
npm start
```

Application at http://localhost:4200 (proxies API calls to :8080)

## Docker Deployment

```bash
cd docker
docker compose -f docker-compose.prod.yml up --build -d
```

Frontend: http://localhost  
Backend API: http://localhost:8080/api

## API Documentation

Interactive Swagger UI available at `/api/swagger-ui.html` when the backend is running.

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/auth/register | Register user |
| POST | /api/v1/auth/login | Authenticate |
| POST | /api/v1/auth/refresh | Refresh token |
| GET/POST | /api/v1/projects | Project CRUD |
| GET/POST | /api/v1/materials | Material management |
| POST | /api/v1/datasets | Upload experiment data (multipart) |
| POST | /api/v1/simulations | Submit simulation job |
| GET | /api/v1/simulations/{id}/result | Get identified parameters |
| POST | /api/v1/ml/predict | ML auto-calibration |
| GET | /api/v1/marketplace/plugins | Browse plugin marketplace |
| GET | /api/v1/billing/subscription | Get subscription status |
| POST | /api/v1/billing/checkout | Stripe checkout |
| POST | /api/v1/reports | Generate report (PDF/CSV/JSON) |
| WS | /ws/collaboration/{docId} | Real-time CRDT collaboration |

## Compute Engine

Constitutive law implementations:

- **Maxwell:** G(t) = G * exp(-t/tau)
- **Kelvin-Voigt:** J(t) = (1/G) * (1 - exp(-t/tau))
- **Prony Series (N-branch):** G(t) = G_inf + sum(G_i * exp(-t/tau_i))

Parameter identification via Levenberg-Marquardt optimizer with numerical Jacobian (central differences) and bounded constraints.

**V3 additions:**
- FEM 3D thermal solver (implicit Euler, lumped mass matrix)
- Thermo-mechanical coupling (staggered scheme, viscous dissipation)
- Time-Temperature Superposition: WLF and Arrhenius models
- ML auto-calibration: XGBoost classifier + PyTorch parameter estimator (ONNX export)

## Testing

```bash
# Backend
cd rheosim-backend
mvn verify

# ML Service
cd rheosim-ml
pytest

# Load tests (requires running backend)
k6 run -e LOAD_TEST_EMAIL=test@example.com -e LOAD_TEST_PASSWORD=secret tests/load/k6-load-test.js
```

88+ tests across all layers:
- Domain tests (entity invariants, lifecycle)
- Application tests (use case logic)
- Infrastructure tests (parsers, validators, scientific convergence)
- ArchUnit tests (hexagonal architecture rules)
- ML tests (feature extraction, data generation, API endpoints)
- Load tests (k6, 500 VUs peak)

Coverage report: `target/site/jacoco/index.html`

## CI/CD

GitHub Actions pipeline on push to `main`/`develop`:
1. Backend build & test (with PostgreSQL service container)
2. Frontend build
3. Docker image build (on main only)

## Edge Deployment (Air-Gapped)

```bash
cd deploy/edge
# Create Kubernetes secrets first
kubectl create secret generic rheosim-postgresql-secret --from-literal=password=...
kubectl create secret generic rheosim-redis-secret --from-literal=redis-password=...
# Install
./install-airgapped.sh
```

## Security

Multi-layered security hardening:
- JWT auth with sessionStorage (XSS-resistant) + strict rate limiting (10 req/min on auth)
- CSP headers (frame-ancestors none, object-src none, upgrade-insecure-requests)
- Input sanitization filter (XSS/injection protection on query params)
- WebSocket authentication with per-document session limits
- Zero-trust Kubernetes network policies (default-deny)
- TLS-enforced ingress, Redis auth, secrets via Kubernetes Secrets
- Stripe webhook signature verification (HMAC-SHA256)

See `documentation/documentation-technique.md` section 4 for full details.

## Project Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Architecture | Modular monolith (hexagonal), microservices-ready for V2 |
| 2 | Compute V1 | Pure Java (1D/2D sufficient, no C++ interop complexity) |
| 3 | Compute V2 | C++ via gRPC microservice (3D FEM) |
| 4 | Compute V3 | Thermo-mechanical coupling (staggered, WLF/Arrhenius TTS) |
| 5 | ML Service | Python (FastAPI + gRPC), XGBoost + PyTorch, ONNX export |
| 6 | Messaging | Kafka KRaft (no Zookeeper) |
| 7 | Collaboration | WebSocket binary (CRDT) + JSON (awareness) |
| 8 | Billing | Stripe SaaS (FREE/PRO/ENTERPRISE tiers) |
| 9 | Edge | K3s + Helm, air-gapped support |
| 10 | Lombok | Removed — incompatible with Java 25 runtime |

## License

Proprietary - All rights reserved.
