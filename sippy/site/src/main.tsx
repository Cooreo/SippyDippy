import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import Lenis from "lenis";
import { MotionConfig } from "motion/react";
import "./styles/index.css";
import { App } from "./App";
import { ThemeProvider } from "./lib/theme";
import { EffectsProvider } from "./lib/effects";
import { setLenis } from "./lib/lenis";

// Smooth scroll (Lenis) — disabled entirely under prefers-reduced-motion.
// The instance is registered in lib/lenis.ts so in-page scrolls (CTAs, nav
// links) have ONE owner of the scroll position (root-cause R2-2).
const prefersReducedMotion =
  typeof window !== "undefined" &&
  typeof window.matchMedia === "function" &&
  window.matchMedia("(prefers-reduced-motion: reduce)").matches;

if (!prefersReducedMotion) {
  const lenis = new Lenis({ lerp: 0.1 });
  setLenis(lenis);
  const raf = (time: number) => {
    lenis.raf(time);
    requestAnimationFrame(raf);
  };
  requestAnimationFrame(raf);
}

const rootEl = document.getElementById("root");
if (!rootEl) throw new Error("Root element #root missing in index.html");

createRoot(rootEl).render(
  <StrictMode>
    <MotionConfig>
      <ThemeProvider>
        <EffectsProvider>
          <App />
        </EffectsProvider>
      </ThemeProvider>
    </MotionConfig>
  </StrictMode>,
);
