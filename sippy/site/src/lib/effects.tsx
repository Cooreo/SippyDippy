/**
 * "Reduce effects" — a dev/testing switch (lives in /gallery) that emulates
 * both user preferences at once, so the degraded paths can be exercised
 * without changing OS settings:
 *
 *   data-emulate-motion     → mirrors `prefers-reduced-motion: reduce`
 *   data-emulate-transparent → mirrors `prefers-reduced-transparency: reduce`
 *
 * While active it also forces the blur-only refraction fallback via
 * setFxOverride("blur") (see lib/fx.ts), so you see the full Safari/Firefox
 * look plus the opaque-glass and still-motion paths together.
 *
 * The real OS preferences are always respected on their own — this switch
 * only adds the same rules via data attributes (CSS in styles/index.css).
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { setFxOverride } from "./fx";

interface EffectsContextValue {
  reduceEffects: boolean;
  setReduceEffects: (on: boolean) => void;
}

const EffectsContext = createContext<EffectsContextValue | null>(null);

export function EffectsProvider({ children }: { children: ReactNode }) {
  const [reduceEffects, setReduceEffectsState] = useState(false);

  const setReduceEffects = useCallback((on: boolean) => {
    setReduceEffectsState(on);
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.toggleAttribute("data-emulate-motion", reduceEffects);
    root.toggleAttribute("data-emulate-transparent", reduceEffects);
    setFxOverride(reduceEffects ? "blur" : null);
    return () => {
      root.removeAttribute("data-emulate-motion");
      root.removeAttribute("data-emulate-transparent");
      setFxOverride(null);
    };
  }, [reduceEffects]);

  const value = useMemo(
    () => ({ reduceEffects, setReduceEffects }),
    [reduceEffects, setReduceEffects],
  );

  return (
    <EffectsContext.Provider value={value}>
      {children}
    </EffectsContext.Provider>
  );
}

export function useEffects(): EffectsContextValue {
  const ctx = useContext(EffectsContext);
  if (!ctx) throw new Error("useEffects must be used inside <EffectsProvider>");
  return ctx;
}
