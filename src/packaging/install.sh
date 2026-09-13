#!/usr/bin/env bash
set -e

# MuzikPlayer User-Space Installer
# Installs MuzikPlayer without requiring root privileges or external Java/build dependencies.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

XDG_DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
INSTALL_DIR="$XDG_DATA_HOME/muzikplayer"
BIN_DIR="$HOME/.local/bin"
DESKTOP_DIR="$XDG_DATA_HOME/applications"
ICON_SCALABLE_DIR="$XDG_DATA_HOME/icons/hicolor/scalable/apps"
ICON_PNG_DIR="$XDG_DATA_HOME/icons/hicolor/512x512/apps"

echo "Installing MuzikPlayer to $INSTALL_DIR..."
mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_SCALABLE_DIR" "$ICON_PNG_DIR"

# Copy application files (bundled JRE and binaries)
cp -r "$SRC_APP_DIR"/* "$INSTALL_DIR/"
chmod +x "$INSTALL_DIR/bin/MuzikPlayer"

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
cat <<EOF > "$DESKTOP_DIR/io.github.coderaulia.MuzikPlayer.desktop"
[Desktop Entry]
Name=MuzikPlayer
Comment=A modern desktop music player for your local library
Exec=$BIN_DIR/muzikplayer %U
Icon=io.github.coderaulia.MuzikPlayer
Terminal=false
Type=Application
Categories=AudioVideo;Audio;Player;Music;
MimeType=inode/directory;application/ogg;application/x-ogg;application/x-ogm-audio;audio/aac;audio/mp4;audio/mpeg;audio/mpegurl;audio/ogg;audio/vnd.rn-realaudio;audio/vorbis;audio/x-flac;audio/x-mp3;audio/x-mpeg;audio/x-mpegurl;audio/x-ms-wma;audio/x-musepack;audio/x-oggflac;audio/x-pn-realaudio;audio/x-scpls;audio/x-speex;audio/x-vorbis;audio/x-vorbis+ogg;audio/x-wav;x-content/audio-player;audio/x-aac;audio/m4a;audio/x-m4a;audio/mp3;audio/ac3;audio/flac;application/xspf+xml;audio/x-opus+ogg;application/vnd.apple.mpegurl
StartupWMClass=MuzikPlayer
EOF

chmod +x "$DESKTOP_DIR/io.github.coderaulia.MuzikPlayer.desktop"

# Refresh desktop databases if tools are available
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$XDG_DATA_HOME/icons/hicolor" 2>/dev/null || true
fi

echo ""
echo "=== MuzikPlayer successfully installed! ==="
echo "• Executable: $BIN_DIR/muzikplayer"
echo "• Desktop entry: $DESKTOP_DIR/io.github.coderaulia.MuzikPlayer.desktop"
echo "• You can launch it from your application launcher or by running: muzikplayer"
if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    echo "Note: Make sure $BIN_DIR is in your PATH (e.g. export PATH=\"\$HOME/.local/bin:\$PATH\")."
fi
