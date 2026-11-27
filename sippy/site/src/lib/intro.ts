/**
 * Intro gating — pure and unit-tested.
 *
 * Modes:
 *  "play"  — full 5-beat cinematic intro (~3.9 s, skippable)
 *  "skip"  — no intro (already seen this session, or ?intro=0)
 *  "fade"  — prefers-reduced-motion: a single simple fade instead of the show
 *
 * Precedence (documented + tested):
 *  1. ?intro=1 forces play, ?intro=0 forces skip (test flags — win over
 *     everything, including reduced motion, since they exist for testing)
 *  2. prefers-reduced-motion → fade
 *  3. sessionStorage (once per session) → skip, else play
 */
export type IntroMode = "play" | "skip" | "fade";

export const INTRO_SESSION_KEY = "sippy-intro-played";

export function introForcedFromQuery(search: string): IntroMode | null {
  const v = new URLSearchParams(search).get("intro");
  if (v === "1") return "play";
  if (v === "0") return "skip";
  return null;
}

export function getIntroMode(opts: {
  search: string;
  reducedMotion: boolean;
  sessionHasPlayed: boolean;
}): IntroMode {
  const forced = introForcedFromQuery(opts.search);
  if (forced) return forced;
  if (opts.reducedMotion) return "fade";
  return opts.sessionHasPlayed ? "skip" : "play";
}

export function sessionHasPlayed(): boolean {
  try {
    return window.sessionStorage.getItem(INTRO_SESSION_KEY) === "1";
  } catch {
    return false;
  }
}

export function markIntroPlayed(): void {
  try {
    window.sessionStorage.setItem(INTRO_SESSION_KEY, "1");
  } catch {
    /* private mode — intro will simply play each session */
  }
}
