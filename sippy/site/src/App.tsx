import { lazy, Suspense, useEffect } from "react";
import { MotionConfig, useReducedMotion } from "motion/react";
import { debugOutlineRequested, enableDebugOutline } from "./lib/debug";
import { useRoute } from "./lib/router";
import { useEffects } from "./lib/effects";
import { Backdrop } from "./components/Backdrop";
import { RefractionDefs } from "./components/RefractionDefs";
import { HomePage } from "./pages/HomePage";

// The gallery and asset sheet are heavy — lazy-load them so the home
// page's LCP stays fast.
const GalleryPage = lazy(
  () =>
    import("./pages/GalleryPage").then((m) => ({ default: m.GalleryPage })),
);
const AssetsPage = lazy(
  () => import("./pages/AssetsPage").then((m) => ({ default: m.AssetsPage })),
);

export function App() {
  const route = useRoute();
  const osReducedMotion = useReducedMotion() ?? false;
  const { reduceEffects } = useEffects();
  // MotionConfig.reducedMotion collapses every motion/react animation to an
  // immediate state — the reduced-motion contract for all spring animations.
  // It reacts to the OS setting AND to the gallery's "reduce effects" switch.
  const reduced = osReducedMotion || reduceEffects;

  // ?debug=outline — outline every element + highlight horizontal overflow
  useEffect(() => {
    if (!debugOutlineRequested()) return;
    return enableDebugOutline();
  }, []);

  return (
    <MotionConfig reducedMotion={reduced ? "always" : "user"}>
      {/* The backdrop is the FIRST child of #root — outside .app-shell,
          outside any transformed ancestor (see PROGRESS.md, root cause A). */}
      <Backdrop />
      <RefractionDefs />
      <div className="app-shell">
        {route === "/gallery" ? (
          <Suspense fallback={null}>
            <GalleryPage />
          </Suspense>
        ) : route === "/assets" ? (
          <Suspense fallback={null}>
            <AssetsPage />
          </Suspense>
        ) : (
          <HomePage />
        )}
      </div>
    </MotionConfig>
  );
}
