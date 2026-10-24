# Download MuzikPlayer

Pre-built Linux release packages for **MuzikPlayer 1.5.7**. Every package includes a bundled, optimized Java runtime with low-latency ZGC and FFmpeg-based native decoding—**no Java, Gradle, or development dependencies required**.

---

## Latest Release: v1.5.7 (2026-10-24)

Direct downloads from GitHub Releases:

| Package | Target System | Direct Download Link |
|---|---|---|
| **Portable Bundle (`.tar.gz`)** | Any modern Linux (x86_64 / amd64) | [MuzikPlayer-1.5.7-linux-amd64.tar.gz](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7-linux-amd64.tar.gz) |
| **Debian / Ubuntu (`.deb`)** | Ubuntu 22.04+, Debian 12+, Linux Mint, Pop!_OS | [MuzikPlayer-1.5.7.deb](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7.deb) |
| **RPM Package (`.rpm`)** | Fedora 39+, RHEL 9+, openSUSE Tumbleweed/Leap | [MuzikPlayer-1.5.7.rpm](https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7.rpm) |
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
wget https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7.deb
sudo apt install ./MuzikPlayer-1.5.7.deb
```

### 3. Fedora / RHEL / openSUSE (`.rpm`)
```bash
wget https://github.com/coderaulia/muzik/releases/download/v1.5.7/MuzikPlayer-1.5.7.rpm
sudo dnf install ./MuzikPlayer-1.5.7.rpm
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
