# Tasker2

A basic native Android task app (Kotlin, Jetpack Compose, Room). Local storage only, no account or sync.
Dark theme with category tabs, similar in look to Google Tasks.

## Features

- **Categories** shown as tabs across the top, each with its open-task count. Default categories
  are Daily, Weekly and Monthly. Names can be any Unicode text (for example Georgian).
  Use **⋮ → Manage categories** to add, rename, delete, and reorder them (↑/↓ arrows).
  The tab order is saved in the database. Deleting a category also deletes its tasks.
- **Tasks**: add with the **+** button, or many at once with the **+++** button (numbered title fields; field N unlocks once N-1 has text and a new empty one appears automatically; notes and priority are kept per title (default: no priority), one category for the whole batch; tasks are created in order 1, 2, 3…, blanks skipped). Tap a task to edit its title, notes, priority, category or
  completed state, or to delete it. Tap the round circle on the left to complete a task (works even when it is crossed out); tap the square checkbox on the far right to toggle a
  **crossed-out** state (thick strikethrough, dimmed title and dimmed-but-colored priority, stays in the open list). Completed tasks go to a
  collapsed-by-default section shown only as an arrow on the right; tap it to expand, tap the check mark to reopen one.
  An empty category shows nothing.
- **Move a task** to another category: long-press it and pick **Move to…**, or change the
  Category dropdown in the editor.
- **Priority**: None (default, stored as 0, so no database migration) or 1 to 10 (10 = most urgent). Open tasks are sorted highest priority
  first, tasks with no priority last; ties are ordered by creation time (oldest first). A row with a priority shows the
  priority number and an urgency bar on the right; a row with none shows neither. The color runs from gray-yellow (low) to red (high); the theme is dark orange.

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

### GitHub publish (after the APK is published)

After a successful, checked deploy, `scripts/deploy.sh` runs `scripts/deploy/github.sh`:

1. `git push origin main` (plain fast-forward; never forced, nothing is ever deleted remotely).
2. Creates tag `v<versionName>` on HEAD if missing and pushes it. A tag that already exists elsewhere is never moved (warning, no release).
3. Creates a GitHub Release `v<versionName>` with `tasker2-v<versionName>.apk` attached and the version's entry from
   `CHANGELOG.md` as notes. If the release exists, only a missing asset is uploaded; releases are never deleted.
   Uses `gh` when installed and logged in (or `GH_TOKEN`/`GITHUB_TOKEN` set), otherwise curl with `GITHUB_TOKEN` (or `GH_TOKEN`).

Everything here is non-fatal: with no credentials, no network or a rejected push it prints
`deploy: github: skipped/failed: <reason>` and the deploy still exits 0 (the APK is not rolled back). The whole step is
limited to 40 s (`TASKER2_GITHUB_BUDGET`, per command `TASKER2_GITHUB_STEP_MAX`). Set `TASKER2_GITHUB_PUBLISH=0` to disable it.
Pushing needs SSH access to `origin`; the release needs `gh auth login` or a token with `contents: write` on the repo.
APKs (`dist/` is git-ignored) only go to Releases, never into git history. The deploy tests disable the step by default and
test it with fake `git`/`gh`/`curl`.

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

- **Settings** (⋮ → Settings), saved in SharedPreferences and applied immediately: category tab font size (11–24sp, default 15),
  task list font size (12–24sp, default 17; notes scale proportionally), and the gap between tabs in spaces (1–12, default 6,
  measured at the current tab font size). Each control shows its value and a live preview; **Reset to defaults** restores them.
  Also: gap between task rows (0–24dp slider, default 0 = unchanged) and two switches, *Show priority number* and *Show priority bar* (both default on; tasks without priority show neither). Crossed-out rows are drawn in pure neutral grays only (`TaskLogic.Gray`). Logic is in `domain/AppSettings.kt` and `data/SettingsRepository.kt`.
- **+++ screen layout**: the whole form is one scroll container with `imePadding`, and the focused title is brought into view with
  its neighbours, so the keyboard can no longer squeeze the title list away. Enter/Next moves to the next title.

UI notes: category tabs have no ripple/pressed background (selection = text color + underline) and are
spaced by the width of six spaces (default) in the 15sp tab font. The Completed section is just an expand arrow
(collapsed by default) with no background.
