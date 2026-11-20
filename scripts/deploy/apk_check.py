"""Exit 0 only if the given file is a tasker2 APK: a readable zip with a manifest naming dev.tasker2 and dex code."""
import sys
import zipfile

PACKAGE = "dev.tasker2"


def problem(path):
    try:
        with zipfile.ZipFile(path) as apk:
            names = set(apk.namelist())
            if "AndroidManifest.xml" not in names:
                return "no AndroidManifest.xml"
            if not any(n.startswith("classes") and n.endswith(".dex") for n in names):
                return "no classes*.dex"
            bad = apk.testzip()
            if bad:
                return f"corrupt entry {bad}"
            manifest = apk.read("AndroidManifest.xml")
    except FileNotFoundError:
        return "missing"
    except (zipfile.BadZipFile, OSError) as exc:
        return f"not a valid APK ({exc})"
    if PACKAGE.encode("utf-16-le") not in manifest and PACKAGE.encode() not in manifest:
        return f"manifest does not name package {PACKAGE}"
    return None


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: apk_check.py <apk>")
    reason = problem(sys.argv[1])
    if reason:
        sys.exit(f"{sys.argv[1]}: {reason}")
    print(f"{sys.argv[1]}: ok")
