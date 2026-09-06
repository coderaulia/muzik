# MuzikPlayer — Agent Instructions

## Project

Compose Desktop (Kotlin) music player for Linux, refactored from
tambourine-music-player into MuzikPlayer (`io.github.coderaulia.muzikplayer`,
flatpak id `io.github.coderaulia.MuzikPlayer`).

## Layout

```
muzikplayer/
├── design/        Design assets: HTML mockups + screenshots + DESIGN.md (Libadwaita Slate tokens)
│   ├── home-page/        home.html + screen.png + DESIGN.md
│   ├── library-page/     library.html + screen.png
│   ├── playlists-page/   playlists.html + screen.png
│   └── settings-page/    settings.html + screen.png + DESIGN.md
└── src/           Application source
    ├── src/main/kotlin/io/github/coderaulia/muzikplayer/
    │   ├── audio/     Audio engine — playback, decoding, seeking (preserved from upstream)
    │   ├── data/      Library, metadata (jaudiotagger), song queue (preserved)
    │   ├── color/     Cover-art color extraction (preserved)
    │   ├── mpris/     MPRIS D-Bus integration (rebranded bus name)
    │   ├── ui/        Compose UI — being modernized to the /design spec
    │   └── utils/
    └── flatpak/     Flatpak packaging (io.github.coderaulia.MuzikPlayer)
```

## Commands

```bash
cd src
./gradlew run       # dev run
./gradlew test      # unit tests
./gradlew build     # full build incl. tests
```

Flatpak packaging: `./flatpak/build.sh` (requires flatpak-builder + freedesktop
24.08 runtimes). See `src/DEVELOPMENT.md`.

## Git conventions

- Main branch is `main` (matching `origin/main`). Never commit to master.
- Commit after each meaningful, verified change. Never push without explicit user approval.
- Design folder naming: `design/<page>-page/` (home-page, library-page, …).

## UI work

- The UI is being modernized to the mockups in `design/*-page/`. Design tokens:
  Libadwaita Slate (see `design/home-page/DESIGN.md`), ported in `ui/Theme.kt`.
- Shell (headerbar, sidebar, transport bar) lives in `ui/AppShell.kt`; panel
  content in `ui/App.kt` + view files (`LibraryHeader.kt`, `SongListUI.kt`,
  `SongQueueUI.kt`, `PlayerUI.kt`).
- Upstream-preserved packages (`audio/`, `data/`, `color/`, `mpris/`) should be
  extended, not rewritten; UI changes stay in `ui/`.
- Typography uses named system font families: `SF Pro Text` for UI/body text,
  `SF Pro Display` for large display text, and `SF Mono` for audio metrics and
  technical labels. These names must retain platform fallback because SF Pro
  fonts are not bundled or redistributed by the project.
- Home, Library, Playlists, and Settings are separate design-led views. The
  shell navigation must show one active view at a time on compact layouts;
  selecting a library song must update the shared player queue and begin
  playback.

## Progress

1. ~~Branding & package rename~~ ✅
2. ~~Theme port — Libadwaita Slate tokens~~ ✅ `ui/Theme.kt`
3. ~~Chrome — persistent app shell + transport bar~~ ✅ `ui/AppShell.kt`
4. Views — Home (now-playing inspector), Library (track table), Playlists (M3U),
   Settings. Sidebar playlists navigation wired to library filters (258a825).
5. ~~Design view integration and playback navigation~~ ✅ Home/Playlists views
   integrated with shell navigation; library row playback wired to the shared
   queue and verified with `./gradlew test`.

## Current functional work

The next implementation work is split into verified phases. Keep these changes
separate from unrelated UI redesign work already in progress.

### Phase 1 — Window and playback foundations

- Wire the custom minimize, maximize/restore, and close controls to the native
  Compose Desktop window.
- Open the app in a usable floating window by default; preserve valid saved
  size, position, and placement without forcing fullscreen.
- Keep playback actions on the shared `PlayerController`/`SongQueue`, including
  play/pause, previous, next, shuffle, repeat, seek, queue, and volume.
- Guard controls when no queue exists and verify queue boundary and shuffle
  behavior with tests.

### Phase 2 — Playlist functionality

- Implement playlist creation through the existing M3U/live-library flow.
- Make adding selected songs write to the active playlist and refresh library
  state.
- Validate playlist names, avoid duplicate entries, report filesystem errors,
  and test persistence/update behavior.

### Phase 3 — Responsive shell

- Verify compact, medium, and wide layouts; remove transport and playlist
  overflow caused by fixed desktop widths.
- Collapse navigation and settings into usable compact layouts.

### Phase 4 — Settings redesign

- Redesign `AppSettingsWindow` against `design/settings-page/settings.html`
  with category navigation, library/storage, audio, appearance, integration,
  and about panes.
- Every visible control must be backed by a real preference/backend capability
  or be explicitly presented as status-only.

Commit each meaningful, verified phase or sub-phase separately. Do not push
without explicit user approval.
