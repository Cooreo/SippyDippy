/**
 * SurfaceBreakIntro — round-3 smooth-exit intro ("surface break").
 *
 * Invariants (SPEC.md section 3):
 *  - No `#000` frame ever: darkest tone is `--backdrop-base` / `#060a14`, and
 *    WebGL clears to that tone at `alpha = 1` (never transparent black).
 *  - The page `<Backdrop />` renders from frame 0 under `.sb` (`background:
 *    transparent`); the hole opens from `markCentre(vp)` (`2.5–3.0 s`) to
 *    reveal that live backdrop, so removing `.sb` changes zero backdrop pixels.
 *  - Over the last 800 ms (`3.0–3.8 s`), `overlay.style.opacity` crossfades
 *    smoothly to `0` while Dippy waves from the hole's edge, the mark FLIPs
 *    to the nav slot, and hero words rise blur-to-sharp.
 *  - `.sb` is removed ONLY AFTER `overlay.style.opacity` has reached `"0"`.
 *  - Skip (click, tap, Esc, or any key) jumps to end state with a 150 ms
 *    crossfade to `opacity = 0` and no black flash.
 *  - Reduced motion: 500 ms crossfade from mark to page over `--backdrop-base`
 *    with NO `<canvas>`, NO WebGL context, NO ring, and NO black frame.
 */
import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";
import { isReducedMotionActive, resolveQualityTier } from "../../lib/quality";
import { asset } from "../../lib/router";
import { createIntroGL, type IntroGL } from "../../lib/intro-shader";
import {
  DARKEST_BACKDROP_RGB,
  INTRO,
  INTRO_REDUCED_FADE_MS,
  INTRO_SKIP_FADE_MS,
  clamp01,
  holeOpenFraction,
  maxRevealRadius,
  overlayOpacity,
  revealRadius,
  type Viewport,
} from "../../lib/intro2";

const MARK_PX = 132; // intro mark box (matches .sb-mark)

interface Props {
  /** t reached 3.0 s (page revealed) — hero words rise, cover lifts. */
  onReveal: () => void;
  /** The user skipped — collapse all delays to ~0. */
  onSkip: () => void;
  /** Timeline finished (after overlay opacity reached 0) — unmount overlay. */
  onDone: () => void;
  /** ?debug=perf — log frame times to the console. */
  perf: boolean;
}

function easeInOut(p: number): number {
  return p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
}

function isReducedMotionPreferred(): boolean {
  return isReducedMotionActive();
}

/** Layout position (offsets ignore transforms — the nav's entry
 *  animation is a transform, so getBoundingClientRect would lie). */
function layoutCenter(el: HTMLElement): { x: number; y: number } {
  let x = 0;
  let y = 0;
  let n: HTMLElement | null = el;
  while (n) {
    x += n.offsetLeft;
    y += n.offsetTop;
    n = n.offsetParent as HTMLElement | null;
  }
  return { x: x + el.offsetWidth / 2, y: y + el.offsetHeight / 2 };
}

function navLogoTarget(): { x: number; y: number; size: number } | null {
  const brand = document.querySelector<HTMLElement>(".gnav-brand");
  const svg = brand?.querySelector("svg");
  if (!brand || !svg) return null;
  const c = layoutCenter(brand);
  const size = svg.getBoundingClientRect().width;
  return {
    x: c.x - brand.offsetWidth / 2 + size / 2,
    y: c.y,
    size,
  };
}

function SippyMarkSvg() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="100%"
      height="100%"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 3.5c-2.5 4-6 8-6 11.5a6 6 0 0 0 12 0c0-3.5-3.5-7.5-6-11.5Z" />
      <path d="M9.1 14.6c-.4 2 0 3.4 1.4 4.4" />
    </svg>
  );
}

