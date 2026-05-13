# RheoSim Enterprise — Documentation Fonctionnelle

## 1. Presentation Generale

### 1.1 Objectif

RheoSim Enterprise est une plateforme scientifique de simulation viscoelastique destinee aux ingenieurs materiaux et chercheurs en rheologie. Elle permet d'importer des donnees experimentales, d'identifier les parametres de lois de comportement constitutives (1D et 3D FEM), de collaborer en equipe, et de generer des rapports professionnels.

### 1.2 Perimetre

| Capacite | V1 | V2 | V3 |
|----------|----|----|-----|
| Simulation 1D/2D | Maxwell, Kelvin-Voigt, Prony | Conserve | Conserve |
| Simulation 3D FEM | - | Tetraedres P1, viscoelastique | + Thermo-mecanique, WLF/Arrhenius |
| Import de donnees | CSV, Excel | + MinIO S3 storage | Conserve |
| Identification parametrique | Levenberg-Marquardt Java | + C++ haute performance (gRPC) | + ML auto-calibration (ONNX) |
| Generation de rapports | PDF, CSV, JSON | + export VTK (3D) | Conserve |
| Multi-utilisateurs | JWT, roles USER/ADMIN | + RBAC par projet | + SaaS billing (FREE/PRO/ENTERPRISE) |
| Collaboration | - | Organisations, invitations | + CRDT temps-reel, presence |
| Multi-tenant | - | Isolation par organisation | + Quotas par tier |
| Notifications temps reel | - | WebSocket STOMP/SockJS | + Push notifications (PWA) |
| Visualisation 3D | - | Three.js (deformation) | Conserve |
| Marketplace | - | - | Catalogue de plugins, avis, versions |
| Edge/Offline | - | - | PWA + K3s air-gapped deployment |
| Securite | JWT + RBAC | + Network Policies | + Rate limiting auth, CSP, sanitisation |
| Observabilite | Actuator | Prometheus + Grafana + Jaeger | + k6 load testing (500 VUs) |

### 1.3 Utilisateurs Cibles

- **Ingenieur materiaux** : importe ses donnees experimentales, lance des identifications parametriques, visualise les deformations 3D
- **Chercheur en rheologie** : calibre des modeles constitutifs, compare les resultats avec des donnees experimentales, partage des projets
- **Responsable qualite** : consulte les rapports generes, verifie la convergence des identifications
- **Chef de projet** : gere les organisations, invite des collaborateurs, consulte l'audit trail

---

## 2. Modules Fonctionnels

### 2.1 Module Authentification (Identity)

**Acteurs** : Tous les utilisateurs

| Fonctionnalite | Description |
|----------------|-------------|
| Inscription | Formulaire (email, mot de passe, nom, prenom). Validation email unique. |
| Connexion | Email + mot de passe. Retourne un access token JWT (15 min) + refresh token (7 jours). |
| Deconnexion | Revocation du refresh token. |
| Rafraichissement | Renouvellement automatique du token avant expiration. |
| Rate Limiting | 60 req/min API, **10 req/min auth** (protection renforcee V3). |
| Audit | Chaque action d'authentification est tracee (table audit_logs). |

**Regles metier** :
- Mot de passe minimum 8 caracteres
- Email doit etre unique dans le systeme
- Refresh token a usage unique (rotation a chaque utilisation)
- Les tokens expires sont nettoyes periodiquement
- JWT signe en RS256 (asymetrique) — cle publique distribuee aux microservices

### 2.2 Module Projets (Project)

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Creer un projet | Nom + description. Statut initial = DRAFT. |
| Activer un projet | Passage de DRAFT a ACTIVE. |
| Archiver un projet | Passage a ARCHIVED (lecture seule). |
| Lister les projets | Projets de l'utilisateur + projets partages avec lui. |
| Associer des materiaux | Un projet contient N materiaux. |
| Partager un projet | Inviter un collaborateur avec un role (EDITOR, VIEWER). |

**Cycle de vie** :
```
DRAFT → ACTIVE → ARCHIVED
```

