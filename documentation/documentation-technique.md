# RheoSim Enterprise — Documentation Technique

## 1. Vue d'Ensemble Architecturale

### 1.1 Architecture Globale V2

```
                    ┌─────────────────────────────────────────┐
                    │           Ingress (nginx + TLS)          │
                    │         Rate Limit, Auth, Routing        │
                    └──────────────┬───────────────────────────┘
                                   │
         ┌─────────────┬───────────┼───────────┬──────────────┐
         │             │           │           │              │
    ┌────▼────┐  ┌─────▼────┐ ┌───▼────┐ ┌───▼─────┐  ┌─────▼─────┐
    │Identity │  │ Project  │ │Experiment│ │Simulation│  │ Reporting │
    │Service  │  │ Service  │ │ Service │ │ Service  │  │  Service  │
    │(Java)   │  │(Java)    │ │(Java)   │ │(Java)    │  │ (Java)    │
    └────┬────┘  └────┬─────┘ └───┬────┘ └───┬─────┘  └─────┬─────┘
         │            │           │           │              │
         └────────────┴───────────┼───────────┴──────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │      Apache Kafka          │
                    │   (Event Bus inter-services)│
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │   Compute Engine (C++)     │
                    │   gRPC + FEM 3D + LM      │
                    │   OpenMP parallelise       │
                    └───────────────────────────┘
```

### 1.2 Architecture Hexagonale (Ports & Adapters)

Le backend suit une architecture hexagonale stricte avec separation en modules Maven :

```
rheosim-backend/
├── rheosim-domain/           # Coeur metier (entites, events, ports)
│   └── com.rheosim.domain
│       ├── identity/         # User, Role, RefreshToken
│       ├── project/          # Project, Material, MaterialModel
│       ├── experiment/       # Dataset, DataColumn, ValidationError
│       ├── simulation/       # SimulationJob, SimulationResult, ConstitutiveLaw
│       ├── reporting/        # Report, DublinCoreMetadata
│       └── collaboration/    # Organization, Member, Invitation, AuditEvent (V2)
│
├── rheosim-application/      # Cas d'usage, orchestration
│   └── com.rheosim.application
│       ├── identity/         # AuthenticationUseCase
│       ├── project/          # ProjectUseCase, MaterialUseCase
│       ├── experiment/       # DatasetUseCase
│       ├── simulation/       # SimulationUseCase
│       ├── reporting/        # ReportingUseCase
│       └── collaboration/    # OrganizationUseCase, ProjectCollaborationUseCase (V2)
│
├── rheosim-infrastructure/   # Adaptateurs (JPA, REST, Kafka, gRPC, WebSocket)
│   └── com.rheosim.infrastructure
│       ├── identity/         # Security config, JWT, controllers
│       ├── project/          # JPA adapters, controllers
│       ├── experiment/       # Parsers, validators, file storage
│       ├── simulation/       # Compute engine, LM optimizer
│       ├── reporting/        # PDF/CSV/JSON generators
│       ├── collaboration/    # Controller, bean config (V2)
│       ├── notification/     # WebSocket config, NotificationService (V2)
│       ├── compute/grpc/     # ComputeEngineClient, RoutingAdapter (V2)
│       └── config/
│           └── observability/ # OpenTelemetry, Prometheus metrics (V2)
│
└── rheosim-bootstrap/        # Point d'entree Spring Boot
    └── com.rheosim
        ├── RheoSimApplication.java
        └── config/OpenApiConfig.java
```

### 1.3 Regles Architecturales (Enforced by ArchUnit)

1. **Domain ne depend de rien** : Aucune dependance vers application, infrastructure ou frameworks
2. **Application depend uniquement de Domain** : Utilise les ports definis dans domain
3. **Infrastructure implemente les ports** : Adapters vers JPA, REST, Kafka, gRPC, filesystem
4. **Pas de cycle de dependances** : Verification ArchUnit automatique
5. **Les ports sont des interfaces dans domain** : Inversion de dependance

