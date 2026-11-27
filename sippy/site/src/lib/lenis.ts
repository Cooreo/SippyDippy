import type Lenis from "lenis";

/**
 * Single owner of smooth scrolling (root-cause R2-2).
 *
 * Lenis re-writes the window scroll position from its own virtual state every
 * frame; calling native `scrollIntoView({behavior:"smooth"})` while it runs
 * makes the two systems fight and the scroll disappears. So every in-page
 * scroll on the site goes through this one instance (see lib/scroll.ts) —
 * the instance is created in main.tsx and handed in here.
 */
let instance: Lenis | null = null;

export function setLenis(lenis: Lenis | null): void {
  instance = lenis;
}

export function getLenis(): Lenis | null {
  return instance;
}
