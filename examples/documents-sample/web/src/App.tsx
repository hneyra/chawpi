import { automationModule } from '@chawpi/automation'
import { ChawpiApp } from '@chawpi/core'
import { documentsModule } from '@chawpi/documents'

// core plus two modules: document templates and issuing, and the automations that can issue them
export function App() {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'Documents sample', storagePrefix: 'documents-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[documentsModule(), automationModule()]}
    />
  )
}
