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
const NAV_DELAY_NORMAL = 250;

/**
 * The full marketing page (Phase 3, round 2).
 *
 * Intro gating: once per session via sessionStorage; forced with ?intro=1 /
 * ?intro=0; never on /#/gallery (that is a different route); reduced
 * motion → a ~350 ms crossfade and no Intro is rendered at all (so no
 * WebGL context is ever created). While the intro covers the page, the
 * hero is visually hidden but stays focusable (`.intro-cover`, opacity).
 *
 * The nav pill sits inside the same `.wrap` as the hero grid (R2-4):
 * one container, one inline padding — edges align by construction.
 */
export function HomePage() {
  const reduced = useReducedMotion() ?? false;
  const [introMode] = useState<IntroMode>(() =>
    getIntroMode({
      search: window.location.search,
      reducedMotion: reduced,
      sessionHasPlayed: sessionHasPlayed(),
    }),
  );
  // ?intro=1 under reduced motion: the intro renders NOTHING by contract
  // (RM = no shader, no timeline) — degrade to the 350 ms fade so the
  // page is never left covered
  const effective: IntroMode =
    reduced && introMode === "play" ? "fade" : introMode;
  const playing = effective === "play";

  const [introActive, setIntroActive] = useState(playing);
  const [introCover, setIntroCover] = useState(playing);
  const [wordsPlay, setWordsPlay] = useState(false);
  const [wordsFast, setWordsFast] = useState(false);
  const [navDelay, setNavDelay] = useState(playing ? NAV_DELAY_INTRO : NAV_DELAY_NORMAL);

  // stable callbacks — the intro's effect must not re-run mid-flight
  const onReveal = useCallback(() => {
    setIntroCover(false);
    setWordsPlay(true);
  }, []);
  const onSkip = useCallback(() => {
    // end state within 150 ms: collapse every delay to ~0
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
    <div
      className={`page${effective === "fade" ? " fade-in" : ""}${
        introCover ? " intro-cover" : ""
      }`}
    >
      <header className="page-nav">
        <div className="wrap">
          <GlassNav
            sections={NAV_SECTIONS}
            enterDelay={navDelay}
            cta={<CtaButton apk size="sm" arrow />}
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
