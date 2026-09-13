#!/usr/bin/env bash
set -e

# MuzikPlayer User-Space Uninstaller
# Removes files installed by install.sh.

XDG_DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
INSTALL_DIR="$XDG_DATA_HOME/muzikplayer"
BIN_DIR="$HOME/.local/bin"
DESKTOP_DIR="$XDG_DATA_HOME/applications"
ICON_SCALABLE_DIR="$XDG_DATA_HOME/icons/hicolor/scalable/apps"
ICON_PNG_DIR="$XDG_DATA_HOME/icons/hicolor/512x512/apps"

echo "Removing MuzikPlayer..."

rm -rf "$INSTALL_DIR"
rm -f "$BIN_DIR/muzikplayer"
rm -f "$DESKTOP_DIR/io.github.coderaulia.MuzikPlayer.desktop"
rm -f "$ICON_SCALABLE_DIR/io.github.coderaulia.MuzikPlayer.svg"
rm -f "$ICON_PNG_DIR/io.github.coderaulia.MuzikPlayer.png"

# Refresh desktop databases if tools are available
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$XDG_DATA_HOME/icons/hicolor" 2>/dev/null || true
fi

echo "MuzikPlayer uninstalled successfully."
