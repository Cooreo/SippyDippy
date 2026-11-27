import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

export type QualityTier = "low" | "high";

interface NavigatorWithDeviceHints extends Navigator {
  deviceMemory?: number;
}

function queryMatches(q: string): boolean {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") {
    return false;
  }
  try {
    return window.matchMedia(q).matches;
  } catch {
    return false;
  }
}

/**
 * Parse explicit `?quality=low|high` override from `location.search` or hash
 * query (`#/?quality=low`).
 */
export function parseQualityOverride(
  search?: string,
  hash?: string,
): QualityTier | null {
  if (typeof window === "undefined" && search === undefined && hash === undefined) {
    return null;
  }
  const s = search ?? window.location.search;
  const h = hash ?? window.location.hash;
  const check = (raw: string): QualityTier | null => {
    const v = new URLSearchParams(raw).get("quality");
    if (v === "low" || v === "high") return v;
    return null;
  };
  const fromSearch = check(s);
  if (fromSearch) return fromSearch;
  const qIdx = h.indexOf("?");
  if (qIdx >= 0) return check(h.slice(qIdx));
  return null;
}

/**
 * Reduced motion always wins over quality tier — true when OS
 * `prefers-reduced-motion: reduce` matches or `[data-emulate-motion]` is set.
 */
export function isReducedMotionActive(): boolean {
  if (
    typeof document !== "undefined" &&
    document.documentElement.hasAttribute("data-emulate-motion")
  ) {
    return true;
  }
  return queryMatches("(prefers-reduced-motion: reduce)");
}

/**
 * True when the primary input is touch-only (no hover capability or coarse
 * pointer without a fine-hover companion).
 */
export function isTouchOnlyDevice(): boolean {
  const hoverNone = queryMatches("(hover: none)");
  if (hoverNone) return true;
  const coarse = queryMatches("(pointer: coarse)");
  const fineHover = queryMatches("(hover: hover) and (pointer: fine)");
  return coarse && !fineHover;
}

/**
 * True when fine hover exists (`(hover: hover) and (pointer: fine)`), or in
 * headless test environments that expose no touch-only signals.
 */
export function canHoverFine(): boolean {
  if (queryMatches("(hover: hover) and (pointer: fine)")) return true;
  if (isTouchOnlyDevice()) return false;
  // In happy-dom / jsdom default matchMedia stubs all return false; treat
  // desktop-width non-touch environments as hover-capable unless <768px.
  if (typeof window !== "undefined" && window.innerWidth > 0 && window.innerWidth < 768) {
    return false;
  }
  return !queryMatches("(pointer: none)");
}

/**
 * Step 1a — Quality tier heuristic:
 *  - `?quality=low|high` overrides.
 *  - `"low"` when touch-only, viewport `< 768px`, low device memory (`<= 4 GB`),
 *    or few CPU cores (`<= 4`).
 *  - `"high"` otherwise.
 */
export function resolveQualityTier(): QualityTier {
  const override = parseQualityOverride();
  if (override) return override;
  if (typeof window === "undefined") return "high";

  if (isTouchOnlyDevice()) return "low";
  if (window.innerWidth > 0 && window.innerWidth < 768) return "low";

  const nav = window.navigator as NavigatorWithDeviceHints | undefined;
  if (nav) {
    if (typeof nav.deviceMemory === "number" && nav.deviceMemory > 0 && nav.deviceMemory <= 4) {
      return "low";
    }
    if (
      typeof nav.hardwareConcurrency === "number" &&
      nav.hardwareConcurrency > 0 &&
      nav.hardwareConcurrency <= 4
    ) {
      return "low";
    }
  }
  return "high";
}

export function syncQualityDataset(tier: QualityTier = resolveQualityTier()): QualityTier {
  if (typeof document !== "undefined") {
    document.documentElement.dataset.quality = tier;
  }
  return tier;
}

export function syncDocHiddenDataset(): void {
  if (typeof document === "undefined") return;
  if (document.hidden) {
    document.documentElement.dataset.docHidden = "true";
  } else {
    delete document.documentElement.dataset.docHidden;
  }
}

const QualityContext = createContext<QualityTier | null>(null);

export function QualityProvider({ children }: { children: ReactNode }) {
  const [tier, setTier] = useState<QualityTier>(() => syncQualityDataset());

  useEffect(() => {
    const update = () => {
      const next = syncQualityDataset();
      setTier((prev) => (prev === next ? prev : next));
    };
    update();
    syncDocHiddenDataset();

    const onVisibility = () => syncDocHiddenDataset();
    window.addEventListener("resize", update, { passive: true });
    window.addEventListener("hashchange", update);
    window.addEventListener("popstate", update);
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      window.removeEventListener("resize", update);
      window.removeEventListener("hashchange", update);
      window.removeEventListener("popstate", update);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return <QualityContext.Provider value={tier}>{children}</QualityContext.Provider>;
}

/**
 * Returns the active quality tier (`"low"` or `"high"`). Works both inside and
 * outside `<QualityProvider>` (falling back to `resolveQualityTier()`).
 */
export function useQualityTier(): QualityTier {
  const ctx = useContext(QualityContext);
  return ctx ?? resolveQualityTier();
}
