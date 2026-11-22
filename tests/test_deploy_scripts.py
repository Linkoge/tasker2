"""Deploy/backup/health/rollback scripts, run against disposable directories only.

The build is replaced by a fake command, and fake systemctl/curl/adb/gradlew shims sit first on
PATH and record any call, so the tests prove the scripts never touch services or devices.
"""
import json
import os
import subprocess
import zipfile
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"


def make_apk(path, package="dev.tasker2", marker="v1", dex=True):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as apk:
        apk.writestr("AndroidManifest.xml", b"\x03\x00\x08\x00" + package.encode("utf-16-le"))
        if dex:
            apk.writestr("classes.dex", b"dex\n035\x00")
        apk.writestr("assets/marker.txt", marker)
    return path


def marker(path):
    with zipfile.ZipFile(path) as apk:
        return apk.read("assets/marker.txt").decode()


@pytest.fixture
def env(tmp_path):
    shims = tmp_path / "shims"
    shims.mkdir()
    calls = tmp_path / "shim-calls.log"
    for tool in ["systemctl", "curl", "adb", "gradlew"]:
        shim = shims / tool
        shim.write_text(f'#!/bin/sh\necho "{tool} $*" >> "{calls}"\nexit 97\n')
        shim.chmod(0o755)
    built = tmp_path / "build-out" / "tasker2.apk"
    fake_build = tmp_path / "fake_build.sh"
    # The fake build copies $FAKE_BUILD_SOURCE to the built-APK path, or fails if FAKE_BUILD_FAIL is set.
    fake_build.write_text(
        '#!/bin/sh\nset -e\n[ -z "$FAKE_BUILD_FAIL" ] || { echo "build failed" >&2; exit 3; }\n'
        f'mkdir -p "{built.parent}"\n[ -z "$FAKE_BUILD_SOURCE" ] || cp "$FAKE_BUILD_SOURCE" "{built}"\n'
    )
    fake_build.chmod(0o755)
    values = {
        "PATH": f"{shims}:{os.environ['PATH']}",
        "HOME": str(tmp_path / "home"),
        "TASKER2_APK_TARGET": str(tmp_path / "published" / "tasker2.apk"),
        "TASKER2_STATE_DIR": str(tmp_path / "state"),
        "TASKER2_BACKUP_DIR": str(tmp_path / "backups"),
        "TASKER2_BUILD_CMD": str(fake_build),
        "TASKER2_BUILT_APK": str(built),
    }
    environment = {k: v for k, v in os.environ.items() if not k.startswith(("TASKER2_", "DEPLOY_"))}
    values["TASKER2_GITHUB_PUBLISH"] = "0"  # tests never touch GitHub unless a test opts in with fakes
    environment.update(values)

    class Env:
        tmp = tmp_path
        target = Path(values["TASKER2_APK_TARGET"])
        backups = Path(values["TASKER2_BACKUP_DIR"])

        def run(self, script, **extra):
            result = subprocess.run(
                [str(SCRIPTS / script)], cwd=ROOT, env={**environment, **extra},
                capture_output=True, text=True, timeout=60,
            )
            return result

        def shim_calls(self):
            return calls.read_text() if calls.exists() else ""

    yield Env()
    assert Env().shim_calls() == "", "deploy scripts must not call systemctl/curl/adb/gradlew"


def test_full_cycle_backup_deploy_health_rollback(env):
    make_apk(env.target, marker="old")
    new = make_apk(env.tmp / "src" / "new.apk", marker="new")

    assert env.run("backup.sh").returncode == 0
    deployed = env.run("deploy.sh", FAKE_BUILD_SOURCE=str(new), DEPLOY_COMMIT="abc123")
    assert deployed.returncode == 0, deployed.stderr
    assert "abc123" in deployed.stdout
    assert marker(env.target) == "new"
    assert env.run("health.sh").returncode == 0

    rolled = env.run("rollback.sh")
    assert rolled.returncode == 0, rolled.stderr
    assert marker(env.target) == "old"
    assert env.run("health.sh").returncode == 0