export function SurfaceBreakIntro({ onReveal, onSkip, onDone, perf }: Props) {
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionPreferred();
  const rootRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const maskRef = useRef<HTMLDivElement>(null);
  const worldRef = useRef<HTMLDivElement>(null);
  const lightRef = useRef<HTMLDivElement>(null);
  const caustRef = useRef<HTMLDivElement>(null);
  const shaftRefs = useRef<(HTMLElement | null)[]>([]);
  const dropRefs = useRef<(HTMLElement | null)[]>([]);
  const ringRef = useRef<HTMLDivElement>(null);
  const markRef = useRef<HTMLDivElement>(null);
  const dippyRef = useRef<HTMLImageElement>(null);

  // Reduced-motion path: 500 ms crossfade from mark to page (no canvas, no GL,
  // no ring, no black frame), skipped in 150 ms on click/tap/key.
  useEffect(() => {
    if (!reduced) return;
    const overlay = rootRef.current;
    if (!overlay) return;

    overlay.style.opacity = "1";
    onReveal();

    let finished = false;
    let raf = 0;
    let zeroPainted = false;
    let fadeStart = performance.now();
    let fadeDur = INTRO_REDUCED_FADE_MS;
    let startOpacity = 1;

    const cleanupListeners = () => {
      window.removeEventListener("keydown", onSkipInput);
      window.removeEventListener("pointerdown", onSkipInput);
    };

    const finish = () => {
      if (finished) return;
      finished = true;
      cancelAnimationFrame(raf);
      cleanupListeners();
      overlay.style.opacity = "0";
      onDone();
    };

    const onSkipInput = () => {
      if (finished) return;
      startOpacity = Number(overlay.style.opacity || "1");
      fadeStart = performance.now();
      fadeDur = INTRO_SKIP_FADE_MS;
      overlay.style.pointerEvents = "none";
      onSkip();
    };

    window.addEventListener("keydown", onSkipInput);
    window.addEventListener("pointerdown", onSkipInput, { passive: true });

    const step = (now: number) => {
      if (finished) return;
      const p = clamp01((now - fadeStart) / fadeDur);
      const op = startOpacity * (1 - easeInOut(p));
      if (p >= 1 || op <= 0.001) {
        overlay.style.opacity = "0";
        overlay.style.pointerEvents = "none";
        if (zeroPainted) {
          finish();
          return;
        }
        zeroPainted = true;
      } else {
        overlay.style.opacity = op.toFixed(4);
      }
      raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);

    return () => {
      finished = true;
      cancelAnimationFrame(raf);
      cleanupListeners();
    };
  }, [reduced, onReveal, onSkip, onDone]);

  // Full surface-break path (shader or CSS fallback)
  useEffect(() => {
    if (reduced) return;
    const overlay = rootRef.current;
    const canvas = canvasRef.current;
    if (!overlay || !canvas) return;

    overlay.style.opacity = "1";

    const isLow = resolveQualityTier() === "low";
    const computeDpr = () => {
      const base = Math.min(window.devicePixelRatio || 1, 1.5);
      return isLow ? Math.max(0.5, base * 0.5) : base;
    };

    const vp: Viewport = { w: window.innerWidth, h: window.innerHeight };
    let dpr = computeDpr();
    let gl: IntroGL | null = null;
    let mode: "pending" | "shader" | "css" = "pending";

    const onResize = () => {
      vp.w = window.innerWidth;
      vp.h = window.innerHeight;
      dpr = computeDpr();
      gl?.resize(vp.w, vp.h, dpr);
    };
    window.addEventListener("resize", onResize, { passive: true });

    // Low tier: pre-compile WebGL shader at half resolution BEFORE the intro
    // clock starts so no compilation stall can ever interrupt the animation.
    if (isLow) {
      const pre = createIntroGL(canvas, DARKEST_BACKDROP_RGB);
      if (pre) {
        gl = pre;
        pre.resize(vp.w, vp.h, dpr);
        pre.precompile();
        mode = "shader";
        canvas.style.opacity = "1";
        const s = overlay.querySelector<HTMLElement>(".sb-scene");
        if (s) s.style.display = "none";
      } else {
        mode = "css";
      }
    }

    // — backdrop texture (responsive variant on low tier, full on desktop).
    // The intro is the same dark "dive" moment in both themes, so it always
    // samples the DARK reef; in light theme it uses the 640 variant so it
    // never competes with the (different) daylight wallpaper the page loads.
    const isLightTheme = !document.documentElement.classList.contains("dark");
    const caustUrl = asset(
      isLow || isLightTheme
        ? "assets/hero-caustics-640.webp"
        : "assets/hero-caustics.webp",
    );
    const img = new Image();
    let textureReady = false;
    img.onload = () => {
      textureReady = true;
      caustRef.current?.style.setProperty(
        "background-image",
        `url(${caustUrl})`,
      );
      if (gl) {
        gl.setTexture(img);
      }
    };
    img.src = caustUrl;

    // Desktop high-tier 300 ms window: texture ready + WebGL → shader, else CSS.
    const decide = isLow
      ? 0
      : window.setTimeout(() => {
          if (mode !== "pending") return;
          if (textureReady) {
            const g = createIntroGL(canvas, DARKEST_BACKDROP_RGB);
            if (g) {
              gl = g;
              g.resize(vp.w, vp.h, dpr);
              g.precompile();
              g.setTexture(img);
              mode = "shader";
              canvas.style.transition = "opacity 0.15s ease";
              canvas.style.opacity = "0";
              requestAnimationFrame(() => {
                canvas.style.opacity = "1";
              });
              window.setTimeout(() => {
                const s = rootRef.current?.querySelector<HTMLElement>(".sb-scene");
                if (s) s.style.display = "none";
              }, 180);
              return;
            }
          }
          mode = "css";
        }, 300);

    // — timeline clock (pauses while the tab is hidden) —
    let t0 = performance.now();
    let pausedAt: number | null = null;
    let lastNow = t0;
    let frame = 0;
    let minDt = Infinity;
    let maxDt = 0;
    let sumDt = 0;
    let dippyShown = false;
    let revealed = false;
    let passed80 = false;
    let zeroPainted = false;
    let finished = false;
    let skipped = false;
    let skipStart = 0;
    let skipStartOp = 1;
    let raf = 0;

    const onVisibility = () => {
      if (document.hidden) {
        pausedAt = performance.now();
        if (raf) {
          cancelAnimationFrame(raf);
          raf = 0;
        }
      } else if (pausedAt !== null) {
        t0 += performance.now() - pausedAt;
        lastNow = performance.now();
        pausedAt = null;
        if (!finished && !raf) {
          raf = requestAnimationFrame(frameFn);
        }
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    const finish = () => {
      if (finished) return;
      finished = true;
      cancelAnimationFrame(raf);
      window.clearTimeout(decide);
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("resize", onResize);
      overlay.style.opacity = "0";
      gl?.dispose();
      if (perf && frame > 0) {
        console.log(
          `[intro-perf] frames=${frame} avg=${(sumDt / frame).toFixed(2)}ms ` +
            `min=${minDt.toFixed(2)}ms max=${maxDt.toFixed(2)}ms renderer=${mode}`,
        );
      }
      onDone();
    };

    const applySkip = () => {
      if (skipped || finished) return;
      skipped = true;
      zeroPainted = false;
      skipStart = performance.now();
      skipStartOp = Number(overlay.style.opacity || "1");
      overlay.style.pointerEvents = "none";

      const target = navLogoTarget();
      if (target && markRef.current) {
        const s = target.size / MARK_PX;
        markRef.current.style.transition = "opacity 0.12s ease";
        markRef.current.style.transform = `translate(${target.x - vp.w / 2}px, ${target.y - vp.h / 2}px) scale(${s.toFixed(4)})`;
        markRef.current.style.opacity = "0";
      }
      onSkip();
      if (!revealed) {
        revealed = true;
        onReveal();
      }
    };

    const onKey = () => applySkip();
    const onPointer = () => applySkip();
    window.addEventListener("keydown", onKey);
    window.addEventListener("pointerdown", onPointer, { passive: true });

    const cssFrame = (t: number) => {
      const light = Math.min(1, Math.max(0, (t - 0.4) / 1.2));
      const sc = 1.12 - 0.12 * Math.min(1, Math.max(0, (t - 0.4) / 1.2));
      if (worldRef.current) worldRef.current.style.transform = `scale(${sc})`;
      if (lightRef.current)
        lightRef.current.style.opacity = String(light * 0.9);
      shaftRefs.current.forEach((s, i) => {
        if (s) {
          s.style.opacity = String(
            light * (0.5 + 0.4 * Math.sin(t * 0.6 + i * 2.1)),
          );
          s.style.transform = `skewX(-14deg) translateX(${((t * 2.4 * (i + 1)) % 24).toFixed(2)}%)`;
        }
      });
      // four gooey droplets: edges → mark centre
      const mp = easeInOut(Math.min(1, Math.max(0, (t - 1.6) / 0.85)));
      const minDim = Math.min(vp.w, vp.h);
      dropRefs.current.forEach((d, i) => {
        if (!d) return;
        const a = 0.7854 + i * 1.5708;
        const sx = Math.cos(a) * minDim * 0.55;
        const sy = Math.sin(a) * minDim * 0.45;
        const ex = Math.cos(a) * 10;
        const ey = Math.sin(a) * 8;
        const x = sx + (ex - sx) * mp;
        const y = sy + (ey - sy) * mp;
        const fadeIn = Math.min(1, Math.max(0, (t - 1.6) / 0.25));
        const fadeOut = 1 - Math.min(1, Math.max(0, (t - 2.45) / 0.12));
        d.style.opacity = String(fadeIn * fadeOut);
        d.style.transform = `translate(${x.toFixed(1)}px, ${y.toFixed(1)}px) scale(${(1 - 0.15 * mp).toFixed(3)})`;
      });
      // ring (scale + opacity only)
      if (ringRef.current) {
        const rt =
          Math.min(1, Math.max(0, (t - 2.5) / 0.05)) *
          (1 - Math.min(1, Math.max(0, (t - 2.9) / 0.1)));
        const rr = 0.03 + 1.02 * Math.min(1, Math.max(0, (t - 2.5) / 0.5));
        ringRef.current.style.opacity = String(rt * 0.95);
        ringRef.current.style.transform = `scale(${((rr * vp.h) / 60).toFixed(3)})`;
      }
      // CSS fallback circular hole centred on the mark: once `r >= maxR`, keep
      // the scene hidden (`opacity = 0`) so the hole NEVER snaps shut!
      const mask = maskRef.current;
      if (mask && mode !== "shader") {
        const r = revealRadius(t, vp);
        const maxR = maxRevealRadius(vp);
        if (r <= 1) {
          mask.style.opacity = "1";
          mask.style.maskImage = "none";
          mask.style.webkitMaskImage = "none";
        } else if (r >= maxR) {
          mask.style.opacity = "0";
        } else {
          mask.style.opacity = "1";
          const g = `radial-gradient(circle at 50% 50%, transparent ${Math.max(
            0,
            r - 2,
          ).toFixed(1)}px, black ${r.toFixed(1)}px)`;
          mask.style.maskImage = g;
          mask.style.webkitMaskImage = g;
        }
      }
    };

    const showDippy = () => {
      const dippy = dippyRef.current;
      if (!dippy) return;
      const dx = vp.w * 0.5 + Math.min(vp.w * 0.07, 120);
      const dy = vp.h * 0.5 + vp.h * 0.16;
      dippy.style.left = `${dx}px`;
      dippy.style.top = `${dy}px`;
      dippy.style.transition =
        "transform 0.38s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease";
      requestAnimationFrame(() => {
        dippy.style.opacity = "1";
        dippy.style.transform = "translate(-50%, 0) scale(1)";
      });
      dippy.style.animation = "sb-dippy-bob 1.2s ease-in-out 0.32s infinite";
    };

    const frameFn = (now: number) => {
      if (finished || pausedAt !== null) {
        raf = 0;
        return;
      }

      // 150 ms skip crossfade to opacity = 0 before unmounting
      if (skipped) {
        const p = clamp01((now - skipStart) / INTRO_SKIP_FADE_MS);
        const op = skipStartOp * (1 - easeInOut(p));
        if (p >= 1 || op <= 0.001) {
          overlay.style.opacity = "0";
          if (zeroPainted) {
            finish();
            return;
          }
          zeroPainted = true;
        } else {
          overlay.style.opacity = op.toFixed(4);
        }
        raf = requestAnimationFrame(frameFn);
        return;
      }

      const dt = now - lastNow;
      lastNow = now;
      const t = (now - t0) / 1000;
      frame += 1;
      if (perf) {
        minDt = Math.min(minDt, dt);
        maxDt = Math.max(maxDt, dt);
        sumDt += dt;
        console.log(
          `[intro-perf] f=${frame} dt=${dt.toFixed(2)}ms t=${t.toFixed(3)}s`,
        );
      }

      const clampedT = Math.min(t, INTRO.done);
      if (mode === "shader" && gl) {
        gl.render(clampedT, revealRadius(clampedT, vp) * dpr);
      } else {
        cssFrame(clampedT);
      }

      // pointer-events off once the hole is >80 % open
      if (!passed80 && holeOpenFraction(t, vp) > 0.8) {
        passed80 = true;
        overlay.style.pointerEvents = "none";
      }

      // ~2.75 s: Dippy pops up right at the expanding hole's edge and waves
      // through the 3.0–3.8 s exit crossfade
      if (!dippyShown && t >= 2.75) {
        dippyShown = true;
        showDippy();
      }

      // 3.0 s: hole covers viewport → hero words rise blur-to-sharp, mark FLIPs
      // into nav brand slot, and 800 ms overlay crossfade (3.0–3.8 s) begins
      if (!revealed && t >= INTRO.breakEnd) {
        revealed = true;
        onReveal();
        const target = navLogoTarget();
        const mark = markRef.current;
        if (target && mark) {
          const s = target.size / MARK_PX;
          const dx = target.x - vp.w / 2;
          const dy = target.y - vp.h / 2;
          const start = performance.now();
          const fly = 480;
          const stepFlip = () => {
            if (finished) return;
            const p = Math.min(1, (performance.now() - start) / fly);
            const e = easeInOut(p);
            mark.style.transform = `translate(${(dx * e).toFixed(1)}px, ${(dy * e).toFixed(1)}px) scale(${(1 - (1 - s) * e).toFixed(4)})`;
            if (p < 1) requestAnimationFrame(stepFlip);
            else {
              mark.style.transition = "opacity 0.12s ease";
              mark.style.opacity = "0";
            }
          };
          requestAnimationFrame(stepFlip);
        }
      }

      // Last 800 ms (3.0 → 3.8 s): crossfade overlay opacity smoothly to 0;
      // unmount ONLY AFTER opacity has reached "0" and painted.
      const op = overlayOpacity(t);
      if (op <= 0) {
        overlay.style.opacity = "0";
        if (zeroPainted && t >= INTRO.done) {
          finish();
          return;
        }
        zeroPainted = true;
      } else {
        overlay.style.opacity = String(op);
      }

      raf = requestAnimationFrame(frameFn);
    };

    if (markRef.current) markRef.current.style.opacity = "0";
    raf = requestAnimationFrame(frameFn);

    const markAppear = window.setTimeout(() => {
      const m = markRef.current;
      if (m && !skipped && !finished) {
        m.style.transition = "opacity 0.15s ease";
        m.style.opacity = "1";
      }
    }, 2400);

    return () => {
      finished = true;
      cancelAnimationFrame(raf);
      window.clearTimeout(decide);
      window.clearTimeout(markAppear);
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("resize", onResize);
      gl?.dispose();
    };
  }, [reduced, onReveal, onSkip, onDone, perf]);

  // Reduced motion: 500 ms mark-to-page crossfade over `--backdrop-base`.
  // Strictly NO `<canvas>` (no WebGL context), NO ring, and NO `#000`.
  if (reduced) {
    return (
      <div
        ref={rootRef}
        className="sb sb--reduced"
        aria-hidden="true"
        role="presentation"
      >
        <div className="sb-rm-backdrop" />
        <div className="sb-mark">
          <SippyMarkSvg />
        </div>
      </div>
    );
  }

  return (
    <div ref={rootRef} className="sb" aria-hidden="true" role="presentation">
      {/* CSS fallback scene (also guarantees non-black frames 0..300 ms). */}
      <div ref={maskRef} className="sb-masked">
        <div className="sb-scene">
          <div ref={worldRef} className="sb-world">
            <div className="sb-deep" />
            <div ref={caustRef} className="sb-caustics" />
            <div ref={lightRef} className="sb-light" />
            <div
              ref={(el) => {
                shaftRefs.current[0] = el;
              }}
              className="sb-shaft"
              style={{ left: "18%" }}
            />
            <div
              ref={(el) => {
                shaftRefs.current[1] = el;
              }}
              className="sb-shaft"
              style={{ left: "46%" }}
            />
            <div
              ref={(el) => {
                shaftRefs.current[2] = el;
              }}
              className="sb-shaft"
              style={{ left: "72%" }}
            />
          </div>
          {Array.from({ length: 10 }).map((_, i) => (
            <span
              key={i}
              className="sb-bubble"
              style={{
                left: `${8 + ((i * 37) % 84)}%`,
                width: `${5 + ((i * 13) % 9)}px`,
                height: `${5 + ((i * 13) % 9)}px`,
                animationDuration: `${3.2 + ((i * 17) % 24) / 10}s`,
                animationDelay: `${-((i * 29) % 40) / 10}s`,
                opacity: 0,
              }}
            />
          ))}
          {Array.from({ length: 4 }).map((_, i) => (
            <span
              key={`d${i}`}
              ref={(el) => {
                dropRefs.current[i] = el;
              }}
              className="sb-drop"
            />
          ))}
          <div ref={ringRef} className="sb-ring" />
        </div>
      </div>
      {/* WebGL scene (clears to #060a14 at alpha = 1; alpha hole reveals <Backdrop />). */}
      <canvas ref={canvasRef} className="sb-canvas" style={{ opacity: 0 }} />
      {/* Centred DOM mark — FLIP source to nav brand slot */}
      <div ref={markRef} className="sb-mark">
        <SippyMarkSvg />
      </div>
      {/* Dippy popping up from the hole's edge during crossfade and waving */}
      <img
        ref={dippyRef}
        className="sb-dippy"
        src={asset("assets/dippy-waving.webp")}
        srcSet={`${asset("assets/dippy-waving-128.webp")} 128w, ${asset("assets/dippy-waving.webp")} 227w`}
        sizes="74px"
        alt=""
        width={74}
        height={80}
        style={{
          left: 0,
          top: 0,
          transform: "translate(-50%, 40px) scale(0.7)",
        }}
        decoding="async"
      />
    </div>
  );
}
