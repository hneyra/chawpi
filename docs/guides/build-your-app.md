# Build your app

An app is your own Spring Boot main class plus one or more chawpi starters, and your own React entry plus one or
more `@chawpi/*` packages. Start minimal — the core starter and a plain PostgreSQL database — and add modules one
line at a time as you need them.

## Before you start

- Java 25 (the backend's Gradle toolchain), Node >=26, PostgreSQL 18 (add PostGIS only if you install `gis`).
- A GitHub token with `read:packages`, and registry setup for Gradle and npm: see
  [../development/releasing.md](../development/releasing.md#consuming-a-published-library) ("Consuming a published
  library").

## A minimal app: backend

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    id("org.springframework.boot") version "4.1.1"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/hneyra/chawpi")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(platform("chawpi:chawpi-bom:0.1.0"))
    implementation("chawpi:chawpi-spring-boot-starter")
}

kotlin { jvmToolchain(25) }
```

```kotlin
// src/main/kotlin/com/example/myapp/MyApp.kt
package com.example.myapp

import chawpi.core.autoconfigure.ChawpiApplication
import org.springframework.boot.runApplication

@ChawpiApplication
class MyApp

fun main(args: Array<String>) {
    runApplication<MyApp>(*args)
}
```

```yaml
# src/main/resources/application.yml
chawpi:
  security:
    jwt:
      secret: ${CHAWPI_JWT_SECRET}
  seed:
    dev: true   # admin@chawpi.local / admin, development only
server:
  port: 8090
```

Without any of that yaml, the starter still boots: `ChawpiEnvironmentPostProcessor` fills in
`CHAWPI_DB_HOST` (`localhost`), `CHAWPI_DB_PORT` (`5432`), `CHAWPI_DB_NAME`/`CHAWPI_DB_USERNAME`/`CHAWPI_DB_PASSWORD`
(`chawpi`/`chawpi`/`chawpi`) and the R2DBC URL built from them, at the lowest precedence — set the environment
variables, or `chawpi.database.*` in your own yaml, to point at a different server. It never fills in
`chawpi.security.jwt.secret`: a library must not ship a secret that works, so an app with no secret configured
fails to start, on purpose.

One rule that only matters once, and matters a lot: **your app must not live in package `chawpi` or below it**
(ADR-024). `@ChawpiApplication` component-scans from your application class's package downward; if that package is
`chawpi` or a sub-package, the scan also reaches the library's own controllers and registers them a second time.

This gets you `/api/objects`, `/api/records`, identity, audit and admin, already routed — see
[../api/rest.md](../api/rest.md) for the full surface.

## A minimal app: frontend

```json
{
  "dependencies": {
    "@chawpi/core": "*",
    "@chawpi/ui": "*",
    "@tanstack/react-query": "^5.103.1",
    "i18next": "^26.4.2",
    "react": "^19.3.0",
    "react-dom": "^19.3.0",
    "react-i18next": "^17.0.14",
    "react-router": "^8.4.0"
  }
}
```

```tsx
// src/main.tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ChawpiApp } from '@chawpi/core'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ChawpiApp config={{ apiBaseUrl: '/api', appName: 'My App' }} modules={[]} />
  </StrictMode>
)
```

```css
/* src/index.css */
@import 'tailwindcss';
@import '@chawpi/ui/theme.css';
@source '../node_modules/@chawpi';
```

Point your dev server's proxy at the backend: Vite, `server.proxy['/api'] = 'http://localhost:8090'` (or whatever
`server.port` you set above). `ChawpiApp` needs no extra providers around it — router, query client and i18n are
built inside it from `config` and `modules`.

## Add modules

Core is always installed. Every other module is one backend starter plus one frontend package; an app adds a
module by listing both.

Registration always looks like `<ChawpiApp modules={[<name>Module()]} />` (`gis` also takes `{ workerUrl }`, see
below).

| Module | Backend starter | Frontend package | Extra requirements | Doc |
|---|---|---|---|---|
| views | `chawpi-spring-boot-starter-views` | `@chawpi/views` | None | [views.md](../modules/views.md) |
| forms | `chawpi-spring-boot-starter-forms` | `@chawpi/forms` | None | [forms.md](../modules/forms.md) |
| pages | `chawpi-spring-boot-starter-pages` | `@chawpi/pages` | Backend starter pulls in `chawpi-forms` | [pages.md](../modules/pages.md) |
| workflow | `chawpi-spring-boot-starter-workflow` | `@chawpi/workflow` | None | [workflow.md](../modules/workflow.md) |
| automation | `chawpi-spring-boot-starter-automation` | `@chawpi/automation` | Calls `DocumentIssuer` | [automation.md](../modules/automation.md) |
| documents | `chawpi-spring-boot-starter-documents` | `@chawpi/documents` | Implements `DocumentIssuer` | [documents.md](../modules/documents.md) |
| gis | `chawpi-spring-boot-starter-gis` | `@chawpi/gis` | PostGIS database; frontend also needs `maplibre-gl`, a `workerUrl` | [gis.md](../modules/gis.md) |
| agent | `chawpi-spring-boot-starter-agent` | `@chawpi/agent` | Needs `ANTHROPIC_API_KEY` (or another provider) | [agent.md](../modules/agent.md) |

`documents` and `automation` connect through `DocumentIssuer`: `automation`'s `GENERATE_DOCUMENT` action calls it,
`documents` implements it, and installing only one of the two still works — the call is optional. `gis` needs a
PostGIS-enabled PostgreSQL; `terra-draw` and its adapter come as regular dependencies of
`@chawpi/gis`, but `maplibre-gl` is a peer you add yourself, and `gisModule` takes a `workerUrl` pointing at
MapLibre's worker script — see [gis.md](../modules/gis.md) for the exact recipe. `pages` also needs `forms` on the
frontend if you use the `FORM` page component outside a metadata-defined page. `agent` falls back to
`ANTHROPIC_API_KEY` for `chawpi.agent.api-key`; another Embabel provider starter works too, see
[agent.md](../modules/agent.md).

## A full app

```kotlin
dependencies {
    implementation(platform("chawpi:chawpi-bom:0.1.0"))
    implementation("chawpi:chawpi-spring-boot-starter")
    implementation("chawpi:chawpi-spring-boot-starter-views")
    implementation("chawpi:chawpi-spring-boot-starter-forms")
    implementation("chawpi:chawpi-spring-boot-starter-pages")
    implementation("chawpi:chawpi-spring-boot-starter-workflow")
    implementation("chawpi:chawpi-spring-boot-starter-automation")
    implementation("chawpi:chawpi-spring-boot-starter-documents")
    implementation("chawpi:chawpi-spring-boot-starter-gis")
    implementation("chawpi:chawpi-spring-boot-starter-agent")
}
```

```tsx
import { ChawpiApp } from '@chawpi/core'
import { agentModule } from '@chawpi/agent'
import { automationModule } from '@chawpi/automation'
import { documentsModule } from '@chawpi/documents'
import { formsModule } from '@chawpi/forms'
import { gisModule } from '@chawpi/gis'
import { pagesModule } from '@chawpi/pages'
import { viewsModule } from '@chawpi/views'
import { workflowModule } from '@chawpi/workflow'

