# RheoSim Enterprise — Plan d'Implementation V3

## 1. Vision V3

Faire evoluer RheoSim d'une plateforme de simulation vers un **ecosysteme intelligent** integrant le Machine Learning pour l'auto-calibration, la simulation multi-physique (thermo-mecanique), un marketplace de modeles constitutifs, et une experience utilisateur enrichie avec collaboration temps reel et edge computing.

### 1.1 Objectifs Strategiques

| Objectif | Description | Mesure de succes |
|----------|-------------|-----------------|
| Intelligence | ML auto-calibration : suggestion automatique de modeles et parametres initiaux | Reduction 50% du temps d'identification |
| Multi-physique | Couplage thermo-mecanique (dilatation, dependance temperature) | Validation sur cas industriels |
| Marketplace | Plugins de modeles constitutifs uploadables par la communaute | 10+ modeles communautaires |
| Collaboration temps reel | Edition collaborative simultanee (CRDT) | 5+ editeurs simultanes sans conflit |
| Edge Computing | Calcul local pour donnees sensibles (on-premise) | Deployment air-gapped valide |
| SaaS | Offre multi-tier (Free, Pro, Enterprise) avec billing | Stripe integration |
| Mobile | Application mobile responsive pour monitoring | PWA score > 90 |

### 1.2 Perimetre V3

| Inclus | Exclu (V4+) |
|--------|-------------|
| ML auto-calibration (scikit-learn / ONNX) | RL-based inverse design |
| Thermo-mecanique couple | Electro-mecanique, piezoelectricite |
| Marketplace modeles (upload .wasm/.so) | IDE integre pour creation modeles |
| Collaboration CRDT | Video conferencing integre |
| Edge computing (Kubernetes on-premise) | IoT direct sensor integration |
| PWA mobile monitoring | Application native iOS/Android |
| Billing SaaS (Stripe) | Crypto payments |
| GPU acceleration (CUDA/OpenCL) | Quantum computing integration |
| Simulation streaming (resultats progressifs) | VR/AR visualization |

---

## 2. Architecture Cible V3

### 2.1 Vue d'Ensemble

```
                    ┌──────────────────────────────────────────────────┐
                    │              CDN + WAF (CloudFlare)              │
                    └──────────────────────┬───────────────────────────┘
                                           │
                    ┌──────────────────────▼───────────────────────────┐
                    │           API Gateway (Kong / Envoy)              │
                    │    Rate Limit, Auth, Routing, Load Balancing      │
                    └──────────────────────┬───────────────────────────┘
                                           │
      ┌────────┬───────────┬───────────────┼───────────┬──────────┬───────────┐
      │        │           │               │           │          │           │
 ┌────▼──┐ ┌──▼────┐ ┌────▼────┐ ┌────────▼───┐ ┌────▼───┐ ┌───▼───┐ ┌────▼────┐
 │Identity│ │Project│ │Experiment│ │ Simulation │ │Reporting│ │  ML   │ │Marketplace│
 │Service │ │Service│ │ Service │ │  Service   │ │ Service │ │Service│ │  Service  │
 └───┬────┘ └───┬───┘ └────┬────┘ └─────┬─────┘ └────┬───┘ └───┬───┘ └────┬─────┘
     │          │           │            │            │          │           │
     └──────────┴───────────┼────────────┴────────────┴──────────┴───────────┘
                            │
              ┌─────────────▼─────────────┐
              │       Apache Kafka         │
              │   (Event Bus + Streaming)  │
              └─────────────┬─────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
┌────────▼────────┐ ┌──────▼──────┐ ┌────────▼────────┐
│ Compute Engine  │ │  ML Engine  │ │  Thermo Engine  │
│ C++ (FEM/LM)   │ │ Python/ONNX │ │  C++ (couplage) │
│ gRPC + GPU      │ │ gRPC        │ │  gRPC           │
└─────────────────┘ └─────────────┘ └─────────────────┘
```

### 2.2 Stack Technique V3