**Regles metier** :
- Un projet ne peut etre active que s'il contient au moins un materiau
- Un projet archive ne peut plus etre modifie
- Seul le proprietaire (OWNER) peut archiver ou supprimer un projet
- Les EDITOR peuvent modifier le contenu, les VIEWER ne font que consulter
- Un projet est isole par organisation (multi-tenant)

### 2.3 Module Materiaux (Material)

**Acteurs** : Utilisateur authentifie (OWNER ou EDITOR du projet)

| Fonctionnalite | Description |
|----------------|-------------|
| Creer un materiau | Nom, grade, famille, fournisseur. Associe a un projet. |
| Mettre a jour le modele | Affecter un modele constitutif (Maxwell, KV, Prony). Incremente la version. |
| Modifier les details | Mise a jour partielle (nom, grade, fournisseur). |
| Supprimer un materiau | Suppression logique. |

**Familles de materiaux** : THERMOPLASTIC, ELASTOMER, THERMOSET, COMPOSITE, FOAM, OTHER

**Modeles constitutifs disponibles** :
- Maxwell (1 branche) : G, tau
- Kelvin-Voigt : G, tau
- Prony (N branches) : G_inf, {G_i, tau_i} pour i=1..N

### 2.4 Module Donnees Experimentales (Experiment)

**Acteurs** : Utilisateur authentifie (OWNER ou EDITOR)

| Fonctionnalite | Description |
|----------------|-------------|
| Importer un fichier | Upload CSV ou Excel (max 50 Mo). Stockage MinIO (V2). |
| Validation automatique | Verification colonnes requises, types, valeurs aberrantes. |
| Lister les datasets | Par projet, avec filtre par statut. |
| Supprimer un dataset | Suppression du fichier et de la reference. |

**Types d'experiences supportes** :

| Type | Colonnes requises |
|------|-------------------|
| CREEP | TIME + (STRAIN ou COMPLIANCE) |
| RELAXATION | TIME + (STRESS ou MODULUS) |
| FREQUENCY_SWEEP | FREQUENCY + (STORAGE_MODULUS ou LOSS_MODULUS) |
| FLOW_CURVE | SHEAR_RATE + (VISCOSITY ou STRESS) |
| TEMPERATURE_SWEEP | TEMPERATURE + mesure |
| STRAIN_SWEEP | STRAIN + mesure |
| DYNAMIC_OSCILLATORY | FREQUENCY + mesures dynamiques |

**Cycle de vie d'un dataset** :
```
UPLOADED → VALIDATING → VALID / INVALID → PROCESSING
```

**Regles de validation** :
- Minimum 3 points de mesure
- Colonnes obligatoires selon le type d'experience
- Detection de valeurs negatives sur grandeurs positives (warning)
- Detection de NaN/Infinity (erreur)

### 2.5 Module Simulation (Compute)

**Acteurs** : Utilisateur authentifie (OWNER ou EDITOR)

| Fonctionnalite | Description |
|----------------|-------------|
| Soumettre un job 1D | Identification parametrique classique (Java ou C++ engine). |
| Soumettre un job 3D FEM | Simulation par elements finis 3D (C++ engine obligatoire). |
| Suivi du statut | Polling ou WebSocket (QUEUED → RUNNING → COMPLETED/FAILED). |
| Suivi de progression | Barre de progression temps reel via WebSocket. |
| Consulter les resultats | Parametres identifies, R², nb iterations, courbe ajustee. |
| Visualiser les resultats 3D | Maillage + deformation + contraintes Von Mises (Three.js). |
| Annuler un job | Annulation d'un job en attente ou en cours. |

**Types de simulation** :
- **PARAMETER_IDENTIFICATION** : identification des parametres d'un modele constitutif a partir de donnees experimentales
- **FEM_3D** : simulation par elements finis 3D (maillage tetraedrique, viscoelasticite)
- **FORWARD_SIMULATION** : calcul de la reponse d'un modele avec des parametres donnes

