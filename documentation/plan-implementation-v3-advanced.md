# RheoSim Enterprise — Plan d'Implementation V3 Advanced

## Vue d'Ensemble

Ce plan couvre les fonctionnalites avancees du V3 qui necessitent une implementation dediee apres le socle principal. Chaque phase est independante et peut etre implementee en parallele.

---

## Phase A — GPU Acceleration (CUDA/OpenCL) — 6 semaines

### Objectif
Accelerer les calculs FEM 3D et l'inference ML par GPU, avec fallback CPU automatique.

### A.1 Semaines 1-2 : Infrastructure CUDA

| Tache | Detail | Livrable |
|-------|--------|----------|
| CUDA setup CMake | Detection CUDA toolkit, compilation conditionnelle | CMakeLists.txt GPU |
| Kernel assemblage | `cu_assemble_stiffness()` — 1 thread par element | kernel_assembly.cu |
| Transfer host↔device | Strategie memoire (pinned, unified, streams) | Memory manager |
| OpenCL fallback | Meme interface, backend OpenCL pour AMD/Intel | opencl_backend.cpp |
| Feature flag | `--gpu=auto|cuda|opencl|cpu` au runtime | Config CLI |

### A.2 Semaines 3-4 : Solveur GPU

| Tache | Detail | Livrable |
|-------|--------|----------|
| cuSPARSE assemblage | COO → CSR conversion sur GPU | Sparse matrix GPU |
| cuSOLVER direct | Cholesky creux pour systemes < 500k DOF | Solveur direct GPU |
| AmgX iteratif | Algebraic MultiGrid pour systemes > 500k DOF | Solveur AMG |
| Jacobien parallele | Evaluations LM en batch sur GPU | LM GPU kernel |
| Benchmark CI | Script benchmark auto (100k, 500k, 1M elements) | benchmark_gpu.sh |

### A.3 Semaines 5-6 : Integration & Kubernetes

| Tache | Detail | Livrable |
|-------|--------|----------|
| ONNX GPU inference | TensorRT provider pour ONNX Runtime | ML GPU inference |
| NVIDIA GPU Operator | Helm chart avec device plugin + time-slicing | gpu-operator values |
| Pod scheduling | nodeSelector gpu=true, tolerations | K8s manifests |
| Threshold auto | < 10k elements → CPU, >= 10k → GPU | Routing logic |
| Tier enforcement | FREE → CPU only, PRO/ENTERPRISE → GPU | QuotaFilter update |
| Dockerfile GPU | Image CUDA base + runtime | Dockerfile.gpu |

### Architecture

```
ComputeEngine
├── DeviceManager
│   ├── detect_gpu() → CUDA / OpenCL / None
│   └── select_device(problem_size, user_tier)
├── CpuBackend (existant)
│   ├── assemble_omp()
│   └── solve_sparse_lu()
└── GpuBackend
    ├── CudaBackend
    │   ├── cu_assemble()
    │   ├── cu_solve_cholesky()
    │   └── cu_jacobian_batch()
    └── OpenClBackend
        ├── cl_assemble()
        └── cl_solve()
```

### Fichiers a creer

```
rheosim-compute/
├── include/rheosim/
│   ├── gpu_backend.h          # Interface abstraite GPU
│   ├── cuda_backend.h         # Implementation CUDA
│   └── opencl_backend.h       # Implementation OpenCL
├── src/
│   ├── gpu_backend.cpp        # Device detection, routing
│   ├── cuda/
│   │   ├── kernel_assembly.cu # Assemblage par element
│   │   ├── kernel_solver.cu   # cuSOLVER wrapper
│   │   └── kernel_jacobian.cu # LM Jacobien parallele
│   └── opencl/
│       ├── kernels.cl         # Kernels OpenCL
│       └── opencl_backend.cpp # Host code
├── benchmark/
│   ├── benchmark_gpu.cpp      # Benchmark executable
│   └── benchmark_gpu.sh       # Script CI
└── CMakeLists.txt             # +find_package(CUDA), option(USE_GPU)
```

### Criteres de succes

| Metrique | Cible |
|----------|-------|
| Speedup assemblage (1M elements) | > 10x vs OpenMP |
| Speedup solveur (1M DOF) | > 5x vs SparseLU |
| Temps FEM 1M elements total | < 2 min |
| Fallback CPU transparent | Aucune erreur si pas de GPU |

---

## Phase B — WebAssembly Plugin Runtime — 4 semaines

### Objectif
Executer les plugins de modeles constitutifs communautaires dans un sandbox WebAssembly securise.