| Composant | V2 | V3 |
|-----------|----|----|
| Compute FEM | C++ CPU (OpenMP) | C++ CPU + GPU (CUDA/OpenCL) |
| ML | - | Python (scikit-learn, PyTorch) + ONNX Runtime |
| Multi-physique | - | C++ couplage thermo-mecanique |
| API Gateway | Spring Cloud Gateway | Kong / Envoy (service mesh) |
| Service Mesh | - | Istio (mTLS, traffic management) |
| Cache | Redis standalone | Redis Cluster + CDN edge |
| Object Storage | MinIO | MinIO + CDN pour assets statiques |
| Collaboration | REST + WebSocket | CRDT (Yjs) + WebSocket |
| Billing | - | Stripe API |
| Search | - | Elasticsearch (full-text) |
| Marketplace | - | WebAssembly sandbox + OCI registry |
| Mobile | Angular SPA | Angular PWA + Service Workers |
| Deployment | K8s single cluster | K8s multi-cluster (cloud + edge) |
| GPU | - | NVIDIA GPU Operator + device plugin |
| Feature Flags | - | LaunchDarkly / Unleash |

---

## 3. Phases d'Implementation

### Phase 1 — ML Auto-Calibration Engine (8 semaines)

**Objectif** : Service ML capable de recommander le modele constitutif et les parametres initiaux a partir de donnees experimentales brutes.

#### 3.1.1 Semaines 1-3 : ML Service Setup & Training Pipeline

| Tache | Detail | Livrable |
|-------|--------|----------|
| ML Service skeleton | Python FastAPI + gRPC, Docker, CI | Service demarre |
| Dataset synthetique | Generation de 100k courbes (Maxwell, KV, Prony) avec bruit | Dataset d'entrainement |
| Feature engineering | Extraction features (pentes, inflexions, asymptotes, FFT) | Pipeline features |
| Modele classification | Random Forest / XGBoost pour prediction type de loi | Accuracy > 90% |
| Modele regression | Neural network pour estimation parametres initiaux | RMSE < 20% |
| ONNX export | Conversion modeles en ONNX pour inference rapide | Modeles .onnx |
| Tests | Validation croisee, tests sur donnees reelles | Rapport metriques |

**Architecture ML Service** :
```
rheosim-ml/
├── src/
│   ├── training/
│   │   ├── data_generator.py       # Generation donnees synthetiques
│   │   ├── feature_extractor.py    # Extraction features rheologiques
│   │   ├── model_classifier.py     # Classification type de loi
│   │   ├── param_estimator.py      # Estimation parametres initiaux
│   │   └── train_pipeline.py       # Pipeline MLflow
│   ├── inference/
│   │   ├── onnx_predictor.py       # Inference ONNX Runtime
│   │   ├── grpc_server.py          # gRPC service
│   │   └── api_server.py           # FastAPI REST fallback
│   └── proto/
│       └── ml_service.proto        # gRPC definitions
├── models/                          # Modeles entraines (.onnx)
├── tests/
├── Dockerfile
├── requirements.txt
└── pyproject.toml
```

#### 3.1.2 Semaines 4-5 : Integration avec Simulation Service

| Tache | Detail | Livrable |
|-------|--------|----------|
| gRPC client Java | Appel ML service depuis Simulation service | Client compile |
| Workflow auto-calibration | Dataset upload → ML prediction → identification raffinee | E2E test |
| Confidence scoring | Score de confiance sur chaque prediction | UI confidence badge |
| Feedback loop | Stocker resultats reels pour re-entrainement | Pipeline retrain |
| Frontend | UI suggestion modele + parametres avec confidence | Composant Angular |

#### 3.1.3 Semaines 6-8 : GPU Acceleration & Performance

| Tache | Detail | Livrable |
|-------|--------|----------|
| CUDA integration | Kernels assemblage FEM + solveur sur GPU | Speedup > 10x |
| OpenCL fallback | Support AMD/Intel GPU | Multi-vendor |
| Batch inference ML | Traitement batch pour datasets volumineux | Throughput 1000 pred/s |
| Benchmark | Comparaison CPU vs GPU sur cas 1M elements | Rapport performance |
| GPU Operator | K8s NVIDIA GPU scheduling | Pods GPU |

