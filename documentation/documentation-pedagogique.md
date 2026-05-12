# RheoSim Enterprise — Documentation Pedagogique

## Introduction

Ce document est un guide d'apprentissage pour les developpeurs rejoignant le projet RheoSim Enterprise. Il explique les concepts architecturaux, les patterns utilises, et les fondements scientifiques de la plateforme.

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
    │ JPA, File, etc.  │  │ Controllers    │
    └──────────────────┘  └────────────────┘
```

**Principe** : Le domaine definit des **ports** (interfaces). L'infrastructure fournit des **adapters** (implementations).

### 1.3 Exemple Concret dans RheoSim

```
Domain (Port) :
  interface SimulationJobRepository {
      SimulationJob save(SimulationJob job);
      Optional<SimulationJob> findById(UUID id);
  }

Infrastructure (Adapter) :
  class SimulationJobRepositoryAdapter implements SimulationJobRepository {
      private final SimulationJobJpaRepository jpaRepo; // Spring Data
      // ... implementation avec conversion JPA entity ↔ domain entity
  }
```

**Benefice** : Le domaine ne sait pas qu'il y a une base PostgreSQL derriere. On pourrait remplacer par MongoDB sans toucher au metier.

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

**Regle** : Un `User` dans Identity n'est pas le meme objet qu'un `ownerId` dans Project. Chaque contexte a sa propre representation.

### 2.2 Entite vs Value Object

```java
// ENTITE : a une identite (UUID), mutable, cycle de vie
public class SimulationJob {
    private UUID id;          // Identite
    private JobStatus status; // Etat mutable
    public void start() { this.status = RUNNING; }
}

// VALUE OBJECT : pas d'identite, immutable, compare par valeur
public record MaterialModel(
    ConstitutiveModelType modelType,
    int numberOfBranches,
    double equilibriumModulus,
    List<PronyBranch> branches,
    double referenceTemperatureK
) {}
```

### 2.3 Domain Events

Un evenement signale que quelque chose d'important s'est passe :

```java
public class DatasetUploadedEvent extends DomainEvent {
    private final UUID datasetId;
    private final UUID projectId;
    // Publie quand un dataset est uploade avec succes
}
```

Les events sont publies sur Kafka pour decoupler les contextes.

### 2.4 Use Case (Application Layer)

Un use case orchestre un scenario metier sans contenir de logique :

```java
public class DatasetUseCase {
    private final DatasetRepository repository;     // Port
    private final FileStoragePort fileStorage;       // Port
    private final List<DataParserPort> parsers;     // Port (strategy)
    private final DataValidatorPort validator;       // Port
    
    public DatasetResponse upload(UploadDatasetRequest request) {
        // 1. Trouver le parser adapte (strategy pattern)
        // 2. Parser le fichier
        // 3. Stocker le fichier
        // 4. Sauvegarder l'entite
        // 5. Publier un event
    }
}
```

---

## 3. Patterns de Conception Utilises

### 3.1 Strategy Pattern — Lois Constitutives

**Probleme** : Plusieurs modeles rheologiques avec la meme interface mais des equations differentes.

**Solution** :

```java
// Interface commune (port)
public interface ConstitutiveLaw {
    double computeRelaxationModulus(double time, double[] params);
    double computeStorageModulus(double omega, double[] params);
    // ...
}

// Implementations (strategies)
public class MaxwellLaw implements ConstitutiveLaw {
    public double computeRelaxationModulus(double t, double[] p) {
        return p[0] * Math.exp(-t / p[1]); // G * exp(-t/tau)
    }
}

public class PronySeriesLaw implements ConstitutiveLaw {
    public double computeRelaxationModulus(double t, double[] p) {
        double g = p[0]; // G_infinity
        for (int i = 0; i < (p.length - 1) / 2; i++) {
            g += p[1 + 2*i] * Math.exp(-t / p[2 + 2*i]);
        }
        return g;
    }
}
```

**Ou c'est utilise** : Compute engine, parsers de donnees (CSV/Excel), generateurs de rapports (PDF/CSV/JSON).

### 3.2 Builder Pattern — Entites Complexes

**Probleme** : Constructeurs avec beaucoup de parametres, validation a la creation.

```java
public class Material {
    // Constructeur prive
    private Material() {}
    
    public static MaterialBuilder builder() { return new MaterialBuilder(); }
    
    public static class MaterialBuilder {
        private String name;
        private MaterialFamily family;
        // ...
        
