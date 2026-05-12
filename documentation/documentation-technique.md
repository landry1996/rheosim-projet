# RheoSim Enterprise — Documentation Technique

## 1. Vue d'Ensemble Architecturale

### 1.1 Architecture Hexagonale (Ports & Adapters)

Le projet suit une architecture hexagonale stricte avec separation en modules Maven :

```
rheosim-backend/
├── rheosim-domain/           # Coeur metier (entites, events, ports)
│   └── com.rheosim.domain
│       ├── identity/         # User, Role, RefreshToken
│       ├── project/          # Project, Material, MaterialModel
│       ├── experiment/       # Dataset, DataColumn, ValidationError
│       ├── simulation/       # SimulationJob, SimulationResult
│       └── reporting/        # Report, DublinCoreMetadata
│
├── rheosim-application/      # Cas d'usage, orchestration
│   └── com.rheosim.application
│       ├── identity/         # AuthenticationUseCase
│       ├── project/          # ProjectUseCase, MaterialUseCase
│       ├── experiment/       # DatasetUseCase
│       ├── simulation/       # SimulationUseCase
│       └── reporting/        # ReportingUseCase
│
├── rheosim-infrastructure/   # Adaptateurs (JPA, REST, Kafka, Engine)
│   └── com.rheosim.infrastructure
│       ├── identity/         # Security config, JWT, controllers
│       ├── project/          # JPA adapters, controllers
│       ├── experiment/       # Parsers, validators, file storage
│       ├── simulation/       # Compute engine, LM optimizer
│       └── reporting/        # PDF/CSV/JSON generators
│
└── rheosim-bootstrap/        # Point d'entree Spring Boot
    └── com.rheosim
        ├── RheoSimApplication.java
        └── config/OpenApiConfig.java
```

### 1.2 Regles Architecturales (Enforced by ArchUnit)

1. **Domain ne depend de rien** : Aucune dependance vers application, infrastructure ou frameworks
2. **Application depend uniquement de Domain** : Utilise les ports definis dans domain
3. **Infrastructure implemente les ports** : Adapters vers JPA, REST, Kafka, filesystem
4. **Pas de cycle de dependances** : Verification ArchUnit automatique
5. **Les ports sont des interfaces dans domain** : Inversion de dependance

### 1.3 Communication Inter-Contextes

```
[Identity] ──event──→ Kafka ──→ [Audit]
[Experiment] ──event──→ Kafka ──→ [Notification]
[Simulation] ──event──→ Kafka ──→ [Notification]
```

Events publies via `DomainEvent` abstrait, serialises en JSON (StringSerializer).

---

## 2. Stack Technique Detaillee

### 2.1 Backend

| Composant | Version | Role |
|-----------|---------|------|
| Java | 21 (compile) / 25 (runtime) | Langage |
| Spring Boot | 3.4.5 | Framework applicatif |
| Spring Security | 6.x | Authentification/autorisation |
| Spring Data JPA | 3.x | Persistence |
| Hibernate | 6.x | ORM |
| Flyway | 10.x | Migrations base de donnees |
| Apache Kafka | Client 3.7 | Messaging event-driven |
| jjwt | 0.12.6 | Generation/verification JWT |
| MapStruct | 1.6.3 | Mapping compile-time |
| Apache Commons Math | 3.6.1 | Optimisation numerique (LM) |
| Apache Commons CSV | 1.12.0 | Parsing CSV |
| Apache POI | 5.3.0 | Parsing Excel |
| Apache PDFBox | 3.0.3 | Generation PDF |
| Bucket4j | 8.14.0 | Rate limiting |
| springdoc-openapi | 2.8.4 | Documentation API |

### 2.2 Frontend

| Composant | Version | Role |
|-----------|---------|------|
| Angular | 21.2 | Framework SPA |
| TypeScript | 5.9 | Langage |
| Chart.js | 4.x | Visualisation graphique |
| RxJS | 7.8 | Programmation reactive |

### 2.3 Infrastructure

