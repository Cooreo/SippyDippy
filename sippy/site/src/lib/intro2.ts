/**
 * Surface-break intro — timeline, geometry, colour invariants, and capability
 * decisions. Pure + unit-tested (no WebGL required).
 *
 * Storyboard (total 3.85 s <= 4.5 s hard cap):
 *   0.0–0.4   darkest backdrop tone (#060a14, never #000), faint caustic
 *             flicker, first bubbles at bottom edge
 *   0.4–1.6   rise: light ramps up, scene eases scale 1.12→1.0, light shafts
 *             sweep, bubbles stream upward
 *   1.6–2.5   four glossy droplets drift in from the edges and merge into
 *             the Sippy mark at viewport centre; stroke draws as it forms
 *   2.5–3.0   surface break: bright ring erupts from the mark's centre; a
 *             circular hole grows from that exact centre to cover the
 *             viewport, revealing the live <Backdrop /> underneath; Dippy
 *             pops up from the hole's edge at ~2.75 s and starts waving
 *   3.0–3.8   800 ms exit crossfade: overlay opacity ramps smoothly 1 → 0
 *             while the mark FLIPs into the nav brand slot, Dippy waves at
 *             the hole edge, and hero words rise blur-to-sharp
 *   3.8–3.85  overlay sits at opacity = 0 for a clean painted frame before
 *             unmounting (zero cut, zero backdrop colour shift)
 */
export const INTRO = {
  /** 0.0–0.4 s — darkest backdrop tone (#060a14), first bubbles */
  darkEnd: 0.4,
  /** 0.4–1.6 s — rise (light ramp + scene scale 1.12→1.0) */
  riseEnd: 1.6,
  /** 1.6–2.5 s — droplet merge + mark stroke-draw */
  mergeEnd: 2.5,
  /** 2.5–3.0 s — surface break: ring + reveal hole opens across viewport */
  breakEnd: 3.0,
  /** 3.0–3.8 s — 800 ms overlay crossfade to 0 + mark FLIP + Dippy + hero words */
  flyEnd: 3.8,
  /** 3.85 s — unmount guard after opacity has reached 0 */
  done: 3.85,
} as const;

export const INTRO_CROSSFADE_MS = Math.round(
  (INTRO.flyEnd - INTRO.breakEnd) * 1000,
); // 800 ms
export const INTRO_SKIP_FADE_MS = 150;
export const INTRO_REDUCED_FADE_MS = 500;
export const INTRO_TOTAL_MS = Math.round(INTRO.done * 1000); // 3850 ms

/** The hard cap from the brief. */
export const INTRO_MAX_MS = 4500;

export interface Rgb01 {
  r: number;
  g: number;
  b: number;
}

/**
 * The page's darkest backdrop tone (`--bg-0` / `.dark --backdrop-base` = #060a14).
 * Strictly non-black (every channel > 0); WebGL clears to this tone at alpha = 1.
 */
export const DARKEST_BACKDROP_HEX = "#060a14";
export const DARKEST_BACKDROP_RGB: Rgb01 = {
  r: 6 / 255,
  g: 10 / 255,
  b: 20 / 255,
};

/* — easing — */
export function easeOutCubic(p: number): number {
  return 1 - Math.pow(1 - p, 3);
}
export function easeInOutCubic(p: number): number {
  return p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2;
}
export function clamp01(p: number): number {
  return p < 0 ? 0 : p > 1 ? 1 : p;
}

/**
 * Overlay opacity across the timeline:
 *  - 1.0 from 0.0 s to 3.0 s (`INTRO.breakEnd`)
 *  - smooth 800 ms crossfade to 0.0 by 3.8 s (`INTRO.flyEnd`), weighted so
 *    Dippy's wave at 3.0–3.5 s stays lively before settling smoothly to 0
 *  - 0.0 from 3.8 s onward (including `INTRO.done` unmount)
 */
export function overlayOpacity(t: number): number {
  if (t <= INTRO.breakEnd) return 1;
  if (t >= INTRO.flyEnd) return 0;
  const p = clamp01((t - INTRO.breakEnd) / (INTRO.flyEnd - INTRO.breakEnd));
  return Number((1 - Math.pow(p, 1.55)).toFixed(4));
}

