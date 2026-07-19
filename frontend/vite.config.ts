import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    // Proxy hacia el backend Spring Boot: evita CORS en desarrollo.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
