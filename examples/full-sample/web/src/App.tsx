import { agentModule } from '@chawpi/agent'
import { automationModule } from '@chawpi/automation'
import { ChawpiApp } from '@chawpi/core'
import { documentsModule } from '@chawpi/documents'
import { formsModule } from '@chawpi/forms'
import { gisModule } from '@chawpi/gis'
import { pagesModule } from '@chawpi/pages'
import { viewsModule } from '@chawpi/views'
import { workflowModule } from '@chawpi/workflow'

export interface AppProps {
  // maplibre's worker script url: main.tsx passes the one vite bundles. see @chawpi/gis "MapLibre worker"
  workerUrl?: string
}

// core plus all eight modules, as in @chawpi/core's README "Full app"
export function App({ workerUrl }: AppProps) {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'Full sample', storagePrefix: 'full-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[gisModule({ workerUrl }), workflowModule(), pagesModule(), viewsModule(), formsModule(), documentsModule(), automationModule(), agentModule()]}
    />
  )
}