**Proto ML Service** :
```protobuf
service MLService {
    rpc PredictModel(PredictionRequest) returns (PredictionResponse);
    rpc EstimateParameters(EstimationRequest) returns (EstimationResponse);
    rpc RetrainModel(RetrainRequest) returns (RetrainResponse);
    rpc HealthCheck(Empty) returns (HealthResponse);
}

message PredictionRequest {
    repeated double time_points = 1;
    repeated double values = 2;
    string experiment_type = 3;
}

message PredictionResponse {
    string recommended_model = 1;
    double confidence = 2;
    repeated double initial_parameters = 3;
    repeated ModelAlternative alternatives = 4;
}

message ModelAlternative {
    string model_type = 1;
    double confidence = 2;
    repeated double initial_parameters = 3;
}
```

---

### Phase 2 — Multi-Physique Thermo-Mecanique (8 semaines)

**Objectif** : Coupler les effets thermiques (dilatation, dependance en temperature des proprietes) avec la simulation mecanique FEM 3D.

#### 3.2.1 Semaines 9-11 : Modele Thermique

| Tache | Detail | Livrable |
|-------|--------|----------|
| Equation de la chaleur | FEM thermique 3D (tetraedres P1) | Solveur thermique |
| Conditions limites | Dirichlet (T imposee), Neumann (flux), convection (Robin) | CL thermiques |
| WLF/Arrhenius | Dependance temperature des temps de relaxation | Modeles TTS |
| Shift factor | a_T(T) = exp(C1*(T-Tref)/(C2+T-Tref)) (WLF) | Implementation validee |
| Tests | Cas analytiques : barre chauffee, cylindre en refroidissement | R² > 0.99 |

**Formulation couplage** :
```
Thermique :    C * dT/dt + K_th * T = Q + Q_dissipation
Mecanique :    K(T) * u = F_ext + F_thermal + F_history

Couplage faible (staggered) :
  1. Resoudre thermique → T(t+dt)
  2. Mettre a jour proprietes mecaniques avec T
  3. Resoudre mecanique → u(t+dt)
  4. Calculer dissipation visqueuse → Q_dissipation
  5. Iterer si couplage fort necessaire
```

#### 3.2.2 Semaines 12-13 : Couplage Thermo-Mecanique

| Tache | Detail | Livrable |
|-------|--------|----------|
| Assemblage couple | Matrice thermique + mecanique unifiee ou staggered | Solveur couple |
| Dilatation thermique | epsilon_th = alpha * (T - T_ref) | Force thermique |
| Dissipation visqueuse | Q = sigma : d(epsilon)/dt | Source chaleur |
| Time-Temperature Superposition | Shift temporel des spectres de Prony | TTS implementation |
| Integration temporelle | Schema implicite couple (BDF2) | Stabilite |

#### 3.2.3 Semaines 14-16 : Validation & Frontend

| Tache | Detail | Livrable |
|-------|--------|----------|
| Cas validation | Poutre en dilatation libre, trempe cylindre | Tests passes |
| Gradient thermique | Visualisation champ de temperature 3D | Three.js thermal map |
| Contraintes residuelles | Simulation refroidissement + contraintes figees | Cas industriel |
| Frontend | Dual color map (T + sigma), animation temporelle | Composant Angular |
| Export | Format VTK multi-champ (u, T, sigma, epsilon) | Export valide |
| Documentation | Guide utilisateur multi-physique | Doc complete |

---

### Phase 3 — Marketplace & Plugin Architecture (6 semaines)

**Objectif** : Permettre a la communaute d'uploader et partager des modeles constitutifs sous forme de plugins (WebAssembly ou shared libraries).

#### 3.3.1 Semaines 17-19 : Infrastructure Plugin

| Tache | Detail | Livrable |
|-------|--------|----------|
| Plugin SDK | API C/Rust pour creer un modele constitutif | SDK publie |
| WebAssembly runtime | Wasmtime sandbox pour execution securisee | Runtime OK |
| Plugin registry | OCI-based registry pour stocker les plugins | Registry operationnel |
| Validation | Tests automatiques du plugin (interface, bornes, convergence) | CI plugin |
| Isolation securite | Sandbox CPU/memory limits, pas d'acces filesystem | Securite validee |

