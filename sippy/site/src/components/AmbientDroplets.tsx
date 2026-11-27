import { useEffect, useRef } from "react";
import { asset } from "../lib/router";

/**
 * Ambient droplets (Phase 3 richness pass) — at most 12 small droplet
 * sprites drifting slowly across the backdrop layer, with subtle pointer
 * parallax. Paused off-screen (IntersectionObserver per droplet) and not
 * rendered at all under reduced motion / reduced effects.
 *
 * Deterministic pseudo-random layout (fixed seed) so it's stable between
 * visits and testable.
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

export function AmbientDroplets({ count = 12 }: { count?: number }) {
  const layerRef = useRef<HTMLDivElement>(null);
  const specs = useRef<DropSpec[]>(seededSpecs(Math.min(count, 12))).current;

  // pointer parallax on the whole layer (subtle)
  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;
    let tx = 0;
    let ty = 0;
    let cx = 0;
    let cy = 0;
    let raf = 0;
    const onMove = (e: PointerEvent) => {
      tx = (e.clientX / window.innerWidth - 0.5) * 2;
      ty = (e.clientY / window.innerHeight - 0.5) * 2;
    };
    const tick = () => {
      cx += (tx - cx) * 0.04;
      cy += (ty - cy) * 0.04;
      layer.style.transform = `translate3d(${(-cx * 5).toFixed(2)}px, ${(-cy * 4).toFixed(2)}px, 0)`;
      raf = requestAnimationFrame(tick);
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    raf = requestAnimationFrame(tick);
    return () => {
      window.removeEventListener("pointermove", onMove);
      cancelAnimationFrame(raf);
    };
  }, []);

  // pause offscreen droplets
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
  }, []);

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
            // --depth used by the CSS drift for per-droplet variance
            ["--depth" as string]: sp.depth,
          }}
          loading="lazy"
          decoding="async"
        />
      ))}
    </div>
  );
}
