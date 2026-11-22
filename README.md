# Tasker2

A basic native Android task app (Kotlin, Jetpack Compose, Room). Local storage only, no account or sync.
Dark theme with category tabs, similar in look to Google Tasks.

## Features

- **Categories** shown as tabs across the top, each with its open-task count. Default categories
  are Daily, Weekly and Monthly. Names can be any Unicode text (for example Georgian).
  Use **⋮ → Manage categories** to add, rename, delete, and reorder them (↑/↓ arrows).
  The tab order is saved in the database. Deleting a category also deletes its tasks.
- **Tasks**: add with the **+** button, or many at once with the **+++** button (numbered title fields; field N unlocks once N-1 has text and a new empty one appears automatically; notes and priority are kept per title, one category for the whole batch; tasks are created in order 1, 2, 3…, blanks skipped). Tap a task to edit its title, notes, priority, category or
  completed state, or to delete it. Tap the circle to complete a task; long-press the circle to toggle a
  **crossed-out** state (strikethrough, grayed, stays in the open list). Completed tasks go to a
  collapsed-by-default section shown only as an arrow on the right; tap it to expand, tap the check mark to reopen one.
  An empty category shows nothing.
- **Move a task** to another category: long-press it and pick **Move to…**, or change the
  Category dropdown in the editor.
- **Priority** from 1 to 10 (10 = most urgent, default 5). Open tasks are sorted highest priority
  first; tasks with the same priority are ordered by creation time (oldest first). Each row shows the
  priority number and an urgency bar on the right. The color runs from gray-yellow (low) to red (high); the theme is dark orange.

## Project layout

- `app/src/main/java/dev/tasker2/data` holds the Room entities, DAO, database and repository.
- `app/src/main/java/dev/tasker2/domain/TaskLogic.kt` holds pure sorting, reorder and move logic,
  which the unit tests cover.
- `app/src/main/java/dev/tasker2/ui` holds the Compose screens (main, task editor, manage categories).
- `app/src/test` holds the JVM unit tests. `tests/` holds the pytest smoke checks.

## Build

Requirements: JDK 17 and an Android SDK with platform 36 (`ANDROID_HOME`, or `~/Android/Sdk`).
The Gradle wrapper downloads Gradle 9.1.

```bash
scripts/build_apk.sh                 # unit tests + release APK -> dist/tasker2.apk
scripts/build_apk.sh ../tasker2.apk  # also copy the APK to the given path
```

Or run the steps by hand: `./gradlew testDebugUnitTest assembleRelease`. The output is
`app/build/outputs/apk/release/app-release.apk`.

### Signing

By default the release build is signed with the local Android debug key (`~/.android/debug.keystore`),
so it installs directly. To use your own key, create a git-ignored `keystore.properties` in the
repo root:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Never commit keystores or passwords. `.gitignore` already excludes `*.jks`, `*.keystore` and
`keystore.properties`.

## Install

```bash
adb install -r dist/tasker2.apk
```

You can also copy the APK to the phone and open it there. You must allow installs from unknown
sources. An APK signed with a different key cannot be installed over an existing one; uninstall
the old app first.

## Deploy

Deploying means publishing a verified release APK to `../tasker2.apk` (next to the repository).
`.linko-deploy.json` wires these scripts into the automatic deploy. They run from the repository root:

- `scripts/backup.sh` copies the published APK into the backup directory. If no APK is published yet, it writes an
  `.absent` marker instead. It keeps the newest 5 entries.
- `scripts/deploy.sh` runs `scripts/build_apk.sh` in fast mode (`SKIP_TESTS=1`: `assembleRelease` only, reusable Gradle daemon, `--offline` with
  online fallback; unit tests already ran before merge), checks the new APK, and then
  replaces the published one atomically. It also keeps a versioned copy `../tasker2-apks/tasker2-v<versionName>.apk`
  (versionName read from `app/build.gradle.kts`; old versioned copies are never deleted; override the folder with `TASKER2_VERSIONED_DIR`). A failed build or check leaves the published APK as it was.
- `scripts/health.sh` passes when the published APK is a valid zip whose manifest names `dev.tasker2` and that
  has dex code (`scripts/deploy/apk_check.py`).
- `scripts/rollback.sh` restores the newest backup (only `tasker2.apk`; versioned copies are left alone). If that backup is an `.absent` marker, it removes the
  published APK.

You can override these environment variables: `TASKER2_APK_TARGET` (publish path), `TASKER2_BACKUP_DIR`
(defaults to the runner's `DEPLOY_BACKUP_DIR`, then `~/.local/share/tasker2/backups`), `TASKER2_KEEP_BACKUPS`,
`TASKER2_BUILD_CMD` and `TASKER2_BUILT_APK`. Running `scripts/build_apk.sh` by hand without flags still runs the
unit tests (`--skip-tests` or `SKIP_TESTS=1` gives the fast build). The scripts never restart services and never install to devices.

## Versioning

The current version is shown in the app's ⋮ menu (disabled "Version x.y" item). For each release: edit
`versionName` and raise `versionCode` (by at least 1) in `app/build.gradle.kts`, then add a line to `CHANGELOG.md`.
The deploy reads `versionName` from that file to name the versioned APK.

## Tests

```bash
./gradlew testDebugUnitTest     # JVM unit tests: priority sorting, category reorder, moving tasks, bulk-add form logic
python -m pytest -q -m smoke    # project-structure smoke checks
python -m pytest -q             # also the deploy script tests (disposable dirs, fake build)
```

UI notes: category tabs have no ripple/pressed background (selection = text color + underline) and are
spaced by the width of three spaces in the 15sp tab font. The Completed section is just an expand arrow
(collapsed by default) with no background.
