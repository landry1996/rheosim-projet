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

## Taches Terminees V3

- [x] PHASE 1 : ML Auto-Calibration
  - [x] Service Python ML complet (FastAPI + gRPC)
  - [x] Training pipeline (data generator, feature extractor, XGBoost, PyTorch)
  - [x] ONNX export et inference
  - [x] Integration gRPC backend Java (MLServiceGrpcClient)
  - [x] Frontend : ML suggestion component avec score de confiance
  - [x] Docker Compose : ml-service container

- [x] PHASE 2 : Couplage Thermo-Mecanique
  - [x] ThermalSolver C++ (FEM, Euler implicite, masse lumpee)
  - [x] TTSModel (WLF + Arrhenius)
  - [x] ThermoMechanicalSolver (staggered coupling)
  - [x] Conditions aux limites thermiques (Dirichlet, Neumann, Robin)

- [x] PHASE 3 : Marketplace de Plugins
  - [x] Domain : Plugin, PluginVersion, PluginReview
  - [x] Application : MarketplaceUseCase, DTOs
  - [x] Infrastructure : JPA entities, repos, MarketplaceController
  - [x] Migration Flyway V2 (marketplace tables + GIN indexes)
  - [x] Frontend : catalogue + detail

- [x] PHASE 4 : Collaboration CRDT + Billing SaaS
  - [x] WebSocket handler binaire/texte (CRDT + awareness)
  - [x] Frontend CollaborationService + PresenceIndicator
  - [x] Domain billing : Subscription, Tier, TierLimits
  - [x] QuotaEnforcementFilter
  - [x] BillingController (checkout, portal, webhook Stripe)
  - [x] Migration Flyway V3

- [x] PHASE 5 : Edge Computing + PWA
  - [x] Helm chart K3s (backend, ML, network policies)
  - [x] Script install-airgapped.sh
  - [x] PWA : Service Worker + Web App Manifest
  - [x] PWA Service Angular (install prompt, push)

- [x] PHASE 6 : Securite & Performance
  - [x] SecurityHeadersFilter (CSP complet, HSTS, Cache-Control)
  - [x] k6 load test (500 VUs, 5 scenarios)
  - [x] Network policies Kubernetes zero-trust
  - [x] Audit securite complet + corrections

- [x] AUDIT SECURITE & HARDENING
  - [x] Migration localStorage → sessionStorage (anti-XSS)
  - [x] WebSocket auth interceptor + origines restreintes
  - [x] Rate limiting renforce auth (10/min)
  - [x] XSRF protection Angular
  - [x] Stripe webhook signature verification (HMAC-SHA256)
  - [x] InputSanitizationFilter (XSS/injection query params)
  - [x] WebSocket limites (64KB, 50 sessions/doc)
  - [x] ML input validation (10000 pts, 20 batch, types whitelistes)
  - [x] Credentials externalises (.env, K8s secrets)
  - [x] Docker ports 127.0.0.1 + Redis requirepass
  - [x] TLS ingress + CORS headers restreints

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

- [x] BLOC 9 : DevOps & Deploiement
  - [x] Dockerfile backend (multi-stage, Eclipse Temurin 21, non-root user)
  - [x] Dockerfile frontend (multi-stage, Node 22 build + nginx)
  - [x] nginx.conf (SPA routing + reverse proxy /api/)
  - [x] docker-compose.prod.yml (full stack orchestration)
  - [x] .dockerignore (backend + frontend)
  - [x] CI/CD pipeline GitHub Actions (backend test, frontend build, Docker build)
  - [x] OpenAPI config (SecurityScheme JWT, metadata)
  - [x] Swagger UI disponible sur /api/swagger-ui.html
  - [x] README professionnel (architecture, quickstart, API, stack, decisions)
  - [x] BUILD SUCCESS — backend compile OK, frontend build OK

## Taches A Faire

(aucune — V3 complet, audit securite effectue)

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
- Compute engine C++ operationnel (3D FEM + thermo-mecanique + TTS)
- Service ML Python operationnel (FastAPI + gRPC + ONNX)
- Chaque bounded context a ses propres packages dans domain/application/infrastructure
- Communication inter-contextes via Domain Events (Kafka)
- Communication synchrone backend→ML via gRPC (port 50052)
- Kafka en mode KRaft (pas de Zookeeper)
- Securite auditee : sessionStorage, rate limiting auth, CSP complet, WebSocket auth
- Deploiement edge K3s air-gapped supporte (Helm chart + script)
- PWA installable avec cache offline et push notifications
