# RheoSim Enterprise — Documentation Pedagogique

## Introduction

Ce document est un guide d'apprentissage pour les developpeurs rejoignant le projet RheoSim Enterprise. Il explique les concepts architecturaux, les patterns utilises, les fondements scientifiques, et les nouveaux composants V2 (C++ FEM, microservices, collaboration).

---

## 1. Architecture Hexagonale — Comprendre le Pourquoi

### 1.1 Le Probleme

Dans une application classique en couches (Controller → Service → Repository), le code metier est **contamine** par les frameworks :

```java
// Mauvais : le metier depend de Spring, JPA, etc.
@Service
public class SimulationService {
    @Autowired
    private SimulationJobJpaRepository repo; // Dependance infrastructure
    
    @Transactional // Framework dans le metier
    public void submit(SimulationJob job) { ... }
}
```

Problemes :
- Impossible de tester le metier sans Spring
- Changement de base de donnees = reecriture du metier
- Le domaine est noye dans la plomberie technique

### 1.2 La Solution : Hexagonal (Ports & Adapters)

```
            ┌─────────────────────────────┐
            │         DOMAIN              │
            │   (Entites, Regles metier)  │
            │                             │
            │    ┌─── PORTS ───┐          │
            │    │ (interfaces)│          │
            └────┼─────────────┼──────────┘
                 │             │
    ┌────────────┼─────┐  ┌───┼────────────┐
    │ ADAPTERS         │  │ ADAPTERS       │
    │ (Infrastructure) │  │ (REST/Kafka)   │
    │ JPA, gRPC, File  │  │ Controllers    │
    └──────────────────┘  └────────────────┘
```

**Principe** : Le domaine definit des **ports** (interfaces). L'infrastructure fournit des **adapters** (implementations).

### 1.3 Exemple Concret dans RheoSim

```
Domain (Port) :
  interface ParameterIdentificationPort {
      SimulationResult identify(ConstitutiveLaw law, double[] xData, ...);
  }

Infrastructure (Adapter V1 — Java) :
  class LevenbergMarquardtIdentifier implements ParameterIdentificationPort {
      // Apache Commons Math implementation
  }

Infrastructure (Adapter V2 — Routing) :
  @Primary
  class ComputeEngineRoutingAdapter implements ParameterIdentificationPort {
      // Si C++ dispo → gRPC call
      // Sinon → fallback Java local
  }
```

**Benefice** : Le domaine ne sait pas s'il y a un moteur Java ou C++ derriere. On peut ajouter un GPU engine demain sans toucher au metier.

### 1.4 Regle d'Or

> **Les dependances pointent toujours vers le centre (domain).**

- Domain → rien
- Application → Domain
- Infrastructure → Domain + Application

Jamais l'inverse. C'est verifie automatiquement par ArchUnit.

---

## 2. Domain-Driven Design (DDD) — Concepts Cles

### 2.1 Bounded Context

Un bounded context est un perimetre fonctionnel autonome avec son propre vocabulaire :

| Context | Vocabulaire propre |
|---------|-------------------|
| Identity | User, Role, Token, Authentication |
| Project | Project, Material, MaterialModel |
| Experiment | Dataset, DataColumn, ExperimentType |
| Simulation | SimulationJob, ConstitutiveLaw, FitTarget |
| Reporting | Report, DublinCoreMetadata, ReportFormat |
| Collaboration (V2) | Organization, Member, Invitation, ProjectRole, AuditEvent |

**Regle** : Un `User` dans Identity n'est pas le meme objet qu'un `ownerId` dans Project. Chaque contexte a sa propre representation.

### 2.2 Entite vs Value Object

```java
// ENTITE : a une identite (UUID), mutable, cycle de vie
public class SimulationJob {
    private UUID id;          // Identite
    private JobStatus status; // Etat mutable
    public void start() { this.status = RUNNING; }
}

// VALUE OBJECT : pas d'identite, immutable, compare par valeur (record Java)
public record Organization(
    UUID id,
    String name,
    String slug,
    UUID ownerUserId,
    Instant createdAt,
    Instant updatedAt
) {
    public static Organization create(String name, String slug, UUID ownerUserId) {
        return new Organization(UUID.randomUUID(), name, slug, ownerUserId, Instant.now(), Instant.now());
    }
}
```

### 2.3 Domain Events

Un evenement signale que quelque chose d'important s'est passe :

```java
public class DatasetUploadedEvent extends DomainEvent {
    private final UUID datasetId;
    private final UUID projectId;
}
```

Les events sont publies sur Kafka pour decoupler les contextes. En V2, ils declenchent aussi des notifications WebSocket.

