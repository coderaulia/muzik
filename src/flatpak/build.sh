#!/usr/bin/env bash

set -ex

# Build JAR
# Uncomment for release builds
./gradlew createReleaseDistributable
#./gradlew createReleaseDistributable -POptimizeProGuard=false

# Running the app to collect classlist
cp build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg.bkp
echo "java-options=-Xshare:off" >> build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg
echo "java-options=-XX:DumpLoadedClassList=build/compose/binaries/main-release/app/MuzikPlayer/lib/app/resources/MuzikPlayer.classlist" >> build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg
build/compose/binaries/main-release/app/MuzikPlayer/bin/MuzikPlayer

mv build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg.bkp build/compose/binaries/main-release/app/MuzikPlayer/lib/app/MuzikPlayer.cfg


# Build and install flatpak
flatpak-builder --user --install --force-clean build-dir flatpak/io.github.coderaulia.MuzikPlayer.yml