### B.1 Semaines 1-2 : Runtime & SDK

| Tache | Detail | Livrable |
|-------|--------|----------|
| Wasmtime integration | Embedder C++ (wasmtime-cpp) | Runtime compile |
| Plugin C ABI | Interface standardisee (relaxation, creep, storage, loss) | plugin_api.h |
| SDK Rust | Crate `rheosim-plugin-sdk` avec macros proc | Crate publie |
| SDK C | Headers + example plugin Maxwell custom | Example compile |
| Sandbox limits | CPU: 5s timeout, Memory: 64MB, pas de filesystem/network | Config securite |
| Validation pipeline | Tests auto interface (bornes, NaN, convergence) | CI validator |

### B.2 Semaines 3-4 : Integration & Registry

| Tache | Detail | Livrable |
|-------|--------|----------|
| OCI registry | Stockage .wasm dans registry compatible Docker | Registry config |
| Upload pipeline | Upload .wasm → scan (wasm-validate) → hash → publish | API endpoint |
| Hot-reload | Charger/decharger plugins sans restart du compute engine | Plugin manager |
| ConstitutiveLaw bridge | Plugin WASM expose meme interface que lois C++ natives | Adapter pattern |
| gRPC plugin dispatch | Simulation service specifie plugin_id → compute route vers WASM | Proto update |
| Frontend plugin test | Page "Test your plugin" avec donnees synthetiques | Composant Angular |

### Architecture

```
PluginManager
├── load_plugin(wasm_bytes) → PluginInstance
├── unload_plugin(id)
├── list_plugins() → PluginMetadata[]
└── PluginInstance
    ├── call_relaxation_modulus(t, params) → double
    ├── call_storage_modulus(omega, params) → double
    ├── call_loss_modulus(omega, params) → double
    ├── call_creep_compliance(t, params) → double
    └── get_metadata() → PluginMetadata

Sandbox (Wasmtime)
├── fuel_limit: 1_000_000_000 (CPU)
├── memory_limit: 64MB
├── epoch_interruption: 5s
└── wasi: deny all
```

### Fichiers a creer

```
rheosim-compute/
├── include/rheosim/
│   ├── plugin_api.h           # C ABI interface
│   ├── plugin_manager.h       # Chargement/execution plugins
│   └── wasm_runtime.h         # Wasmtime wrapper
├── src/
│   ├── plugin_manager.cpp     # Implementation
│   └── wasm_runtime.cpp       # Wasmtime C++ wrapper
├── sdk/
│   ├── c/
│   │   ├── plugin_api.h       # Headers SDK C
│   │   └── example_maxwell/   # Plugin example
│   └── rust/
│       ├── Cargo.toml         # Crate rheosim-plugin-sdk
│       └── src/lib.rs         # Macros + traits
└── CMakeLists.txt             # +find_package(wasmtime)

rheosim-backend/
└── rheosim-infrastructure/
    └── marketplace/
        └── adapter/
            └── PluginUploadValidator.java  # Validation WASM
```

### Criteres de succes

| Metrique | Cible |
|----------|-------|
| Temps execution plugin (1 appel) | < 1ms |
| Isolation memoire | 0 acces hors sandbox |
| Timeout respect | Kill apres 5s |
| Compatible plugins | C, Rust, AssemblyScript |

---

## Phase C — Service Mesh & mTLS (Istio) — 3 semaines

### Objectif
Zero-trust networking avec chiffrement mutuel TLS entre tous les services, observabilite avancee du trafic.

### C.1 Semaine 1 : Installation Istio

| Tache | Detail | Livrable |
|-------|--------|----------|
| Istio install | `istioctl install --set profile=production` | Control plane |
| Sidecar injection | Label namespace `istio-injection=enabled` | Envoy proxies |
| PeerAuthentication | Mode STRICT (mTLS obligatoire) | Policy appliquee |
| AuthorizationPolicy | Regles explicites service→service | Policies |
| Migration Network Policies | Completer avec Istio policies (L7) | Coexistence |

### C.2 Semaine 2 : Traffic Management

| Tache | Detail | Livrable |
|-------|--------|----------|
| VirtualService | Routing, retries, timeouts par service | VS manifests |
| DestinationRule | Circuit breaker, connection pool | DR manifests |
| Canary deployments | Weight-based routing (90/10) | Canary config |
| Rate limiting Envoy | Global rate limit service (ratelimit) | Envoy filter |
| Fault injection | Test resilience (abort, delay) | Test scenarios |

### C.3 Semaine 3 : Observabilite & Securite

