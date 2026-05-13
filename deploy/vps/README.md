# RheoSim - Déploiement VPS

## Prérequis

- VPS avec minimum **4 Go RAM**, 2 vCPU, 40 Go SSD
- Ubuntu 22.04 ou Debian 12
- Nom de domaine pointant vers l'IP du VPS (enregistrement DNS A)
- Accès root ou sudo

## Déploiement rapide

```bash
# Sur le VPS en tant que root
curl -fsSL https://raw.githubusercontent.com/landry1996/rheosim-projet/main/deploy/vps/deploy.sh | bash -s -- votre-domaine.com
```

Ou manuellement :

```bash
git clone https://github.com/landry1996/rheosim-projet.git /opt/rheosim
cd /opt/rheosim/deploy/vps
chmod +x deploy.sh backup.sh
./deploy.sh votre-domaine.com
```

## Architecture de déploiement

```
Internet
    │
    ▼
┌──────────────────┐
│   Nginx Proxy    │ :80 (redirect) / :443 (SSL)
│   + Certbot      │
└────────┬─────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌──────────────┐
│Frontend│ │  Backend API │ :8080
│(static)│ │ (Spring Boot)│
└────────┘ └──────┬───────┘
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
┌──────────┐ ┌────────┐ ┌───────┐
│PostgreSQL│ │ Kafka  │ │ Redis │
│  :5432   │ │ :9092  │ │ :6379 │
└──────────┘ └────────┘ └───────┘
                  │
                  ▼
          ┌────────────┐
          │ ML Service │ :8000 / :50052
          │ (FastAPI)  │
          └────────────┘
```

## Fichiers

| Fichier | Description |
|---------|-------------|
| `docker-compose.yml` | Stack complète de production |
| `nginx/nginx.conf` | Configuration principale Nginx |
| `nginx/conf.d/rheosim.conf` | Virtual host avec SSL et proxy |
| `.env.production` | Template des variables d'environnement |
| `init-db.sql` | Initialisation du schéma PostgreSQL |
| `deploy.sh` | Script de déploiement automatisé |
| `backup.sh` | Sauvegarde/restauration PostgreSQL |

## Commandes utiles

```bash
cd /opt/rheosim/deploy/vps

# Voir les logs
docker compose logs -f
docker compose logs -f backend

# Redémarrer un service
docker compose restart backend

# Mise à jour
git pull origin main
docker compose up -d --build

# Sauvegarde manuelle
./backup.sh

# Restauration
./backup.sh restore /opt/rheosim-backups/rheosim_20260514.sql.gz

# Renouveler le certificat SSL (automatique via certbot container)
docker compose exec certbot certbot renew
```

## Sécurité

- Firewall UFW : seuls SSH, HTTP, HTTPS sont ouverts
- fail2ban : protection contre le brute-force SSH
- SSL/TLS 1.2+ avec Let's Encrypt (renouvellement automatique)
- Rate limiting Nginx : 30 req/s API, 5 req/min login
- Réseau interne Docker isolé (PostgreSQL, Kafka, Redis non exposés)
- Headers de sécurité : HSTS, X-Frame-Options, X-Content-Type-Options

## Monitoring

L'endpoint `/api/actuator/health` est accessible en interne.
Prometheus metrics disponibles sur `/api/actuator/prometheus` (non exposé publiquement).

Pour un monitoring externe, envisager :
- Uptime monitoring : UptimeRobot, Hetrix
- Logs : Loki + Grafana
- Metrics : Prometheus + Grafana