**Routage compute engine** :
```
Job soumis → ComputeEngineRoutingAdapter
             ├── C++ gRPC engine (si disponible et mode 3D)
             └── Java engine (fallback ou mode 1D)
```

**Cibles d'ajustement (FitTarget)** :
- RELAXATION_MODULUS : G(t)
- CREEP_COMPLIANCE : J(t)
- STORAGE_MODULUS : G'(omega)
- LOSS_MODULUS : G''(omega)
- COMPLEX_VISCOSITY : |eta*|(omega)

**Metriques de sortie** :
- R² (coefficient de determination)
- Nombre d'iterations
- Convergence (oui/non)
- Parametres identifies avec noms
- Courbe ajustee (serie temporelle)
- Champs 3D : deplacements, contraintes Von Mises (mode FEM)

**Regles metier** :
- Maximum 4 jobs concurrents par instance
- Timeout d'un job : 1 heure
- Un job CANCELLED ne peut pas etre relance
- Les jobs FEM 3D sont routes vers le C++ engine exclusivement

### 2.6 Module Rapports (Reporting)

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Generer un rapport | Choix du format (PDF, CSV, JSON) et du titre. |
| Telecharger | Download du fichier genere. |
| Lister les rapports | Par projet, avec statut. |
| Supprimer | Suppression du fichier et de la reference. |

**Contenu d'un rapport** :
- Metadonnees Dublin Core (titre, createur, date, sujet)
- Informations projet et materiau
- Parametres du modele constitutif
- Metriques de qualite d'ajustement
- Courbes ajustees (si disponibles)

**Cycle de vie** :
```
GENERATING → READY / FAILED → EXPIRED
```

### 2.7 Module Collaboration (V2)

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Creer une organisation | Nom + slug (URL-friendly). Createur = OWNER. |
| Inviter un membre | Par email, avec role (ADMIN, MEMBER). Expiration 7 jours. |
| Accepter une invitation | L'invite rejoint l'organisation avec le role attribue. |
| Partager un projet | Ajouter un collaborateur avec role EDITOR ou VIEWER. |
| Audit trail | Historique complet des actions par utilisateur et par organisation. |
| Retirer un membre | L'OWNER ou ADMIN peut retirer un membre de l'organisation. |

**Roles Organisation** :

| Role | Permissions |
|------|-------------|
| OWNER | Tout (creer, inviter, supprimer, gerer les roles) |
| ADMIN | Inviter, retirer des membres, gerer les projets |
| MEMBER | Acceder aux projets partages selon leur role projet |

**Roles Projet** :

| Role | Permissions |
|------|-------------|
| OWNER | Tout (CRUD complet, archiver, supprimer, partager) |
| EDITOR | Modifier le contenu (materiaux, datasets, simulations) |
| VIEWER | Lecture seule (consulter projets et resultats) |

**Regles metier** :
- Une organisation a un seul OWNER (le createur)
- Un utilisateur peut appartenir a plusieurs organisations
- L'isolation multi-tenant filtre automatiquement par organization_id
- Les invitations expirent apres 7 jours si non acceptees

### 2.8 Module Notifications (V2)

**Acteurs** : Tous les utilisateurs connectes

| Fonctionnalite | Description |
|----------------|-------------|
| Simulation terminee | Notification push quand un job complete ou echoue. |
| Invitation recue | Notification quand l'utilisateur est invite a une organisation. |
| Projet partage | Notification quand un projet est partage avec l'utilisateur. |
| Progression simulation | Stream temps reel de la progression d'un job. |

**Technologie** : WebSocket STOMP sur SockJS, destinations personnelles (`/user/{id}/queue/notifications`) et broadcast (`/topic/simulations/{id}`).

---

## 3. Parcours Utilisateur Type

### 3.1 Scenario : Identification parametrique d'un elastomere

