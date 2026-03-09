#!/usr/bin/env bash
#
# Deploy plugin to the main IDE and attempt dynamic reload.
#
# Usage:
#   ./deploy-to-ide.sh          # build + deploy + reload
#   ./deploy-to-ide.sh --skip-build   # deploy only (assumes ZIP is fresh)
#
set -euo pipefail

DIST_DIR="plugin-core/build/distributions"

# Detect the top-level directory name inside the ZIP (e.g. "ide-agent-for-copilot")
detect_zip_root_dir() {
    local zip="$1"
    unzip -l "$zip" | awk 'NR>3 && $NF ~ /\/$/ { split($NF,a,"/"); print a[1]; exit }'
}

# Auto-detect IntelliJ install dir for the plugin
detect_install_dir() {
    local plugin_dir_name="$1"
    local base="$HOME/.local/share/JetBrains"

    # 1. Toolbox per-IDE plugin dir: ~/.local/share/JetBrains/IntelliJIdea*/<plugin>
    local ide_dir
    ide_dir=$(ls -dt "$base"/IntelliJIdea* 2>/dev/null | head -1)
    if [[ -n "$ide_dir" && -d "$ide_dir/$plugin_dir_name" ]]; then
        echo "$ide_dir/$plugin_dir_name"
        return
    fi

    # 2. Toolbox app-level: ~/.local/share/JetBrains/Toolbox/apps/.../plugins/<plugin>
    local dir
    dir=$(find "$base/Toolbox/apps" -maxdepth 3 -name "plugins" -type d 2>/dev/null | while read -r d; do
        [[ -d "$d/$plugin_dir_name" ]] && echo "$d/$plugin_dir_name" && break
    done)
    if [[ -n "$dir" ]]; then
        echo "$dir"
        return
    fi

    # 3. Fallback: newest IntelliJIdea dir (create target)
    if [[ -n "$ide_dir" ]]; then
        echo "$ide_dir/$plugin_dir_name"
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

# Step 3: Detect plugin directory name from ZIP contents
PLUGIN_DIR_NAME=$(detect_zip_root_dir "$LATEST_ZIP")
if [[ -z "$PLUGIN_DIR_NAME" ]]; then
    echo "❌ Could not detect plugin directory name in ZIP"
    exit 1
fi

# Step 4: Deploy files to plugin install directory
INSTALL_DIR=$(detect_install_dir "$PLUGIN_DIR_NAME")
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

# Step 5: Try dynamic reload via PSI bridge (best-effort)
# Read port from ~/.copilot/psi-bridge.json first; fall back to port scan.
echo "🔄 Attempting dynamic reload..."
RELOADED=false

try_reload_on_port() {
    local port="$1"
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://127.0.0.1:$port/health" \
        --connect-timeout 0.3 --max-time 1 2>/dev/null || echo "000")
    if [[ "$http_code" != "200" ]]; then return 1; fi
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "http://127.0.0.1:$port/reload-plugin" \
        -H "Content-Type: application/json" \
        -d "{\"zipPath\":\"$LATEST_ZIP_ABS\"}" \
        --connect-timeout 3 --max-time 5 2>/dev/null || echo "000")
    [[ "$http_code" == "200" ]]
}

# Try port file first
BRIDGE_FILE="$HOME/.copilot/psi-bridge.json"
if [[ -f "$BRIDGE_FILE" ]] && command -v python3 &>/dev/null; then
    KNOWN_PORT=$(python3 -c "
import json, sys
with open('$BRIDGE_FILE') as f:
    reg = json.load(f)
# Try exact project path, then first entry
key = '$(pwd)'
entry = reg.get(key) or next(iter(reg.values()), None)
if entry and 'port' in entry:
    print(entry['port'])
" 2>/dev/null || echo "")
    if [[ -n "$KNOWN_PORT" ]] && try_reload_on_port "$KNOWN_PORT"; then
        echo "🔄 Restart scheduled via port $KNOWN_PORT — IDE will restart"
        RELOADED=true
    fi
fi

# Fall back to port scan
if [[ "$RELOADED" != "true" ]]; then
    for PORT in $(seq 36400 36450) 8642 8643; do
        if try_reload_on_port "$PORT"; then
            echo "🔄 Restart scheduled via port $PORT — IDE will restart"
            RELOADED=true
            break
        fi
    done
fi

if [[ "$RELOADED" != "true" ]]; then
    echo "ℹ️  No running PSI bridge found — restart IDE manually to apply"
fi
