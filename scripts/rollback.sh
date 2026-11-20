#!/usr/bin/env bash
# Restore the newest backup made by backup.sh. An ".absent" backup means nothing was published
# before, so the published APK is removed.
. "$(dirname "${BASH_SOURCE[0]}")/deploy/common.sh"

latest="$(find "$TASKER2_BACKUP_DIR" -maxdepth 1 -type f \( -name 'tasker2-*.apk' -o -name 'tasker2-*.absent' \) 2>/dev/null | sort | tail -n 1)"
if [ -z "$latest" ]; then
    echo "rollback: no backup in $TASKER2_BACKUP_DIR" >&2
    exit 1
fi
case "$latest" in
    *.absent)
        rm -f -- "$TASKER2_APK_TARGET"
        echo "rollback: removed $TASKER2_APK_TARGET (nothing was published before)"
        ;;
    *)
        check_apk "$latest"
        publish "$latest"
        echo "rollback: restored $TASKER2_APK_TARGET from $latest"
        ;;
esac
