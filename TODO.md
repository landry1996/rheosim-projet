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
- [x] BLOC 2 : Domain Identity — User, Role, RefreshToken entities
- [x] BLOC 2 : Ports — UserRepository, RoleRepository, RefreshTokenRepository, PasswordEncoder, TokenProvider
- [x] BLOC 2 : Domain Events — UserRegisteredEvent
- [x] BLOC 2 : Application layer — DTOs (records), AuthenticationUseCase, UserMapper
- [x] BLOC 2 : Infrastructure — JPA entities, JPA repositories, Adapters
- [x] BLOC 2 : Security — SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider, BcryptPasswordEncoder
- [x] BLOC 2 : REST — AuthController (register, login, refresh, logout)
- [x] BLOC 2 : GlobalExceptionHandler
- [x] BLOC 2 : Kafka topics configuration
- [x] BLOC 2 : Flyway V1 migration (identity tables + audit_logs)
- [x] BLOC 2 : Rate limiting (Bucket4j RateLimitingFilter)
- [x] BLOC 2 : Audit logging service (AuditService async + JdbcTemplate)
- [x] BLOC 2 : Tests unitaires AuthenticationUseCase (7 tests)
- [x] BLOC 2 : ArchUnit tests (5 regles hexagonales)
- [x] BLOC 3 : Domain — Project, Material, MaterialModel, MaterialFamily, ConstitutiveModelType, ProjectStatus
- [x] BLOC 3 : Ports — ProjectRepository, MaterialRepository
- [x] BLOC 3 : Application — ProjectUseCase, MaterialUseCase
- [x] BLOC 3 : DTOs — CreateProjectRequest, ProjectResponse, CreateMaterialRequest, MaterialResponse, UpdateMaterialModelRequest, MaterialModelResponse
- [x] BLOC 3 : REST — ProjectController, MaterialController
- [x] BLOC 3 : Infrastructure — JPA entities, JPA repositories, Adapters (ProjectRepositoryAdapter, MaterialRepositoryAdapter)
- [x] BLOC 3 : Flyway V2 migration (projects, materials, project_materials)
- [x] BLOC 3 : ArchUnit test mis a jour (ports Project + Material)
- [x] BLOC 4 : Domain — Dataset, ExperimentType, DatasetStatus, DataColumn, ValidationError
- [x] BLOC 4 : Ports — DatasetRepository, FileStoragePort, DataParserPort, DataValidatorPort
- [x] BLOC 4 : Events — DatasetUploadedEvent, DatasetValidatedEvent
- [x] BLOC 4 : Application — DatasetUseCase, DTOs (UploadDatasetRequest, DatasetResponse, etc.)
- [x] BLOC 4 : Infrastructure — CsvDataParserAdapter, ExcelDataParserAdapter, RheologyDataValidatorAdapter
- [x] BLOC 4 : Infrastructure — LocalFileStorageAdapter, DatasetRepositoryAdapter, DatasetEventPublisher
- [x] BLOC 4 : REST — DatasetController (upload multipart, validate, list, get, delete)
- [x] BLOC 4 : Flyway V3 migration (datasets table)
- [x] BLOC 4 : ArchUnit test mis a jour (ports Experiment)
- [x] BLOC 4 : Dependencies — Apache Commons CSV 1.12.0, Apache POI 5.3.0
- [x] BLOC 5 : Domain — SimulationJob, JobStatus, SimulationType, SimulationResult
- [x] BLOC 5 : Ports — ConstitutiveLaw, SimulationJobRepository, ParameterIdentificationPort
- [x] BLOC 5 : Events — SimulationJobSubmittedEvent, SimulationJobCompletedEvent
- [x] BLOC 5 : Application — SimulationUseCase, DTOs (SubmitJobRequest, JobResponse, JobResultResponse)
- [x] BLOC 5 : Engine — MaxwellLaw, KelvinVoigtLaw, PronySeriesLaw (implementations)
- [x] BLOC 5 : Engine — LevenbergMarquardtIdentifier (Commons Math, numerical Jacobian)
- [x] BLOC 5 : Engine — SimulationJobProcessor (scheduled polling, auto-execution)
- [x] BLOC 5 : Infrastructure — SimulationJobRepositoryAdapter, JPA entity, JPA repository
- [x] BLOC 5 : REST — SimulationController (submit, status, result, cancel)
- [x] BLOC 5 : Flyway V4 migration (simulation_jobs table)
- [x] BLOC 5 : ArchUnit test mis a jour (ports Simulation)
- [x] BLOC 5 : Tests scientifiques — 11 tests de validation analytique (Maxwell, KV, Prony)
- [x] BLOC 8 : Tests domain — ProjectTest (9), MaterialTest (6), ReportTest (4)
- [x] BLOC 8 : Tests application — ProjectUseCaseTest (7), SimulationUseCaseTest (4)
- [x] BLOC 8 : Tests infrastructure — CsvDataParserAdapterTest (6), RheologyDataValidatorAdapterTest (6)
- [x] BLOC 8 : Tests scientifiques — ParameterIdentificationTest (3) — convergence LM sur donnees synthetiques
- [x] BLOC 8 : JaCoCo coverage reporting (plugin configure dans parent POM)
- [x] BUILD SUCCESS — 88 tests passent, compilation validee

## Taches En Cours

(aucune)

## Taches Terminees (suite)

- [x] BLOC 7 : Frontend Angular 21 standalone
  - [x] Setup Angular 21+ standalone (CLI 21.2.3, Chart.js)
  - [x] Core : models (User, Project, Material, Dataset, Simulation, Report)
  - [x] Core : services (Auth, Project, Material, Dataset, Simulation, Report)
  - [x] Core : guards (authGuard, guestGuard)
  - [x] Core : interceptors (authInterceptor avec refresh token)
  - [x] Shared : NavbarComponent, SidebarComponent
  - [x] Feature : LoginComponent, RegisterComponent
  - [x] Feature : DashboardComponent (CRUD projets, stats)
  - [x] Feature : DatasetUploadComponent (drag & drop, multipart)
  - [x] Feature : DatasetListComponent (table, status badges)
  - [x] Feature : SimulationComponent (calibration + Chart.js)
  - [x] Feature : ReportsComponent (generation + download)
  - [x] Routing lazy-loaded avec guards
  - [x] Proxy config pour dev (backend localhost:8080)
  - [x] Environments (dev / prod)
  - [x] BUILD SUCCESS — ng build OK, tous les chunks generes

## Taches A Faire

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
