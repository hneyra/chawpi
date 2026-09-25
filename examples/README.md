# Examples

Each sample is a runnable app made of a `server/` (Spring Boot, built only from the chawpi starters
and `chawpi-bom`) and a `web/` (Vite + React, built only from the `@chawpi/*` packages it needs).
Samples are never published.

| Sample | Modules | Database (compose) | Ports: server, web |
|---|---|---|---|
| [`simple-sample`](simple-sample/README.md) | core | plain PostgreSQL, `--profile core` (5433), `chawpi` | 8091, 5171 |
| [`documents-sample`](documents-sample/README.md) | core, documents, automation | PostGIS (`postgres`), `chawpi_documents` | 8092, 5172 |
| [`gis-sample`](gis-sample/README.md) | core, gis (+ `perene/` model) | PostGIS, `chawpi_gis`; GeoServer optional | 8090, 5173 |
| [`full-sample`](full-sample/README.md) | every module | PostGIS, `chawpi_full`; GeoServer optional | 8093, 5174 |

Every sample:

```bash
yarn install && yarn build                      # once: builds the @chawpi/* packages the webs import
./gradlew :<sample>-server:bootRun              # CHAWPI_DB_HOST/PORT/NAME/USERNAME/PASSWORD pick the database
yarn workspace <sample>-web dev                 # proxies /api to the server
```

`CHAWPI_DB_PORT` has no default and is required for the documents, gis and full samples; only
simple-sample defaults it (to compose's plain PostgreSQL, 5433).

Port 5432 is never used on its own: no sample defaults to it and no test connects to it, so a PostgreSQL
already running there (another app's) is never touched by accident. compose's `postgres` service is
published on 5432, so pointing a sample at it is always an explicit `CHAWPI_DB_PORT=5432`.

Give each sample its own database: they install different modules, and one sample cannot read the field
types another left in a shared database. The PostGIS samples default to `chawpi_documents`, `chawpi_gis`
and `chawpi_full`; with compose, create them once:

```bash
for db in chawpi_documents chawpi_gis chawpi_full; do docker exec chawpi-postgres createdb -U chawpi "$db"; done
```

Log in as `admin@chawpi.local` / `admin` (the dev seed, `chawpi.seed.dev`, on by default in the samples).

Tests: `./gradlew :<sample>-server:integrationTest` (Testcontainers, or an external database through
`CHAWPI_TEST_DB_*`) and `yarn workspace <sample>-web test`.
