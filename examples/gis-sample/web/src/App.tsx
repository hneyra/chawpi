import { ChawpiApp } from '@hneyra/core'
import { gisModule } from '@hneyra/gis'

export interface AppProps {
  // maplibre's worker script url: main.tsx passes the one vite bundles. see @hneyra/gis "MapLibre worker"
  workerUrl?: string
}

// core plus gis: GEOMETRY fields, the record map, the map page and layers
export function App({ workerUrl }: AppProps) {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'GIS sample', storagePrefix: 'gis-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[gisModule({ workerUrl })]}
    />
  )
}
