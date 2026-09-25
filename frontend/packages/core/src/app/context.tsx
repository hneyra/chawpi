import { createContext, use, useMemo } from 'react'
import type { ApiClient } from '../api/client'
import { createLinks, type ChawpiLinks } from '../links/links'
import type { ChawpiRegistry } from '../registry/createRegistry'
import type { ChawpiConfig } from './config'

export interface ChawpiContextValue {
  config: ChawpiConfig
  registry: ChawpiRegistry
  apiClient: ApiClient
}

export const ChawpiContext = createContext<ChawpiContextValue | null>(null)

export function useChawpi(): ChawpiContextValue {
  const value = use(ChawpiContext)
  if (!value) throw new Error('chawpi hooks must be used inside ChawpiApp (or ChawpiProviders)')
  return value
}

export function useChawpiConfig(): ChawpiConfig {
  return useChawpi().config
}

export function useRegistry(): ChawpiRegistry {
  return useChawpi().registry
}

export function useApiClient(): ApiClient {
  return useChawpi().apiClient
}

// every in-app url goes through here, so a module mounted elsewhere is still found
export function useChawpiLinks(): ChawpiLinks {
  const registry = useRegistry()
  return useMemo(() => createLinks(registry), [registry])
}