### 1.4 Communication Inter-Services

```
[Identity] ──event──→ Kafka ──→ [Audit]
[Experiment] ──event──→ Kafka ──→ [Notification] ──WebSocket──→ [Frontend]
[Simulation] ──event──→ Kafka ──→ [Notification] ──WebSocket──→ [Frontend]
[Simulation] ──gRPC──→ [C++ Compute Engine]
```

Events publies via `DomainEvent` abstrait, serialises en JSON (StringSerializer).
Communication synchrone via gRPC pour les appels compute engine.

---

## 2. Stack Technique Detaillee

### 2.1 Backend

| Composant | Version | Role |
|-----------|---------|------|
| Java | 21 (compile) / 25 (runtime) | Langage |
| Spring Boot | 3.4.5 | Framework applicatif |
| Spring Security | 6.x | Authentification/autorisation |
| Spring Data JPA | 3.x | Persistence |
| Spring WebSocket | 6.x | Notifications temps reel (V2) |
| Hibernate | 6.x | ORM |
| Flyway | 10.x | Migrations base de donnees |
| Apache Kafka | Client 3.7 | Messaging event-driven |
| jjwt | 0.12.6 | Generation/verification JWT (RS256) |
| MapStruct | 1.6.3 | Mapping compile-time |
| Apache Commons Math | 3.6.1 | Optimisation numerique (LM) |
| Apache Commons CSV | 1.12.0 | Parsing CSV |
| Apache POI | 5.3.0 | Parsing Excel |
| Apache PDFBox | 3.0.3 | Generation PDF |
| Bucket4j | 8.14.0 | Rate limiting |
| springdoc-openapi | 2.8.4 | Documentation API |
| Micrometer + OTel | 1.4.3 / 1.44.1 | Tracing distribue (V2) |
| micrometer-registry-prometheus | Latest | Export metriques Prometheus (V2) |

### 2.2 Compute Engine C++ (V2)

| Composant | Version | Role |
|-----------|---------|------|
| C++ | 17 | Langage |
| Eigen | 3.4 | Algebre lineaire, matrices creuses |
| gRPC | 1.60+ | Communication inter-services |
| Protobuf | 3.25+ | Serialisation |
| GoogleTest | 1.14 | Tests unitaires |
| spdlog | 1.12 | Logging |
| OpenMP | 5.0 | Parallelisme CPU |
| CMake | 3.20+ | Build system |

### 2.3 Frontend

| Composant | Version | Role |
|-----------|---------|------|
| Angular | 21.2 | Framework SPA |
| TypeScript | 5.9 | Langage |
| Chart.js | 4.x | Visualisation graphique 2D |
| Three.js | 0.170 | Visualisation 3D maillage/deformation (V2) |
| @stomp/stompjs | 7.x | Client WebSocket STOMP (V2) |
| SockJS | 1.6 | Fallback WebSocket (V2) |
| RxJS | 7.8 | Programmation reactive |

### 2.4 Infrastructure

| Composant | Version | Role |
|-----------|---------|------|
| PostgreSQL | 15 | Base de donnees relationnelle |
| Apache Kafka | 3.7 (KRaft) | Bus d'evenements |
| Redis | 7 | Cache sessions |
| MinIO | Latest | Object storage S3-compatible (V2) |
| Docker | Latest | Conteneurisation |
| Kubernetes | 1.28+ | Orchestration (V2) |
| Helm | 3.x | Package manager K8s (V2) |
| nginx | Alpine | Reverse proxy / Ingress |
| GitHub Actions | v4 | CI/CD |

### 2.5 Observabilite (V2)

| Composant | Version | Role |
|-----------|---------|------|
| Prometheus | Latest | Collecte metriques |
| Grafana | Latest | Dashboards visualisation |
| Loki | Latest | Aggregation logs |
| Jaeger | Latest | Traces distribuees |
| OpenTelemetry Collector | Latest | Pipeline telemetrie unifie |
| Alertmanager | Latest | Alertes et routing |

