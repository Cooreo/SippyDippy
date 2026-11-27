import { useReducedMotion } from "motion/react";
import { useEffects } from "../lib/effects";
import { asset } from "../lib/router";
import { AuroraBackdrop } from "./AuroraBackdrop";
import { AmbientDroplets } from "./AmbientDroplets";

/**
 * The ONE backdrop layer (root-cause fix A).
 *
 * A single fixed, full-viewport element mounted as the FIRST child of #root
 * — outside `.app-shell` and every other ancestor, so no transform/filter/
 * will-change anywhere in the content tree can re-anchor or clip it.
 * Inside, in order:
 *   1. base colour (the darkest backdrop tone — mirrored on html/body)
 *   2. hero caustics art (feathered 400px+ ramp out — no hard edge)
 *   3. aurora blobs (transparent canvas, screen-composited)
 *   4. ambient droplets (≤12, pointer parallax, paused offscreen; absent
 *      under reduced motion / reduced effects)
 *   5. the dark hero scrim (one smooth 380px-fade ramp — no mid-plateau)
 *
 * Every section stays TRANSPARENT on top; section art (when present) is a
 * feathered, soft layer with 200px+ edge fades — so there is exactly one
 * colour system and no hard horizontal seam can form.
 */
export function Backdrop() {
  const reduced = useReducedMotion() ?? false;
  const { reduceEffects } = useEffects();
  const ambient = !reduced && !reduceEffects;

  return (
    <div className="backdrop" aria-hidden="true">
      <div
        className="backdrop-caustics"
        style={{ backgroundImage: `url(${asset("assets/hero-caustics.webp")})` }}
      />
      <AuroraBackdrop />
      {ambient && <AmbientDroplets count={12} />}
      <div className="backdrop-scrim" />
    </div>
  );
}
