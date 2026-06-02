/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/__tests__/**/*.test.ts', '**/__tests__/**/*.test.tsx'],
  // React Native is a peer dependency and is not installed here. Tests run the
  // pure JS/TS components against a lightweight host-element stub instead of the
  // real native runtime (no native modules, deterministic, fast).
  moduleNameMapper: {
    '^react-native$': '<rootDir>/__mocks__/react-native.tsx',
  },
};
