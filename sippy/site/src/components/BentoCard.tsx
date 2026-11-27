/**
 * BentoCard — glass feature card with cursor-following specular highlight
 * and a slight tilt toward the pointer (transform only; disabled under
 * reduced motion and the gallery's "reduce effects" switch).
 */
import {
  useRef,
  type PointerEvent,
  type ReactNode,
} from "react";
import { GlassSurface } from "./glass/GlassSurface";

export function BentoCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  const onMove = (e: PointerEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const nx = (e.clientX - r.left) / r.width - 0.5;
    const ny = (e.clientY - r.top) / r.height - 0.5;
    el.style.setProperty("--tilt-y", `${(nx * 5).toFixed(2)}deg`);
    el.style.setProperty("--tilt-x", `${(-ny * 5).toFixed(2)}deg`);
  };

  const onLeave = () => {
    const el = ref.current;
    if (!el) return;
    el.style.setProperty("--tilt-x", "0deg");
    el.style.setProperty("--tilt-y", "0deg");
  };

  return (
    <div
      ref={ref}
      className={`bento${className ? ` ${className}` : ""}`}
      onPointerMove={onMove}
      onPointerLeave={onLeave}
    >
      <GlassSurface specular className="bento-inner">
        {children}
      </GlassSurface>
    </div>
  );
}
