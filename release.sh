#!/usr/bin/env bash
set -e  # exit on error

# --- validate parameters ---
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <release-version> <next-snapshot-version>"
  echo "Example: $0 1.2.3 1.2.4-SNAPSHOT"
  exit 1
fi

RELEASE_VERSION=$1
NEXT_VERSION=$2

echo "Release version: $RELEASE_VERSION"
echo "Next snapshot version: $NEXT_VERSION"

# --- ensure working tree is clean ---
if ! git diff-index --quiet HEAD --; then
  echo "ERROR: Working tree is not clean. Please commit or stash changes first."
  exit 1
fi

# --- set release version ---
echo "➡️ Setting release version..."
mvn -B versions:set -DnewVersion=${RELEASE_VERSION}
mvn -B versions:commit

# --- commit ---
git add -A
git commit -m "release ${RELEASE_VERSION}"

# --- create tag (without 'v') ---
git tag -a "${RELEASE_VERSION}" -m "Release ${RELEASE_VERSION}"

# --- set next snapshot version ---
echo "➡️ Setting next snapshot version..."
mvn -B versions:set -DnewVersion=${NEXT_VERSION}
mvn -B versions:commit

# --- commit ---
git add -A
git commit -m "prepare next development iteration ${NEXT_VERSION}"

# --- push commits and tag ---
echo "➡️ Pushing changes and tag..."
git push origin HEAD
git push origin "${RELEASE_VERSION}"

echo "Done!"
