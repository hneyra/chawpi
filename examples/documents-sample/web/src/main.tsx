import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
// the printable document sheet's css, not run through tailwind (see @hneyra/documents)
import '@hneyra/documents/print.css'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