        public Material build() {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("name is required");
            if (family == null)
                throw new IllegalArgumentException("family is required");
            
            Material m = new Material();
            m.id = UUID.randomUUID();
            m.name = name;
            m.family = family;
            m.version = 1;
            return m;
        }
    }
}
```

**Note** : Lombok etant retire (incompatible Java 25), les builders sont ecrits manuellement.

### 3.3 Observer Pattern — Domain Events + Kafka

```
[SimulationJob.complete()] 
    → publie SimulationJobCompletedEvent
    → DatasetEventPublisher serialise en JSON
    → Kafka topic "simulation.completed"
    → (Futur) Notification service consomme
```

### 3.4 Template Method — Validation de Donnees

```java
// Algorithme commun de validation
public List<ValidationError> validate(ExperimentType type, List<DataColumn> cols, List<double[]> rows) {
    List<ValidationError> errors = new ArrayList<>();
    
    // 1. Verifier nombre de lignes (commun)
    if (rows.isEmpty()) errors.add(error("No data rows"));
    if (rows.size() < 3) errors.add(error("Minimum 3 points"));
    
    // 2. Verifier colonnes requises (specifique au type)
    errors.addAll(validateRequiredColumns(type, cols));
    
    // 3. Verifier qualite des donnees (commun)
    errors.addAll(validateDataQuality(cols, rows));
    
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
- **Exemple** : Polymere fondu soumis a une deformation constante

#### Modele de Kelvin-Voigt (ressort + amortisseur en parallele)

```
    ┌───[G]───┐
    │         │
    ├───[η]───┤
    │         │
    └─────────┘
```

- **Fluage** : J(t) = (1/G) * (1 - exp(-t/tau))
- **Interpretation** : Le materiau se deforme progressivement vers un equilibre
- **Exemple** : Elastomere sous charge constante

#### Serie de Prony (N elements de Maxwell en parallele)

```
    ┌───[G₁]───[η₁]───┐
    ├───[G₂]───[η₂]───┤
    ├───[G₃]───[η₃]───┤
    ├───[G∞]───────────┤  (ressort seul = equilibre)
    └──────────────────┘
```

- **Relaxation** : G(t) = G_inf + Σ G_i * exp(-t/tau_i)
- **Interpretation** : Spectre de temps de relaxation (plusieurs mecanismes)
- **Exemple** : Polymere reel avec distribution de masses molaires

### 4.2 Domaine Frequentiel

En sollicitation oscillatoire (omega = pulsation) :

- **Module de conservation** G'(omega) : energie stockee (elastique)
- **Module de perte** G''(omega) : energie dissipee (visqueuse)
- **Viscosité complexe** |eta*| = sqrt(G'² + G''²) / omega

Pour Maxwell :
```
G'(omega)  = G * (omega*tau)² / (1 + (omega*tau)²)
G''(omega) = G * (omega*tau)  / (1 + (omega*tau)²)
```

**Propriete** : G'' est maximal quand omega*tau = 1 (pic de dissipation).

### 4.3 Identification Parametrique

**Objectif** : Trouver les parametres [G, tau] (ou [G_inf, G_i, tau_i]) qui ajustent au mieux les donnees experimentales.

**Methode : Levenberg-Marquardt**

C'est un algorithme iteratif qui minimise :
```
S(p) = Σ [y_exp(i) - y_model(x_i, p)]²
```

Principe :
1. Partir d'une estimation initiale p₀
2. Calculer le Jacobien J (derivees partielles de chaque residuel par rapport a chaque parametre)
3. Resoudre : delta_p = (J^T * J + lambda * I)^(-1) * J^T * r
   - Si lambda grand → pas de gradient (prudent)
   - Si lambda petit → pas de Gauss-Newton (rapide pres du minimum)
4. Ajuster lambda selon le progres
5. Repeter jusqu'a convergence

**Dans RheoSim** :
- Jacobien numerique (differences centrales) : J_ij ≈ [f(p+h) - f(p-h)] / (2h)
- Contraintes bornees : parametres physiques toujours positifs
- Critere de convergence : variation relative < 10⁻¹²

### 4.4 Coefficient R²

```
R² = 1 - SS_res / SS_tot

SS_res = Σ (y_exp - y_fit)²      (residus)
SS_tot = Σ (y_exp - y_mean)²     (variance totale)
```

- R² = 1 : ajustement parfait
- R² = 0 : le modele predit la moyenne (inutile)
- R² < 0 : le modele est pire que la moyenne

**En pratique** : R² > 0.99 = bon ajustement pour un modele viscoelastique simple.

---

## 5. Guide du Developpeur

### 5.1 Ajouter un Nouveau Modele Constitutif

1. **Creer l'implementation dans infrastructure** :

```java
// rheosim-infrastructure/.../engine/PowerLawFluid.java
public class PowerLawFluid implements ConstitutiveLaw {
    // params = [K (consistency), n (power index)]
    
    @Override
    public double computeRelaxationModulus(double t, double[] params) {
        // ... implementation specifique
    }
    
    @Override
    public int getParameterCount() { return 2; }
    
    @Override
    public String[] getParameterNames() { return new String[]{"K", "n"}; }
}
```

2. **Ajouter dans l'enum** `ConstitutiveModelType` (domain)
3. **Enregistrer dans le processor** : ajouter le mapping type → implementation
4. **Ecrire des tests** : solutions analytiques connues
5. **Ajouter dans le frontend** : option dans le select de SimulationComponent

### 5.2 Ajouter un Nouveau Type d'Experience

1. **Ajouter dans l'enum** `ExperimentType` (domain)
2. **Definir les colonnes requises** dans `RheologyDataValidatorAdapter`
3. **Mettre a jour le frontend** : option dans DatasetUploadComponent
4. **Ecrire des tests** : cas valide + cas invalide

### 5.3 Ajouter un Endpoint API

1. **Definir le DTO** dans `rheosim-application` (record Java)
2. **Creer/modifier le Use Case** dans `rheosim-application`
3. **Creer le Controller** dans `rheosim-infrastructure`
4. **Documenter** : annotations OpenAPI (@Operation, @ApiResponse)
5. **Tester** : test unitaire du use case + test du controller

### 5.4 Conventions de Code

| Element | Convention |
|---------|-----------|
| Entites domain | Classes mutables avec Builder manuel |
| Value Objects | Records Java (immutables) |
| DTOs | Records Java |
| Ports | Interfaces dans `domain/.../port/` |
| Adapters | Classes dans `infrastructure/.../adapter/` |
| Controllers | Dans `infrastructure/.../adapter/` (c'est un adapter REST) |
| Tests | `*Test.java`, @DisplayName descriptif |
| Packages | `com.rheosim.{layer}.{context}.{type}` |

### 5.5 Commandes Utiles

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

# Docker
cd docker
docker compose up -d                  # Infra locale (PG, Kafka, Redis)
docker compose -f docker-compose.prod.yml up --build  # Full stack
```

---

## 6. Exercices Pratiques

### Exercice 1 : Comprendre l'architecture

1. Ouvrir `HexagonalArchitectureTest.java` et lire les 5 regles
2. Essayer d'ajouter `import org.springframework...` dans une classe domain
3. Lancer `mvn test` et observer l'echec ArchUnit
4. Retirer l'import et verifier que les tests passent

### Exercice 2 : Ajouter un modele constitutif

1. Implementer le modele de Burgers (Maxwell + Kelvin-Voigt en serie)
   - J(t) = 1/G₁ + t/eta₁ + (1/G₂)(1 - exp(-t*G₂/eta₂))
2. Ecrire 3 tests de validation analytique
3. Lancer une identification sur des donnees synthetiques

### Exercice 3 : Tracer le flux d'un upload

Suivre le chemin complet d'un upload de fichier CSV :
1. `DatasetController.upload()` → reception multipart
2. `DatasetUseCase.upload()` → orchestration
3. `CsvDataParserAdapter.parse()` → parsing + inference colonnes
4. `LocalFileStorageAdapter.store()` → sauvegarde fichier
5. `DatasetRepositoryAdapter.save()` → persistence BDD
6. `DatasetEventPublisher.publish()` → event Kafka

### Exercice 4 : Frontend — Ajouter un ecran

1. Creer un composant `MaterialDetailComponent` dans `features/materials/`
2. Afficher les proprietes du materiau et son modele constitutif
3. Ajouter la route lazy-loaded dans `app.routes.ts`
4. Naviguer depuis le dashboard

---

## 7. Ressources Complementaires

### 7.1 Rheologie et Viscoelasticite

- Ferry, J.D. — *Viscoelastic Properties of Polymers* (reference)
- Tschoegl, N.W. — *The Phenomenological Theory of Linear Viscoelastic Behavior*
- Macosko, C.W. — *Rheology: Principles, Measurements, and Applications*

### 7.2 Architecture Logicielle

- Vernon, V. — *Implementing Domain-Driven Design*
- Cockburn, A. — *Hexagonal Architecture* (article original)
- Martin, R.C. — *Clean Architecture*

### 7.3 Optimisation Numerique

- Levenberg (1944) — "A Method for the Solution of Certain Non-Linear Problems in Least Squares"
- Marquardt (1963) — "An Algorithm for Least-Squares Estimation of Nonlinear Parameters"
- Press et al. — *Numerical Recipes* (chapitre 15 : Modeling of Data)

### 7.4 Technologies

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Angular Documentation](https://angular.dev)
- [Apache Commons Math Javadoc](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/)
- [Chart.js Documentation](https://www.chartjs.org/docs/)