| Tache | Detail | Livrable |
|-------|--------|----------|
| Kiali dashboard | Topology de services, health | Kiali deploy |
| Jaeger traces | Distributed tracing via Envoy | Jaeger config |
| mTLS verification | `istioctl analyze`, pas de plaintext | Audit passe |
| Certificate rotation | Auto-rotation certs (default 24h) | Validation |
| Egress control | ServiceEntry pour APIs externes (Stripe, MLflow) | Egress policies |

### Fichiers a creer/modifier

```
deploy/
├── istio/
│   ├── peer-authentication.yaml    # STRICT mTLS
│   ├── authorization-policies.yaml # Service-to-service rules
│   ├── virtual-services.yaml       # Routing + retries
│   ├── destination-rules.yaml      # Circuit breakers
│   ├── gateway.yaml                # Ingress gateway
│   └── service-entries.yaml        # Egress whitelist
└── edge/
    └── values.yaml                 # +istio.enabled: true
```

### Criteres de succes

| Metrique | Cible |
|----------|-------|
| mTLS coverage | 100% des communications |
| Latence ajoutee (sidecar) | < 5ms P99 |
| Circuit breaker | Trip apres 5 erreurs consecutives |
| Cert rotation | Automatique, 0 downtime |

---

## Phase D — Chaos Engineering (Litmus) — 2 semaines

### Objectif
Valider la resilience du systeme face aux pannes reelles (pod kill, reseau, disque).

### D.1 Semaine 1 : Setup & Scenarios

| Tache | Detail | Livrable |
|-------|--------|----------|
| Litmus install | Helm chart LitmusChaos | Operator installe |
| Pod kill | Tuer aleatoirement 1 pod backend | Experiment |
| Pod delete | Supprimer un pod ML service | Experiment |
| Network loss | 100% packet loss entre backend↔postgresql | Experiment |
| Network latency | +500ms entre backend↔ML service | Experiment |
| Disk fill | Remplir PVC PostgreSQL a 95% | Experiment |

### D.2 Semaine 2 : Validation & CI

| Tache | Detail | Livrable |
|-------|--------|----------|
| Steady state | Definir les probes (HTTP 200, latence < 2s) | Probes config |
| Run experiments | Executer chaque scenario, verifier recovery | Rapport chaos |
| Auto-healing | Verifier que K8s restart les pods tues | Validation |
| Graceful degradation | ML down → backend retourne 503 propre | Test |
| CI integration | Chaos tests en pipeline nightly | GitHub Action |
| Runbook | Documentation recovery pour chaque scenario | Runbook.md |

### Fichiers a creer

```
tests/chaos/
├── pod-kill-backend.yaml       # ChaosEngine: pod-kill
├── pod-kill-ml.yaml            # ChaosEngine: pod-delete
├── network-loss-postgres.yaml  # ChaosEngine: network-loss
├── network-latency-ml.yaml     # ChaosEngine: network-latency
├── disk-fill-postgres.yaml     # ChaosEngine: disk-fill
├── steady-state-probes.yaml    # Probes definition
└── README.md                   # Guide execution
```

### Criteres de succes

| Scenario | Recovery attendu |
|----------|-----------------|
| Pod kill backend | < 30s (liveness probe + restart) |
| Pod kill ML | Fallback 503 immediat, recovery < 60s |
| Network loss PostgreSQL | Connexion pool retry, recovery < 10s |
| Network latency ML | Timeout 5s, circuit breaker trip |
| Disk fill 95% | Alerte, pas de corruption |

---

## Phase E — GDPR Compliance — 3 semaines

### Objectif
Conformite RGPD complete : export de donnees, droit a l'oubli, gestion du consentement.

### E.1 Semaine 1 : Droit d'Acces & Export

| Tache | Detail | Livrable |
|-------|--------|----------|
| Data export API | `GET /api/v1/privacy/export` → ZIP (JSON) | Endpoint |
| Collecte donnees | Aggreger : user, projets, materiaux, simulations, rapports | Query service |
| Format standard | JSON structure conforme Article 20 (portabilite) | Schema documente |
| Rate limit export | 1 export par 24h par utilisateur | Protection |
| Notification | Email avec lien de telechargement (expire 48h) | Email service |
| Audit trail | Logger chaque demande d'export | Audit event |

### E.2 Semaine 2 : Droit a l'Oubli

