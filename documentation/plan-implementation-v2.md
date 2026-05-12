# RheoSim Enterprise — Plan d'Implementation V2

## Statut d'Avancement

| Phase | Statut | Commit | Date |
|-------|--------|--------|------|
| Phase 1 — Compute Engine C++ | ✅ Complete | `d3c0c33` | 2026-05-12 |
| Phase 2 — Microservices Decomposition | ✅ Complete | `adbf883` | 2026-05-13 |
| Phase 3 — Infrastructure & Observabilite | ✅ Complete | `cbf327b` | 2026-05-13 |
| Phase 4 — Fonctionnalites Avancees | ✅ Complete | `ca7961c` | 2026-05-13 |

---

## 1. Vision V2

Passer d'un monolithe modulaire (V1) a une architecture microservices avec un moteur de calcul C++ haute performance pour la simulation 3D par elements finis (FEM).

### 1.1 Objectifs Strategiques

| Objectif | Description | Mesure de succes |
|----------|-------------|-----------------|
| Performance 3D | Simulation FEM 3D en temps raisonnable (<10 min pour maillage 100k elements) | Benchmark sur cas standards |
| Scalabilite | Support 100+ utilisateurs simultanes, jobs paralleles distribues | Load testing |
| Multi-tenant | Isolation complete entre organisations | Tests de penetration |
| Collaboration | Projets partages, roles par projet | Tests fonctionnels |
| Extensibilite | Ajout de nouveaux modeles constitutifs sans redeploiement du monolithe | Plugin architecture |

### 1.2 Perimetre V2

| Inclus | Exclu (V3+) |
|--------|-------------|
| Compute Engine C++ (gRPC) | Machine Learning auto-calibration |
| FEM 3D (tetraedres lineaires) | Simulation multi-physique (thermo-mecanique) |
| Microservices (5 services) | Marketplace de modeles |
| Multi-tenant + collaboration | Application mobile |
| Kubernetes deployment | Edge computing |
| Monitoring (Prometheus + Grafana) | Real-time streaming simulation |
| Notification temps reel (WebSocket) | - |

---

## 2. Architecture Cible V2

### 2.1 Vue d'Ensemble

```
                    ┌─────────────────────────────────────────┐
                    │           API Gateway (Kong)             │
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
                    │   gRPC Service             │
                    │   FEM 3D + 1D Legacy       │
                    └───────────────────────────┘
```

### 2.2 Stack Technique V2

| Composant | V1 | V2 |
|-----------|----|----|
| Architecture | Monolithe modulaire | Microservices |
| Compute 1D | Java (Commons Math) | Java (conserve) |
| Compute 3D | - | C++ (Eigen + custom FEM) |
| Inter-service | Kafka events | Kafka + gRPC (sync) |
| API Gateway | - | Kong / Spring Cloud Gateway |
| Service Discovery | - | Kubernetes DNS |
| Config | application.yml | Spring Cloud Config / ConfigMap |
| Orchestration | Docker Compose | Kubernetes (Helm charts) |
| Monitoring | Actuator | Prometheus + Grafana + Loki |
| Tracing | - | OpenTelemetry + Jaeger |
| Cache | Redis (prevu) | Redis Cluster |
| Object Storage | Local filesystem | MinIO (S3-compatible) |
| Notifications | - | WebSocket (STOMP) |
| Search | - | Elasticsearch (optionnel) |

---

## 3. Phases d'Implementation

### Phase 1 — Compute Engine C++ (8 semaines)

**Objectif** : Microservice C++ autonome communiquant via gRPC, capable de FEM 3D lineaire.

#### 3.1.1 Semaines 1-2 : Setup & Infrastructure gRPC

| Tache | Detail | Livrable |
|-------|--------|----------|
| Setup projet C++ | CMake, structure src/include/test, CI | Repository compile |
| Proto definitions | `compute.proto` : SimulationRequest, SimulationResponse, MeshData | Fichiers .proto |
| gRPC server skeleton | C++ gRPC server, healthcheck | Server demarre |
| gRPC client Java | Spring Boot gRPC client adapter | Client compile |
| Docker image C++ | Multi-stage (build + runtime alpine) | Image optimisee |
| Integration test | Java client → C++ server (echo) | Test passe |

