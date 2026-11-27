import { useEffect, useRef, useState } from "react";
import {
  motion,
  useReducedMotion,
  useScroll,
  useTransform,
  type MotionValue,
} from "motion/react";
import { PhoneMock, type PhoneMockVariant } from "../components/PhoneMock";

/** Small matchMedia hook (motion/react 12.43 doesn't export useMediaQuery). */
function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(
    () => window.matchMedia(query).matches,
  );
  useEffect(() => {
    const mql = window.matchMedia(query);
    const onChange = () => setMatches(mql.matches);
    onChange();
    mql.addEventListener("change", onChange);
    return () => mql.removeEventListener("change", onChange);
  }, [query]);
  return matches;
}

const SCREENS: {
  variant: PhoneMockVariant;
  label: string;
  caption: string;
}[] = [
  { variant: "home", label: "Today", caption: "Log a sip, watch the goal fill." },
  { variant: "add", label: "Add", caption: "Pick from 24+ real drinks." },
  { variant: "friends", label: "Friends", caption: "Meet Dippy — rename, add, care." },
];

function PinnedPhone({ i, progress }: { i: number; progress: MotionValue<number> }) {
  const screen = SCREENS[i];
  const x = useTransform(progress, [0, 1], [46 * i, 46 * i - 92]);
  const xPct = useTransform(x, (v) => `${v}%`);
  const scale = useTransform(x, [-92, -46, 0, 46, 92], [0.82, 0.9, 1, 0.9, 0.82]);
  const opacity = useTransform(x, [-92, -46, 0, 46, 92], [0.25, 0.55, 1, 0.55, 0.25]);
  return (
    <motion.div
      className="showcase-phone"
      style={{ x: xPct, scale, opacity }}
      aria-hidden={i !== 1}
    >
      <PhoneMock variant={screen.variant} />
      <div className="showcase-caption">
        <b>{screen.label}</b>
        <span>{screen.caption}</span>
      </div>
    </motion.div>
  );
}

/**
 * App showcase — scroll-pinned (300vh): three phone screens (built from real
 * Sippy glass components) transition as you scroll, reference-5 style.
 * Reduced motion (or narrow screens) → a static stacked layout.
 */
export function Showcase() {
  const ref = useRef<HTMLElement>(null);
  const reduced = useReducedMotion() ?? false;
  const narrow = useMediaQuery("(max-width: 760px)");
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end end"],
  });
  const staticLayout = reduced || narrow;

  return (
    <section id="showcase" ref={ref} className={`showcase${staticLayout ? " showcase-static" : ""}`}>
      <div className="showcase-pin">
        <h2>Three screens. One habit.</h2>
        <div className="showcase-stage">
          {staticLayout ? (
            <div className="showcase-row">
              {SCREENS.map((s) => (
                <div className="showcase-phone" key={s.variant}>
                  <PhoneMock variant={s.variant} />
                  <div className="showcase-caption">
                    <b>{s.label}</b>
                    <span>{s.caption}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            SCREENS.map((_, i) => (
              <PinnedPhone key={i} i={i} progress={scrollYProgress} />
            ))
          )}
        </div>
      </div>
    </section>
  );
}
