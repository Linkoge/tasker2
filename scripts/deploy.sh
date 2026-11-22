#!/usr/bin/env bash
# Build the release APK (fast build: no unit tests, they run before merge), verify it, then publish it atomically.
# The published APK is only replaced after the new one passes the APK check.
. "$(dirname "${BASH_SOURCE[0]}")/deploy/common.sh"

cd "$TASKER2_REPO"
export SKIP_TESTS="${SKIP_TESTS:-1}"
rm -f -- "$TASKER2_BUILT_APK"
"$TASKER2_BUILD_CMD"
[ -f "$TASKER2_BUILT_APK" ] || { echo "deploy: build produced no $TASKER2_BUILT_APK" >&2; exit 1; }
check_apk "$TASKER2_BUILT_APK"
publish "$TASKER2_BUILT_APK"
echo "deploy: published $TASKER2_APK_TARGET${DEPLOY_COMMIT:+ (commit $DEPLOY_COMMIT)}"