<ChawpiApp
  config={{ apiBaseUrl: '/api', appName: 'My App' }}
  modules={[
    gisModule({ workerUrl }),
    workflowModule(),
    pagesModule(),
    viewsModule(),
    formsModule(),
    documentsModule(),
    automationModule(),
    agentModule()
  ]}
/>
```

That order, and the rest of the setup (worker/CSS imports, `@source`), is `@chawpi/core`'s README "Full app"
section — follow it exactly; this guide only lists the pieces. [full-sample](../../examples/full-sample/README.md)
shows the whole setup in a running app.

## Configure

The settings apps change most, each under `chawpi.*` (environment `CHAWPI_*`):

| Setting | Property | Module doc |
|---|---|---|
| Database coordinates | `chawpi.database.host`/`port`/`name`/`username`/`password` | [core.md](../modules/core.md) |
| JWT secret | `chawpi.security.jwt.secret` | [core.md](../modules/core.md) |
| Metadata/data schemas | `chawpi.database.metadata-schema`, `chawpi.database.data-schema` | [core.md](../modules/core.md) |
| CORS origins | `chawpi.web.cors-allowed-origin-patterns` | [core.md](../modules/core.md) |
| Dev seed data | `chawpi.seed.dev` | [core.md](../modules/core.md) |
| Module enabled flags | `chawpi.<module>.enabled` (default `true`) | each module's doc |
| GeoServer URL | `chawpi.gis.geoserver.url` | [gis.md](../modules/gis.md) |
| Model provider key | `chawpi.agent.api-key` (defaults to `ANTHROPIC_API_KEY`) | [agent.md](../modules/agent.md) |

## Override a bean

Every chawpi bean is `@ConditionalOnMissingBean`, so an app overrides one by declaring its own bean of that type:

```kotlin
@Configuration
class SecurityBeans {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
```

Spring picks up your `@Bean` before the library's auto-configuration runs its own `@ConditionalOnMissingBean`
method, so the library's `PasswordEncoder` never gets created.

## Write your own module

Backend: a library with an `@AutoConfiguration` class registered in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, beans implementing core's SPIs
(for example a `FieldTypeHandler`), its own `ModuleMigration` for schema changes, and a `chawpi.<name>.enabled`
switch. Frontend: a `ChawpiModule` factory (routes, nav, field renderers, page components — see `@chawpi/core`'s
README, "Writing a module"). [ADR-025](../adr/0025-extension-spis.md) documents the SPIs and
[ADR-028](../adr/0028-frontend-module-registry.md) the frontend registry; `views` is the smallest shipped module
and a good model to copy from.

## Test it

See [../modules/testing.md](../modules/testing.md).

## Examples

See [../../examples/README.md](../../examples/README.md) for how to run them. Each is a server and a web app built
the way this guide describes:

- [simple-sample](../../examples/simple-sample/README.md): core only, on plain PostgreSQL.
- [documents-sample](../../examples/documents-sample/README.md): core plus documents and automation.
- [gis-sample](../../examples/gis-sample/README.md): core plus gis, with the Perené cadastre model.
- [full-sample](../../examples/full-sample/README.md): every module, the app of "A full app" above in working form.
