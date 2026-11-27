/**
 * Surface-break intro — timeline, geometry and capability decisions.
 * Pure + unit-tested (no DOM, no GL).
 *
 * Storyboard (target 4.2 s, hard cap 4.5 s):
 *   0.0–0.4  near-black, one faint caustic flicker, first bubbles
 *   0.4–1.6  rise: light ramps dark→bright, scene eases scale 1.12→1.0,
 *            light shafts sweep, bubbles stream upward
 *   1.6–2.5  four glossy droplets drift in from the edges and merge into
 *            the Sippy mark (gooey); the mark's stroke draws as it forms
 *   2.5–3.0  surface break: a bright ring erupts from the mark's centre,
 *            distorts the scene; a circular hole grows from that exact
 *            centre until it covers the viewport (page underneath)
 *   3.0–3.8  the mark FLIPs into the nav logo slot while the nav fades in;
 *            Dippy pops from the hole's edge and waves; the hero headline
 *            rises word by word, blur to sharp
 */
export const INTRO = {
  /** 0.0–0.4 s — near-black flicker, first bubbles at the bottom edge */
  darkEnd: 0.4,
  /** 0.4–1.6 s — rise (light ramp + scene scale 1.12→1.0) */
  riseEnd: 1.6,
  /** 1.6–2.5 s — droplet merge + mark stroke-draw (stroke starts at 1.85) */
  mergeEnd: 2.5,
  /** 2.5–3.0 s — surface break: ring + reveal hole */
  breakEnd: 3.0,
  /** 3.0–3.8 s — mark FLIP + nav + Dippy + headline words */
  flyEnd: 3.8,
  /** 3.8–4.2 s — settle/hold */
  done: 4.2,
} as const;

export const INTRO_TOTAL_MS = 4200;

/** The hard cap from the brief. */
export const INTRO_MAX_MS = 4500;

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

/* — geometry — */
export interface Viewport {
  w: number;
  h: number;
}

/**
 * The mark (and therefore the reveal) is centred on the viewport — the
 * same "logo target" rule as round 1, expressed as one function so the
 * shader, the DOM mark, the hole and the tests can never disagree.
 */
export function markCentre(v: Viewport): { x: number; y: number } {
  return { x: v.w / 2, y: v.h / 2 };
}

/** Distance from the mark centre to the FARTHEST viewport corner. */
export function maxRevealRadius(v: Viewport): number {
  const c = markCentre(v);
  return Math.hypot(
    Math.max(c.x, v.w - c.x),
    Math.max(c.y, v.h - c.y),
  );
}

/** Centre of the reveal circle at time t — always the mark centre. */
export function revealCentre(t: number, v: Viewport): { x: number; y: number } {
  void t;
  return markCentre(v);
}

/**
 * Reveal radius in px: 0 until the surface break (2.5 s), then eases out
 * to "covers the viewport" by 3.0 s and stays there.
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
 *  - reduced motion → "none" (a ~350 ms logo crossfade, NO canvas, so no
 *    WebGL context is ever created)
 *  - WebGL + backdrop texture ready within the 300 ms window → "shader"
 *  - otherwise → "css" (same timeline with gradients/blur/ring — the
 *    brief forbids a blank frame)
 */
export function introRenderer(opts: {
  reducedMotion: boolean;
  webgl: boolean;
  textureReady: boolean;
}): IntroRenderer {
  if (opts.reducedMotion) return "none";
  return opts.webgl && opts.textureReady ? "shader" : "css";
}
