import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // El front usa rutas relativas (/api, /ws); en desarrollo Vite las reenvía al backend.
  // Así cualquier dispositivo de la red que abra http://<ip-del-equipo>:5173 llega al mismo backend.
  const backend = loadEnv(mode, process.cwd(), '').VITE_BACKEND_URL || 'http://localhost:8081'
  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: true,
      proxy: {
        '/api': { target: backend, changeOrigin: true },
        '/ws': { target: backend.replace(/^http/, 'ws'), ws: true, changeOrigin: true },
      },
    },
    build: {
      outDir: '../back/src/main/resources/static',
      emptyOutDir: true,
    },
  }
})
