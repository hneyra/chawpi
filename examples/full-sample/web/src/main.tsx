import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// ?worker&url bundles the worker with its maplibre-gl-shared import into one file (plain ?url does not)
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import 'maplibre-gl/dist/maplibre-gl.css'
// the printable document sheet's css, not run through tailwind (see @hneyra/documents)
import '@hneyra/documents/print.css'
import { App } from './App'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App workerUrl={workerUrl} />
  </StrictMode>
)
