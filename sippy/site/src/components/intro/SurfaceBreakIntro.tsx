/**
 * SurfaceBreakIntro — round-2 intro ("surface break").
 *
 * The page renders underneath from t=0 (zero CLS). The overlay carries:
 *  - the scene: a WebGL canvas (shader path) or .sb-scene (CSS path —
 *    same timeline with gradients/blur/ring); texture not ready within
 *    300 ms or no WebGL → CSS path, so a blank frame is never shown
 *  - the mark: DOM logo SVG, centred; at 3.0 s it FLIPs to the nav logo
 *    slot while the nav fades in
 *  - Dippy popping from the hole edge (3.05 s)
 *  - the reveal: an alpha hole centred on the mark (shader) / a
 *    transform-scaled shadow circle (CSS)
 *
 * Rules honoured: pointer-events none once the hole is >80% open; skip on
 * ANY key/click/tap (end state within 150 ms); paused when the tab is
 * hidden; canvas removed + GL context released on finish; the overlay is
 * aria-hidden with no focusable content (never traps focus); reduced
 * motion → this component renders nothing (HomePage plays a 350 ms
 * crossfade instead, and no WebGL context is ever created).
 *
 * NOTE (honest scope): the ring refracts the INTRO SCENE ONLY — inside the
 * shader. The page itself is a separate DOM layer and is never refracted
 * (that would require capturing the DOM).
 */
import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";
import { asset } from "../../lib/router";
import { createIntroGL, type IntroGL } from "../../lib/intro-shader";
import {
  INTRO,
  holeOpenFraction,
  maxRevealRadius,
  revealRadius,
  type Viewport,
} from "../../lib/intro2";

const MARK_PX = 132; // intro mark box (matches .sb-mark)

interface Props {
  /** t reached 3.0 s (page revealed) — hero words rise, cover lifts. */
  onReveal: () => void;
  /** The user skipped — collapse all delays to ~0. */
  onSkip: () => void;
  /** Timeline finished (or skip applied) — unmount the overlay. */
  onDone: () => void;
  /** ?debug=perf — log frame times to the console. */
  perf: boolean;
}

