import { useRef, type PointerEvent } from "react";
import {
  motion,
  useMotionValue,
  useReducedMotion,
  useSpring,
  useTransform,
} from "motion/react";
import { CtaButton } from "../components/CtaButton";
import { PhoneMock } from "../components/PhoneMock";
import { asset } from "../lib/router";
import { springs } from "../lib/tokens";

const WORDS = ["Drink", "consistently.", "Joyfully."];

/**
 * Hero — two-column grid inside the shared 1280px wrap (same width as the
 * nav), vertically centered, full-viewport height.
 *
 * Round 2: the CTA row is the shared CtaButton (primary "See install
 * steps"/"Get the APK" + ghost "How it works"); the phone is the live
 * recreation (PhoneDevice + app components + demo loop). Words rise
 * blur-to-sharp when the intro reveals (playWords), fast under a skip.
 *
 * The hero is dark via the backdrop's soft scrim — it has NO section
 * background of its own.
 */
export function Hero({
  playWords,
  wordsFast,
}: {
  /** The intro has revealed the page — words rise now. */
  playWords: boolean;
  /** Skipped intro — finish the entrance fast (end state in ~150 ms). */
  wordsFast: boolean;
}) {
  const reduced = useReducedMotion();
  const visualRef = useRef<HTMLDivElement>(null);
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const rotateY = useSpring(useTransform(mx, [-0.5, 0.5], [-5.5, 5.5]), springs.gentle);
  const rotateX = useSpring(useTransform(my, [-0.5, 0.5], [4.5, -4.5]), springs.gentle);

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (reduced) return; // no parallax under reduced motion
    const el = visualRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    mx.set((e.clientX - r.left) / r.width - 0.5);
    my.set((e.clientY - r.top) / r.height - 0.5);
  };

  const wordDur = wordsFast ? 0.15 : 0.5;
  const wordStagger = wordsFast ? 0.02 : 0.09;

  return (
    <section id="hero" className="hero">
      <div className="wrap hero-inner">
        <div className="hero-copy">
          <h1 className="hero-title">
            {WORDS.map((w, i) => (
              <motion.span
                key={w}
                className="hero-word"
                initial={{ opacity: 0, y: 26, filter: "blur(10px)" }}
                animate={
                  playWords
                    ? { opacity: 1, y: 0, filter: "blur(0px)" }
                    : { opacity: 0 }
                }
                transition={{
                  duration: wordDur,
                  delay: i * wordStagger,
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                {w}
              </motion.span>
            ))}
          </h1>
          <motion.p
            className="lead"
            initial={{ opacity: 0, y: 16 }}
            animate={playWords ? { opacity: 1, y: 0 } : { opacity: 0 }}
            transition={{ delay: wordsFast ? 0.1 : 0.55, duration: wordDur }}
          >
            Sippy tracks water and every other drink you actually have — with
            friends who notice, streaks that keep you honest, and reminders
            that never nag.
          </motion.p>
          <motion.div
            className="hero-cta"
            initial={{ opacity: 0, y: 14 }}
            animate={playWords ? { opacity: 1, y: 0 } : { opacity: 0 }}
            transition={{ delay: wordsFast ? 0.12 : 0.75, duration: wordDur }}
          >
            {/* primary CTA — strongest element; label follows the APK rule */}
            <CtaButton apk size="lg" arrow />
            {/* ghost glass button, same hover behaviour */}
            <CtaButton
              variant="ghost"
              size="lg"
              label="How it works"
              target="how"
              focusId="how-heading"
            />
          </motion.div>
        </div>
        <div className="hero-visual" ref={visualRef} onPointerMove={onPointerMove}>
          {/* Dippy floating by the phone — gentle idle bob, never over text */}
          <motion.div
            className="hero-buddy-wrap"
            animate={reduced ? { y: 0 } : { y: [0, -7, 0] }}
            transition={{ duration: 5.2, repeat: Infinity, ease: "easeInOut", delay: 0.8 }}
          >
            <img
              className="hero-buddy"
              src={asset("assets/dippy-happy.webp")}
              alt=""
              width={76}
              height={76}
              loading="eager"
              decoding="async"
            />
          </motion.div>
          <motion.div
            animate={reduced ? { y: 0 } : { y: [0, -9, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
          >
            <motion.div style={{ rotateX, rotateY }}>
              <PhoneMock variant="home" />
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
