# Releasing

## Flow

1. Merge conventional commits (`feat:`, `fix:`, ...) to `main`.
2. `release-please.yml` runs on every push to `main`. It opens or updates a "release PR" with the
   bumped version and `CHANGELOG.md` entries. No release yet — just a PR.
3. Merging that PR makes release-please tag `vX.Y.Z` and publish a GitHub Release.
4. The release publishing triggers `publish.yml`, which builds and pushes every Maven and npm
   library to GitHub Packages, version = the tag without `v`.

## One-time setup: `RELEASE_PLEASE_TOKEN`

A release created with the default `GITHUB_TOKEN` does not trigger other workflows — GitHub blocks
that on purpose, to stop workflow loops. Without a real token, step 3 above happens but step 4 never
fires.

Fix: create a fine-grained PAT with **Contents: Read and write** and **Pull requests: Read and
write** on this repo, and store it as the repo secret `RELEASE_PLEASE_TOKEN`
(Settings → Secrets and variables → Actions). `release-please.yml` already falls back to
`GITHUB_TOKEN` when the secret is absent, so releases still work without it — they just won't
publish anything until the PAT is added.

## First release is pinned

The `.` package in `release-please-config.json` carries `"release-as": "0.1.0"` so the first release
lands on 0.1.0 instead of release-please's default 1.0.0 bump. Delete that key right after v0.1.0
ships.

## Versions are lockstep

One version for the whole repo (`gradle.properties`, root `package.json`, every
`frontend/packages/*/package.json`). release-please bumps them together; there is no per-library
versioning.

## Guards

Two scripts stop a release from publishing the wrong thing. CI runs both on every pull request, and `publish.yml`
runs both right before uploading:

- `node frontend/tooling/check-release.mjs [--pack <version>]` checks that exactly the eleven public `@chawpi/*`
  packages would be published, that release-please bumps each of them, and that every workspace app is private.
  With `--pack`, it copies the packages, applies `set-version.mjs`, and runs `npm pack --dry-run` on each: every
  internal range must be the release version and every entry point must be in the tarball.
- `.github/scripts/check-maven-publications.sh <dir>` compares what `./gradlew publishToMavenLocal
  -Dmaven.repo.local=<dir>` produced with the twenty expected Maven artifacts.

A new library fails both until it is added on purpose: to `EXPECTED_PUBLIC` and release-please `extra-files` for npm,
to the script's list for Maven.

## Consuming a published library

Maven (`~/.gradle/init.d/github-packages.init.gradle.kts` or per-project `settings.gradle.kts`):

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/hneyra/chawpi")
        credentials {
            username = "<github-username>"
            password = "<PAT with read:packages>"
        }
    }
}
```

npm (`.npmrc`):

```
@chawpi:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=<PAT with read:packages>
```

GitHub Packages requires auth even for public packages — an unauthenticated `read:packages` PAT is
enough to install, no `write` scope needed.
