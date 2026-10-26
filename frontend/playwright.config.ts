import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.PLAYWRIGHT_PORT || 3100);
const fixturePort = Number(process.env.PLAYWRIGHT_FIXTURE_PORT || 3181);
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://127.0.0.1:${port}`;
const fixtureServer = {
  command: `node e2e/subjects-api-fixture.mjs`,
  url: `http://127.0.0.1:${fixturePort}/api/public/subjects`,
  reuseExistingServer: false,
  timeout: 30_000,
  env: { PORT: String(fixturePort) },
};

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  webServer: process.env.PLAYWRIGHT_BASE_URL
    ? [fixtureServer]
    : [
        fixtureServer,
        {
          command: `npm run start -- --port ${port}`,
          url: baseURL,
          reuseExistingServer: false,
          timeout: 120_000,
          env: {
            PORT: String(port),
            HOSTNAME: '127.0.0.1',
            BACKEND_API_URL: `http://127.0.0.1:${fixturePort}`,
          },
        },
      ],
  use: {
    baseURL,
    timezoneId: 'Asia/Ho_Chi_Minh',
    trace: 'retain-on-failure',
    launchOptions: process.env.PLAYWRIGHT_EXECUTABLE_PATH
      ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE_PATH }
      : undefined,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
