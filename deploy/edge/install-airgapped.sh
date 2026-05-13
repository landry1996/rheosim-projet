#!/bin/bash
# RheoSim Edge - Air-Gapped Installation Script
# This script installs RheoSim on an air-gapped K3s cluster

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGES_DIR="${SCRIPT_DIR}/images"
CHART_DIR="${SCRIPT_DIR}"
NAMESPACE="rheosim"
RELEASE_NAME="rheosim"

echo "============================================"
echo "  RheoSim Enterprise - Edge Installation"
echo "  Version: 3.0.0"
echo "============================================"
echo ""

# Check prerequisites
check_prerequisites() {
    echo "[1/6] Checking prerequisites..."

    if ! command -v k3s &> /dev/null; then
        echo "ERROR: K3s not found. Install K3s first:"
        echo "  curl -sfL https://get.k3s.io | sh -"
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        echo "ERROR: Helm not found. Install Helm first."
        exit 1
    fi

    echo "  ✓ K3s found: $(k3s --version | head -1)"
    echo "  ✓ Helm found: $(helm version --short)"
}

# Load container images
load_images() {
    echo "[2/6] Loading container images..."

    if [ -d "$IMAGES_DIR" ]; then
        for image_tar in "$IMAGES_DIR"/*.tar; do
            if [ -f "$image_tar" ]; then
                echo "  Loading: $(basename "$image_tar")"
                sudo k3s ctr images import "$image_tar"
            fi
        done
    else
        echo "  WARNING: No images directory found. Expecting images pre-loaded."
    fi
}

# Create namespace
create_namespace() {
    echo "[3/6] Creating namespace..."
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
}

# Validate license
validate_license() {
    echo "[4/6] Validating license..."

    if [ -f "${SCRIPT_DIR}/license.key" ]; then
        echo "  ✓ License file found"
        kubectl create secret generic rheosim-license \
            --from-file=license.key="${SCRIPT_DIR}/license.key" \
            --namespace "$NAMESPACE" \
            --dry-run=client -o yaml | kubectl apply -f -
    else
        echo "  WARNING: No license.key found. Running in evaluation mode (30 days)."
    fi
}

# Install with Helm
install_chart() {
    echo "[5/6] Installing RheoSim..."

    helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
        --namespace "$NAMESPACE" \
        --values "${CHART_DIR}/values.yaml" \
        --set global.imageRegistry="" \
        --set ingress.enabled=true \
        --wait \
        --timeout 10m
}

# Verify installation
verify() {
    echo "[6/6] Verifying installation..."
    echo ""

    kubectl get pods -n "$NAMESPACE"
    echo ""

    echo "============================================"
    echo "  Installation Complete!"
    echo ""
    echo "  Access RheoSim at: http://rheosim.local"
    echo "  (Add 'rheosim.local' to /etc/hosts)"
    echo ""
    echo "  Default credentials:"
    echo "    Admin: admin@rheosim.local / admin123"
    echo "============================================"
}

# Main
check_prerequisites
load_images
create_namespace
validate_license
install_chart
verify
