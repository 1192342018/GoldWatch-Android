#!/bin/sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
BOOT_DIR="$APP_HOME/.gradle-bootstrap"
GRADLE_HOME="$BOOT_DIR/gradle-$GRADLE_VERSION"
ZIP="$BOOT_DIR/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BOOT_DIR"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -L --fail -o "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required to bootstrap Gradle." >&2
      exit 1
    fi
  fi
  unzip -q -o "$ZIP" -d "$BOOT_DIR"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