---

## 3. Modele de Donnees

### 3.1 Schema PostgreSQL

```sql
-- Schema: rheosim

-- ====== Identity ======
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (id UUID PRIMARY KEY, name VARCHAR(50) UNIQUE NOT NULL);
CREATE TABLE user_roles (user_id UUID, role_id UUID, PRIMARY KEY(user_id, role_id));
CREATE TABLE refresh_tokens (id UUID PRIMARY KEY, token VARCHAR(500) UNIQUE, user_id UUID, expires_at TIMESTAMP, revoked BOOLEAN DEFAULT FALSE);
CREATE TABLE audit_logs (id BIGSERIAL PRIMARY KEY, action VARCHAR(100), user_id UUID, details TEXT, ip_address VARCHAR(45), created_at TIMESTAMP DEFAULT NOW());

-- ====== Projects ======
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    owner_id UUID REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),  -- V2: multi-tenant
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE materials (id UUID PRIMARY KEY, name VARCHAR(200), grade VARCHAR(100), family VARCHAR(50), supplier VARCHAR(200), version INTEGER DEFAULT 1, model_type VARCHAR(50), model_data JSONB, project_id UUID, created_at TIMESTAMP DEFAULT NOW());

-- ====== Experiment ======
CREATE TABLE datasets (id UUID PRIMARY KEY, file_name VARCHAR(500), experiment_type VARCHAR(50), status VARCHAR(20) DEFAULT 'UPLOADED', project_id UUID, row_count INTEGER DEFAULT 0, columns_metadata JSONB, validation_errors JSONB, file_path VARCHAR(1000), content_type VARCHAR(100), created_at TIMESTAMP DEFAULT NOW());

-- ====== Simulation ======
CREATE TABLE simulation_jobs (id UUID PRIMARY KEY, project_id UUID, material_id UUID, dataset_id UUID, simulation_type VARCHAR(50), status VARCHAR(20) DEFAULT 'QUEUED', fit_target VARCHAR(50), initial_guess DOUBLE PRECISION[], result_data JSONB, submitted_at TIMESTAMP DEFAULT NOW(), started_at TIMESTAMP, completed_at TIMESTAMP, error_message TEXT);

-- ====== Reporting ======
CREATE TABLE reports (id UUID PRIMARY KEY, title VARCHAR(300), format VARCHAR(10), status VARCHAR(20) DEFAULT 'GENERATING', project_id UUID, generated_by UUID, file_path VARCHAR(1000), file_size_bytes BIGINT DEFAULT 0, metadata JSONB, created_at TIMESTAMP DEFAULT NOW());

-- ====== Collaboration (V2) ======
CREATE TABLE organizations (id UUID PRIMARY KEY, name VARCHAR(100), slug VARCHAR(100) UNIQUE, owner_user_id UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW());
CREATE TABLE organization_members (id UUID PRIMARY KEY, organization_id UUID, user_id UUID, role VARCHAR(20), joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), UNIQUE(organization_id, user_id));
CREATE TABLE invitations (id UUID PRIMARY KEY, organization_id UUID, email VARCHAR(255), role VARCHAR(20), status VARCHAR(20) DEFAULT 'PENDING', invited_by_user_id UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), expires_at TIMESTAMP WITH TIME ZONE);
CREATE TABLE project_collaborators (id UUID PRIMARY KEY, project_id UUID, user_id UUID, role VARCHAR(20), added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), UNIQUE(project_id, user_id));
CREATE TABLE audit_events (id UUID PRIMARY KEY, organization_id UUID, user_id UUID, action VARCHAR(50), resource_type VARCHAR(50), resource_id UUID, details TEXT, occurred_at TIMESTAMP WITH TIME ZONE DEFAULT NOW());
```