### 2.4 Use Case (Application Layer)

Un use case orchestre un scenario metier sans contenir de logique :

```java
public class OrganizationUseCase {
    private final OrganizationRepository organizationRepository;     // Port
    private final OrganizationMemberRepository memberRepository;     // Port
    private final AuditEventRepository auditRepository;              // Port
    
    public Organization createOrganization(String name, String slug, UUID ownerUserId) {
        Organization org = Organization.create(name, slug, ownerUserId);
        org = organizationRepository.save(org);
        memberRepository.save(OrganizationMember.create(org.id(), ownerUserId, OrganizationRole.OWNER));
        auditRepository.save(AuditEvent.create(org.id(), ownerUserId, "CREATE", "ORGANIZATION", org.id(), "..."));
        return org;
    }
}
```

---

## 3. Patterns de Conception Utilises

### 3.1 Strategy Pattern — Lois Constitutives

**Probleme** : Plusieurs modeles rheologiques avec la meme interface mais des equations differentes.

**Solution** (Java et C++) :

```java
// Interface commune (port)
public interface ConstitutiveLaw {
    double computeRelaxationModulus(double time, double[] params);
    double computeStorageModulus(double omega, double[] params);
}

// Implementation (strategy)
public class MaxwellLaw implements ConstitutiveLaw {
    public double computeRelaxationModulus(double t, double[] p) {
        return p[0] * Math.exp(-t / p[1]); // G * exp(-t/tau)
    }
}
```

```cpp
// C++ equivalent
class ConstitutiveLaw {
public:
    virtual double compute_relaxation_modulus(double t, const std::vector<double>& params) = 0;
    virtual ~ConstitutiveLaw() = default;
};

class MaxwellLaw : public ConstitutiveLaw {
    double compute_relaxation_modulus(double t, const std::vector<double>& p) override {
        return p[0] * std::exp(-t / p[1]);
    }
};
```

**Ou c'est utilise** : Compute engine (Java + C++), parsers de donnees (CSV/Excel), generateurs de rapports (PDF/CSV/JSON).

### 3.2 Adapter Pattern — Routing gRPC (V2)

**Probleme** : Le systeme doit pouvoir utiliser un moteur de calcul C++ distant via gRPC, mais aussi fonctionner sans (fallback Java).

**Solution** :

```java
@Component
@Primary
public class ComputeEngineRoutingAdapter implements ParameterIdentificationPort {
    private final ComputeEngineClient grpcClient;
    private final LevenbergMarquardtIdentifier javaIdentifier;

    public SimulationResult identify(...) {
        if (grpcClient.isAvailable()) {
            try {
                return grpcClient.identify(...);
            } catch (Exception e) {
                log.warn("C++ failed, fallback Java");
            }
        }
        return javaIdentifier.identify(...);
    }
}
```

**Lecon** : `@Primary` resout le conflit Spring quand deux beans implementent la meme interface.

### 3.3 Builder Pattern — Entites Complexes

```java
public class Material {
    private Material() {}
    
    public static MaterialBuilder builder() { return new MaterialBuilder(); }
    
    public static class MaterialBuilder {
        private String name;
        private MaterialFamily family;
        
        public Material build() {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("name is required");
            Material m = new Material();
            m.id = UUID.randomUUID();
            m.name = name;
            m.family = family;
            return m;
        }
    }
}
```

**Note** : Lombok etant retire (incompatible Java 25), les builders sont ecrits manuellement. Les value objects simples utilisent des records Java.

### 3.4 Observer Pattern — Events + Kafka + WebSocket (V2)

```
[SimulationJob.complete()]
    → SimulationJobCompletedEvent
    → Kafka topic "simulation.completed"
    → NotificationService.notifySimulationCompleted()
    → WebSocket /user/{userId}/queue/notifications
    → Frontend signal update
```

### 3.5 Template Method — Validation de Donnees

```java
public List<ValidationError> validate(ExperimentType type, List<DataColumn> cols, List<double[]> rows) {
    List<ValidationError> errors = new ArrayList<>();
    if (rows.size() < 3) errors.add(error("Minimum 3 points"));
    errors.addAll(validateRequiredColumns(type, cols));      // specifique
    errors.addAll(validateDataQuality(cols, rows));          // commun
    return errors;
}
```

---

## 4. Fondements Scientifiques

### 4.1 Viscoelasticite — Concepts de Base

Un materiau **viscoelastique** combine :
- **Elasticite** (ressort) : deformation proportionnelle a la contrainte, instantanee, reversible
- **Viscosite** (amortisseur) : deformation proportionnelle a la vitesse de contrainte, dependante du temps