| Composant | Version | Role |
|-----------|---------|------|
| PostgreSQL | 15 | Base de donnees relationnelle |
| Apache Kafka | 3.7 (KRaft) | Bus d'evenements |
| Redis | 7 | Cache (prevu V2) |
| Docker | Latest | Conteneurisation |
| nginx | Alpine | Reverse proxy frontend |
| GitHub Actions | v4 | CI/CD |

---

## 3. Modele de Donnees

### 3.1 Schema PostgreSQL

```sql
-- Schema: rheosim

-- Identity
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

-- Audit
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    user_id UUID,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    owner_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE materials (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    grade VARCHAR(100),
    family VARCHAR(50) NOT NULL,
    supplier VARCHAR(200),
    version INTEGER DEFAULT 1,
    model_type VARCHAR(50),
    model_data JSONB,
    project_id UUID REFERENCES projects(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE project_materials (
    project_id UUID REFERENCES projects(id),
    material_id UUID REFERENCES materials(id),
    PRIMARY KEY (project_id, material_id)
);

-- Experiment
CREATE TABLE datasets (
    id UUID PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    experiment_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    project_id UUID REFERENCES projects(id),
    row_count INTEGER DEFAULT 0,
    columns_metadata JSONB,
    validation_errors JSONB,
    file_path VARCHAR(1000),
    content_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Simulation
CREATE TABLE simulation_jobs (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    material_id UUID REFERENCES materials(id),
    dataset_id UUID REFERENCES datasets(id),
    simulation_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    fit_target VARCHAR(50),
    initial_guess DOUBLE PRECISION[],
    result_data JSONB,
    submitted_at TIMESTAMP DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

-- Reporting
CREATE TABLE reports (
    id UUID PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    format VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    project_id UUID REFERENCES projects(id),
    generated_by UUID REFERENCES users(id),
    file_path VARCHAR(1000),
    file_size_bytes BIGINT DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 3.2 Migrations Flyway

| Version | Fichier | Contenu |
|---------|---------|---------|
| V1 | `V1__create_identity_tables.sql` | users, roles, user_roles, refresh_tokens, audit_logs |
| V2 | `V2__create_project_tables.sql` | projects, materials, project_materials |
| V3 | `V3__create_dataset_table.sql` | datasets |
| V4 | `V4__create_simulation_jobs_table.sql` | simulation_jobs |
| V5 | `V5__create_reports_table.sql` | reports |

---

## 4. Securite

### 4.1 Authentification JWT

```
Client                    Backend
  │                          │
  │── POST /auth/login ─────→│ Verifie credentials
  │←── {accessToken,         │ Genere tokens
  │     refreshToken} ───────│
  │                          │
  │── GET /api/... ─────────→│ Valide accessToken
  │   Authorization: Bearer  │ (JwtAuthenticationFilter)
  │                          │
  │── POST /auth/refresh ───→│ Verifie refreshToken
  │←── {newAccessToken,      │ Rotation du refresh
  │     newRefreshToken} ────│
```

**Configuration tokens** :
- Access token : 15 minutes (HS256)
- Refresh token : 7 jours (UUID opaque, stocke en base)
- Secret : configurable via `JWT_SECRET` (minimum 32 caracteres)

### 4.2 RBAC

| Role | Permissions |
|------|-------------|
| USER | CRUD sur ses propres projets, materiaux, datasets, simulations, rapports |
| ADMIN | Tout USER + gestion utilisateurs + acces tous projets |

### 4.3 Protection

- **Rate Limiting** : Bucket4j, 60 req/min par IP
- **CORS** : Origines configurables (defaut localhost:4200)
- **Validation** : Bean Validation sur tous les DTOs entrants
- **SQL Injection** : Parameterized queries via JPA/Hibernate
- **XSS** : Angular sanitize par defaut
- **CSRF** : Non necessaire (API stateless JWT)

---

## 5. Moteur de Calcul Scientifique

### 5.1 Architecture du Compute Engine

```
ConstitutiveLaw (interface/port)
├── MaxwellLaw
├── KelvinVoigtLaw
└── PronySeriesLaw

