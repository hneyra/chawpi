# Architecture

## Shape

An app is its own Spring Boot main class plus the chawpi starters it chooses, and its own React entry point plus
the `@hneyra/*` packages behind those starters. Chawpi ships nothing runnable by itself.

```
                 React app (ChawpiApp + registered modules)
                              │  REST /api + GeoJSON
              Spring Boot 4.1 WebFlux app (chawpi starters: core + chosen modules)
                              │  R2DBC (reactive)
                    PostgreSQL 18 (+ PostGIS only with chawpi-gis)
                              │
                     GeoServer (only with chawpi-gis)  →  WMS / WFS / WMTS
```

One deployable per app: still a modular monolith ([ADR-001](../adr/0001-modular-monolith.md), amended by
[ADR-024](../adr/0024-libraries-and-starters.md)).

## Libraries and the module graph

The backend graph is acyclic:

```
core  <-  views, forms, workflow, automation, documents, gis, agent
core  <-  forms  <-  pages
automation  ->  documents, only through the optional DocumentIssuer port (documents implements it)
```

| Module | Maven artifact | Starter | npm package | Doc |
|---|---|---|---|---|
| core | `chawpi-core` | `chawpi-spring-boot-starter` | `@hneyra/core` (+ `@hneyra/ui`) | [core](../modules/core.md) |
| views | `chawpi-views` | `chawpi-spring-boot-starter-views` | `@hneyra/views` | [views](../modules/views.md) |
| forms | `chawpi-forms` | `chawpi-spring-boot-starter-forms` | `@hneyra/forms` | [forms](../modules/forms.md) |
| pages | `chawpi-pages` | `chawpi-spring-boot-starter-pages` | `@hneyra/pages` | [pages](../modules/pages.md) |
| workflow | `chawpi-workflow` | `chawpi-spring-boot-starter-workflow` | `@hneyra/workflow` | [workflow](../modules/workflow.md) |
| automation | `chawpi-automation` | `chawpi-spring-boot-starter-automation` | `@hneyra/automation` | [automation](../modules/automation.md) |
| documents | `chawpi-documents` | `chawpi-spring-boot-starter-documents` | `@hneyra/documents` | [documents](../modules/documents.md) |
| gis | `chawpi-gis` | `chawpi-spring-boot-starter-gis` | `@hneyra/gis` | [gis](../modules/gis.md) |
| agent | `chawpi-agent` | `chawpi-spring-boot-starter-agent` | `@hneyra/agent` | [agent](../modules/agent.md) |
| testing | `chawpi-test` | — | `@hneyra/testing` | [testing](../modules/testing.md) |

`pages` also depends on `forms`; `gis`, `workflow` and `agent` compile against `pages` only optionally
(`compileOnly`), to register a page component or record transitions when pages is present. `chawpi-core` depends on
no module: `CoreArchitectureTest` fails the build the moment core imports a module package or a module's Gradle
coordinate. See [ADR-024](../adr/0024-libraries-and-starters.md).

## Core packages

| Package (`chawpi.core.<area>`) | Responsibility |
|---|---|
| `common` | RFC 7807 errors, paging, health |
| `platform` | `ChawpiSchemas`, `SqlIdentifier`, Flyway runner (`ChawpiMigrations`), `ModuleMigration`, `SystemColumns`, database and JWT properties |
| `identity` | Users, roles, login, JWT issuing, tenant resolution from the token |
| `metadata` | Custom Objects, Custom Fields, relationships, `FieldTypeRegistry`, `ObjectSchemaManager` |
| `audit` | Append-only audit log and its query service |
| `data` | `RecordStore` port, `PhysicalTableRecordStore`, the dynamic record and relationship APIs |
| `admin` | User and role administration |
| `organization` | Organizations, the tenant |
| `autoconfigure` | `@AutoConfiguration` classes that wire every bean above |

`CoreArchitectureTest` enforces the layering as a DAG, `platform` at the bottom: `common` ← `platform` ← `identity`
← `metadata` ← `audit` ← `data`, with `admin` and `organization` standing on `metadata` and `autoconfigure` on top
of all of them. A package may only import the packages below it in this order; the test fails the build otherwise.

## Extension SPIs

| SPI | Lives in | Pattern | Implemented by |
|---|---|---|---|
| `FieldTypeHandler` + `FieldTypeRegistry` | `chawpi-core` (`metadata`) | Strategy + Registry | core's 12 scalar types; `GEOMETRY` from chawpi-gis |
| `RecordQueryContributor` + `RecordCriterion` | `chawpi-core` (`data`) | Strategy | modules that narrow the record query (list) |
| `SystemColumnContributor` → `SystemColumns` | `chawpi-core` (`platform`) | Registry | modules that add a reserved column name (list) |
| `RecordChangeListener` | `chawpi-core` (`data`) | Observer | modules that react to a record write (list) |
| `ObjectRemovalListener`, `FieldUsage` | `chawpi-core` (`metadata`) | Observer / Chain | modules that store something about an object or field (list) |
| `WorkflowStates` | `chawpi-core` (`data`) | Null Object | `NoWorkflowStates` (core default); chawpi-workflow's real implementation |
| `ModuleMigration` | `chawpi-core` (`platform`) | Registry | every module, one entry each, plus core's own and its dev seed |
| `PageComponentProvider` | `chawpi-pages` | Strategy | chawpi-pages itself (the HISTORY component); other modules that add a page component |
| `DocumentIssuer` | `chawpi-automation` | Port | `NoDocumentIssuer` (automation default); `DocumentIssuerAdapter` in chawpi-documents |