#### Modele de Maxwell (ressort + amortisseur en serie)

```
    ┌───[G]───[η]───┐
    │    ressort  piston │
    └───────────────────┘
```

- **Relaxation** : G(t) = G * exp(-t/tau) avec tau = eta/G
- **Interpretation** : Le materiau relaxe sa contrainte exponentiellement

#### Modele de Kelvin-Voigt (ressort + amortisseur en parallele)

```
    ┌───[G]───┐
    ├───[η]───┤
    └─────────┘
```

- **Fluage** : J(t) = (1/G) * (1 - exp(-t/tau))
- **Interpretation** : Le materiau se deforme progressivement vers un equilibre

#### Serie de Prony (N elements de Maxwell en parallele)

```
    ┌───[G₁]───[η₁]───┐
    ├───[G₂]───[η₂]───┤
    ├───[G∞]───────────┤  (equilibre)
    └──────────────────┘
```

- **Relaxation** : G(t) = G_inf + Σ G_i * exp(-t/tau_i)
- **Interpretation** : Spectre de temps de relaxation (plusieurs mecanismes)

### 4.2 Domaine Frequentiel

En sollicitation oscillatoire (omega = pulsation) :

- **Module de conservation** G'(omega) : energie stockee (elastique)
- **Module de perte** G''(omega) : energie dissipee (visqueuse)
- **Viscosite complexe** |eta*| = sqrt(G'² + G''²) / omega

Pour Maxwell :
```
G'(omega)  = G * (omega*tau)² / (1 + (omega*tau)²)
G''(omega) = G * (omega*tau)  / (1 + (omega*tau)²)
```

### 4.3 Identification Parametrique (Levenberg-Marquardt)

**Objectif** : Trouver les parametres qui ajustent au mieux les donnees experimentales.

**Principe** : minimiser S(p) = Σ [y_exp(i) - y_model(x_i, p)]²

1. Partir d'une estimation initiale p₀
2. Calculer le Jacobien J (derivees partielles)
3. Resoudre : delta_p = (J^T * J + lambda * I)^(-1) * J^T * r
   - lambda grand → pas de gradient (prudent)
   - lambda petit → pas de Gauss-Newton (rapide)
4. Ajuster lambda selon le progres
5. Repeter jusqu'a convergence (tolerance 10⁻¹²)

**R²** = 1 - SS_res / SS_tot (1 = parfait, 0 = inutile)

### 4.4 Methode des Elements Finis 3D (V2)

#### Principe

Resoudre l'equation d'equilibre K * u = F sur un maillage tetraedrique :
- K = matrice de rigidite globale (assemblee a partir des elements)
- u = vecteur de deplacements inconnus
- F = vecteur des forces externes

#### Element Tetraedre P1

Un tetraedre a 4 noeuds avec interpolation lineaire. Chaque noeud a 3 DDL (ux, uy, uz), donc l'element a 12 DDL.

```
Matrice de deformation B (6×12) : relie deformations aux deplacements
Matrice de comportement D (6×6) : relie contraintes aux deformations
Matrice de rigidite elementaire : Ke = V * B^T * D * B
```

#### Viscoelasticite en FEM

Pour un modele de Maxwell generalise avec schema d'Euler implicite :

```
D_eff(dt) = D_elastic * (dt / (dt + tau))
```

Le module effectif depend du pas de temps dt. Pour une serie de Prony :
```
D_eff = D_inf + Σ D_i * (dt / (dt + tau_i))
```

#### Assemblage (OpenMP)

```cpp
#pragma omp parallel
{
    std::vector<Triplet> local_triplets;
    #pragma omp for
    for (int e = 0; e < num_elements; ++e) {
        Matrix12d Ke = compute_element_stiffness(e);
        // Ajouter aux triplets locaux
    }
    #pragma omp critical
    global_triplets.insert(end, local_triplets.begin(), local_triplets.end());
}
K.setFromTriplets(global_triplets.begin(), global_triplets.end());
```

#### Conditions Limites (Methode de Penalite)

```cpp
double penalty = 1e20;
for (auto dof : fixed_dofs) {
    K.coeffRef(dof, dof) += penalty;
    F(dof) = penalty * prescribed_value;
}
```

Simple a implementer et fonctionne bien pour les cas ou les conditions sont strictes.

---

## 5. Microservices & Communication (V2)

### 5.1 Pourquoi les Microservices

Le monolithe modulaire V1 est limite :
- Un seul deployable → un changement de l'auth redeploy tout
- Scaling uniforme → le compute ne peut pas scaler independamment
- Technologie unique → pas de C++ pour le FEM

### 5.2 Communication gRPC

**Avantages par rapport a REST** :
- Protobuf binaire : 5-10x plus compact que JSON
- Streaming bidirectionnel : progression simulation
- Typage fort : contrat `.proto` partage
- HTTP/2 : multiplexage, compression headers

```protobuf
service ComputeEngine {
    rpc Identify(IdentificationRequest) returns (IdentificationResponse);
    rpc SimulateFEM(FEMRequest) returns (stream FEMProgress);
    rpc HealthCheck(Empty) returns (HealthResponse);
}
```

### 5.3 Pattern Routing + Fallback

Le `ComputeEngineRoutingAdapter` illustre un pattern resilience :
1. Tester la disponibilite (health check)
2. Tenter l'appel principal (C++)
3. En cas d'echec, fallback sur implementation locale (Java)
4. Logger le fallback pour monitoring

Ce pattern evite un single point of failure sur le compute engine.

### 5.4 Event-Driven avec Kafka

Chaque service publie des events pour informer les autres :
- `simulation.completed` → Notification service → WebSocket
- `dataset.uploaded` → Validation async
- `invitation.sent` → Email service (futur)

Les events sont des faits immuables — "il s'est passe X" plutot que "fais Y".

---

## 6. Observabilite (V2)

### 6.1 Les 3 Piliers

| Pilier | Outil | Question |
|--------|-------|----------|
| **Metriques** | Prometheus + Grafana | "Combien ?" (latence, throughput, erreurs) |
| **Logs** | Loki | "Que s'est-il passe ?" (details textuels) |
| **Traces** | Jaeger + OTel | "Ou est le temps passe ?" (chemin d'une requete) |

### 6.2 Tracing Distribue

Une requete traverse plusieurs services :
```
Frontend → Ingress → Identity (auth) → Simulation → Compute Engine (gRPC) → PostgreSQL
```

OpenTelemetry genere un `traceId` unique qui suit la requete a travers tous les services. Chaque etape est un `span` avec duree et metadata.

**Configuration Spring Boot** :
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

### 6.3 Alerting

Les regles Prometheus declenchent des alertes :
```yaml
- alert: HighLatencyP95
  expr: histogram_quantile(0.95, ...) > 2
  for: 5m
  labels:
    severity: warning
```

Alertmanager route vers Slack (warning) ou PagerDuty (critical).

---

## 7. Guide du Developpeur

### 7.1 Ajouter un Nouveau Modele Constitutif

**Java** :
1. Implementer `ConstitutiveLaw` dans `infrastructure/.../engine/`
2. Ajouter dans l'enum `ConstitutiveModelType` (domain)
3. Enregistrer dans le processor : mapping type → implementation
4. Ecrire des tests : solutions analytiques connues

**C++** :
1. Creer header dans `include/rheosim/` heritant de `ConstitutiveLaw`
2. Implementer dans `src/`
3. Ajouter le mapping dans `grpc_server.cpp`
4. Tests GoogleTest avec cas analytiques

### 7.2 Ajouter un Endpoint API

1. Definir le DTO dans `rheosim-application` (record Java)
2. Creer/modifier le Use Case dans `rheosim-application`
3. Creer le Controller dans `rheosim-infrastructure`
4. Documenter : annotations OpenAPI (@Operation, @ApiResponse)
5. Tester : test unitaire du use case + test du controller

### 7.3 Ajouter un Composant Frontend

1. Creer un composant standalone dans `features/`
2. Utiliser signals pour l'etat local
3. Injecter le service HTTP correspondant
4. Ajouter la route lazy-loaded dans `app.routes.ts`
5. Pour la 3D : utiliser `MeshViewerComponent` comme base

### 7.4 Conventions de Code

| Element | Convention |
|---------|-----------|
| Entites domain | Classes mutables avec Builder manuel |
| Value Objects | Records Java (immutables) |
| DTOs | Records Java |
| Ports | Interfaces dans `domain/.../port/` |
| Adapters | Classes dans `infrastructure/` |
| Controllers | Dans `infrastructure/` (c'est un adapter REST) |
| Tests | `*Test.java`, @DisplayName descriptif |
| Packages | `com.rheosim.{layer}.{context}.{type}` |
| C++ headers | `include/rheosim/` avec guards |
| C++ sources | `src/` avec fichier par classe |

### 7.5 Commandes Utiles

```bash
# Backend
cd rheosim-backend
mvn compile                           # Compiler
mvn test                              # Lancer les tests
mvn verify                            # Tests + verifications
mvn spring-boot:run -pl rheosim-bootstrap  # Demarrer l'app

# Frontend
cd rheosim-frontend
npm start                             # Dev server (localhost:4200)
npm run build                         # Build production
npm test                              # Tests unitaires

# C++ Compute Engine
cd rheosim-compute
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . -j$(nproc)
ctest --output-on-failure             # Run tests

# Docker
docker compose -f docker/docker-compose.prod.yml up --build
docker compose -f deploy/monitoring/docker-compose.monitoring.yml up -d

# Kubernetes
helm install rheosim deploy/helm/rheosim -f deploy/helm/rheosim/values-dev.yaml
helm upgrade rheosim deploy/helm/rheosim -f deploy/helm/rheosim/values-prod.yaml

# Load testing
k6 run tests/load/k6-simulation-load.js
```

---

## 8. Exercices Pratiques

### Exercice 1 : Comprendre l'architecture

1. Ouvrir `HexagonalArchitectureTest.java` et lire les 5 regles
2. Essayer d'ajouter `import org.springframework...` dans une classe domain
3. Lancer `mvn test` et observer l'echec ArchUnit
4. Retirer l'import et verifier que les tests passent

### Exercice 2 : Ajouter un modele constitutif

1. Implementer le modele de Burgers (Maxwell + Kelvin-Voigt en serie)
   - J(t) = 1/G₁ + t/eta₁ + (1/G₂)(1 - exp(-t*G₂/eta₂))
2. Ecrire 3 tests de validation analytique (Java et C++)
3. Lancer une identification sur des donnees synthetiques

### Exercice 3 : Tracer le flux d'un upload

Suivre le chemin complet d'un upload de fichier CSV :
1. `DatasetController.upload()` → reception multipart
2. `DatasetUseCase.upload()` → orchestration
3. `CsvDataParserAdapter.parse()` → parsing + inference colonnes
4. `LocalFileStorageAdapter.store()` → sauvegarde fichier
5. `DatasetRepositoryAdapter.save()` → persistence BDD
6. `DatasetEventPublisher.publish()` → event Kafka
7. `NotificationService.notifyDatasetReady()` → WebSocket (V2)

### Exercice 4 : Comprendre le routing gRPC (V2)

1. Lire `ComputeEngineRoutingAdapter.java`
2. Identifier les 3 chemins possibles (gRPC ok, gRPC fail, pas de gRPC)
3. Expliquer pourquoi `@Primary` est necessaire
4. Simuler un fallback en mettant `grpc.enabled=false`

### Exercice 5 : Visualisation 3D (V2)

1. Ouvrir `mesh-viewer.component.ts`
2. Comprendre la decomposition tetraedre → 4 faces triangulaires
3. Modifier l'echelle de couleur (HSL → autre mapping)
4. Ajouter un bouton pour exporter la scene en capture d'ecran

### Exercice 6 : Deployer sur Kubernetes (V2)

1. Lire `deploy/helm/rheosim/Chart.yaml` et ses dependances
2. Comparer `values-dev.yaml` et `values-prod.yaml`
3. Expliquer pourquoi le compute-engine a un `nodeSelector`
4. Ajouter un nouveau service dans le umbrella chart

---

## 9. Ressources Complementaires

### 9.1 Rheologie et Viscoelasticite

- Ferry, J.D. — *Viscoelastic Properties of Polymers* (reference)
- Tschoegl, N.W. — *The Phenomenological Theory of Linear Viscoelastic Behavior*
- Macosko, C.W. — *Rheology: Principles, Measurements, and Applications*

### 9.2 Architecture Logicielle

- Vernon, V. — *Implementing Domain-Driven Design*
- Cockburn, A. — *Hexagonal Architecture* (article original)
- Martin, R.C. — *Clean Architecture*
- Newman, S. — *Building Microservices* (2nd ed.)

### 9.3 Elements Finis

- Zienkiewicz, O.C. — *The Finite Element Method* (reference FEM)
- Hughes, T.J.R. — *The Finite Element Method: Linear Static and Dynamic Analysis*
- Eigen documentation — sparse solvers, parallelism

### 9.4 Optimisation Numerique

- Levenberg (1944) — "A Method for the Solution of Certain Non-Linear Problems in Least Squares"
- Marquardt (1963) — "An Algorithm for Least-Squares Estimation of Nonlinear Parameters"
- Press et al. — *Numerical Recipes* (chapitre 15 : Modeling of Data)

### 9.5 Technologies

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Angular Documentation](https://angular.dev)
- [gRPC Documentation](https://grpc.io/docs/)
- [Three.js Documentation](https://threejs.org/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [OpenTelemetry](https://opentelemetry.io/docs/)
- [Prometheus Alerting Rules](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
- [k6 Load Testing](https://k6.io/docs/)
