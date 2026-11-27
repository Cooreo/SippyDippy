/**
 * Wallpapers — the ONE painted layer of the fixed backdrop, per theme.
 *
 * Dark is the deep-night reef (the original hero asset). Light is a bright,
 * high-key daylight version of the same water: rays from the surface, pale
 * sand and caustics, so switching the theme visibly changes the "wallpaper"
 * instead of re-tinting the same photo.
 *
 * Every entry ships the same three widths (640 / 960 / 1376) so the browser
 * picks by `sizes="100vw"` exactly as it did before, and every URL goes
 * through `asset()` so the GitHub Pages base path keeps working.
 */
import { asset } from "./router";

export type WallpaperTheme = "dark" | "light";

export interface Wallpaper {
  theme: WallpaperTheme;
  /** Full-size 1376 x 768 variant. */
  src: string;
  /** `srcset` with the 640 / 960 / 1376 variants. */
  srcSet: string;
  width: number;
  height: number;
  /** CSS `object-position` — keeps the sun + rays in frame. */
  position: string;
}

const WIDTH = 1376;
const HEIGHT = 768;

function variant(theme: WallpaperTheme, stem: string, position: string): Wallpaper {
  const full = `${stem}.webp`;
  const mid = `${stem}-960.webp`;
  const small = `${stem}-640.webp`;
  return {
    theme,
    src: asset(`assets/${full}`),
    srcSet: `${asset(`assets/${small}`)} 640w, ${asset(`assets/${mid}`)} 960w, ${asset(`assets/${full}`)} 1376w`,
    width: WIDTH,
    height: HEIGHT,
    position,
  };
}

export const WALLPAPERS: Record<WallpaperTheme, Wallpaper> = {
  dark: variant("dark", "hero-caustics", "50% -16%"),
  light: variant("light", "hero-caustics-light", "50% -6%"),
};

export function wallpaperFor(theme: WallpaperTheme): Wallpaper {
  return WALLPAPERS[theme];
}

export function otherWallpaperTheme(theme: WallpaperTheme): WallpaperTheme {
  return theme === "dark" ? "light" : "dark";
}
