import { agentModule } from '@hneyra/agent'
import { automationModule } from '@hneyra/automation'
import { ChawpiApp } from '@hneyra/core'
import { documentsModule } from '@hneyra/documents'
import { formsModule } from '@hneyra/forms'
import { gisModule } from '@hneyra/gis'
import { pagesModule } from '@hneyra/pages'
import { viewsModule } from '@hneyra/views'
import { workflowModule } from '@hneyra/workflow'

export interface AppProps {
  // maplibre's worker script url: main.tsx passes the one vite bundles. see @hneyra/gis "MapLibre worker"
  workerUrl?: string
}

// core plus all eight modules, as in @hneyra/core's README "Full app"
export function App({ workerUrl }: AppProps) {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'Full sample', storagePrefix: 'full-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[gisModule({ workerUrl }), workflowModule(), pagesModule(), viewsModule(), formsModule(), documentsModule(), automationModule(), agentModule()]}
    />
  )
}
