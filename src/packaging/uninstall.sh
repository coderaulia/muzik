#!/usr/bin/env bash
set -e

# MuzikPlayer User-Space Uninstaller
# Removes files installed by install.sh.

usage() {
    cat <<EOF
Usage: $(basename "$0") [--prefix DIR] [--help]

  --prefix DIR   Remove the install placed under DIR by 'install.sh --prefix DIR'
                 instead of \$XDG_DATA_HOME (default: ~/.local/share).
  --help         Show this help and exit.
EOF
}

CUSTOM_PREFIX=""
while [ $# -gt 0 ]; do
    case "$1" in
        --prefix)
            [ $# -ge 2 ] || { echo "Error: --prefix requires a directory argument." >&2; exit 1; }
            CUSTOM_PREFIX="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Error: unknown argument '$1'." >&2
            usage >&2
            exit 1
            ;;
    esac
done

XDG_DATA_HOME="${CUSTOM_PREFIX:-${XDG_DATA_HOME:-$HOME/.local/share}}"
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
echo "Note: if install.sh added a PATH export for ~/.local/bin to your shell"
echo "config, that line is left in place (it's harmless and other tools may"
echo "rely on the same PATH entry) — remove it by hand if you no longer want it."
