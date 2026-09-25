# ADR-010: Own JWT for the MVP, OIDC-ready

**Status**: accepted · 2026-09-17

> Imported from sapgis on 2026-09-25. Identifiers renamed sapgis → chawpi; the decision is unchanged. See [sapgis origin](../sapgis-origin.md).

## Context

The platform needs authentication, tenancy and permissions from day one. A full identity provider in
the first sprint would delay the vertical slice; skipping security would bake in wrong assumptions.

## Decision

Users live in `chawpi.users` with BCrypt hashes. Login issues an HS256 JWT carrying `sub`, `org` and
`roles`; the backend validates it as a WebFlux OAuth2 resource server. Permissions are
`(role, object, action)` rows, evaluated in services.

## Consequences

- No extra infrastructure, and the tenant is carried by the token rather than by request data.
- Moving to Keycloak or another OIDC provider swaps the decoder and the login endpoint; the
  permission model and every call site stay as they are.
- Symmetric secrets must be managed per environment. The application refuses to start on a secret
  shorter than 32 bytes.
