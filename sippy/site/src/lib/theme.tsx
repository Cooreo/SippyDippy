/**
 * Light/dark theme. Dark is the hero theme; light is a first-class theme.
 * Persists the explicit choice in localStorage; "system" follows the OS.
 * Applies the `.dark` class on <html> (both the Sippy tokens and the Glin
 * token overrides in styles/index.css key off it).
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

type Mode = "light" | "dark";

interface ThemeContextValue {
  mode: Mode;
  system: boolean;
  /** Flip to the opposite of the currently rendered mode. */
  toggle: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const STORAGE_KEY = "sippy-theme";

function readStored(): Mode | null {
  try {
    const v = window.localStorage.getItem(STORAGE_KEY);
    return v === "light" || v === "dark" ? v : null;
  } catch {
    return null;
  }
}

function systemPrefersDark(): boolean {
  return (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-color-scheme: dark)").matches
  );
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<Mode | null>(() =>
    typeof window === "undefined" ? null : readStored(),
  );
  const [sysDark, setSysDark] = useState<boolean>(() => systemPrefersDark());

  useEffect(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function")
      return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => setSysDark(mq.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);

  const mode: Mode = stored ?? (sysDark ? "dark" : "light");

  useEffect(() => {
    document.documentElement.classList.toggle("dark", mode === "dark");
  }, [mode]);

  const toggle = useCallback(() => {
    const next: Mode = mode === "dark" ? "light" : "dark";
    setStored(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* private mode — theme just won't persist */
    }
  }, [mode]);

  const value = useMemo<ThemeContextValue>(
    () => ({ mode, system: stored === null, toggle }),
    [mode, stored, toggle],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside <ThemeProvider>");
  return ctx;
}
