# simple-sample

chawpi **core alone**, on **plain PostgreSQL** (no PostGIS): objects, fields, records, relationships,
history, users, roles and permissions. It proves every module, GIS included, is optional.

| | |
|---|---|
| Server | `server/`, project `:simple-sample-server`, port 8091. Dependencies: `chawpi-bom` + `chawpi-spring-boot-starter` |
| Web | `web/`, workspace `simple-sample-web`, port 5171. Dependencies: `@hneyra/core`, `@hneyra/ui` |
| Login | `admin@chawpi.local` / `admin` (dev seed, `CHAWPI_SEED_DEV=true` by default here) |

## Run

From the repository root:

```bash
# 1. a database: compose's plain postgres on 5433 (or any PostgreSQL 18, see the variables below)
docker compose -f infra/docker/compose.yml --profile core up -d postgres-plain

# 2. the server
./gradlew :simple-sample-server:bootRun

# 3. the web (the first time: yarn install && yarn build, to build the @hneyra packages)
yarn workspace simple-sample-web dev          # http://localhost:5171, /api proxied to :8091
```

| Variable | Default | |
|---|---|---|
| `CHAWPI_DB_HOST` / `CHAWPI_DB_PORT` / `CHAWPI_DB_NAME` | `localhost` / `5433` / `chawpi` | the database |
| `CHAWPI_DB_USERNAME` / `CHAWPI_DB_PASSWORD` | `chawpi` / `chawpi` | |
| `CHAWPI_SEED_DEV` | `true` | seeds the admin user; turn off outside a demo |
| `CHAWPI_JWT_SECRET` | a sample-only value | set your own (>= 32 bytes) anywhere real |
| `CHAWPI_API_URL` (web) | `http://localhost:8091` | where the dev server proxies `/api` |

A remote database works the same way, e.g. through an ssh tunnel:
`CHAWPI_DB_PORT=5443 CHAWPI_DB_NAME=chawpi_simple ./gradlew :simple-sample-server:bootRun`.

## What it demonstrates

- `server/build.gradle.kts`: the BOM plus one starter, nothing else. `SimpleSampleApplication` is one
  annotation, `@ChawpiApplication`; `application.yml` is the database, the seed and the JWT secret.
- `web/src/App.tsx`: `<ChawpiApp modules={[]} />`, the whole frontend.
- Asking for a `GEOMETRY` field here is a 4xx: the type belongs to the gis module, which is absent.

## Tests

```bash
./gradlew :simple-sample-server:integrationTest   # Testcontainers postgres:18, or CHAWPI_TEST_DB_* for an external database
yarn workspace simple-sample-web test             # mounts App against a fetch mock
```