### 3.2 Migrations Flyway

| Version | Fichier | Contenu |
|---------|---------|---------|
| V1 | `V1__create_identity_tables.sql` | users, roles, user_roles, refresh_tokens, audit_logs |
| V2 | `V2__create_project_tables.sql` | projects, materials, project_materials |
| V3 | `V3__create_dataset_table.sql` | datasets |
| V4 | `V4__create_simulation_jobs_table.sql` | simulation_jobs |
| V5 | `V5__create_reports_table.sql` | reports |
| V6 | `V6__create_collaboration_tables.sql` | organizations, members, invitations, collaborators, audit_events |

---

## 4. Securite

### 4.1 Authentification JWT (RS256)

```
Client                    Backend
  │                          │
  │── POST /auth/login ─────→│ Verifie credentials
  │←── {accessToken,         │ Signe avec cle privee RS256
  │     refreshToken} ───────│
  │                          │
  │── GET /api/... ─────────→│ Verifie avec cle publique
  │   Authorization: Bearer  │ (distribuee a tous les services)
  │                          │
  │── POST /auth/refresh ───→│ Verifie refreshToken
  │←── {newAccessToken,      │ Rotation du refresh
  │     newRefreshToken} ────│
```

**Configuration tokens** :
- Access token : 15 minutes (RS256 asymetrique)
- Refresh token : 7 jours (UUID opaque, stocke en base)
- Cle privee : montee via Kubernetes Secret (`/etc/secrets/jwt/private.pem`)
- Cle publique : distribuee a tous les services pour validation locale

### 4.2 RBAC

| Role Systeme | Permissions |
|------|-------------|
| USER | CRUD sur ses propres projets + projets partages selon role projet |
| ADMIN | Tout USER + gestion utilisateurs + acces tous projets |

| Role Projet | Permissions |
|------|-------------|
| OWNER | CRUD complet, archiver, supprimer, partager |
| EDITOR | Modifier contenu (materiaux, datasets, simulations) |
| VIEWER | Lecture seule |

### 4.3 Protection

- **Rate Limiting** : Bucket4j, 60 req/min par IP
- **CORS** : Origines configurables (defaut localhost:4200)
- **Validation** : Bean Validation sur tous les DTOs entrants
- **SQL Injection** : Parameterized queries via JPA/Hibernate
- **XSS** : Angular sanitize par defaut
- **CSRF** : Non necessaire (API stateless JWT)
- **Network Policies** : Isolation inter-pods K8s (V2)
- **Sealed Secrets** : Chiffrement des secrets K8s (V2)
- **TLS** : cert-manager + Let's Encrypt sur Ingress (V2)

---

## 5. Moteur de Calcul Scientifique

### 5.1 Architecture Double Engine

```
ComputeEngineRoutingAdapter (@Primary, implements ParameterIdentificationPort)
├── ComputeEngineClient (gRPC → C++ engine)
│   └── isAvailable() → health check
│   └── identify() → gRPC call
└── LevenbergMarquardtIdentifier (Java fallback)
    └── Apache Commons Math 3

Si C++ disponible → delegation gRPC
Si C++ indisponible → fallback Java local
Si C++ echoue → fallback Java avec log warning
```

### 5.2 Compute Engine C++ (V2)

