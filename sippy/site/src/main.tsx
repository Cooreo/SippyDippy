import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { MotionConfig } from "motion/react";
import "./styles/index.css";
import { App } from "./App";
import { ThemeProvider } from "./lib/theme";
import { EffectsProvider } from "./lib/effects";
import { setLenis } from "./lib/lenis";
import {
  QualityProvider,
  canHoverFine,
  isReducedMotionActive,
  resolveQualityTier,
  syncDocHiddenDataset,
  syncQualityDataset,
} from "./lib/quality";

// Stamp quality tier + visibility datasets immediately before first render.
const initialTier = syncQualityDataset();
syncDocHiddenDataset();

// Smooth scroll (Lenis) — enabled ONLY on desktop high-tier with fine hover and
// reduced motion off. Touch / low-tier devices keep native browser scrolling,
// and on desktop the rAF loop sleeps whenever scrolling settles or the tab is
// hidden.
if (
  typeof window !== "undefined" &&
  initialTier === "high" &&
  canHoverFine() &&
  !isReducedMotionActive()
) {
  void import("lenis").then(({ default: Lenis }) => {
    if (resolveQualityTier() === "low" || isReducedMotionActive()) return;
    const lenis = new Lenis({ lerp: 0.1 });
    let rafId = 0;
    let running = false;
    let wakeUntil = 0;

    const tick = (time: number) => {
      if (document.hidden || isReducedMotionActive() || resolveQualityTier() === "low") {
        running = false;
        rafId = 0;
        return;
      }
      lenis.raf(time);
      const active =
        lenis.isScrolling ||
        Math.abs(lenis.velocity) > 0.0005 ||
        time < wakeUntil;
      if (active) {
        rafId = requestAnimationFrame(tick);
      } else {
        running = false;
        rafId = 0;
      }
    };

    const wake = () => {
      if (document.hidden || isReducedMotionActive() || resolveQualityTier() === "low") {
        return;
      }
      wakeUntil = performance.now() + 1400;
      if (!running) {
        running = true;
        rafId = requestAnimationFrame(tick);
      }
    };

    setLenis(lenis, wake);
    window.addEventListener("wheel", wake, { passive: true });
    window.addEventListener("keydown", wake, { passive: true });
    document.addEventListener("visibilitychange", () => {
      if (document.hidden && rafId) {
        cancelAnimationFrame(rafId);
        running = false;
        rafId = 0;
      }
    });
  });
}

const rootEl = document.getElementById("root");
if (!rootEl) throw new Error("Root element #root missing in index.html");

createRoot(rootEl).render(
  <StrictMode>
    <MotionConfig>
      <QualityProvider>
        <ThemeProvider>
          <EffectsProvider>
            <App />
          </EffectsProvider>
        </ThemeProvider>
      </QualityProvider>
    </MotionConfig>
  </StrictMode>,
);
