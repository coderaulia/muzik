# Changelog

All notable changes to MuzikPlayer are documented in this file.

## [1.5.4] — 2026-09-13

### Added
- **Zero-Dependency Portable Bundle** — Bundled JRE archive (`.tar.gz`) with user-space `install.sh` and `uninstall.sh` scripts for one-click desktop launcher and icon registration on any Linux distribution without root privileges.
- **Native RPM Packaging** — Self-contained `.rpm` package with embedded JVM runtime for Fedora, RHEL, and openSUSE.
- **Automated GitHub Actions Releases** — Root `.github/workflows/tagged-release.yml` for multi-architecture builds producing `.deb`, `.rpm`, and portable `.tar.gz` release artifacts automatically on tag push (`v*`).

### Changed
- **Installation Documentation** — Detailed package installation guides for daily users (no dependencies needed) and build prerequisites for developers across Fedora, Debian/Ubuntu, and Arch Linux.
- **Release Build Optimization** — Made ProGuard opt-in via `-PenableProguard=true` to enable reliable, fast jpackage and distributable builds across all modern JDK environments.

## [1.5.3] — 2026-09-09

### Added
- **Light Theme** — Full Libadwaita Slate Light theme with dynamic token switching across the entire app (shell, sidebar, transport bar, all views, settings).
- **Theme Toggle** — System / Dark / Light color scheme selector in Settings → Appearance that immediately updates all UI surfaces.
- **Settings Enhancements**
  - Real-time search filtering across all preference panes.
  - Library rescan trigger from Settings → Library.
  - ListenBrainz token input dialog with persistent storage and connection status badge.
- **Release Artifacts** — Portable `tar.gz` archive for Linux x86_64.

### Changed
- **Dynamic Color Tokens** — Migrated all hardcoded dark hex colors in `AppShell.kt`, `HomePage.kt`, `PlaylistsPage.kt`, `LibraryHeader.kt`, and `AppSettings.kt` to composable properties reading from `LocalMuzikColors.current`, enabling live theme switching.
- **Theme Architecture** — Added `MuzikColors` data class, `SlateDarkColors`, `SlateLightColors` palettes, and `MuzikTheme.Provider` composable in `Theme.kt`.
- **README** — Complete rewrite with features, installation, project structure, keyboard shortcuts, and tech stack documentation.

### Fixed
- **Font Preload Crash** — Fixed `SIGSEGV` in `Main.kt` caused by concurrent multi-threaded calls to `fontResolver.resolve` during Skiko native font registration.
- **Compilation Errors** — Fixed missing `pathString` import in `HomePage.kt`, missing `repeatMode` declaration in `AppShell.kt`, and illegal composable call in `remember` in `LibraryHeader.kt`.

## [1.5.2] — Previous Release

### Added
- Redesigned settings page with Libadwaita Slate design specifications.
- Wired all UI elements to live audio metadata, library data, and playback processes.
- Native Debian and RPM release targets in build configuration.
- Flatpak release metadata preparation.
- Responsive compact shell navigation.
- Playlist persistence testing and hardening.
- Playback queue foundation tests.
- Bundled audio decoder for common formats.
- Home and playlist page actions.
- Responsive transport bar.
- Player queue command handling.