**Structure** :
```
rheosim-compute/
├── CMakeLists.txt              # FetchContent (Eigen, GTest, spdlog)
├── proto/compute.proto         # gRPC service definitions
├── include/rheosim/
│   ├── constitutive_law.h      # Interface abstraite
│   ├── maxwell_law.h
│   ├── kelvin_voigt_law.h
│   ├── prony_series_law.h
│   ├── levenberg_marquardt.h   # LM optimizer avec Options, Result
│   ├── mesh.h                  # Mesh struct + create_beam factory
│   ├── tetrahedron_p1.h        # Element P1 (Ke, B, D)
│   └── fem_solver.h            # FEMSolver + FEMResult
├── src/
│   ├── maxwell_law.cpp         # G(t), G'(w), G''(w), J(t)
│   ├── kelvin_voigt_law.cpp
│   ├── prony_series_law.cpp    # N-branch implementation
│   ├── levenberg_marquardt.cpp # Numerical Jacobian, bounded, R²
│   ├── tetrahedron_p1.cpp      # B matrix, D matrix, D_eff viscoelastic
│   ├── mesh.cpp                # Hex → 6 tets decomposition
│   ├── fem_solver.cpp          # OpenMP assembly, SparseLU/BiCGSTAB
│   ├── grpc_server.cpp         # gRPC service implementation
│   └── main_standalone.cpp     # Demo standalone
├── test/                       # 30+ tests (GoogleTest)
└── Dockerfile                  # Ubuntu 22.04 multi-stage
```

**Services gRPC** (definis dans `compute.proto`) :
- `Identify` : identification parametrique (unaire)
- `SimulateFEM` : simulation FEM 3D avec streaming progression
- `HealthCheck` : verification de disponibilite

**FEM 3D** :
- Maillage : tetraedres P1 (4 noeuds, interpolation lineaire)
- Assemblage : OpenMP parallelise (thread-local triplets + merge)
- Solveur : Eigen SparseLU (direct) ou BiCGSTAB (iteratif)
- Conditions limites : methode de penalite (Dirichlet)
- Viscoelasticite : D_eff(dt) = D_elastic * factor (Euler implicite)

### 5.3 Interface ConstitutiveLaw (Java + C++)

```java
public interface ConstitutiveLaw {
    double computeRelaxationModulus(double time, double[] params);
    double computeCreepCompliance(double time, double[] params);
    double computeStorageModulus(double omega, double[] params);
    double computeLossModulus(double omega, double[] params);
    double computeComplexViscosity(double omega, double[] params);
    int getParameterCount();
    String[] getParameterNames();
    double[] getDefaultLowerBounds();
    double[] getDefaultUpperBounds();
}
```

### 5.4 Algorithme d'Identification (Java + C++)

**Methode** : Levenberg-Marquardt (moindres carres non-lineaires)

1. Construction du probleme
2. Jacobien numerique par differences centrales (pas = 1e-8 * parametre)
3. Contraintes bornees (clipping post-iteration)
4. Criteres de convergence : tolerance relative 1e-12, max 1000 iterations
5. Calcul R² = 1 - SS_res / SS_tot

### 5.5 Job Processing

- Polling toutes les 2 secondes (`@Scheduled(fixedDelay = 2000)`)
- Maximum 4 jobs en parallele par pod
- Statut : QUEUED → RUNNING → COMPLETED/FAILED
- Timeout : 1 heure par job
- Progression broadcast via WebSocket (`/topic/simulations/{id}`)
- Resultats stockes en JSONB dans `simulation_jobs.result_data`

---

## 6. API REST

### 6.1 Convention

- Base path : `/api`
- Versionning : `/api/v1/`
- Content-Type : `application/json` (sauf upload multipart)
- Authentification : Header `Authorization: Bearer <token>`
- Erreurs : format uniforme `{error, message, timestamp, path}`

### 6.2 Endpoints

#### Authentification
```
POST /api/v1/auth/register     → AuthResponse
POST /api/v1/auth/login        → AuthResponse
POST /api/v1/auth/refresh      → AuthResponse
POST /api/v1/auth/logout       → 204 No Content
```

#### Projets
```
GET    /api/v1/projects         → Project[]
POST   /api/v1/projects         → Project
GET    /api/v1/projects/{id}    → Project
PUT    /api/v1/projects/{id}    → Project
DELETE /api/v1/projects/{id}    → 204
PATCH  /api/v1/projects/{id}/activate → Project
PATCH  /api/v1/projects/{id}/archive  → Project
```

