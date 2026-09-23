#!/usr/bin/env bash
set -e

# MuzikPlayer User-Space Installer
# Installs MuzikPlayer without requiring root privileges or external Java/build dependencies.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    cat <<EOF
Usage: $(basename "$0") [--prefix DIR] [--help]

  --prefix DIR   Install under DIR instead of \$XDG_DATA_HOME (default: ~/.local/share).
                 The app is placed in DIR/muzikplayer.
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

# Identify MuzikPlayer source directory
if [ -d "$SCRIPT_DIR/MuzikPlayer" ]; then
    SRC_APP_DIR="$SCRIPT_DIR/MuzikPlayer"
elif [ -f "$SCRIPT_DIR/bin/MuzikPlayer" ]; then
    SRC_APP_DIR="$SCRIPT_DIR"
elif [ -d "$SCRIPT_DIR/../build/compose/binaries/main-release/app/MuzikPlayer" ]; then
    SRC_APP_DIR="$SCRIPT_DIR/../build/compose/binaries/main-release/app/MuzikPlayer"
else
    echo "Error: Could not locate MuzikPlayer application files." >&2
    exit 1
fi

XDG_DATA_HOME="${CUSTOM_PREFIX:-${XDG_DATA_HOME:-$HOME/.local/share}}"
INSTALL_DIR="$XDG_DATA_HOME/muzikplayer"
BIN_DIR="$HOME/.local/bin"
DESKTOP_DIR="$XDG_DATA_HOME/applications"
ICON_SCALABLE_DIR="$XDG_DATA_HOME/icons/hicolor/scalable/apps"
ICON_PNG_DIR="$XDG_DATA_HOME/icons/hicolor/512x512/apps"

echo "Installing MuzikPlayer to $INSTALL_DIR..."
rm -rf "$INSTALL_DIR"
mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_SCALABLE_DIR" "$ICON_PNG_DIR"

