import { useRef, type PointerEvent } from "react";
import {
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "motion/react";
import { GlassButton } from "../components/glass/GlassButton";
import { PhoneMock } from "../components/PhoneMock";
import { springs } from "../lib/tokens";

const WORDS = ["Drink", "consistently.", "Joyfully."];

/**
 * Hero — huge headline (words rise blur-to-sharp, staggered — beat 4 of the
 * intro), one-line pitch, shimmer CTA + glass secondary, and the app mockup
 * (real Sippy glass components) with mouse parallax + gentle idle float.
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
      <div className="hero-copy">
        <motion.h1
          className="hero-title"
          initial="hidden"
          animate="show"
          variants={{
            show: { transition: { staggerChildren: 0.09, delayChildren: startDelay / 1000 } },
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
          friends who notice, streaks that keep you honest, and reminders that
          never nag.
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
              document.getElementById("download")?.scrollIntoView({ behavior: "smooth" })
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
        <motion.div
          animate={{ y: [0, -9, 0] }}
          transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
        >
          <motion.div style={{ rotateX, rotateY, transformStyle: "preserve-3d" }}>
            <PhoneMock variant="home" />
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
