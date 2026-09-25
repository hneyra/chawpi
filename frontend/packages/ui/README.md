# @chawpi/ui

Module guide: [docs/modules/core.md](../../../docs/modules/core.md) (ui has no module doc of its own).

Primitives shared by every chawpi package: `Button`, `Card*`, `Dialog*`, `Input`, `Textarea`, `Label`,
`Select*`, `Table`/`Th`/`Td`/`Badge`, `Tabs`, the `cn()` class merger, and the Tailwind 4 theme
tokens (`theme.css`).

Peer dependencies: `react`, `react-dom`, `react-i18next` (the dialog's close label reads `common.close`).

## Install

```
# .npmrc
@chawpi:registry=https://npm.pkg.github.com
```

```
yarn add @chawpi/ui
```

## Tailwind (required)

The packages ship class names, not compiled CSS. Your app's Tailwind 4 build must see them:

```css
/* src/index.css */
@import 'tailwindcss';
@import '@chawpi/ui/theme.css';
@source '../node_modules/@chawpi';
```

`@source` is relative to the css file. Point it at the `node_modules/@chawpi` folder your app
resolves (in a monorepo, often the root `node_modules`).
