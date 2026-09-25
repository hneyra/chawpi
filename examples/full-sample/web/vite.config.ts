import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const here = (path: string) => fileURLToPath(new URL(path, import.meta.url))
// tests run against the packages' sources, as frontend/packages/* do, so `yarn test` needs no build.
// `vite build` ignores these: it resolves the packages' dist, the way an app outside this repo does.
const source = (name: string) => here(`../../../frontend/packages/${name}/src/index.ts`)
const api = process.env.CHAWPI_API_URL ?? 'http://localhost:8093'
const modules = ['ui', 'core', 'testing', 'gis', 'workflow', 'pages', 'views', 'forms', 'documents', 'automation', 'agent']

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5174, strictPort: true, proxy: { '/api': api } },
  preview: { port: 5174, strictPort: true, proxy: { '/api': api } },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    // e2e/ is playwright's (yarn e2e), not vitest's
    include: ['src/**/*.test.tsx'],
    alias: Object.fromEntries(modules.map((name) => [`@chawpi/${name}`, source(name)]))
  }
})
