import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em dev, /api vai para a API local: sem CORS e sem mexer no backend.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
});