| Tache | Detail | Livrable |
|-------|--------|----------|
| Delete request API | `POST /api/v1/privacy/delete-account` | Endpoint |
| Soft delete | Marquer user comme `deleted_at`, anonymiser email | Migration |
| Cascade delete | Supprimer : projets, datasets, fichiers, simulations | Cascade service |
| Retention policy | Conserver audit logs 30j post-deletion (obligation legale) | Scheduler |
| Confirmation | Email de confirmation + delai 72h (annulation possible) | Workflow |
| Purge scheduler | Job CRON : supprimer definitivement apres retention | @Scheduled job |

### E.3 Semaine 3 : Consentement & Preferences

| Tache | Detail | Livrable |
|-------|--------|----------|
| Consent management | Table `user_consents` (marketing, analytics, necessary) | Migration Flyway |
| Consent API | `GET/PUT /api/v1/privacy/consents` | Endpoints |
| Cookie banner | Composant Angular (necessary only by default) | Frontend component |
| Privacy policy | Page /privacy avec politique complete | Page statique |
| Data processing register | Documentation interne traitements | Document |
| DPO contact | Endpoint /api/v1/privacy/contact | Formulaire |

### Fichiers a creer

```
rheosim-backend/
├── rheosim-domain/
│   └── privacy/
│       ├── model/UserConsent.java
│       ├── model/DeletionRequest.java
│       ├── port/PrivacyRepository.java
│       └── port/DataExportPort.java
├── rheosim-application/
│   └── privacy/
│       ├── usecase/PrivacyUseCase.java
│       └── dto/DataExportResponse.java
├── rheosim-infrastructure/
│   └── privacy/
│       ├── adapter/PrivacyController.java
│       ├── adapter/DataExportService.java
│       ├── adapter/AccountDeletionScheduler.java
│       └── entity/UserConsentEntity.java
└── resources/db/migration/
    └── V4__create_privacy_tables.sql

rheosim-frontend/
└── src/app/features/privacy/
    ├── cookie-banner.component.ts
    ├── privacy-settings.component.ts
    └── data-export.component.ts
```

### Criteres de succes

| Exigence RGPD | Implementation |
|---------------|----------------|
| Article 15 (acces) | Export ZIP en < 5min |
| Article 17 (oubli) | Suppression complete en < 72h |
| Article 20 (portabilite) | Format JSON standard |
| Article 7 (consentement) | Opt-in explicite, revocable |
| Article 30 (registre) | Documentation interne |

---

## Phase F — Secret Rotation & Vault — 2 semaines

### Objectif
Rotation automatique des secrets (JWT keys, DB passwords, API keys) sans downtime.

### F.1 Semaine 1 : HashiCorp Vault Setup

| Tache | Detail | Livrable |
|-------|--------|----------|
| Vault install | Helm chart HashiCorp Vault (HA mode) | Vault cluster |
| KV secrets engine | Stocker JWT secret, DB password, Stripe key | Secrets migres |
| Kubernetes auth | Service account → Vault policy | Auth method |
| Agent injector | Vault sidecar pour injection secrets dans pods | Injector config |
| Dynamic DB creds | Vault genere credentials PostgreSQL temporaires (TTL 1h) | DB engine |
| Transit engine | Chiffrement/dechiffrement cle gere par Vault | Transit config |

### F.2 Semaine 2 : Rotation & Integration

| Tache | Detail | Livrable |
|-------|--------|----------|
| JWT key rotation | Nouvelle cle toutes les 24h, ancienne valide 48h (grace period) | Rotation policy |
| DB password rotation | Vault rotate credentials, apps reconnectent auto | Rotation auto |
| Stripe key rotation | Webhook secret rotation via Vault | Automation |
| Spring Vault | `spring-cloud-vault` pour injection dans application.yml | Config Spring |
| External Secrets Operator | ESO sync Vault → K8s Secrets | ESO manifests |
| Monitoring | Alertes si secret expire sans rotation | Alertmanager rule |

### Fichiers a creer

```
deploy/
├── vault/
│   ├── values.yaml                 # Vault Helm values
│   ├── policies/
│   │   ├── backend-policy.hcl     # Backend read access
│   │   ├── ml-policy.hcl          # ML service access
│   │   └── admin-policy.hcl       # Admin full access
│   ├── roles/
│   │   └── k8s-auth-roles.yaml   # ServiceAccount bindings
│   └── secret-engines/
│       ├── database.hcl           # PostgreSQL dynamic creds
│       └── transit.hcl            # Encryption keys
├── external-secrets/
│   ├── cluster-secret-store.yaml  # Vault connection
│   └── external-secrets.yaml      # Secret sync definitions
└── edge/
    └── values.yaml                # +vault.enabled, vault.address
```

### Criteres de succes