**Proto definition** :
```protobuf
syntax = "proto3";
package rheosim.compute;

service ComputeEngine {
  rpc Identify(IdentificationRequest) returns (IdentificationResponse);
  rpc SimulateFEM(FEMRequest) returns (stream FEMProgress);
  rpc HealthCheck(Empty) returns (HealthResponse);
}

message IdentificationRequest {
  string model_type = 1;
  repeated double time_points = 2;
  repeated double measured_values = 3;
  repeated double initial_guess = 4;
  repeated double lower_bounds = 5;
  repeated double upper_bounds = 6;
  string fit_target = 7;
}

message IdentificationResponse {
  repeated double parameters = 1;
  repeated string parameter_names = 2;
  double r_squared = 3;
  int32 iterations = 4;
  bool converged = 5;
  repeated double fitted_curve = 6;
}

message FEMRequest {
  MeshData mesh = 1;
  MaterialProperties material = 2;
  repeated BoundaryCondition boundary_conditions = 3;
  repeated LoadStep load_steps = 4;
  SolverSettings settings = 5;
}

message MeshData {
  repeated double nodes = 1;       // [x1,y1,z1, x2,y2,z2, ...]
  repeated int32 elements = 2;     // [n1,n2,n3,n4, ...] tetrahedra
  int32 nodes_per_element = 3;
}
```

#### 3.1.2 Semaines 3-4 : Lois Constitutives C++

| Tache | Detail | Livrable |
|-------|--------|----------|
| Interface ConstitutiveLaw C++ | Classe abstraite virtuelle | Header |
| Maxwell C++ | Port direct de la version Java | Tests analytiques |
| Kelvin-Voigt C++ | Port direct | Tests analytiques |
| Prony Series C++ | Port direct, optimise Eigen | Tests analytiques |
| Levenberg-Marquardt C++ | Eigen-based, vectorise | Convergence tests |
| Benchmark Java vs C++ | Comparaison performance 1D | Rapport benchmark |

**Structure C++** :
```
rheosim-compute/
├── CMakeLists.txt
├── proto/
│   └── compute.proto
├── include/rheosim/
│   ├── constitutive_law.h
│   ├── maxwell_law.h
│   ├── prony_series_law.h
│   ├── levenberg_marquardt.h
│   ├── fem_solver.h
│   └── mesh.h
├── src/
│   ├── main.cpp              (gRPC server)
│   ├── maxwell_law.cpp
│   ├── prony_series_law.cpp
│   ├── levenberg_marquardt.cpp
│   ├── fem_solver.cpp
│   └── mesh_io.cpp
├── test/
│   ├── test_maxwell.cpp
│   ├── test_prony.cpp
│   ├── test_lm_convergence.cpp
│   └── test_fem_beam.cpp
├── Dockerfile
└── docker-compose.yml
```

#### 3.1.3 Semaines 5-7 : FEM 3D

| Tache | Detail | Livrable |
|-------|--------|----------|
| Maillage | Lecteur de maillage (format .msh Gmsh) | Parser teste |
| Element tetraedrique | Fonctions de forme P1, matrice de rigidite | Tests elementaires |
| Assemblage global | Matrice creuse (Eigen SparseMatrix) | Test poutre |
| Conditions limites | Dirichlet (deplacement impose), Neumann (force) | CL appliquees |
| Solveur lineaire | Eigen SparseLU / iteratif (BiCGSTAB) | Resolution OK |
| Viscoelasticite | Integration temporelle (schema implicite Euler) | Test relaxation 3D |
| Validation | Cas analytiques : poutre en flexion, cylindre en torsion | R² > 0.99 |

**Formulation FEM viscoelastique** :
```
K * u(t+dt) = F_ext + F_history

K = matrice de rigidite elastique instantanee
F_history = contribution de l'historique viscoelastique (serie de Prony)

Pour chaque pas de temps :
1. Calculer F_history a partir des variables internes h_i(t)
2. Resoudre le systeme lineaire
3. Mettre a jour les variables internes :
   h_i(t+dt) = exp(-dt/tau_i) * h_i(t) + G_i * (epsilon(t+dt) - epsilon(t))
```

#### 3.1.4 Semaine 8 : Integration & Performance

| Tache | Detail | Livrable |
|-------|--------|----------|
| Integration Java ↔ C++ | SimulationUseCase appelle gRPC quand type=3D | Tests integration |
| Streaming progress | FEMProgress stream pendant calcul | Frontend progress bar |
| Parallelisme | OpenMP pour assemblage + solveur | Speedup mesure |
| Profiling | Valgrind/perf sur cas 100k elements | < 10 min |
| Documentation | API gRPC, format maillage, modeles supportes | Doc technique |

---

### Phase 2 — Decomposition Microservices (6 semaines)

**Objectif** : Extraire les bounded contexts en services autonomes.

