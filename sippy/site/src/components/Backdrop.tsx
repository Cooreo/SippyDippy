import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";
import { useEffects } from "../lib/effects";
import { isReducedMotionActive, useQualityTier } from "../lib/quality";
import { useTheme } from "../lib/theme";
import { otherWallpaperTheme, wallpaperFor } from "../lib/wallpaper";
import { AuroraBackdrop } from "./AuroraBackdrop";
import { AmbientDroplets } from "./AmbientDroplets";

/**
 * The ONE backdrop layer (root-cause fix A).
 *
 * A single fixed, full-viewport element mounted as the FIRST child of #root
 * — outside `.app-shell` and every other ancestor, so no transform/filter/
 * will-change anywhere in the content tree can re-anchor or clip it.
 *
 * The painted wallpaper is theme-aware: dark = the deep-night reef, light =
 * the bright daylight water (see lib/wallpaper.ts). The inactive theme's
 * file is warmed after first paint on the high tier, so toggling the theme
 * swaps the image with no blank frame and no layout shift.
 */
export function Backdrop() {
  const osReduced = useReducedMotion() ?? false;
  const { reduceEffects } = useEffects();
  const tier = useQualityTier();
  const { mode } = useTheme();
  const reduced = osReduced || isReducedMotionActive();
  const ambient = !reduced && !reduceEffects;
  const wallpaper = wallpaperFor(mode);

  // Keep a reference so the speculative decode is not garbage-collected.
  const warmRef = useRef<HTMLImageElement | null>(null);

  useEffect(() => {
    if (tier !== "high") return;
    const other = wallpaperFor(otherWallpaperTheme(mode));
    const id = window.setTimeout(() => {
      if (document.hidden) return;
      const img = new Image();
      img.decoding = "async";
      img.srcset = other.srcSet;
      img.sizes = "100vw";
      img.src = other.src;
      warmRef.current = img;
    }, 2400);
    return () => window.clearTimeout(id);
  }, [mode, tier]);

  return (
    <div className="backdrop" aria-hidden="true" data-wallpaper={mode}>
      <div className="backdrop-caustics">
        {/* No `key`: the element is reused on a theme flip, so the browser
            keeps painting the old wallpaper until the new frame decodes. */}
        <img
          className="backdrop-caustics-img"
          data-wallpaper={mode}
          src={wallpaper.src}
          srcSet={wallpaper.srcSet}
          sizes="100vw"
          width={wallpaper.width}
          height={wallpaper.height}
          alt=""
          fetchPriority="high"
          decoding="async"
          style={{ objectPosition: wallpaper.position }}
        />
      </div>
      <AuroraBackdrop />
      {ambient && <AmbientDroplets count={tier === "low" ? 4 : 12} />}
      <div className="backdrop-scrim" />
    </div>
  );
}
