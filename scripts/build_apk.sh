#!/usr/bin/env bash
# Build the release APK (debug-signed unless keystore.properties exists), run unit tests,
# and copy the APK to dist/tasker2.apk and, optionally, to the path given as $1.
# Fast mode (SKIP_TESTS=1 or --skip-tests, used by scripts/deploy.sh): assembleRelease only, reusable Gradle
# daemon, --offline when it works (falls back to online). Without it, tests run and no daemon is kept.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ "${1:-}" = "--skip-tests" ]; then SKIP_TESTS=1; shift; fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
if [ ! -f local.properties ]; then
  [ -d "$SDK" ] || { echo "Android SDK not found at $SDK (set ANDROID_HOME)" >&2; exit 1; }
  echo "sdk.dir=$SDK" > local.properties
fi

if [ "${SKIP_TESTS:-0}" = "1" ]; then
  # Fast deploy path: no unit tests (they run in the pre-merge checks), daemon reused.
  ./gradlew --offline assembleRelease || {
    echo "build_apk: offline build failed, retrying online" >&2
    ./gradlew assembleRelease
  }
else
  ./gradlew --no-daemon testDebugUnitTest assembleRelease
fi

APK=app/build/outputs/apk/release/app-release.apk
mkdir -p dist
cp "$APK" dist/tasker2.apk
echo "APK: $(pwd)/dist/tasker2.apk"
if [ "${1:-}" != "" ]; then
  cp "$APK" "$1"
  echo "APK copied to: $1"
fi
