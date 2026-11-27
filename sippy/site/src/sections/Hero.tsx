import { useEffect, useRef, type PointerEvent } from "react";
import {
  motion,
  useMotionValue,
  useReducedMotion,
  useSpring,
  useTransform,
} from "motion/react";
import { CtaButton } from "../components/CtaButton";
import { HeroWaterRing } from "../components/HeroWaterRing";
import {
  canHoverFine,
  isReducedMotionActive,
  useQualityTier,
} from "../lib/quality";
import { asset } from "../lib/router";
import { springs } from "../lib/tokens";

const WORDS = ["Drink", "consistently.", "Joyfully."];

/**
 * Hero — two-column grid inside the shared 1280px wrap (same width as the
 * nav), vertically centered, full-viewport height.
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
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionActive();
  const tier = useQualityTier();
  const hoverTilt = tier === "high" && canHoverFine();

  const sectionRef = useRef<HTMLElement>(null);
  const visualRef = useRef<HTMLDivElement>(null);
  const rectRef = useRef<DOMRect | null>(null);
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const rotateY = useSpring(useTransform(mx, [-0.5, 0.5], [-5.5, 5.5]), springs.gentle);
  const rotateX = useSpring(useTransform(my, [-0.5, 0.5], [4.5, -4.5]), springs.gentle);

  // Pause Hero's CSS float loops whenever the section scrolls offscreen.
  useEffect(() => {
    const sec = sectionRef.current;
    if (!sec) return;
    const io = new IntersectionObserver(
      (entries) => {
        const visible = entries[0]?.isIntersecting ?? true;
        if (visible) {
          delete sec.dataset.offscreen;
        } else {
          sec.dataset.offscreen = "true";
        }
      },
      { threshold: 0 },
    );
    io.observe(sec);

    const invalidateRect = () => {
      rectRef.current = null;
    };
    window.addEventListener("scroll", invalidateRect, { passive: true });
    window.addEventListener("resize", invalidateRect, { passive: true });
    return () => {
      io.disconnect();
      window.removeEventListener("scroll", invalidateRect);
      window.removeEventListener("resize", invalidateRect);
    };
  }, []);

  const onPointerEnter = () => {
    if (reduced || !hoverTilt || !visualRef.current) return;
    rectRef.current = visualRef.current.getBoundingClientRect();
  };

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (reduced || !hoverTilt || e.pointerType === "touch") return;
    const el = visualRef.current;
    if (!el) return;
    const r = rectRef.current ?? (rectRef.current = el.getBoundingClientRect());
    if (r.width <= 0 || r.height <= 0) return;
    mx.set((e.clientX - r.left) / r.width - 0.5);
    my.set((e.clientY - r.top) / r.height - 0.5);
  };

  const onPointerLeave = () => {
    rectRef.current = null;
    mx.set(0);
    my.set(0);
  };

  const wordDur = wordsFast ? 0.15 : 0.5;
  const wordStagger = wordsFast ? 0.02 : 0.09;
  const useWordBlur = tier === "high" && !reduced;

  return (
    <section id="hero" className="hero" ref={sectionRef}>
      <div className="wrap hero-inner">
        <div className="hero-copy">
          <h1 className="hero-title">
            {WORDS.map((w, i) => (
              <motion.span
                key={w}
                className="hero-word"
                initial={
                  reduced
                    ? { opacity: 1, y: 0 }
                    : useWordBlur
                      ? { opacity: 0, y: 26, filter: "blur(10px)" }
                      : { opacity: 0, y: 20 }
                }
                animate={
                  playWords || reduced
                    ? useWordBlur
                      ? { opacity: 1, y: 0, filter: "blur(0px)" }
                      : { opacity: 1, y: 0 }
                    : { opacity: 0 }
                }
                transition={{
                  duration: reduced ? 0 : wordDur,
                  delay: reduced ? 0 : i * wordStagger,
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                {w}
              </motion.span>
            ))}
          </h1>
          <motion.p
            className="lead"
            initial={reduced ? { opacity: 1, y: 0 } : { opacity: 0, y: 16 }}
            animate={playWords || reduced ? { opacity: 1, y: 0 } : { opacity: 0 }}
            transition={{ delay: reduced ? 0 : wordsFast ? 0.1 : 0.55, duration: reduced ? 0 : wordDur }}
          >
            Sippy tracks water and every other drink you actually have — with
            friends who notice, streaks that keep you honest, and reminders
            that never nag.
          </motion.p>
          <motion.div
            className="hero-cta"
            initial={reduced ? { opacity: 1, y: 0 } : { opacity: 0, y: 14 }}
            animate={playWords || reduced ? { opacity: 1, y: 0 } : { opacity: 0 }}
            transition={{ delay: reduced ? 0 : wordsFast ? 0.12 : 0.75, duration: reduced ? 0 : wordDur }}
          >
            <CtaButton apk size="lg" />
            <CtaButton
              variant="ghost"
              size="lg"
              label="How it works"
              target="how"
              focusId="how-heading"
            />
          </motion.div>
        </div>
        <div
          className="hero-visual"
          ref={visualRef}
          onPointerEnter={hoverTilt ? onPointerEnter : undefined}
          onPointerMove={hoverTilt ? onPointerMove : undefined}
          onPointerLeave={hoverTilt ? onPointerLeave : undefined}
        >
          {/* Dippy floating by the ring — CSS transform bob, paused offscreen/hidden/RM */}
          <div className="hero-buddy-wrap">
            <img
              className="hero-buddy"
              src={asset("assets/dippy-happy.webp")}
              srcSet={`${asset("assets/dippy-happy-128.webp")} 128w, ${asset("assets/dippy-happy.webp")} 248w`}
              sizes="(max-width: 1000px) 64px, 76px"
              alt=""
              width={76}
              height={76}
              loading="eager"
              decoding="async"
            />
          </div>
          <div className="hero-orb-bob">
            <motion.div style={hoverTilt && !reduced ? { rotateX, rotateY } : undefined}>
              <HeroWaterRing />
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  );
}
