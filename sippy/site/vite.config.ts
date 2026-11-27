/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  // GitHub Pages readiness: base path is configurable via VITE_BASE
  // (e.g. VITE_BASE=/sippy/ npm run build). .env provides the default
  // (VITE_BASE=/); shell env overrides it (loadEnv merges process env).
  // envDir "." keeps the config free of `process` (no @types/node needed).
  const env = loadEnv(mode, ".", "VITE_");
  const base = env.VITE_BASE || "/";

  return {
    base,
    plugins: [react(), tailwindcss()],
    server: {
      host: "0.0.0.0",
      port: 5173,
      strictPort: true,
      // Dev server only: accept the sandbox preview proxy host.
      allowedHosts: true,
    },
    preview: {
      host: "0.0.0.0",
      port: 4173,
      strictPort: true,
      allowedHosts: true,
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            react: ["react", "react-dom"],
            motion: ["motion"],
            fluid: ["fluidkit"],
            glin: ["@glinui/ui"],
          },
        },
      },
    },
    test: {
      environment: "happy-dom",
      setupFiles: ["./src/test/setup.ts"],
      server: {
        deps: {
          inline: ["@glinui/ui"],
        },
      },
    },
  };
});
