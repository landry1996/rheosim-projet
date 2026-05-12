# RHEOSIM ENTERPRISE — Suivi de Projet

## Decisions Techniques

| # | Decision | Choix | Justification |
|---|----------|-------|---------------|
| 1 | Architecture | Monolithe modulaire hexagonal | V1 MVP, bounded contexts prepares pour microservices V2 |
| 2 | Compute Engine V1 | Java pur (pas C++) | 1D/2D suffisant en Java, evite complexite inter-langage |
| 3 | Messaging | Apache Kafka (KRaft, sans Zookeeper) | Event-driven, replay, contrainte projet |
| 4 | Backend | Spring Boot 3.4.5 + Java 21 (compile target) | Runtime Java 25, cross-compile vers 21 |
| 5 | Frontend | Angular 18+ standalone | Maturite enterprise, signals, standalone components |
| 6 | BDD | PostgreSQL 15 + Flyway | ACID, migrations versionnees |
| 7 | Securite | Spring Security + JWT (jjwt) + Refresh Token + RBAC | Best practices, stateless |
| 8 | Mapping | MapStruct | Compile-time, type-safe, performant |
| 9 | API Doc | OpenAPI/Swagger (springdoc 2.8.4) | Standard industrie |
| 10 | Tests | JUnit 5 + Mockito + Testcontainers + ArchUnit | Couverture complete |
| 11 | Containers | Docker + Docker Compose | Dev local + deploiement |
| 12 | Lombok | RETIRE — incompatible Java 25 | Records Java + builders manuels a la place |

## Taches Terminees

- [x] Analyse du cahier des charges
- [x] BLOC 1 : Structure projet Maven multi-modules
- [x] BLOC 1 : Configuration application.yml / dev / test / secret.example
- [x] BLOC 1 : Docker Compose (PostgreSQL, Kafka KRaft, Redis, Kafka-UI)
- [x] BLOC 1 : .gitignore
- [x] BLOC 2 (partiel) : Domain Identity — User, Role, RefreshToken entities
- [x] BLOC 2 (partiel) : Ports — UserRepository, RoleRepository, RefreshTokenRepository, PasswordEncoder, TokenProvider
- [x] BLOC 2 (partiel) : Domain Events — UserRegisteredEvent
- [x] BLOC 2 (partiel) : Application layer — DTOs (records), AuthenticationUseCase, UserMapper
- [x] BLOC 2 (partiel) : Infrastructure — JPA entities, JPA repositories, Adapters
- [x] BLOC 2 (partiel) : Security — SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider, BcryptPasswordEncoder
- [x] BLOC 2 (partiel) : REST — AuthController (register, login, refresh, logout)
- [x] BLOC 2 (partiel) : GlobalExceptionHandler
- [x] BLOC 2 (partiel) : Kafka topics configuration
- [x] BLOC 2 (partiel) : Flyway V1 migration (identity tables + audit_logs)
- [x] BUILD SUCCESS — compilation validee

## Taches En Cours

- [ ] BLOC 2 (suite) : Rate limiting, Audit logging service, tests

## Taches A Faire

### BLOC 2 — Securite & Authentification (reste)
- [ ] Rate limiting (Bucket4j)
- [ ] Audit logging service (persister dans audit_logs)
- [ ] Tests unitaires AuthenticationUseCase
- [ ] Tests integration AuthController (Testcontainers)
- [ ] ArchUnit test (verification deps hexagonales)

### BLOC 3 — Module Project & Material (Domain Core)
- [ ] Entites Project, Material, MaterialModel, ConstitutiveModelType (domaine)
- [ ] Ports (interfaces repository)
- [ ] Use cases (application layer)
- [ ] DTOs + MapStruct mappers
- [ ] REST Controllers
- [ ] JPA Adapters (infrastructure)
- [ ] Flyway V2 migration
- [ ] Tests unitaires + integration

### BLOC 4 — Module Experiment (Import & Validation)
- [ ] Entites Dataset, Experiment, ExperimentType
- [ ] Import CSV/Excel (parsers)
- [ ] Pipeline de validation des donnees
- [ ] Stockage fichiers (local / S3)
- [ ] Events Kafka pour notification

### BLOC 5 — Module Simulation (Compute Engine Java 1D)
- [ ] Interface ConstitutiveLaw (port)
- [ ] Implementation Maxwell, KelvinVoigt, Prony-N
- [ ] Identification parametrique (L-BFGS-B via Apache Commons Math)
- [ ] Job lifecycle (QUEUED, RUNNING, COMPLETED, FAILED)
- [ ] Event-driven job processing via Kafka
- [ ] Resultats : series temporelles + metriques

### BLOC 6 — Module Reporting
- [ ] Generation PDF (Apache PDFBox)
- [ ] Templates de rapports
- [ ] Export CSV, JSON
- [ ] Metadonnees Dublin Core

### BLOC 7 — Frontend Angular
- [ ] Setup Angular 18+ standalone
- [ ] Module Auth (login, register, guards)
- [ ] Dashboard
- [ ] Ecran upload dataset
- [ ] Ecran calibration modele
- [ ] Ecran resultats + graphiques (Chart.js)
- [ ] Ecran generation rapport

### BLOC 8 — Tests & Qualite
- [ ] ArchUnit (verification architecture hexagonale)
- [ ] Testcontainers pour tests integration
- [ ] Tests de validation scientifique (solutions analytiques)
- [ ] Coverage > 70% sur domain/application

### BLOC 9 — DevOps & Deploiement
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Docker images optimisees
- [ ] Documentation API (Swagger UI)
- [ ] README professionnel

## Risques Identifies

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|-------------|--------|------------|
| 1 | Scope creep V1 | Haute | Haut | Discipline strict du NO, focus 1D/2D |
| 2 | Performance compute Java | Faible | Moyen | Suffisant pour 1D/2D, profiling precoce |
| 3 | Convergence identification | Moyenne | Haut | Regularisation, contraintes physiques |
| 4 | Complexite Kafka pour MVP | Moyenne | Moyen | Config minimale, 1 topic par contexte |
| 5 | Java 25 runtime vs target 21 | Faible | Faible | Cross-compile fonctionne, Lombok retire |

## Bugs Detectes

- [RESOLU] Lombok 1.18.36 incompatible Java 25 (ExceptionInInitializerError) → retire du projet

## Notes

- Runtime = Java 25, compilation target = Java 21 (--release 21)
- Lombok retire : on utilise records Java pour DTOs, builders manuels pour entites
- Spring Boot 4.x n'est pas encore GA (mai 2026) — on utilise 3.4.5
- Le compute engine C++ est prevu pour V2 (3D FEM)
- Chaque bounded context a ses propres packages dans domain/application/infrastructure
- Communication inter-contextes via Domain Events (Kafka)
- Pas de microservices en V1 (ADR-4 du CDC)
- Kafka en mode KRaft (pas de Zookeeper)
