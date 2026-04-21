/* / / / <reference types="vitest/config" /> */
import { fileURLToPath } from 'node:url'
import { defineConfig, UserConfig } from 'vite'
import { configDefaults } from 'vitest/config'

export default defineConfig({
  test: {
    // reporters: ['default', 'hanging-process'],
    environment: 'jsdom',
    exclude: [...configDefaults.exclude, 'e2e/*'],
    root: fileURLToPath(new URL('./', import.meta.url)),
    transformMode: {
      web: [/\.[jt]sx$/]
    }
  }
} as UserConfig)
