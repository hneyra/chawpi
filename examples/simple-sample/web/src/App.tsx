import { ChawpiApp } from '@chawpi/core'

// core alone. every @chawpi module is opt-in and this sample opts into none.
export function App() {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'Simple sample', storagePrefix: 'simple-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[]}
    />
  )
}