**Interface Plugin (C ABI)** :
```c
// plugin_api.h — Interface que chaque plugin doit implementer
typedef struct {
    const char* name;
    const char* version;
    const char* author;
    const char* description;
    int parameter_count;
    const char** parameter_names;
} PluginMetadata;

// Fonctions exportees par le plugin
extern PluginMetadata plugin_get_metadata();
extern double plugin_relaxation_modulus(double t, const double* params, int n_params);
extern double plugin_storage_modulus(double omega, const double* params, int n_params);
extern double plugin_loss_modulus(double omega, const double* params, int n_params);
extern double plugin_creep_compliance(double t, const double* params, int n_params);
extern void plugin_get_default_bounds(double* lower, double* upper, int n_params);
```

#### 3.3.2 Semaines 20-22 : Marketplace Frontend & Backend

| Tache | Detail | Livrable |
|-------|--------|----------|
| Marketplace Service | CRUD modeles, versioning, reviews, downloads | API REST |
| Upload pipeline | Upload .wasm → validation → scan securite → publish | Pipeline CI |
| Search & Discovery | Elasticsearch full-text sur noms, descriptions, tags | Recherche |
| Rating & Reviews | Notation et commentaires par les utilisateurs | UI reviews |
| Frontend | Catalogue, page detail, upload wizard | Pages Angular |
| Licensing | Support licences (MIT, Apache, Commercial) | Metadata |

