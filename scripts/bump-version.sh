#!/usr/bin/env bash
set -e

# Bumps the MuzikPlayer version in every place it's hardcoded.
#
# The single source of truth is `version = "..."` in src/build.gradle.kts.
# This script updates it and propagates the value to README.md, which
# otherwise duplicates it by hand in install command examples and the
# "Version:" field. It does NOT touch CHANGELOG.md: changelog prose needs a
# human, so it just reminds you to add an entry.

usage() {
    cat <<EOF
Usage: $(basename "$0") <new-version>

Example: $(basename "$0") 1.5.5
EOF
}

if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    usage
    exit 0
fi

if [ $# -ne 1 ]; then
    usage >&2
    exit 1
fi

NEW_VERSION="$1"
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: expected a version like 1.5.5, got '$NEW_VERSION'." >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_GRADLE="$REPO_ROOT/src/build.gradle.kts"
README="$REPO_ROOT/README.md"

OLD_VERSION="$(grep -oP '(?<=^version = ")[^"]+' "$BUILD_GRADLE")"
if [ -z "$OLD_VERSION" ]; then
    echo "Error: could not find 'version = \"...\"' in $BUILD_GRADLE." >&2
    exit 1
fi

if [ "$OLD_VERSION" = "$NEW_VERSION" ]; then
    echo "Error: $BUILD_GRADLE is already at version $NEW_VERSION." >&2
    exit 1
fi

echo "Bumping MuzikPlayer version: $OLD_VERSION -> $NEW_VERSION"

sed -i "s|^version = \"$OLD_VERSION\"|version = \"$NEW_VERSION\"|" "$BUILD_GRADLE"

# Only replace the old version string where it appears as a whole dotted
# number (avoids accidentally touching an unrelated string that merely
# contains the old version as a substring).
sed -i "s|$OLD_VERSION|$NEW_VERSION|g" "$README"

echo "Updated: $BUILD_GRADLE"
echo "Updated: $README"
echo ""
echo "Still needed:"
echo "  - Add a new entry to CHANGELOG.md for $NEW_VERSION (not automated)."
echo "  - Review 'git diff' before committing."
