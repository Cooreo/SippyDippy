import { useCallback, useState } from "react";
import { useReducedMotion } from "motion/react";
import { GlassNav, type GlassNavSection } from "../components/glass/GlassNav";
import { SurfaceBreakIntro } from "../components/intro/SurfaceBreakIntro";
import { CtaButton } from "../components/CtaButton";
import { Hero } from "../sections/Hero";
import { Features } from "../sections/Features";
import { Showcase } from "../sections/Showcase";
import { HowItWorks } from "../sections/HowItWorks";
import { Download } from "../sections/Download";
import { Faq } from "../sections/Faq";
import { Footer } from "../sections/Footer";
import {
  getIntroMode,
  markIntroPlayed,
  sessionHasPlayed,
  type IntroMode,
} from "../lib/intro";
import { debugPerfRequested } from "../lib/debug";

const NAV_SECTIONS: GlassNavSection[] = [
  { id: "features", label: "Features" },
  { id: "showcase", label: "App" },
  { id: "how", label: "How" },
  { id: "download", label: "Download" },
];

/** Nav fades in while the mark flies into its slot (intro beat 3.0–3.8). */
const NAV_DELAY_INTRO = 3000;
const NAV_DELAY_NORMAL = 200;

function isReducedMotionPreferred(): boolean {
  if (
    typeof document !== "undefined" &&
    document.documentElement.hasAttribute("data-emulate-motion")
  ) {
    return true;
  }
  if (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function"
  ) {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }
  return false;
}

/**
 * The full marketing page (Phase 3, round 3).
 *
 * Intro gating:
 *  - once per session via sessionStorage (`sippy-intro-played`)
 *  - forced with `?intro=1` or skipped with `?intro=0`
 *  - never on `/#/gallery` (separate route in App.tsx)
 *  - reduced motion → `SurfaceBreakIntro` plays a 500 ms mark-to-page
 *    crossfade over `--backdrop-base` (no `<canvas>`, no WebGL, no ring,
 *    no black frame)
 */
export function HomePage() {
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionPreferred();
  const [introMode] = useState<IntroMode>(() =>
    getIntroMode({
      search: window.location.search,
      reducedMotion: reduced,
      sessionHasPlayed: sessionHasPlayed(),
    }),
  );
  const effective: IntroMode =
    reduced && introMode === "play" ? "fade" : introMode;
  const playingFull = effective === "play";
  const showOverlay = effective === "play" || effective === "fade";

  const [introActive, setIntroActive] = useState(showOverlay);
  const [introCover, setIntroCover] = useState(playingFull);
  // Critical R3-4 fix: when `!playingFull` (`skip` or `fade`), hero words must
  // start visible (`true`) rather than waiting for a full-intro reveal!
  const [wordsPlay, setWordsPlay] = useState(!playingFull);
  const [wordsFast, setWordsFast] = useState(effective !== "play");
  const [navDelay, setNavDelay] = useState(
    playingFull ? NAV_DELAY_INTRO : NAV_DELAY_NORMAL,
  );

  const onReveal = useCallback(() => {
    setIntroCover(false);
    setWordsPlay(true);
  }, []);
  const onSkip = useCallback(() => {
    setNavDelay(0);
    setWordsFast(true);
    setWordsPlay(true);
    setIntroCover(false);
  }, []);
  const onDone = useCallback(() => {
    markIntroPlayed();
    setIntroActive(false);
  }, []);

  return (
    <div className={`page${introCover ? " intro-cover" : ""}`}>
      <header className="page-nav">
        <div className="wrap">
          <GlassNav
            sections={NAV_SECTIONS}
            enterDelay={navDelay}
            cta={<CtaButton apk size="sm" />}
          />
        </div>
      </header>
      <main>
        <Hero playWords={wordsPlay} wordsFast={wordsFast} />
        <Features />
        <Showcase />
        <HowItWorks />
        <Download />
        <Faq />
      </main>
      <Footer />
      {introActive && (
        <SurfaceBreakIntro
          onReveal={onReveal}
          onSkip={onSkip}
          onDone={onDone}
          perf={debugPerfRequested()}
        />
      )}
    </div>
  );
}