function easeInOut(p: number): number {
  return p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
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

/**
 * Centre of the nav logo icon in layout coordinates. The nav's entry
 * animation is a translate, so getBoundingClientRect would include it —
 * offsets ignore transforms; the icon's width is transform-independent
 * (the nav never scales), so GBC is safe for the width only.
 */
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

export function SurfaceBreakIntro({ onReveal, onSkip, onDone, perf }: Props) {
  const reduced = useReducedMotion() ?? false;
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

  useEffect(() => {
    // reduced motion: NOTHING runs here — no canvas, no GL context
    // (HomePage plays the 350 ms crossfade instead)
    if (reduced) return;
    const overlay = rootRef.current;
    const canvas = canvasRef.current;
    if (!overlay || !canvas) return;

    const vp: Viewport = { w: window.innerWidth, h: window.innerHeight };
    const onResize = () => {
      vp.w = window.innerWidth;
      vp.h = window.innerHeight;
      dpr = Math.min(window.devicePixelRatio || 1, 1.5);
      gl?.resize(vp.w, vp.h, dpr);
    };
    window.addEventListener("resize", onResize);

    let dpr = Math.min(window.devicePixelRatio || 1, 1.5);
    let gl: IntroGL | null = null;
    let mode: "pending" | "shader" | "css" = "pending";

    // — backdrop texture (the SAME asset as the hero backdrop) —
    const img = new Image();
    let textureReady = false;
    img.onload = () => {
      textureReady = true;
      caustRef.current?.style.setProperty("background-image", `url(${asset("assets/hero-caustics.webp")})`);
    };
    img.src = asset("assets/hero-caustics.webp");

    // 300 ms window: texture ready → shader, otherwise CSS. Never blank.
    const decide = window.setTimeout(() => {
      if (mode !== "pending") return;
      if (textureReady) {
        const g = createIntroGL(canvas);
        if (g) {
          gl = g;
          g.resize(vp.w, vp.h, dpr);
          g.setTexture(img);
          mode = "shader";
          canvas.style.transition = "opacity 0.15s ease";
          canvas.style.opacity = "0";
          requestAnimationFrame(() => {
            canvas.style.opacity = "1";
          });
          // the canvas now owns the scene — its alpha hole must reveal the
          // PAGE, not the CSS fallback scene, so remove the fallback once
          // the crossfade has finished
          window.setTimeout(() => {
            const s = rootRef.current?.querySelector<HTMLElement>(".sb-scene");
            if (s) s.style.display = "none";
          }, 220);
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
    let revealed = false;
    let passed80 = false;
    let finished = false;
    let skipped = false;
    let raf = 0;

    const onVisibility = () => {
      if (document.hidden) {
        pausedAt = performance.now();
      } else if (pausedAt !== null) {
        t0 += performance.now() - pausedAt; // keep the timeline paused
        lastNow = performance.now();
        pausedAt = null;
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    const applySkip = () => {
      if (skipped || finished) return;
      skipped = true;
      // jump the clock past the reveal: end state within one frame (<150 ms)
      t0 = performance.now() - 3.95 * 1000;
      lastNow = performance.now();
      // mark lands in the nav slot instantly
      const target = navLogoTarget();
      if (target && markRef.current) {
        const s = target.size / MARK_PX;
        markRef.current.style.transition = "opacity 0.12s ease";
        markRef.current.style.transform = `translate(${target.x - vp.w / 2}px, ${target.y - vp.h / 2}px) scale(${s.toFixed(4)})`;
        markRef.current.style.opacity = "0";
      }
      if (dippyRef.current) dippyRef.current.style.opacity = "0";
      onSkip();
      onReveal();
      window.setTimeout(() => finish(), 140);
    };

    const finish = () => {
      if (finished) return;
      finished = true;
      cancelAnimationFrame(raf);
      window.clearTimeout(decide);
      window.removeEventListener("keydown", onKey);
      overlay.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("resize", onResize);
      gl?.dispose(); // GL context released (WEBGL_lose_context); the canvas
      // node leaves the DOM with the overlay unmount — React owns it
      if (perf && frame > 0) {
        console.log(
          `[intro-perf] frames=${frame} avg=${(sumDt / frame).toFixed(2)}ms ` +
            `min=${minDt.toFixed(2)}ms max=${maxDt.toFixed(2)}ms renderer=${mode}`,
        );
      }
      onDone();
    };

    const onKey = () => applySkip();
    const onPointer = () => applySkip();
    window.addEventListener("keydown", onKey);
    overlay.addEventListener("pointerdown", onPointer);

    const cssFrame = (t: number) => {
      const light = Math.min(1, Math.max(0, (t - 0.4) / 1.2));
      const sc = 1.12 - 0.12 * Math.min(1, Math.max(0, (t - 0.4) / 1.2));
      if (worldRef.current) worldRef.current.style.transform = `scale(${sc})`;
      if (lightRef.current) lightRef.current.style.opacity = String(light * 0.9);
      shaftRefs.current.forEach((s, i) => {
        if (s) {
          s.style.opacity = String(light * (0.5 + 0.4 * Math.sin(t * 0.6 + i * 2.1)));
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
        // .sb-ring base circle is 60px: scale to rr * viewport height
        ringRef.current.style.transform = `scale(${((rr * vp.h) / 60).toFixed(3)})`;
      }
      // reveal: mask the SCENE subtree with a growing circular hole so the
      // PAGE shows through (the mark/dippy live OUTSIDE the masked subtree
      // so they stay visible while the hole opens). Fallback path only —
      // the shader path carves the same hole as canvas alpha.
      const mask = maskRef.current;
      if (mask && mode === "css") {
        const r = revealRadius(t, vp);
        if (r <= 1 || r >= maxRevealRadius(vp) + 4) {
          mask.style.maskImage = "none";
          mask.style.webkitMaskImage = "none";
        } else {
          const g = `radial-gradient(circle at 50% 50%, transparent ${Math.max(
            0,
            r - 2,
          ).toFixed(1)}px, black ${r.toFixed(1)}px)`;
          mask.style.maskImage = g;
          mask.style.webkitMaskImage = g;
        }
      }
    };

    const frameFn = () => {
      if (finished) return;
      const now = performance.now();
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

      if (mode === "shader" && gl) {
        // u_revealR is in DEVICE px (u_res is the drawing buffer size)
        gl.render(Math.min(t, INTRO.done), revealRadius(t, vp) * dpr);
      } else if (mode === "css") {
        cssFrame(Math.min(t, INTRO.done));
      }

      // pointer-events off once the hole is >80% open
      if (!passed80 && holeOpenFraction(t, vp) > 0.8) {
        passed80 = true;
        overlay.style.pointerEvents = "none";
      }

      // 3.0 s: reveal the page (words rise), then the mark FLIPs to the nav
      // (a skip has already applied the end state — never replay the beats)
      if (!revealed && !skipped && t >= INTRO.breakEnd) {
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
        // Dippy pops from the hole edge
        const dippy = dippyRef.current;
        if (dippy) {
          const dx = vp.w * 0.5 + Math.min(vp.w * 0.07, 120);
          const dy = vp.h * 0.5 + vp.h * 0.16;
          dippy.style.left = `${dx}px`;
          dippy.style.top = `${dy}px`;
          dippy.style.transition = "transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease";
          requestAnimationFrame(() => {
            dippy.style.opacity = "1";
            dippy.style.transform = "translate(-50%, 0) scale(1)";
          });
          dippy.style.animation = "sb-dippy-bob 1.6s ease-in-out 0.4s infinite";
          window.setTimeout(() => {
            if (dippy) {
              dippy.style.animation = "none";
              dippy.style.transition = "opacity 0.25s ease";
              dippy.style.opacity = "0";
            }
          }, 900);
        }
      }
      // 4.2 s: timeline complete (a skip finishes via its own 140 ms timeout)
      if (!skipped && t >= INTRO.done) {
        finish();
        return;
      }
      raf = requestAnimationFrame(frameFn);
    };

    // initial static state (frame 0) — dark, hidden mark/dippy/hole
    if (markRef.current) markRef.current.style.opacity = "0";
    raf = requestAnimationFrame(frameFn);

    // mark appears when the shader/blob forms it (2.4 s)
    const markAppear = window.setTimeout(() => {
      const m = markRef.current;
      if (m && !skipped) {
        m.style.transition = "opacity 0.15s ease";
        m.style.opacity = "1";
      }
    }, 2400);

    return () => {
      // unmount: release everything. Deliberately NOT onDone() —
      // StrictMode double-invokes effects in dev and onDone unmounts the
      // component (it would kill the second, real run); a mid-intro
      // navigation simply doesn't mark the intro played.
      finished = true;
      cancelAnimationFrame(raf);
      window.clearTimeout(decide);
      window.clearTimeout(markAppear);
      window.removeEventListener("keydown", onKey);
      overlay.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("resize", onResize);
      gl?.dispose();
    };
  }, [reduced, onReveal, onSkip, onDone, perf]);

  // reduced motion: nothing renders — no canvas, no GL context
  if (reduced) return null;

  return (
    <div
      ref={rootRef}
      className="sb"
      aria-hidden="true"
      role="presentation"
    >
      {/* CSS fallback scene (also the guaranteed non-blank first frames).
          This subtree gets the reveal MASK (growing circular hole) — the
          mark and Dippy live outside it so they survive the reveal. */}
      <div ref={maskRef} className="sb-masked">
        <div className="sb-scene">
          <div ref={worldRef} className="sb-world">
            <div className="sb-deep" />
            <div ref={caustRef} className="sb-caustics" />
            <div ref={lightRef} className="sb-light" />
            <div ref={(el) => { shaftRefs.current[0] = el; }} className="sb-shaft" style={{ left: "18%" }} />
            <div ref={(el) => { shaftRefs.current[1] = el; }} className="sb-shaft" style={{ left: "46%" }} />
            <div ref={(el) => { shaftRefs.current[2] = el; }} className="sb-shaft" style={{ left: "72%" }} />
          </div>
          {/* bubbles — CSS-driven (auto-pause when the tab is hidden) */}
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
            <span key={`d${i}`} ref={(el) => { dropRefs.current[i] = el; }} className="sb-drop" />
          ))}
          <div ref={ringRef} className="sb-ring" />
        </div>
      </div>
      {/* shader scene (fades in over the CSS scene if it wins the 300 ms
          race); its own alpha hole does the reveal in shader mode */}
      <canvas ref={canvasRef} className="sb-canvas" style={{ opacity: 0 }} />
      {/* the mark (DOM, crisp) — FLIP source at viewport centre */}
      <div ref={markRef} className="sb-mark">
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
      </div>
      {/* Dippy popping from the hole edge */}
      <img
        ref={dippyRef}
        className="sb-dippy"
        src={asset("assets/dippy-waving.webp")}
        alt=""
        width={74}
        height={80}
        style={{ left: 0, top: 0, transform: "translate(-50%, 40px) scale(0.7)" }}
        decoding="async"
      />
    </div>
  );
}
