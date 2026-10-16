#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/create-release.sh --overview TEXT [--next-bump patch|minor|major] [--draft] [--dry-run]
       scripts/create-release.sh --overview-file PATH [--next-bump patch|minor|major] [--draft] [--dry-run]

Create a SemVer GitHub release from the current X.Y.Z-SNAPSHOT version in
build.sbt, attach the assembled application JAR, then commit the next snapshot.

Options:
  --overview TEXT       Human-written overview for the release notes
  --overview-file PATH  Read the overview from a text file
  --next-bump LEVEL     Version increment for next development cycle (default: patch)
  --draft               Create the release as a draft
  --dry-run             Print the release details without contacting GitHub
  -h, --help            Show this help
EOF
}

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

overview=''
overview_file=''
draft=false
dry_run=false
next_bump=patch

while (($#)); do
  case "$1" in
    --overview)
      (($# >= 2)) || fail '--overview requires a value'
      overview=$2
      shift 2
      ;;
    --overview-file)
      (($# >= 2)) || fail '--overview-file requires a path'
      overview_file=$2
      shift 2
      ;;
    --next-bump)
      (($# >= 2)) || fail '--next-bump requires patch, minor, or major'
      next_bump=$2
      shift 2
      ;;
    --draft)
      draft=true
      shift
      ;;
    --dry-run)
      dry_run=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

[[ "$next_bump" == patch || "$next_bump" == minor || "$next_bump" == major ]] ||
  fail '--next-bump must be patch, minor, or major'
[[ -n "$overview" || -n "$overview_file" ]] ||
  fail 'provide a release overview with --overview or --overview-file'
[[ -z "$overview" || -z "$overview_file" ]] ||
  fail 'use only one of --overview and --overview-file'

if [[ -n "$overview_file" && "$overview_file" != /* ]]; then
  overview_file="$PWD/$overview_file"
fi

repo_root=$(git rev-parse --show-toplevel) || fail 'run this script inside the Git repository'
cd "$repo_root"

if [[ -n "$overview_file" ]]; then
  [[ -f "$overview_file" && -r "$overview_file" ]] ||
    fail "cannot read overview file: $overview_file"
  overview=$(cat "$overview_file")
fi
[[ -n "${overview//[[:space:]]/}" ]] || fail 'the release overview cannot be empty'

versions=$(sed -nE \
  's/^[[:space:]]*ThisBuild[[:space:]]*\/[[:space:]]*version[[:space:]]*:=[[:space:]]*"([^"]+)".*$/\1/p' \
  "$repo_root/build.sbt")
scala_versions=$(sed -nE \
  's/^[[:space:]]*ThisBuild[[:space:]]*\/[[:space:]]*scalaVersion[[:space:]]*:=[[:space:]]*"([^"]+)".*$/\1/p' \
  "$repo_root/build.sbt")
[[ $(printf '%s\n' "$versions" | sed '/^$/d' | wc -l) -eq 1 ]] ||
  fail 'expected exactly one literal ThisBuild / version setting in build.sbt'
[[ $(printf '%s\n' "$scala_versions" | sed '/^$/d' | wc -l) -eq 1 ]] ||
  fail 'expected exactly one literal ThisBuild / scalaVersion setting in build.sbt'
scala_version=$scala_versions
jar_file="$repo_root/target/scala-$scala_version/linux/ITSAHardwareLogger.jar"
if [[ "$versions" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)-SNAPSHOT$ ]]; then
  release_started=false
elif [[ "$versions" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  release_started=true
else
  fail "build.sbt version must be SemVer in X.Y.Z-SNAPSHOT form, found: $versions"
fi
major=${BASH_REMATCH[1]}
minor=${BASH_REMATCH[2]}
patch=${BASH_REMATCH[3]}
version="$major.$minor.$patch"
next_major=$major
next_minor=$minor
next_patch=$patch
case "$next_bump" in
  major)
    next_major=$((major + 1))
    next_minor=0
    next_patch=0
    ;;
  minor)
    next_minor=$((minor + 1))
    next_patch=0
    ;;
  patch)
    next_patch=$((patch + 1))
    ;;
esac
next_version="$next_major.$next_minor.$next_patch-SNAPSHOT"
tag="v$version"

git check-ref-format "refs/tags/$tag" >/dev/null ||
  fail "the build version does not produce a valid Git tag: $tag"
if [[ "$release_started" == true ]]; then
  git rev-parse -q --verify "refs/tags/$tag" >/dev/null ||
    fail "version is already stable but its recovery tag is missing: $tag"
  [[ "$(git rev-parse HEAD)" == "$(git rev-parse "refs/tags/$tag^{commit}")" ]] ||
    fail "stable version recovery requires HEAD to be the commit tagged $tag"
  release_commit=$(git rev-parse HEAD)
  previous_tag=$(git describe --tags --abbrev=0 "$tag^" 2>/dev/null || true)
else
  if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
    fail "tag already exists locally: $tag"
  fi
  previous_tag=$(git describe --tags --abbrev=0 HEAD 2>/dev/null || true)
fi

if [[ -n "$previous_tag" ]]; then
  changelog=$(git log --no-merges --format='%h%x09%s' "refs/tags/$previous_tag..HEAD" |
    awk 'BEGIN { FS = "\t" } { sub(/\\n.*/, "", $2); printf "- %s (%s)\n", $2, $1 }')
else
  changelog=$(git log --no-merges --format='%h%x09%s' HEAD |
    awk 'BEGIN { FS = "\t" } { sub(/\\n.*/, "", $2); printf "- %s (%s)\n", $2, $1 }')
fi
[[ -n "$changelog" ]] || fail 'no commits found for the release changelog'

title="ITSA Hardware Logger $tag"
notes=$(mktemp)
trap 'rm -f "$notes"' EXIT
{
  printf '## Overview\n\n%s\n\n' "$overview"
  printf '## Changelog\n\n%s\n' "$changelog"
} > "$notes"

if [[ -n "$(git status --porcelain)" ]]; then
  printf 'Warning: the worktree has uncommitted changes; the release will use committed HEAD only.\n' >&2
fi

if [[ "$dry_run" == true ]]; then
  printf 'Tag: %s\nTitle: %s\n' "$tag" "$title"
  printf 'Release version: %s\nNext development version: %s\n' "$version" "$next_version"
  if [[ -n "$previous_tag" ]]; then
    printf 'Changelog range: %s..HEAD\n' "$previous_tag"
  else
    printf 'Changelog range: all commits\n'
  fi
  printf '\n%s' "$(cat "$notes")"
  exit 0
fi

command -v gh >/dev/null 2>&1 || fail 'GitHub CLI (gh) is required to create a release'
command -v sbt >/dev/null 2>&1 || fail 'sbt is required to build the release JAR'
gh auth status >/dev/null || fail 'authenticate with GitHub CLI first (gh auth login)'
[[ -z "$(git status --porcelain)" ]] ||
  fail 'commit or stash all worktree changes before creating a release'
branch=$(git symbolic-ref --quiet --short HEAD) ||
  fail 'create releases from a branch, not a detached HEAD'
git remote get-url origin >/dev/null 2>&1 ||
  fail 'configure an origin remote before creating a release'
if ! git var GIT_COMMITTER_IDENT >/dev/null 2>&1; then
  fail 'configure your Git commit identity before creating a release'
fi

if [[ "$release_started" == false ]]; then
  sed -i -E \
    "s|^([[:space:]]*ThisBuild[[:space:]]*/[[:space:]]*version[[:space:]]*:=[[:space:]]*\")[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT(\".*)$|\1$version\2|" \
    "$repo_root/build.sbt"
  git add build.sbt
  git commit -m "Release $tag"
  git tag -a "$tag" -m "Release $tag"
  release_commit=$(git rev-parse HEAD)
fi

sbt assembly
[[ -s "$jar_file" ]] || fail "assembly completed without producing the release JAR: $jar_file"

git push origin "refs/tags/$tag"

if ! gh release view "$tag" >/dev/null 2>&1; then
  gh_args=(release create "$tag" "$jar_file" --title "$title" --notes-file "$notes")
  if [[ "$draft" == true ]]; then
    gh_args+=(--draft)
  fi
  gh "${gh_args[@]}"
fi

sed -i -E \
  "s|^([[:space:]]*ThisBuild[[:space:]]*/[[:space:]]*version[[:space:]]*:=[[:space:]]*\")[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?(\".*)$|\1$next_version\3|" \
  "$repo_root/build.sbt"
git add build.sbt
git commit -m "Start $next_version development"
git push origin "$branch" "refs/tags/$tag"
