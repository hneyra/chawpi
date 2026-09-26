# GIS module

An app that installs this module lets an object carry one or more geometry fields, drawn and edited on a map,
queried by bounding box, served as GeoJSON features, and published as WMS/WFS layers in GeoServer.

## Install

```kotlin
implementation("chawpi:chawpi-spring-boot-starter-gis")
```

Needs a PostgreSQL server with PostGIS: the image `postgis/postgis:18-3.6`, as `infra/docker/compose.yml`'s
`postgres` service builds on (`infra/docker/postgres/Dockerfile`).

```bash
yarn add @hneyra/gis
yarn add maplibre-gl@6.10.0
```

`terra-draw` and `terra-draw-maplibre-gl-adapter` are regular dependencies of `@hneyra/gis`, not peers to add
yourself — see the package README for the exact versions and why `maplibre-gl` is pinned in the app too.

```tsx
<ChawpiApp modules={[gisModule({ workerUrl })]} />
```

`workerUrl` points at MapLibre's worker script as your bundler serves it; see the package README, "MapLibre
worker", for the recipe (Vite, webpack/rspack, or anything else).

See [../guides/build-your-app.md](../guides/build-your-app.md).

## What it adds

The `GEOMETRY` field type (registered under the record payload section `geometries`, not `attributes`), the
`bbox` and `geometry` record query parameters, feature endpoints that serve an object's records as GeoJSON, GeoServer
layer publishing, and the `MAP` page component (`@Order(100)`, only when chawpi-pages is present).

REST routes:

| Method | Path |
|---|---|
| GET | `/api/gis/layers` |
| POST | `/api/gis/layers/{object}` |
| DELETE | `/api/gis/layers/{object}` |
| POST | `/api/gis/layers/{object}/{geometry}` |
| DELETE | `/api/gis/layers/{object}/{geometry}` |
| GET | `/api/gis/services` |
| GET | `/api/gis/objects/{object}/features` |
| GET | `/api/gis/objects/{object}/features/{id}` |

A `/layers/{object}` call without a `{geometry}` segment means the object's first geometry field, so links saved
before an object could carry more than one geometry still work.

Frontend routes and slots:

| Slot | Contribution |
|---|---|
| route `gis:map` | `/gis/map` (`?object=&geometry=`): pick an object, draw and browse its features |
| route `gis:layers` | `/gis/layers`: GeoServer publishing status, publish/unpublish, WMS/WFS links |
| nav | group "GIS" (order 20): Maps, Layers, Map views (placeholder, disabled) |
| `fieldRenderers.GEOMETRY` | input, settings (`geometryType`, `srid`) for a geometry field, section `geometries` |
| `pageComponents.MAP` | draws the record's shapes, optionally one targeted geometry field |
| `recordListActions` | "Map" button on spatial objects' record lists |
| `dashboardCards`, `objectTileDetails`, `objectColumns` | spatial object count, `TYPE · EPSG:n` lines, an objects-table column |
| `auditValueFormatters`, `auditFieldLabels` | a shape change reads "geometry updated", never coordinates |
| `recordQueryKeys` | `['features', object]` goes stale on every write to that object's records |

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `chawpi.gis.enabled` | `true` | `false` removes the `GEOMETRY` type, `bbox`/`geometry` query parameters, gis routes and the postgis migration |
| `chawpi.gis.geoserver.enabled` | `true` | `false` refuses layer publish/unpublish with a message; features and the `GEOMETRY` type still work |
| `chawpi.gis.geoserver.url` | `http://localhost:8081/geoserver` | base URL of the GeoServer REST API |
| `chawpi.gis.geoserver.username` | `admin` | GeoServer REST credentials |
| `chawpi.gis.geoserver.password` | `geoserver` | GeoServer REST credentials |
| `chawpi.gis.geoserver.workspace` | `chawpi` | GeoServer workspace layers publish into |
| `chawpi.gis.geoserver.datastore.name` | `chawpi-postgis` | name GeoServer gives the JDBC data store |
| `chawpi.gis.geoserver.datastore.host` | `postgres` | how GeoServer, in its own container, reaches Postgres |
| `chawpi.gis.geoserver.datastore.port` | `5432` | how GeoServer, in its own container, reaches Postgres |
| `chawpi.gis.geoserver.datastore.database` | `chawpi` | how GeoServer, in its own container, reaches Postgres |
| `chawpi.gis.geoserver.datastore.schema` | none | schema GeoServer's data store reads; unset follows `chawpi.database.data-schema` |
| `chawpi.gis.geoserver.datastore.username` | `chawpi` | how GeoServer, in its own container, reaches Postgres |
| `chawpi.gis.geoserver.datastore.password` | `chawpi` | how GeoServer, in its own container, reaches Postgres |