#### 3.2.1 Semaines 9-10 : Identity Service

| Tache | Detail |
|-------|--------|
| Extraction | Identity context → service autonome Spring Boot |
| Base dediee | PostgreSQL schema isole (ou instance separee) |
| API Auth | Conserve les endpoints actuels |
| Token validation | Service autonome + cle publique partagee (RS256 au lieu de HS256) |
| Migration JWT | HS256 → RS256 (asymetrique), cle publique distribuee |
| Multi-tenant | Ajout `organization_id` sur User, tenant isolation |

#### 3.2.2 Semaines 11-12 : Project + Experiment Services

| Tache | Detail |
|-------|--------|
| Project Service | Extraction, base dediee, API REST |
| Experiment Service | Extraction, MinIO pour fichiers (remplace local storage) |
| Event contracts | Schemas Avro pour events Kafka (schema registry) |
| Saga patterns | Upload dataset → validation → notification (choreography) |

#### 3.2.3 Semaines 13-14 : Simulation + Reporting Services

| Tache | Detail |
|-------|--------|
| Simulation Service | Extraction, routing 1D (Java) vs 3D (C++ gRPC) |
| Job queue | Kafka-based job queue avec dead letter |
| Reporting Service | Extraction, templates enrichis |
| Notification Service | Nouveau service : WebSocket push, email (optionnel) |

---

### Phase 3 — Infrastructure & Observabilite (4 semaines)

#### 3.3.1 Semaines 15-16 : Kubernetes & Helm

| Tache | Detail |
|-------|--------|
| Helm charts | 1 chart par service + chart umbrella |
| Namespace isolation | dev, staging, prod |
| Horizontal Pod Autoscaler | CPU/memory-based scaling |
| Persistent Volumes | PostgreSQL, Kafka, MinIO |
| Secrets management | Kubernetes Secrets + Sealed Secrets |
| Ingress | nginx-ingress avec TLS (cert-manager) |

**Structure Helm** :
```
deploy/
├── charts/
│   ├── identity-service/
│   ├── project-service/
│   ├── experiment-service/
│   ├── simulation-service/
│   ├── reporting-service/
│   ├── compute-engine/
│   └── infrastructure/     (kafka, postgres, redis, minio)
└── rheosim/                 (umbrella chart)
    ├── Chart.yaml
    ├── values.yaml
    ├── values-dev.yaml
    ├── values-staging.yaml
    └── values-prod.yaml
```

#### 3.3.2 Semaines 17-18 : Monitoring & Observabilite

| Tache | Detail |
|-------|--------|
| Prometheus | Scraping metriques Spring Actuator + custom |
| Grafana | Dashboards : latence API, jobs queue, compute time |
| Loki | Aggregation logs centralise |
| OpenTelemetry | Traces distribuees inter-services |
| Jaeger | Visualisation traces |
| Alerting | PagerDuty/Slack sur erreurs critiques |
| Health dashboards | Status page par service |

**Metriques custom** :
```
rheosim_simulation_duration_seconds{model_type, dimension}
rheosim_simulation_convergence_rate{model_type}
rheosim_dataset_upload_bytes_total
rheosim_active_jobs_count{status}
rheosim_fem_elements_processed_total
```

---

### Phase 4 — Fonctionnalites Avancees (6 semaines)

#### 3.4.1 Semaines 19-20 : Collaboration & Multi-Tenant

| Tache | Detail |
|-------|--------|
| Organisations | CRUD organisations, invitation par email |
| Roles projet | OWNER, EDITOR, VIEWER par projet |
| Partage projet | Invitation a collaborer sur un projet |
| Tenant isolation | Filtre automatique par org_id sur toutes les queries |
| Audit trail | Historique complet des actions par utilisateur |

#### 3.4.2 Semaines 21-22 : Frontend V2

| Tache | Detail |
|-------|--------|
| Visualisation 3D | Three.js : affichage maillage + resultats deformation |
| WebSocket | Notifications temps reel (job termine, invitation) |
| Comparaison modeles | Superposition courbes experimentales vs simulees |
| Export avance | Export resultats 3D (VTK format pour ParaView) |
| Dashboard avance | Graphiques tendance, historique convergence |
| Responsive | Adaptation tablette (pas mobile, application desktop-first) |

#### 3.4.3 Semaines 23-24 : Qualite & Performance

| Tache | Detail |
|-------|--------|
| Load testing | k6 / Gatling : 100 utilisateurs simultanes |
| Chaos engineering | Litmus : resilience aux pannes (pod kill, network partition) |
| Security audit | OWASP ZAP, dependency scanning (Snyk) |
| Performance profiling | Async Profiler (Java), perf (C++) |
| Documentation API | OpenAPI complete avec exemples |
| Migration guide | V1 → V2 pour les utilisateurs existants |

