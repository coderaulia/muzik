# Download MuzikPlayer

Pre-built Linux release packages for **MuzikPlayer 1.5.7**. Every package includes a bundled, optimized Java runtime with low-latency ZGC and FFmpeg-based native decoding—**no Java, Gradle, or development dependencies required**.

---

## Latest Release: v1.5.7 (2026-10-24)

Direct downloads from GitHub Releases:

| Package | Target System | Direct Download Link |
|---|---|---|
| **Portable Bundle (`.tar.gz`)** | Any modern Linux (x86_64 / amd64) | [MuzikPlayer-1.5.7-linux-amd64.tar.gz](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7-linux-amd64.tar.gz) |
| **Portable Bundle (`.tar.gz`)** | Any modern Linux (ARM64 / aarch64) | [MuzikPlayer-1.5.7-linux-aarch64.tar.gz](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7-linux-aarch64.tar.gz) |
| **Debian / Ubuntu (`.deb`)** | Ubuntu 22.04+, Debian 12+, Linux Mint, Pop!_OS (x86_64) | [muzikplayer_1.5.7_amd64.deb](https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer_1.5.7_amd64.deb) |
| **Debian / Ubuntu (`.deb`)** | Ubuntu 22.04+, Debian 12+, Raspberry Pi OS (arm64) | [muzikplayer_1.5.7_arm64.deb](https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer_1.5.7_arm64.deb) |
| **RPM Package (`.rpm`)** | Fedora 39+, RHEL 9+, openSUSE (x86_64) | [muzikplayer-1.5.7-1.x86_64.rpm](https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer-1.5.7-1.x86_64.rpm) |
| **RPM Package (`.rpm`)** | Fedora 39+, RHEL 9+, openSUSE (aarch64) | [muzikplayer-1.5.7-1.aarch64.rpm](https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer-1.5.7-1.aarch64.rpm) |
| **Flatpak Bundle (`.flatpak`)** | Any distribution with Flatpak | [MuzikPlayer-1.5.7.flatpak](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7.flatpak) |

All releases and source archives: [https://github.com/coderaulia/muzik/releases/tag/v1.5.7](https://github.com/coderaulia/muzik/releases/tag/v1.5.7)

---

## Installation Instructions

### 1. Portable Bundle (Recommended for Any Linux)
Extract the archive and run the included user-space installer:
```bash
wget https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7-linux-amd64.tar.gz
tar -xzf MuzikPlayer-1.5.7-linux-amd64.tar.gz
cd MuzikPlayer
./install.sh
```
- Installs to `~/.local/share/muzikplayer`
- Links executable to `~/.local/bin/muzikplayer`
- Adds desktop entry and application icons to your desktop environment
- No `sudo` or root privileges needed!

To uninstall at any time:
```bash
cd MuzikPlayer && ./uninstall.sh
```

### 2. Debian / Ubuntu / Linux Mint (`.deb`)
```bash
wget https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer_1.5.7_amd64.deb
sudo apt install ./muzikplayer_1.5.7_amd64.deb
```

### 3. Fedora / RHEL / openSUSE (`.rpm`)
```bash
wget https://github.com/coderaulia/muzik/releases/download/v1.5.7/muzikplayer-1.5.7-1.x86_64.rpm
sudo dnf install ./muzikplayer-1.5.7-1.x86_64.rpm
```

### 4. Flatpak Bundle
Ensure Freedesktop 26.08 runtime is installed:
```bash
flatpak install flathub org.freedesktop.Platform//26.08 org.freedesktop.Sdk//26.08
flatpak install --user MuzikPlayer-1.5.7.flatpak
flatpak run io.github.coderaulia.MuzikPlayer
```

---

## Verifying Integrity

Check sha256 checksums from the release page:
```bash
sha256sum MuzikPlayer-1.5.7-linux-amd64.tar.gz
```