ParameterIdentificationPort (interface/port)
└── LevenbergMarquardtIdentifier (adapter)
    └── Apache Commons Math 3 LeastSquaresBuilder

SimulationJobProcessor (@Scheduled)
└── Poll QUEUED jobs → Execute → Store result
```

### 5.2 Interface ConstitutiveLaw

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

### 5.3 Algorithme d'Identification

**Methode** : Levenberg-Marquardt (moindres carres non-lineaires)

**Implementation** :
1. Construction du probleme `LeastSquaresProblem` (Commons Math 3)
2. Jacobien numerique par differences centrales (pas = 1e-8 * parametre)
3. Contraintes bornees (clipping post-iteration)
4. Criteres de convergence : tolerance relative 1e-12, max 1000 iterations
5. Calcul R² = 1 - SS_res / SS_tot

**Parametres Maxwell** : [G (Pa), tau (s)]
- G(t) = G * exp(-t/tau)
- G'(w) = G * (w*tau)² / (1 + (w*tau)²)
- G''(w) = G * (w*tau) / (1 + (w*tau)²)

**Parametres Prony N-branches** : [G_inf, G_1, tau_1, G_2, tau_2, ...]
- G(t) = G_inf + sum(G_i * exp(-t/tau_i))

### 5.4 Job Processing

- Polling toutes les 2 secondes (`@Scheduled(fixedDelay = 2000)`)
- Maximum 4 jobs en parallele
- Statut : QUEUED → RUNNING → COMPLETED/FAILED
- Timeout : 1 heure par job
- Resultats stockes en JSONB dans `simulation_jobs.result_data`

---

## 6. API REST

### 6.1 Convention

- Base path : `/api`
- Versionning : `/api/v1/` pour les endpoints metier (prevu)
- Content-Type : `application/json` (sauf upload multipart)
- Authentification : Header `Authorization: Bearer <token>`
- Erreurs : format uniforme `{error, message, timestamp, path}`

### 6.2 Endpoints Principaux

#### Authentification

```
POST /api/auth/register     → AuthResponse
POST /api/auth/login        → AuthResponse
POST /api/auth/refresh      → AuthResponse
POST /api/auth/logout       → 204 No Content
```

#### Projets

```
GET    /api/projects         → Project[]
POST   /api/projects         → Project
GET    /api/projects/{id}    → Project
PUT    /api/projects/{id}    → Project
DELETE /api/projects/{id}    → 204
PATCH  /api/projects/{id}/activate → Project
PATCH  /api/projects/{id}/archive  → Project
```

#### Materiaux

```
GET    /api/materials?projectId=   → Material[]
POST   /api/materials              → Material
GET    /api/materials/{id}         → Material
PUT    /api/materials/{id}/model   → Material
DELETE /api/materials/{id}         → 204
```

#### Datasets

```
POST   /api/datasets               → Dataset (multipart: file + projectId + experimentType)
POST   /api/datasets/{id}/validate → Dataset
GET    /api/datasets?projectId=    → Dataset[]
GET    /api/datasets/{id}          → Dataset
DELETE /api/datasets/{id}          → 204
```

#### Simulations

```
POST   /api/simulations            → SimulationJob
GET    /api/simulations/{id}       → SimulationJob
GET    /api/simulations/{id}/result → SimulationResult
POST   /api/simulations/{id}/cancel → 204
GET    /api/simulations?projectId=  → SimulationJob[]
```

#### Rapports

```
POST   /api/reports                → Report
GET    /api/reports?projectId=     → Report[]
GET    /api/reports/{id}           → Report
GET    /api/reports/{id}/download  → Binary (blob)
DELETE /api/reports/{id}           → 204
```

### 6.3 Gestion d'Erreurs

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Email already exists",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/auth/register"
}
```

