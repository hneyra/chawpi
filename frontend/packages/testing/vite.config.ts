import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import pkg from './package.json' with { type: 'json' }

const external = Object.keys(pkg.peerDependencies)
const here = (path: string) => fileURLToPath(new URL(path, import.meta.url))

export default defineConfig({
  plugins: [react()],
  build: {
    lib: { entry: here('./src/index.ts'), formats: ['es'], fileName: 'index' },
    rolldownOptions: {
      external: (id: string) => external.some((dep) => id === dep || id.startsWith(`${dep}/`))
    },
    sourcemap: true
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    alias: {
      '@chawpi/ui': here('../ui/src/index.ts'),
      '@chawpi/core': here('../core/src/index.ts'),
      '@chawpi/testing': here('./src/index.ts')
    }
  }
})
