import { defineConfig, devices } from '@playwright/test';

/**
 * Responsive visual-regression tests for the public-facing routes.
 *
 * Runs in three viewports: iPhone 13 (390 × 844), iPad portrait (768 × 1024),
 * and desktop (1280 × 800). Authenticated routes are out of scope for now —
 * those need a Keycloak login-replay setup.
 *
 * Local: `cd frontend/app && yarn playwright test`
 * CI:    `.github/workflows/visual-regression.yml`
 */
export default defineConfig({
  testDir: './tests/visual',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
    ignoreHTTPSErrors: true,
  },
  webServer: process.env.PLAYWRIGHT_BASE_URL
    ? undefined
    : {
        command: 'npx --yes serve -s dist -l 4173',
        url: 'http://127.0.0.1:4173',
        timeout: 30_000,
        reuseExistingServer: !process.env.CI,
      },
  projects: [
    {
      name: 'iphone-13',
      use: { ...devices['iPhone 13'] },
    },
    {
      name: 'ipad-portrait',
      use: {
        ...devices['iPad (gen 7)'],
        viewport: { width: 768, height: 1024 },
      },
    },
    {
      name: 'desktop-1280',
      use: { viewport: { width: 1280, height: 800 } },
    },
  ],
});
