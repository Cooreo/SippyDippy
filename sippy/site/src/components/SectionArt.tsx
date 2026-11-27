import { asset } from "../lib/router";
import { hasAsset } from "../lib/assets";

/**
 * One feathered section art layer (Phase 3 richness pass). Absolutely
 * positioned inside its section, oversized so the 200px+ edge fade (mask)
 * always lands inside the art — no seam can form against the backdrop
 * (all pieces share one palette family). Skips itself when the asset
 * source image has not been generated yet (manifest lookup) — the section
 * then rests on the global backdrop gradients, which are the same palette.
 */
export function SectionArt({ name }: { name: string }) {
  if (!hasAsset(name)) return null;
  return (
    <div
      className="section-art"
      style={{ backgroundImage: `url(${asset(`assets/${name}.webp`)})` }}
      aria-hidden="true"
    />
  );
}