# Copy application files (bundled JRE and binaries)
rm -f "$INSTALL_DIR/bin/MuzikPlayer"
cp -r "$SRC_APP_DIR"/* "$INSTALL_DIR/"
chmod +x "$INSTALL_DIR/bin/MuzikPlayer"

# Fallback: sanitize Red Hat crypto-policies directive if still present in the
# bundled runtime. The primary fix lives in src/build.gradle.kts (fixCryptoPolicies),
# which already patches java.security for every artifact this project builds
# (deb/rpm/portable tarball/Flatpak); this only matters for a runtime that was
# hand-assembled or repackaged outside that build.
JAVA_SEC="$INSTALL_DIR/lib/runtime/conf/security/java.security"
if [ -f "$JAVA_SEC" ] && grep -q '^include redhat/' "$JAVA_SEC"; then
    sed -i 's|^include redhat/|#include redhat/|' "$JAVA_SEC"
fi

# Symlink executable into ~/.local/bin
ln -sf "$INSTALL_DIR/bin/MuzikPlayer" "$BIN_DIR/muzikplayer"

# Copy icons if available
if [ -f "$SCRIPT_DIR/icon.svg" ]; then
    cp "$SCRIPT_DIR/icon.svg" "$ICON_SCALABLE_DIR/io.github.coderaulia.MuzikPlayer.svg"
elif [ -f "$SRC_APP_DIR/../flatpak/icon.svg" ]; then
    cp "$SRC_APP_DIR/../flatpak/icon.svg" "$ICON_SCALABLE_DIR/io.github.coderaulia.MuzikPlayer.svg"
fi

if [ -f "$SCRIPT_DIR/icon.png" ]; then
    cp "$SCRIPT_DIR/icon.png" "$ICON_PNG_DIR/io.github.coderaulia.MuzikPlayer.png"
elif [ -f "$SRC_APP_DIR/../flatpak/icon.png" ]; then
    cp "$SRC_APP_DIR/../flatpak/icon.png" "$ICON_PNG_DIR/io.github.coderaulia.MuzikPlayer.png"
fi

# Generate desktop entry
DESKTOP_FILE="$DESKTOP_DIR/io.github.coderaulia.MuzikPlayer.desktop"
AUDIO_MIME_TYPES="application/ogg;application/x-ogg;application/x-ogm-audio;audio/aac;audio/mp4;audio/mpeg;audio/mpegurl;audio/ogg;audio/vnd.rn-realaudio;audio/vorbis;audio/x-flac;audio/x-mp3;audio/x-mpeg;audio/x-mpegurl;audio/x-ms-wma;audio/x-musepack;audio/x-oggflac;audio/x-pn-realaudio;audio/x-scpls;audio/x-speex;audio/x-vorbis;audio/x-vorbis+ogg;audio/x-wav;audio/x-aac;audio/m4a;audio/x-m4a;audio/mp3;audio/ac3;audio/flac;application/xspf+xml;audio/x-opus+ogg;application/vnd.apple.mpegurl"
cat <<EOF > "$DESKTOP_FILE"
[Desktop Entry]
Name=MuzikPlayer
Comment=A modern desktop music player for your local library
Exec=$BIN_DIR/muzikplayer %U
Icon=io.github.coderaulia.MuzikPlayer
Terminal=false
Type=Application
Categories=AudioVideo;Audio;Player;Music;
MimeType=inode/directory;x-content/audio-player;$AUDIO_MIME_TYPES
StartupWMClass=MuzikPlayer
EOF

chmod +x "$DESKTOP_FILE"

if command -v desktop-file-validate >/dev/null 2>&1; then
    if ! desktop-file-validate "$DESKTOP_FILE"; then
        echo "Warning: generated .desktop file failed validation (see above)." >&2
    fi
fi

# Refresh desktop databases if tools are available
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$XDG_DATA_HOME/icons/hicolor" 2>/dev/null || true
fi

# Register MuzikPlayer as the default handler for the audio types it declares
if command -v xdg-mime >/dev/null 2>&1; then
    mkdir -p "${XDG_CONFIG_HOME:-$HOME/.config}"
    IFS=';' read -ra MIME_LIST <<< "inode/directory;x-content/audio-player;$AUDIO_MIME_TYPES"
    for mime in "${MIME_LIST[@]}"; do
        [ -n "$mime" ] || continue
        xdg-mime default io.github.coderaulia.MuzikPlayer.desktop "$mime" 2>/dev/null || true
    done
fi

# Add ~/.local/bin to PATH for future shells if it isn't already there
PATH_LINE='export PATH="$HOME/.local/bin:$PATH"'
PATH_FIXED=0
if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    case "$SHELL" in
        */fish)
            RC_FILE="$HOME/.config/fish/config.fish"
            FISH_LINE='set -gx PATH $HOME/.local/bin $PATH'
            mkdir -p "$(dirname "$RC_FILE")"
            if [ ! -f "$RC_FILE" ] || ! grep -qF "$FISH_LINE" "$RC_FILE"; then
                { echo ""; echo "# Added by MuzikPlayer installer"; echo "$FISH_LINE"; } >> "$RC_FILE"
                PATH_FIXED=1
            fi
            ;;
        */zsh)
            RC_FILE="$HOME/.zshrc"
            ;;
        */bash)
            RC_FILE="$HOME/.bashrc"
            ;;
        *)
            RC_FILE=""
            ;;
    esac
    if [ -n "${RC_FILE:-}" ] && [ "$PATH_FIXED" -eq 0 ]; then
        if [ ! -f "$RC_FILE" ] || ! grep -qF "$PATH_LINE" "$RC_FILE"; then
            { echo ""; echo "# Added by MuzikPlayer installer"; echo "$PATH_LINE"; } >> "$RC_FILE"
        fi
        PATH_FIXED=1
    fi
fi

echo ""
echo "=== MuzikPlayer successfully installed! ==="
echo "• Executable: $BIN_DIR/muzikplayer"
echo "• Desktop entry: $DESKTOP_FILE"
echo "• You can launch it from your application launcher or by running: muzikplayer"
if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    if [ "$PATH_FIXED" -eq 1 ]; then
        echo "Note: Added $BIN_DIR to your PATH in ${RC_FILE:-your shell config}. Restart your shell (or source it) to use the 'muzikplayer' command directly."
    else
        echo "Note: Make sure $BIN_DIR is in your PATH (e.g. export PATH=\"\$HOME/.local/bin:\$PATH\")."
    fi
fi