---

## 4. Compute Engine C++ — Design Detaille

### 4.1 Dependances C++

| Librairie | Version | Role |
|-----------|---------|------|
| Eigen | 3.4 | Algebre lineaire, matrices creuses |
| gRPC | 1.60+ | Communication inter-services |
| Protobuf | 3.25+ | Serialisation |
| GoogleTest | 1.14 | Tests unitaires |
| spdlog | 1.12 | Logging |
| fmt | 10.x | Formatage strings |
| OpenMP | 5.0 | Parallelisme CPU |

### 4.2 Architecture Interne

```
┌─────────────────────────────────────────────────┐
│                gRPC Server                       │
│  ┌───────────────┐  ┌────────────────────────┐  │
│  │ Identification │  │     FEM Solver         │  │
│  │   Service      │  │                        │  │
│  │  (LM optimizer)│  │  ┌──────┐  ┌───────┐  │  │
│  │               │  │  │Mesh  │  │Element│  │  │
│  │  ┌──────────┐ │  │  │Reader│  │Library│  │  │
│  │  │Constitut.│ │  │  └──────┘  └───────┘  │  │
│  │  │Laws      │ │  │  ┌──────┐  ┌───────┐  │  │
│  │  └──────────┘ │  │  │Assemb│  │Solver │  │  │
│  │               │  │  │ler   │  │(Eigen)│  │  │
│  └───────────────┘  │  └──────┘  └───────┘  │  │
│                      └────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 4.3 Element Fini Tetraedrique P1

```
Fonctions de forme (coordonnees barycentriques) :
  N1 = 1 - xi - eta - zeta
  N2 = xi
  N3 = eta
  N4 = zeta

Matrice de rigidite elementaire :
  Ke = V * B^T * D * B

  V = volume du tetraedre
  B = matrice deformation-deplacement (constante pour P1)
  D = matrice de comportement (3D viscoelastique)

Pour viscoelasticite lineaire (Prony) :
  D(t) = D_inf + sum(D_i * exp(-t/tau_i))
  
  → Integration temporelle implicite :
  D_eff = D_inf + sum(D_i * (1 - exp(-dt/tau_i)) * tau_i / dt)
```

### 4.4 Parallelisation

| Etape | Strategie | Speedup attendu |
|-------|-----------|-----------------|
| Assemblage | OpenMP parallel for sur elements | ~4x (4 cores) |
| Solveur iteratif | Eigen built-in parallelism | ~2-3x |
| Multi-jobs | 1 job = 1 thread gRPC | Lineaire |
| SIMD | Eigen auto-vectorisation (AVX2) | ~2x operations matricielles |

---

## 5. Migration V1 → V2

### 5.1 Strategie : Strangler Fig Pattern

Ne pas reecrire d'un coup. Extraire progressivement :

```
Etape 1: V1 monolithe + C++ compute engine (gRPC)
         └── Le monolithe appelle le C++ pour les jobs 3D

Etape 2: Extraire Identity Service
         └── Les autres contextes restent dans le monolithe

Etape 3: Extraire Project + Experiment
         └── Simulation + Reporting restent

Etape 4: Extraire Simulation + Reporting
         └── Monolithe vide → supprime
```

### 5.2 Compatibilite Base de Donnees

- Flyway V6+ : split des tables vers les schemas dedies
- Pas de breaking changes sur les APIs REST (versionning /v1 vs /v2)
- Events Kafka : ajout schema registry Avro pour evolution compatible

### 5.3 Migration Frontend

- Le frontend ne change pas de structure (services HTTP restent identiques)
- Ajout WebSocket pour notifications
- Ajout Three.js pour visualisation 3D
- Routes supplementaires pour collaboration/organisations

---

## 6. Estimation Effort & Planning

### 6.1 Resume par Phase

| Phase | Duree | Effort (j/h) | Equipe |
|-------|-------|--------------|--------|
| Phase 1 : Compute C++ | 8 semaines | 40 j/h | 1 dev C++ + 1 dev Java |
| Phase 2 : Microservices | 6 semaines | 30 j/h | 2 devs Java |
| Phase 3 : Infrastructure | 4 semaines | 20 j/h | 1 dev DevOps + 1 dev |
| Phase 4 : Features | 6 semaines | 30 j/h | 2 devs full-stack |
| **Total** | **24 semaines** | **120 j/h** | **Equipe 3-4 personnes** |

### 6.2 Timeline

```
Mois 1-2     │ Phase 1 : Compute Engine C++
             │ ████████████████████████████████
             │