#### Materiaux
```
GET    /api/v1/materials?projectId=   → Material[]
POST   /api/v1/materials              → Material
PUT    /api/v1/materials/{id}/model   → Material
DELETE /api/v1/materials/{id}         → 204
```

#### Datasets
```
POST   /api/v1/datasets               → Dataset (multipart)
POST   /api/v1/datasets/{id}/validate → Dataset
GET    /api/v1/datasets?projectId=    → Dataset[]
DELETE /api/v1/datasets/{id}          → 204
```

#### Simulations
```
POST   /api/v1/simulations            → SimulationJob
GET    /api/v1/simulations/{id}       → SimulationJob
GET    /api/v1/simulations/{id}/result → SimulationResult
GET    /api/v1/simulations/{id}/mesh-results → MeshData (3D)
POST   /api/v1/simulations/{id}/cancel → 204
GET    /api/v1/simulations?projectId=  → SimulationJob[]
```

#### Rapports
```
POST   /api/v1/reports                → Report
GET    /api/v1/reports?projectId=     → Report[]
GET    /api/v1/reports/{id}/download  → Binary (blob)
DELETE /api/v1/reports/{id}           → 204
```

#### Organisations (V2)
```
POST   /api/v1/organizations                          → Organization
GET    /api/v1/organizations                          → Organization[]
GET    /api/v1/organizations/{id}/members             → Member[]
POST   /api/v1/organizations/{id}/invitations         → Invitation
POST   /api/v1/organizations/invitations/{id}/accept  → 200
DELETE /api/v1/organizations/{id}/members/{userId}     → 204
POST   /api/v1/organizations/{id}/projects/{pid}/collaborators → Collaborator
GET    /api/v1/organizations/{id}/projects/{pid}/collaborators → Collaborator[]
```

#### WebSocket (V2)
```
CONNECT  /ws (SockJS)
SUBSCRIBE /user/{userId}/queue/notifications  → Notifications personnelles
SUBSCRIBE /topic/simulations/{id}             → Progression simulation
```

---

## 7. Frontend Angular

### 7.1 Architecture

```
src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── models/             # Interfaces TypeScript
│   ├── services/           # HttpClient services + WebSocketService (V2)
│   ├── guards/             # authGuard, guestGuard
│   └── interceptors/       # JWT auto-attach + refresh on 401
│
├── shared/                  # Composants reutilisables
│   └── components/         # Navbar, Sidebar
│
├── features/               # Lazy-loaded feature modules
│   ├── auth/              # Login, Register
│   ├── dashboard/         # Vue projets + stats
│   ├── datasets/          # Upload, List
│   ├── simulation/        # Calibration + Chart.js
│   ├── reports/           # Generation + download
│   ├── visualization/     # Three.js MeshViewer, ResultsViewer (V2)
│   └── collaboration/     # OrganizationComponent (V2)
│
└── app.ts                  # Root component (layout conditionnel)
```

### 7.2 Patterns Utilises

- **Signals** : Etat reactif (Angular 16+)
- **Standalone Components** : Pas de NgModules, imports directs
- **Lazy Loading** : `loadComponent()` dans les routes
- **Functional Guards** : `CanActivateFn`
- **Functional Interceptors** : `HttpInterceptorFn`
- **Input signals** : `input()` pour les composants enfants (V2)
- **Effect** : reactions aux changements de signaux (V2)

### 7.3 Visualisation 3D (V2)

```
MeshViewerComponent
├── Three.js Scene (WebGL)
├── OrbitControls (rotation/zoom camera)
├── Tetrahedron → 4 faces triangulaires
├── Vertex colors (HSL par displacement magnitude)
├── Wireframe toggle
├── Deformation scale slider
└── Color maps: displacement | Von Mises stress
```

---

## 8. DevOps & Deployment

### 8.1 Docker

