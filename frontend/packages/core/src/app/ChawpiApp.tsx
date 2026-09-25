import { QueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { BrowserRouter } from 'react-router'
import { createApiClient } from '../api/client'
import { createChawpiI18n } from '../i18n/createI18n'
import type { ChawpiModule } from '../registry/contract'
import { createRegistry } from '../registry/createRegistry'
import { ChawpiProviders } from './ChawpiProviders'
import { ChawpiRoutes } from './ChawpiRoutes'
import { resolveConfig, type ChawpiConfig } from './config'
import { coreModule } from './coreModule'

export interface ChawpiAppProps {
  config?: Partial<ChawpiConfig>
  modules?: ChawpiModule[]
}

// the whole app from config + modules. both are read once, at mount: they are wiring, not state.
export function ChawpiApp({ config, modules = [] }: ChawpiAppProps) {
  const [app] = useState(() => {
    const resolved = resolveConfig(config)
    const registry = createRegistry([coreModule, ...modules])
    const apiClient = createApiClient({ baseUrl: resolved.apiBaseUrl, storagePrefix: resolved.storagePrefix })
    const i18n = createChawpiI18n({ languages: resolved.languages, storageKey: apiClient.keys.lang, modules: registry.modules })
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } })
    return { config: resolved, registry, apiClient, i18n, queryClient }
  })

  return (
    <ChawpiProviders config={app.config} registry={app.registry} apiClient={app.apiClient} i18n={app.i18n} queryClient={app.queryClient}>
      <BrowserRouter basename={app.config.basename}>
        <ChawpiRoutes />
      </BrowserRouter>
    </ChawpiProviders>
  )
}