Mois 3       │ Phase 2 : Microservices (debut)
             │ ████████████████████
             │
Mois 4       │ Phase 2 (fin) + Phase 3 (debut)
             │ ██████████ ████████████████
             │
Mois 5       │ Phase 3 (fin) + Phase 4 (debut)
             │ ████████ ████████████████████
             │
Mois 6       │ Phase 4 (fin) + Stabilisation
             │ ████████████████████████ ████
```

### 6.3 Jalons

| Jalon | Semaine | Critere de validation |
|-------|---------|----------------------|
| M1 : C++ gRPC operationnel | S2 | Java appelle C++, echo fonctionne |
| M2 : Identification C++ validee | S4 | Memes resultats que Java (R² identique) |
| M3 : FEM 3D poutre | S7 | Deflexion poutre = solution analytique (1%) |
| M4 : FEM 3D viscoelastique | S8 | Relaxation 3D converge |
| M5 : Identity Service autonome | S10 | Auth fonctionne en microservice |
| M6 : Tous services extraits | S14 | Monolithe supprime |
| M7 : Kubernetes deploye | S16 | Staging operationnel |
| M8 : Monitoring complet | S18 | Dashboard Grafana + alertes |
| M9 : Collaboration OK | S20 | Multi-user sur meme projet |
| M10 : V2 Release | S24 | Load test 100 users OK |

---

## 7. Risques V2

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|-------------|--------|------------|
| 1 | Complexite FEM 3D sous-estimee | Haute | Haut | Commencer par P1 lineaire, cas simples. Prevoir buffer +2 semaines. |
| 2 | Performance C++ insuffisante | Faible | Haut | Profiling precoce, solveur iteratif, parallelisme OpenMP. |
| 3 | Integration gRPC instable | Moyenne | Moyen | Circuit breaker, retry, fallback sur Java 1D. |
| 4 | Migration microservices casse la V1 | Moyenne | Haut | Strangler fig, feature flags, deploiement canary. |
| 5 | Complexite operationnelle K8s | Haute | Moyen | Formation equipe, Helm charts testes, GitOps (ArgoCD). |
| 6 | Latence inter-services | Moyenne | Moyen | Cache Redis, batch requests, async quand possible. |
| 7 | Perte de donnees migration | Faible | Critique | Backup pre-migration, dry-run, rollback plan. |
| 8 | Scope creep V2 | Haute | Haut | Definition stricte du perimetre, NO sur features V3. |

---

## 8. Prerequis & Competences

### 8.1 Competences Requises

| Competence | Phase | Niveau |
|------------|-------|--------|
| C++ moderne (C++17/20) | Phase 1 | Senior |
| Algebre lineaire (Eigen) | Phase 1 | Intermediaire |
| Methode elements finis | Phase 1 | Senior |
| gRPC / Protobuf | Phase 1-2 | Intermediaire |
| Kubernetes / Helm | Phase 3 | Intermediaire |
| Spring Cloud | Phase 2 | Intermediaire |
| Prometheus / Grafana | Phase 3 | Junior |
| Three.js | Phase 4 | Intermediaire |

### 8.2 Infrastructure Prerequise

- Cluster Kubernetes (managed : GKE, EKS, ou AKS)
- Registry d'images (GitHub Container Registry ou DockerHub)
- MinIO ou S3 pour object storage
- CI/CD : GitHub Actions + ArgoCD pour GitOps
- Environnements : dev (local), staging (K8s), prod (K8s)

---

## 9. Definition of Done — V2 Release

- [ ] Compute Engine C++ : identification 1D + FEM 3D viscoelastique
- [ ] 5 microservices independants deployes sur Kubernetes
- [ ] API Gateway avec rate limiting et auth centralisee
- [ ] Migration JWT HS256 → RS256
- [ ] Multi-tenant avec organisations
- [ ] Collaboration (roles par projet)
- [ ] Frontend : visualisation 3D (Three.js), WebSocket notifications
- [ ] Monitoring : Prometheus + Grafana + alertes
- [ ] Tracing distribue : OpenTelemetry + Jaeger
- [ ] Load test : 100 utilisateurs simultanes sans degradation
- [ ] Security audit passe (OWASP Top 10)
- [ ] Documentation API complete (OpenAPI + gRPC)
- [ ] Migration guide V1 → V2
- [ ] Zero downtime deployment (rolling update)
