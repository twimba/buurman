import { expect, test } from '@playwright/test';

/**
 * Visual baseline for the public (unauthenticated) routes. Captures one
 * full-page screenshot per route per viewport. Update baselines with
 * `yarn playwright test --update-snapshots`.
 *
 * Authenticated routes are deliberately out of scope until we add a Keycloak
 * login-replay fixture.
 */

const PUBLIC_ROUTES = [
  { name: 'landing', path: '/' },
  { name: 'login', path: '/login' },
];

for (const { name, path } of PUBLIC_ROUTES) {
  test(`${name} matches baseline`, async ({ page }) => {
    await page.goto(path);
    // Disable Satoshi font swapping flicker by waiting for fonts to be ready.
    await page.evaluate(() => document.fonts.ready);
    // Give animations a moment to settle.
    await page.waitForTimeout(300);
    await expect(page).toHaveScreenshot(`${name}.png`, {
      fullPage: true,
      animations: 'disabled',
      maxDiffPixelRatio: 0.02,
    });
  });
}
