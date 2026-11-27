import { getLenis } from "./lenis";

/**
 * In-page scrolling + landing feedback (root-cause R2-2).
 *
 *  - scroll position is owned by Lenis when it exists (single owner);
 *    native scrollIntoView is the fallback (and the ONLY path under
 *    reduced motion, where Lenis is not even constructed)
 *  - targets carry `scroll-margin-top` (CSS) so the fixed nav pill never
 *    covers the landing point
 *  - after landing, focus moves to the target heading (tabindex=-1,
 *    preventScroll) and the card rim flashes once (~1.2 s, opacity only)
 */

/** Fixed nav strip: 16px top + ~56px pill. */
export const NAV_OFFSET_PX = 88;

export function prefersReducedMotion(): boolean {
  return (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

export function scrollToId(id: string): void {
  const el = document.getElementById(id);
  if (!el) return;
  const lenis = getLenis();
  const reduced = prefersReducedMotion();
  if (lenis && !reduced) {
    lenis.scrollTo(el, { offset: -NAV_OFFSET_PX, duration: 1.1 });
  } else {
    // instant under reduced motion; smooth native otherwise (no Lenis)
    el.scrollIntoView({ behavior: reduced ? "auto" : "smooth", block: "start" });
  }
}

/** Move focus to a heading without disturbing the scroll position. */
export function focusHeading(id: string): void {
  const el = document.getElementById(id);
  if (el) el.focus({ preventScroll: true });
}

/**
 * Flash a card's rim once (~1.2 s). Adds `.rim-flash` (an opacity-only
 * overlay animation) and removes it when the animation ends.
 */
export function flashRim(el: HTMLElement | null, ms = 1200): void {
  if (!el) return;
  el.classList.remove("rim-flash");
  // force reflow so the animation can restart on rapid re-clicks
  void el.offsetWidth;
  el.classList.add("rim-flash");
  window.setTimeout(() => el.classList.remove("rim-flash"), ms);
}
