import type { Config } from 'jest';
import nextJest from 'next/jest.js';

const createJestConfig = nextJest({
  // Provide the path to your Next.js app to load next.config.js and .env files in your test environment
  dir: './',
})

// Add any custom config to be passed to Jest
const config: Config = {
  coverageProvider: 'v8',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testPathIgnorePatterns: ['<rootDir>/e2e/', '<rootDir>/.next/'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@ant-design/colors/es/(.*)$': '<rootDir>/node_modules/@ant-design/colors/lib/$1',
    '^@ant-design/colors$': '<rootDir>/node_modules/@ant-design/colors/lib/index.js',
  },
  transformIgnorePatterns: [
    '/node_modules/(?!(@ant-design|rc-.*|@babel/runtime|@ant-design/icons|@ant-design/colors)/)',
  ],
}

// createJestConfig is exported this way to ensure that next/jest can load the Next.js config which is async
export default createJestConfig(config)
