#!/usr/bin/env bash
# what `./gradlew publish` would upload, checked before it does: publish to a throwaway local
# repository first, then compare the artifact ids with the list below. a new library fails here
# until someone adds it on purpose; a sample or the integration tests leaking in fails here too.
# usage: check-maven-publications.sh <local-repo-dir>   (the dir given to -Dmaven.repo.local)
set -euo pipefail

repo="${1:?usage: check-maven-publications.sh <local-repo-dir>}"

expected="chawpi-agent
chawpi-automation
chawpi-bom
chawpi-core
chawpi-documents
chawpi-forms
chawpi-gis
chawpi-pages
chawpi-spring-boot-starter
chawpi-spring-boot-starter-agent
chawpi-spring-boot-starter-automation
chawpi-spring-boot-starter-documents
chawpi-spring-boot-starter-forms
chawpi-spring-boot-starter-gis
chawpi-spring-boot-starter-pages
chawpi-spring-boot-starter-views
chawpi-spring-boot-starter-workflow
chawpi-test
chawpi-views
chawpi-workflow"

if [ ! -d "$repo/chawpi" ]; then
  echo "maven publications: nothing under $repo/chawpi" >&2
  exit 1
fi

actual="$(ls "$repo/chawpi" | LC_ALL=C sort)"
if [ "$actual" != "$(printf '%s\n' "$expected" | LC_ALL=C sort)" ]; then
  echo "maven publications differ from the expected set (< expected, > actual):" >&2
  diff <(printf '%s\n' "$expected" | LC_ALL=C sort) <(printf '%s\n' "$actual") >&2 || true
  exit 1
fi
echo "maven publications: ok ($(printf '%s\n' "$actual" | wc -l | tr -d ' '))"
