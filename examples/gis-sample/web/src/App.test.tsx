import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mockFetch, type FetchMock } from '@chawpi/testing'
import { App } from './App'

const admin = { id: 'u1', email: 'admin@chawpi.local', displayName: 'Admin', organizationId: 'o1', roles: ['ADMIN'] }

let fetch: FetchMock | null = null
beforeEach(() => {
  localStorage.clear()
  window.history.pushState({}, '', '/')
})
afterEach(() => fetch?.restore())

// mounts the real App and signs in through its login form. anything the shell asks for beyond these
// three routes gets the mock's 404 problem, the same answer an absent backend module gives.
// no workerUrl: no map mounts on the home page, and main.tsx is the only place that bundles the worker.
async function signIn() {
  fetch = mockFetch([
    { method: 'POST', path: '/auth/login', body: { token: 't', expiresAt: '2026-12-31T00:00:00Z', user: admin } },
    { path: '/auth/me/permissions', body: { admin: true, objects: {} } },
    { path: '/objects', body: [] }
  ])
  render(<App />)
  const email = await screen.findByLabelText('Correo')
  await userEvent.clear(email)
  await userEvent.type(email, 'admin@chawpi.local')
  await userEvent.type(screen.getByLabelText('Contraseña'), 'admin')
  await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
  expect(await screen.findByRole('heading', { name: 'Inicio' })).toBeInTheDocument()
}

describe('gis-sample web', () => {
  it('signs the seeded admin in', async () => {
    await signIn()
    expect(screen.getAllByText('GIS sample').length).toBeGreaterThan(0)
    expect(localStorage.getItem('gis-sample.token')).toBe('t')
  })

  it('adds the map screens, and only those', async () => {
    await signIn()
    for (const label of ['Mapas', 'Capas']) {
      expect(screen.getByRole('link', { name: label })).toBeInTheDocument()
    }
    for (const label of ['Documentos', 'Reglas', 'Workflows', 'Páginas', 'Asistente']) {
      expect(screen.queryByRole('link', { name: label })).not.toBeInTheDocument()
    }
  })
})
