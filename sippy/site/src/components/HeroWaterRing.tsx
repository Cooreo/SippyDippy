import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";
import {
  ORB_BUBBLES,
  ORB_CYCLE_MS,
  ORB_DRAIN_MS,
  ORB_HOLD_MS,
  ORB_RISE_MS,
  easeOrbDrain,
  easeOrbRise,
} from "../lib/orbMotion";
import { isReducedMotionActive, useQualityTier } from "../lib/quality";

const INITIAL_STATIC_PROGRESS = 0.68;

/**
 * HeroWaterRing — the hero's lively water orb.
 *
 * Strictly obeys the SPEC.md Water Orb Motion Rule: ONLY `transform` and
 * `opacity` are animated anywhere in the orb.
 *
 *  - Two pre-drawn wave layers on the surface at distinct speeds (4.6 s / 2.8 s)
 *    and amplitudes, driven by CSS `translate3d` only.
 *  - Rising bubbles capped at 8 on high tier and 4 on low tier.
 *  - Paused when offscreen (`IntersectionObserver`), when `document.hidden`,
 *    and under reduced motion.
 */
export function HeroWaterRing() {
  const osReduced = useReducedMotion() ?? false;
  const tier = useQualityTier();
  const bubbles = tier === "low" ? ORB_BUBBLES.slice(0, 4) : ORB_BUBBLES;

  const wrapRef = useRef<HTMLDivElement>(null);
  const fillRef = useRef<HTMLDivElement>(null);
  const numRef = useRef<HTMLSpanElement>(null);
  const doneRef = useRef<HTMLSpanElement>(null);
  const progressRef = useRef<number>(INITIAL_STATIC_PROGRESS);

  useEffect(() => {
    const wrap = wrapRef.current;
    const fill = fillRef.current;
    const num = numRef.current;
    const done = doneRef.current;
    if (!wrap || !fill || !num || !done) return;

    let lastLabel = "";
    const setLabel = (label: string) => {
      if (label !== lastLabel) {
        lastLabel = label;
        num.textContent = label;
      }
    };

    const apply = (p: number, sloshDeg: number, reached: boolean) => {
      const clamped = Math.max(0, Math.min(1, p));
      progressRef.current = clamped;
      wrap.style.setProperty("--hr-progress", clamped.toFixed(3));
      const ty = ((1 - clamped) * 100).toFixed(2);
      fill.style.transform =
        Math.abs(sloshDeg) > 0.01
          ? `translateY(${ty}%) rotate(${sloshDeg.toFixed(2)}deg)`
          : `translateY(${ty}%)`;
      num.style.opacity = reached ? "0" : "1";
      done.style.opacity = reached ? "1" : "0";
    };

    const reduced = osReduced || isReducedMotionActive();
    if (reduced) {
      const p = progressRef.current;
      wrap.classList.remove("is-done");
      wrap.dataset.reduced = "true";
      setLabel(`${Math.round(p * 100)}%`);
      apply(p, 0, false);
      return;
    }

    delete wrap.dataset.reduced;
    progressRef.current = 0;

    let raf = 0;
    let inView = true;
    let running = false;
    let t0 = performance.now();
    let frozen = 0;

    const frame = (now: number) => {
      if (!running) return;
      if (isReducedMotionActive()) {
        running = false;
        const p = progressRef.current;
        wrap.classList.remove("is-done");
        wrap.dataset.reduced = "true";
        setLabel(`${Math.round(p * 100)}%`);
        apply(p, 0, false);
        return;
      }
      const t = (now - t0) % ORB_CYCLE_MS;
      if (t < ORB_RISE_MS) {
        const p = easeOrbRise(t / ORB_RISE_MS);
        const topSway =
          p > 0.7 ? Math.sin((t / 1000) * 4.2) * 0.8 * ((p - 0.7) / 0.3) : 0;
        setLabel(`${Math.round(p * 100)}%`);
        apply(p, topSway, false);
        wrap.classList.remove("is-done");
      } else if (t < ORB_RISE_MS + ORB_HOLD_MS) {
        const ht = t - ORB_RISE_MS;
        const slosh =
          Math.sin((ht / 1000) * Math.PI * 2.4) *
          2.2 *
          Math.exp(-ht / 950);
        setLabel("100%");
        apply(1, slosh, true);
        wrap.classList.add("is-done");
      } else {
        const dt = t - ORB_RISE_MS - ORB_HOLD_MS;
        const u = dt / ORB_DRAIN_MS;
        const p = 1 - easeOrbDrain(u);
        const slosh = Math.sin(u * Math.PI * 2) * 1.5 * (1 - u);
        setLabel(`${Math.round(p * 100)}%`);
        apply(p, slosh, false);
        wrap.classList.remove("is-done");
      }
      raf = requestAnimationFrame(frame);
    };

    const syncRunning = () => {
      const shouldRun = inView && !document.hidden && !isReducedMotionActive();
      if (shouldRun && !running) {
        running = true;
        delete wrap.dataset.paused;
        t0 = performance.now() - frozen;
        raf = requestAnimationFrame(frame);
      } else if (!shouldRun && running) {
        running = false;
        wrap.dataset.paused = "true";
        frozen = performance.now() - t0;
        cancelAnimationFrame(raf);
      }
    };

    const io = new IntersectionObserver(
      (entries) => {
        inView = entries[0]?.isIntersecting ?? true;
        syncRunning();
      },
      { threshold: 0.15 },
    );
    io.observe(wrap);
    document.addEventListener("visibilitychange", syncRunning);
    syncRunning();

    return () => {
      running = false;
      cancelAnimationFrame(raf);
      io.disconnect();
      document.removeEventListener("visibilitychange", syncRunning);
    };
  }, [osReduced]);

  return (
    <div
      ref={wrapRef}
      className="hr"
      role="img"
      aria-label="Sippy water goal filling to 100 percent"
    >
      <div className="hr-disc">
        <div className="hr-glow" aria-hidden="true" />
        <div className="hr-water" aria-hidden="true">
          <div ref={fillRef} className="hr-fill">
            {/* Back wave: pre-drawn layer translated horizontally */}
            <div className="hr-wave hr-wave--back">
              <svg viewBox="0 0 1200 1000" preserveAspectRatio="none" aria-hidden="true">
                <path
                  fill="rgba(96, 165, 250, 0.44)"
                  d="M0,80 C95,18 205,18 300,80 C395,142 505,142 600,80 C695,18 805,18 900,80 C995,142 1105,142 1200,80 L1200,1000 L0,1000 Z"
                />
              </svg>
            </div>
            {/* Front wave: pre-drawn layer translated horizontally */}
            <div className="hr-wave hr-wave--front">
              <svg viewBox="0 0 1200 1000" preserveAspectRatio="none" aria-hidden="true">
                <defs>
                  <linearGradient
                    id="hrWaterGradBase"
                    x1="0"
                    y1="80"
                    x2="0"
                    y2="1000"
                    gradientUnits="userSpaceOnUse"
                  >
                    <stop offset="0" stopColor="rgb(96 155 245)" stopOpacity="0.54" />
                    <stop offset="0.45" stopColor="rgb(59 122 232)" stopOpacity="0.66" />
                    <stop offset="1" stopColor="rgb(37 92 208)" stopOpacity="0.80" />
                  </linearGradient>
                  <linearGradient
                    id="hrWaterGradVivid"
                    x1="0"
                    y1="80"
                    x2="0"
                    y2="1000"
                    gradientUnits="userSpaceOnUse"
                  >
                    <stop offset="0" stopColor="rgb(59 130 246)" stopOpacity="0.72" />
                    <stop offset="0.45" stopColor="rgb(37 99 235)" stopOpacity="0.84" />
                    <stop offset="1" stopColor="rgb(29 78 216)" stopOpacity="0.94" />
                  </linearGradient>
                </defs>
                <path
                  fill="url(#hrWaterGradBase)"
                  d="M0,80 C100,42 200,42 300,80 C400,118 500,118 600,80 C700,42 800,42 900,80 C1000,118 1100,118 1200,80 L1200,1000 L0,1000 Z"
                />
                <path
                  className="hr-wave-vivid"
                  fill="url(#hrWaterGradVivid)"
                  d="M0,80 C100,42 200,42 300,80 C400,118 500,118 600,80 C700,42 800,42 900,80 C1000,118 1100,118 1200,80 L1200,1000 L0,1000 Z"
                />
              </svg>
            </div>
            <div className="hr-fill-vivid" />
            <div className="hr-bubbles">
              {bubbles.map((b, i) => (
                <span
                  key={i}
                  className="hr-bubble"
                  style={{
                    left: `${b.x}%`,
                    width: `${b.size}px`,
                    height: `${b.size}px`,
                    animationDuration: `${b.dur}s`,
                    animationDelay: `${b.delay}s`,
                    ["--bub-dx" as string]: `${b.drift}px`,
                  }}
                />
              ))}
            </div>
          </div>
        </div>
        <div className="hr-caustic" aria-hidden="true">
          <span className="hr-caustic-a" />
          <span className="hr-caustic-b" />
        </div>
        <div className="hr-splash" aria-hidden="true" />
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