**Backend** (multi-stage) :
1. Stage build : Eclipse Temurin JDK 21, Maven, compile + package
2. Stage runtime : Eclipse Temurin JRE 21 Alpine, utilisateur non-root

**Frontend** (multi-stage) :
1. Stage build : Node 22 Alpine, `ng build --production`
2. Stage runtime : nginx Alpine, SPA routing + reverse proxy

**Compute Engine C++** (multi-stage) :
1. Stage build : Ubuntu 22.04, CMake, build + link
2. Stage runtime : Ubuntu 22.04 minimal, OpenMP runtime

### 8.2 Kubernetes & Helm (V2)

```
deploy/helm/
├── identity-service/     # Chart + templates (deployment, service, HPA)
├── simulation-service/   # Chart + templates
├── compute-engine/       # Chart + templates (node selector, tolerations)
├── infrastructure/       # Bitnami deps (PostgreSQL, Kafka, Redis, MinIO)
└── rheosim/              # Umbrella chart
    ├── Chart.yaml        # Dependencies
    ├── values.yaml       # Defaults
    ├── values-dev.yaml   # 1 replica, pas d'autoscaling
    ├── values-staging.yaml # 2 replicas, autoscaling modere
    ├── values-prod.yaml  # Full autoscaling, node selectors
    └── templates/
        ├── namespace.yaml
        ├── ingress.yaml       # TLS + path routing
        ├── networkpolicy.yaml # Default deny + allow intra-namespace
        └── sealed-secrets.yaml # DB, JWT, Redis, MinIO secrets
```

**Autoscaling** :
| Service | Min | Max | CPU Target |
|---------|-----|-----|-----------|
| identity-service | 2 | 4 | 70% |
| simulation-service | 2 | 8 | 70% |
| compute-engine | 2 | 16 | 60% |

### 8.3 Monitoring & Observabilite (V2)

```
deploy/monitoring/
├── docker-compose.monitoring.yml  # Stack complete
├── prometheus.yml                 # Scrape configs (8 jobs)
├── alerts/rules.yaml              # 11 alerting rules
├── alertmanager.yml               # Routing critical/warning
├── grafana-dashboard.json         # 7 panels (latence, jobs, CPU, convergence)
├── loki-config.yaml               # Log aggregation config
└── otel-collector-config.yaml     # OTel pipeline (traces → Jaeger, logs → Loki)
```

**Metriques custom Prometheus** :
```
rheosim_simulation_duration_seconds{model_type, dimension}
rheosim_simulation_convergence_rate{model_type}
rheosim_dataset_upload_bytes_total
rheosim_active_jobs_count{status}
rheosim_compute_grpc_calls_total
rheosim_compute_fallback_total
```

**Alertes** :
- ServiceDown (1 min) → critical
- HighLatencyP95 (>2s, 5 min) → warning
- HighErrorRate (>5%, 5 min) → critical
- SimulationQueueBacklog (>50, 10 min) → warning
- SimulationFailureRate (>30%, 15 min) → warning
- ComputeEngineCPUSaturation → warning
- PostgreSQLConnectionPoolExhausted → warning
- KafkaConsumerLag (>10k) → warning
- DiskSpaceLow (<10%) → critical

### 8.4 CI/CD Pipeline

```
Push/PR → [backend-test] → [frontend-test] → [docker-build]
              │                   │                 │
         Java 21 + PG       Node 22 + build    Buildx multi-arch
         mvn verify          ng build           (main only)
```