def test_deploy_rejects_invalid_build_and_keeps_published_apk(env):
    make_apk(env.target, marker="old")
    for bad in [make_apk(env.tmp / "bad1.apk", package="com.other.app"),
                make_apk(env.tmp / "bad2.apk", dex=False)]:
        result = env.run("deploy.sh", FAKE_BUILD_SOURCE=str(bad))
        assert result.returncode != 0
        assert marker(env.target) == "old"
    not_zip = env.tmp / "bad3.apk"
    not_zip.write_text("not a zip")
    assert env.run("deploy.sh", FAKE_BUILD_SOURCE=str(not_zip)).returncode != 0
    assert marker(env.target) == "old"


def test_deploy_fails_when_build_fails_or_produces_nothing(env):
    make_apk(env.target, marker="old")
    stale = make_apk(env.tmp / "build-out" / "tasker2.apk", marker="stale")
    assert env.run("deploy.sh", FAKE_BUILD_FAIL="1").returncode != 0
    assert not stale.exists(), "a stale build output must not be published"
    assert env.run("deploy.sh").returncode != 0
    assert marker(env.target) == "old"


def test_health_fails_for_missing_or_corrupt_apk(env):
    assert env.run("health.sh").returncode != 0
    env.target.parent.mkdir(parents=True)
    env.target.write_bytes(b"PK\x03\x04garbage")
    assert env.run("health.sh").returncode != 0


def test_first_deploy_rollback_removes_published_apk(env):
    assert env.run("backup.sh").returncode == 0
    assert list(env.backups.glob("*.absent"))
    new = make_apk(env.tmp / "new.apk", marker="new")
    assert env.run("deploy.sh", FAKE_BUILD_SOURCE=str(new)).returncode == 0
    assert env.target.exists()
    assert env.run("rollback.sh").returncode == 0
    assert not env.target.exists()


def test_rollback_without_backup_fails_and_leaves_apk(env):
    make_apk(env.target, marker="current")
    assert env.run("rollback.sh").returncode != 0
    assert marker(env.target) == "current"


def test_backup_prunes_to_keep_limit_and_uses_deploy_backup_dir(env):
    make_apk(env.target)
    for _ in range(4):
        assert env.run("backup.sh", TASKER2_KEEP_BACKUPS="2").returncode == 0
    assert len(list(env.backups.glob("tasker2-*.apk"))) == 2

    app_dir = env.tmp / "app-backups"
    result = env.run("backup.sh", TASKER2_BACKUP_DIR="", DEPLOY_BACKUP_DIR=str(app_dir))
    # An empty TASKER2_BACKUP_DIR falls back to DEPLOY_BACKUP_DIR provided by the deploy runner.
    assert result.returncode == 0, result.stderr
    assert len(list(app_dir.glob("tasker2-*.apk"))) == 1


def test_linko_deploy_json_points_at_executable_scripts():
    config = json.loads((ROOT / ".linko-deploy.json").read_text())
    assert set(config) == {"deploy_command", "backup_command", "health_command", "rollback_command"}
    for argv in config.values():
        assert isinstance(argv, list) and argv and all(isinstance(a, str) for a in argv)
        script = ROOT / argv[0]
        assert script.is_file() and os.access(script, os.X_OK), argv[0]


def test_deploy_build_is_fast_and_manual_build_runs_tests():
    deploy = (SCRIPTS / "deploy.sh").read_text()
    build = (SCRIPTS / "build_apk.sh").read_text()
    assert 'SKIP_TESTS="${SKIP_TESTS:-1}"' in deploy
    # Fast path: no unit tests, daemon reusable, offline first with online fallback.
    fast, slow = build.split('if [ "${SKIP_TESTS:-0}" = "1" ]; then')[1].split("else")
    assert "testDebugUnitTest" not in fast and "--no-daemon" not in fast
    assert "--offline assembleRelease" in fast and "retrying online" in fast
    # Manual run without flags keeps the unit tests.
    assert "testDebugUnitTest assembleRelease" in slow


def test_deploy_passes_skip_tests_to_build_command(env):
    make_apk(env.tmp / "src.apk")
    probe = env.tmp / "probe.sh"
    probe.write_text(
        f'#!/bin/sh\necho "SKIP_TESTS=$SKIP_TESTS" > "{env.tmp}/probe.out"\n'
        f'mkdir -p "{env.tmp}/build-out"\ncp "{env.tmp}/src.apk" "{env.tmp}/build-out/tasker2.apk"\n'
    )
    probe.chmod(0o755)
    assert env.run("deploy.sh", TASKER2_BUILD_CMD=str(probe)).returncode == 0
    assert (env.tmp / "probe.out").read_text().strip() == "SKIP_TESTS=1"


