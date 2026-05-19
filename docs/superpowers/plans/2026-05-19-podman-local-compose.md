# Podman Local Compose Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Créer `docker/docker-compose.local.yml` et `docker/.env.local.example` pour lancer RheoSim (infra + services applicatifs) avec `podman-compose` en mode rootless.

**Architecture:** Fichier Compose unique sans `version:`, réseau bridge unique `rheosim-net`, ports liés à `127.0.0.1`. Adaptations Podman : suppression des blocs `deploy.resources`, label `:z` sur les bind mounts de fichiers, profil Spring `dev` sur le backend.

**Tech Stack:** podman-compose ≥ 1.0, Compose Spec (sans `version:`), Podman rootless

---

## Fichiers créés/modifiés

| Action | Fichier | Contenu |
|---|---|---|
| Créer | `docker/.env.local.example` | Template des variables d'env avec commentaires |
| Créer | `docker/docker-compose.local.yml` | Compose adapté Podman : 7 services, 1 réseau, 4 volumes |

---

## Task 1 : Créer le fichier `.env.local.example`

**Files:**
- Create: `docker/.env.local.example`

- [ ] **Step 1 : Écrire le fichier**

```
# docker/.env.local.example
# Copier vers docker/.env et remplir les valeurs

# --- Obligatoires (le compose échoue au démarrage si absent) ---
POSTGRES_PASSWORD=devpassword123
REDIS_PASSWORD=devredispass123
JWT_SECRET=local-dev-secret-min-32-characters-ok

# --- Optionnels (valeur par défaut utilisée si absent) ---
POSTGRES_DB=rheosim_dev
POSTGRES_USER=rheosim
ML_API_KEY=
```

- [ ] **Step 2 : Copier vers `.env` pour les tests locaux**

```bash
cp docker/.env.local.example docker/.env
```

Vérifier que `docker/.env` est dans `.gitignore` :

```bash
grep "\.env" .gitignore
```

Attendu : une ligne contenant `*.env` ou `docker/.env`.

---

## Task 2 : Créer `docker-compose.local.yml` — infra

**Files:**
- Create: `docker/docker-compose.local.yml`

- [ ] **Step 1 : Créer le fichier avec les 3 services d'infrastructure**

Contenu de `docker/docker-compose.local.yml` :

```yaml
services:

  # ─────────────────────────────────────────
  # INFRASTRUCTURE
  # ─────────────────────────────────────────

  postgres:
    image: postgres:15-alpine
    container_name: rheosim-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-rheosim_dev}
      POSTGRES_USER: ${POSTGRES_USER:-rheosim}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set}
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql:ro,z
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s
    networks:
      - rheosim-net

  kafka:
    image: bitnami/kafka:3.7
    container_name: rheosim-kafka
    environment:
      KAFKA_CFG_NODE_ID: 1
      KAFKA_CFG_PROCESS_ROLES: broker,controller
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "127.0.0.1:9092:9092"
    volumes:
      - kafka_data:/bitnami/kafka
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions.sh --bootstrap-server localhost:9092"]
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 30s
    networks:
      - rheosim-net

  redis:
    image: redis:7-alpine
    container_name: rheosim-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:?REDIS_PASSWORD must be set} --maxmemory 256mb --maxmemory-policy allkeys-lru
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - rheosim-net

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: rheosim-kafka-ui
    environment:
      KAFKA_CLUSTERS_0_NAME: rheosim-local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    ports:
      - "127.0.0.1:8090:8080"
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - rheosim-net
```

- [ ] **Step 2 : Valider la syntaxe YAML de l'infra**

```bash
cd docker
podman-compose -f docker-compose.local.yml config
```

Attendu : sortie YAML normalisée sans erreur. Si `podman-compose` n'est pas installé :
```bash
pip install podman-compose
```

---

## Task 3 : Ajouter les services applicatifs dans `docker-compose.local.yml`

**Files:**
- Modify: `docker/docker-compose.local.yml`

- [ ] **Step 1 : Ajouter ml-service, backend, frontend et les déclarations volumes/networks**

