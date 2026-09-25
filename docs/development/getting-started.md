# Development

## Requirements

Java 25, Node 26, Yarn 1, Docker. The Gradle wrapper pins Gradle 9.7.1.

## Layout

- `backend/` — `build-logic` (convention plugins), `chawpi-*` (core and module libraries), `starters/`
  (`chawpi-spring-boot-starter-*`, one per module plus the base starter), `chawpi-bom`, `chawpi-test`
  (shared test support), `chawpi-integration-tests` (the original app's API tests, run against assembled
  test apps).
- `frontend/` — `packages/` (`@chawpi/*`, one per module plus `core`, `ui`, `testing`), `tooling/`
  (release and scaffolding scripts).
- `examples/` — runnable sample apps (`server/` + `web/`) that assemble the libraries into a real app.
- `infra/` — `docker/compose.yml` for local Postgres, PostGIS and GeoServer.
- `docs/` — architecture, ADRs, module guides, this guide.

Chawpi is libraries, not an app: `chawpi-core` and the module starters ship no `main` class of their
own. See [../architecture/overview.md](../architecture/overview.md) for how the pieces fit together.

## Run a sample app

Chawpi has no app of its own to run — start one of the samples under `examples/` instead. Bring up a
database (and GeoServer, if the sample needs it):

```bash
docker compose -f infra/docker/compose.yml up -d              # postgres (PostGIS + pgvector), core-only apps too
docker compose -f infra/docker/compose.yml --profile gis up -d # + GeoServer, for gis samples
docker compose -f infra/docker/compose.yml --profile core up -d # plain postgres on 5433, for core-only samples
```

Then run a sample's `server/` and `web/` — see [../../examples/README.md](../../examples/README.md) for
the current samples and how to start each one.