def test_gradle_properties_enable_caching():
    props = (ROOT / "gradle.properties").read_text()
    for line in ["org.gradle.caching=true", "org.gradle.parallel=true"]:
        assert line in props


def test_deploy_keeps_versioned_copy_and_rollback_leaves_it(env):
    import re
    version = re.search(r'versionName\s*=\s*"([^"]+)"', (ROOT / "app/build.gradle.kts").read_text()).group(1)
    versioned = env.target.parent / "tasker2-apks" / f"tasker2-v{version}.apk"
    old_copy = env.target.parent / "tasker2-apks" / "tasker2-v0.0.apk"
    old_copy.parent.mkdir(parents=True)
    old_copy.write_text("old release")
    assert env.run("backup.sh").returncode == 0
    new = make_apk(env.tmp / "new.apk", marker="new")
    assert env.run("deploy.sh", FAKE_BUILD_SOURCE=str(new)).returncode == 0
    assert marker(env.target) == "new" and marker(versioned) == "new"
    assert old_copy.read_text() == "old release"
    assert env.run("rollback.sh").returncode == 0
    assert not env.target.exists() and marker(versioned) == "new"


# ---- GitHub publish step: fake git/gh/curl on PATH, no network ----

HEAD_SHA = "a" * 40


def _version():
    import re
    return re.search(r'versionName\s*=\s*"([^"]+)"', (ROOT / "app/build.gradle.kts").read_text()).group(1)


@pytest.fixture
def gh_env(env):
    d = env.tmp / "ghshims"
    d.mkdir()
    log = env.tmp / "gh-calls.log"
    state = env.tmp / "gh-state"
    state.mkdir()

    def shim(name, body):
        p = d / name
        p.write_text(f"#!/usr/bin/env python3\nimport os, sys\nopen({str(log)!r}, 'a').write('{name} ' + ' '.join(sys.argv[1:]) + '\\n')\n"
                     f"S = {str(state)!r}\n" + body)
        p.chmod(0o755)

    shim("git", '''a = sys.argv[1:]
E = os.environ
if a[:2] == ["remote", "get-url"]: print("git@github.com:Linkoge/tasker2.git")
elif a[:1] == ["push"]:
    if E.get("FAKE_PUSH_FAIL"): sys.exit(1)
elif a[:2] == ["rev-parse", "HEAD"]: print("a" * 40)
elif a[:1] == ["rev-parse"]:
    if E.get("FAKE_LOCAL_TAG"): print(E["FAKE_LOCAL_TAG"])
    else: sys.exit(1)
elif a[:1] == ["tag"]: open(S + "/tag", "w").write(" ".join(a))
elif a[:1] == ["ls-remote"]:
    if E.get("FAKE_REMOTE_TAG"): print(E["FAKE_REMOTE_TAG"] + "\\trefs/tags/x")
else: sys.exit(99)
''')
    shim("gh", '''a = sys.argv[1:]
E = os.environ
if a[:2] == ["auth", "status"]: sys.exit(0 if E.get("FAKE_GH_AUTH") else 1)
elif a[:2] == ["release", "view"]:
    if E.get("FAKE_RELEASE_EXISTS"): print(E.get("FAKE_RELEASE_ASSETS", "")); sys.exit(0)
    sys.exit(1)
''')
    shim("curl", '''a = sys.argv[1:]
E = os.environ
url = a[-1]; method = a[a.index("-X") + 1]; out = a[a.index("-o") + 1]
if "--data-binary" in a: open(S + "/upload-data", "a").write(a[a.index("--data-binary") + 1])
import json
code, body = "200", {}
if method == "GET": code, body = ("200", {"id": 7, "assets": [{"name": E.get("FAKE_RELEASE_ASSETS", "")}]}) if E.get("FAKE_RELEASE_EXISTS") else ("404", {})
elif "uploads.github.com" in url: code = "201"
else: code, body = "201", {"id": 7}
json.dump(body, open(out, "w")); sys.stdout.write(code)
''')

    class G:
        shims = d
        def run(self, **extra):
            if "GITHUB_TOKEN" in extra and "FAKE_GH_AUTH" not in extra:
                (d / "gh").unlink(missing_ok=True)  # token-only setups have no gh: exercise the curl path
            base = {"TASKER2_GITHUB_PUBLISH": "1", "PATH": f"{d}:{os.environ['PATH']}"}
            apk = make_apk(env.tmp / "new.apk", marker="new")
            return env.run("deploy.sh", FAKE_BUILD_SOURCE=str(apk), **{**base, **extra})
        def calls(self):
            return log.read_text().splitlines() if log.exists() else []
    return G()


