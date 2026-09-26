# documents-sample

chawpi **core + documents + automation**: document templates per object, issuing numbered documents
from a record (each issue is written to the record's history), a printable sheet, and automations that
issue documents on their own (`GENERATE_DOCUMENT`). documents and automation meet only through
automation's optional `DocumentIssuer` port: drop either starter and the other still works.

| | |
|---|---|
| Server | `server/`, project `:documents-sample-server`, port 8092. Dependencies: `chawpi-bom`, `chawpi-spring-boot-starter`, `-documents`, `-automation` |
| Web | `web/`, workspace `documents-sample-web`, port 5172. Dependencies: `@hneyra/core`, `@hneyra/ui`, `@hneyra/documents`, `@hneyra/automation` |
| Login | `admin@chawpi.local` / `admin` (dev seed, `CHAWPI_SEED_DEV=true` by default here) |

## Run

From the repository root:

```bash
# 1. a database: compose's postgres service (PostGIS image; plain PostgreSQL 18 works too), and this
#    sample's own database in it (once)
docker compose -f infra/docker/compose.yml up -d postgres
docker exec chawpi-postgres createdb -U chawpi chawpi_documents

# 2. the server. CHAWPI_DB_PORT has no default: compose publishes postgres on 5432, so say it
CHAWPI_DB_PORT=5432 ./gradlew :documents-sample-server:bootRun

# 3. the web (the first time: yarn install && yarn build, to build the @hneyra packages)
yarn workspace documents-sample-web dev       # http://localhost:5172, /api proxied to :8092
```

| Variable | Default | |
|---|---|---|
| `CHAWPI_DB_HOST` / `CHAWPI_DB_PORT` / `CHAWPI_DB_NAME` | `localhost` / **required** / `chawpi_documents` | the database |
| `CHAWPI_DB_USERNAME` / `CHAWPI_DB_PASSWORD` | `chawpi` / `chawpi` | |
| `CHAWPI_SEED_DEV` | `true` | seeds the admin user; turn off outside a demo |
| `CHAWPI_JWT_SECRET` | a sample-only value | set your own (>= 32 bytes) anywhere real |
| `CHAWPI_AUTOMATION_POLL` | `1s` | automation queue drain; `0s` turns it off |
| `CHAWPI_API_URL` (web) | `http://localhost:8092` | where the dev server proxies `/api` |

Through an ssh tunnel: `CHAWPI_DB_PORT=5442 ./gradlew :documents-sample-server:bootRun`.

One database per sample: gis-sample and full-sample leave `GEOMETRY` fields and other modules' columns in
theirs, and documents-sample cannot read those records ("Field type 'GEOMETRY' is not installed").

Port 5432 is never used on its own: no sample defaults to it and no test connects to it, so a PostgreSQL
already running there (another app's) is never touched by accident. compose's `postgres` service is
published on 5432, so pointing a sample at it is always an explicit `CHAWPI_DB_PORT=5432`.

## Try it

The UI starts in Spanish; the labels below are the Spanish ones (English in brackets).

1. Datos → Objetos (Data → Objects) → new object `predio` with a TEXT field `codigo`; create a record.
2. App Builder → Documentos (Documents) → new type `oficio` for `predio`, prefix `OF`, a template using `codigo`.
3. On the record: Documentos emitidos → choose `Oficio` → Emitir (Issue). The number is `OF-<year>-001`, the
   history shows the issue, and the print view opens the sheet.
4. Automatización → Reglas (Automation → Rules) → on `predio`, trigger "record created", action "generate document" `oficio`.
   Create another record: its document appears within a second.

## Tests

```bash
./gradlew :documents-sample-server:integrationTest   # Testcontainers postgres:18, or CHAWPI_TEST_DB_* for an external database
yarn workspace documents-sample-web test             # mounts App against a fetch mock
```
