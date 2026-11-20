"""Fast structural smoke checks for the Android project (no Gradle build)."""
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/dev/tasker2"


@pytest.mark.smoke
def test_gradle_project_files_exist():
    for rel in [
        "settings.gradle.kts",
        "build.gradle.kts",
        "app/build.gradle.kts",
        "gradlew",
        "gradle/wrapper/gradle-wrapper.properties",
        "gradle/wrapper/gradle-wrapper.jar",
        "app/src/main/AndroidManifest.xml",
        "scripts/build_apk.sh",
        "README.md",
    ]:
        assert (ROOT / rel).is_file(), rel


@pytest.mark.smoke
def test_core_sources_and_unit_tests_exist():
    for rel in ["MainActivity.kt", "data/Entities.kt", "data/Dao.kt", "data/AppDatabase.kt",
                "data/TaskRepository.kt", "domain/TaskLogic.kt", "ui/MainScreen.kt",
                "ui/ManageCategoriesScreen.kt", "ui/TaskEditorScreen.kt"]:
        assert (SRC / rel).is_file(), rel
    tests = ROOT / "app/src/test/java/dev/tasker2"
    assert (tests / "TaskLogicTest.kt").is_file()
    assert (tests / "TaskRepositoryTest.kt").is_file()


@pytest.mark.smoke
def test_room_and_compose_configured():
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    for needle in ["androidx.room:room-runtime", "room-compiler", "material3", "compose = true"]:
        assert needle in gradle, needle


@pytest.mark.smoke
def test_no_keystore_or_apk_committed_paths_ignored():
    ignore = (ROOT / ".gitignore").read_text().split()
    for pattern in ["dist/", "*.jks", "keystore.properties", "local.properties"]:
        assert pattern in ignore, pattern
