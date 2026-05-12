# RheoSim Enterprise

Scientific platform for viscoelastic material simulation — parameter identification, constitutive law modeling, and automated report generation.

## Architecture

```
rheosim-projet/
├── rheosim-backend/          # Spring Boot 3.4.5, Java 21 (hexagonal architecture)
│   ├── rheosim-domain/       # Entities, value objects, domain events, ports
│   ├── rheosim-application/  # Use cases, DTOs, orchestration
│   ├── rheosim-infrastructure/ # JPA, Kafka, REST controllers, compute engine
│   └── rheosim-bootstrap/    # Application entry point, configuration
├── rheosim-frontend/         # Angular 21 standalone (signals, lazy routes)
├── docker/                   # Docker Compose (dev & prod)
└── .github/workflows/        # CI/CD pipeline
```

**Bounded Contexts:** Identity, Project, Experiment, Simulation, Reporting

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.5, Java 21 |
| Frontend | Angular 21, Chart.js |
| Database | PostgreSQL 15 + Flyway |
| Messaging | Apache Kafka (KRaft mode) |
| Security | Spring Security + JWT (jjwt) + RBAC |
| Compute | Apache Commons Math 3 (Levenberg-Marquardt) |
| Reports | Apache PDFBox 3, Commons CSV |
| API Docs | OpenAPI 3 / Swagger UI |
| Tests | JUnit 5, Mockito, ArchUnit, JaCoCo |
| DevOps | Docker, GitHub Actions |

## Quick Start

### Prerequisites

- Java 21+ (runtime 25 supported)
- Node.js 22+
- Docker & Docker Compose

### 1. Start infrastructure

```bash
cd docker
docker compose up -d
```

Services: PostgreSQL (:5432), Kafka (:9092), Redis (:6379), Kafka UI (:8090)

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
| POST | /api/auth/register | Register user |
| POST | /api/auth/login | Authenticate |
| POST | /api/auth/refresh | Refresh token |
| GET/POST | /api/v1/projects | Project CRUD |
| GET/POST | /api/v1/materials | Material management |
| POST | /api/v1/datasets | Upload experiment data (multipart) |
| POST | /api/v1/simulations | Submit simulation job |
| GET | /api/v1/simulations/{id}/result | Get identified parameters |
| POST | /api/v1/reports | Generate report (PDF/CSV/JSON) |

## Compute Engine

Constitutive law implementations (V1 — 1D):

- **Maxwell:** G(t) = G * exp(-t/tau)
- **Kelvin-Voigt:** J(t) = (1/G) * (1 - exp(-t/tau))
- **Prony Series (N-branch):** G(t) = G_inf + sum(G_i * exp(-t/tau_i))

Parameter identification via Levenberg-Marquardt optimizer with numerical Jacobian (central differences) and bounded constraints.

## Testing

```bash
cd rheosim-backend
mvn verify
```

88 tests across all layers:
- Domain tests (entity invariants, lifecycle)
- Application tests (use case logic)
- Infrastructure tests (parsers, validators, scientific convergence)
- ArchUnit tests (hexagonal architecture rules)

Coverage report: `target/site/jacoco/index.html`

## CI/CD

GitHub Actions pipeline on push to `main`/`develop`:
1. Backend build & test (with PostgreSQL service container)
2. Frontend build
3. Docker image build (on main only)

## Project Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Architecture | Modular monolith (hexagonal), microservices-ready for V2 |
| 2 | Compute V1 | Pure Java (1D/2D sufficient, no C++ interop complexity) |
| 3 | Compute V2 | C++ via gRPC microservice (3D FEM) |
| 4 | Messaging | Kafka KRaft (no Zookeeper) |
| 5 | Lombok | Removed — incompatible with Java 25 runtime |

## License

Proprietary - All rights reserved.
