import { useEffect, useMemo, useRef } from "react";
import {
  canHoverFine,
  isReducedMotionActive,
  useQualityTier,
} from "../lib/quality";
import { asset } from "../lib/router";

/**
 * Ambient droplets — at most 12 small droplet sprites on high tier and 4 on
 * low tier, drifting slowly across the backdrop layer. Pointer parallax runs
 * ONLY where fine hover exists, wakes on pointermove, and sleeps (`raf = 0`)
 * once eased to rest or when `document.hidden` / reduced motion is active.
 */
interface DropSpec {
  x: number; // % of viewport width
  y: number; // % of viewport height
  s: number; // px size
  o: number; // opacity
  d: number; // drift duration s
  delay: number; // s
  depth: number; // parallax factor
}

function seededSpecs(n: number): DropSpec[] {
  let seed = 1337;
  const rnd = () => {
    seed = (seed * 16807) % 2147483647;
    return seed / 2147483647;
  };
  return Array.from({ length: n }, () => ({
    x: 4 + rnd() * 92,
    y: 6 + rnd() * 86,
    s: 10 + Math.round(rnd() * 14),
    o: 0.14 + rnd() * 0.16,
    d: 7 + rnd() * 7,
    delay: -rnd() * 10,
    depth: 3 + rnd() * 9,
  }));
}

export function AmbientDroplets({ count }: { count?: number }) {
  const tier = useQualityTier();
  const cap = tier === "low" ? 4 : 12;
  const resolvedCount = Math.min(count ?? cap, cap);
  const layerRef = useRef<HTMLDivElement>(null);
  const specs = useMemo(() => seededSpecs(resolvedCount), [resolvedCount]);

  // Pointer parallax — high tier + fine hover only, sleeps when settled/hidden
  useEffect(() => {
    const layer = layerRef.current;
    if (!layer || tier === "low" || !canHoverFine()) {
      if (layer) layer.style.transform = "";
      return;
    }

    let tx = 0;
    let ty = 0;
    let cx = 0;
    let cy = 0;
    let raf = 0;
    let inView = true;

    const tick = () => {
      if (!inView || document.hidden || isReducedMotionActive()) {
        raf = 0;
        return;
      }
      cx += (tx - cx) * 0.04;
      cy += (ty - cy) * 0.04;
      layer.style.transform = `translate3d(${(-cx * 5).toFixed(2)}px, ${(-cy * 4).toFixed(2)}px, 0)`;
      if (Math.abs(tx - cx) > 0.002 || Math.abs(ty - cy) > 0.002) {
        raf = requestAnimationFrame(tick);
      } else {
        raf = 0;
      }
    };

    const wake = () => {
      if (!raf && inView && !document.hidden && !isReducedMotionActive()) {
        raf = requestAnimationFrame(tick);
      }
    };

    const onMove = (e: PointerEvent) => {
      tx = (e.clientX / (window.innerWidth || 1) - 0.5) * 2;
      ty = (e.clientY / (window.innerHeight || 1) - 0.5) * 2;
      wake();
    };

    const onVisibility = () => {
      if (document.hidden && raf) {
        cancelAnimationFrame(raf);
        raf = 0;
      }
    };

    const io = new IntersectionObserver(
      (entries) => {
        inView = entries[0]?.isIntersecting ?? true;
        if (!inView && raf) {
          cancelAnimationFrame(raf);
          raf = 0;
        }
      },
      { threshold: 0 },
    );
    io.observe(layer);

    window.addEventListener("pointermove", onMove, { passive: true });
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      io.disconnect();
      window.removeEventListener("pointermove", onMove);
      document.removeEventListener("visibilitychange", onVisibility);
      if (raf) cancelAnimationFrame(raf);
    };
  }, [tier]);

  // Pause offscreen droplets via IntersectionObserver (`document.hidden` is
  // also handled globally by `html[data-doc-hidden]`).
  useEffect(() => {
    const io = new IntersectionObserver(
      (entries) => {
        for (const en of entries) {
          const el = en.target as HTMLElement;
          el.style.animationPlayState = en.isIntersecting ? "running" : "paused";
        }
      },
      { threshold: 0 },
    );
    layerRef.current
      ?.querySelectorAll<HTMLElement>(".ambient-drop")
      .forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, [specs]);

  return (
    <div ref={layerRef} className="ambient" aria-hidden="true">
      {specs.map((sp, i) => (
        <img
          key={i}
          className="ambient-drop"
          src={asset("assets/droplet-sprite.webp")}
          alt=""
          width={sp.s}
          height={Math.round(sp.s * 1.29)}
          style={{
            left: `${sp.x}%`,
            top: `${sp.y}%`,
            opacity: sp.o,
            animationDuration: `${sp.d}s`,
            animationDelay: `${sp.delay}s`,
            ["--depth" as string]: sp.depth,
          }}
          loading="lazy"
          decoding="async"
        />
      ))}
    </div>
  );
}