Codes HTTP utilises : 200, 201, 204, 400, 401, 403, 404, 409, 413, 429, 500

---

## 7. Frontend Angular

### 7.1 Architecture

```
src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── models/             # Interfaces TypeScript (DTOs)
│   ├── services/           # HttpClient services (1 par bounded context)
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
│   └── reports/           # Generation + download
│
└── app.ts                  # Root component (layout conditionnel)
```

### 7.2 Patterns Utilises

- **Signals** : Etat reactif (Angular 16+), remplace une partie de RxJS
- **Standalone Components** : Pas de NgModules, imports directs
- **Lazy Loading** : `loadComponent()` dans les routes
- **Functional Guards** : `CanActivateFn` au lieu de classes
- **Functional Interceptors** : `HttpInterceptorFn`
- **OnPush-like** : Signals + templates declaratifs

### 7.3 Intercepteur HTTP

```
Request → authInterceptor → [Attach Bearer token] → Backend
                          ← [401 Unauthorized] ←
                          → [Refresh token] →
                          → [Retry original request with new token] →
```

---

## 8. DevOps

### 8.1 Docker

**Backend** (multi-stage) :
1. Stage build : Eclipse Temurin JDK 21, Maven, compile + package
2. Stage runtime : Eclipse Temurin JRE 21 Alpine, utilisateur non-root

**Frontend** (multi-stage) :
1. Stage build : Node 22 Alpine, `ng build --production`
2. Stage runtime : nginx Alpine, SPA routing + reverse proxy

### 8.2 CI/CD Pipeline

```
Push/PR → [backend-test] → [frontend-test] → [docker-build]
              │                   │                 │
         Java 21 + PG       Node 22 + build    Buildx multi-arch
         mvn verify          ng build           (main only)
```

### 8.3 Configuration

Variables d'environnement :

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | dev | Profil Spring |
| `SPRING_DATASOURCE_URL` | localhost:5432 | URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | rheosim | User DB |
| `SPRING_DATASOURCE_PASSWORD` | - | Password DB |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Brokers Kafka |
| `JWT_SECRET` | - | Secret HMAC (min 32 chars) |
| `CORS_ORIGINS` | http://localhost:4200 | Origines CORS |
| `RHEOSIM_STORAGE_PATH` | ./data/uploads | Stockage fichiers |
| `SERVER_PORT` | 8080 | Port serveur |

---

## 9. Tests

### 9.1 Strategie de Tests

| Niveau | Outil | Cible | Couverture |
|--------|-------|-------|-----------|
| Unitaire | JUnit 5 + Mockito | Domain, Application | Invariants metier |
| Integration | Testcontainers | Infrastructure | Persistence, parsers |
| Architecture | ArchUnit | Tous modules | Regles hexagonales |
| Scientifique | JUnit 5 | Compute engine | Solutions analytiques |

### 9.2 Tests Existants (88 tests)

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

### 9.3 Regles ArchUnit

```java
// Domain ne depend d'aucun framework
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..infrastructure..", "..application..",
        "org.springframework..", "jakarta..");

// Application ne depend pas d'infrastructure
noClasses().that().resideInAPackage("..application..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..");

// Ports sont des interfaces
classes().that().resideInAPackage("..port..")
    .should().beInterfaces();
```

---

## 10. Performance et Scalabilite

### 10.1 Bottlenecks Identifies

| Composant | Contrainte | Mitigation |
|-----------|-----------|-----------|
| Compute LM | CPU-bound | 4 threads max, timeout 1h |
| Upload fichiers | IO-bound, 50 Mo max | Streaming, validation async |
| Kafka | Throughput | 1 partition/topic (V1), scalable V2 |
| PostgreSQL | Connexions | HikariCP pool (defaut 10) |

### 10.2 Preparation V2 (Microservices)

- Bounded contexts deja isoles (packages separes)
- Events Kafka = contrat inter-services
- Ports = interfaces stables pour remplacement d'implementation
- Compute Engine → microservice C++ gRPC (3D FEM)
