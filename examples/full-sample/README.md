# full-sample

chawpi with **every module**: core, views, forms, pages, workflow, automation, documents, gis and the
assistant (agent), on PostGIS. The same app as the [original](../../docs/sapgis-origin.md), assembled from the libraries.

| | |
|---|---|
| Server | `server/`, project `:full-sample-server`, port 8093. Dependencies: `chawpi-bom`, `chawpi-spring-boot-starter` and the eight `-<module>` starters |
| Web | `web/`, workspace `full-sample-web`, port 5174. Dependencies: `@chawpi/core`, `@chawpi/ui` and all eight module packages, `maplibre-gl` 6.10.0 |
| Login | `admin@chawpi.local` / `admin` (dev seed, `CHAWPI_SEED_DEV=true` by default here) |

## Run

From the repository root:

```bash
# 1. PostGIS: compose's postgres service (add --profile gis for GeoServer, optional), and this
#    sample's own database in it (once)
docker compose -f infra/docker/compose.yml up -d postgres
docker exec chawpi-postgres createdb -U chawpi chawpi_full

# 2. the server. CHAWPI_DB_PORT has no default: compose publishes postgres on 5432, so say it.
#    ANTHROPIC_API_KEY switches the assistant on.
CHAWPI_DB_PORT=5432 ./gradlew :full-sample-server:bootRun

# 3. the web (the first time: yarn install && yarn build, to build the @chawpi packages)
yarn workspace full-sample-web dev            # http://localhost:5174, /api proxied to :8093
```

| Variable | Default | |
|---|---|---|
| `CHAWPI_DB_HOST` / `CHAWPI_DB_PORT` / `CHAWPI_DB_NAME` | `localhost` / **required** / `chawpi_full` | a PostGIS database |
| `CHAWPI_DB_USERNAME` / `CHAWPI_DB_PASSWORD` | `chawpi` / `chawpi` | |
| `CHAWPI_SEED_DEV` | `true` | seeds the admin user; turn off outside a demo |
| `CHAWPI_JWT_SECRET` | a sample-only value | set your own (>= 32 bytes) anywhere real |
| `CHAWPI_AUTOMATION_POLL` | `1s` | automation queue drain; `0s` turns it off |
| `CHAWPI_GEOSERVER_ENABLED` / `CHAWPI_GEOSERVER_URL` | `false` / `http://localhost:8081/geoserver` | WMS publishing (optional) |
| `ANTHROPIC_API_KEY` | – | the assistant; without it the rest works and the assistant says it is not configured |
| `CHAWPI_API_URL` (web) | `http://localhost:8093` | where the dev server proxies `/api` |

Through an ssh tunnel: `CHAWPI_DB_PORT=5442 ./gradlew :full-sample-server:bootRun`.

One database per sample: the samples install different modules, and one sample cannot read the field types
another left in a shared database.

Port 5432 is never used on its own: no sample defaults to it and no test connects to it, so a PostgreSQL
already running there (another app's) is never touched by accident. compose's `postgres` service is
published on 5432, so pointing a sample at it is always an explicit `CHAWPI_DB_PORT=5432`.

GeoServer's own datastore does not follow `CHAWPI_DB_*`: it always points at compose's `postgres`
service (host `postgres`, port 5432, database `chawpi`, `GeoServerDataStoreProperties` in
`chawpi-gis`), because it runs in another container on the compose network. If the server connects
elsewhere (the ssh tunnel above, or any database but `chawpi`, this sample's default `chawpi_full`
included), GeoServer reads a different, empty database and the published layers show no features. Point
it at the same database instead (for compose: `CHAWPI_GIS_GEOSERVER_DATASTORE_DATABASE=chawpi_full`): set
`chawpi.gis.geoserver.datastore.host` / `.port` / `.database` (env
`CHAWPI_GIS_GEOSERVER_DATASTORE_HOST` / `_PORT` / `_DATABASE`) to match wherever the server itself is
pointed.

## Smoke test (Playwright, headless)

```bash
./gradlew :full-sample-server:bootJar
yarn workspace full-sample-web exec playwright install chromium   # once: the headless browser (npx playwright install chromium)
CHAWPI_DB_PORT=5432 yarn workspace full-sample-web e2e             # CHAWPI_DB_NAME defaults to chawpi_full, as for bootRun
```

It starts the jar and the dev server (or reuses running ones), sets up an object with a `GEOMETRY`
field, a workflow and a document type through the api, then logs in, opens the record and its map,
issues a document and applies a transition through the ui.

The smoke does not clean up after itself: its `e2e…` object, workflow, document type and the records it
creates are left behind in `chawpi_full` (or whatever database `CHAWPI_DB_NAME` points at). Re-running it
against the same database is fine, but do not point it at a database you care about.

## Manual checklist (the original app's flows)

The UI starts in Spanish; the labels below are the Spanish ones (English in brackets).

1. **Login** as `admin@chawpi.local` / `admin`: the home page lists the objects.
2. **Object with a GEOMETRY field**: Datos → Objetos (Data → Objects) → new object `predio`; add a TEXT field `codigo`
   and a GEOMETRY field `lote` (Polygon, SRID 4326). Save; it appears in the list.
3. **Record + map**: open `predio` → new record, draw a polygon on the map, save. The record page
   shows the polygon; GIS → Mapas (Maps) shows the `predio` layer with it.
4. **Issue a document**: App Builder → Documentos (Documents) → new type `oficio` for `predio` (prefix `OF`, a template
   using `codigo`). On the record: Documentos emitidos → `Oficio` → Emitir (Issue) → `OF-<year>-001`; its
   print view opens; the history shows the issue.
5. **Workflow transition**: Automatización → Workflows (Automation → Workflows) → `predio`: states Borrador (initial) → Aprobado
   (final), transition Aprobar. On a record: the state badge says Borrador; Aprobar → Aprobado, and
   the history shows the transition.
6. Optional: App Builder → Páginas / Vistas / Formularios (Pages / Views / Forms) edit the record page, list columns and form;
   Automatización → Reglas (Rules) adds a rule; with `ANTHROPIC_API_KEY`, Automatización → Asistente
   (Assistant) answers.

## Tests

```bash
./gradlew :full-sample-server:integrationTest   # Testcontainers postgis/postgis:18-3.6, or CHAWPI_TEST_DB_* + CHAWPI_TEST_GIS_DB_PORT
yarn workspace full-sample-web test             # mounts App against a fetch mock
```
