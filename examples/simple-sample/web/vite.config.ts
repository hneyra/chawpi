import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const here = (path: string) => fileURLToPath(new URL(path, import.meta.url))
// tests run against the packages' sources, as frontend/packages/* do, so `yarn test` needs no build.
// `vite build` ignores these: it resolves the packages' dist, the way an app outside this repo does.
const source = (name: string) => here(`../../../frontend/packages/${name}/src/index.ts`)
const api = process.env.CHAWPI_API_URL ?? 'http://localhost:8091'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5171, strictPort: true, proxy: { '/api': api } },
  preview: { port: 5171, strictPort: true, proxy: { '/api': api } },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    include: ['src/**/*.test.tsx'],
    alias: {
      '@chawpi/ui': source('ui'),
      '@chawpi/core': source('core'),
      '@chawpi/testing': source('testing')
    }
  }
})