**Modele de donnees Marketplace** :
```sql
CREATE TABLE marketplace_plugins (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) UNIQUE NOT NULL,
    description TEXT,
    author_id UUID REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    license VARCHAR(50),
    tags TEXT[],
    downloads_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE plugin_versions (
    id UUID PRIMARY KEY,
    plugin_id UUID REFERENCES marketplace_plugins(id),
    version VARCHAR(50) NOT NULL,
    artifact_url VARCHAR(1000) NOT NULL,
    artifact_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    changelog TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(plugin_id, version)
);

CREATE TABLE plugin_reviews (
    id UUID PRIMARY KEY,
    plugin_id UUID REFERENCES marketplace_plugins(id),
    user_id UUID REFERENCES users(id),
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

### Phase 4 — Collaboration Temps Reel & SaaS (6 semaines)

**Objectif** : Collaboration simultanee sur les projets (edition CRDT) et monetisation via offre SaaS multi-tier.

#### 3.4.1 Semaines 23-25 : CRDT & Real-Time Collaboration

| Tache | Detail | Livrable |
|-------|--------|----------|
| Yjs integration | CRDT backend (y-websocket provider) | Serveur collaboration |
| Awareness | Curseurs et presence des autres editeurs | UI presence |
| Conflit resolution | Merge automatique des modifications simultanees | Tests 5 editeurs |
| Undo/Redo | Historique par utilisateur (non global) | UX validee |
| Operational Transform | Fallback pour cas non-CRDT (parametres numeriques) | OT implementation |
| Permissions | CRDT respecte les roles (VIEWER ne peut pas editer) | Tests permissions |

#### 3.4.2 Semaines 26-28 : SaaS & Billing

| Tache | Detail | Livrable |
|-------|--------|----------|
| Stripe integration | Subscriptions, invoices, payment methods | Billing API |
| Tier management | Free (1 projet, 5 sims/mois), Pro (illimite), Enterprise (on-premise) | Plans definis |
| Usage metering | Comptage simulations, storage, compute time | Metriques usage |
| Quotas enforcement | Limites par tier (rejeter si quota depasse) | Middleware quotas |
| Admin dashboard | Gestion clients, revenus, usage analytics | Dashboard admin |
| Onboarding | Wizard premier projet, trial 14 jours Pro | UX onboarding |

**Plans SaaS** :

| Feature | Free | Pro | Enterprise |
|---------|------|-----|-----------|
| Projets | 1 | Illimite | Illimite |
| Simulations/mois | 5 | Illimite | Illimite |
| Storage | 100 Mo | 10 Go | Illimite |
| FEM 3D | - | ✓ | ✓ |
| GPU compute | - | ✓ | ✓ |
| ML auto-calibration | - | ✓ | ✓ |
| Collaboration | 2 users | 10 users | Illimite |
| Marketplace upload | - | ✓ | ✓ |
| SLA | - | 99.5% | 99.9% |
| Support | Community | Email | Dedie |
| On-premise | - | - | ✓ |
| Prix | 0€ | 49€/mois | Sur devis |

---

### Phase 5 — Edge Computing & PWA (4 semaines)

**Objectif** : Deployer RheoSim en mode edge (on-premise) pour les clients entreprise et fournir une PWA pour le monitoring mobile.

#### 3.5.1 Semaines 29-30 : Edge Deployment

| Tache | Detail | Livrable |
|-------|--------|----------|
| K3s packaging | Version legere K8s pour deployment on-premise | Helm chart edge |
| Air-gapped install | Bundle OCI images, install sans internet | Script install offline |
| Data sovereignty | Toutes les donnees restent on-premise | Audit compliance |
| Sync selective | Sync marketplace plugins (download only) | Sync agent |
| License server | Validation licence offline (RSA signed token) | License manager |
| Monitoring local | Prometheus + Grafana embedded | Stack locale |

#### 3.5.2 Semaines 31-32 : PWA & Mobile

| Tache | Detail | Livrable |
|-------|--------|----------|
| Service Worker | Cache offline, push notifications | SW enregistre |
| App Shell | Skeleton UI pour chargement instantane | First paint < 1s |
| Responsive redesign | Adaptation tablet + mobile (cards, drawer) | Breakpoints OK |
| Push notifications | Web Push API pour alertes simulation | Notifications browser |
| Install prompt | Banner "Ajouter a l'ecran d'accueil" | A2HS |
| Lighthouse audit | Score PWA > 90, Performance > 80, A11y > 90 | Rapport Lighthouse |

---

### Phase 6 — Qualite, Performance & GA (4 semaines)

**Objectif** : Hardening final avant release General Availability.

#### 3.6.1 Semaines 33-34 : Security & Compliance

| Tache | Detail | Livrable |
|-------|--------|----------|
| Penetration testing | OWASP ZAP + Burp Suite, rapport vulnerabilites | 0 critical/high |
| Dependency scanning | Snyk/Dependabot sur Java, C++, Python, npm | 0 vulns critiques |
| SOC 2 preparation | Policies, access controls, audit logging | Checklist SOC 2 |
| GDPR compliance | Data export, right to deletion, consent management | GDPR features |
| mTLS inter-service | Istio mutual TLS entre tous les pods | Zero-trust network |
| Secret rotation | Automatic key rotation (Vault/External Secrets) | Rotation policy |

#### 3.6.2 Semaines 35-36 : Performance & Release

| Tache | Detail | Livrable |
|-------|--------|----------|
| Load test final | k6 : 500 users simultanes, 1000 req/s | Rapport perf |
| Chaos engineering | Litmus : pod kill, network partition, disk full | Resilience validee |
| GPU benchmark | FEM 1M elements sur GPU, < 2 min | Benchmark publie |
| Documentation finale | API reference, user guide, admin guide | Docs completes |
| Migration V2→V3 | Script migration automatique, zero-downtime | Runbook migration |
| Release GA | Tag v3.0.0, changelog, annonce | Release publiee |

---

## 4. ML Engine — Design Detaille

### 4.1 Pipeline d'Entrainement

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Data Generator  │────→│ Feature Extractor │────→│  Model Training │
│ (100k courbes)  │     │ (slopes, FFT,    │     │  (XGBoost, NN)  │
│                 │     │  inflections)     │     │                 │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                           │
                                                  ┌────────▼────────┐
                                                  │  ONNX Export    │
                                                  │  + MLflow Track │
                                                  └────────┬────────┘
                                                           │
                                                  ┌────────▼────────┐
                                                  │  Model Registry │
                                                  │  (versioned)    │
                                                  └─────────────────┘
```

