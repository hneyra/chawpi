import { ChawpiApp } from '@hneyra/core'

// core alone. every @hneyra module is opt-in and this sample opts into none.
export function App() {
  return (
    <ChawpiApp
      config={{ apiBaseUrl: '/api', appName: 'Simple sample', storagePrefix: 'simple-sample', defaultLoginEmail: 'admin@chawpi.local' }}
      modules={[]}
    />
  )
}
