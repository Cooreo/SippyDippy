import { useRef } from "react";
import { motion, useScroll, useSpring } from "motion/react";
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
 * How it works — 3 steps with scroll-linked reveal (blur-to-sharp) and a
 * progress line that draws as you scroll.
 */
export function HowItWorks() {
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start 0.8", "end 0.45"],
  });
  const lineScale = useSpring(scrollYProgress, springs.gentle);

  return (
    <section id="how" className="how" ref={ref}>
      {/* focus target for the "How it works" CTA landing (preventScroll) */}
      <h2 id="how-heading" tabIndex={-1}>
        How it works
      </h2>
      <div className="how-track">
        <motion.div
          className="how-line"
          style={{ scaleY: lineScale }}
          aria-hidden="true"
        />
        {STEPS.map((s, i) => (
          <motion.div
            key={s.n}
            className="how-step"
            initial={{ opacity: 0, y: 28, filter: "blur(6px)" }}
            whileInView={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            viewport={{ once: true, margin: "-12% 0px" }}
            transition={{
              duration: 0.55,
              ease: [0.22, 1, 0.36, 1],
              delay: i * 0.05,
            }}
          >
            <div className="how-num" aria-hidden="true">
              {s.n}
            </div>
            <h3>{s.title}</h3>
            <p>{s.body}</p>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
