# Post-publish GitHub step for deploy.sh. Source it; do not run it.
# Pushes main (fast-forward only), tags v<version>, and creates/updates a GitHub Release with the APK.
# Every failure is a warning, never an error: the APK deploy is already done and must not be rolled back.
# Disable with TASKER2_GITHUB_PUBLISH=0. Never force-pushes, never deletes refs or releases, never prints the token.

TASKER2_GITHUB_BUDGET="${TASKER2_GITHUB_BUDGET:-40}"   # total seconds for the whole GitHub step
TASKER2_GITHUB_STEP_MAX="${TASKER2_GITHUB_STEP_MAX:-15}" # max seconds for one network command

gh_warn() { echo "deploy: github: $*" >&2; }

# gh_t <cmd...>: run a command bounded by the step max and the remaining total budget.
gh_t() {
    local left=$(( GH_DEADLINE - $(date +%s) ))
    [ "$left" -gt 0 ] || { gh_warn "failed: time budget of ${TASKER2_GITHUB_BUDGET}s exhausted"; return 124; }
    [ "$left" -le "$TASKER2_GITHUB_STEP_MAX" ] || left="$TASKER2_GITHUB_STEP_MAX"
    timeout -k 2 "$left" "$@"
}

# gh_notes <version> <outfile>: the version's entry from CHANGELOG.md ("- 1.3: ..." bullet or a "## 1.3" section).
gh_notes() {
    local f="$TASKER2_REPO/CHANGELOG.md"
    [ -f "$f" ] && awk -v v="$1" '
        function isver(s) { return s ~ /^- [0-9]/ || s ~ /^#+[ \t]+\[?v?[0-9]/ }
        !on && ($0 ~ "^- v?" v "[: ]" || $0 ~ "^#+[ \t]+\\[?v?" v "([]: \t-]|$)") { on = 1; print; next }
        on && isver($0) { exit }
        on && /^# / { exit }
        on { print }' "$f" > "$2" || true
    [ -s "$2" ] || echo "Release v$1" > "$2"
}

# gh_api <METHOD> <url> <outfile> [data-file] [content-type]: curl with the token kept out of argv. Prints HTTP code.
gh_api() {
    local method="$1" url="$2" out="$3" data="${4:-}" ctype="${5:-application/json}" args=()
    [ -z "$data" ] || args=(--data-binary "@$data" -H "Content-Type: $ctype")
    printf 'header = "Authorization: Bearer %s"\n' "$GH_API_TOKEN" | gh_t curl -sS -K - -X "$method" \
        -H "Accept: application/vnd.github+json" "${args[@]}" -o "$out" -w '%{http_code}' "$url" 2>/dev/null
}

gh_json_get() { python3 -I -c 'import json,sys
d=json.load(open(sys.argv[1]))
for k in sys.argv[2].split("."):
    d=d.get(k) if isinstance(d,dict) else None
print("" if d is None else d)' "$1" "$2" 2>/dev/null; }

# Release via the gh CLI. Return 0 when done/skipped-as-existing.
gh_release_cli() {
    local tag="$1" apk="$2" name="$3" notes="$4" assets
    if assets="$(gh_t gh release view "$tag" --json assets --jq '.assets[].name' 2>/dev/null)"; then
        if printf '%s\n' "$assets" | grep -qx -- "$name"; then
            echo "deploy: github: release $tag already has $name; nothing to do"
        else
            gh_t gh release upload "$tag" "$apk" >/dev/null || { gh_warn "failed: could not upload $name to release $tag"; return 1; }
            echo "deploy: github: uploaded $name to existing release $tag"
        fi
        return 0
    fi
    gh_t gh release create "$tag" "$apk" --title "$tag" --notes-file "$notes" --verify-tag >/dev/null \
        || { gh_warn "failed: could not create release $tag via gh"; return 1; }
    echo "deploy: github: created release $tag with $name"
}

# Release via the REST API and curl.
gh_release_curl() {
    local tag="$1" apk="$2" name="$3" notes="$4" tmp="$5" slug code rid has
    slug="$(git remote get-url origin 2>/dev/null | sed -E 's#^(git@[^:]+:|https?://[^/]+/|ssh://[^/]+/)##; s#\.git$##')"
    case "$slug" in */*) ;; *) gh_warn "failed: cannot derive owner/repo from remote origin"; return 1 ;; esac
    local api="https://api.github.com/repos/$slug"
    code="$(gh_api GET "$api/releases/tags/$tag" "$tmp/rel.json")" || { gh_warn "failed: GitHub API unreachable"; return 1; }
    if [ "$code" = "200" ]; then
        rid="$(gh_json_get "$tmp/rel.json" id)"
        has="$(python3 -I -c 'import json,sys
print(any(a.get("name")==sys.argv[2] for a in json.load(open(sys.argv[1])).get("assets",[])))' "$tmp/rel.json" "$name" 2>/dev/null)"
        if [ "$has" = "True" ]; then
            echo "deploy: github: release $tag already has $name; nothing to do"; return 0
        fi
    elif [ "$code" = "404" ]; then
        python3 -I -c 'import json,sys
json.dump({"tag_name":sys.argv[1],"name":sys.argv[1],"body":open(sys.argv[2]).read()},open(sys.argv[3],"w"))' "$tag" "$notes" "$tmp/new.json"
        code="$(gh_api POST "$api/releases" "$tmp/rel.json" "$tmp/new.json")" || code=""
        case "$code" in 201) rid="$(gh_json_get "$tmp/rel.json" id)" ;;
            *) gh_warn "failed: creating release $tag returned HTTP ${code:-?}"; return 1 ;; esac
    else
        gh_warn "failed: looking up release $tag returned HTTP ${code:-?} (check token scope)"; return 1
    fi
    [ -n "$rid" ] || { gh_warn "failed: no release id in API response"; return 1; }
    code="$(gh_api POST "https://uploads.github.com/repos/$slug/releases/$rid/assets?name=$name" "$tmp/asset.json" "$apk" application/vnd.android.package-archive)" || code=""
    [ "$code" = "201" ] || { gh_warn "failed: uploading $name returned HTTP ${code:-?}"; return 1; }
    echo "deploy: github: release $tag has $name"
}

# github_publish <apk> <version>: always returns 0.
github_publish() {
    local apk="$1" version="$2" tag="v$2" name="tasker2-v$2.apk" head tmp mode="" rsha lsha
    if [ "${TASKER2_GITHUB_PUBLISH:-1}" = "0" ]; then
        echo "deploy: github: skipped: disabled by TASKER2_GITHUB_PUBLISH=0"; return 0
    fi
    command -v git >/dev/null 2>&1 && command -v timeout >/dev/null 2>&1 || { gh_warn "skipped: git or timeout missing"; return 0; }
    GH_DEADLINE=$(( $(date +%s) + TASKER2_GITHUB_BUDGET ))
    export GIT_TERMINAL_PROMPT=0
    export GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-ssh -o BatchMode=yes -o ConnectTimeout=10}"
    git remote get-url origin >/dev/null 2>&1 || { gh_warn "skipped: no git remote 'origin'"; return 0; }

    # 1. plain fast-forward push of main (a rejected push is reported, never forced)
    gh_t git push origin main >/dev/null 2>&1 || { gh_warn "failed: git push origin main did not succeed (no access, network error or non-fast-forward); not forcing"; return 0; }
    echo "deploy: github: pushed main"

    # 2. tag v<version> on HEAD; never move an existing tag
    head="$(git rev-parse HEAD 2>/dev/null)" || { gh_warn "failed: cannot resolve HEAD"; return 0; }
    lsha="$(git rev-parse -q --verify "refs/tags/$tag^{commit}" 2>/dev/null || true)"
    if [ -z "$lsha" ]; then
        git tag "$tag" HEAD 2>/dev/null || { gh_warn "failed: cannot create tag $tag"; return 0; }
        lsha="$head"
    fi
    rsha="$(gh_t git ls-remote origin "refs/tags/$tag" "refs/tags/$tag^{}" 2>/dev/null | awk 'END{print $1}')" || rsha=""
    if [ -n "$rsha" ] && [ "$rsha" != "$head" ]; then
        gh_warn "warning: tag $tag already exists on origin at ${rsha:0:7}, not HEAD ${head:0:7}; not moving it, skipping release (bump versionName)"; return 0
    fi
    if [ -z "$rsha" ]; then
        if [ "$lsha" != "$head" ]; then
            gh_warn "warning: local tag $tag points at ${lsha:0:7}, not HEAD ${head:0:7}; not moving it, skipping release (bump versionName)"; return 0
        fi
        gh_t git push origin "refs/tags/$tag" >/dev/null 2>&1 || { gh_warn "failed: could not push tag $tag"; return 0; }
        echo "deploy: github: pushed tag $tag"
    fi

    # 3. release with the APK; prefer gh, else curl with GITHUB_TOKEN / GH_TOKEN
    GH_API_TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
    if command -v gh >/dev/null 2>&1 && { [ -n "$GH_API_TOKEN" ] || gh_t gh auth status >/dev/null 2>&1; }; then
        mode=cli
    elif [ -n "$GH_API_TOKEN" ] && command -v curl >/dev/null 2>&1; then
        mode=curl
    else
        gh_warn "skipped: no release credentials (install and log in gh, or set GITHUB_TOKEN); code and tag were pushed, release not created"; return 0
    fi
    tmp="$(mktemp -d)" || { gh_warn "failed: no temp dir"; return 0; }
    cp -- "$apk" "$tmp/$name" && gh_notes "$version" "$tmp/notes.md"
    if [ "$mode" = cli ]; then gh_release_cli "$tag" "$tmp/$name" "$name" "$tmp/notes.md" || true
    else gh_release_curl "$tag" "$tmp/$name" "$name" "$tmp/notes.md" "$tmp" || true; fi
    rm -rf -- "$tmp"
    return 0
}
