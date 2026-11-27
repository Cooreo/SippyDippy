/**
 * AuroraBackdrop — procedural gradient-mesh backdrop (spec:
 * shared/docs/aurora.md). Single canvas, 4 blobs, time-based drift, 30fps cap,
 * DPR capped at 1.5 on high tier / 0.75 on low tier, pointer parallax only
 * where fine hover exists, paused when offscreen or `document.hidden`, and one
 * static frame (zero rAF) under reduced motion.
 */
import { useEffect, useRef } from "react";
import {
  canHoverFine,
  isReducedMotionActive,
  useQualityTier,
} from "../lib/quality";

interface BlobDef {
  h: number;
  r: number;
  ax: number;
  ay: number;
  px: number;
  py: number;
  period: number;
}

const BLOBS: BlobDef[] = [
  { h: 214, r: 0.42, ax: 0.18, ay: 0.14, px: 0.0, py: 0.0, period: 17 },
  { h: 268, r: 0.38, ax: 0.22, ay: 0.18, px: 2.1, py: 4.2, period: 23 },
  { h: 185, r: 0.4, ax: 0.2, ay: 0.15, px: 4.0, py: 1.3, period: 13 },
  { h: 300, r: 0.34, ax: 0.16, ay: 0.12, px: 1.2, py: 5.0, period: 21 },
];
const BASE_ALPHA = [0.5, 0.34, 0.3, 0.16];

export function AuroraBackdrop() {
  const ref = useRef<HTMLCanvasElement | null>(null);
  const tier = useQualityTier();

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const maxDpr = tier === "low" ? 0.75 : 1.5;
    const DPR = Math.min(window.devicePixelRatio || 1, maxDpr);
    const enablePointer = tier === "high" && canHoverFine();

    let W = 0;
    let H = 0;
    let inView = true;
    let raf = 0;
    let last = 0;
    let blobs: { def: BlobDef; img: HTMLCanvasElement; ox: number; oy: number }[] =
      [];

    const prerender = () => {
      const v = Math.max(W, H) || 1;
      const dark = document.documentElement.classList.contains("dark");
      const scale = dark ? 1 : 0.45;
      blobs = BLOBS.map((def, i) => {
        const alpha = BASE_ALPHA[i] * scale;
        const size = Math.ceil(def.r * 2 * v);
        const c = document.createElement("canvas");
        c.width = size;
        c.height = size;
        const g = c.getContext("2d");
        if (g) {
          const grad = g.createRadialGradient(
            size / 2,
            size / 2,
            0,
            size / 2,
            size / 2,
            size / 2,
          );
          grad.addColorStop(0, `hsla(${def.h}, 92%, 60%, ${alpha})`);
          grad.addColorStop(1, `hsla(${def.h}, 92%, 60%, 0)`);
          g.fillStyle = grad;
          g.fillRect(0, 0, size, size);
        }
        return { def, img: c, ox: 0, oy: 0 };
      });
    };

    const draw = (t: number) => {
      if (!blobs.length || W <= 0 || H <= 0) return;
      ctx.clearRect(0, 0, W, H);
      ctx.globalCompositeOperation = "screen";
      const v = Math.max(W, H);
      for (const b of blobs) {
        const x =
          v * (0.5 + Math.sin(t / b.def.period + b.def.px) * b.def.ax) + b.ox;
        const y =
          v * (0.5 + Math.cos(t / (b.def.period * 0.8) + b.def.py) * b.def.ay) +
          b.oy;
        ctx.drawImage(b.img, x - b.img.width / 2, y - b.img.height / 2);
      }
      ctx.globalCompositeOperation = "source-over";
    };

    const resize = () => {
      W = canvas.clientWidth;
      H = canvas.clientHeight;
      canvas.width = Math.max(1, Math.round(W * DPR));
      canvas.height = Math.max(1, Math.round(H * DPR));
      ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
      prerender();
      draw(isReducedMotionActive() ? 0 : performance.now() / 1000);
    };

    let ptrX = 0;
    let ptrY = 0;
    const onPointer = (e: PointerEvent) => {
      ptrX = e.clientX / (W || window.innerWidth || 1) - 0.5;
      ptrY = e.clientY / (H || window.innerHeight || 1) - 0.5;
    };

    const shouldRun = () =>
      inView && !document.hidden && !isReducedMotionActive();

    const tick = (now: number) => {
      if (!shouldRun()) {
        raf = 0;
        if (isReducedMotionActive()) draw(0);
        return;
      }
      if (enablePointer) {
        for (let i = 0; i < blobs.length; i += 1) {
          const b = blobs[i];
          b.ox += (-ptrX * (10 + i * 3) - b.ox) * 0.05;
          b.oy += (-ptrY * (8 + i * 2) - b.oy) * 0.05;
        }
      }
      if (now - last >= 33) {
        draw(now / 1000);
        last = now;
      }
      raf = requestAnimationFrame(tick);
    };

    const syncLoop = () => {
      if (shouldRun()) {
        if (!raf) raf = requestAnimationFrame(tick);
      } else {
        if (raf) {
          cancelAnimationFrame(raf);
          raf = 0;
        }
        if (isReducedMotionActive()) draw(0);
      }
    };

    const io = new IntersectionObserver(
      (entries) => {
        inView = entries[0]?.isIntersecting ?? true;
        syncLoop();
      },
      { threshold: 0 },
    );
    io.observe(canvas);

    // Re-tint blobs on theme flip and react to [data-emulate-motion].
    const mo = new MutationObserver(() => {
      prerender();
      draw(isReducedMotionActive() ? 0 : performance.now() / 1000);
      syncLoop();
    });
    mo.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["class", "data-emulate-motion"],
    });

    const mql =
      typeof window.matchMedia === "function"
        ? window.matchMedia("(prefers-reduced-motion: reduce)")
        : null;
    mql?.addEventListener("change", syncLoop);

    resize();
    window.addEventListener("resize", resize, { passive: true });
    document.addEventListener("visibilitychange", syncLoop);
    if (enablePointer) {
      window.addEventListener("pointermove", onPointer, { passive: true });
    }
    syncLoop();

    return () => {
      if (raf) cancelAnimationFrame(raf);
      io.disconnect();
      mo.disconnect();
      mql?.removeEventListener("change", syncLoop);
      window.removeEventListener("resize", resize);
      document.removeEventListener("visibilitychange", syncLoop);
      if (enablePointer) {
        window.removeEventListener("pointermove", onPointer);
      }
    };
  }, [tier]);

  return <canvas ref={ref} className="aurora" aria-hidden="true" />;
}