export function parseCssColorToRgb01(raw: string): Rgb01 | null {
  const s = raw.trim();
  const hex6 = /^#([0-9a-f]{6})$/i.exec(s);
  if (hex6) {
    const n = parseInt(hex6[1], 16);
    return {
      r: ((n >> 16) & 255) / 255,
      g: ((n >> 8) & 255) / 255,
      b: (n & 255) / 255,
    };
  }
  const rgb = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i.exec(s);
  if (rgb) {
    return {
      r: Number(rgb[1]) / 255,
      g: Number(rgb[2]) / 255,
      b: Number(rgb[3]) / 255,
    };
  }
  return null;
}

/** Reads the live `--backdrop-base` colour from `:root` / `.dark`, defaulting to `#060a14`. */
export function readPageBackdropRgb(doc?: Document): Rgb01 {
  const d = doc ?? (typeof document !== "undefined" ? document : undefined);
  if (!d) return DARKEST_BACKDROP_RGB;
  const raw = getComputedStyle(d.documentElement).getPropertyValue(
    "--backdrop-base",
  );
  return parseCssColorToRgb01(raw) ?? DARKEST_BACKDROP_RGB;
}

/**
 * Pure compositor of the intro overlay's base layer over the frame-0 page
 * backdrop at time `t` (at the viewport centre).
 *  - Before 2.5 s: opaque darkest backdrop tone (`DARKEST_BACKDROP_RGB`, never `#000`).
 *  - By 3.0 s (`holeOpenFraction === 1`) and 3.8 s (`overlayOpacity === 0`):
 *    alpha is 0, so the visible colour equals `pageBackdrop` identically.
 */
export function compositeIntroFrameColor(
  t: number,
  vp: Viewport,
  pageBackdrop: Rgb01,
): Rgb01 {
  const sceneAlpha = (1 - holeOpenFraction(t, vp)) * overlayOpacity(t);
  return {
    r: DARKEST_BACKDROP_RGB.r * sceneAlpha + pageBackdrop.r * (1 - sceneAlpha),
    g: DARKEST_BACKDROP_RGB.g * sceneAlpha + pageBackdrop.g * (1 - sceneAlpha),
    b: DARKEST_BACKDROP_RGB.b * sceneAlpha + pageBackdrop.b * (1 - sceneAlpha),
  };
}

/* — geometry — */
export interface Viewport {
  w: number;
  h: number;
}

/**
 * The mark (and therefore the reveal) is centred on the viewport — expressed
 * as one function so the shader, the DOM mark, the hole, and the tests never
 * disagree.
 */
export function markCentre(v: Viewport): { x: number; y: number } {
  return { x: v.w / 2, y: v.h / 2 };
}

/** Distance from the mark centre to the FARTHEST viewport corner. */
export function maxRevealRadius(v: Viewport): number {
  const c = markCentre(v);
  return Math.hypot(Math.max(c.x, v.w - c.x), Math.max(c.y, v.h - c.y));
}

/** Centre of the reveal circle at time t — always the mark centre. */
export function revealCentre(t: number, v: Viewport): { x: number; y: number } {
  void t;
  return markCentre(v);
}

/**
 * Reveal radius in px: 0 until the surface break (2.5 s), then eases out
 * to cover the entire viewport by 3.0 s and stays open through crossfade.
 */
export function revealRadius(t: number, v: Viewport): number {
  if (t <= INTRO.mergeEnd) return 0;
  const p = clamp01((t - INTRO.mergeEnd) / (INTRO.breakEnd - INTRO.mergeEnd));
  return maxRevealRadius(v) * easeOutCubic(p);
}

/** 0..1 — how "open" the hole is (drives the >80% pointer-events rule). */
export function holeOpenFraction(t: number, v: Viewport): number {
  const max = maxRevealRadius(v);
  return max === 0 ? 0 : revealRadius(t, v) / max;
}

/* — capability / renderer decision — */
export type IntroRenderer = "shader" | "css" | "none";

/**
 * Which renderer plays the timeline:
 *  - reduced motion → "none" (500 ms mark-to-page DOM crossfade, NO canvas,
 *    so no WebGL context is ever created)
 *  - WebGL + backdrop texture ready within the 300 ms window → "shader"
 *  - otherwise → "css" (same timeline with CSS gradients/ring, no black frame)
 */
export function introRenderer(opts: {
  reducedMotion: boolean;
  webgl: boolean;
  textureReady: boolean;
}): IntroRenderer {
  if (opts.reducedMotion) return "none";
  return opts.webgl && opts.textureReady ? "shader" : "css";
}
