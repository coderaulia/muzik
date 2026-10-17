# MuzikPlayer Multiplatform Architecture (Linux • Windows • Android)

This document describes the multi-module Kotlin Multiplatform (KMP) layout inside `src/` for running MuzikPlayer with 1:1 feature parity across **Linux Desktop**, **Windows Desktop**, and **Android Mobile**.

## Directory Structure

```text
muzik/
├── design/                        # Shared Libadwaita Slate HTML mockups & tokens
└── src/                           # Gradle multi-project root
    ├── build.gradle.kts           # Root desktop build (Linux DEB/RPM + Windows MSI/EXE)
    ├── settings.gradle.kts        # Includes :shared, :desktopApp, and conditional :androidApp
    ├── shared/                    # KMP shared module (commonMain, desktopMain, androidMain)
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── commonMain/kotlin/io/github/coderaulia/muzikplayer/platform/PlatformContracts.kt
    │       ├── desktopMain/kotlin/io/github/coderaulia/muzikplayer/platform/DesktopPlatform.kt
    │       └── androidMain/kotlin/io/github/coderaulia/muzikplayer/platform/AndroidPlatform.kt
    ├── desktopApp/                # Desktop KMP target (Linux & Windows platform glue)
    │   ├── build.gradle.kts
    │   └── src/jvmMain/kotlin/io/github/coderaulia/muzikplayer/desktop/WindowsSupport.kt
    └── androidApp/                # Android Mobile application module
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml
            └── kotlin/io/github/coderaulia/muzikplayer/android/
                ├── MainActivity.kt
                └── MuzikPlaybackService.kt
```

## Building & Running

### Linux & Windows Desktop
```bash
cd src
./gradlew run                           # Run desktop app (loads .so on Linux, .dll on Windows)
./gradlew test                          # Run unit tests
./gradlew packageDistributionForCurrentOS # Build .deb/.rpm on Linux or .msi/.exe on Windows
```

### Android Mobile
1. Android SDK is installed on this machine at `/home/asw/Android/Sdk` and configured in `src/local.properties` (`sdk.dir=/home/asw/Android/Sdk`).
2. Build the installable Android APK:
```bash
cd src
./gradlew :androidApp:assembleDebug
# Output APK: src/androidApp/build/outputs/apk/debug/androidApp-debug.apk
```
3. Install onto a connected Android phone or emulator via `adb`:
```bash
/home/asw/Android/Sdk/platform-tools/adb install -r src/androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

