// fixture workspaces on disk: the order must follow dependencies + peerDependencies, never
// devDependencies, and a real cycle must stop the build instead of looping.
import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'

import { buildOrder, expandWorkspaces, readWorkspaces } from './run-ordered.mjs'

function writePackage(root, dir, manifest) {
  mkdirSync(join(root, dir), { recursive: true })
  writeFileSync(join(root, dir, 'package.json'), JSON.stringify(manifest))
}

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'run-ordered-'))
  writePackage(root, 'frontend/packages/testing', { name: '@hneyra/testing', peerDependencies: { '@hneyra/core': '*' } })
  writePackage(root, 'frontend/packages/core', { name: '@hneyra/core', dependencies: { '@hneyra/ui': '*', react: '19.3.0' } })
  writePackage(root, 'frontend/packages/ui', { name: '@hneyra/ui', devDependencies: { '@hneyra/testing': '*' } })
  writePackage(root, 'examples/simple-sample/web', { name: 'simple-sample-web', dependencies: { '@hneyra/core': '*' } })
  // a folder without package.json is not a workspace
  mkdirSync(join(root, 'examples/gis-sample/web'), { recursive: true })
  return root
}

test('expands the workspace globs and skips folders without package.json', () => {
  const root = fixture()
  try {
    const dirs = expandWorkspaces(root, ['frontend/packages/*', 'examples/*/web']).map((dir) => dir.slice(root.length + 1))
    assert.deepEqual(dirs.sort(), ['examples/simple-sample/web', 'frontend/packages/core', 'frontend/packages/testing', 'frontend/packages/ui'])
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('orders dependencies first and ignores devDependencies', () => {
  const root = fixture()
  try {
    const order = buildOrder(readWorkspaces(root, ['frontend/packages/*', 'examples/*/web'])).map((pkg) => pkg.name)
    assert.ok(order.indexOf('@hneyra/ui') < order.indexOf('@hneyra/core'))
    assert.ok(order.indexOf('@hneyra/core') < order.indexOf('@hneyra/testing'))
    assert.ok(order.indexOf('@hneyra/core') < order.indexOf('simple-sample-web'))
    assert.equal(order.length, 4)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('refuses a dependency cycle', () => {
  const packages = [
    { name: 'a', dir: 'a', manifest: { name: 'a', dependencies: { b: '1' } } },
    { name: 'b', dir: 'b', manifest: { name: 'b', peerDependencies: { a: '1' } } }
  ]
  assert.throws(() => buildOrder(packages), /workspace cycle: a -> b -> a/)
})
