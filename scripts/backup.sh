#!/usr/bin/env bash
# Save the currently published APK so rollback.sh can restore it. If nothing is published yet,
# record an ".absent" marker so a rollback removes the new APK instead of leaving it.
. "$(dirname "${BASH_SOURCE[0]}")/deploy/common.sh"

mkdir -p "$TASKER2_BACKUP_DIR"
name="tasker2-$(stamp)"
if [ -f "$TASKER2_APK_TARGET" ]; then
    cp -- "$TASKER2_APK_TARGET" "$TASKER2_BACKUP_DIR/$name.apk"
    echo "backup: $TASKER2_BACKUP_DIR/$name.apk"
else
    : > "$TASKER2_BACKUP_DIR/$name.absent"
    echo "backup: nothing published at $TASKER2_APK_TARGET yet (marker $name.absent)"
fi

# Keep only the newest TASKER2_KEEP_BACKUPS entries (names sort by time).
mapfile -t entries < <(find "$TASKER2_BACKUP_DIR" -maxdepth 1 -type f \( -name 'tasker2-*.apk' -o -name 'tasker2-*.absent' \) | sort)
for ((i = 0; i < ${#entries[@]} - TASKER2_KEEP_BACKUPS; i++)); do rm -f -- "${entries[i]}"; done
