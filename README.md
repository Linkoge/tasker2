# Tasker2

A basic native Android task app (Kotlin, Jetpack Compose, Room). Local storage only, no account or sync.
Dark theme with category tabs, similar in look to Google Tasks.

## Features

- **Categories** shown as tabs across the top, each with its open-task count. Default categories
  are Daily, Weekly and Monthly. Names can be any Unicode text (for example Georgian).
  Use **⋮ → Manage categories** to add, rename, delete, and reorder them (↑/↓ arrows).
  The tab order is saved in the database. Deleting a category also deletes its tasks.
- **Tasks**: add with the **+** button. Tap a task to edit its title, notes, priority, category or
  completed state, or to delete it. Tap the circle to complete a task. Completed tasks go to a
  collapsible **Completed (N)** section; tap the check mark to reopen one.
- **Move a task** to another category: long-press it and pick **Move to…**, or change the
  Category dropdown in the editor.
- **Priority** from 1 to 10 (10 = most urgent, default 5). Open tasks are sorted highest priority
  first; tasks with the same priority are ordered by creation time (oldest first). Each row shows the
  priority number and an urgency bar on the right. The color runs from blue (low) to red (high).

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

## Tests

```bash
./gradlew testDebugUnitTest     # JVM unit tests: priority sorting, category reorder, moving tasks
python -m pytest -q -m smoke    # project-structure smoke checks
```
