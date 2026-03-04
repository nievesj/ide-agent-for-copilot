#!/usr/bin/env bash
#
# Deploy plugin to the main IDE and attempt dynamic reload.
#
# Usage:
#   ./deploy-to-ide.sh          # build + deploy + reload
#   ./deploy-to-ide.sh --skip-build   # deploy only (assumes ZIP is fresh)
#
set -euo pipefail

PLUGIN_DIR_NAME="plugin-core"
DIST_DIR="plugin-core/build/distributions"
BRIDGE_FILE="$HOME/.copilot/psi-bridge.json"

# Auto-detect IntelliJ install dir
detect_install_dir() {
    local base="$HOME/.local/share/JetBrains"

    # 1. Toolbox per-IDE plugin dir: ~/.local/share/JetBrains/IntelliJIdea*/plugin-core
    local ide_dir
    ide_dir=$(ls -dt "$base"/IntelliJIdea* 2>/dev/null | head -1)
    if [[ -n "$ide_dir" && -d "$ide_dir/$PLUGIN_DIR_NAME" ]]; then
        echo "$ide_dir/$PLUGIN_DIR_NAME"
        return
    fi

    # 2. Toolbox app-level: ~/.local/share/JetBrains/Toolbox/apps/.../plugins/plugin-core
    local dir
    dir=$(find "$base/Toolbox/apps" -maxdepth 3 -name "plugins" -type d 2>/dev/null | while read -r d; do
        [[ -d "$d/$PLUGIN_DIR_NAME" ]] && echo "$d/$PLUGIN_DIR_NAME" && break
    done)
    if [[ -n "$dir" ]]; then
        echo "$dir"
        return
    fi

    # 3. Fallback: newest IntelliJIdea dir (create if needed)
    if [[ -n "$ide_dir" ]]; then
        echo "$ide_dir/$PLUGIN_DIR_NAME"
    fi
}

# Step 1: Build (unless --skip-build)
if [[ "${1:-}" != "--skip-build" ]]; then
    echo "🔨 Building plugin ZIP..."
    ./gradlew :plugin-core:buildPlugin -x buildSearchableOptions --quiet
fi

# Step 2: Find latest ZIP
LATEST_ZIP=$(ls -t "$DIST_DIR"/*.zip 2>/dev/null | head -1)
if [[ -z "$LATEST_ZIP" ]]; then
    echo "❌ No ZIP found in $DIST_DIR"
    exit 1
fi
LATEST_ZIP_ABS=$(realpath "$LATEST_ZIP")
echo "📦 ZIP: $(basename "$LATEST_ZIP")"

# Step 3: Always deploy files to plugin install directory
INSTALL_DIR=$(detect_install_dir)
if [[ -z "$INSTALL_DIR" ]]; then
    echo "❌ Could not find plugin install directory"
    exit 1
fi
echo "📂 Target: $INSTALL_DIR"

echo "🗑  Removing old version..."
rm -rf "$INSTALL_DIR"

echo "📂 Extracting..."
unzip -q "$LATEST_ZIP" -d "$(dirname "$INSTALL_DIR")"

if [[ ! -d "$INSTALL_DIR" ]]; then
    echo "❌ Extraction failed"
    exit 1
fi
echo "✅ Files deployed"

# Step 4: Try dynamic reload via PSI bridge (best-effort)
if [[ -f "$BRIDGE_FILE" ]]; then
    PORT=$(python3 -c "
import json, sys
try:
    reg = json.load(open('$BRIDGE_FILE'))
    for v in reg.values():
        print(v.get('port', '')); break
except: pass
" 2>/dev/null || true)

    if [[ -n "$PORT" ]]; then
        echo "🔄 Requesting dynamic reload on port $PORT..."
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "http://127.0.0.1:$PORT/reload-plugin" \
            -H "Content-Type: application/json" \
            -d "{\"zipPath\":\"$LATEST_ZIP_ABS\"}" \
            --connect-timeout 3 --max-time 5 2>/dev/null || echo "000")
        if [[ "$HTTP_CODE" == "200" ]]; then
            echo "🔄 Reload requested — if it fails, restart IDE to apply"
        else
            echo "ℹ️  Dynamic reload unavailable (HTTP $HTTP_CODE) — restart IDE to apply"
        fi
    else
        echo "ℹ️  No PSI bridge found — restart IDE to apply"
    fi
else
    echo "ℹ️  No running IDE detected — restart IDE to apply"
fi
