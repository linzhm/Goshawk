#!/usr/bin/env bash
# Prepare reviewable upstream updates without modifying the current branch.
set -euo pipefail
mode=${1:-preview}
ref=${2:-upstream/master}
case "$mode" in preview|prepare) ;; *) echo "Usage: $0 preview|prepare [upstream/ref] [commit ...]" >&2; exit 2;; esac
case "$ref" in upstream/*) ;; *) echo "Choose an upstream/* ref." >&2; exit 2;; esac
repo=$(git rev-parse --show-toplevel)
cd "$repo"
url=$(git remote get-url upstream)
case "$url" in https://github.com/sparrowwallet/sparrow.git|git@github.com:sparrowwallet/sparrow.git) ;; *) echo "Unexpected upstream URL: $url" >&2; exit 2;; esac
git fetch --no-tags --recurse-submodules=no upstream
target=$(git rev-parse --verify "$ref^{commit}")
base=$(git merge-base HEAD "$target")
echo "Upstream changes since common ancestor $base:"
git diff --stat "$base" "$target"
echo "Upstream changes touching Goshawk resource overrides:"
while IFS= read -r path; do
    case "$path" in
        src/main/resources/*)
            overlay="branding/goshawk/resources/${path#src/main/resources/}"
            if [ -f "$overlay" ]; then printf 'REVIEW %s -> %s\n' "$path" "$overlay"; fi ;;
        src/main/deploy/*)
            printf 'REVIEW installer change: %s\n' "$path" ;;
    esac
done < <(git diff --name-only "$base" "$target")
if [ "$mode" = preview ]; then exit 0; fi
if [ -n "$(git status --porcelain)" ]; then
    echo "Commit or set aside current changes before preparing a sync worktree." >&2
    exit 2
fi
if [ $# -ge 2 ]; then shift 2; elif [ $# -eq 1 ]; then shift; fi
for commit in "$@"; do
    git rev-parse --verify "$commit^{commit}" >/dev/null
    git merge-base --is-ancestor "$commit" "$target" || { echo "Commit is not in $ref: $commit" >&2; exit 2; }
done
stamp=$(date +%Y%m%d-%H%M%S)
branch="sync/sparrow-$stamp"
worktree="$(dirname "$repo")/goshawk-upstream-$stamp"
git worktree add -b "$branch" "$worktree" HEAD
echo "Prepared $worktree on $branch"
if [ $# -eq 0 ]; then
    git -C "$worktree" merge --no-commit --no-ff "$target"
    echo "Review the pending merge, run checks, then commit in the worktree."
else
    git -C "$worktree" cherry-pick -x "$@"
    echo "Selected upstream commits imported. Review and test before integrating."
fi
echo "No changes have been pushed; the original checkout is unchanged."
