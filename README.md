# Chawpi

**A metadata-driven application platform, as libraries.** An administrator defines Custom Objects,
Custom Fields, relationships, forms, views, pages, workflows, automations, documents and permissions;
chawpi turns that metadata into a working application at runtime: real PostgreSQL tables, a REST API,
dynamic forms and tables, detail pages. GIS (PostGIS geometry fields, maps, GeoServer) is one optional
module among others.

Chawpi comes from [sapgis](docs/sapgis-origin.md).

## Use it in your app

Backend (Kotlin/Spring Boot 4.1 WebFlux):

```kotlin
dependencies {
    implementation(platform("chawpi:chawpi-bom:0.1.0"))
    implementation("chawpi:chawpi-spring-boot-starter")          // core
    implementation("chawpi:chawpi-spring-boot-starter-documents") // opt-in module
}
```

Frontend (React 19):

```tsx
<ChawpiApp config={{ apiBaseUrl: '/api' }} modules={[documentsModule()]} />
```

Step by step, minimal to full: [docs/guides/build-your-app.md](docs/guides/build-your-app.md).

Modules: views, forms, pages, workflow, automation, documents, gis, agent — one page each in
[docs/modules](docs/modules/README.md). Runnable samples in [examples/](examples/README.md), from
`simple-sample` (core only, plain PostgreSQL) to `full-sample` (every module).

## Layout

```
backend/    Gradle libraries: chawpi-core, chawpi-<module>, starters, chawpi-bom, chawpi-test
frontend/   npm packages: @hneyra/ui, @hneyra/core, @hneyra/<module>, @hneyra/testing
examples/   runnable sample apps (server + web)
infra/      docker compose for local development
docs/       architecture, modules, guides, domain, api, gis, security, development, adr, HISTORY.md
```

## Commands

```bash
docker compose -f infra/docker/compose.yml up -d          # PostGIS + pgvector (add --profile gis for GeoServer)
./gradlew build                                          # ktlint + unit tests
./gradlew integrationTest                                # API tests against a container
yarn install && yarn lint && yarn test && yarn build     # every frontend package
```

Commits follow [Conventional Commits](https://www.conventionalcommits.org) (checked by a git hook and
CI). Releases are cut by release-please; publishing a GitHub Release publishes every library to
GitHub Packages — see [docs/development/releasing.md](docs/development/releasing.md).

## Decisions

Every architectural decision is an ADR in [docs/adr](docs/adr/README.md); what shipped and when is in
[docs/HISTORY.md](docs/HISTORY.md).
