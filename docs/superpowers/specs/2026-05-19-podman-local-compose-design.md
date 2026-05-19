# Design : docker-compose local pour Podman

**Date :** 2026-05-19  
**Scope :** Fichier Compose unique pour lancer RheoSim en local avec `podman-compose` (rootless)

---

## Contexte

Le projet dispose de plusieurs fichiers Compose existants orientés CI/VPS (`docker/docker-compose.yml`, `docker/docker-compose.prod.yml`, `deploy/vps/docker-compose.yml`). Aucun n'est adapté à Podman rootless pour un usage développeur local.

---

## Périmètre

**Inclus :**
- Infrastructure : PostgreSQL 15, Kafka (KRaft, bitnami:3.7), Redis 7, Kafka UI
- Services applicatifs : backend Spring Boot, frontend Angular/nginx, ml-service FastAPI+gRPC

**Exclus :**
- Compute C++ (`rheosim-compute`) — non requis en dev
- Stack monitoring (Prometheus, Grafana, Loki, Jaeger) — optionnelle, déjà dans `deploy/monitoring/`
- Nginx reverse proxy — inutile en local, chaque service expose directement son port

---

## Fichiers créés

| Fichier | Rôle |
|---|---|
| `docker/docker-compose.local.yml` | Compose adapté Podman rootless |
| `docker/.env.local.example` | Template des variables d'environnement |

---

## Architecture

### Services et ports

| Service | Image/Source | Port host (127.0.0.1) | Dépendances |
|---|---|---|---|
| `postgres` | postgres:15-alpine | 5432 | — |
| `kafka` | bitnami/kafka:3.7 | 9092 | — |
| `redis` | redis:7-alpine | 6379 | — |
| `kafka-ui` | provectuslabs/kafka-ui | 8090 | kafka healthy |
| `ml-service` | build `../rheosim-ml` | 8000, 50052 | — |
| `backend` | build `../rheosim-backend` | 8080 | postgres + kafka + redis healthy |
| `frontend` | build `../rheosim-frontend` | 80 | backend healthy |

### Réseau

Un seul réseau `rheosim-net` (bridge). Pas de réseau `internal` séparé : en rootless Podman, l'isolation inter-réseaux n'est pas garantie et crée une fausse impression de sécurité.

Les services se parlent via leurs noms de service (ex. `kafka:9092`, `postgres:5432`).

---

## Adaptations Podman rootless

1. **Pas de `version:`** — déprécié dans la spec Compose actuelle.
2. **Pas de `deploy.resources.limits`** — non supporté par `podman-compose` (mode Swarm uniquement). Retiré pour éviter les warnings et erreurs silencieuses.
3. **Label `:z` sur les bind mounts** — relabellise pour SELinux. Appliqué à `init-db.sql:ro,z`. Les volumes nommés n'en ont pas besoin.
4. **`KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092`** — communication inter-container via nom de service (pas `localhost`).
5. **`$$` dans les healthchecks** — conservé : podman-compose gère l'échappement de la même façon que Docker Compose.
6. **Tous les ports liés à `127.0.0.1`** — évite l'exposition sur toutes les interfaces (best practice en dev local).

---

## Variables d'environnement

### Obligatoires (`:?` = erreur au démarrage si absente)

| Variable | Usage |
|---|---|
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL |
| `REDIS_PASSWORD` | Mot de passe Redis |
| `JWT_SECRET` | Secret JWT Spring Security (≥ 32 caractères) |

### Avec valeur par défaut

| Variable | Défaut | Usage |
|---|---|---|
| `POSTGRES_DB` | `rheosim_dev` | Nom de la base |
| `POSTGRES_USER` | `rheosim` | Utilisateur PostgreSQL |
| `ML_API_KEY` | *(vide)* | Clé API ML service (optionnelle) |

---

## Utilisation

```bash
# 1. Créer le fichier .env
cp docker/.env.local.example docker/.env
# Éditer docker/.env et remplir les 3 variables obligatoires

# 2. Lancer (premier lancement : build des images)
podman-compose -f docker/docker-compose.local.yml up -d --build

# 3. Relancer sans rebuild
podman-compose -f docker/docker-compose.local.yml up -d

# 4. Arrêter
podman-compose -f docker/docker-compose.local.yml down

# 5. Arrêter et supprimer les volumes
podman-compose -f docker/docker-compose.local.yml down -v
```

---

## Décisions de design

- **`SPRING_PROFILES_ACTIVE: dev`** sur le backend : active les logs détaillés et désactive les contraintes HTTPS côté Spring.
- **`APP_CORS_ALLOWED_ORIGINS: http://localhost`** : permet au frontend servi sur le port 80 d'appeler le backend sur 8080.
- **`read_only: true` + `tmpfs: /tmp`** sur ml-service : conservé depuis le compose existant, compatible Podman.
- **Kafka UI exposé sur 8090** : évite le conflit avec Grafana (3000) et autres outils courants.
