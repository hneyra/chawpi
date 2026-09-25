import { expect, test, type APIRequestContext } from '@playwright/test'

// the original app's manual flows, headless: login, an object with a GEOMETRY field, its record with the map,
// issuing a document and a workflow transition. the object, workflow and document type are set up
// through the api (the fixture, not the object-builder ui: see the plan's Self-Review "Not in P6");
// every step a person actually does (login, the map, issuing, the transition) is done through the ui.
const ADMIN = { email: 'admin@chawpi.local', password: 'admin' }
const RING = [
  [-77.03, -12.05],
  [-77.02, -12.05],
  [-77.02, -12.04],
  [-77.03, -12.04],
  [-77.03, -12.05]
]

const unique = (prefix: string) => `${prefix}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

async function send(request: APIRequestContext, token: string, method: 'get' | 'post' | 'put', path: string, data?: unknown, status = 200) {
  const response = await request[method](`/api${path}`, { headers: { Authorization: `Bearer ${token}` }, data })
  expect(response.status(), `${method.toUpperCase()} ${path}`).toBe(status)
  return response.json()
}

test('login, a record with its map, issue a document, apply a transition', async ({ page, request }) => {
  const login = await request.post('/api/auth/login', { data: ADMIN })
  expect(login.status()).toBe(200)
  const { token } = (await login.json()) as { token: string }

  const object = unique('e2e')
  const prefix = unique('E').toUpperCase().slice(0, 10)
  await send(
    request,
    token,
    'post',
    '/objects',
    {
      name: object,
      label: 'Predio E2E',
      pluralLabel: 'Predios E2E',
      fields: [
        { name: 'codigo', label: 'Codigo', type: 'TEXT' },
        { name: 'lote', label: 'Lote', type: 'GEOMETRY', geometryType: 'POLYGON', srid: 4326 }
      ]
    },
    201
  )
  await send(request, token, 'put', `/objects/${object}/workflow`, {
    name: unique('wf'),
    label: 'Aprobacion',
    enabled: true,
    definition: {
      states: [
        { name: 'draft', label: 'Borrador', type: 'INITIAL' },
        { name: 'approved', label: 'Aprobado', type: 'FINAL' }
      ],
      transitions: [{ name: 'approve', label: 'Aprobar', from: 'draft', to: 'approved', roles: [] }]
    }
  })
  await send(
    request,
    token,
    'post',
    `/objects/${object}/document-types`,
    { name: 'oficio', prefix, label: 'Oficio', template: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Oficio E2E' }] }] } },
    201
  )
  const record = (await send(
    request,
    token,
    'post',
    `/objects/${object}/records`,
    { attributes: { codigo: 'P-1' }, geometries: { lote: { type: 'Polygon', coordinates: [RING] } } },
    201
  )) as { id: string }

  // 1. login through the ui
  await page.goto('/')
  await page.getByLabel('Correo').fill(ADMIN.email)
  await page.getByLabel('Contraseña').fill(ADMIN.password)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page.getByRole('heading', { name: 'Inicio' })).toBeVisible()

  // 2. the object with its GEOMETRY field is in the catalog
  await page.goto('/data/objects')
  await expect(page.getByText(/Predios? E2E/).first()).toBeVisible()

  // 3. the record, with its polygon on a map
  await page.goto(`/data/objects/${object}/records/${record.id}`)
  await expect(page.locator('#codigo')).toHaveValue('P-1')
  await expect(page.locator('canvas.maplibregl-canvas').first()).toBeVisible()

  // 4. issue a document from the record. the issued number first shows inside a modal dialog
  // (RecordDocuments opens it via setViewing); close it before checking the refetched table, or the
  // dialog's aria-hidden backdrop blocks step 5's button and the duplicate text breaks strict mode.
  await page.getByRole('combobox').filter({ hasText: 'Elige un tipo' }).click()
  await page.getByRole('option', { name: 'Oficio' }).click()
  await page.getByRole('button', { name: 'Emitir' }).click()
  const number = new RegExp(`${prefix}-\\d{4}-001`)
  await expect(page.getByRole('dialog').getByText(number)).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toBeHidden()
  await expect(page.getByRole('cell', { name: number })).toBeVisible()

  // 5. move the record through its workflow
  await expect(page.getByTestId('workflow-state')).toHaveText('Borrador')
  await page.getByRole('button', { name: /^Aprobar/ }).click()
  await expect(page.getByTestId('workflow-state')).toHaveText('Aprobado')
  expect(await send(request, token, 'get', `/objects/${object}/records/${record.id}/transitions`)).toEqual([])
})
