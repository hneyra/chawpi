# @chawpi/forms

Module guide: [docs/modules/forms.md](../../../docs/modules/forms.md).

The form builder: group an object's fields into titled sections and save the layout under a name. Core's
`DynamicForm` and the page renderer's FORM component already draw stored forms, so this package only adds the
screen that edits them.

## Install

```
# .npmrc
@chawpi:registry=https://npm.pkg.github.com
```

```
yarn add @chawpi/core @chawpi/ui @chawpi/forms
```

## Usage

```tsx
import { ChawpiApp } from '@chawpi/core'
import { formsModule } from '@chawpi/forms'

export function App() {
  return <ChawpiApp config={{ apiBaseUrl: '/api', appName: 'My App' }} modules={[formsModule()]} />
}
```

Tailwind: the app's stylesheet already has `@import "@chawpi/ui/theme.css"` and `@source
"../node_modules/@chawpi"` (see `@chawpi/ui`), which covers this package's classes too.

## What it adds

| Slot | Value |
|---|---|
| route `forms:builder` | `/builder/forms` → `FormBuilderPage` (lazy) |
| nav | "Formularios" / "Forms" in core's builder group, order 20 |
| i18n | namespace `forms` (es, en) |

## Options

| Option | Default | Meaning |
|---|---|---|
| `basePath` | `'builder'` | url prefix of the route: `formsModule({ basePath: 'config' })` serves `/config/forms` |

## Backend

Paths are the backend's routes. The frontend reaches them through `apiBaseUrl` (default `/api`), so a proxy that
mounts the API elsewhere changes the prefix, not these paths.

It needs the chawpi forms backend module (`GET/POST /objects/{object}/forms`, `PUT/DELETE
/objects/{object}/forms/{name}`). Saving or resetting a form invalidates the cached record pages, because
pages embed forms by name. An absent backend module must answer 404.