### 4.2 Features Rheologiques Extraites

| Feature | Description | Utilite |
|---------|-------------|---------|
| slope_initial | Pente initiale de la courbe | Distingue Maxwell vs Prony |
| slope_terminal | Pente a temps long | Identifie G_infinity |
| inflection_count | Nombre de points d'inflexion | Nombre de branches Prony |
| relaxation_time_approx | Temps au 1/e du max | Estimation tau dominant |
| frequency_peak | Position du pic de G'' | tau principal |
| plateau_modulus | Valeur asymptotique | G_inf ou G_0 |
| spectral_width | Largeur du spectre FFT | Distribution des tau |
| noise_level | Ecart-type des residus lisse | Qualite des donnees |

### 4.3 Feedback Loop

```
User soumet données → ML prediction → Identification LM → Résultat final
                                                              │
                                                              ▼
                                                    Stockage (features, true_model, true_params)
                                                              │
                                                              ▼ (batch hebdomadaire)
                                                    Re-entrainement modele
                                                              │
                                                              ▼
                                                    Nouveau modele ONNX deploye
```

---

## 5. GPU Acceleration — Design

### 5.1 Strategie

| Operation | CPU (actuel) | GPU (V3) | Speedup attendu |
|-----------|-------------|----------|-----------------|
| Assemblage FEM | OpenMP threads | CUDA kernels par element | 10-20x |
| Solveur lineaire | Eigen SparseLU | cuSPARSE + cuSOLVER | 5-50x (selon taille) |
| LM Jacobien | Sequentiel | Parallel evaluations GPU | 10x |
| ML inference | ONNX CPU | ONNX GPU (TensorRT) | 5x |

### 5.2 Architecture GPU

```
┌─────────────────────────────────────────┐
│           Compute Engine C++            │
├─────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────────┐ │
│  │ CPU Path    │  │   GPU Path       │ │
│  │ (OpenMP)    │  │   (CUDA)         │ │
│  │             │  │                  │ │
│  │ Assemblage  │  │ cu_assemble()    │ │
│  │ SparseLU    │  │ cuSolverSp      │ │
│  │ LM iterate  │  │ cu_jacobian()   │ │
│  └─────────────┘  └──────────────────┘ │
│                                         │
│  Runtime selection based on:            │
│  - Problem size (< 10k → CPU)           │
│  - GPU availability                     │
│  - User tier (Free → CPU only)          │
└─────────────────────────────────────────┘
```

---

## 6. Thermo-Mecanique — Formulation

### 6.1 Equation de la Chaleur

```
ρ * Cp * ∂T/∂t = ∇·(k·∇T) + Q_ext + Q_dissipation

Q_dissipation = σ : ε_dot  (dissipation visqueuse)
```

Discretisation FEM : memes tetraedres P1, 1 DDL temperature par noeud.

### 6.2 Time-Temperature Superposition (TTS)

Le principe de superposition temps-temperature stipule que les proprietes viscoelastiques a une temperature T sont equivalentes a celles a la temperature de reference T_ref, avec un decalage temporel :

```
G(t, T) = G(t / a_T, T_ref)

WLF : log(a_T) = -C1 * (T - T_ref) / (C2 + T - T_ref)
Arrhenius : log(a_T) = Ea / R * (1/T - 1/T_ref)
```

### 6.3 Couplage Staggered

```
Pour chaque pas de temps dt :
    1. Resoudre thermique :
       (C/dt + K_th) * T^{n+1} = C/dt * T^n + Q^n
       
    2. Calculer shift factor a_T(T^{n+1}) pour chaque element
    
    3. Mettre a jour les temps de relaxation effectifs :
       tau_i_eff = tau_i * a_T
       
    4. Resoudre mecanique avec proprietes mises a jour :
       K(T^{n+1}) * u^{n+1} = F_ext + F_thermal + F_history
       
    5. Calculer dissipation : Q_diss = sigma : d(epsilon)/dt
    
    6. (Optionnel) Iterer 2-5 si couplage fort
```

