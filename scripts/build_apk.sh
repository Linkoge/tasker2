#!/usr/bin/env bash
# Build the release APK (debug-signed unless keystore.properties exists), run unit tests,
# and copy the APK to dist/tasker2.apk and, optionally, to the path given as $1.
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
if [ ! -f local.properties ]; then
  [ -d "$SDK" ] || { echo "Android SDK not found at $SDK (set ANDROID_HOME)" >&2; exit 1; }
  echo "sdk.dir=$SDK" > local.properties
fi

./gradlew --no-daemon testDebugUnitTest assembleRelease

APK=app/build/outputs/apk/release/app-release.apk
mkdir -p dist
cp "$APK" dist/tasker2.apk
echo "APK: $(pwd)/dist/tasker2.apk"
if [ "${1:-}" != "" ]; then
  cp "$APK" "$1"
  echo "APK copied to: $1"
fi
