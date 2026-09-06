# MuzikPlayer

A desktop music player for your local library, built on a Compose Desktop (Kotlin) engine.

- **App ID:** `io.github.coderaulia.MuzikPlayer`
- **Kotlin package:** `io.github.coderaulia.muzikplayer`
- **Branding:** "M" logo — blue rounded tile, white M mark (`src/flatpak/icon.svg`)

## Repository layout

```
muzikplayer/
├── design/        Design assets: HTML mockups + screenshots + DESIGN.md (Libadwaita Slate tokens)
└── src/           Application source (cloned & refactored from tambourine-music-player)
    ├── src/main/kotlin/io/github/coderaulia/muzikplayer/
    │   ├── audio/     Audio engine — playback, decoding, seeking (preserved from upstream)
    │   ├── data/      Library, metadata (jaudiotagger), song queue (preserved)
    │   ├── color/     Cover-art color extraction (preserved)
    │   ├── mpris/     MPRIS D-Bus integration (rebranded bus name)
    │   ├── ui/        Compose UI — being modernized to the /design spec
    │   └── utils/
    ├── src/test/kotlin/
    └── flatpak/     Flatpak packaging (io.github.coderaulia.MuzikPlayer)
```

## Build & run

```bash
cd src
./gradlew run          # dev run
./gradlew test         # unit tests
```

See `src/DEVELOPMENT.md` for details, and `design/DESIGN.md` (under `design/home/`) for the
design token spec driving the UI overhaul.

## UI overhaul plan

The UI is being rebuilt to match `/design`:

1. ~~Branding & package rename~~ ✅ `io.github.coderaulia.muzikplayer`, "M" logo, "MuzikPlayer" titles
2. Theme port — Libadwaita Slate tokens into `ui/Theme.kt`
3. Chrome — headerbar (logo/tabs/search/window buttons), sidebar, transport bar
4. Views — Home (now-playing inspector), Library (track table), Playlists (M3U persistence)
