/**
 * BentoCard — shared glass card with spring pointer reaction (used by both
 * Features bento cards and Showcase feature cards — single implementation).
 *
 *  - Fine hover pointer (not reduced motion):
 *      • cursor-following specular highlight (transform + opacity only)
 *      • 3D tilt up to ±6 deg on a spring
 *      • 4px lift on hover
 *      • mascot parallax (--mascot-x / --mascot-y, shifting against pointer)
 *  - Touch / non-hover devices (not reduced motion):
 *      • press feedback only (scale 0.98 on spring), no tilt/lift/parallax
 *  - Reduced motion (OS or gallery emulate switch):
 *      • no tilt, lift, press scale or parallax; keeps static .spec highlight
 */
import {
  useEffect,
  useRef,
  useState,
  type PointerEvent,
  type ReactNode,
} from "react";
import { motion, useReducedMotion, useSpring } from "motion/react";
import { springs } from "../lib/tokens";
import { GlassSurface } from "./glass/GlassSurface";

const MAX_TILT_DEG = 6;
const PARALLAX_PX = 6;

function queryHoverFine(): boolean {
  return (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(hover: hover) and (pointer: fine)").matches
  );
}

function isMotionEmulatedOff(): boolean {
  return (
    typeof document !== "undefined" &&
    document.documentElement.hasAttribute("data-emulate-motion")
  );
}

export function BentoCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const osReduced = useReducedMotion() ?? false;
  const [canHover, setCanHover] = useState<boolean>(queryHoverFine);

  useEffect(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function")
      return;
    const mq = window.matchMedia("(hover: hover) and (pointer: fine)");
    const onChange = () => setCanHover(mq.matches);
    onChange();
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);

  const rotateX = useSpring(0, springs.base);
  const rotateY = useSpring(0, springs.base);
  const liftY = useSpring(0, springs.base);
  const scale = useSpring(1, springs.snappy);

  const resetPointerVars = (el: HTMLDivElement | null) => {
    if (!el) return;
    el.style.setProperty("--spec-o", "0");
    el.style.setProperty("--mascot-x", "0px");
    el.style.setProperty("--mascot-y", "0px");
  };

  const onEnter = (e: PointerEvent<HTMLDivElement>) => {
    const reduced = osReduced || isMotionEmulatedOff();
    if (reduced || !canHover || e.pointerType === "touch") return;
    liftY.set(-4);
  };

  const onMove = (e: PointerEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el) return;
    const reduced = osReduced || isMotionEmulatedOff();
    if (reduced || !canHover || e.pointerType === "touch") {
      rotateX.set(0);
      rotateY.set(0);
      liftY.set(0);
      resetPointerVars(el);
      return;
    }
    const r = el.getBoundingClientRect();
    if (r.width <= 0 || r.height <= 0) return;
    const lx = e.clientX - r.left;
    const ly = e.clientY - r.top;
    const nx = Math.max(-0.5, Math.min(0.5, lx / r.width - 0.5));
    const ny = Math.max(-0.5, Math.min(0.5, ly / r.height - 0.5));

    rotateY.set(nx * MAX_TILT_DEG * 2);
    rotateX.set(-ny * MAX_TILT_DEG * 2);
    liftY.set(-4);

    el.style.setProperty("--spec-x", `${lx.toFixed(1)}px`);
    el.style.setProperty("--spec-y", `${ly.toFixed(1)}px`);
    el.style.setProperty("--spec-o", "1");
    el.style.setProperty("--mascot-x", `${(-nx * PARALLAX_PX * 2).toFixed(2)}px`);
    el.style.setProperty("--mascot-y", `${(-ny * PARALLAX_PX * 2).toFixed(2)}px`);
  };

  const onLeave = () => {
    rotateX.set(0);
    rotateY.set(0);
    liftY.set(0);
    scale.set(1);
    resetPointerVars(ref.current);
  };

  const onDown = (e: PointerEvent<HTMLDivElement>) => {
    const reduced = osReduced || isMotionEmulatedOff();
    if (reduced) return;
    const target = e.target as HTMLElement | null;
    if (target?.closest('input[type="range"]')) return;
    scale.set(0.98);
  };

  const onUp = () => {
    scale.set(1);
  };

  const reduced = osReduced || isMotionEmulatedOff();

  return (
    <motion.div
      ref={ref}
      className={`bento${className ? ` ${className}` : ""}`}
      style={
        reduced
          ? undefined
          : {
              transformPerspective: 900,
              rotateX,
              rotateY,
              y: liftY,
              scale,
            }
      }
      onPointerEnter={onEnter}
      onPointerMove={onMove}
      onPointerLeave={onLeave}
      onPointerDown={onDown}
      onPointerUp={onUp}
      onPointerCancel={onUp}
    >
      <GlassSurface specular className="bento-inner">
        <span aria-hidden="true" className="bento-cursor-spec" />
        {children}
      </GlassSurface>
    </motion.div>
  );
}
