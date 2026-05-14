# RheoSim — Guide Complet de Déploiement

## Table des matières

1. [Architecture du projet](#1-architecture-du-projet)
2. [Prérequis](#2-prérequis)
3. [Lancement en local (développement)](#3-lancement-en-local-développement)
4. [Lancement en local avec Docker](#4-lancement-en-local-avec-docker)
5. [Déploiement sur VPS en production](#5-déploiement-sur-vps-en-production)
6. [Maintenance et opérations](#6-maintenance-et-opérations)
7. [Dépannage](#7-dépannage)

---

## 1. Architecture du projet

```
rheosim-projet/
├── rheosim-backend/          # API Java 21 — Spring Boot 3.4.5 (architecture hexagonale)
│   ├── rheosim-domain/       # Entités métier, ports
│   ├── rheosim-application/  # Cas d'utilisation, services
│   ├── rheosim-infrastructure/ # Adaptateurs (JPA, Kafka, Redis, Elasticsearch)
│   └── rheosim-bootstrap/    # Point d'entrée Spring Boot, configurations
├── rheosim-frontend/         # SPA Angular 21 — TypeScript 5.9
├── rheosim-ml/               # Service ML — Python 3.11, FastAPI + gRPC
├── rheosim-compute/          # Moteur de calcul — C++17, Eigen3, OpenMP, gRPC
├── docker/                   # Docker Compose pour le développement local
├── deploy/vps/               # Configuration de déploiement VPS production
└── documentation/            # Documentation du projet
```

### Services et ports

| Service | Technologie | Port dev | Port prod |
|---------|-------------|----------|-----------|
| Backend API | Spring Boot 3.4.5 / Java 21 | 8080 | 8080 (interne) |
| Frontend | Angular 21 | 4200 | 443 (via Nginx) |
| ML Service | FastAPI / Python 3.11 | 8000 + 50052 (gRPC) | 8000 + 50052 (interne) |
| PostgreSQL | PostgreSQL 15 | 5432 | 5432 (interne) |
| Kafka | Apache Kafka 3.7 (KRaft) | 9092 | 9092 (interne) |
| Redis | Redis 7 | 6379 | 6379 (interne) |
| Kafka UI | kafka-ui (dev only) | 8090 | — |
| Nginx Proxy | Nginx Alpine | — | 80/443 |

---

## 2. Prérequis

### Pour le développement local (sans Docker)

| Outil | Version minimale | Vérification |
|-------|-----------------|--------------|
| Java JDK | 21 | `java --version` |
| Maven | 3.9+ | `mvn --version` |
| Node.js | 22.x | `node --version` |
| npm | 10.x | `npm --version` |
| Angular CLI | 21.x | `ng version` |
| Python | 3.11+ | `python --version` |
| PostgreSQL | 15 | `psql --version` |
| Git | 2.x | `git --version` |

### Pour Docker (local ou VPS)

| Outil | Version minimale | Vérification |
|-------|-----------------|--------------|
| Docker | 24.x | `docker --version` |
| Docker Compose | 2.20+ (plugin) | `docker compose version` |
| Git | 2.x | `git --version` |

### Pour le VPS en production

- **OS** : Ubuntu 22.04 LTS ou Debian 12
- **RAM** : 4 Go minimum (8 Go recommandé)
- **CPU** : 2 vCPU minimum
- **Disque** : 40 Go SSD minimum
- **Réseau** : IP publique fixe
- **Domaine** : Nom de domaine avec enregistrement DNS A pointant vers l'IP du VPS

---

## 3. Lancement en local (développement)

### 3.1 Cloner le dépôt

```bash
git clone https://github.com/landry1996/rheosim-projet.git
cd rheosim-projet
```

### 3.2 Démarrer l'infrastructure (PostgreSQL, Kafka, Redis)

Créer le fichier d'environnement :

```bash
cd docker
cp ../docker/.env.example .env
```

Éditer `.env` :

```env
POSTGRES_DB=rheosim_dev
POSTGRES_USER=rheosim
POSTGRES_PASSWORD=rheosim_dev_password
REDIS_PASSWORD=redis_dev_password
ML_API_KEY=dev-key-123
```

Lancer les services d'infrastructure :

```bash
docker compose up -d postgres kafka redis
```

Vérifier que tout est sain :

```bash
docker compose ps
# Attendre que tous les services soient "healthy"
```

### 3.3 Backend (Spring Boot)

#### Configuration des secrets

```bash
cd ../rheosim-backend/rheosim-bootstrap/src/main/resources
cp application-secret.example.yml application-secret.yml
```

Éditer `application-secret.yml` :

```yaml
spring:
  datasource:
    username: rheosim
    password: rheosim_dev_password

rheosim:
  security:
    jwt:
      secret-key: <générer avec: openssl rand -base64 64>
```

#### Compiler et lancer

```bash
cd rheosim-backend

# Compiler tout le projet (skip tests pour le premier lancement)
mvn clean install -DskipTests

# Lancer avec le profil dev
mvn -pl rheosim-bootstrap spring-boot:run -Dspring-boot.run.profiles=dev
```

Le backend sera accessible sur `http://localhost:8080/api`.

Vérification :

```bash
curl http://localhost:8080/api/actuator/health
# Réponse attendue: {"status":"UP"}
```

#### Swagger UI

Ouvrir dans le navigateur : `http://localhost:8080/api/swagger-ui.html`

### 3.4 Frontend (Angular)

```bash
cd rheosim-frontend

# Installer les dépendances
npm install

# Lancer le serveur de développement
npm start
# ou: ng serve
```

L'application sera accessible sur `http://localhost:4200`.

Le proxy Angular (configuré dans `environment.ts`) redirige les appels API vers `http://localhost:8080/api/v1`.

### 3.5 Service ML (Python)

```bash
cd rheosim-ml

# Créer un environnement virtuel
python -m venv .venv

# Activer l'environnement
# Linux/Mac:
source .venv/bin/activate
# Windows:
.venv\Scripts\activate

# Installer les dépendances
pip install -e ".[dev]"

# Lancer le service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Vérification :

```bash
curl http://localhost:8000/health
```

### 3.6 Moteur de calcul C++ (optionnel)

```bash
cd rheosim-compute

mkdir build && cd build

# Configuration CMake
cmake .. -DCMAKE_BUILD_TYPE=Debug \
         -DBUILD_TESTS=ON \
         -DUSE_GPU=OFF

# Compilation
cmake --build . --parallel $(nproc)

# Lancer les tests
ctest --output-on-failure
```

### 3.7 Résumé — Tout lancer en local

Terminal 1 — Infrastructure :
```bash
cd docker && docker compose up -d postgres kafka redis
```

Terminal 2 — Backend :
```bash
cd rheosim-backend && mvn -pl rheosim-bootstrap spring-boot:run -Dspring-boot.run.profiles=dev
```

Terminal 3 — Frontend :
```bash
cd rheosim-frontend && npm start
```

Terminal 4 — ML Service :
```bash
cd rheosim-ml && source .venv/bin/activate && uvicorn app.main:app --port 8000 --reload
```

Accès : `http://localhost:4200`

---

## 4. Lancement en local avec Docker (tout conteneurisé)

Cette méthode lance **tous les services** dans des conteneurs Docker, sans installer Java, Node ou Python sur la machine hôte.

### 4.1 Configuration

```bash
cd docker
cp .env.example .env
```

Éditer `.env` avec des mots de passe (même faibles en dev) :

```env
POSTGRES_DB=rheosim_dev
POSTGRES_USER=rheosim
POSTGRES_PASSWORD=dev_password_123
REDIS_PASSWORD=redis_dev_123
ML_API_KEY=dev-key
```

### 4.2 Lancer la stack complète

```bash
# Depuis le répertoire docker/
docker compose -f docker-compose.yml up -d --build
```

Cela lance : PostgreSQL, Kafka, Redis, ML Service, et Kafka UI.

Pour le backend et frontend en conteneurs aussi :

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### 4.3 Vérification

```bash
# Voir l'état de tous les services
docker compose ps

# Voir les logs en temps réel
docker compose logs -f

# Tester un service spécifique
docker compose logs backend
```

### 4.4 Arrêter tout

```bash
docker compose down

# Pour supprimer aussi les volumes (données) :
docker compose down -v
```

---

## 5. Déploiement sur VPS en production

### 5.1 Méthode automatisée (recommandée)

#### Étape 1 : Préparer le DNS

Chez votre registraire DNS, créer un enregistrement :

```
Type: A
Nom: rheosim (ou @ pour le domaine nu)
Valeur: <IP_DU_VPS>
TTL: 300
```

Attendre la propagation (5-30 minutes) :

```bash
dig +short rheosim.votre-domaine.com
# Doit afficher l'IP de votre VPS
```

#### Étape 2 : Se connecter au VPS

```bash
ssh root@<IP_DU_VPS>
```

#### Étape 3 : Lancer le script de déploiement

```bash
# Télécharger et exécuter le script
curl -fsSL https://raw.githubusercontent.com/landry1996/rheosim-projet/main/deploy/vps/deploy.sh -o deploy.sh
chmod +x deploy.sh
./deploy.sh rheosim.votre-domaine.com
```

Le script effectue automatiquement :
1. Vérification des ressources système (RAM >= 4 Go)
2. Installation de Docker, Git, UFW, fail2ban
3. Configuration du firewall (SSH + HTTP + HTTPS uniquement)
4. Configuration de fail2ban contre le brute-force
5. Clonage du dépôt dans `/opt/rheosim`
6. Génération automatique des secrets (PostgreSQL, JWT, Redis)
7. Obtention du certificat SSL via Let's Encrypt
8. Construction et lancement de tous les services
9. Configuration d'une sauvegarde quotidienne automatique

#### Étape 4 : Configurer les variables restantes

```bash
nano /opt/rheosim/deploy/vps/.env
```

Modifier les valeurs optionnelles :
- `STRIPE_WEBHOOK_SECRET` — si vous utilisez la facturation
- `SMTP_*` — si vous envoyez des emails
- `ML_API_KEY` — clé d'accès au service ML

Relancer après modification :

```bash
cd /opt/rheosim/deploy/vps
docker compose down && docker compose up -d
```

### 5.2 Méthode manuelle (pas à pas)

#### Étape 1 : Préparer le serveur

```bash
# Mise à jour du système
apt update && apt upgrade -y

# Installer les dépendances
apt install -y git curl wget ufw fail2ban ca-certificates gnupg lsb-release

# Configurer le firewall
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable

# Configurer fail2ban
cat > /etc/fail2ban/jail.local <<'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
logpath = /var/log/auth.log
EOF
systemctl enable fail2ban && systemctl restart fail2ban
```

#### Étape 2 : Installer Docker

```bash
# Ajouter le dépôt Docker officiel
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Vérifier l'installation
docker --version
docker compose version
```

#### Étape 3 : Cloner le projet

```bash
git clone https://github.com/landry1996/rheosim-projet.git /opt/rheosim
cd /opt/rheosim/deploy/vps
```

#### Étape 4 : Configurer l'environnement

```bash
cp .env.production .env
```

Éditer `.env` et remplir TOUTES les valeurs :

```bash
nano .env
```

```env
# OBLIGATOIRE — Remplacer toutes les valeurs
DOMAIN_NAME=rheosim.votre-domaine.com
POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
JWT_SECRET=$(openssl rand -base64 64 | tr -d '/+=' | head -c 64)
CORS_ORIGINS=https://rheosim.votre-domaine.com
```

Générer les secrets en une commande :

```bash
# Génération interactive
PG_PASS=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
REDIS_PASS=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
JWT=$(openssl rand -base64 64 | tr -d '/+=' | head -c 64)

echo "POSTGRES_PASSWORD=$PG_PASS"
echo "REDIS_PASSWORD=$REDIS_PASS"
echo "JWT_SECRET=$JWT"
```

#### Étape 5 : Mettre à jour la configuration Nginx

```bash
# Remplacer le placeholder de domaine
DOMAIN="rheosim.votre-domaine.com"
sed -i "s/\${DOMAIN_NAME}/$DOMAIN/g" nginx/conf.d/rheosim.conf
```

#### Étape 6 : Obtenir le certificat SSL

```bash
# Créer les répertoires
mkdir -p certbot/www certbot/conf

# Lancer un Nginx temporaire pour le challenge ACME
docker run -d --name certbot-nginx \
  -p 80:80 \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  nginx:alpine \
  sh -c 'echo "server { listen 80; location /.well-known/acme-challenge/ { root /var/www/certbot; } location / { return 444; } }" > /etc/nginx/conf.d/default.conf && nginx -g "daemon off;"'

# Attendre 3 secondes que Nginx démarre
sleep 3

# Demander le certificat
docker run --rm \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  --email admin@votre-domaine.com \
  --agree-tos --no-eff-email \
  -d $DOMAIN

# Nettoyer le Nginx temporaire
docker stop certbot-nginx && docker rm certbot-nginx
```

#### Étape 7 : Construire et lancer

```bash
# Construire et lancer tous les services
docker compose --env-file .env up -d --build

# Suivre les logs pendant le démarrage
docker compose logs -f
```

#### Étape 8 : Vérifier le déploiement

```bash
# État des services
docker compose ps

# Tester le health check backend
docker compose exec backend wget -qO- http://localhost:8080/api/actuator/health

# Tester l'accès HTTPS externe
curl -I https://rheosim.votre-domaine.com
```

### 5.3 Vérification post-déploiement

| Test | Commande | Résultat attendu |
|------|----------|------------------|
| Site accessible | `curl -I https://DOMAIN` | HTTP/2 200 |
| API health | `curl https://DOMAIN/api/actuator/health` | `{"status":"UP"}` |
| SSL valide | `curl -vI https://DOMAIN 2>&1 \| grep "SSL certificate"` | Certificate OK |
| Tous containers UP | `docker compose ps` | Tous "running (healthy)" |
| Base de données | `docker exec rheosim-postgres pg_isready` | Accepting connections |

---

## 6. Maintenance et opérations

### 6.1 Mise à jour de l'application

```bash
cd /opt/rheosim

# Récupérer les dernières modifications
git pull origin main

# Reconstruire et relancer
cd deploy/vps
docker compose up -d --build

# Vérifier que tout redémarre correctement
docker compose ps
docker compose logs -f --tail=50
```

### 6.2 Sauvegardes

Les sauvegardes automatiques sont configurées quotidiennement dans `/opt/rheosim-backups/`.

```bash
# Sauvegarde manuelle
cd /opt/rheosim/deploy/vps
./backup.sh

# Lister les sauvegardes
ls -lh /opt/rheosim-backups/

# Restaurer une sauvegarde
./backup.sh restore /opt/rheosim-backups/rheosim_20260514_120000.sql.gz
```

### 6.3 Renouvellement SSL

Le certificat SSL est renouvelé automatiquement par le conteneur Certbot (toutes les 12h, il vérifie si le renouvellement est nécessaire).

Pour forcer un renouvellement :

```bash
docker compose exec certbot certbot renew --force-renewal
docker compose restart nginx-proxy
```

### 6.4 Logs

```bash
# Tous les services
docker compose logs -f

# Un service spécifique
docker compose logs -f backend
docker compose logs -f postgres
docker compose logs -f nginx-proxy

# Les 100 dernières lignes
docker compose logs --tail=100 backend
```

### 6.5 Scaling

Pour augmenter les ressources d'un service, éditer `docker-compose.yml` :

```yaml
backend:
  deploy:
    resources:
      limits:
        memory: 2G  # Augmenté de 1G à 2G
```

Puis :

```bash
docker compose up -d backend
```

### 6.6 Monitoring système

```bash
# Utilisation mémoire par conteneur
docker stats --no-stream

# Espace disque
df -h

# Utilisation des volumes Docker
docker system df
```

---

## 7. Dépannage

### Le backend ne démarre pas

```bash
# Vérifier les logs
docker compose logs backend

# Causes fréquentes :
# 1. PostgreSQL pas encore prêt → attendre le healthcheck
# 2. Mot de passe incorrect → vérifier .env
# 3. Mémoire insuffisante → vérifier avec: docker stats
```

### Erreur 502 Bad Gateway

```bash
# Vérifier que le backend est healthy
docker compose ps backend

# Vérifier la connectivité réseau interne
docker compose exec nginx-proxy wget -qO- http://backend:8080/api/actuator/health
```

### Certificat SSL expiré

```bash
# Vérifier la date d'expiration
docker compose exec certbot certbot certificates

# Forcer le renouvellement
docker compose exec certbot certbot renew --force-renewal
docker compose restart nginx-proxy
```

### Base de données corrompue

```bash
# Restaurer depuis la dernière sauvegarde
docker compose stop backend ml-service
./backup.sh restore /opt/rheosim-backups/<dernier_backup>.sql.gz
docker compose start backend ml-service
```

### Kafka ne démarre pas

```bash
# Kafka a besoin de temps au premier démarrage (60s)
docker compose logs kafka

# Si le volume est corrompu :
docker compose down
docker volume rm rheosim-projet_kafka_data
docker compose up -d
```

### Espace disque plein

```bash
# Nettoyer les images Docker non utilisées
docker system prune -a

# Nettoyer les vieilles sauvegardes
find /opt/rheosim-backups -name "*.sql.gz" -mtime +30 -delete

# Vérifier les logs Docker
du -sh /var/lib/docker/containers/*
```

### Le frontend affiche une page blanche

```bash
# Vérifier que les fichiers statiques sont dans le volume
docker compose exec nginx-proxy ls /usr/share/nginx/html/app/

# Reconstruire le frontend
docker compose build frontend
docker compose up -d frontend nginx-proxy
```

---

## Annexe A — Variables d'environnement

| Variable | Obligatoire | Description | Exemple |
|----------|-------------|-------------|---------|
| `DOMAIN_NAME` | Oui (prod) | Nom de domaine | `rheosim.example.com` |
| `POSTGRES_DB` | Oui | Nom de la base | `rheosim` |
| `POSTGRES_USER` | Oui | Utilisateur PostgreSQL | `rheosim` |
| `POSTGRES_PASSWORD` | Oui | Mot de passe PostgreSQL | `(généré)` |
| `REDIS_PASSWORD` | Oui | Mot de passe Redis | `(généré)` |
| `JWT_SECRET` | Oui | Secret JWT (64+ chars) | `(généré)` |
| `BACKEND_MAX_HEAP` | Non | Heap max Java | `512m` |
| `CORS_ORIGINS` | Oui (prod) | Origines CORS autorisées | `https://rheosim.example.com` |
| `ML_API_KEY` | Non | Clé API service ML | `(généré)` |
| `STRIPE_WEBHOOK_SECRET` | Non | Secret webhook Stripe | `whsec_xxx` |
| `SMTP_HOST` | Non | Serveur SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Non | Port SMTP | `587` |

## Annexe B — Ports et réseaux

### Réseau `rheosim-net` (accessible par le proxy)

- `nginx-proxy` : 80, 443 (exposés sur l'hôte)
- `backend` : 8080 (interne)
- `frontend` : 80 (interne)

### Réseau `rheosim-internal` (isolé, jamais exposé)

- `postgres` : 5432
- `kafka` : 9092, 9093
- `redis` : 6379
- `ml-service` : 8000, 50052
- `backend` : (double réseau, peut accéder aux deux)

## Annexe C — Checklist de déploiement

- [ ] DNS A record configuré et propagé
- [ ] VPS accessible en SSH
- [ ] Script `deploy.sh` exécuté avec succès
- [ ] `.env` complété avec tous les secrets
- [ ] Certificat SSL obtenu (`https://` fonctionne)
- [ ] Backend health check OK (`/api/actuator/health`)
- [ ] Frontend accessible et fonctionnel
- [ ] Sauvegarde automatique configurée
- [ ] Firewall actif (vérifier avec `ufw status`)
- [ ] fail2ban actif (vérifier avec `fail2ban-client status`)
- [ ] Test de charge léger effectué
- [ ] Monitoring externe configuré (UptimeRobot, etc.)