Listener and contributor lists run in `@Order`, synchronously, inside the caller's own call: `RecordService` opens
no transaction of its own, so a listener that needs atomicity opens one itself. An empty list means no module
installed, not a null pointer. See [ADR-025](../adr/0025-extension-spis.md).

## Frontend packages

`@hneyra/ui` (Tailwind primitives), `@hneyra/core` (the app shell, `ChawpiApp`, the registry, everything that works
with no module installed), one package per backend module (`@hneyra/views`, `forms`, `pages`, `workflow`,
`automation`, `documents`, `gis`, `agent`) and `@hneyra/testing`.

A module is a `ChawpiModule` value, usually built by a factory (`gisModule({ workerUrl })`), passed to
`<ChawpiApp modules={[...]} />`. `createRegistry` merges the list once, at mount, into routes, nav groups and
items, field renderers, page components and actions, record panels, history renderers and i18n resources — core
never imports a module, it asks the registry for a slot. Heavy libraries (MapLibre, xyflow, tiptap, dnd-kit) stay
out of the initial bundle behind lazy routes. See [ADR-028](../adr/0028-frontend-module-registry.md).

## Two schemas, two lifecycles

- **Metadata schema** (`chawpi.database.metadata-schema`, default `chawpi`) — platform metadata and identity, plus
  every installed module's own tables. Evolved by one Flyway run per module, each with its own history table
  `flyway_history_<module>`, so a module can join an app later without touching another module's history
  ([ADR-026](../adr/0026-per-module-migrations.md)).
- **Data schema** (`chawpi.database.data-schema`, default `app_data`) — business data. One physical table per
  Custom Object, built at runtime by `ObjectSchemaManager` from the metadata
  ([ADR-004](../adr/0004-physical-table-per-object.md)).

## Request path

```
HTTP  →  controller  →  service (permission + tenant)  →  RecordStore  →  SQL
                                     │
                                     └→ audit_log
```

Every query filters by `organization_id`, resolved from the JWT and never from the request body. A field type's
handler contributes its own SQL fragment for reads and writes; chawpi-gis's `GEOMETRY` handler wraps the column in
`ST_AsGeoJSON(...)` on the way out and `ST_GeomFromGeoJSON(...)` on the way in, so the API always sees GeoJSON.

## Reactive persistence

WebFlux is reactive, so JPA, Hibernate and Envers are out. There is no ORM-style data access layer at all: every
query, fixed schema or dynamic, is SQL issued through `DatabaseClient` with bound values, never interpolated.
Schema names come from `ChawpiSchemas` (validated once at construction), and identifiers are validated and quoted
by `SqlIdentifier`. Migrations run through Flyway over a short-lived JDBC connection at startup
([ADR-008](../adr/0008-flyway-over-jdbc.md)), one run per module ([ADR-026](../adr/0026-per-module-migrations.md)).
Geometry is converted in SQL by chawpi-gis's field type handler, because R2DBC has no PostGIS codec
([ADR-007](../adr/0007-geometry-over-r2dbc.md)).

## Security chain

Core contributes one `SecurityWebFilterChain` at `@Order(0)`, because Spring Boot's reactive resource-server
auto-configuration always adds a chain of its own regardless of `@ConditionalOnMissingBean`. A module or app that
needs its own chain (its own `securityMatcher`) declares one with `@Order` below `0`, so it runs first. See
[core module — security](../modules/core.md#security) and
[../security/authentication.md](../security/authentication.md).

## Decisions

Every decision is an ADR: [../adr/README.md](../adr/README.md). This page leans on:

- [ADR-001](../adr/0001-modular-monolith.md) — modular monolith
- [ADR-004](../adr/0004-physical-table-per-object.md) — one physical table per Custom Object
- [ADR-007](../adr/0007-geometry-over-r2dbc.md) — geometry over R2DBC as GeoJSON in SQL
- [ADR-008](../adr/0008-flyway-over-jdbc.md) — Flyway over a short-lived JDBC DataSource
- [ADR-024](../adr/0024-libraries-and-starters.md) — libraries, starters, a BOM and explicit auto-configuration
- [ADR-025](../adr/0025-extension-spis.md) — the core is extended through SPIs, never by knowing its modules
- [ADR-026](../adr/0026-per-module-migrations.md) — each module owns its migrations and its Flyway history
- [ADR-027](../adr/0027-gis-optional.md) — GIS is optional, and the API is unchanged when it is present
- [ADR-028](../adr/0028-frontend-module-registry.md) — frontend modules plug into a registry
- [ADR-029](../adr/0029-polyglot-monorepo-and-publishing.md) — polyglot monorepo and publishing
