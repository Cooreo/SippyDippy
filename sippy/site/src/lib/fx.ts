/**
 * Refraction mode control.
 *
 * "full" = edge refraction enabled: SVG displacement filter inside
 *         backdrop-filter (my CSS glass) + fluidkit `refraction` props.
 *         Chromium-only capability — this is a progressive enhancement.
 * "blur" = clean fallback: backdrop blur + saturation only (Safari/Firefox).
 *
 * Sources of the mode, in priority order:
 *   1. Runtime override (setFxOverride) — used by the /gallery "reduce effects"
 *      switch so the fallback can be tested live.
 *   2. URL query `?fx=blur` / `?fx=full` — forced comparison mode.
 *   3. Feature detection — full only where the browser actually supports
 *      filter references inside backdrop-filter (Chromium).
 */
import { useSyncExternalStore } from "react";

export type FxMode = "full" | "blur";

let override: FxMode | null = null;
const subscribers = new Set<() => void>();

export function setFxOverride(mode: FxMode | null): void {
  override = mode;
  subscribers.forEach((fn) => fn());
}

export function fxFromQuery(search: string): FxMode | null {
  const p = new URLSearchParams(search).get("fx");
  return p === "blur" || p === "full" ? p : null;
}

/** True only in Chromium-class engines: filter refs inside backdrop-filter. */
export function supportsCssRefraction(): boolean {
  if (typeof CSS === "undefined" || typeof CSS.supports !== "function") return false;
  return (
    CSS.supports("backdrop-filter", "url(#x)") ||
    CSS.supports("-webkit-backdrop-filter", "url(#x)")
  );
}

export function getFxMode(): FxMode {
  if (override) return override;
  const fromQuery =
    typeof window !== "undefined" ? fxFromQuery(window.location.search) : null;
  if (fromQuery) return fromQuery;
  return supportsCssRefraction() ? "full" : "blur";
}

function subscribe(fn: () => void): () => void {
  subscribers.add(fn);
  return () => {
    subscribers.delete(fn);
  };
}

const snapshot = (): FxMode => getFxMode();

/** Re-render-safe access (components update when the gallery switch flips). */
export function useFxMode(): FxMode {
  return useSyncExternalStore(subscribe, snapshot, snapshot);
}
