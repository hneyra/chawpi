# gis-sample

chawpi **core + gis** on **PostGIS**: `GEOMETRY` fields (points, lines, polygons, any SRID), the map on
a record, the map page, layers and GeoJSON features. GeoServer (WMS publishing) is optional and off by
default. `perene/` is a real cadastre model (13 objects, 11 spatial, EPSG:32718) to load into it.

| | |
|---|---|
| Server | `server/`, project `:gis-sample-server`, port 8090. Dependencies: `chawpi-bom`, `chawpi-spring-boot-starter`, `-gis` |
| Web | `web/`, workspace `gis-sample-web`, port 5173. Dependencies: `@chawpi/core`, `@chawpi/ui`, `@chawpi/gis`, `maplibre-gl` 6.10.0 (as `@chawpi/gis`) |
| Login | `admin@chawpi.local` / `admin` (dev seed, `CHAWPI_SEED_DEV=true` by default here) |

## Run

From the repository root:

```bash
# 1. PostGIS: compose's postgres service (add --profile gis for GeoServer, optional), and this
#    sample's own database in it (once)
docker compose -f infra/docker/compose.yml up -d postgres
docker exec chawpi-postgres createdb -U chawpi chawpi_gis

# 2. the server. CHAWPI_DB_PORT has no default: compose publishes postgres on 5432, so say it
CHAWPI_DB_PORT=5432 ./gradlew :gis-sample-server:bootRun

# 3. the web (the first time: yarn install && yarn build, to build the @chawpi packages)
yarn workspace gis-sample-web dev             # http://localhost:5173, /api proxied to :8090
```

| Variable | Default | |
|---|---|---|
| `CHAWPI_DB_HOST` / `CHAWPI_DB_PORT` / `CHAWPI_DB_NAME` | `localhost` / **required** / `chawpi_gis` | a PostGIS database |
| `CHAWPI_DB_USERNAME` / `CHAWPI_DB_PASSWORD` | `chawpi` / `chawpi` | |
| `CHAWPI_SEED_DEV` | `true` | seeds the admin user; turn off outside a demo |
| `CHAWPI_JWT_SECRET` | a sample-only value | set your own (>= 32 bytes) anywhere real |
| `CHAWPI_GEOSERVER_ENABLED` | `false` | publish layers to GeoServer |
| `CHAWPI_GEOSERVER_URL` | `http://localhost:8081/geoserver` | compose's `geoserver` (profile `gis`) |
| `CHAWPI_API_URL` (web) | `http://localhost:8090` | where the dev server proxies `/api` |

Through an ssh tunnel: `CHAWPI_DB_PORT=5442 ./gradlew :gis-sample-server:bootRun`.

One database per sample: the samples install different modules, and one sample cannot read the field types
another left in a shared database.

Port 5432 is never used on its own: no sample defaults to it and no test connects to it, so a PostgreSQL
already running there (another app's) is never touched by accident. compose's `postgres` service is
published on 5432, so pointing a sample at it is always an explicit `CHAWPI_DB_PORT=5432`.

## GeoServer (optional)

```bash
docker compose -f infra/docker/compose.yml --profile gis up -d
CHAWPI_GEOSERVER_ENABLED=true CHAWPI_GIS_GEOSERVER_DATASTORE_DATABASE=chawpi_gis CHAWPI_DB_PORT=5432 ./gradlew :gis-sample-server:bootRun
```

Without it, everything but WMS publishing works: features are served as GeoJSON by the server itself.

GeoServer's own datastore does not follow `CHAWPI_DB_*`: it always points at compose's `postgres`
service (host `postgres`, port 5432, database `chawpi`, `GeoServerDataStoreProperties` in
`chawpi-gis`), because it runs in another container on the compose network. If the server connects
elsewhere (an ssh tunnel, or any database but `chawpi`, this sample's default `chawpi_gis` included),
GeoServer reads a different, empty database and the published layers show no features. Point it at the
same database instead, as the command above does for `chawpi_gis`: set `chawpi.gis.geoserver.datastore.host` / `.port` / `.database` (env
`CHAWPI_GIS_GEOSERVER_DATASTORE_HOST` / `_PORT` / `_DATABASE`) to match wherever the server itself is
pointed.

## Load the Perené cadastre model

With the server running on 8090 (`apply.py`'s default; `--core` or `$CHAWPI_CORE` for another url):

```bash
cd examples/gis-sample/perene
python3 apply.py --validate-only   # checks model.json, calls nothing
python3 apply.py                   # done: 30 created, 0 skipped  (13 objects + 17 relationships)
python3 apply.py                   # idempotent: done: 0 created, 30 skipped
python3 apply.py --drop            # removes it again, in reverse order
```

After loading, `GET /api/gis/layers` lists 11 layers (SRID 32718) and the web's map page offers them.
Details of the model: [perene/README.md](perene/README.md).

## What it demonstrates

- `server/build.gradle.kts`: the BOM, the core starter and the gis starter. The app class is one annotation.
- `web/src/main.tsx`: the MapLibre worker recipe for Vite (`?worker&url`) and `maplibre-gl.css`;
  `web/src/App.tsx`: `modules={[gisModule({ workerUrl })]}`.

## Tests

```bash
./gradlew :gis-sample-server:integrationTest   # Testcontainers postgis/postgis:18-3.6, or CHAWPI_TEST_DB_* + CHAWPI_TEST_GIS_DB_PORT
yarn workspace gis-sample-web test             # mounts App against a fetch mock
cd examples/gis-sample/perene && python3 -m unittest -v
```
