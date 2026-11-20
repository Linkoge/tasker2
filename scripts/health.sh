#!/usr/bin/env bash
# Healthy when the published APK exists and is a valid tasker2 APK.
. "$(dirname "${BASH_SOURCE[0]}")/deploy/common.sh"

check_apk "$TASKER2_APK_TARGET"