1. L'utilisateur se connecte
2. Il cree un projet "Characterisation SBR 2024"
3. Il cree un materiau "SBR Rubber" (famille ELASTOMER, grade "High", fournisseur "BASF")
4. Il importe un fichier CSV contenant des donnees de relaxation (time, G(t))
5. Le systeme valide automatiquement les donnees (colonnes TIME + MODULUS presentes, >3 points)
6. L'utilisateur soumet un job de type PARAMETER_IDENTIFICATION avec :
   - Modele : Maxwell
   - Cible : RELAXATION_MODULUS
7. Le systeme identifie G = 5000 Pa et tau = 2.0 s (R² = 0.999)
8. L'utilisateur genere un rapport PDF
9. Le rapport contient les parametres identifies et les metadonnees Dublin Core

### 3.2 Scenario : Simulation FEM 3D d'une piece polymere (V2)

1. L'utilisateur se connecte et selectionne une organisation
2. Il cree un projet "Analyse Poutre HDPE"
3. Il importe un maillage tetraedrique (.msh)
4. Il soumet un job FEM 3D avec un modele Prony 3 branches
5. Le job est route vers le C++ compute engine (gRPC)
6. L'utilisateur suit la progression en temps reel via WebSocket
7. Une fois termine, il visualise le maillage deforme en 3D (Three.js)
8. Les champs de contraintes Von Mises sont affiches en echelle de couleur
9. Il exporte les resultats en format VTK pour post-traitement ParaView

### 3.3 Scenario : Collaboration en equipe (V2)

1. Le chef de projet cree une organisation "Equipe Polymeres BASF"
2. Il invite 3 collegues par email (2 MEMBER, 1 ADMIN)
3. Les collegues acceptent l'invitation via notification WebSocket
4. Le chef partage le projet "SBR 2024" avec role EDITOR pour 2 collegues
5. Le 3e collegue (stagiaire) recoit un acces VIEWER
6. Chaque modification est tracee dans l'audit trail
7. L'ADMIN peut consulter l'historique complet des actions

---

## 4. Contraintes et Limites

| Contrainte | V1 | V2 | V3 |
|------------|----|----|-----|
| Taille fichier max | 50 Mo | 50 Mo (MinIO) | 50 Mo |
| Jobs concurrents | 4 par instance | 4 par pod, scalable HPA | Idem |
| Modeles | 1D uniquement | 1D + FEM 3D | + Thermo-mecanique |
| ML points max | - | - | 10000 points/requete, 20 batch |
| Collaboration | Pas de partage | Organisations + roles | + CRDT temps reel (50 users/doc) |
| Notifications | Polling | WebSocket temps reel | + Push PWA |
| Deployment | Docker Compose | Kubernetes + Helm | + K3s edge, air-gapped |
| Monitoring | Actuator | Prometheus + Grafana | + k6 500 VUs |
| Load | ~10 users | 100+ users | 500+ users |
| Rate limit auth | 60/min | 60/min | 10/min (anti brute-force) |

---

## 5. Modules V3

### 5.1 Module ML Auto-Calibration

**Acteurs** : Ingenieur materiaux, Chercheur

| Fonctionnalite | Description |
|----------------|-------------|
| Prediction de modele | A partir d'une courbe experimentale, le ML suggere le type de loi constitutive (Maxwell, KV, Prony 2/3/4) avec un score de confiance |
| Estimation de parametres | Le reseau de neurones fournit des parametres initiaux estimes pour l'optimiseur LM |
| Alternatives | Presentation de 3 modeles alternatifs classes par confiance |
| Application directe | L'utilisateur peut appliquer la suggestion en 1 clic pour lancer l'identification |

**Regles metier** :
- Minimum 10 points de donnees requis
- Maximum 10000 points par requete
- Types d'experience autorises : relaxation, creep, oscillation, flow
- Si le service ML est indisponible, l'utilisateur peut toujours faire une identification manuelle

### 5.2 Module Marketplace

**Acteurs** : Tous les utilisateurs authentifies