Env form once below the table: `CHAWPI_GIS_ENABLED`.

The GeoServer datastore schema follows `chawpi.database.data-schema` unless `chawpi.gis.geoserver.datastore.schema`
is set: an app that only configures the former still gets a working layer.

## Extension points

**Implements:** `chawpi.core.metadata.FieldTypeHandler` (`GeometryFieldType`, the `GEOMETRY` type),
`chawpi.core.data.RecordQueryContributor` (`BboxQuery`, the `bbox`/`geometry` parameters),
`chawpi.core.metadata.ObjectRemovalListener` (`LayerCleanup`, unpublishing every layer of an object when the
object itself is deleted), and pages' `chawpi.pages.PageComponentProvider` (`MapPageComponent`, the `MAP` type,
only when chawpi-pages is on the classpath).

**Overridable beans:** `geometryFieldType`, `bboxQuery`, `geoServerClient`, `layerCleanup`, `layerService`,
`layerController`, `featureController`, `mapPageComponent` — all `@ConditionalOnMissingBean`, so an app can
replace any of them.

## Database

Migration location `classpath:db/chawpi/gis`, history table `flyway_history_gis`. `V1__gis.sql`: `CREATE EXTENSION
IF NOT EXISTS postgis WITH SCHEMA public`, three new columns on core's `custom_fields` (`geometry_type`, `srid`,
`dimension`), and `custom_fields_type_valid` dropped and redefined with `GEOMETRY` appended to core's twelve types
([ADR-026 addendum](../adr/0026-per-module-migrations.md#addendum-2026-09-25-p7-a-check-has-one-extending-owner)).
chawpi-gis also adds its own CHECKs on those three columns (a `GEOMETRY` field must carry all three; the shape and
dimension are each one of a fixed set).

## Frontend package

`@hneyra/gis`: `gisModule(options)`, with `options.basePath` (default `'gis'`) and `options.workerUrl` (MapLibre's
worker script URL). Main exports from `index.ts`: `gisModule`, `gisMessages`, `MapView`, `GeometryField`,
`useFeatures`, `featureIdOf`, `geometryFields`, `wmsTileUrl`, and the `Feature`/`FeatureCollection`/
`GeoJsonGeometry`/`GeometryMeta`/`GeometryType` types.

`GeometryField` is the geometry editor a form places where the field's author put it; the registered field
renderer (`GeometryInput`) wraps it, reading the field's declared `geometryType` and `srid`. `MapView` is
`React.lazy`-loaded from a thin wrapper (`LazyMapView`), so MapLibre and terra-draw are pulled in only once a map
is actually drawn, never in the app's first chunk.

i18n namespace `gis`, exported as `gisMessages` (checked in `index.ts`).

Saving a geometry field's settings sends `{ geometryType, srid }`, defaulting to `POLYGON`/`4326`; `dimension` is
never sent — it is the server's call.

See [../../frontend/packages/gis/README.md](../../frontend/packages/gis/README.md) for the full API, including the
MapLibre worker recipe.

## Without this module

Plain PostgreSQL works: core never requires PostGIS. `GEOMETRY` is an unknown field type (`400` on create),
`bbox`/`geometry` are unknown query parameters (`400`), and the gis REST routes answer `404`
([ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md) D1). The map and layers screens and their nav entries
are absent. A page whose generated or edited layout still names a `MAP` component draws a muted placeholder
instead of the map ([ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md) D4).

## Behaviour differences

[ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md) D1: a gis route with the module absent answers `404`
to an authenticated caller, never `403`. D2: any edit of a `GEOMETRY` field with the module absent answers `409`,
revalidated against the handler on every edit. D4: a `MAP` component with the module absent draws a placeholder
instead of failing to load the page.

## Known limitations

- A `MULTI*` geometry field cannot be drawn in the UI: the draw modes are single-part, as in the original app. The
  API accepts `MULTI*` GeoJSON. See [../gis/geometry.md](../gis/geometry.md) for storage and wire format.
