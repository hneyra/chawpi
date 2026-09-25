import { defineConfig, devices } from '@playwright/test'

// the full-sample smoke: the real server jar (./gradlew :full-sample-server:bootJar) on a real PostGIS
// database, and the vite dev server in front of it. CHAWPI_DB_PORT (and usually CHAWPI_DB_NAME) pick
// the database, exactly as for bootRun.
export default defineConfig({
  testDir: './e2e',
  timeout: 120_000,
  expect: { timeout: 15_000 },
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: { baseURL: 'http://localhost:5174', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'java -jar ../server/build/libs/app.jar',
      url: 'http://localhost:8093/actuator/health',
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      // no assistant in the smoke, whatever the shell has
      env: { ANTHROPIC_API_KEY: '' }
    },
    {
      command: 'yarn dev',
      url: 'http://localhost:5174',
      timeout: 60_000,
      reuseExistingServer: !process.env.CI
    }
  ]
})
