import { useRef, type PointerEvent } from "react";
import {
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "motion/react";
import { GlassButton } from "../components/glass/GlassButton";
import { PhoneMock } from "../components/PhoneMock";
import { asset } from "../lib/router";
import { springs } from "../lib/tokens";

const WORDS = ["Drink", "consistently.", "Joyfully."];

/**
 * Hero — two-column grid inside the shared 1280px wrap (same width as the
 * nav, root-cause fix B), vertically centered, full-viewport height. Words
 * rise blur-to-sharp (staggered; delayed to land as the intro lifts when
 * playing). The phone mockup (real Sippy glass components) gets spring
 * parallax with the pointer and a gentle idle float.
 *
 * The hero is dark via the backdrop's soft scrim — it has NO section
 * background of its own (root-cause fix A).
 */
export function Hero({ startDelay = 0 }: { startDelay?: number }) {
  const visualRef = useRef<HTMLDivElement>(null);
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const rotateY = useSpring(useTransform(mx, [-0.5, 0.5], [-5.5, 5.5]), springs.gentle);
  const rotateX = useSpring(useTransform(my, [-0.5, 0.5], [4.5, -4.5]), springs.gentle);

  const onPointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const el = visualRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    mx.set((e.clientX - r.left) / r.width - 0.5);
    my.set((e.clientY - r.top) / r.height - 0.5);
  };

  return (
    <section id="hero" className="hero">
      <div className="wrap hero-inner">
        <div className="hero-copy">
          <motion.h1
            className="hero-title"
            initial="hidden"
            animate="show"
            variants={{
              show: {
                transition: { staggerChildren: 0.09, delayChildren: startDelay / 1000 },
              },
              hidden: {},
            }}
          >
            {WORDS.map((w) => (
              <motion.span
                key={w}
                className="hero-word"
                variants={{
                  hidden: { opacity: 0, y: 26, filter: "blur(10px)" },
                  show: {
                    opacity: 1,
                    y: 0,
                    filter: "blur(0px)",
                    transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] },
                  },
                }}
              >
                {w}
              </motion.span>
            ))}
          </motion.h1>
          <motion.p
            className="lead"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: startDelay / 1000 + 0.55, duration: 0.5 }}
          >
            Sippy tracks water and every other drink you actually have — with
            friends who notice, streaks that keep you honest, and reminders
            that never nag.
          </motion.p>
          <motion.div
            className="hero-cta"
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: startDelay / 1000 + 0.75, duration: 0.5 }}
          >
            <GlassButton
              size="lg"
              onClick={() =>
                document
                  .getElementById("download")
                  ?.scrollIntoView({ behavior: "smooth" })
              }
            >
              Get the APK
            </GlassButton>
            <a className="hero-secondary" href="#how">
              How it works
            </a>
          </motion.div>
        </div>
        <div
          className="hero-visual"
          ref={visualRef}
          onPointerMove={onPointerMove}
          aria-hidden="true"
        >
          {/* Dippy floating by the phone — gentle idle bob, never over text */}
          <img
            className="hero-buddy"
            src={asset("assets/dippy-happy.webp")}
            alt=""
            width={76}
            height={76}
            loading="eager"
            decoding="async"
          />
          <motion.div
            animate={{ y: [0, -9, 0] }}
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
