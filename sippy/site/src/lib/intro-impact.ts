import type { CSSProperties } from "react";

export interface RectLike {
  left: number;
  top: number;
  width: number;
  height: number;
}
export interface ViewportLike {
  width: number;
  height: number;
}
export interface Point {
  x: number;
  y: number;
}

/**
 * The single source of truth for the intro's impact point (root-cause fix E).
 *
 * The droplet's falling tip, every ripple ring and the logo reveal all share
 * THIS ONE point — the centre of the logo target, in viewport coordinates.
 * Intro.tsx measures the logo's getBoundingClientRect once (and on resize),
 * publishes the point as CSS custom properties, and every animated element
 * is positioned from those variables. There are no hard-coded offsets and
 * no per-element origins, so nothing can drift out of alignment.
 *
 * Pure and deterministic → unit-testable at any viewport size.
 */
export function impactPoint(logoRect: RectLike, viewport: ViewportLike): Point {
  const x = logoRect.left + logoRect.width / 2;
  const y = logoRect.top + logoRect.height / 2;
  return {
    x: Math.min(Math.max(x, 0), viewport.width),
    y: Math.min(Math.max(y, 0), viewport.height),
  };
}

/** Where the falling droplet's TIP lands (identical to the impact point). */
export function dropTip(point: Point): Point {
  return { x: point.x, y: point.y };
}

/** Centre of ripple ring `i` — by construction the shared impact point. */
export function rippleCenter(point: Point, _i: number): Point {
  return { x: point.x, y: point.y };
}

/** CSS custom properties shared by droplet, ripples and reveal mask. */
export function impactVars(point: Point): CSSProperties {
  return {
    "--impact-x": `${point.x}px`,
    "--impact-y": `${point.y}px`,
  } as CSSProperties;
}

/* — Timing (total intro budget: 3600 ms, per brief) — */
export const DROP_DELAY_MS = 250;
export const DROP_FALL_MS = 550;
/** impact moment = end of the fall */
export const IMPACT_MS = DROP_DELAY_MS + DROP_FALL_MS;
export const RIPPLE_COUNT = 3;
export const RIPPLE_STAGGER_MS = 120;
export const RIPPLE_DURATION_MS = 700;
/** first ripple starts 60 ms after impact, so the splash reads first */
export const rippleStart = (i: number) =>
  IMPACT_MS + 60 + i * RIPPLE_STAGGER_MS;
/** the FIRST ripple reveals the logo (same moment) */
export const LOGO_REVEAL_MS = rippleStart(0);
/** the logo stroke then draws itself */
export const LOGO_DRAW_MS = LOGO_REVEAL_MS + 350;
export const LOGO_FILL_MS = LOGO_DRAW_MS + 550;
/** overlay lifts (matches the hero words' startDelay) */
export const LIFT_MS = 1900;
export const INTRO_TOTAL_MS = 3600;
