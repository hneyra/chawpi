# Core module

An app that installs core gets identity and login, organizations (the tenant), Custom Objects and Fields, dynamic
records and related records, relationships, caller permissions, audit and history, and the admin screens for users,
roles and permissions. Every other module builds on it; core itself depends on nothing else in chawpi.

## Install

```kotlin
implementation("chawpi:chawpi-spring-boot-starter")
```

The starter is core plus what an app runs on: the R2DBC and JDBC PostgreSQL drivers, Flyway, and actuator
(`/actuator/health` is already public in core's security chain).

```bash
yarn add @hneyra/core @hneyra/ui
```

```tsx
<ChawpiApp config={{ apiBaseUrl: '/api', appName: 'My app', storagePrefix: 'myapp' }} modules={[]} />
```

See [../guides/build-your-app.md](../guides/build-your-app.md).

Core has no `chawpi.core.enabled` switch: every module has one, core is the base they stand on
([ADR-024](../adr/0024-libraries-and-starters.md), [ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md) D8).

### Your app

Never put the app's main class in package `chawpi` or below it: `@ChawpiApplication`'s (and plain
`@ConfigurationPropertiesScan`'s) component scan starts at the app's own package, so a scan that reaches into
`chawpi.*` would register the library's controllers a second time ([ADR-024](../adr/0024-libraries-and-starters.md)).
An app overrides any core bean by declaring its own bean of the same type — see "Extension points" below.

## What it adds

- Identity: login and the signed-in user, under `/api/auth` (`POST /api/auth/login`, `GET /api/auth/me`,
  `GET /api/auth/me/permissions` for caller permissions).
- Organizations: the tenant itself, under `/api/organizations`.
- Custom Objects and Fields: object and field metadata, under `/api/objects` and `/api/metadata/objects` (system
  fields under `/api/metadata/system-fields`), the 12 scalar field types and the `FieldTypeRegistry`.
- Relationships: `/api/relationships`, plus the related-record routes nested under `/api/objects/{object}`.
- Dynamic records and related records: `/api/objects/{object}/records`.
- Audit and history: `/api/audit` and `/api/objects/{object}/records/{id}/history`.
- Admin: users and roles, under `/api/users` and `/api/roles`.

See [../api/rest.md](../api/rest.md) for the full method-by-method table.

Screens (`frontend/packages/core/src/app/coreModule.ts`): login (public), the dashboard, the objects list and
builder, relationships, the record list/form/detail pages, users, roles, permissions and audit. Nav groups: `data`
(order 10), `builder` (30, filled by other modules), `automation` (40, filled by other modules) and
`administration` (50) — modules that add screens of their own place them in these same groups.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `chawpi.database.host` | `localhost` | PostgreSQL host |
| `chawpi.database.port` | `5432` | PostgreSQL port |
| `chawpi.database.name` | `chawpi` | database name |
| `chawpi.database.username` | `chawpi` | database user |
| `chawpi.database.password` | `chawpi` | database password |
| `chawpi.database.metadata-schema` | `chawpi` | schema Flyway owns: identity, organizations, metadata |
| `chawpi.database.data-schema` | `app_data` | schema holding one physical table per custom object (ADR-004) |
| `chawpi.database.migrate` | `true` | `false` when the app runs migrations another way |
| `chawpi.security.jwt.secret` | *(none)* | HS256 signing key, at least 32 bytes; required, a library must not ship one that works |
| `chawpi.web.problem-base-uri` | `https://chawpi.dev/problems` | RFC 7807 `type` base; the full type is this plus `/<status>` |
| `chawpi.web.cors-allowed-origin-patterns` | `["http://localhost:*"]` | browser origins the API answers |
| `chawpi.seed.dev` | `false` | `true` adds the dev seed migration (see "Database") |

`chawpi.security.jwt.issuer` (default `chawpi`) and `chawpi.security.jwt.ttl` (default 8 hours) are also read from
`JwtProperties` but rarely need changing. `metadata-schema` and `data-schema` must differ and are validated as
plain identifiers at boot.

Without any YAML, `ChawpiEnvironmentPostProcessor` adds lowest-precedence defaults: `chawpi.database.*` from
`CHAWPI_DB_HOST`, `CHAWPI_DB_PORT`, `CHAWPI_DB_NAME`, `CHAWPI_DB_USERNAME`, `CHAWPI_DB_PASSWORD` (env names, not a
mechanical `CHAWPI_DATABASE_*` transform of the property path); `spring.r2dbc.url`/`username`/`password` built from
those; an R2DBC pool of 5 to 20 connections; and `spring.webflux.problemdetails.enabled=true`. It never sets
`chawpi.security.jwt.secret` — only `CHAWPI_JWT_SECRET` does, and an app that sets neither fails at boot instead of
starting with a usable default.

## Security

Core declares one `SecurityWebFilterChain` at `@Order(0)`, `@ConditionalOnMissingBean`: Spring Boot's reactive
resource-server auto-configuration always contributes a catch-all chain of its own, so core's must win on order,
not by being the only one. Public paths are `/api/auth/login`, `/api/health`, `/actuator/health/**` and every
`OPTIONS` request; everything else needs a valid token. An app that declares its own `SecurityWebFilterChain` bean
replaces core's chain entirely, public paths and CORS included. A module that needs a chain next to core's declares
it in an auto-configuration that runs after core's, with its own `securityMatcher` and an `@Order` below `0`.

The signing key is a `ChawpiJwtKey` bean, wrapping the raw `SecretKey` in its own type so an app's unrelated
`SecretKey` bean can never become the JWT key by accident, and injection never turns ambiguous. An app that wants a
different key declares its own `ChawpiJwtKey` bean instead of setting the property.

`PasswordEncoder` is a `BCryptPasswordEncoder` bean, `@ConditionalOnMissingBean` like everything else here.

See [../security/authentication.md](../security/authentication.md) for the full authentication, tenancy and
authorization model.

## Extension points

**Defines** (core depends on no module, so it implements none of its own SPIs; see
[../architecture/overview.md#extension-spis](../architecture/overview.md#extension-spis) and
[ADR-025](../adr/0025-extension-spis.md)):

- `FieldTypeHandler` + `FieldTypeRegistry` — a field type's validation, storage and read shape; the registry checks
  for collisions at boot and holds core's 12 scalar types first, then every module's handler in `@Order`.
- `RecordQueryContributor` + `RecordCriterion` — a `WHERE` fragment a module adds to the record list query, always
  parenthesised before it joins the rest.
- `SystemColumnContributor` → `SystemColumns` — a reserved column name a module owns on every record table, so no
  custom field can take it.
- `RecordChangeListener` — runs synchronously, in `@Order`, inside the caller's own call right after a record write.
- `ObjectRemovalListener`, `FieldUsage` — a module's veto or note when an object or field is about to be removed.
- `WorkflowStates` — the state a record is in, if any; core's default is `NoWorkflowStates`, a null object.
- `ModuleMigration` — one Flyway location and history table per module ([ADR-026](../adr/0026-per-module-migrations.md));
  core registers its own (`core`) and its opt-in dev seed (`core_seed`).

**Overridable beans:** every bean core declares is `@ConditionalOnMissingBean`, so an app replaces any of them by
declaring its own bean of the same type, grouped by the auto-configuration that owns them:

- Platform (`ChawpiPlatformAutoConfiguration`): `chawpiSchemas`, `systemColumns`, `chawpiMigrations`,
  `globalExceptionHandler`, `healthController`.
- Security (`ChawpiSecurityAutoConfiguration`): `chawpiJwtKey`, `jwtDecoder`, `passwordEncoder`,
  `securityFilterChain`, `corsConfigurationSource`, `roleQueries`, `roleDirectory`, `currentUser`, `accessPolicy`,
  `userRepository`, `jwtService`, `authService`, `authController`.
- Metadata (`ChawpiMetadataAutoConfiguration`): `fieldTypeRegistry`, `customObjectRepository`,
  `customFieldRepository`, `relationshipRepository`, `objectSchemaManager`, `metadataService`, `relationshipService`,
  `metadataMapper`, `relationshipMapper`, `callerPermissionsService`, `objectController`, `objectMetadataController`,
  `systemFieldController`, `relationshipController`, `callerPermissionsController`.
- Data (`ChawpiDataAutoConfiguration`): `auditService`, `auditQueryService`, `auditController`, `workflowStates`,
  `recordStore`, `recordQueryParser`, `recordService`, `relatedRecordService`, `recordController`,
  `relatedRecordController`.
- Admin (`ChawpiAdminAutoConfiguration`): `adminService`, `userAdminController`, `roleAdminController`,
  `organizationRepository`, `organizationService`, `organizationController`.

`chawpiCoreMigration` and `chawpiCoreSeedMigration` are the two exceptions: they register `ModuleMigration` values
into an ordered list, not a single replaceable bean, so they carry no `@ConditionalOnMissingBean`.

## Database

Migration location `classpath:db/chawpi/core`, history table `flyway_history_core`, order `0`
([ADR-026](../adr/0026-per-module-migrations.md)). Creates the `pgcrypto` extension `WITH SCHEMA public` (shared by
every app in the database, so it outlives any one app) and, when the server ships it, `pgvector` the same way.
Tables: `organizations`, `users`, `roles`, `user_roles`, `custom_objects`, `custom_fields`, `relationships`,
`permissions`, `field_permissions`, `audit_log`.

The opt-in dev seed, `classpath:db/chawpi/core-seed` (history table `flyway_history_core_seed`, order `10`), runs
only with `chawpi.seed.dev=true` and inserts a demo organization, an `ADMIN` role with every permission, and the
user `admin@chawpi.local` / `admin`
([ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md) D6, [ADR-030](../adr/0030-rebrand-sapgis-to-chawpi.md)).
The password hash is written out as a literal `$2a$` BCrypt hash instead of calling pgcrypto's `crypt()`, because
with several schemas in one database the extension resolves from whichever schema created it first.

## Frontend package

`@hneyra/core`: `ChawpiApp`, the config type and defaults from `config.ts` (`apiBaseUrl` `/api`, `appName` unset,
`storagePrefix` `chawpi`, `defaultLoginEmail` `''`, plus `languages`, `appTagline` and `basename`), the registry
types (`ChawpiModule` and its contribution types), `createRegistry`, `useChawpiLinks` (route building, `to()`/`has()`
for module routes, and typed helpers for every core screen) and `useAuth`. i18n namespace `core`; core's own strings
are always reachable through `fallbackNS`.

`@hneyra/ui`: the shared primitives (`Button`, `Card*`, `Dialog*`, `Input`, `Textarea`, `Label`, `Select*`,
`Table`/`Th`/`Td`/`Badge`, `Tabs`), the `cn()` class merger, and the Tailwind 4 theme (`theme.css`). An app's
Tailwind entry point consumes it as:

```css
@import 'tailwindcss';
@import '@hneyra/ui/theme.css';
@source '../node_modules/@hneyra';
```

`@source` must see every `@hneyra/*` package's class names, not only `@hneyra/ui`'s own.

See [../../frontend/packages/core/README.md](../../frontend/packages/core/README.md) and
[../../frontend/packages/ui/README.md](../../frontend/packages/ui/README.md) for the full API.

## Without this module

Core is always installed.

## Behaviour differences

[ADR-031](../adr/0031-deliberate-deviations-from-sapgis.md):

- D1: a route of a module that is not installed answers `404` to an authenticated caller and `401` without a token,
  never `403`.
- D2: any edit of a field whose type's module is not installed answers `409` — the field is revalidated against its
  handler on every edit.
- D6: the dev seed user is created only with `chawpi.seed.dev=true`, as `admin@chawpi.local`.
- D8: core has no `chawpi.<module>.enabled` switch; every other module has one.
- D9: link and unlink of related records now hold both records to the record API's rules, with `404` instead of an
  unchecked write, and one `UPDATE` history row on each.
- D10: the audit "after" snapshot stores every field, not just the caller's writable projection.
- D11: a `401` in the middle of a session signs the user out and returns to the login page, instead of only dropping
  the token.
- D12: the record detail page falls back to the default page only on a `404` from the custom page, not on any error.
- D13: a `NAVIGATE` page action with no target links to the objects list, instead of `/undefined`.
- D14: the login form's email is empty by default, configurable with `ChawpiApp` `config.defaultLoginEmail`.
- D16: field type names are trimmed before matching, so `" text "` is `TEXT`.

## Known limitations

None.
