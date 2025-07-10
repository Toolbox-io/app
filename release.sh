#!/bin/bash

# 1. Setup
set -e
cd "$(dirname "$0")"

# 2. Variables
TAG=$(git describe --tags --abbrev=0)
ASSET_PATH=./app/build/outputs/apk/release/app-release.apk
DISPLAY_NAME=Toolbox.io.$TAG.apk

# 3. Build release variant
./gradlew assembleRelease

# 4. Move locally
mv "$ASSET_PATH" "releases/$DISPLAY_NAME"
ASSET_PATH=./releases/$DISPLAY_NAME

# 6. Release to GitHub
prerelease_opt=$([[ "$TAG" != v*-pre* ]] || echo "--prerelease")

new_release() {
  gh release create \
    "$TAG" \
    --generate-notes \
    "$prerelease_opt" \
    "$ASSET_PATH"
}

edit_existing() {
  gh release edit \
    "$TAG" \
    "$prerelease_opt"
  gh release upload \
    "$TAG" \
    --clobber \
    "$ASSET_PATH"
}

git push --tags
new_release || edit_existing