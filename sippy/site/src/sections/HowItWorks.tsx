import { useRef } from "react";
import { motion, useReducedMotion, useScroll, useSpring } from "motion/react";
import { isReducedMotionActive, useQualityTier } from "../lib/quality";
import { springs } from "../lib/tokens";

const STEPS = [
  {
    n: "01",
    title: "Pick a drink, log a sip",
    body: "Water or iced oolong — tap it, pick the volume, done. Takes two seconds.",
  },
  {
    n: "02",
    title: "Watch the goal fill",
    body: "Your daily minimum, counted by what the drink actually hydrates. Dippy notices.",
  },
  {
    n: "03",
    title: "Keep the streak",
    body: "Hit the minimum every day. Milestones at 3, 7, 14, 30 and 100 — with a small celebration each.",
  },
];

/**
 * How it works — 3 steps in a 2-column grid (step badge in the left column,
 * title + body in the right column — never absolutely positioned over text).
 */
export function HowItWorks() {
  const ref = useRef<HTMLElement>(null);
  const tier = useQualityTier();
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionActive();
  const useBlurReveal = tier === "high" && !reduced;

  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start 0.8", "end 0.45"],
  });
  const lineScale = useSpring(scrollYProgress, springs.gentle);

  return (
    <section id="how" className="how" ref={ref}>
      <h2 id="how-heading" tabIndex={-1}>
        How it works
      </h2>
      <div className="how-track">
        <motion.div
          className="how-line"
          style={reduced ? undefined : { scaleY: lineScale }}
          aria-hidden="true"
        />
        {STEPS.map((s, i) => (
          <motion.div
            key={s.n}
            className="how-step"
            initial={
              reduced
                ? { opacity: 1, y: 0 }
                : useBlurReveal
                  ? { opacity: 0, y: 28, filter: "blur(6px)" }
                  : { opacity: 0, y: 20 }
            }
            whileInView={
              useBlurReveal
                ? { opacity: 1, y: 0, filter: "blur(0px)" }
                : { opacity: 1, y: 0 }
            }
            viewport={{ once: true, margin: "-12% 0px" }}
            transition={{
              duration: reduced ? 0 : 0.55,
              ease: [0.22, 1, 0.36, 1],
              delay: reduced ? 0 : i * 0.05,
            }}
          >
            <div className="how-num" aria-hidden="true">
              {s.n}
            </div>
            <div className="how-step-body">
              <h3>{s.title}</h3>
              <p>{s.body}</p>
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