### 8.5 Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | dev | Profil Spring |
| `SPRING_DATASOURCE_URL` | localhost:5432 | URL PostgreSQL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Brokers Kafka |
| `JWT_PRIVATE_KEY_PATH` | - | Chemin cle privee RS256 |
| `JWT_PUBLIC_KEY_PATH` | - | Chemin cle publique RS256 |
| `CORS_ORIGINS` | http://localhost:4200 | Origines CORS |
| `RHEOSIM_STORAGE_PATH` | ./data/uploads | Stockage fichiers |
| `RHEOSIM_COMPUTE_GRPC_HOST` | localhost | Host compute engine |
| `RHEOSIM_COMPUTE_GRPC_PORT` | 50051 | Port gRPC |
| `RHEOSIM_COMPUTE_GRPC_ENABLED` | false | Active le routing gRPC |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | http://localhost:4318 | Endpoint OTel |
| `OTEL_SAMPLING_PROBABILITY` | 1.0 | Taux d'echantillonnage traces |

---

## 9. Tests

### 9.1 Strategie de Tests

| Niveau | Outil | Cible | Couverture |
|--------|-------|-------|-----------|
| Unitaire | JUnit 5 + Mockito | Domain, Application | Invariants metier |
| Integration | Testcontainers | Infrastructure | Persistence, parsers |
| Architecture | ArchUnit | Tous modules | Regles hexagonales |
| Scientifique (Java) | JUnit 5 | Compute engine | Solutions analytiques |
| Scientifique (C++) | GoogleTest | Compute engine C++ | 30+ tests FEM/LM |
| Load | k6 | API endpoints | 100+ users simultanes |

### 9.2 Tests Backend (88+ tests Java)

- **ProjectTest** (9) : builder, validation, lifecycle, materiaux
- **MaterialTest** (6) : builder, versioning modele, updateDetails
- **ReportTest** (4) : transitions lifecycle
- **ProjectUseCaseTest** (7) : CRUD, ownership, doublons
- **SimulationUseCaseTest** (4) : submit, max-concurrent, cancel, result guard
- **CsvDataParserAdapterTest** (6) : parsing, inference colonnes, NaN, erreurs
- **RheologyDataValidatorAdapterTest** (6) : colonnes requises, qualite donnees
- **ParameterIdentificationTest** (3) : convergence Levenberg-Marquardt
- **ConstitutiveLawTest** (11) : validation analytique Maxwell, KV, Prony
- **HexagonalArchitectureTest** (5) : regles ArchUnit

### 9.3 Tests C++ (30+ tests GoogleTest)

- **test_maxwell** (7) : relaxation, storage, loss, creep, limites
- **test_kelvin_voigt** (6) : fluage, moduli frequentiels, limites
- **test_prony_series** (8) : N-branches, limites, equilibre, spectre
- **test_lm_convergence** (4) : convergence monotone, bornes, R², multi-modele
- **test_fem_beam** (5+) : volume conservation, symetrie, cantilever, zero load

### 9.4 Load Testing (k6)

| Scenario | VUs | Duree | Seuils |
|----------|-----|-------|--------|
| Smoke | 5 | 1 min | P95 < 2s |
| Load | 0→100 | 9 min | Error rate < 5% |
| Stress | 0→300 | 14 min | P99 < 5s |

---

## 10. Performance et Scalabilite

### 10.1 Optimisations

| Composant | Strategie |
|-----------|----------|
| Compute C++ | OpenMP parallelisation assemblage FEM |
| Compute C++ | Eigen SparseMatrix (memoire O(nnz)) |
| Compute C++ | BiCGSTAB iteratif pour grands systemes |
| Java Identifier | LM avec Jacobien numerique optimise |
| Kafka | KRaft mode, 3 brokers, 3 controllers |
| PostgreSQL | Index sur organization_id, user_id, project_id |
| Frontend | Lazy loading, signals (evite change detection) |
| K8s | HPA CPU-based (60-70% target) |
| Compute pods | Node selector sur machines high-CPU |

### 10.2 Capacite cible V2

| Metrique | Cible |
|----------|-------|
| Utilisateurs simultanes | 100+ |
| Simulation 1D | < 5 sec |
| Simulation FEM 100k elements | < 10 min |
| Upload 50 Mo | < 30 sec |
| API P95 latence | < 2 sec |
| Disponibilite | 99.5% |
