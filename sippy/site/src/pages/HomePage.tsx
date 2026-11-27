import { useState } from "react";
import { useReducedMotion } from "motion/react";
import { GlassNav, type GlassNavSection } from "../components/glass/GlassNav";
import { Intro } from "../components/Intro";
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

const NAV_SECTIONS: GlassNavSection[] = [
  { id: "features", label: "Features" },
  { id: "showcase", label: "App" },
  { id: "how", label: "How" },
  { id: "download", label: "Download" },
];

const HERO_DELAY = 1900; // ms — hero words rise as the intro veil lifts
const NAV_DELAY = 3000; // ms — nav pill drops in (intro beat 5)

/**
 * The full marketing page (Phase 3). Intro gating: once per session via
 * sessionStorage, forced with ?intro=1 / ?intro=0, reduced-motion → simple
 * fade (no Intro rendered at all).
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
  const [introActive, setIntroActive] = useState(introMode === "play");
  const playing = introMode === "play";

  return (
    <div className={`page${introMode === "fade" ? " fade-in" : ""}`}>
      <header className="page-nav">
        <GlassNav
          sections={NAV_SECTIONS}
          enterDelay={playing ? NAV_DELAY : 250}
          onCtaClick={() =>
            document
              .getElementById("download")
              ?.scrollIntoView({ behavior: "smooth" })
          }
        />
      </header>
      <main>
        <Hero startDelay={playing ? HERO_DELAY : 0} />
        <Features />
        <Showcase />
        <HowItWorks />
        <Download />
        <Faq />
      </main>
      <Footer />
      {introActive && (
        <Intro
          onDone={() => {
            markIntroPlayed();
            setIntroActive(false);
          }}
        />
      )}
    </div>
  );
}
