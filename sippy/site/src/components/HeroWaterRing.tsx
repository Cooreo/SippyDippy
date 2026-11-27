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
 * Motion budget: transform + opacity only. The water level is a
 * translateY on the fill, the wave is a CSS translateX on a clipped
 * element (the sanctioned exception), the shimmer is a transform sweep
 * + opacity. Paused when offscreen (IntersectionObserver);
 * prefers-reduced-motion → static 100 % state with the label, no
 * animation, no rAF loop.
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
              <svg viewBox="0 0 1200 120" preserveAspectRatio="none" aria-hidden="true">
                <path d="M0,60 C100,8 200,8 300,60 C400,112 500,112 600,60 C700,8 800,8 900,60 C1000,112 1100,112 1200,60 L1200,120 L0,120 Z" />
              </svg>
            </div>
          </div>
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