An app boots without writing any YAML: `CHAWPI_DB_HOST`, `_PORT`, `_NAME`, `_USERNAME` and `_PASSWORD`
default to `localhost` / `5432` / `chawpi` / `chawpi` / `chawpi`. `CHAWPI_JWT_SECRET` has no default —
set it yourself, at least 32 bytes — because a usable out-of-the-box signing key would be a security
hole shipped by the library (see [Configuration](#configuration)).

To assemble the starters into an app of your own, rather than run a sample, see
[../guides/build-your-app.md](../guides/build-your-app.md).

## Configuration

Every app gets these defaults from `ChawpiEnvironmentPostProcessor`, without writing any YAML:

| Variable | Default | Meaning |
|---|---|---|
| `CHAWPI_DB_HOST` | `localhost` | Postgres host |
| `CHAWPI_DB_PORT` | `5432` | Postgres port |
| `CHAWPI_DB_NAME` | `chawpi` | Database name |
| `CHAWPI_DB_USERNAME` | `chawpi` | Database user |
| `CHAWPI_DB_PASSWORD` | `chawpi` | Database password |
| `CHAWPI_JWT_SECRET` | none (required) | JWT signing key, ≥ 32 bytes |

A module adds its own properties (`chawpi.<module>.*`) with its own defaults — see the module's page
under `../modules/*.md`, for example [../modules/gis.md](../modules/gis.md).

## Local secrets

API keys live in `infra/.local/secrets.local.env`, which `.gitignore` keeps out of the repository —
nothing under `infra/.local/` has ever been committed, and nothing should be. Load them into a shell
before starting a sample server when you want the assistant to answer for real:

```bash
set -a; source infra/.local/secrets.local.env; set +a
```

Then start the sample server as [../../examples/README.md](../../examples/README.md) describes.

Without `ANTHROPIC_API_KEY` the assistant reports itself unavailable and every other part of Chawpi
works unchanged — that is deliberate, and `EmbabelGate` exists to keep it true (the agent framework
refuses to start without a model).

## Tests

```bash
./gradlew build   # ktlint + unit tests + architecture tests, no docker needed
yarn test         # vitest + testing-library across the frontend packages
yarn test:tooling # tests for the frontend/tooling scripts
```

## Integration tests

Integration tests are tagged `integration` and excluded from `build`, so a machine without a usable
docker daemon still gets a green build.

**1. Default: Testcontainers.** `./gradlew integrationTest` runs every suite. Core-only suites start a
`postgres:18` container; suites that need PostGIS start `postgis/postgis:18-3.6` instead (each suite
sets its own `chawpi.test.db.image` system property). This needs a local docker daemon. CI runs it on
GitHub-hosted runners, where this is the only mode.

**2. Remote docker daemon: external-database mode.** When `DOCKER_HOST` points at a remote daemon,
published container ports are not on `localhost`, so Testcontainers cannot reach them. Start the test
databases on that host yourself, tunnel their ports to your machine, and point the suites at the
tunnel instead of at Testcontainers:

```bash
nc -z localhost 5443 && echo plain db reachable
nc -z localhost 5442 && echo postgis db reachable
```

| Variable | Meaning |
|---|---|
| `CHAWPI_TEST_DB_HOST` | Tunnel host, e.g. `localhost`. Setting this switches every suite to external-database mode. |
| `CHAWPI_TEST_DB_PORT` | Tunnelled port of the plain Postgres database. |
| `CHAWPI_TEST_DB_NAME` | Database name. Must end in `_test` — the suite refuses to wipe anything else. |
| `CHAWPI_TEST_DB_USERNAME` | Database user. |
| `CHAWPI_TEST_DB_PASSWORD` | Database password. |
| `CHAWPI_TEST_GIS_DB_PORT` | Tunnelled port of the PostGIS database, for suites that need PostGIS. Name, user and password are shared. |

Once `CHAWPI_TEST_DB_HOST` is set, the other four `CHAWPI_TEST_DB_*` variables are required — there is
no silent default for a wipe target. Add `--rerun` when replaying a suite, because Gradle's build cache
could otherwise hand back a stale green result instead of hitting the external database again:

```bash
./gradlew :chawpi-integration-tests:coreOnly --rerun
```

**3. `it-env.sh`: your own local helper.** `backend/chawpi-integration-tests/it-env.sh` is git-ignored
— it holds machine-specific tunnel coordinates and is never committed. Create your own copy next to it
(same filename, since `.gitignore` already excludes it) with the six variables above:

```bash
#!/usr/bin/env bash
# backend/chawpi-integration-tests/it-env.sh — local only, never committed.
export CHAWPI_TEST_DB_HOST=localhost
export CHAWPI_TEST_DB_PORT=5443
export CHAWPI_TEST_DB_NAME=chawpi_test
export CHAWPI_TEST_DB_USERNAME=chawpi
export CHAWPI_TEST_DB_PASSWORD=changeme
export CHAWPI_TEST_GIS_DB_PORT=5442
```

Source it, never execute it, so the variables land in your current shell:

```bash
source backend/chawpi-integration-tests/it-env.sh
```

**4. Suite lock.** Each JVM takes a session advisory lock on the external database before its one-time
wipe and holds it until it exits, so two suites against the same database run one after the other
instead of wiping each other mid-run. The plain database and the PostGIS database have separate locks,
so a plain suite and a PostGIS suite may still run at the same time. Still, start one suite at a time
per database when running by hand — parallel Gradle invocations against the same database just queue
on the lock rather than run concurrently.

**5. Leftover tables.** A persistent external database accumulates physical tables that the wipe never
sees mid-run — the wipe runs once per JVM, at the first suite that touches that database. If a run is
killed mid-wipe, the old manual recipe still applies:

```bash
psql "postgresql://$CHAWPI_TEST_DB_USERNAME:$CHAWPI_TEST_DB_PASSWORD@$CHAWPI_TEST_DB_HOST:$CHAWPI_TEST_DB_PORT/postgres" \
  -c "DROP DATABASE IF EXISTS $CHAWPI_TEST_DB_NAME;" -c "CREATE DATABASE $CHAWPI_TEST_DB_NAME OWNER $CHAWPI_TEST_DB_USERNAME;"
```

**6. Commands.**

```bash
./gradlew integrationTest                              # every suite, Testcontainers or external db
./gradlew :chawpi-integration-tests:coreOnly            # one module-slice suite, core only
./gradlew :chawpi-integration-tests:gisOnly             # one module-slice suite, needs PostGIS
./gradlew :chawpi-integration-tests:fullApp             # every starter assembled together
```

The full suite list: slices `coreOnly`, `viewsOnly`, `formsOnly`, `pagesOnly`, `workflowOnly`,
`automationOnly`, `documentsOnly`, `gisOnly`, `agentOnly`; and full-app suites `fullApp`, `workflowIt`,
`automationIt`, `documentsIt`, `pagesIt`, `viewsFormsIt`, `layersIt`, `agentIt`, `coreParityIt`,
`wireParityIt`, `schemaParityIt`.

## Conventions

- Code, identifiers and comments in English; comments are caveman style — short, say why.
- Formatting follows `.editorconfig` everywhere (Kotlin 4 spaces, TS/YAML/MD 2 spaces, max 160 columns),
  enforced by `./gradlew ktlintFormat` for Kotlin and `yarn format` (prettier) for the frontend.
- Commits follow Conventional Commits, enforced by a commitlint hook and in CI.
- Architectural decisions go in `docs/adr/` (see [../adr/README.md](../adr/README.md)); change history
  in `docs/HISTORY.md`.

## Adding a field type

1. A `FieldTypeHandler` bean, declared in the module's own auto-configuration: the column type,
   validation, and the select/bind SQL, plus an optional payload section. Core never adds a `SELECT`
   alias for a rewritten column — a handler that rewrites the select (gis's `ST_AsGeoJSON`) appends its
   own `AS <quoted read name>`.
2. Frontend: a `fieldRenderers` entry in the module's `ChawpiModule` — input, display and settings for
   the type.
3. Tests: the handler's own unit test, plus an integration test that exercises it through the API.

No object-specific code anywhere. See [ADR-025](../adr/0025-extension-spis.md) for the SPI and
[ADR-028](../adr/0028-frontend-module-registry.md) for the frontend registry.

## Releasing

See [releasing.md](releasing.md) for the release flow and one-time repository setup.
