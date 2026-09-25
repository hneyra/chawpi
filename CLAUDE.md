# Chawpi — metadata-driven application platform, as libraries

Reusable libraries (Spring Boot starters + npm packages) extracted from the original app
([sapgis origin](docs/sapgis-origin.md)). An app adds the core and opts into modules; metadata drives schema, API
and UI at runtime.

## Non-negotiable stack

- **Backend**: Kotlin 2.4.20, Spring Boot 4.1 **WebFlux** (reactive), Gradle 9.7.1 (Kotlin DSL, version catalog,
  convention plugins in `backend/build-logic`), JDK 25
- **Database**: PostgreSQL 18 (+ PostGIS only with `chawpi-gis`, + pgvector optional)
- **Frontend**: React 19.3 (yarn workspaces) + Vite 8 + Tailwind CSS 4 + shadcn-style components + i18next +
  react-router + react-query + zod
- **GIS module**: GeoServer (WMS/WFS/WMTS), MapLibre GL JS
- **Infra**: Docker Compose for local development

## Architectural rules

1. **Metadata-driven**: never generate code per Custom Object. Metadata drives behaviour at runtime.
2. **WebFlux is reactive** ⇒ JPA / Hibernate / Envers are forbidden, and so are Spring Data repositories. Every
   query, fixed schema or dynamic, is SQL through `DatabaseClient` with bound values. Schema names come from
   `ChawpiSchemas` (`${schemas.metadata}.<table>`, `schemas.dataTable(<table>)`), never a literal.
3. **Libraries, not an app**: `chawpi-core` never depends on a module (`CoreArchitectureTest`). Modules extend the
   core only through its SPIs (field types, query contributors, listeners, page components, ports). Every module
   ships its own auto-configuration, `chawpi.<module>.enabled` switch, properties and Flyway migrations (ADR-024,
   ADR-025, ADR-026).
4. **No component scanning of library code**: beans are declared in auto-configurations, with
   `@ConditionalOnMissingBean` where an app may override them. Stereotypes stay on library classes: kotlin-spring
   opens only annotated classes, so `@Transactional` proxies need them (ADR-024).
5. **Multi-tenancy** by `organization_id`; every query filters by the tenant resolved from the JWT.
6. **Dynamic DDL only through `ObjectSchemaManager`**. Identifiers validated and quoted by `SqlIdentifier`. Values
   always bound, never interpolated.
7. **Geometry is a field, and only with `chawpi-gis`**: PostGIS columns with their SRID, GeoJSON in EPSG:4326 over
   the API. Core runs on plain PostgreSQL.
8. **Frontend modules register themselves** (`ChawpiModule`: routes, nav, field renderers, page components, slots,
   i18n). No hardcoded routes or URLs outside the registry; links come from `useChawpiLinks()`. Heavy libraries stay
   behind lazy routes (ADR-028).
9. **Same behaviour as the original app**, except the entries of ADR-031. A new difference needs a new entry.
10. **No overengineering**: an abstraction needs a concrete second user.

## Working rules

- **`.editorconfig` is law** for every file (Kotlin 4 spaces, TS/YAML/MD 2, max 160 columns, LF, final newline).
  Format before finishing: `./gradlew ktlintFormat` for Kotlin, `yarn format` for frontend code. Markdown is
  hand-formatted to the same rules.
- **Conventional Commits** for every commit message and PR title (`feat(core): …`, `fix(gis): …`, `docs: …`).
  Enforced by the commitlint hook and CI. Scopes are free; use the module name.
- Code, identifiers and comments in **English**. Comments **caveman style**: short, say why.
- Change history lives in `docs/HISTORY.md` (newest first), decisions in `docs/adr/` ([index](docs/adr/README.md)).
  A decision is changed by a new ADR, never by editing an old one.

## Commands

```bash
docker compose -f infra/docker/compose.yml up -d
./gradlew build                 # ktlint + unit tests + architecture tests
./gradlew integrationTest       # Testcontainers, or an external DB via CHAWPI_TEST_DB_* (see below)
yarn install && yarn lint && yarn test && yarn build
yarn test:tooling               # frontend/tooling scripts
```

Integration tests against a remote docker daemon or an external database: read
[docs/development/getting-started.md](docs/development/getting-started.md#integration-tests) first (six variables,
suite lock, `it-env.sh`).

## Where things are

- Architecture: [docs/architecture/overview.md](docs/architecture/overview.md)
- Modules: [docs/modules/](docs/modules/README.md) · Build an app: [docs/guides/build-your-app.md](docs/guides/build-your-app.md)
- REST API: [docs/api/rest.md](docs/api/rest.md) · Security: [docs/security/authentication.md](docs/security/authentication.md)
- Releasing: [docs/development/releasing.md](docs/development/releasing.md)

## Definition of Done

Works · has tests · handles errors · is documented · does not break existing features · build passes · tests pass
· core stays module-agnostic · behaviour matches the original app or ADR-031 · formatted with `.editorconfig`.
