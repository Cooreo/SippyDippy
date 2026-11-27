/**
 * Sippy design tokens — JS mirror of the single source of truth:
 * `sippy/shared/tokens.md`. If a value changes there, it changes here AND in
 * the Compose app (Phase 4) — see tokens.md for the app impact notes.
 *
 * CSS side of the same tokens: `src/styles/index.css` (:root / .dark blocks).
 */

/** Radii in px — capsules and continuous-corner rects only. */
export const radius = {
  pill: 999,
  card: 28,
  inner: 20,
} as const;

/** Backdrop blur levels in px. */
export const blur = {
  sm: 24,
  md: 32,
  lg: 40,
} as const;

/** Backdrop saturation boost applied with every blur. */
export const glassSaturation = 165;

/**
 * Spring presets (stiffness / damping / mass).
 * motion/react: pass the object straight to `transition`.
 * Compose: spring(stiffness = ..., damping = ..., mass = ...).
 */
export const springs = {
  base: { stiffness: 300, damping: 30, mass: 1 },
  gentle: { stiffness: 220, damping: 24, mass: 1 },
  snappy: { stiffness: 500, damping: 38, mass: 1 },
  float: { stiffness: 120, damping: 16, mass: 1.2 },
} as const;

export type SpringPreset = keyof typeof springs;

/** Non-spring durations in ms (nothing > 600ms except intro/ambient). */
export const durations = {
  quick: 180,
  base: 300,
  slow: 450,
} as const;

/** Pressed glass scales to this, then springs back. */
export const pressScale = 0.97;