def test_github_happy_path_with_gh(gh_env):
    r = gh_env.run(FAKE_GH_AUTH="1")
    assert r.returncode == 0, r.stderr
    v = _version()
    calls = gh_env.calls()
    assert "git push origin main" in calls
    assert f"git push origin refs/tags/v{v}" in calls
    create = [c for c in calls if c.startswith("gh release create")]
    assert len(create) == 1 and f"v{v}" in create[0] and f"tasker2-v{v}.apk" in create[0] and "--notes-file" in create[0]
    assert not any("curl" in c for c in calls)


def test_github_happy_path_with_curl_token(gh_env):
    r = gh_env.run(GITHUB_TOKEN="tok-" + "x" * 8)
    assert r.returncode == 0, r.stderr
    calls = gh_env.calls()
    assert sum(c.startswith("curl") and "uploads.github.com" in c and f"name=tasker2-v{_version()}.apk" in c for c in calls) == 1
    assert any(c.startswith("curl") and "-X POST" in c and c.endswith("/releases") for c in calls)
    assert not any("tok-" in c for c in calls) and "tok-" not in r.stdout + r.stderr


def test_github_existing_release_is_not_duplicated(gh_env):
    v = _version()
    r = gh_env.run(FAKE_GH_AUTH="1", FAKE_RELEASE_EXISTS="1", FAKE_RELEASE_ASSETS=f"tasker2-v{v}.apk")
    assert r.returncode == 0
    assert not any(c.startswith(("gh release create", "gh release upload")) for c in gh_env.calls())
    r = gh_env.run(FAKE_GH_AUTH="1", FAKE_RELEASE_EXISTS="1")
    assert any(c.startswith("gh release upload") for c in gh_env.calls())
    assert not any(c.startswith("gh release create") for c in gh_env.calls())
    r = gh_env.run(GITHUB_TOKEN="t" * 8, FAKE_RELEASE_EXISTS="1", FAKE_RELEASE_ASSETS=f"tasker2-v{v}.apk")
    assert not any("-X POST" in c for c in gh_env.calls() if c.startswith("curl"))


def test_github_without_credentials_warns_and_exits_zero(gh_env):
    r = gh_env.run()
    assert r.returncode == 0
    assert "deploy: github: skipped:" in r.stderr
    assert not any(c.startswith(("gh release", "curl")) for c in gh_env.calls())


def test_github_push_rejected_is_nonfatal_and_apk_stays_published(gh_env, env):
    r = gh_env.run(FAKE_PUSH_FAIL="1", GITHUB_TOKEN="t" * 8)
    assert r.returncode == 0 and "deploy: github: failed:" in r.stderr
    assert marker(env.target) == "new"
    assert not any(c.startswith(("curl", "gh")) for c in gh_env.calls())


def test_github_existing_remote_tag_elsewhere_is_not_moved(gh_env):
    r = gh_env.run(FAKE_REMOTE_TAG="b" * 40, FAKE_GH_AUTH="1")
    assert r.returncode == 0 and "not moving it" in r.stderr
    assert not any(c.startswith(("gh release", "git push origin refs/tags")) for c in gh_env.calls())


def test_github_disabled_and_never_forces(gh_env):
    r = gh_env.run(TASKER2_GITHUB_PUBLISH="0")
    assert r.returncode == 0 and gh_env.calls() == []
    gh_env.run(FAKE_GH_AUTH="1")
    for c in gh_env.calls():
        if c.startswith("git"):
            assert not any(t in c.split() for t in ("--force", "-f", "--delete", "-d", "--force-with-lease", "--mirror", "--prune"))
            assert not any(t.startswith("+") for t in c.split())
    assert "--force" not in (SCRIPTS / "deploy" / "github.sh").read_text().replace("never force", "")
