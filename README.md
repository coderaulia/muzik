# MuzikPlayer

A modern desktop music player for your local library, built with Compose Desktop (Kotlin/JVM).

![License](https://img.shields.io/badge/license-GPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Linux-lightgrey)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF)

## Features

- **Local Library Management** — Scans and indexes your music directory with support for FLAC, MP3, OGG, WAV, AIFF, and other common formats via the bundled FFmpeg decoder.
- **Now-Playing Home View** — Album art with vinyl record animation, live waveform, lyrics, tag inspector, and DSP visualizations.
- **Library Browser** — Browse by albums, artists, or tracks with real-time search filtering (`Ctrl+K`).
- **Playlist Support** — Create, manage, and persist playlists via M3U files.
- **Playback Controls** — Play/pause, previous/next, shuffle, repeat (all/one), seek, and volume with keyboard shortcuts.
- **Queue Management** — Reorder, add, and clear the playback queue.
- **Dark & Light Themes** — Libadwaita Slate design tokens with System/Dark/Light toggle in Settings.
- **MPRIS Integration** — Exposes playback state to the Linux desktop (media keys, notification widgets).
- **Cover Art Color Extraction** — Dynamic palette derived from album artwork.
- **ListenBrainz Scrobbling** — Optional token-based integration for tracking listening history.
- **Bit-Perfect Playback** — Optional direct hardware output path for audiophile use.

## Screenshots

<!-- Add screenshots here after release -->

## Installation

### For Daily Users (No Dependencies Required)

Pre-built releases include a bundled Java runtime—**no Java, Gradle, or build tools need to be installed on your machine**. Download the latest release from the [Releases](https://github.com/coderaulia/muzik/releases) page.

#### Debian / Ubuntu / Linux Mint (`.deb`)
```bash
sudo apt install ./MuzikPlayer-1.5.3.deb
```

#### Fedora / RHEL / openSUSE (`.rpm`)
```bash
sudo dnf install ./MuzikPlayer-1.5.3.rpm
```

#### Portable Archive (Any Linux — No Root Required)
Download and extract the portable archive, then run the installer to integrate MuzikPlayer into your application launcher:
```bash
tar -xzf MuzikPlayer-1.5.3-linux-x86_64.tar.gz
cd MuzikPlayer
./install.sh
```
To run directly without installing:
```bash
./bin/MuzikPlayer
```
To uninstall at any time, run `./uninstall.sh`.

#### Flatpak
Install the Freedesktop 26.08 runtime:
```bash
flatpak install flathub org.freedesktop.Platform//26.08 org.freedesktop.Sdk//26.08
```
Then install the downloaded `.flatpak` bundle or run:
```bash
flatpak install --user MuzikPlayer-1.5.3.flatpak
flatpak run io.github.coderaulia.MuzikPlayer
```

---

### Building & Developing from Source

If you want to contribute, build from source, or package locally, you will need the build dependencies installed on your system.

#### Prerequisites

- **JDK 25+** (A full development kit with `javac` and `jpackage` is required; a headless JRE is not sufficient)
- **Gradle 9.4+** (included via `./gradlew` wrapper)
- **flatpak-builder** (only required if building Flatpak packages)

##### Install Prerequisites by Distribution

- **Fedora / RHEL:**
  ```bash
  sudo dnf install -y java-25-openjdk-devel java-25-openjdk flatpak-builder
  ```

- **Debian / Ubuntu:**
  ```bash
  sudo apt install -y openjdk-25-jdk flatpak-builder rpm
  ```

- **Arch Linux:**
  ```bash
  sudo pacman -S jdk25-openjdk flatpak-builder rpm-tools
  ```

- **Flatpak Runtimes (for Flatpak builds):**
  ```bash
  flatpak install flathub org.freedesktop.Platform//26.08 org.freedesktop.Sdk//26.08
  ```

#### Build Commands

```bash
cd src

# Run in development mode
./gradlew run

# Run unit tests
./gradlew test

# Build standalone release application
./gradlew createReleaseDistributable

# Build zero-dependency system packages
./gradlew packageReleaseDeb           # Creates .deb package
./gradlew packageReleaseRpm           # Creates .rpm package
./gradlew packagePortableDistributable # Creates portable .tar.gz bundle with installer

# Build and install Flatpak locally
./flatpak/build.sh
```

The release binaries are output under `src/build/compose/binaries/main-release/` and portable archives under `src/build/distributions/`.

See [DEVELOPMENT.md](src/DEVELOPMENT.md) for detailed build instructions and [RELEASE.md](RELEASE.md) for the full release workflow.

## Project Structure

```
muzikplayer/
├── design/        Design assets: HTML mockups + screenshots + DESIGN.md
│   ├── home-page/        Home view mockup + design tokens
│   ├── library-page/     Library view mockup
│   ├── playlists-page/   Playlists view mockup
│   └── settings-page/    Settings view mockup + design tokens
├── src/           Application source
│   ├── src/main/kotlin/io/github/coderaulia/muzikplayer/
│   │   ├── audio/     Audio engine — playback, decoding, seeking
│   │   ├── data/      Library, metadata (jaudiotagger), song queue
│   │   ├── color/     Cover-art color extraction
│   │   ├── mpris/     MPRIS D-Bus integration
│   │   ├── ui/        Compose UI — Libadwaita Slate design system
│   │   └── utils/     Preferences, formatting utilities
│   ├── src/test/kotlin/   Unit tests (Kotest)
│   └── flatpak/     Flatpak packaging (io.github.coderaulia.MuzikPlayer)
├── RELEASE.md     Release guide
└── CHANGELOG.md   Version history
```

## Keyboard Shortcuts

| Shortcut        | Action              |
|-----------------|---------------------|
| `Space`         | Play / Pause        |
| `←` / `→`       | Previous / Next     |
| `Ctrl+K`        | Focus search bar    |
| `Ctrl+Q`        | Quit                |

## Tech Stack

- **Kotlin** 2.3 + **Compose Desktop** (Multiplatform)
- **Material 3** with Libadwaita Slate design tokens
- **FFSampledSP** for native audio decoding
- **jaudiotagger** for metadata reading
- **D-Bus Java** for MPRIS integration
- **Kotest** for unit testing
- **ProGuard** for release optimization

## App Identity

- **App ID:** `io.github.coderaulia.MuzikPlayer`
- **Package:** `io.github.coderaulia.muzikplayer`
- **Version:** 1.5.3

## License

This project is licensed under the GPL-3.0 License.
