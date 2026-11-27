import { useReducedMotion } from "motion/react";
import { useEffects } from "../lib/effects";
import { isReducedMotionActive, useQualityTier } from "../lib/quality";
import { asset } from "../lib/router";
import { AuroraBackdrop } from "./AuroraBackdrop";
import { AmbientDroplets } from "./AmbientDroplets";

/**
 * The ONE backdrop layer (root-cause fix A).
 *
 * A single fixed, full-viewport element mounted as the FIRST child of #root
 * — outside `.app-shell` and every other ancestor, so no transform/filter/
 * will-change anywhere in the content tree can re-anchor or clip it.
 */
export function Backdrop() {
  const osReduced = useReducedMotion() ?? false;
  const { reduceEffects } = useEffects();
  const tier = useQualityTier();
  const reduced = osReduced || isReducedMotionActive();
  const ambient = !reduced && !reduceEffects;

  return (
    <div className="backdrop" aria-hidden="true">
      <div className="backdrop-caustics">
        <img
          className="backdrop-caustics-img"
          src={asset("assets/hero-caustics.webp")}
          srcSet={`${asset("assets/hero-caustics-640.webp")} 640w, ${asset("assets/hero-caustics-960.webp")} 960w, ${asset("assets/hero-caustics.webp")} 1376w`}
          sizes="100vw"
          width={1376}
          height={768}
          alt=""
          fetchPriority="high"
          decoding="async"
        />
      </div>
      <AuroraBackdrop />
      {ambient && <AmbientDroplets count={tier === "low" ? 4 : 12} />}
      <div className="backdrop-scrim" />
    </div>
  );
}
