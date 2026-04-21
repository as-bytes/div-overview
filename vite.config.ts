import { defineConfig } from "vite";
import { cloudflare } from "@cloudflare/vite-plugin";
import vue from '@vitejs/plugin-vue'
import vuetify from "vite-plugin-vuetify";
import { fileURLToPath } from "node:url";

export default defineConfig({
  plugins: [
    cloudflare(),
    vue(),
    vuetify({ autoImport: true })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8080,
    allowedHosts: ['8080-asbytes-myvitereact-kkgjj222kpw.ws-eu121.gitpod.io'],
    // proxy: {
    //   '/api': {
    //     target: 'http://127.0.0.1:8787',
    //     changeOrigin: true,
    //   }
    // }
  }
});
