#!/bin/bash
set -euo pipefail

# =============================================================
# RheoSim VPS Deployment Script
# Tested on: Ubuntu 22.04 / Debian 12
# Requirements: Fresh VPS with root or sudo access, min 4GB RAM
# =============================================================

REPO_URL="https://github.com/landry1996/rheosim-projet.git"
INSTALL_DIR="/opt/rheosim"
DOMAIN=""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[RHEOSIM]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- Pre-checks ---
check_root() {
    if [ "$EUID" -ne 0 ]; then
        error "This script must be run as root (sudo ./deploy.sh)"
    fi
}

check_resources() {
    local mem_total
    mem_total=$(free -m | awk '/^Mem:/{print $2}')
    if [ "$mem_total" -lt 3500 ]; then
        warn "System has ${mem_total}MB RAM. Minimum recommended: 4GB."
        read -p "Continue anyway? (y/N): " -r
        [[ $REPLY =~ ^[Yy]$ ]] || exit 1
    fi
    log "System RAM: ${mem_total}MB - OK"
}

# --- Installation ---
install_docker() {
    if command -v docker &>/dev/null; then
        log "Docker already installed: $(docker --version)"
        return
    fi

    log "Installing Docker..."
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg lsb-release

    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
        https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
        tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin

    systemctl enable docker
    systemctl start docker
    log "Docker installed successfully"
}

install_dependencies() {
    log "Installing system dependencies..."
    apt-get update -qq
    apt-get install -y -qq git ufw fail2ban curl wget unzip
}

setup_firewall() {
    log "Configuring firewall (UFW)..."
    ufw default deny incoming
    ufw default allow outgoing
    ufw allow ssh
    ufw allow 80/tcp
    ufw allow 443/tcp
    echo "y" | ufw enable
    log "Firewall configured: SSH, HTTP, HTTPS allowed"
}

setup_fail2ban() {
    log "Configuring fail2ban..."
    cat > /etc/fail2ban/jail.local <<'JAIL'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
logpath = /var/log/auth.log
JAIL
    systemctl enable fail2ban
    systemctl restart fail2ban
    log "fail2ban configured"
}

clone_repo() {
    if [ -d "$INSTALL_DIR" ]; then
        log "Updating existing installation..."
        cd "$INSTALL_DIR"
        git pull origin main
    else
        log "Cloning repository..."
        git clone "$REPO_URL" "$INSTALL_DIR"
    fi
}

setup_env() {
    local env_file="$INSTALL_DIR/deploy/vps/.env"

    if [ -f "$env_file" ]; then
        log ".env already exists, skipping..."
        return
    fi

    log "Setting up environment variables..."
    cp "$INSTALL_DIR/deploy/vps/.env.production" "$env_file"

    # Auto-generate secrets
    local pg_pass jwt_secret redis_pass
    pg_pass=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)
    jwt_secret=$(openssl rand -base64 64 | tr -d '/+=' | head -c 64)
    redis_pass=$(openssl rand -base64 32 | tr -d '/+=' | head -c 32)

    sed -i "s|POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=${pg_pass}|" "$env_file"
    sed -i "s|JWT_SECRET=.*|JWT_SECRET=${jwt_secret}|" "$env_file"
    sed -i "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=${redis_pass}|" "$env_file"
    sed -i "s|DOMAIN_NAME=.*|DOMAIN_NAME=${DOMAIN}|" "$env_file"
    sed -i "s|CORS_ORIGINS=.*|CORS_ORIGINS=https://${DOMAIN}|" "$env_file"

    warn "Edit $env_file to configure remaining values (Stripe, email, etc.)"
}

setup_ssl_initial() {
    local deploy_dir="$INSTALL_DIR/deploy/vps"

    log "Obtaining initial SSL certificate..."

    # Create temporary nginx config for ACME challenge only
    mkdir -p "$deploy_dir/certbot/www" "$deploy_dir/certbot/conf"

    # Start a minimal nginx for domain validation
    docker run -d --name certbot-init-nginx \
        -p 80:80 \
        -v "$deploy_dir/certbot/www:/var/www/certbot" \
        nginx:alpine \
        sh -c 'echo "server { listen 80; location /.well-known/acme-challenge/ { root /var/www/certbot; } location / { return 444; } }" > /etc/nginx/conf.d/default.conf && nginx -g "daemon off;"'

    sleep 3

    # Request certificate
    docker run --rm \
        -v "$deploy_dir/certbot/conf:/etc/letsencrypt" \
        -v "$deploy_dir/certbot/www:/var/www/certbot" \
        certbot/certbot certonly \
        --webroot --webroot-path=/var/www/certbot \
        --email "admin@${DOMAIN}" \
        --agree-tos --no-eff-email \
        -d "$DOMAIN"

    # Clean up
    docker stop certbot-init-nginx && docker rm certbot-init-nginx

    log "SSL certificate obtained for ${DOMAIN}"
}

process_nginx_template() {
    local deploy_dir="$INSTALL_DIR/deploy/vps"
    local conf_file="$deploy_dir/nginx/conf.d/rheosim.conf"

    sed -i "s/\${DOMAIN_NAME}/${DOMAIN}/g" "$conf_file"
    log "Nginx configuration updated with domain: ${DOMAIN}"
}

start_services() {
    local deploy_dir="$INSTALL_DIR/deploy/vps"
    cd "$deploy_dir"

    log "Building and starting services..."
    docker compose --env-file .env up -d --build

    log "Waiting for services to become healthy..."
    sleep 10

    local retries=30
    while [ $retries -gt 0 ]; do
        if docker compose ps | grep -q "unhealthy\|starting"; then
            sleep 5
            retries=$((retries - 1))
        else
            break
        fi
    done

    docker compose ps
    log "All services started"
}

setup_backup_cron() {
    log "Setting up daily database backup..."
    mkdir -p /opt/rheosim-backups

    cat > /etc/cron.daily/rheosim-backup <<'CRON'
#!/bin/bash
BACKUP_DIR="/opt/rheosim-backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=7

docker exec rheosim-postgres pg_dump -U rheosim rheosim | gzip > "$BACKUP_DIR/rheosim_${TIMESTAMP}.sql.gz"

find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$KEEP_DAYS -delete
CRON
    chmod +x /etc/cron.daily/rheosim-backup
    log "Daily backup configured (keeps 7 days)"
}

# --- Main ---
main() {
    echo ""
    echo "============================================="
    echo "   RheoSim VPS Deployment"
    echo "============================================="
    echo ""

    if [ -z "${1:-}" ]; then
        read -p "Enter your domain name (e.g., rheosim.example.com): " DOMAIN
    else
        DOMAIN="$1"
    fi

    if [ -z "$DOMAIN" ]; then
        error "Domain name is required"
    fi

    check_root
    check_resources
    install_dependencies
    install_docker
    setup_firewall
    setup_fail2ban
    clone_repo
    setup_env
    setup_ssl_initial
    process_nginx_template
    start_services
    setup_backup_cron

    echo ""
    log "=========================================="
    log " Deployment complete!"
    log "=========================================="
    log ""
    log " URL: https://${DOMAIN}"
    log " Logs: cd $INSTALL_DIR/deploy/vps && docker compose logs -f"
    log " Status: docker compose ps"
    log ""
    warn "Don't forget to:"
    warn "  1. Edit /opt/rheosim/deploy/vps/.env for Stripe/email config"
    warn "  2. Set up DNS A record pointing ${DOMAIN} to this server's IP"
    warn "  3. Review firewall rules: ufw status"
    echo ""
}

main "$@"
