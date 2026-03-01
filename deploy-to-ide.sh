#!/usr/bin/env bash
#
# Deploy plugin to the main IDE and trigger dynamic reload.
#
# Usage:
#   ./deploy-to-ide.sh          # build + deploy + reload
#   ./deploy-to-ide.sh --skip-build   # deploy only (assumes ZIP is fresh)
#
set -euo pipefail

PLUGIN_DIR_NAME="plugin-core"
DIST_DIR="plugin-core/build/distributions"

# Auto-detect IntelliJ install dir (Toolbox layout)
detect_install_dir() {
    local base="$HOME/.local/share/JetBrains"
    local dir
    dir=$(find "$base" -maxdepth 2 -name "$PLUGIN_DIR_NAME" -type d 2>/dev/null | head -1)
    if [[ -n "$dir" ]]; then
        echo "$dir"
    else
        # Fallback: newest IntelliJIdea dir
        local ide_dir
        ide_dir=$(ls -dt "$base"/IntelliJIdea* 2>/dev/null | head -1)
        if [[ -n "$ide_dir" ]]; then
            echo "$ide_dir/$PLUGIN_DIR_NAME"
        fi
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
echo "📦 ZIP: $(basename "$LATEST_ZIP")"

# Step 3: Find install directory
INSTALL_DIR=$(detect_install_dir)
if [[ -z "$INSTALL_DIR" ]]; then
    echo "❌ Could not find plugin install directory"
    exit 1
fi
echo "📂 Install: $INSTALL_DIR"

# Step 4: Replace files
echo "🗑  Removing old version..."
rm -rf "$INSTALL_DIR"

echo "📂 Extracting..."
unzip -q "$LATEST_ZIP" -d "$(dirname "$INSTALL_DIR")"

if [[ ! -d "$INSTALL_DIR" ]]; then
    echo "❌ Extraction failed"
    exit 1
fi

echo "✅ Files deployed to $INSTALL_DIR"
echo ""
echo "💡 The agent can now call 'reload_plugin' to hot-reload,"
echo "   or restart the IDE to apply changes."
