import { lazy, Suspense } from "react";
import { MotionConfig, useReducedMotion } from "motion/react";
import { useRoute } from "./lib/router";
import { useEffects } from "./lib/effects";
import { AuroraBackdrop } from "./components/AuroraBackdrop";
import { RefractionDefs } from "./components/RefractionDefs";
import { HomePage } from "./pages/HomePage";

// The gallery is heavy (full component matrix) — lazy-load it so the home
// page's LCP stays fast.
const GalleryPage = lazy(
  () =>
    import("./pages/GalleryPage").then((m) => ({ default: m.GalleryPage })),
);

export function App() {
  const route = useRoute();
  const osReducedMotion = useReducedMotion() ?? false;
  const { reduceEffects } = useEffects();
  // MotionConfig.reduced collapses every motion/react animation to an
  // immediate state — the reduced-motion contract for all spring animations.
  // It reacts to the OS setting AND to the gallery's "reduce effects" switch.
  const reduced = osReducedMotion || reduceEffects;

  return (
    <MotionConfig reducedMotion={reduced ? "always" : "user"}>
      <div className="app-shell">
        <AuroraBackdrop />
        <RefractionDefs />
        {route === "/gallery" ? (
          <Suspense fallback={null}>
            <GalleryPage />
          </Suspense>
        ) : (
          <HomePage />
        )}
      </div>
    </MotionConfig>
  );
}
