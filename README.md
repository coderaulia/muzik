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

### Portable Archive (Linux x86_64)

Download the latest `.tar.gz` from the [Releases](https://github.com/coderaulia/muzik/releases) page:

```bash
tar -xzf MuzikPlayer-1.5.3-linux-x86_64.tar.gz
./MuzikPlayer/bin/MuzikPlayer
```

### Flatpak

Install the required runtime (one-time):

```bash
flatpak install flathub org.freedesktop.Platform//26.08 org.freedesktop.Sdk//26.08
```

Then build and install locally from source:

```bash
cd src
./flatpak/build.sh
flatpak run io.github.coderaulia.MuzikPlayer
```

### Build from Source

**Requirements:** JDK 25+, Gradle 9.4+

```bash
cd src
./gradlew run          # dev run
./gradlew test         # unit tests
./gradlew createReleaseDistributable  # release build
```

The release application is written to `src/build/compose/binaries/main-release/app/MuzikPlayer/`.

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
