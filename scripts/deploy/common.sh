# Shared settings for the tasker2 deploy scripts. Source it; do not run it.
# "Deploying" tasker2 means publishing a verified release APK to TASKER2_APK_TARGET
# (default: tasker2.apk next to the repository, as documented in README.md).
# Every path is overridable through the environment so tests can use disposable directories.
set -euo pipefail

TASKER2_REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TASKER2_APK_TARGET="${TASKER2_APK_TARGET:-$(dirname "$TASKER2_REPO")/tasker2.apk}"
TASKER2_STATE_DIR="${TASKER2_STATE_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/tasker2}"
TASKER2_BACKUP_DIR="${TASKER2_BACKUP_DIR:-${DEPLOY_BACKUP_DIR:-$TASKER2_STATE_DIR/backups}}"
TASKER2_KEEP_BACKUPS="${TASKER2_KEEP_BACKUPS:-5}"
# Command that builds dist/tasker2.apk inside the repository (runs unit tests first).
TASKER2_BUILD_CMD="${TASKER2_BUILD_CMD:-$TASKER2_REPO/scripts/build_apk.sh}"
TASKER2_BUILT_APK="${TASKER2_BUILT_APK:-$TASKER2_REPO/dist/tasker2.apk}"

stamp() { date -u +%Y%m%dT%H%M%S%N; }

# check_apk <path>: fail unless <path> is a real tasker2 APK.
check_apk() { python3 -I "$TASKER2_REPO/scripts/deploy/apk_check.py" "$1"; }

# publish <src>: atomically replace the published APK with <src>.
publish() {
    local dir tmp
    dir="$(dirname "$TASKER2_APK_TARGET")"
    mkdir -p "$dir"
    tmp="$(mktemp "$dir/.tasker2.apk.XXXXXX")"
    cp -- "$1" "$tmp"
    chmod 644 "$tmp"
    mv -f -- "$tmp" "$TASKER2_APK_TARGET"
}
