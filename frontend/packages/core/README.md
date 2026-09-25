# @chawpi/core

Module guide: [docs/modules/core.md](../../../docs/modules/core.md).

The chawpi app in one component: shell, login, dashboard, objects, relationships, records, dynamic
forms, the record detail page renderer, history/audit and administration. Plus the module registry
that lets `@chawpi/gis`, `@chawpi/workflow`, `@chawpi/documents`, … plug in.

Peer dependencies: `react`, `react-dom`, `react-router`, `@tanstack/react-query`, `i18next`,
`react-i18next`. Styling: set up Tailwind as described in `@chawpi/ui`'s README.

## Install

```
# .npmrc
@chawpi:registry=https://npm.pkg.github.com
```

```
yarn add @chawpi/core @chawpi/ui @chawpi/testing -D
```

`@chawpi/core` and `@chawpi/ui` are runtime dependencies of your app; `@chawpi/testing` is dev-only,
for tests. Your `tsconfig.json` needs `"moduleResolution": "bundler"`: these packages resolve through
their `package.json` `exports` map, which the older `node`/`classic` resolutions do not read.

## Quick start

```tsx
import { createRoot } from 'react-dom/client'
import { ChawpiApp } from '@chawpi/core'
import './index.css' // tailwind + @chawpi/ui/theme.css + @source, see @chawpi/ui

createRoot(document.getElementById('root')!).render(
  <ChawpiApp config={{ apiBaseUrl: '/api', appName: 'Catastro', storagePrefix: 'catastro' }} modules={[]} />
)
```

| config | default | meaning |
|---|---|---|
| `apiBaseUrl` | `/api` | REST base url (trailing slash ignored) |
| `storagePrefix` | `chawpi` | localStorage keys `<prefix>.token/.user/.lang`; give each app on an origin its own |
| `languages` | `['es', 'en']` | first is default and fallback; the shell toggle cycles through them |
| `appName`, `appTagline` | the `app.name`/`app.tagline` strings | shell and login header |
| `basename` | – | router basename when not served from `/` |
| `defaultLoginEmail` | `''` | login form prefill (demo apps) |

Config and modules are read once, at mount. One `ChawpiApp` per page: plain `api()` calls use the
client of the mounted app.

## Full app

Assembling core, `@chawpi/ui` and all 8 optional modules into one app:

```tsx
// src/main.tsx
import { createRoot } from 'react-dom/client'
import { ChawpiApp } from '@chawpi/core'
import { agentModule } from '@chawpi/agent'
import { automationModule } from '@chawpi/automation'
import { documentsModule } from '@chawpi/documents'
import { formsModule } from '@chawpi/forms'
import { gisModule } from '@chawpi/gis'
import { pagesModule } from '@chawpi/pages'
import { viewsModule } from '@chawpi/views'
import { workflowModule } from '@chawpi/workflow'
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import 'maplibre-gl/dist/maplibre-gl.css'
import '@chawpi/documents/print.css'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <ChawpiApp
    config={{ apiBaseUrl: '/api', appName: 'Catastro', storagePrefix: 'catastro' }}
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
)
```

```css
/* src/index.css */
@import 'tailwindcss';
@import '@chawpi/ui/theme.css';
@source '../node_modules/@chawpi';
```

`@source` must see every `@chawpi/*` package's class names, ui's theme tokens and documents' print
sheet CSS are separate imports (not run through Tailwind, see their own READMEs), and the maplibre
worker/CSS setup is `@chawpi/gis`'s (see its README for the exact-version pin and bundler recipe).
Drop modules you do not need from both the `import` list and `modules={[...]}`; nothing else in the
snippet changes.

## Backend modules are optional

Core only needs the core backend. Without the pages module the record detail page is generated
from metadata (form, related lists, history); without the views module lists use every visible
field.

## Writing a module

A module is a plain object; every slot is optional.

```tsx
import type { ChawpiModule } from '@chawpi/core'

export function plansModule(): ChawpiModule {
  return {
    id: 'plans', // unique; also the i18n namespace and route-key prefix
    basePath: 'plans', // routes mount under /plans
    routes: [{ id: 'list', path: '', lazy: () => import('./PlansPage') }],
    navGroups: [{ id: 'plans', labelKey: 'plans:nav.group', order: 20 }],
    nav: [{ group: 'plans', labelKey: 'plans:nav.list', order: 10, route: 'list' }],
    i18n: { es: { nav: { group: 'Planos', list: 'Listado' } }, en: { nav: { group: 'Plans', list: 'List' } } }
  }
}
```

| slot | used for |
|---|---|
| `routes` | pages; `chrome`: `shell` (default), `bare` (signed in, no shell), `public` |
| `navGroups`, `nav` | sidebar; core declares `data` 10, `builder` 30, `automation` 40, `administration` 50 |
| `fieldRenderers` | new field types; values live in `record[section]`, the form sends them back there |
| `pageComponents`, `pageActions` | record-page component types and ACTION kinds |
| `recordPanels`, `recordListActions` | extra UI under a record page / in the record list header |
| `historyRenderers`, `auditValueFormatters`, `auditFieldLabels` | history entries and audit values |
| `dashboardCards`, `objectColumns`, `objectTileDetails` | dashboard and object list additions |
| `objectFlags`, `recordQueryKeys` | facts about an object; query keys to refresh after record writes |
| `providers`, `i18n` | app-wide wrappers; strings under the module's own namespace |

`createRegistry` validates the modules at startup and throws a `RegistryError` on a conflict:
- two modules claiming one type;
- a core type claimed;
- duplicate route paths;
- nav pointing at a missing route;
- two modules whose `fieldRenderers[*].settings.defaults` share a key (the object builder flattens
  every renderer's defaults into one map, so a shared key would silently override).

Links: `useChawpiLinks()` gives `records(object)`, `record(object, id)`, … for core screens and
`to('plans:list')` / `has('plans:list')` for module routes.

## Adding a language / overriding strings

Every module ships `es` and `en` (core's own strings too). A module's `i18n` is just
`{ [language]: strings }`, keyed the same way `config.languages` is, so adding a language or
overriding a shipped string is spreading a new object over the module's `i18n` before it goes into
`modules`. Each module also exports its messages object (`gisMessages`, `pagesMessages`, …) for this:

```tsx
import { gisMessages, gisModule } from '@chawpi/gis'

const gis = { ...gisModule(), i18n: { ...gisMessages, pt: { nav: { gis: 'GIS', maps: 'Mapas' /* … */ } } } }

<ChawpiApp config={{ languages: ['es', 'en', 'pt'] }} modules={[gis]} />
```

List every language you ship, including the shipped ones, in `config.languages`: it is what
`createChawpiI18n` reads to build the i18n resources and what the shell's language toggle cycles
through, not the union of what the modules happen to carry. Overriding one key of a shipped
language works the same way: spread `{ ...gisMessages, es: { ...gisMessages.es, map: { ...gisMessages.es.map, title: 'Mapa base' } } }`.

Testing: see `@chawpi/testing`.
