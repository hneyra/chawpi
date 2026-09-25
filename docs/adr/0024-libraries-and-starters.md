# ADR-024: Chawpi ships as libraries: starters, a BOM and explicit auto-configuration

**Status**: accepted · 2026-09-25 · amends ADR-001

## Context

The original app was one Gradle module and one Spring Boot application. Its packages found each other through
component scanning, five of them formed cycles, and several ports were single required beans, so metadata could
not boot without automation and gis. Nobody could take part of it: a new app had to fork the whole thing. ADR-001
chose a modular monolith for one deployable; that choice still holds for an app built on chawpi. What changes is
that the modules become libraries an app assembles.

## Decision

**Artifacts.** Maven group `chawpi`, one version for all of them (ADR-029):

| Artifact | What it is |
|---|---|
| `chawpi-core` | identity, organization, metadata, dynamic data, audit, admin, platform plumbing, the SPIs (ADR-025) |
| `chawpi-<module>` | one optional module each: views, forms, pages, workflow, automation, documents, gis, agent. Each depends on core, pages also on forms |
| `chawpi-spring-boot-starter` | core plus what an app runs on: R2DBC PostgreSQL, JDBC PostgreSQL and Flyway at runtime (ADR-008), actuator |
| `chawpi-spring-boot-starter-<module>` | the starter plus that module. The agent starter also adds Embabel's Anthropic provider (ADR-031 D7) |
| `chawpi-bom` | a `java-platform` importing Spring Boot's BOM, with a constraint on every published chawpi artifact |
| `chawpi-test` | test fixtures an app uses to test itself on a real PostgreSQL |

The module graph is acyclic: core ← every module, forms ← pages. `CoreArchitectureTest` fails the build if core
imports a module package.

**Wiring.** Each library registers its `@AutoConfiguration` classes in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Every bean is declared in an
auto-configuration `@Bean` method, with `@ConditionalOnMissingBean` where an app may replace it: an app overrides a
chawpi bean by declaring its own bean of that type. Library code is never component-scanned. Library classes keep
their stereotype annotations anyway (`@Service`, `@Component`, `@RestController`, `@Repository`): nothing scans them,
but the kotlin-spring plugin opens only annotated classes, and without it a `@Transactional` service is final and its
CGLIB proxy fails at startup. `@RestController` is also how WebFlux's handler mapping finds a controller, and
`@Repository` is what exception translation keys on.

**Switches.** Every module has `chawpi.<module>.enabled`, default `true`. When it is `false`, the module adds no
beans, no routes and no migration. Core has no switch, because it is the base the others stand on.

**Configuration.** Everything lives under `chawpi.*` (environment `CHAWPI_*`). `ChawpiEnvironmentPostProcessor` adds
lowest-precedence defaults: database coordinates from `CHAWPI_DB_*`, the R2DBC URL and pool, and RFC 7807 problem
details. It never adds a JWT secret: a library must not ship one that works.

**Property classes.** A module's switch lives in `Chawpi<Module>Properties` in `chawpi.<module>.autoconfigure`.
Settings that the module's own code reads live beside that code: `AgentProperties` (`chawpi.agent`),
`AutomationProperties` (`chawpi.automation`), `GeoServerProperties` (`chawpi.gis.geoserver`). Moving them into
`autoconfigure` would make domain code import its own wiring. Core's classes (`ChawpiDatabaseProperties`,
`JwtProperties`, `ChawpiWebProperties`) live in `chawpi.core.platform`, the bottom of core's layering.

**The app.** `@SpringBootApplication` is enough. `@ChawpiApplication` is an optional shorthand for
`@SpringBootApplication` plus `@ConfigurationPropertiesScan`, forwarding `exclude`, `excludeName` and
`scanBasePackages`. Scanning starts at the app's package, so **an app must not live in package `chawpi` or below
it**: its scan would reach the library's controllers and register them a second time.

**Public surface.** Kotlin's `explicitApi()` stays off for 0.x. The supported API is: the SPIs of ADR-025, the bean
types an auto-configuration exposes for overriding, the `@ConfigurationProperties` classes, `@ChawpiApplication`,
and the `chawpi-test` fixtures. Every other public declaration is implementation and may change in any 0.x minor.
Turning `explicitApi()` on means adding a visibility modifier to every declaration in nine libraries. That cost buys
nothing until there are outside users to protect, so it is revisited before 1.0.

## Consequences

- An app gets the platform with two dependency lines and no code: the BOM and a starter. Each module is one more
  starter line.
- Any bean can be replaced without forking, and removing a module is removing a dependency.
- `chawpi.<module>.enabled=false` lets an app ship a module's jar and still keep it off.
- Without component scanning, a new library class does nothing until an auto-configuration declares it. Each
  module's auto-configuration test catches a forgotten one.
- Starters carry no code. A change of runtime driver or default provider is a one-line starter change.
