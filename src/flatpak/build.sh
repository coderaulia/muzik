#!/usr/bin/env bash

set -ex

# Build JAR
# Uncomment for release builds
./gradlew createReleaseDistributable
#./gradlew createReleaseDistributable -POptimizeProGuard=false

# Running the app to collect classlist if not already present
CLASSLIST="build/compose/binaries/main-release/app/MuzikPlayer/lib/app/resources/MuzikPlayer.classlist"
if [ ! -f "$CLASSLIST" ]; then
    if [ -f "flatpak/MuzikPlayer.classlist" ]; then
        mkdir -p "$(dirname "$CLASSLIST")"
        cp flatpak/MuzikPlayer.classlist "$CLASSLIST"
    else
        cp build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg.bkp
        echo "java-options=-Xshare:off" >> build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg
        echo "java-options=-XX:DumpLoadedClassList=$CLASSLIST" >> build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg
        build/compose/binaries/main-release/app/MuzikPlayer/bin/MuzikPlayer
        mv build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg.bkp build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg
    fi
fi


# Build and install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.coderaulia.MuzikPlayer.yml