| Metrique | Cible |
|----------|-------|
| Rotation JWT | Toutes les 24h, 0 downtime |
| Rotation DB | TTL 1h, reconnexion < 5s |
| Secret leak detection | Alerte immediate si secret en clair dans logs |
| Recovery time | Vault unseal < 30s apres restart |

---

## Phase G — Elasticsearch Full-Text Search — 2 semaines

### Objectif
Recherche full-text avancee sur plugins, projets, materiaux, datasets avec scoring de pertinence.

### G.1 Semaine 1 : Infrastructure & Indexation

| Tache | Detail | Livrable |
|-------|--------|----------|
| Elasticsearch deploy | Helm chart (3 nodes, 1 master) | Cluster ES |
| Index plugins | Mapping : name, description, tags, author (analyzer french+english) | Index cree |
| Index projets | Mapping : name, description, materials, status | Index cree |
| Index materiaux | Mapping : name, family, grade, model_type | Index cree |
| Kafka → ES sync | Kafka Connect (Elasticsearch Sink Connector) | Connector config |
| Reindex API | `POST /api/v1/admin/reindex` pour reconstruction complete | Admin endpoint |

### G.2 Semaine 2 : API & Frontend

| Tache | Detail | Livrable |
|-------|--------|----------|
| Search API | `GET /api/v1/search?q=...&type=...&page=...` | Endpoint unifie |
| Autocomplete | Suggest API (completion suggester) | Suggestions |
| Facets | Aggregations par type, tag, famille, status | Filtres dynamiques |
| Highlighting | Surbrillance des termes matches | Snippets |
| Frontend | Barre de recherche globale + page resultats | Composant Angular |
| Analytics | Top queries, zero-results tracking | Dashboard Kibana |

### Fichiers a creer

```
rheosim-backend/
├── rheosim-infrastructure/
│   └── search/
│       ├── config/ElasticsearchConfig.java
│       ├── adapter/SearchController.java
│       ├── adapter/ElasticsearchIndexer.java
│       ├── adapter/SearchEventListener.java  # Kafka consumer
│       └── dto/SearchResult.java

rheosim-frontend/
└── src/app/features/search/
    ├── global-search.component.ts
    └── search-results.component.ts

deploy/
├── elasticsearch/
│   └── values.yaml
└── kafka-connect/
    └── elasticsearch-sink.json
```

### Criteres de succes

| Metrique | Cible |
|----------|-------|
| Latence recherche | < 200ms P95 |
| Pertinence | Top-3 results correct > 80% |
| Indexation temps reel | < 5s apres creation/modification |
| Autocomplete | < 100ms |

---

## Planning Global

| Phase | Duree | Dependances | Priorite |
|-------|-------|-------------|----------|
| A — GPU Acceleration | 6 sem | Compute engine existant | Haute |
| B — WebAssembly Plugins | 4 sem | Marketplace existant | Haute |
| C — Service Mesh (Istio) | 3 sem | K8s deployment | Moyenne |
| D — Chaos Engineering | 2 sem | Phase C (Istio) | Moyenne |
| E — GDPR Compliance | 3 sem | Aucune | Haute |
| F — Secret Rotation (Vault) | 2 sem | K8s deployment | Moyenne |
| G — Elasticsearch Search | 2 sem | Kafka existant | Basse |

**Total** : 22 semaines (parallelisable : A+E en parallele, puis B+C, puis D+F+G)

**Chemin critique** : A (6 sem) → B (4 sem) → C (3 sem) → D (2 sem) = 15 semaines

---

## Risques

| Risque | Phase | Impact | Mitigation |
|--------|-------|--------|------------|
| Drivers GPU absents en production | A | Haut | Fallback CPU automatique |
| Wasmtime overhead inattendu | B | Moyen | Cache pre-compilation AOT |
| Istio sidecar memory overhead | C | Moyen | Resource limits, sidecar resources tuning |
| Chaos test casse la prod | D | Critique | Executer uniquement en staging |
| GDPR interpretation ambigue | E | Moyen | Consultation juridique, documentation |
| Vault single point of failure | F | Haut | HA mode (3 replicas), auto-unseal |
| Elasticsearch cluster instable | G | Moyen | 3 data nodes, replicas, monitoring |

---

## KPIs Avances

| KPI | Phase | Cible |
|-----|-------|-------|
| GPU speedup FEM 1M elements | A | > 10x |
| Plugin execution time | B | < 1ms/call |
| mTLS coverage | C | 100% |
| Recovery time pod kill | D | < 30s |
| Data export time | E | < 5min |
| Secret rotation downtime | F | 0s |
| Search latency P95 | G | < 200ms |