---

## 7. CRDT Collaboration — Design

### 7.1 Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Client A   │     │ Y-WebSocket │     │  Client B   │
│  (Yjs doc)  │────→│   Server    │←────│  (Yjs doc)  │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
                    ┌──────▼──────┐
                    │ Persistence │
                    │  (Redis)    │
                    └─────────────┘
```

### 7.2 Documents Collaboratifs

| Document | Type CRDT | Granularite |
|----------|-----------|-------------|
| Projet (metadata) | Y.Map | Champ par champ |
| Materiau (parametres) | Y.Map | Parametre individuel |
| Notes/commentaires | Y.Text | Caractere par caractere |
| Liste materiaux | Y.Array | Element par element |
| Configuration simulation | Y.Map | Champ par champ |

### 7.3 Awareness Protocol

Chaque client envoie sa presence :
```json
{
  "user": { "id": "uuid", "name": "Pierre", "color": "#ff6b6b" },
  "cursor": { "field": "material.G", "position": 5 },
  "selection": null,
  "lastActive": "2026-06-01T10:30:00Z"
}
```

---

## 8. Planning & Jalons

| Semaine | Milestone | Livrable |
|---------|-----------|----------|
| S3 | ML Service MVP | Classification modele + estimation params |
| S5 | ML Integration | Auto-calibration E2E dans Simulation service |
| S8 | GPU v1 | CUDA assemblage FEM, benchmark |
| S11 | Thermique standalone | Solveur thermique 3D valide |
| S13 | Couplage v1 | Thermo-mecanique staggered |
| S16 | Multi-physique complet | Validation cas industriels |
| S19 | Plugin SDK | SDK + runtime WebAssembly |
| S22 | Marketplace live | Upload, search, install plugins |
| S25 | CRDT collaboration | Edition simultanee 5 users |
| S28 | SaaS billing | Stripe subscriptions live |
| S30 | Edge v1 | K3s air-gapped deployment |
| S32 | PWA | Mobile monitoring, push notifications |
| S34 | Security hardened | Pentest clean, mTLS, secret rotation |
| S36 | GA Release | v3.0.0 tag, docs, migration guide |

---

## 9. Risques et Mitigations

| Risque | Impact | Probabilite | Mitigation |
|--------|--------|-------------|-----------|
| GPU driver compatibility | Haut | Moyen | OpenCL fallback + CPU fallback |
| ML prediction accuracy insuffisante | Moyen | Moyen | Feedback loop + human-in-the-loop validation |
| CRDT merge conflicts sur donnees numeriques | Moyen | Haut | OT fallback pour valeurs atomiques + lock optimiste |
| WebAssembly sandbox escape | Critique | Bas | Wasmtime + seccomp + resource limits |
| Stripe PCI compliance | Haut | Bas | Stripe Elements (pas de card data cote serveur) |
| Performance GPU < attentes | Moyen | Moyen | Threshold auto CPU/GPU selon taille probleme |
| Edge deployment complexity | Moyen | Haut | K3s + script install tout-en-un + support dedie |
| GDPR data deletion complexe | Moyen | Moyen | Soft delete + purge scheduler + cascade events |

---

## 10. KPIs V3

| KPI | Cible | Mesure |
|-----|-------|--------|
| Temps moyen identification (avec ML) | < 50% du V2 | Timer Prometheus |
| FEM 1M elements (GPU) | < 2 min | Benchmark CI |
| Utilisateurs simultanes | 500+ | k6 load test |
| PWA Lighthouse score | > 90 | CI Lighthouse |
| Marketplace plugins | 10+ communaute | Compteur |
| SaaS MRR (6 mois post-launch) | 5000€+ | Stripe dashboard |
| Uptime SLA Pro | 99.5% | UptimeRobot |
| Uptime SLA Enterprise | 99.9% | UptimeRobot |
| Pentest findings | 0 critical, 0 high | Rapport ZAP |
| CRDT latence sync | < 100ms | Metriques WebSocket |