| Fonctionnalite | Description |
|----------------|-------------|
| Catalogue | Navigation paginee dans les plugins disponibles |
| Recherche | Recherche full-text + filtrage par tags |
| Publication | Un auteur peut publier un plugin (nom, description, licence, tags) |
| Versions | Gestion des versions avec statut (PENDING, APPROVED, REJECTED) |
| Avis | Notation 1-5 etoiles + commentaire textuel |
| Telechargement | Compteur de telechargements, metric de popularite |

### 5.3 Module Billing SaaS

**Acteurs** : Utilisateurs souhaitant des fonctionnalites avancees

| Tier | Fonctionnalites | Quotas |
|------|----------------|--------|
| FREE | Simulation 1D, 3 projets, 5 materiaux | 100 simulations/mois |
| PRO | + FEM 3D, + ML calibration, projets illimites | 1000 simulations/mois |
| ENTERPRISE | + GPU compute, + marketplace upload, + support prioritaire | Illimite |

**Integration Stripe** : Checkout, portail client, webhooks verifies (HMAC-SHA256)

### 5.4 Module Collaboration CRDT

**Acteurs** : Equipes travaillant sur un meme projet

| Fonctionnalite | Description |
|----------------|-------------|
| Edition temps reel | Synchronisation CRDT binaire via WebSocket |
| Presence | Indicateur de qui est connecte au document (nom, couleur, curseur) |
| Reconnexion | Backoff exponentiel automatique (max 5 tentatives) |
| Limites | 50 utilisateurs simultanes par document |

### 5.5 Module Edge / PWA

**Acteurs** : Equipes en environnement deconnecte

| Fonctionnalite | Description |
|----------------|-------------|
| Installation offline | Script air-gapped pour K3s sans acces internet |
| PWA | Application installable, fonctionne hors-ligne (cache assets) |
| Push notifications | Alertes sur la fin de simulation, avis marketplace |

---

## 6. Glossaire

| Terme | Definition |
|-------|-----------|
| **Rheologie** | Science de l'ecoulement et de la deformation de la matiere |
| **Viscoelasticite** | Comportement intermediaire entre fluide visqueux et solide elastique |
| **Module de relaxation G(t)** | Rapport contrainte/deformation en fonction du temps apres une deformation imposee |
| **Compliance de fluage J(t)** | Rapport deformation/contrainte en fonction du temps sous contrainte constante |
| **Module de conservation G'** | Partie elastique (en phase) de la reponse en oscillation |
| **Module de perte G''** | Partie visqueuse (en quadrature) de la reponse en oscillation |
| **Serie de Prony** | Decomposition d'un spectre de relaxation en somme d'exponentielles |
| **Levenberg-Marquardt** | Algorithme d'optimisation non-lineaire pour moindres carres |
| **R²** | Coefficient de determination (qualite de l'ajustement, 1 = parfait) |
| **Dublin Core** | Standard de metadonnees pour la description de documents |
| **FEM** | Methode des Elements Finis (resolution d'EDP sur un maillage) |
| **Tetraedre P1** | Element fini volumique a 4 noeuds, interpolation lineaire |
| **Von Mises** | Critere de contrainte equivalente pour les materiaux ductiles |
| **gRPC** | Framework RPC haute performance (Google Protocol Buffers) |
| **CRDT** | Conflict-free Replicated Data Type (synchronisation sans conflit) |
| **ONNX** | Open Neural Network Exchange (format portable de modeles ML) |
| **XGBoost** | Algorithme de gradient boosting pour classification/regression |
| **WLF** | Williams-Landel-Ferry (modele de superposition temps-temperature) |
| **CSP** | Content Security Policy (en-tete HTTP de protection navigateur) |
| **PWA** | Progressive Web App (application web installable hors-ligne) |
| **K3s** | Distribution Kubernetes legere pour edge/IoT |
| **STOMP** | Protocole de messagerie texte sur WebSocket |
| **HPA** | Horizontal Pod Autoscaler (Kubernetes) |
| **OpenTelemetry** | Standard de traces distribuees et metriques |
| **Multi-tenant** | Architecture ou plusieurs organisations partagent une meme infrastructure avec isolation |
