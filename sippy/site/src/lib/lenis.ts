import type Lenis from "lenis";

/**
 * Single owner of smooth scrolling (root-cause R2-2).
 *
 * Lenis runs ONLY on the desktop `"high"` quality tier where fine hover exists
 * and reduced motion is off. Touch / low-tier devices use native browser
 * scrolling (`instance === null`), and on desktop the rAF driver sleeps when
 * scrolling settles or when `document.hidden` is true.
 */
let instance: Lenis | null = null;
let wakeCallback: (() => void) | null = null;

export function setLenis(lenis: Lenis | null, wake?: () => void): void {
  instance = lenis;
  wakeCallback = wake ?? null;
}

export function getLenis(): Lenis | null {
  return instance;
}

export function wakeLenis(): void {
  wakeCallback?.();
}
