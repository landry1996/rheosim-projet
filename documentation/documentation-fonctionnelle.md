# RheoSim Enterprise — Documentation Fonctionnelle

## 1. Presentation Generale

### 1.1 Objectif

RheoSim Enterprise est une plateforme scientifique de simulation viscoelastique destinee aux ingenieurs materiaux et chercheurs en rheologie. Elle permet d'importer des donnees experimentales, d'identifier les parametres de lois de comportement constitutives, et de generer des rapports professionnels.

### 1.2 Perimetre V1

| Capacite | Description |
|----------|-------------|
| Simulation 1D/2D | Modeles viscoelastiques lineaires (Maxwell, Kelvin-Voigt, series de Prony) |
| Import de donnees | CSV et Excel (fluage, relaxation, oscillation, courbes d'ecoulement) |
| Identification parametrique | Algorithme de Levenberg-Marquardt avec contraintes bornees |
| Generation de rapports | PDF, CSV, JSON avec metadonnees Dublin Core |
| Multi-utilisateurs | Authentification JWT, roles (USER, ADMIN), projets separes |

### 1.3 Utilisateurs Cibles

- **Ingenieur materiaux** : importe ses donnees experimentales, lance des identifications parametriques, genere des rapports
- **Chercheur en rheologie** : calibre des modeles constitutifs, compare les resultats avec des donnees experimentales
- **Responsable qualite** : consulte les rapports generes, verifie la convergence des identifications

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
| Rate Limiting | 60 requetes/minute par IP (protection contre brute force). |
| Audit | Chaque action d'authentification est tracee (table audit_logs). |

**Regles metier** :
- Mot de passe minimum 8 caracteres
- Email doit etre unique dans le systeme
- Refresh token a usage unique (rotation a chaque utilisation)
- Les tokens expires sont nettoyes periodiquement

### 2.2 Module Projets (Project)

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Creer un projet | Nom + description. Statut initial = DRAFT. |
| Activer un projet | Passage de DRAFT a ACTIVE. |
| Archiver un projet | Passage a ARCHIVED (lecture seule). |
| Lister les projets | Tous les projets de l'utilisateur connecte. |
| Associer des materiaux | Un projet contient N materiaux. |

**Cycle de vie** :
```
DRAFT → ACTIVE → ARCHIVED
```

**Regles metier** :
- Un projet ne peut etre active que s'il contient au moins un materiau
- Un projet archive ne peut plus etre modifie
- Seul le proprietaire peut modifier son projet

### 2.3 Module Materiaux (Material)

**Acteurs** : Utilisateur authentifie

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
- Power-Law (prevu V2)
- Carreau-Yasuda (prevu V2)

### 2.4 Module Donnees Experimentales (Experiment)

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Importer un fichier | Upload CSV ou Excel (max 50 Mo). |
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

**Acteurs** : Utilisateur authentifie

| Fonctionnalite | Description |
|----------------|-------------|
| Soumettre un job | Choix du materiau, dataset, type de simulation, cible d'ajustement. |
| Suivi du statut | Polling du statut (QUEUED → RUNNING → COMPLETED/FAILED). |
| Consulter les resultats | Parametres identifies, R², nb iterations, courbe ajustee. |
| Annuler un job | Annulation d'un job en attente ou en cours. |

**Types de simulation** :
- **PARAMETER_IDENTIFICATION** : identification des parametres d'un modele constitutif a partir de donnees experimentales
- **FORWARD_SIMULATION** : calcul de la reponse d'un modele avec des parametres donnes (prevu)
- **SENSITIVITY_ANALYSIS** : analyse de sensibilite parametrique (prevu)

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

**Regles metier** :
- Maximum 4 jobs concurrents par instance
- Timeout d'un job : 1 heure
- Un job CANCELLED ne peut pas etre relance

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

**Cycle de vie** :
```
GENERATING → READY / FAILED → EXPIRED
```

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

---

## 4. Contraintes et Limites V1

| Contrainte | Detail |
|------------|--------|
| Taille fichier max | 50 Mo par upload |
| Jobs concurrents | 4 maximum par instance |
| Modeles 1D uniquement | Pas de FEM 3D en V1 |
| Pas de collaboration | Un projet = un proprietaire |
| Pas de versioning datasets | Un dataset importe est immuable |
| Formats rapport | PDF, CSV, JSON (pas de DOCX) |

---

## 5. Glossaire

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