Ajouter à la fin du fichier `docker/docker-compose.local.yml`, après le bloc `kafka-ui` :

```yaml

  # ─────────────────────────────────────────
  # SERVICES APPLICATIFS
  # ─────────────────────────────────────────

  ml-service:
    build:
      context: ../rheosim-ml
      dockerfile: Dockerfile
    container_name: rheosim-ml
    environment:
      RHEOSIM_ML_GRPC_PORT: "50052"
      RHEOSIM_ML_HTTP_PORT: "8000"
      RHEOSIM_ML_MODEL_DIR: "/app/models"
      RHEOSIM_ML_API_KEY: ${ML_API_KEY:-}
    ports:
      - "127.0.0.1:8000:8000"
      - "127.0.0.1:50052:50052"
    volumes:
      - ml_models:/app/models
    read_only: true
    tmpfs:
      - /tmp
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s
    networks:
      - rheosim-net

  backend:
    build:
      context: ../rheosim-backend
      dockerfile: Dockerfile
    container_name: rheosim-backend
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-rheosim_dev}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-rheosim}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be set}
      APP_CORS_ALLOWED_ORIGINS: "http://localhost"
    ports:
      - "127.0.0.1:8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    networks:
      - rheosim-net

  frontend:
    build:
      context: ../rheosim-frontend
      dockerfile: Dockerfile
    container_name: rheosim-frontend
    ports:
      - "127.0.0.1:80:80"
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s
    networks:
      - rheosim-net

volumes:
  postgres_data:
  kafka_data:
  redis_data:
  ml_models:

networks:
  rheosim-net:
    driver: bridge
```

- [ ] **Step 2 : Valider le fichier complet**

```bash
cd docker
podman-compose -f docker-compose.local.yml config
```

Attendu : sortie YAML complète avec les 7 services, sans erreur ni warning sur les variables `:?` (le `.env` copié à Task 1 fournit les valeurs).

---

## Task 4 : Vérifier `.gitignore` et committer

**Files:**
- Possibly modify: `.gitignore`

- [ ] **Step 1 : S'assurer que `docker/.env` est ignoré par git**

```bash
grep -n "\.env" .gitignore
```

Si `docker/.env` n'est pas couvert, ajouter la ligne :

```
docker/.env
```

Ne pas committer `docker/.env` (contient des secrets locaux).

- [ ] **Step 2 : Vérifier que seuls les bons fichiers sont staged**

```bash
git status
```

Attendu (fichiers à committer) :
```
docs/superpowers/specs/2026-05-19-podman-local-compose-design.md
docs/superpowers/plans/2026-05-19-podman-local-compose.md
docker/.env.local.example
docker/docker-compose.local.yml
```

`docker/.env` ne doit PAS apparaître.

- [ ] **Step 3 : Committer**

```bash
git add docs/superpowers/specs/2026-05-19-podman-local-compose-design.md \
        docs/superpowers/plans/2026-05-19-podman-local-compose.md \
        docker/.env.local.example \
        docker/docker-compose.local.yml
git commit -m "feat: add Podman-compatible local docker-compose"
```

---

## Référence rapide — commandes podman-compose

```bash
# Premier lancement (build des images)
podman-compose -f docker/docker-compose.local.yml up -d --build

# Relancer sans rebuild
podman-compose -f docker/docker-compose.local.yml up -d

# Voir les logs en temps réel
podman-compose -f docker/docker-compose.local.yml logs -f

# Logs d'un seul service
podman-compose -f docker/docker-compose.local.yml logs -f backend

# Arrêter (conserve les volumes)
podman-compose -f docker/docker-compose.local.yml down

# Arrêter et supprimer les volumes (reset complet)
podman-compose -f docker/docker-compose.local.yml down -v

# Statut des services
podman-compose -f docker/docker-compose.local.yml ps
```

## URLs locales après démarrage

| Service | URL |
|---|---|
| Frontend Angular | http://localhost |
| Backend Spring Boot | http://localhost:8080/actuator/health |
| ML Service (HTTP) | http://localhost:8000/health |
| Kafka UI | http://localhost:8090 |
