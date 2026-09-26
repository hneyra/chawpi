# @hneyra/automation

Module guide: [docs/modules/automation.md](../../../docs/modules/automation.md).

Automation rules for a chawpi app: "when something happens to a record, do something". A form-based rule
builder (trigger, conditions, actions) and the organisation-wide run log.

## Install

```
# .npmrc
@hneyra:registry=https://npm.pkg.github.com
```

```bash
yarn add @hneyra/core @hneyra/ui @hneyra/automation
```

Peers: `@hneyra/core`, `@hneyra/ui`, `react`, `react-dom`, `@tanstack/react-query`, `i18next`, `react-i18next`, `react-router`.

## Usage

```tsx
import { ChawpiApp } from '@hneyra/core'
import { automationModule } from '@hneyra/automation'

export function App() {
  return <ChawpiApp config={{ apiBaseUrl: '/api', appName: 'My App' }} modules={[automationModule()]} />
}
```

## What it adds

| Slot | Value |
|---|---|
| routes | `automation:rules` → `/automation/rules` (rule builder), `automation:runs` → `/automation/runs` (run log); both lazy |
| nav | group `automation` (core's): "Reglas" (order 20), "Ejecuciones" (order 30) |
| i18n | namespace `automation` (es, en) |

It works with or without `@hneyra/workflow` and `@hneyra/documents`. It reads an object's workflow
(`GET /objects/{object}/workflow`) and its document types (`GET /objects/{object}/document-types`)
under the same query keys those modules use, so the cache is shared. When either module is absent,
its endpoint answers 404, and the state, transition and document-type pickers stay empty.

## Options

| Option | Default | Meaning |
|---|---|---|
| `basePath` | `'automation'` | url prefix of both routes |

## Backend

Paths are the backend's routes. The frontend reaches them through `apiBaseUrl` (default `/api`), so a proxy that
mounts the API elsewhere changes the prefix, not these paths.

Needs the chawpi automation backend module: `/api/objects/{object}/automations` (GET/POST/PUT/DELETE),
`/api/objects/{object}/automations/{name}/runs` and `/api/automation-runs`. Endpoints of modules that
are not installed must answer 404 (not 403).
