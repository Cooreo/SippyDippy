export const ORB_RISE_MS = 4500; // 0 → 100 % (~4.5 s, slows near the top)
export const ORB_HOLD_MS = 1500; // hold at 100 % ("Goal reached" + ~600 ms splash)
export const ORB_DRAIN_MS = 1100; // gentle slosh + drain back to 0 %
export const ORB_CYCLE_MS = ORB_RISE_MS + ORB_HOLD_MS + ORB_DRAIN_MS;

/** Decelerating rise curve: brisk start, slows smoothly near 100 %. */
export function easeOrbRise(x: number): number {
  const c = Math.max(0, Math.min(1, x));
  return 1 - Math.pow(1 - c, 2.8);
}

export function easeOrbDrain(x: number): number {
  const c = Math.max(0, Math.min(1, x));
  return c < 0.5 ? 4 * c * c * c : 1 - Math.pow(-2 * c + 2, 3) / 2;
}

/** Deterministic ≤ 8 bubbles rising inside the water (transform + opacity only). */
export const ORB_BUBBLES = [
  { x: 15, size: 7, dur: 3.1, delay: -0.4, drift: 8 },
  { x: 26, size: 5, dur: 2.6, delay: -1.7, drift: -6 },
  { x: 36, size: 9, dur: 3.8, delay: -2.5, drift: 10 },
  { x: 47, size: 6, dur: 2.9, delay: -0.9, drift: -7 },
  { x: 58, size: 10, dur: 4.1, delay: -3.1, drift: 9 },
  { x: 68, size: 6, dur: 2.7, delay: -1.3, drift: -8 },
  { x: 77, size: 8, dur: 3.5, delay: -2.2, drift: 7 },
  { x: 86, size: 5, dur: 3.0, delay: -0.6, drift: -5 },
] as const;
