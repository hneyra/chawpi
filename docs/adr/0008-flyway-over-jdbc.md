# ADR-008: Flyway over a short-lived JDBC DataSource

**Status**: accepted · 2026-09-17

> Imported from sapgis on 2026-09-25. Identifiers renamed sapgis → chawpi; the decision is unchanged. See [sapgis origin](../sapgis-origin.md).

## Context

The application is reactive end to end, but Flyway is JDBC-only and there is no comparable reactive
migration tool.

## Decision

A single Flyway bean builds its own JDBC DataSource from the same `chawpi.database.*` properties and
migrates during context startup, before WebFlux serves traffic. The JDBC driver is a runtime
dependency for that purpose alone.

## Consequences

- Migrations are versioned and verified, with no bespoke tooling.
- Startup blocks briefly on the migration, which is what anyone expects of a migration.
- Two drivers in the image. Small price, and the blocking one is never touched after startup.
