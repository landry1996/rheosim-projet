#!/bin/bash
set -euo pipefail

# =============================================================
# RheoSim Database Backup Script
# Usage: ./backup.sh [restore <filename>]
# =============================================================

BACKUP_DIR="/opt/rheosim-backups"
CONTAINER="rheosim-postgres"
DB_USER="rheosim"
DB_NAME="rheosim"
KEEP_DAYS=7
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

backup() {
    echo "[BACKUP] Starting PostgreSQL backup..."
    local filename="rheosim_${TIMESTAMP}.sql.gz"

    docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/$filename"

    local size
    size=$(du -h "$BACKUP_DIR/$filename" | cut -f1)
    echo "[BACKUP] Created: $filename ($size)"

    # Cleanup old backups
    local deleted
    deleted=$(find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$KEEP_DAYS -delete -print | wc -l)
    if [ "$deleted" -gt 0 ]; then
        echo "[BACKUP] Removed $deleted backup(s) older than $KEEP_DAYS days"
    fi

    echo "[BACKUP] Done. Backups in $BACKUP_DIR:"
    ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5
}

restore() {
    local file="$1"

    if [ ! -f "$file" ]; then
        echo "[ERROR] File not found: $file"
        exit 1
    fi

    echo "[RESTORE] WARNING: This will overwrite the current database!"
    read -p "Are you sure? (yes/NO): " -r
    if [ "$REPLY" != "yes" ]; then
        echo "[RESTORE] Cancelled."
        exit 0
    fi

    echo "[RESTORE] Restoring from: $file"
    gunzip -c "$file" | docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"
    echo "[RESTORE] Done."
}

case "${1:-backup}" in
    backup)
        backup
        ;;
    restore)
        if [ -z "${2:-}" ]; then
            echo "Usage: $0 restore <backup-file.sql.gz>"
            exit 1
        fi
        restore "$2"
        ;;
    *)
        echo "Usage: $0 [backup|restore <file>]"
        exit 1
        ;;
esac
