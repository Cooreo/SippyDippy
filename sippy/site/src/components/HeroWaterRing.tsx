import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";

const FILL_MS = 4000; // 0 → 100 %
const HOLD_MS = 1500; // pause at 100 % before reset
const CYCLE_MS = FILL_MS + HOLD_MS;

const easeInOutCubic = (x: number) =>
  x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;

/**
 * HeroWaterRing — the hero's live preview (replaces the phone).
 *
 * A glass disc that fills with water 0 → 100 % in ~4 s with a gentle wave
 * on the surface; the centred tabular-numbers label counts in sync; at
 * 100 % a brief shimmer sweeps the disc and the label crossfades to
 * "Goal reached"; it holds 1.5 s, resets, and loops.
 *
 * The surface is a SOFT CURVED EDGE: the wave SVG spans from just above
 * the water line to the bottom of the fill and is painted with an SVG
 * linearGradient that matches the fill's CSS gradient stop-for-stop, so
 * there is no straight band where the wave box ends — only the wavy
 * crests rise above the line.
 *
 * Motion budget: transform + opacity only. The water level is a
 * translateY on the fill, the wave is a CSS translateX on a clipped
 * element (the sanctioned exception), the caustic drift and shimmer are
 * transform + opacity. Paused when offscreen (IntersectionObserver);
 * prefers-reduced-motion → static 100 % state with the label, no
 * animation, no rAF loop (the wave curve stays, un-animated).
 */
export function HeroWaterRing() {
  const reduced = useReducedMotion() ?? false;
  const wrapRef = useRef<HTMLDivElement>(null);
  const fillRef = useRef<HTMLDivElement>(null);
  const numRef = useRef<HTMLSpanElement>(null);
  const doneRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const wrap = wrapRef.current;
    const fill = fillRef.current;
    const num = numRef.current;
    const done = doneRef.current;
    if (!wrap || !fill || !num || !done) return;

    const apply = (p: number, reached: boolean) => {
      fill.style.transform = `translateY(${((1 - p) * 100).toFixed(2)}%)`;
      num.style.opacity = reached ? "0" : "1";
      done.style.opacity = reached ? "1" : "0";
    };

    if (reduced) {
      // static 100 % state with the label, no animation
      num.textContent = "100%";
      apply(1, true);
      return;
    }

    let raf = 0;
    let running = true;
    let t0 = performance.now();
    let frozen = 0; // elapsed ms at pause, so the cycle resumes mid-flight

    const io = new IntersectionObserver(
      (entries) => {
        const visible = entries[0].isIntersecting;
        if (visible && !running) {
          running = true;
          t0 = performance.now() - frozen;
          raf = requestAnimationFrame(frame);
        } else if (!visible && running) {
          running = false;
          frozen = performance.now() - t0;
          cancelAnimationFrame(raf);
        }
      },
      { threshold: 0.2 },
    );
    io.observe(wrap);

    const frame = (now: number) => {
      if (!running) return;
      const t = (now - t0) % CYCLE_MS;
      if (t < FILL_MS) {
        const p = easeInOutCubic(t / FILL_MS);
        num.textContent = `${Math.round(p * 100)}%`;
        apply(p, false);
        wrap.classList.remove("is-done");
      } else {
        num.textContent = "100%";
        apply(1, true);
        wrap.classList.add("is-done");
      }
      raf = requestAnimationFrame(frame);
    };
    raf = requestAnimationFrame(frame);

    return () => {
      running = false;
      cancelAnimationFrame(raf);
      io.disconnect();
    };
  }, [reduced]);

  return (
    <div
      ref={wrapRef}
      className="hr"
      role="img"
      aria-label="Sippy water goal filling to 100 percent"
    >
      <div className="hr-disc">
        <div className="hr-water" aria-hidden="true">
          <div ref={fillRef} className="hr-fill">
            <div className="hr-wave">
              <svg viewBox="0 0 1200 1000" preserveAspectRatio="none" aria-hidden="true">
                <defs>
                  <linearGradient
                    id="hrWaterGrad"
                    x1="0"
                    y1="80"
                    x2="0"
                    y2="1000"
                    gradientUnits="userSpaceOnUse"
                  >
                    {/* stop-for-stop match of the .hr-fill CSS gradient
                        (surface at y=80 → 0%, 45%, 100%) — seamless */}
                    <stop offset="0" stopColor="rgb(59 130 246)" stopOpacity="0.62" />
                    <stop offset="0.45" stopColor="rgb(37 99 235)" stopOpacity="0.72" />
                    <stop offset="1" stopColor="rgb(29 78 216)" stopOpacity="0.85" />
                  </linearGradient>
                </defs>
                <path
                  fill="url(#hrWaterGrad)"
                  d="M0,80 C100,24 200,24 300,80 C400,136 500,136 600,80 C700,24 800,24 900,80 C1000,136 1100,136 1200,80 L1200,1000 L0,1000 Z"
                />
              </svg>
            </div>
          </div>
        </div>
        <div className="hr-caustic" aria-hidden="true">
          <span className="hr-caustic-a" />
          <span className="hr-caustic-b" />
        </div>
        <div className="hr-shimmer" aria-hidden="true">
          <div className="hr-shimmer-band" />
        </div>
        <div className="hr-label">
          <span ref={numRef} className="hr-num">
            0%
          </span>
          <span ref={doneRef} className="hr-goal">
            Goal reached
          </span>
        </div>
      </div>
    </div>
  );
}
