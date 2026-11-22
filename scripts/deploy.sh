#!/usr/bin/env bash
# Build the release APK (fast build: no unit tests, they run before merge), verify it, then publish it atomically.
# The published APK is only replaced after the new one passes the APK check.
. "$(dirname "${BASH_SOURCE[0]}")/deploy/common.sh"
. "$(dirname "${BASH_SOURCE[0]}")/deploy/github.sh"

cd "$TASKER2_REPO"
export SKIP_TESTS="${SKIP_TESTS:-1}"
rm -f -- "$TASKER2_BUILT_APK"
"$TASKER2_BUILD_CMD"
[ -f "$TASKER2_BUILT_APK" ] || { echo "deploy: build produced no $TASKER2_BUILT_APK" >&2; exit 1; }
check_apk "$TASKER2_BUILT_APK"
VERSION="$(app_version)"
publish_versioned "$TASKER2_BUILT_APK" "$VERSION"
publish "$TASKER2_BUILT_APK"
echo "deploy: published $TASKER2_APK_TARGET and $TASKER2_VERSIONED_DIR/tasker2-v$VERSION.apk${DEPLOY_COMMIT:+ (commit $DEPLOY_COMMIT)}"
# Optional, non-fatal: push main + tag and create the GitHub Release (TASKER2_GITHUB_PUBLISH=0 disables).
github_publish "$TASKER2_BUILT_APK" "$VERSION" || true
